/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
#include "prims/vectorSupport.hpp"

static bool is_vector(ciKlass* klass) {
  return VectorSupport::is_vector(klass);
}

static bool is_vector_mask(ciKlass* klass) {
  return VectorSupport::is_vector_mask(klass);
}

static bool is_vector_shuffle(ciKlass* klass) {
  return VectorSupport::is_vector_shuffle(klass);
}

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

      Node* new_vbox = NULL;
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

  ciInstanceKlass* iklass = vec_box->box_type()->instance_klass();
  // Multi-field based vectors are InlineTypeNodes and are already
  // scalarized by process_inline_types.
  if (is_vector(iklass)) {
    return;
  }

  // Process debug uses at safepoints
  Unique_Node_List safepoints(C->comp_arena());

  Unique_Node_List worklist(C->comp_arena());
  worklist.push(vec_box);
  while (worklist.size() > 0) {
    Node* n = worklist.pop();
    for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
      Node* use = n->fast_out(i);
      if (use->is_SafePoint()) {
        SafePointNode* sfpt = use->as_SafePoint();
        if (!sfpt->is_Call() || !sfpt->as_Call()->has_non_debug_use(n)) {
          safepoints.push(sfpt);
        }
      } else if (use->is_ConstraintCast()) {
        worklist.push(use); // reversed version of Node::uncast()
      }
    }
  }

  int n_fields = iklass->nof_nonstatic_fields();
  assert(n_fields == 1, "sanity");

  // If a mask is feeding into safepoint[s], then its value should be
  // packed into a boolean/byte vector first, this will simplify the
  // re-materialization logic for both predicated and non-predicated
  // targets.
  bool is_mask = is_vector_mask(iklass);
  if (is_mask && vec_value->Opcode() != Op_VectorStoreMask) {
    const TypeVect* vt = vec_value->bottom_type()->is_vect();
    BasicType bt = vt->element_basic_type();
    vec_value = gvn.transform(VectorStoreMaskNode::make(gvn, vec_value, bt, vt->length()));
  }

  while (safepoints.size() > 0) {
    SafePointNode* sfpt = safepoints.pop()->as_SafePoint();

    uint first_ind = (sfpt->req() - sfpt->jvms()->scloff());
    Node* sobj = new SafePointScalarObjectNode(vec_box->box_type(),
#ifdef ASSERT
                                               vec_box,
#endif // ASSERT
                                               first_ind, n_fields);
    sobj->init_req(0, C->root());
    sfpt->add_req(vec_value);

    sobj = gvn.transform(sobj);

    JVMState *jvms = sfpt->jvms();

    jvms->set_endoff(sfpt->req());
    // Now make a pass over the debug information replacing any references
    // to the allocated object with vector value.
    for (uint i = jvms->debug_start(); i < jvms->debug_end(); i++) {
      Node* debug = sfpt->in(i);
      if (debug != NULL && debug->uncast(/*keep_deps*/false) == vec_box) {
        sfpt->set_req(i, sobj);
      }
    }
    C->record_for_igvn(sfpt);
  }
}

void PhaseVector::expand_vbox_node(VectorBoxNode* vec_box) {
  if (vec_box->outcnt() > 0) {
    Node* vbox = vec_box->get_oop();
    Node* vect = vec_box->get_vec();
    Node* result = expand_vbox_node_helper(vec_box, vbox, vect, vec_box->box_type(), vec_box->vec_type());
    C->gvn_replace_by(vec_box, result);
    C->print_method(PHASE_EXPAND_VBOX, 3, vec_box);
  }
  C->remove_macro_node(vec_box);
}

Node* PhaseVector::expand_vbox_node_helper(Node* vec_box,
                                           Node* vbox,
                                           Node* vect,
                                           const TypeInstPtr* box_type,
                                           const TypeVect* vect_type) {
  if (vbox->is_Phi() && vect->is_Phi()) {
    assert(vbox->as_Phi()->region() == vect->as_Phi()->region(), "");
    Node* new_phi = new PhiNode(vbox->as_Phi()->region(), box_type);
    for (uint i = 1; i < vbox->req(); i++) {
      Node* new_box = expand_vbox_node_helper(vec_box, vbox->in(i), vect->in(i), box_type, vect_type);
      new_phi->set_req(i, new_box);
    }
    new_phi = C->initial_gvn()->transform(new_phi);
    return new_phi;
  } else if (vbox->is_Phi() && (vect->is_Vector() || vect->is_LoadVector())) {
    // Handle the case when the allocation input to VectorBoxNode is a phi
    // but the vector input is not, which can definitely be the case if the
    // vector input has been value-numbered. It seems to be safe to do by
    // construction because VectorBoxNode and VectorBoxAllocate come in a
    // specific order as a result of expanding an intrinsic call. After that, if
    // any of the inputs to VectorBoxNode are value-numbered they can only
    // move up and are guaranteed to dominate.
    Node* new_phi = new PhiNode(vbox->as_Phi()->region(), box_type);
    for (uint i = 1; i < vbox->req(); i++) {
      Node* new_box = expand_vbox_node_helper(vec_box, vbox->in(i), vect, box_type, vect_type);
      new_phi->set_req(i, new_box);
    }
    new_phi = C->initial_gvn()->transform(new_phi);
    return new_phi;
  } else if (vbox->is_Proj() && vbox->in(0)->Opcode() == Op_VectorBoxAllocate) {
    VectorBoxAllocateNode* vbox_alloc = static_cast<VectorBoxAllocateNode*>(vbox->in(0));
    return expand_vbox_alloc_node(vec_box, vbox_alloc, vect, box_type, vect_type);
  } else {
    assert(!vbox->is_Phi(), "");
    // TODO: assert that expanded vbox is initialized with the same value (vect).
    return vbox; // already expanded
  }
}

Node* PhaseVector::expand_vbox_alloc_node_mf(Node* vbox,
                                             VectorBoxAllocateNode* vbox_alloc,
                                             Node* value,
                                             const TypeInstPtr* box_type,
                                             const TypeVect* vect_type) {
  assert(vbox->isa_InlineType(), "");
  JVMState* jvms = clone_jvms(C, vbox_alloc);
  GraphKit kit(jvms);
  PhaseGVN& gvn = kit.gvn();

  ciInstanceKlass* box_klass = box_type->instance_klass();
  BasicType bt = vect_type->element_basic_type();
  int num_elem = vect_type->length();
  int elem_size = type2aelembytes(bt);

  const TypeKlassPtr* klass_type = box_type->as_klass_type();
  Node* klass_node = kit.makecon(klass_type);
  Node* buffer_mem = kit.new_instance(klass_node, NULL, NULL, /* deoptimize_on_exception */ true, vbox->as_InlineType());
  // TODO: Re-use existing value storage routine from InlineTypeNode.
  //vbox->as_InlineType()->store(&kit, buffer, buffer, box_klass);

  // Store the vector value into the array.
  // (The store should be captured by InitializeNode and turned into initialized store later.)
  ciSymbol* payload_sig = ciSymbol::make(VectorSupport::get_vector_payload_field_signature(bt, num_elem)->as_C_string());
  ciSymbol* payload_name = ciSymbol::make(vmSymbols::payload_name()->as_C_string());
  ciField* payload = box_klass->get_field_by_name(payload_name, payload_sig, false);

  Node* buffer_start_adr = kit.basic_plus_adr(buffer_mem, payload->offset());
  const TypePtr* buffer_adr_type = buffer_start_adr->bottom_type()->is_ptr();
  Node* buffer_mem_start = kit.memory(buffer_start_adr);
  Node* vstore = gvn.transform(StoreVectorNode::make(0,
                                                     kit.control(),
                                                     buffer_mem_start,
                                                     buffer_start_adr,
                                                     buffer_adr_type,
                                                     value,
                                                     num_elem));
  // TODO: With respect to aliasing behaviour multi-field alias type should be same as that of
  // array, since multi-field is a bundle of scalars. An alias type determines the size of
  // memory slice updated in global memory at a particular alias index, subsequent memory read
  // with same alias type can directly fetch the value thus saving an extra load operation.
  kit.set_memory(vstore, buffer_adr_type);

  C->set_max_vector_size(MAX2(C->max_vector_size(), vect_type->length_in_bytes()));

  kit.replace_call(vbox_alloc, buffer_mem, true);
  C->remove_macro_node(vbox_alloc);

  return buffer_mem;
}


Node* PhaseVector::expand_vbox_alloc_node(Node* vbox,
                                          VectorBoxAllocateNode* vbox_alloc,
                                          Node* value,
                                          const TypeInstPtr* box_type,
                                          const TypeVect* vect_type) {
  ciInstanceKlass* box_klass = box_type->instance_klass();
  if (is_vector(box_klass)) {
    return expand_vbox_alloc_node_mf(vbox, vbox_alloc, value, box_type, vect_type);
  }

  JVMState* jvms = clone_jvms(C, vbox_alloc);
  GraphKit kit(jvms);
  PhaseGVN& gvn = kit.gvn();

  BasicType bt = vect_type->element_basic_type();
  int num_elem = vect_type->length();

  bool is_mask = is_vector_mask(box_klass);
  // If boxed mask value is present in a predicate register, it must be
  // spilled to a vector though a VectorStoreMaskOperation before actual StoreVector
  // operation to vector payload field.
  if (is_mask && (value->bottom_type()->isa_vectmask() || bt != T_BOOLEAN)) {
    value = gvn.transform(VectorStoreMaskNode::make(gvn, value, bt, num_elem));
    // Although type of mask depends on its definition, in terms of storage everything is stored in boolean array.
    bt = T_BOOLEAN;
    assert(value->bottom_type()->is_vect()->element_basic_type() == bt,
           "must be consistent with mask representation");
  }

  // Generate array allocation for the field which holds the values.
  const TypeKlassPtr* array_klass = TypeKlassPtr::make(ciTypeArrayKlass::make(bt));
  Node* arr = kit.new_array(kit.makecon(array_klass), kit.intcon(num_elem), 1);

  // Store the vector value into the array.
  // (The store should be captured by InitializeNode and turned into initialized store later.)
  Node* arr_adr = kit.array_element_address(arr, kit.intcon(0), bt);
  const TypePtr* arr_adr_type = arr_adr->bottom_type()->is_ptr();
  Node* arr_mem = kit.memory(arr_adr);
  Node* vstore = gvn.transform(StoreVectorNode::make(0,
                                                     kit.control(),
                                                     arr_mem,
                                                     arr_adr,
                                                     arr_adr_type,
                                                     value,
                                                     num_elem));
  kit.set_memory(vstore, arr_adr_type);

  C->set_max_vector_size(MAX2(C->max_vector_size(), vect_type->length_in_bytes()));

  // Generate the allocate for the Vector object.
  const TypeKlassPtr* klass_type = box_type->as_klass_type();
  Node* klass_node = kit.makecon(klass_type);
  Node* vec_obj = kit.new_instance(klass_node);

  // Store the allocated array into object.
  ciField* field = ciEnv::current()->vector_VectorPayload_klass()->get_field_by_name(ciSymbols::payload_name(),
                                                                                     ciSymbols::object_signature(),
                                                                                     false);
  assert(field != NULL, "");
  Node* vec_field = kit.basic_plus_adr(vec_obj, field->offset_in_bytes());
  const TypePtr* vec_adr_type = vec_field->bottom_type()->is_ptr();

  // The store should be captured by InitializeNode and turned into initialized store later.
  Node* field_store = gvn.transform(kit.access_store_at(vec_obj,
                                                        vec_field,
                                                        vec_adr_type,
                                                        arr,
                                                        TypeOopPtr::make_from_klass(field->type()->as_klass()),
                                                        T_OBJECT,
                                                        IN_HEAP));
  kit.set_memory(field_store, vec_adr_type);

  kit.replace_call(vbox_alloc, vec_obj, true);
  C->remove_macro_node(vbox_alloc);

  return vec_obj;
}

Node* PhaseVector::get_loaded_payload(VectorUnboxNode* vec_unbox) {
   Node* obj = vec_unbox->obj();
   while(obj->is_InlineType()) {
      obj = obj->as_InlineType()->field_value(0);
   }
   if (obj->bottom_type()->isa_vect()) {
     return obj;
   }
   return NULL;
}

void PhaseVector::expand_vunbox_node_mf(VectorUnboxNode* vec_unbox) {
  if (vec_unbox->outcnt() > 0) {
    GraphKit kit;
    PhaseGVN& gvn = kit.gvn();

    Node* obj = vec_unbox->obj();
    const TypeInstPtr* tinst = gvn.type(obj)->isa_instptr();
    ciInstanceKlass* from_kls = tinst->instance_klass();
    const TypeVect* vt = vec_unbox->bottom_type()->is_vect();
    BasicType bt = vt->element_basic_type();
    BasicType masktype = bt;
    int num_elem = vt->length();

    int elem_size = type2aelembytes(bt);
    Node* vec_val_load = get_loaded_payload(vec_unbox);
    if (vec_val_load == NULL) {
      assert(obj->isa_InlineType(), "");
      ciSymbol* payload_sig = ciSymbol::make(VectorSupport::get_vector_payload_field_signature(bt, num_elem)->as_C_string());
      ciSymbol* payload_name = ciSymbol::make(vmSymbols::payload_name()->as_C_string());
      ciField* payload = from_kls->get_field_by_name(payload_name, payload_sig, false);

      Node* mem = vec_unbox->mem();
      Node* ctrl = vec_unbox->in(0);
      Node* vec_adr = gvn.transform(kit.basic_plus_adr(obj, payload->offset()));

      const TypePtr *adr_type = gvn.type(vec_adr)->isa_ptr();

      int num_elem = vt->length();
      vec_val_load = LoadVectorNode::make(0,
                                          ctrl,
                                          mem,
                                          vec_adr,
                                          adr_type,
                                          num_elem,
                                          bt);
      vec_val_load = gvn.transform(vec_val_load);
    }

    C->set_max_vector_size(MAX2(C->max_vector_size(), vt->length_in_bytes()));

    gvn.hash_delete(vec_unbox);
    vec_unbox->disconnect_inputs(C);
    C->gvn_replace_by(vec_unbox, vec_val_load);
  }
  C->remove_macro_node(vec_unbox);
}

void PhaseVector::expand_vunbox_node(VectorUnboxNode* vec_unbox) {
  if (vec_unbox->outcnt() > 0) {
    GraphKit kit;
    PhaseGVN& gvn = kit.gvn();

    Node* obj = vec_unbox->obj();
    const TypeInstPtr* tinst = gvn.type(obj)->isa_instptr();
    ciInstanceKlass* from_kls = tinst->instance_klass();
    const TypeVect* vt = vec_unbox->bottom_type()->is_vect();
    BasicType bt = vt->element_basic_type();
    BasicType masktype = bt;

    if (is_vector(from_kls)) {
      return expand_vunbox_node_mf(vec_unbox);
    }

    if (is_vector_mask(from_kls)) {
      bt = T_BOOLEAN;
    } else if (is_vector_shuffle(from_kls)) {
      bt = T_BYTE;
    }

    ciField* field = ciEnv::current()->vector_VectorPayload_klass()->get_field_by_name(ciSymbols::payload_name(),
                                                                                       ciSymbols::object_signature(),
                                                                                       false);
    assert(field != NULL, "");
    int offset = field->offset_in_bytes();
    Node* vec_adr = kit.basic_plus_adr(obj, offset);

    Node* mem = vec_unbox->mem();
    Node* ctrl = vec_unbox->in(0);
    Node* vec_field_ld;
    {
      DecoratorSet decorators = MO_UNORDERED | IN_HEAP;
      C2AccessValuePtr addr(vec_adr, vec_adr->bottom_type()->is_ptr());
      MergeMemNode* local_mem = MergeMemNode::make(mem);
      gvn.record_for_igvn(local_mem);
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      C2OptAccess access(gvn, ctrl, local_mem, decorators, T_OBJECT, obj, addr);
      const Type* type = TypeOopPtr::make_from_klass(field->type()->as_klass());
      vec_field_ld = bs->load_at(access, type);
    }

    // For proper aliasing, attach concrete payload type.
    ciKlass* payload_klass = ciTypeArrayKlass::make(bt);
    const Type* payload_type = TypeAryPtr::make_from_klass(payload_klass)->cast_to_ptr_type(TypePtr::NotNull);
    vec_field_ld = gvn.transform(new CastPPNode(vec_field_ld, payload_type));

    Node* adr = kit.array_element_address(vec_field_ld, gvn.intcon(0), bt);
    const TypePtr* adr_type = adr->bottom_type()->is_ptr();
    int num_elem = vt->length();
    Node* vec_val_load = LoadVectorNode::make(0,
                                              ctrl,
                                              mem,
                                              adr,
                                              adr_type,
                                              num_elem,
                                              bt);
    vec_val_load = gvn.transform(vec_val_load);

    C->set_max_vector_size(MAX2(C->max_vector_size(), vt->length_in_bytes()));

    if (is_vector_mask(from_kls)) {
      vec_val_load = gvn.transform(new VectorLoadMaskNode(vec_val_load, TypeVect::makemask(masktype, num_elem)));
    } else if (is_vector_shuffle(from_kls) && !vec_unbox->is_shuffle_to_vector()) {
      assert(vec_unbox->bottom_type()->is_vect()->element_basic_type() == masktype, "expect shuffle type consistency");
      vec_val_load = gvn.transform(new VectorLoadShuffleNode(vec_val_load, TypeVect::make(masktype, num_elem)));
    }

    gvn.hash_delete(vec_unbox);
    vec_unbox->disconnect_inputs(C);
    C->gvn_replace_by(vec_unbox, vec_val_load);
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
