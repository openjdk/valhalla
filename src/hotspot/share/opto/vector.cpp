/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciSymbols.hpp"
#include "gc/shared/barrierSet.hpp"
#include "opto/castnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/phaseX.hpp"
#include "opto/rootnode.hpp"
#include "opto/vector.hpp"
#include "utilities/macros.hpp"

void PhaseVector::optimize_vector_boxes() {
  Compile::TracePhase tp("vector_elimination", &timers[_t_vector_elimination]);

  // Signal GraphKit it's post-parse phase.
  assert(C->inlining_incrementally() == false, "sanity");
  C->set_inlining_incrementally(true);

  C->for_igvn()->clear();
  C->initial_gvn()->replace_with(&_igvn);

  expand_vunbox_nodes();
  scalarize_vbox_nodes();

  C->inline_vector_reboxing_calls();

  expand_vbox_nodes();
  eliminate_vbox_alloc_nodes();

  C->set_inlining_incrementally(false);

  do_cleanup();
}

void PhaseVector::do_cleanup() {
  if (C->failing())  return;
  {
    Compile::TracePhase tp("vector_pru", &timers[_t_vector_pru]);
    ResourceMark rm;
    PhaseRemoveUseless pru(C->initial_gvn(), C->for_igvn());
    if (C->failing())  return;
  }
  {
    Compile::TracePhase tp("incrementalInline_igvn", &timers[_t_vector_igvn]);
    _igvn = PhaseIterGVN(C->initial_gvn());
    _igvn.optimize();
    if (C->failing())  return;
  }
  C->print_method(PHASE_ITER_GVN_BEFORE_EA, 3);
}

void PhaseVector::scalarize_vbox_nodes() {
  if (C->failing())  return;

  if (!EnableVectorReboxing) {
    return; // don't scalarize vector boxes
  }

  int macro_idx = C->macro_count() - 1;
  while (macro_idx >= 0) {
    Node * n = C->macro_node(macro_idx);
    assert(n->is_macro(), "only macro nodes expected here");
    if (n->Opcode() == Op_VectorBox) {
      VectorBoxNode* vbox = static_cast<VectorBoxNode*>(n);
      scalarize_vbox_node(vbox);
      if (C->failing())  return;
      C->print_method(PHASE_SCALARIZE_VBOX, 3, vbox);
    }
    if (C->failing())  return;
    macro_idx = MIN2(macro_idx - 1, C->macro_count() - 1);
  }
}

void PhaseVector::expand_vbox_nodes() {
  if (C->failing())  return;

  int macro_idx = C->macro_count() - 1;
  while (macro_idx >= 0) {
    Node * n = C->macro_node(macro_idx);
    assert(n->is_macro(), "only macro nodes expected here");
    if (n->Opcode() == Op_VectorBox) {
      VectorBoxNode* vbox = static_cast<VectorBoxNode*>(n);
      expand_vbox_node(vbox);
      if (C->failing())  return;
    }
    if (C->failing())  return;
    macro_idx = MIN2(macro_idx - 1, C->macro_count() - 1);
  }
}

void PhaseVector::expand_vunbox_nodes() {
  if (C->failing())  return;

  int macro_idx = C->macro_count() - 1;
  while (macro_idx >= 0) {
    Node * n = C->macro_node(macro_idx);
    assert(n->is_macro(), "only macro nodes expected here");
    if (n->Opcode() == Op_VectorUnbox) {
      VectorUnboxNode* vec_unbox = static_cast<VectorUnboxNode*>(n);
      expand_vunbox_node(vec_unbox);
      if (C->failing())  return;
      C->print_method(PHASE_EXPAND_VUNBOX, 3, vec_unbox);
    }
    if (C->failing())  return;
    macro_idx = MIN2(macro_idx - 1, C->macro_count() - 1);
  }
}

void PhaseVector::eliminate_vbox_alloc_nodes() {
  if (C->failing())  return;

  int macro_idx = C->macro_count() - 1;
  while (macro_idx >= 0) {
    Node * n = C->macro_node(macro_idx);
    assert(n->is_macro(), "only macro nodes expected here");
    if (n->Opcode() == Op_VectorBoxAllocate) {
      VectorBoxAllocateNode* vbox_alloc = static_cast<VectorBoxAllocateNode*>(n);
      eliminate_vbox_alloc_node(vbox_alloc);
      if (C->failing())  return;
      C->print_method(PHASE_ELIMINATE_VBOX_ALLOC, 3, vbox_alloc);
    }
    if (C->failing())  return;
    macro_idx = MIN2(macro_idx - 1, C->macro_count() - 1);
  }
}

static JVMState* clone_jvms(Compile* C, SafePointNode* sfpt) {
  JVMState* new_jvms = sfpt->jvms()->clone_shallow(C);
  uint size = sfpt->req();
  SafePointNode* map = new SafePointNode(size, new_jvms);
  for (uint i = 0; i < size; i++) {
    map->init_req(i, sfpt->in(i));
  }
  Node* mem = map->memory();
  if (!mem->is_MergeMem()) {
    // Since we are not in parsing, the SafePointNode does not guarantee that the memory
    // input is necessarily a MergeMemNode. But we need to ensure that there is that
    // MergeMemNode, since the GraphKit assumes the memory input of the map to be a
    // MergeMemNode, so that it can directly access the memory slices.
    PhaseGVN& gvn = *C->initial_gvn();
    Node* mergemem = MergeMemNode::make(mem);
    gvn.set_type_bottom(mergemem);
    map->set_memory(mergemem);
  }
  new_jvms->set_map(map);
  return new_jvms;
}

void PhaseVector::scalarize_vbox_node(VectorBoxNode* vec_box) {
  Node* vec_value = vec_box->get_vec();
  PhaseGVN& gvn = *C->initial_gvn();

  // Process merged VBAs

  if (EnableVectorAggressiveReboxing) {
    Unique_Node_List calls(C->comp_arena());
    for (DUIterator_Fast imax, i = vec_box->fast_outs(imax); i < imax; i++) {
      Node* use = vec_box->fast_out(i);
      if (use->is_CallJava()) {
        CallJavaNode* call = use->as_CallJava();
        if (call->has_non_debug_use(vec_box) && vec_box->get_oop()->is_Phi()) {
          calls.push(call);
        }
      }
    }

    while (calls.size() > 0) {
      CallJavaNode* call = calls.pop()->as_CallJava();
      // Attach new VBA to the call and use it instead of Phi (VBA ... VBA).

      JVMState* jvms = clone_jvms(C, call);
      GraphKit kit(jvms);
      PhaseGVN& gvn = kit.gvn();

      // Adjust JVMS from post-call to pre-call state: put args on stack
      uint nargs = call->method()->arg_size();
      kit.ensure_stack(kit.sp() + nargs);
      for (uint i = TypeFunc::Parms; i < call->tf()->domain_sig()->cnt(); i++) {
        kit.push(call->in(i));
      }
      jvms = kit.sync_jvms();

      Node* new_vbox = nullptr;
      {
        Node* vect = vec_box->get_vec();
        const TypeInstPtr* vbox_type = vec_box->box_type();
        const TypeVect* vt = vec_box->vec_type();
        BasicType elem_bt = vt->element_basic_type();
        int num_elem = vt->length();

        new_vbox = kit.box_vector(vect, vbox_type, elem_bt, num_elem, /*deoptimize=*/true);

        kit.replace_in_map(vec_box, new_vbox);
      }

      kit.dec_sp(nargs);
      jvms = kit.sync_jvms();

      call->set_req(TypeFunc::Control , kit.control());
      call->set_req(TypeFunc::I_O     , kit.i_o());
      call->set_req(TypeFunc::Memory  , kit.reset_memory());
      call->set_req(TypeFunc::FramePtr, kit.frameptr());
      call->replace_edge(vec_box, new_vbox);

      C->record_for_igvn(call);
    }
  }
}

void PhaseVector::expand_vbox_node(VectorBoxNode* vec_box) {
  if (vec_box->outcnt() > 0) {
    VectorSet visited;
    Node* vbox = vec_box->get_oop();
    Node* vect = vec_box->get_vec();

    Node* result = expand_vbox_node_helper(vbox, vect, vec_box->box_type(),
                                           vec_box->vec_type(), visited);

    C->gvn_replace_by(vec_box, result);
    C->print_method(PHASE_EXPAND_VBOX, 3, vec_box);
  }
  C->remove_macro_node(vec_box);
}

Node* PhaseVector::expand_vbox_node_helper(Node* vbox,
                                           Node* vect,
                                           const TypeInstPtr* box_type,
                                           const TypeVect* vect_type,
                                           VectorSet &visited) {
  // JDK-8304948 shows an example that there may be a cycle in the graph.
  if (visited.test_set(vbox->_idx)) {
    assert(vbox->is_Phi(), "should be phi");
    return vbox; // already visited
  }

  // Handle the case when the allocation input to VectorBoxNode is a Proj.
  // This is the normal case before expanding.
  if (vbox->is_Proj() && vbox->in(0)->Opcode() == Op_VectorBoxAllocate) {
    VectorBoxAllocateNode* vbox_alloc = static_cast<VectorBoxAllocateNode*>(vbox->in(0));
    return expand_vbox_alloc_node(vbox_alloc, vect, box_type, vect_type);
  }

  // Handle the case when both the allocation input and vector input to
  // VectorBoxNode are Phi. This case is generated after the transformation of
  // Phi: Phi (VectorBox1 VectorBox2) => VectorBox (Phi1 Phi2).
  // With this optimization, the relative two allocation inputs of VectorBox1 and
  // VectorBox2 are gathered into Phi1 now. Similarly, the original vector
  // inputs of two VectorBox nodes are in Phi2.
  //
  // See PhiNode::merge_through_phi in cfg.cpp for more details.
  if (vbox->is_Phi() && vect->is_Phi()) {
    assert(vbox->as_Phi()->region() == vect->as_Phi()->region(), "");
    for (uint i = 1; i < vbox->req(); i++) {
      Node* new_box = expand_vbox_node_helper(vbox->in(i), vect->in(i),
                                              box_type, vect_type, visited);
      if (!new_box->is_Phi()) {
        C->initial_gvn()->hash_delete(vbox);
        vbox->set_req(i, new_box);
      }
    }
    return C->initial_gvn()->transform(vbox);
  }

  // Handle the case when the allocation input to VectorBoxNode is a phi
  // but the vector input is not, which can definitely be the case if the
  // vector input has been value-numbered. It seems to be safe to do by
  // construction because VectorBoxNode and VectorBoxAllocate come in a
  // specific order as a result of expanding an intrinsic call. After that, if
  // any of the inputs to VectorBoxNode are value-numbered they can only
  // move up and are guaranteed to dominate.
  if (vbox->is_Phi() && (vect->is_Vector() || vect->is_LoadVector())) {
    for (uint i = 1; i < vbox->req(); i++) {
      Node* new_box = expand_vbox_node_helper(vbox->in(i), vect,
                                              box_type, vect_type, visited);
      if (!new_box->is_Phi()) {
        C->initial_gvn()->hash_delete(vbox);
        vbox->set_req(i, new_box);
      }
    }
    return C->initial_gvn()->transform(vbox);
  }

  assert(!vbox->is_Phi(), "should be expanded");
  // TODO: assert that expanded vbox is initialized with the same value (vect).
  return vbox; // already expanded
}

Node* PhaseVector::expand_vbox_alloc_node(VectorBoxAllocateNode* vbox_alloc,
                                          Node* vect,
                                          const TypeInstPtr* box_type,
                                          const TypeVect* vect_type) {
  JVMState* jvms = clone_jvms(C, vbox_alloc);
  GraphKit kit(jvms);
  PhaseGVN& gvn = kit.gvn();

  ciInlineKlass* vk = static_cast<ciInlineKlass*>(box_type->inline_klass());

  // Re-generate an InlineTypeNode to represent the payload field. This is necessary
  // in case the input "vect" is not the original vector value when creating the
  // VectorBox (e.g. original vector value is a PhiNode).
  ciInlineKlass* payload = vk->declared_nonstatic_field_at(0)->type()->as_inline_klass();
  Node* payload_value = InlineTypeNode::make_uninitialized(gvn, payload, true);
  payload_value->as_InlineType()->set_field_value(0, vect);
  payload_value = gvn.transform(payload_value);

  // Re-generate an InlineTypeNode to represent the vector object. New a buffer
  // and save its field value to the buffer.
  InlineTypeNode* vector = InlineTypeNode::make_uninitialized(gvn, vk, false);
  vector->set_field_value(0, payload_value);
  vector = gvn.transform(vector)->as_InlineType();

  Node* klass_node = kit.makecon(TypeKlassPtr::make(vk));
  Node* alloc_oop  = kit.new_instance(klass_node, NULL, NULL, /* deoptimize_on_exception */ true);
  vector->store(&kit, alloc_oop, alloc_oop, vk);

  C->set_max_vector_size(MAX2(C->max_vector_size(), vect_type->length_in_bytes()));

  kit.replace_call(vbox_alloc, alloc_oop, true);
  C->remove_macro_node(vbox_alloc);
  return alloc_oop;
}

void PhaseVector::expand_vunbox_node(VectorUnboxNode* vec_unbox) {
  if (vec_unbox->outcnt() > 0) {
    GraphKit kit;
    PhaseGVN& gvn = kit.gvn();

    Node* node = vec_unbox->obj();
    while(node->is_InlineType()) {
      node = node->as_InlineType()->field_value(0);
    }

    assert(node->bottom_type()->isa_vect() != NULL, "not a vector");
    assert(Type::cmp(vec_unbox->bottom_type(), node->bottom_type()) == 0, "type is not matched");

    C->set_max_vector_size(MAX2(C->max_vector_size(), vec_unbox->bottom_type()->is_vect()->length_in_bytes()));
    gvn.hash_delete(vec_unbox);
    vec_unbox->disconnect_inputs(C);
    C->gvn_replace_by(vec_unbox, node);
  }
  C->remove_macro_node(vec_unbox);
}

void PhaseVector::eliminate_vbox_alloc_node(VectorBoxAllocateNode* vbox_alloc) {
  JVMState* jvms = clone_jvms(C, vbox_alloc);
  GraphKit kit(jvms);
  // Remove VBA, but leave a safepoint behind.
  // Otherwise, it may end up with a loop without any safepoint polls.
  kit.replace_call(vbox_alloc, kit.map(), true);
  C->remove_macro_node(vbox_alloc);
}
