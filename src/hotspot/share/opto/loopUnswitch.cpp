/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "precompiled.hpp"
#include "memory/allocation.inline.hpp"
#include "opto/mulnode.hpp"
#include "opto/addnode.hpp"
#include "opto/connode.hpp"
#include "opto/convertnode.hpp"
#include "opto/loopnode.hpp"
#include "opto/opaquenode.hpp"
#include "opto/rootnode.hpp"

//================= Loop Unswitching =====================
//
// orig:                       transformed:
//                               if (invariant-test) then
//  predicate                      predicate
//  loop                           loop
//    stmt1                          stmt1
//    if (invariant-test) then       stmt2
//      stmt2                        stmt4
//    else                         endloop
//      stmt3                    else
//    endif                        predicate [clone]
//    stmt4                        loop [clone]
//  endloop                          stmt1 [clone]
//                                   stmt3
//                                   stmt4 [clone]
//                                 endloop
//                               endif
//
// Note: the "else" clause may be empty


//------------------------------policy_unswitching-----------------------------
// Return TRUE or FALSE if the loop should be unswitched
// (ie. clone loop with an invariant test that does not exit the loop)
bool IdealLoopTree::policy_unswitching( PhaseIdealLoop *phase ) const {
  if (!LoopUnswitching) {
    return false;
  }
  if (!_head->is_Loop()) {
    return false;
  }

  // If nodes are depleted, some transform has miscalculated its needs.
  assert(!phase->exceeding_node_budget(), "sanity");

  // check for vectorized loops, any unswitching was already applied
  if (_head->is_CountedLoop() && _head->as_CountedLoop()->is_unroll_only()) {
    return false;
  }

  LoopNode* head = _head->as_Loop();
  if (head->unswitch_count() + 1 > head->unswitch_max()) {
    return false;
  }

  if (head->is_flattened_arrays()) {
    return false;
  }

  Node_List unswitch_iffs;
  if (phase->find_unswitching_candidate(this, unswitch_iffs) == NULL) {
    return false;
  }

  // Too speculative if running low on nodes.
  return phase->may_require_nodes(est_loop_clone_sz(2));
}

//------------------------------find_unswitching_candidate-----------------------------
// Find candidate "if" for unswitching
IfNode* PhaseIdealLoop::find_unswitching_candidate(const IdealLoopTree *loop, Node_List& unswitch_iffs) const {

  // Find first invariant test that doesn't exit the loop
  LoopNode *head = loop->_head->as_Loop();
  IfNode* unswitch_iff = NULL;
  Node* n = head->in(LoopNode::LoopBackControl);
  while (n != head) {
    Node* n_dom = idom(n);
    if (n->is_Region()) {
      if (n_dom->is_If()) {
        IfNode* iff = n_dom->as_If();
        if (iff->in(1)->is_Bool()) {
          BoolNode* bol = iff->in(1)->as_Bool();
          if (bol->in(1)->is_Cmp()) {
            // If condition is invariant and not a loop exit,
            // then found reason to unswitch.
            if (loop->is_invariant(bol) && !loop->is_loop_exit(iff)) {
              unswitch_iff = iff;
            }
          }
        }
      }
    }
    n = n_dom;
  }
  if (unswitch_iff != NULL) {
    unswitch_iffs.push(unswitch_iff);
  }

  // Collect all non-flattened array checks for unswitching to create a fast loop
  // without checks (only non-flattened array accesses) and a slow loop with checks.
  if (unswitch_iff == NULL || unswitch_iff->is_non_flattened_array_check(&_igvn)) {
    for (uint i = 0; i < loop->_body.size(); i++) {
      IfNode* n = loop->_body.at(i)->isa_If();
      if (n != NULL && n != unswitch_iff && n->is_non_flattened_array_check(&_igvn) &&
          loop->is_invariant(n->in(1)) && !loop->is_loop_exit(n)) {
        unswitch_iffs.push(n);
        if (unswitch_iff == NULL) {
          unswitch_iff = n;
        }
      }
    }
  }
  return unswitch_iff;
}

//------------------------------do_unswitching-----------------------------
// Clone loop with an invariant test (that does not exit) and
// insert a clone of the test that selects which version to
// execute.
void PhaseIdealLoop::do_unswitching(IdealLoopTree *loop, Node_List &old_new) {

  LoopNode *head = loop->_head->as_Loop();
  Node* entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  if (find_predicate_insertion_point(entry, Deoptimization::Reason_loop_limit_check) != NULL
      || (UseProfiledLoopPredicate && find_predicate_insertion_point(entry, Deoptimization::Reason_profile_predicate) != NULL)
      || (UseLoopPredicate && find_predicate_insertion_point(entry, Deoptimization::Reason_predicate) != NULL)) {
    assert(entry->is_IfProj(), "sanity - must be ifProj since there is at least one predicate");
    if (entry->outcnt() > 1) {
      // Bailout if there are loop predicates from which there are additional control dependencies (i.e. from
      // loop entry 'entry') to previously partially peeled statements since this case is not handled and can lead
      // to wrong execution. Remove this bailout, once this is fixed.
      return;
    }
  }
  // Find first invariant test that doesn't exit the loop
  Node_List unswitch_iffs;
  IfNode* unswitch_iff = find_unswitching_candidate((const IdealLoopTree *)loop, unswitch_iffs);
  assert(unswitch_iff != NULL && unswitch_iffs.size() > 0, "should be at least one");

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("Unswitch   %d ", head->unswitch_count()+1);
    loop->dump_head();
    for (uint i = 0; i < unswitch_iffs.size(); i++) {
      unswitch_iffs.at(i)->dump(3);
      tty->cr();
    }
  }
#endif

  // Need to revert back to normal loop
  if (head->is_CountedLoop() && !head->as_CountedLoop()->is_normal_loop()) {
    head->as_CountedLoop()->set_normal_loop();
  }

  ProjNode* proj_true = create_slow_version_of_loop(loop, old_new, unswitch_iff->Opcode(), CloneIncludesStripMined);

#ifdef ASSERT
  assert(proj_true->is_IfTrue(), "must be true projection");
  entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  Node* predicate = find_predicate(entry);
  if (predicate == NULL) {
    // No empty predicate
    Node* uniqc = proj_true->unique_ctrl_out();
    assert((uniqc == head && !head->is_strip_mined()) || (uniqc == head->in(LoopNode::EntryControl)
           && head->is_strip_mined()), "must hold by construction if no predicates");
  } else {
    // There is at least one empty predicate. When calling 'skip_loop_predicates' on each found empty predicate,
    // we should end up at 'proj_true'.
    Node* proj_before_first_empty_predicate = skip_loop_predicates(entry);
    if (UseProfiledLoopPredicate) {
      predicate = find_predicate(proj_before_first_empty_predicate);
      if (predicate != NULL) {
        proj_before_first_empty_predicate = skip_loop_predicates(predicate);
      }
    }
    if (UseLoopPredicate) {
      predicate = find_predicate(proj_before_first_empty_predicate);
      if (predicate != NULL) {
        proj_before_first_empty_predicate = skip_loop_predicates(predicate);
      }
    }
    assert(proj_true == proj_before_first_empty_predicate, "must hold by construction if at least one predicate");
  }
#endif
  // Increment unswitch count
  LoopNode* head_clone = old_new[head->_idx]->as_Loop();
  int nct = head->unswitch_count() + 1;
  head->set_unswitch_count(nct);
  head_clone->set_unswitch_count(nct);

  // Add test to new "if" outside of loop
  IfNode* invar_iff   = proj_true->in(0)->as_If();
  Node* invar_iff_c   = invar_iff->in(0);
  invar_iff->_prob    = unswitch_iff->_prob;
  BoolNode* bol       = unswitch_iff->in(1)->as_Bool();
  if (unswitch_iffs.size() > 1) {
    // Flattened array checks are used on array access to switch between
    // a legacy object array access and a flattened inline type array
    // access. We want the performance impact on legacy accesses to be
    // as small as possible so we make two copies of the loop: a fast
    // one where all accesses are known to be legacy, a slow one where
    // some accesses are to flattened arrays. Flattened array checks
    // can be removed from the fast loop but not from the slow loop
    // as it can have a mix of flattened/legacy accesses.
    bol = bol->clone()->as_Bool();
    register_new_node(bol, invar_iff->in(0));
    Node* cmp = bol->in(1)->clone();
    register_new_node(cmp, invar_iff->in(0));
    bol->set_req(1, cmp);
    // Combine all checks into a single one that fails if one array is flattened
    Node* in1 = NULL;
    for (uint i = 0; i < unswitch_iffs.size(); i++) {
      Node* array_tag = unswitch_iffs.at(i)->in(1)->in(1)->in(1);
      array_tag = new AndINode(array_tag, _igvn.intcon(Klass::_lh_array_tag_vt_value));
      register_new_node(array_tag, invar_iff->in(0));
      if (in1 == NULL) {
        in1 = array_tag;
      } else {
        in1 = new OrINode(in1, array_tag);
        register_new_node(in1, invar_iff->in(0));
      }
    }
    cmp->set_req(1, in1);
  }
  invar_iff->set_req(1, bol);

  // Hoist invariant casts out of each loop to the appropriate control projection.
  Node_List worklist;
  for (uint i = 0; i < unswitch_iffs.size(); i++) {
    IfNode* iff = unswitch_iffs.at(i)->as_If();
    for (DUIterator_Fast imax, i = iff->fast_outs(imax); i < imax; i++) {
      ProjNode* proj = iff->fast_out(i)->as_Proj();
      // Copy to a worklist for easier manipulation
      for (DUIterator_Fast jmax, j = proj->fast_outs(jmax); j < jmax; j++) {
        Node* use = proj->fast_out(j);
        if (use->Opcode() == Op_CheckCastPP && loop->is_invariant(use->in(1))) {
          worklist.push(use);
        }
      }
      ProjNode* invar_proj = invar_iff->proj_out(proj->_con)->as_Proj();
      while (worklist.size() > 0) {
        Node* use = worklist.pop();
        Node* nuse = use->clone();
        nuse->set_req(0, invar_proj);
        _igvn.replace_input_of(use, 1, nuse);
        register_new_node(nuse, invar_proj);
        // Same for the clone if we are removing checks from the slow loop
        if (unswitch_iffs.size() == 1) {
          Node* use_clone = old_new[use->_idx];
          _igvn.replace_input_of(use_clone, 1, nuse);
        }
      }
    }
  }

  // Hardwire the control paths in the loops into if(true) and if(false)
  for (uint i = 0; i < unswitch_iffs.size(); i++) {
    IfNode* iff = unswitch_iffs.at(i)->as_If();
    _igvn.rehash_node_delayed(iff);
    dominated_by(proj_true, iff, false, false);
  }
  IfNode* unswitch_iff_clone = old_new[unswitch_iff->_idx]->as_If();
  if (unswitch_iffs.size() == 1) {
    ProjNode* proj_false = invar_iff->proj_out(0)->as_Proj();
    _igvn.rehash_node_delayed(unswitch_iff_clone);
    dominated_by(proj_false, unswitch_iff_clone, false, false);
  } else {
    // Leave the flattened array checks in the slow loop and
    // prevent it from being unswitched again based on these checks.
    head_clone->mark_flattened_arrays();
  }

  // Reoptimize loops
  loop->record_for_igvn();
  for(int i = loop->_body.size() - 1; i >= 0 ; i--) {
    Node *n = loop->_body[i];
    Node *n_clone = old_new[n->_idx];
    _igvn._worklist.push(n_clone);
  }

#ifndef PRODUCT
  if (TraceLoopUnswitching) {
    for (uint i = 0; i < unswitch_iffs.size(); i++) {
      tty->print_cr("Loop unswitching orig: %d @ %d  new: %d @ %d",
                    head->_idx,                unswitch_iffs.at(i)->_idx,
                    old_new[head->_idx]->_idx, old_new[unswitch_iffs.at(i)->_idx]->_idx);
    }
  }
#endif

  C->set_major_progress();
}

//-------------------------create_slow_version_of_loop------------------------
// Create a slow version of the loop by cloning the loop
// and inserting an if to select fast-slow versions.
// Return control projection of the entry to the fast version.
ProjNode* PhaseIdealLoop::create_slow_version_of_loop(IdealLoopTree *loop,
                                                      Node_List &old_new,
                                                      int opcode,
                                                      CloneLoopMode mode) {
  LoopNode* head  = loop->_head->as_Loop();
  bool counted_loop = head->is_CountedLoop();
  Node*     entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  _igvn.rehash_node_delayed(entry);
  IdealLoopTree* outer_loop = loop->_parent;

  head->verify_strip_mined(1);

  Node *cont      = _igvn.intcon(1);
  set_ctrl(cont, C->root());
  Node* opq       = new Opaque1Node(C, cont);
  register_node(opq, outer_loop, entry, dom_depth(entry));
  Node *bol       = new Conv2BNode(opq);
  register_node(bol, outer_loop, entry, dom_depth(entry));
  IfNode* iff = (opcode == Op_RangeCheck) ? new RangeCheckNode(entry, bol, PROB_MAX, COUNT_UNKNOWN) :
    new IfNode(entry, bol, PROB_MAX, COUNT_UNKNOWN);
  register_node(iff, outer_loop, entry, dom_depth(entry));
  ProjNode* iffast = new IfTrueNode(iff);
  register_node(iffast, outer_loop, iff, dom_depth(iff));
  ProjNode* ifslow = new IfFalseNode(iff);
  register_node(ifslow, outer_loop, iff, dom_depth(iff));
  uint idx_before_clone = Compile::current()->unique();

  // Clone the loop body.  The clone becomes the slow loop.  The
  // original pre-header will (illegally) have 3 control users
  // (old & new loops & new if).
  clone_loop(loop, old_new, dom_depth(head->skip_strip_mined()), mode, iff);
  assert(old_new[head->_idx]->is_Loop(), "" );

  // Fast (true) control
  Node* iffast_pred = clone_loop_predicates(entry, iffast, !counted_loop, false, idx_before_clone, old_new);

  // Slow (false) control
  Node* ifslow_pred = clone_loop_predicates(entry, ifslow, !counted_loop, true, idx_before_clone, old_new);

  Node* l = head->skip_strip_mined();
  _igvn.replace_input_of(l, LoopNode::EntryControl, iffast_pred);
  set_idom(l, iffast_pred, dom_depth(l));
  LoopNode* slow_l = old_new[head->_idx]->as_Loop()->skip_strip_mined();
  _igvn.replace_input_of(slow_l, LoopNode::EntryControl, ifslow_pred);
  set_idom(slow_l, ifslow_pred, dom_depth(l));

  recompute_dom_depth();

  return iffast;
}

LoopNode* PhaseIdealLoop::create_reserve_version_of_loop(IdealLoopTree *loop, CountedLoopReserveKit* lk) {
  Node_List old_new;
  LoopNode* head  = loop->_head->as_Loop();
  bool counted_loop = head->is_CountedLoop();
  Node*     entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  _igvn.rehash_node_delayed(entry);
  IdealLoopTree* outer_loop = head->is_strip_mined() ? loop->_parent->_parent : loop->_parent;

  ConINode* const_1 = _igvn.intcon(1);
  set_ctrl(const_1, C->root());
  IfNode* iff = new IfNode(entry, const_1, PROB_MAX, COUNT_UNKNOWN);
  register_node(iff, outer_loop, entry, dom_depth(entry));
  ProjNode* iffast = new IfTrueNode(iff);
  register_node(iffast, outer_loop, iff, dom_depth(iff));
  ProjNode* ifslow = new IfFalseNode(iff);
  register_node(ifslow, outer_loop, iff, dom_depth(iff));

  // Clone the loop body.  The clone becomes the slow loop.  The
  // original pre-header will (illegally) have 3 control users
  // (old & new loops & new if).
  clone_loop(loop, old_new, dom_depth(head), CloneIncludesStripMined, iff);
  assert(old_new[head->_idx]->is_Loop(), "" );

  LoopNode* slow_head = old_new[head->_idx]->as_Loop();

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print_cr("PhaseIdealLoop::create_reserve_version_of_loop:");
    tty->print("\t iff = %d, ", iff->_idx); iff->dump();
    tty->print("\t iffast = %d, ", iffast->_idx); iffast->dump();
    tty->print("\t ifslow = %d, ", ifslow->_idx); ifslow->dump();
    tty->print("\t before replace_input_of: head = %d, ", head->_idx); head->dump();
    tty->print("\t before replace_input_of: slow_head = %d, ", slow_head->_idx); slow_head->dump();
  }
#endif

  // Fast (true) control
  _igvn.replace_input_of(head->skip_strip_mined(), LoopNode::EntryControl, iffast);
  // Slow (false) control
  _igvn.replace_input_of(slow_head->skip_strip_mined(), LoopNode::EntryControl, ifslow);

  recompute_dom_depth();

  lk->set_iff(iff);

#ifndef PRODUCT
  if (TraceLoopOpts ) {
    tty->print("\t after  replace_input_of: head = %d, ", head->_idx); head->dump();
    tty->print("\t after  replace_input_of: slow_head = %d, ", slow_head->_idx); slow_head->dump();
  }
#endif

  return slow_head->as_Loop();
}

CountedLoopReserveKit::CountedLoopReserveKit(PhaseIdealLoop* phase, IdealLoopTree *loop, bool active = true) :
  _phase(phase),
  _lpt(loop),
  _lp(NULL),
  _iff(NULL),
  _lp_reserved(NULL),
  _has_reserved(false),
  _use_new(false),
  _active(active)
  {
    create_reserve();
  };

CountedLoopReserveKit::~CountedLoopReserveKit() {
  if (!_active) {
    return;
  }

  if (_has_reserved && !_use_new) {
    // intcon(0)->iff-node reverts CF to the reserved copy
    ConINode* const_0 = _phase->_igvn.intcon(0);
    _phase->set_ctrl(const_0, _phase->C->root());
    _iff->set_req(1, const_0);

    #ifndef PRODUCT
      if (TraceLoopOpts) {
        tty->print_cr("CountedLoopReserveKit::~CountedLoopReserveKit()");
        tty->print("\t discard loop %d and revert to the reserved loop clone %d: ", _lp->_idx, _lp_reserved->_idx);
        _lp_reserved->dump();
      }
    #endif
  }
}

bool CountedLoopReserveKit::create_reserve() {
  if (!_active) {
    return false;
  }

  if(!_lpt->_head->is_CountedLoop()) {
    if (TraceLoopOpts) {
      tty->print_cr("CountedLoopReserveKit::create_reserve: %d not counted loop", _lpt->_head->_idx);
    }
    return false;
  }
  CountedLoopNode *cl = _lpt->_head->as_CountedLoop();
  if (!cl->is_valid_counted_loop()) {
    if (TraceLoopOpts) {
      tty->print_cr("CountedLoopReserveKit::create_reserve: %d not valid counted loop", cl->_idx);
    }
    return false; // skip malformed counted loop
  }
  if (!cl->is_main_loop()) {
    bool loop_not_canonical = true;
    if (cl->is_post_loop() && (cl->slp_max_unroll() > 0)) {
      loop_not_canonical = false;
    }
    // only reject some loop forms
    if (loop_not_canonical) {
      if (TraceLoopOpts) {
        tty->print_cr("CountedLoopReserveKit::create_reserve: %d not canonical loop", cl->_idx);
      }
      return false; // skip normal, pre, and post (conditionally) loops
    }
  }

  _lp = _lpt->_head->as_Loop();
  _lp_reserved = _phase->create_reserve_version_of_loop(_lpt, this);

  if (!_lp_reserved->is_CountedLoop()) {
    return false;
  }

  Node* ifslow_pred = _lp_reserved->skip_strip_mined()->in(LoopNode::EntryControl);

  if (!ifslow_pred->is_IfFalse()) {
    return false;
  }

  Node* iff = ifslow_pred->in(0);
  if (!iff->is_If() || iff != _iff) {
    return false;
  }

  if (iff->in(1)->Opcode() != Op_ConI) {
    return false;
  }

  return _has_reserved = true;
}
