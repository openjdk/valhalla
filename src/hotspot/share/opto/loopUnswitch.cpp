/*
 * Copyright (c) 2006, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/predicates.hpp"
#include "opto/rootnode.hpp"

//================= Loop Unswitching =====================
//
// orig:                       transformed:
//                               if (invariant-test) then
//  predicates                     predicates
//  loop                           loop
//    stmt1                          stmt1
//    if (invariant-test) then       stmt2
//      stmt2                        stmt4
//    else                         endloop
//      stmt3                    else
//    endif                        predicates [clone]
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

  if (head->is_flat_arrays()) {
    return false;
  }

  Node_List unswitch_iffs;
  if (phase->find_unswitching_candidate(this, unswitch_iffs) == nullptr) {
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
  IfNode* unswitch_iff = nullptr;
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
  if (unswitch_iff != nullptr) {
    unswitch_iffs.push(unswitch_iff);
  }

  // Collect all non-flat array checks for unswitching to create a fast loop
  // without checks (only non-flat array accesses) and a slow loop with checks.
  if (unswitch_iff == nullptr || unswitch_iff->is_flat_array_check(&_igvn)) {
    for (uint i = 0; i < loop->_body.size(); i++) {
      IfNode* n = loop->_body.at(i)->isa_If();
      if (n != nullptr && n != unswitch_iff && n->is_flat_array_check(&_igvn) &&
          loop->is_invariant(n->in(1)) && !loop->is_loop_exit(n)) {
        unswitch_iffs.push(n);
        if (unswitch_iff == nullptr) {
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
  LoopNode* head = loop->_head->as_Loop();
  if (has_control_dependencies_from_predicates(head)) {
    return;
  }

  // Find first invariant test that doesn't exit the loop
  Node_List unswitch_iffs;
  IfNode* unswitch_iff = find_unswitching_candidate((const IdealLoopTree *)loop, unswitch_iffs);
  assert(unswitch_iff != nullptr && unswitch_iff == unswitch_iffs.at(0), "should be at least one");
  bool flat_array_checks = unswitch_iffs.size() > 1;

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

  C->print_method(PHASE_BEFORE_LOOP_UNSWITCHING, 4, head);

  // Need to revert back to normal loop
  if (head->is_CountedLoop() && !head->as_CountedLoop()->is_normal_loop()) {
    head->as_CountedLoop()->set_normal_loop();
  }

  IfNode* invar_iff = create_slow_version_of_loop(loop, old_new, unswitch_iffs, CloneIncludesStripMined);
  ProjNode* proj_true = invar_iff->proj_out(1);
  verify_fast_loop(head, proj_true);

  // Increment unswitch count
  LoopNode* head_clone = old_new[head->_idx]->as_Loop();
  int nct = head->unswitch_count() + 1;
  head->set_unswitch_count(nct);
  head_clone->set_unswitch_count(nct);

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
        if (!flat_array_checks) {
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
    dominated_by(proj_true->as_IfProj(), iff);
  }
  IfNode* unswitch_iff_clone = old_new[unswitch_iff->_idx]->as_If();
  if (!flat_array_checks) {
    ProjNode* proj_false = invar_iff->proj_out(0)->as_Proj();
    _igvn.rehash_node_delayed(unswitch_iff_clone);
    dominated_by(proj_false->as_IfProj(), unswitch_iff_clone);
  } else {
    // Leave the flat array checks in the slow loop and
    // prevent it from being unswitched again based on these checks.
    head_clone->mark_flat_arrays();
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

  C->print_method(PHASE_AFTER_LOOP_UNSWITCHING, 4, head_clone);

  C->set_major_progress();
}

bool PhaseIdealLoop::has_control_dependencies_from_predicates(LoopNode* head) const {
  Node* entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  Predicates predicates(entry);
  if (predicates.has_any()) {
    assert(entry->is_IfProj(), "sanity - must be ifProj since there is at least one predicate");
    if (entry->outcnt() > 1) {
      // Bailout if there are predicates from which there are additional control dependencies (i.e. from loop
      // entry 'entry') to previously partially peeled statements since this case is not handled and can lead
      // to a wrong execution. Remove this bailout, once this is fixed.
      return true;
    }
  }
  return false;
}

//-------------------------create_slow_version_of_loop------------------------
// Create a slow version of the loop by cloning the loop
// and inserting an if to select fast-slow versions.
// Return the inserted if.
IfNode* PhaseIdealLoop::create_slow_version_of_loop(IdealLoopTree *loop,
                                                    Node_List &old_new,
                                                    Node_List &unswitch_iffs,
                                                    CloneLoopMode mode) {
  LoopNode* head  = loop->_head->as_Loop();
  bool counted_loop = head->is_CountedLoop();
  Node*     entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  _igvn.rehash_node_delayed(entry);
  IdealLoopTree* outer_loop = loop->_parent;

  head->verify_strip_mined(1);

  // Add test to new "if" outside of loop
  IfNode* unswitch_iff = unswitch_iffs.at(0)->as_If();
  BoolNode* bol = unswitch_iff->in(1)->as_Bool();
  if (unswitch_iffs.size() > 1) {
    // Flat array checks are used on array access to switch between
    // a legacy object array access and a flat inline type array
    // access. We want the performance impact on legacy accesses to be
    // as small as possible so we make two copies of the loop: a fast
    // one where all accesses are known to be legacy, a slow one where
    // some accesses are to flat arrays. Flat array checks
    // can be removed from the fast loop (true proj) but not from the
    // slow loop (false proj) as it can have a mix of flat/legacy accesses.
    assert(bol->_test._test == BoolTest::ne, "IfTrue proj must point to flat array");
    bol = bol->clone()->as_Bool();
    register_new_node(bol, entry);
    FlatArrayCheckNode* cmp = bol->in(1)->clone()->as_FlatArrayCheck();
    register_new_node(cmp, entry);
    bol->set_req(1, cmp);
    // Combine all checks into a single one that fails if one array is a flat array
    assert(cmp->req() == 3, "unexpected number of inputs for FlatArrayCheck");
    cmp->add_req_batch(C->top(), unswitch_iffs.size() - 1);
    for (uint i = 0; i < unswitch_iffs.size(); i++) {
      Node* array = unswitch_iffs.at(i)->in(1)->in(1)->in(FlatArrayCheckNode::ArrayOrKlass);
      cmp->set_req(FlatArrayCheckNode::ArrayOrKlass + i, array);
    }
  }

  IfNode* iff = (unswitch_iff->Opcode() == Op_RangeCheck) ? new RangeCheckNode(entry, bol, unswitch_iff->_prob, unswitch_iff->_fcnt) :
      new IfNode(entry, bol, unswitch_iff->_prob, unswitch_iff->_fcnt);
  register_node(iff, outer_loop, entry, dom_depth(entry));
  IfProjNode* iffast = new IfTrueNode(iff);
  register_node(iffast, outer_loop, iff, dom_depth(iff));
  IfProjNode* ifslow = new IfFalseNode(iff);
  register_node(ifslow, outer_loop, iff, dom_depth(iff));

  // Clone the loop body.  The clone becomes the slow loop.  The
  // original pre-header will (illegally) have 3 control users
  // (old & new loops & new if).
  clone_loop(loop, old_new, dom_depth(head->skip_strip_mined()), mode, iff);
  assert(old_new[head->_idx]->is_Loop(), "" );

  // Fast (true) and Slow (false) control
  IfProjNode* iffast_pred = iffast;
  IfProjNode* ifslow_pred = ifslow;
  clone_parse_and_assertion_predicates_to_unswitched_loop(loop, old_new, iffast_pred, ifslow_pred);

  Node* l = head->skip_strip_mined();
  _igvn.replace_input_of(l, LoopNode::EntryControl, iffast_pred);
  set_idom(l, iffast_pred, dom_depth(l));
  LoopNode* slow_l = old_new[head->_idx]->as_Loop()->skip_strip_mined();
  _igvn.replace_input_of(slow_l, LoopNode::EntryControl, ifslow_pred);
  set_idom(slow_l, ifslow_pred, dom_depth(l));

  recompute_dom_depth();

  return iff;
}

#ifdef ASSERT
void PhaseIdealLoop::verify_fast_loop(LoopNode* head, const ProjNode* proj_true) const {
  assert(proj_true->is_IfTrue(), "must be true projection");
  Node* entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  Predicates predicates(entry);
  if (!predicates.has_any()) {
    // No Parse Predicate.
    Node* uniqc = proj_true->unique_ctrl_out();
    assert((uniqc == head && !head->is_strip_mined()) || (uniqc == head->in(LoopNode::EntryControl)
                                                          && head->is_strip_mined()), "must hold by construction if no predicates");
  } else {
    // There is at least one Parse Predicate. When skipping all predicates/predicate blocks, we should end up
    // at 'proj_true'.
    assert(proj_true == predicates.entry(), "must hold by construction if at least one Parse Predicate");
  }
}
#endif // ASSERT

