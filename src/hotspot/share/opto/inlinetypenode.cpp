/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciInlineKlass.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/gc_globals.hpp"
#include "opto/addnode.hpp"
#include "opto/castnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/inlinetypenode.hpp"
#include "opto/rootnode.hpp"
#include "opto/phaseX.hpp"

// Clones the inline type to handle control flow merges involving multiple inline types.
// The inputs are replaced by PhiNodes to represent the merged values for the given region.
InlineTypeBaseNode* InlineTypeBaseNode::clone_with_phis(PhaseGVN* gvn, Node* region, bool is_init) {
  InlineTypeBaseNode* vt = clone()->as_InlineTypeBase();
  if (vt->is_InlineTypePtr()) {
    // Use nullable type
    const Type* t = Type::get_const_type(inline_klass());
    gvn->set_type(vt, t);
    vt->as_InlineTypePtr()->set_type(t);
  }

  // Create a PhiNode for merging the oop values
  const Type* phi_type = Type::get_const_type(inline_klass());
  PhiNode* oop = PhiNode::make(region, vt->get_oop(), phi_type);
  gvn->set_type(oop, phi_type);
  gvn->record_for_igvn(oop);
  vt->set_oop(oop);

  // Create a PhiNode for merging the is_init values
  Node* is_init_node;
  if (is_init) {
    is_init_node = gvn->intcon(1);
  } else {
    phi_type = Type::get_const_basic_type(T_BOOLEAN);
    is_init_node = PhiNode::make(region, vt->get_is_init(), phi_type);
    gvn->set_type(is_init_node, phi_type);
    gvn->record_for_igvn(is_init_node);
  }
  vt->set_req(IsInit, is_init_node);

  // Create a PhiNode each for merging the field values
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* type = vt->field_type(i);
    Node*  value = vt->field_value(i);
    if (value->is_InlineTypeBase()) {
      // Handle inline type fields recursively
      value = value->as_InlineTypeBase()->clone_with_phis(gvn, region);
    } else {
      phi_type = Type::get_const_type(type);
      value = PhiNode::make(region, value, phi_type);
      gvn->set_type(value, phi_type);
      gvn->record_for_igvn(value);
    }
    vt->set_field_value(i, value);
  }
  gvn->set_type(vt, vt->bottom_type());
  gvn->record_for_igvn(vt);
  return vt;
}

// Checks if the inputs of the InlineTypeBaseTypeNode were replaced by PhiNodes
// for the given region (see InlineTypeBaseTypeNode::clone_with_phis).
bool InlineTypeBaseNode::has_phi_inputs(Node* region) {
  // Check oop input
  bool result = get_oop()->is_Phi() && get_oop()->as_Phi()->region() == region;
#ifdef ASSERT
  if (result) {
    // Check all field value inputs for consistency
    for (uint i = Values; i < field_count(); ++i) {
      Node* n = in(i);
      if (n->is_InlineTypeBase()) {
        assert(n->as_InlineTypeBase()->has_phi_inputs(region), "inconsistent phi inputs");
      } else {
        assert(n->is_Phi() && n->as_Phi()->region() == region, "inconsistent phi inputs");
      }
    }
  }
#endif
  return result;
}

// Check if all inline type fields have inline type node values
bool InlineTypeBaseNode::can_merge() {
  for (uint i = 0; i < field_count(); ++i) {
    ciType* type = field_type(i);
    Node* val = field_value(i);
    if (type->is_inlinetype() &&
        (!val->is_InlineTypeBase() || !val->as_InlineTypeBase()->can_merge())) {
      return false;
    }
  }
  return true;
}

// Merges 'this' with 'other' by updating the input PhiNodes added by 'clone_with_phis'
InlineTypeBaseNode* InlineTypeBaseNode::merge_with(PhaseGVN* gvn, const InlineTypeBaseNode* other, int pnum, bool transform) {
  // Merge oop inputs
  PhiNode* phi = get_oop()->as_Phi();
  phi->set_req(pnum, other->get_oop());
  if (transform) {
    set_oop(gvn->transform(phi));
  }

  Node* is_init = get_is_init();
  if (is_init->is_Phi()) {
    phi = is_init->as_Phi();
    phi->set_req(pnum, other->get_is_init());
    if (transform) {
      set_req(IsInit, gvn->transform(phi));
    }
  } else {
    assert(is_init->find_int_con(0) == 1, "only with a non null inline type");
  }

  // Merge field values
  for (uint i = 0; i < field_count(); ++i) {
    Node* val1 =        field_value(i);
    Node* val2 = other->field_value(i);
    if (val1->is_InlineTypeBase()) {
      val1->as_InlineTypeBase()->merge_with(gvn, val2->as_InlineTypeBase(), pnum, transform);
    } else {
      assert(val1->is_Phi(), "must be a phi node");
      val1->set_req(pnum, val2);
    }
    if (transform) {
      set_field_value(i, gvn->transform(val1));
    }
  }
  return this;
}

// Adds a new merge path to an inline type node with phi inputs
void InlineTypeBaseNode::add_new_path(Node* region) {
  assert(has_phi_inputs(region), "must have phi inputs");

  PhiNode* phi = get_oop()->as_Phi();
  phi->add_req(NULL);
  assert(phi->req() == region->req(), "must be same size as region");

  phi = get_is_init()->as_Phi();
  phi->add_req(NULL);
  assert(phi->req() == region->req(), "must be same size as region");

  for (uint i = 0; i < field_count(); ++i) {
    Node* val = field_value(i);
    if (val->is_InlineTypeBase()) {
      val->as_InlineTypeBase()->add_new_path(region);
    } else {
      val->as_Phi()->add_req(NULL);
      assert(val->req() == region->req(), "must be same size as region");
    }
  }
}

Node* InlineTypeBaseNode::field_value(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return in(Values + index);
}

// Get the value of the field at the given offset.
// If 'recursive' is true, flattened inline type fields will be resolved recursively.
Node* InlineTypeBaseNode::field_value_by_offset(int offset, bool recursive) const {
  // If the field at 'offset' belongs to a flattened inline type field, 'index' refers to the
  // corresponding InlineTypeNode input and 'sub_offset' is the offset in flattened inline type.
  int index = inline_klass()->field_index_by_offset(offset);
  int sub_offset = offset - field_offset(index);
  Node* value = field_value(index);
  assert(value != NULL, "field value not found");
  if (recursive && value->is_InlineType()) {
    InlineTypeNode* vt = value->as_InlineType();
    if (field_is_flattened(index)) {
      // Flattened inline type field
      sub_offset += vt->inline_klass()->first_field_offset(); // Add header size
      return vt->field_value_by_offset(sub_offset, recursive);
    } else {
      assert(sub_offset == 0, "should not have a sub offset");
      return vt;
    }
  }
  assert(!(recursive && value->is_InlineType()), "should not be an inline type");
  assert(sub_offset == 0, "offset mismatch");
  return value;
}

void InlineTypeBaseNode::set_field_value(uint index, Node* value) {
  assert(index < field_count(), "index out of bounds");
  set_req(Values + index, value);
}

void InlineTypeBaseNode::set_field_value_by_offset(int offset, Node* value) {
  set_field_value(field_index(offset), value);
}

int InlineTypeBaseNode::field_offset(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return inline_klass()->declared_nonstatic_field_at(index)->offset();
}

uint InlineTypeBaseNode::field_index(int offset) const {
  uint i = 0;
  for (; i < field_count() && field_offset(i) != offset; i++) { }
  assert(i < field_count(), "field not found");
  return i;
}

ciType* InlineTypeBaseNode::field_type(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return inline_klass()->declared_nonstatic_field_at(index)->type();
}

bool InlineTypeBaseNode::field_is_flattened(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = inline_klass()->declared_nonstatic_field_at(index);
  assert(!field->is_flattened() || field->type()->is_inlinetype(), "must be an inline type");
  return field->is_flattened();
}

bool InlineTypeBaseNode::field_is_null_free(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = inline_klass()->declared_nonstatic_field_at(index);
  assert(!field->is_flattened() || field->type()->is_inlinetype(), "must be an inline type");
  return field->is_null_free();
}

void InlineTypeBaseNode::make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt) {
  ciInlineKlass* vk = inline_klass();
  uint nfields = vk->nof_nonstatic_fields();
  JVMState* jvms = sfpt->jvms();
  // Replace safepoint edge by SafePointScalarObjectNode and add field values
  assert(jvms != NULL, "missing JVMS");
  uint first_ind = (sfpt->req() - jvms->scloff());
  SafePointScalarObjectNode* sobj = new SafePointScalarObjectNode(inline_ptr(),
#ifdef ASSERT
                                                                  NULL,
#endif
                                                                  first_ind, nfields);
  sobj->init_req(0, igvn->C->root());
  // Nullable inline types have an is_init field that needs
  // to be checked before using the field values.
  if (!igvn->type(get_is_init())->is_int()->is_con(1)) {
    sfpt->add_req(get_is_init());
  } else {
    sfpt->add_req(igvn->C->top());
  }
  // Iterate over the inline type fields in order of increasing
  // offset and add the field values to the safepoint.
  for (uint j = 0; j < nfields; ++j) {
    int offset = vk->nonstatic_field_at(j)->offset();
    Node* value = field_value_by_offset(offset, true /* include flattened inline type fields */);
    if (value->is_InlineTypeBase()) {
      // Add inline type field to the worklist to process later
      worklist.push(value);
    }
    sfpt->add_req(value);
  }
  jvms->set_endoff(sfpt->req());
  sobj = igvn->transform(sobj)->as_SafePointScalarObject();
  igvn->rehash_node_delayed(sfpt);
  for (uint i = jvms->debug_start(); i < jvms->debug_end(); i++) {
    Node* debug = sfpt->in(i);
    if (debug != NULL && debug->uncast() == this) {
      sfpt->set_req(i, sobj);
    }
  }
}

void InlineTypeBaseNode::make_scalar_in_safepoints(PhaseIterGVN* igvn, bool allow_oop) {
  // If the inline type has a constant or loaded oop, use the oop instead of scalarization
  // in the safepoint to avoid keeping field loads live just for the debug info.
  Node* oop = get_oop();
  bool use_oop = allow_oop && (is_InlineTypePtr() || is_allocated(igvn)) &&
                 (oop->is_Con() || oop->is_Load() || (oop->isa_DecodeN() && oop->in(1)->is_Load()));

  ResourceMark rm;
  Unique_Node_List safepoints;
  Unique_Node_List vt_worklist;
  Unique_Node_List worklist;
  worklist.push(this);
  while (worklist.size() > 0) {
    Node* n = worklist.pop();
    for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
      Node* use = n->fast_out(i);
      if (use->is_SafePoint() && !use->is_CallLeaf() && (!use->is_Call() || use->as_Call()->has_debug_use(n))) {
        safepoints.push(use);
      } else if (use->is_ConstraintCast()) {
        worklist.push(use);
      }
    }
  }

  // Process all safepoint uses and scalarize inline type
  while (safepoints.size() > 0) {
    SafePointNode* sfpt = safepoints.pop()->as_SafePoint();
    if (use_oop) {
      for (uint i = sfpt->jvms()->debug_start(); i < sfpt->jvms()->debug_end(); i++) {
        Node* debug = sfpt->in(i);
        if (debug != NULL && debug->uncast() == this) {
          sfpt->set_req(i, get_oop());
        }
      }
      igvn->rehash_node_delayed(sfpt);
    } else {
      make_scalar_in_safepoint(igvn, vt_worklist, sfpt);
    }
  }
  // Now scalarize non-flattened fields
  for (uint i = 0; i < vt_worklist.size(); ++i) {
    InlineTypeBaseNode* vt = vt_worklist.at(i)->isa_InlineTypeBase();
    vt->make_scalar_in_safepoints(igvn);
  }
  if (outcnt() == 0) {
    igvn->_worklist.push(this);
  }
}

const TypePtr* InlineTypeBaseNode::field_adr_type(Node* base, int offset, ciInstanceKlass* holder, DecoratorSet decorators, PhaseGVN& gvn) const {
  const TypeAryPtr* ary_type = gvn.type(base)->isa_aryptr();
  const TypePtr* adr_type = NULL;
  bool is_array = ary_type != NULL;
  if ((decorators & C2_MISMATCHED) != 0) {
    adr_type = TypeRawPtr::BOTTOM;
  } else if (is_array) {
    // In the case of a flattened inline type array, each field has its own slice
    adr_type = ary_type->with_field_offset(offset)->add_offset(Type::OffsetBot);
  } else {
    ciField* field = holder->get_field_by_offset(offset, false);
    assert(field != NULL, "field not found");
    adr_type = gvn.C->alias_type(field)->adr_type();
  }
  return adr_type;
}

void InlineTypeBaseNode::load(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) {
  // Initialize the inline type by loading its field values from
  // memory and adding the values as input edges to the node.
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = NULL;
    ciType* ft = field_type(i);
    bool null_free = field_is_null_free(i);
    if (ft->is_inlinetype() && ft->as_inline_klass()->is_empty()) {
      // Loading from a field of an empty inline type. Just return the default instance.
      value = InlineTypeNode::make_default(kit->gvn(), ft->as_inline_klass());
    } else if (field_is_flattened(i)) {
      // Recursively load the flattened inline type field
      value = InlineTypeNode::make_from_flattened(kit, ft->as_inline_klass(), base, ptr, holder, offset, decorators);
    } else {
      const TypeOopPtr* oop_ptr = kit->gvn().type(base)->isa_oopptr();
      bool is_array = (oop_ptr->isa_aryptr() != NULL);
      bool mismatched = (decorators & C2_MISMATCHED) != 0;
      if (base->is_Con() && !is_array && !mismatched) {
        // If the oop to the inline type is constant (static final field), we can
        // also treat the fields as constants because the inline type is immutable.
        ciObject* constant_oop = oop_ptr->const_oop();
        ciField* field = holder->get_field_by_offset(offset, false);
        assert(field != NULL, "field not found");
        ciConstant constant = constant_oop->as_instance()->field_value(field);
        const Type* con_type = Type::make_from_constant(constant, /*require_const=*/ true);
        assert(con_type != NULL, "type not found");
        value = kit->gvn().transform(kit->makecon(con_type));
        // Check type of constant which might be more precise than the static field type
        if (con_type->is_inlinetypeptr() && !con_type->is_zero_type()) {
          ft = con_type->inline_klass();
          null_free = true;
        }
      } else {
        // Load field value from memory
        const TypePtr* adr_type = field_adr_type(base, offset, holder, decorators, kit->gvn());
        Node* adr = kit->basic_plus_adr(base, ptr, offset);
        BasicType bt = type2field[ft->basic_type()];
        assert(is_java_primitive(bt) || adr->bottom_type()->is_ptr_to_narrowoop() == UseCompressedOops, "inconsistent");
        const Type* val_type = Type::get_const_type(ft);
        if (is_array) {
          decorators |= IS_ARRAY;
        }
        value = kit->access_load_at(base, adr, adr_type, val_type, bt, decorators);
      }
      // Loading a non-flattened inline type from memory
      if (ft->is_inlinetype()) {
        value = InlineTypeNode::make_from_oop(kit, value, ft->as_inline_klass(), null_free);
      }
    }
    set_field_value(i, value);
  }
}

void InlineTypeBaseNode::store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) const {
  if (kit->gvn().type(base)->isa_aryptr()) {
    kit->C->set_flattened_accesses();
  }
  // The inline type is embedded into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when storing the values.
  if (holder == NULL) {
    holder = inline_klass();
  }
  holder_offset -= inline_klass()->first_field_offset();
  store(kit, base, ptr, holder, holder_offset, decorators);
}

void InlineTypeBaseNode::store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) const {
  // Write field values to memory
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    ciType* ft = field_type(i);
    if (field_is_flattened(i)) {
      // Recursively store the flattened inline type field
      if (!value->is_InlineType()) {
        assert(!kit->gvn().type(value)->maybe_null(), "Inline types are null-free");
        value = InlineTypeNode::make_from_oop(kit, value, ft->as_inline_klass());
      }
      value->as_InlineType()->store_flattened(kit, base, ptr, holder, offset, decorators);
    } else {
      // Store field value to memory
      const TypePtr* adr_type = field_adr_type(base, offset, holder, decorators, kit->gvn());
      Node* adr = kit->basic_plus_adr(base, ptr, offset);
      BasicType bt = type2field[ft->basic_type()];
      assert(is_java_primitive(bt) || adr->bottom_type()->is_ptr_to_narrowoop() == UseCompressedOops, "inconsistent");
      const Type* val_type = Type::get_const_type(ft);
      const TypeAryPtr* ary_type = kit->gvn().type(base)->isa_aryptr();
      if (ary_type != NULL) {
        decorators |= IS_ARRAY;
      }
      kit->access_store_at(base, adr, adr_type, value, val_type, bt, decorators);
    }
  }
}

InlineTypePtrNode* InlineTypeBaseNode::buffer(GraphKit* kit, bool safe_for_replace) {
  assert(is_InlineType(), "sanity");
  // Check if inline type is already allocated
  Node* null_ctl = kit->top();
  Node* not_null_oop = kit->null_check_oop(get_oop(), &null_ctl);
  if (null_ctl->is_top()) {
    // Inline type is allocated
    return as_ptr(&kit->gvn());
  }
  assert(!is_allocated(&kit->gvn()), "should not be allocated");
  RegionNode* region = new RegionNode(3);

  // Oop is non-NULL, use it
  region->init_req(1, kit->control());
  PhiNode* oop = PhiNode::make(region, not_null_oop, inline_ptr()->join_speculative(TypePtr::NOTNULL));
  PhiNode* io  = PhiNode::make(region, kit->i_o(), Type::ABIO);
  PhiNode* mem = PhiNode::make(region, kit->merged_memory(), Type::MEMORY, TypePtr::BOTTOM);

  int bci = kit->bci();
  bool reexecute = kit->jvms()->should_reexecute();
  {
    // Oop is NULL, allocate and initialize buffer
    PreserveJVMState pjvms(kit);
    // Propagate re-execution state and bci
    kit->set_bci(bci);
    kit->jvms()->set_bci(bci);
    kit->jvms()->set_should_reexecute(reexecute);
    kit->set_control(null_ctl);
    kit->kill_dead_locals();
    ciInlineKlass* vk = inline_klass();
    Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
    Node* alloc_oop  = kit->new_instance(klass_node, NULL, NULL, /* deoptimize_on_exception */ true, this);
    store(kit, alloc_oop, alloc_oop, vk);

    // Do not let stores that initialize this buffer be reordered with a subsequent
    // store that would make this buffer accessible by other threads.
    AllocateNode* alloc = AllocateNode::Ideal_allocation(alloc_oop, &kit->gvn());
    assert(alloc != NULL, "must have an allocation node");
    kit->insert_mem_bar(Op_MemBarStoreStore, alloc->proj_out_or_null(AllocateNode::RawAddress));

    region->init_req(2, kit->control());
    oop   ->init_req(2, alloc_oop);
    io    ->init_req(2, kit->i_o());
    mem   ->init_req(2, kit->merged_memory());
  }

  // Update GraphKit
  kit->set_control(kit->gvn().transform(region));
  kit->set_i_o(kit->gvn().transform(io));
  kit->set_all_memory(kit->gvn().transform(mem));
  kit->record_for_igvn(region);
  kit->record_for_igvn(oop);
  kit->record_for_igvn(io);
  kit->record_for_igvn(mem);

  // Use cloned InlineTypeNode to propagate oop from now on
  Node* res_oop = kit->gvn().transform(oop);
  InlineTypeBaseNode* vt = clone()->as_InlineTypeBase();
  vt->set_oop(res_oop);
  vt = kit->gvn().transform(vt)->as_InlineTypeBase();
  if (safe_for_replace) {
    kit->replace_in_map(this, vt);
  }
  // InlineTypeNode::remove_redundant_allocations piggybacks on split if.
  // Make sure it gets a chance to remove this allocation.
  kit->C->set_has_split_ifs(true);
  return vt->as_ptr(&kit->gvn());
}

bool InlineTypeBaseNode::is_allocated(PhaseGVN* phase) const {
  Node* oop = get_oop();
  const Type* oop_type = (phase != NULL) ? phase->type(oop) : oop->bottom_type();
  return !oop_type->maybe_null();
}

InlineTypePtrNode* InlineTypeBaseNode::as_ptr(PhaseGVN* phase) const {
  assert(is_allocated(phase), "must be allocated");
  if (is_InlineTypePtr()) {
    return as_InlineTypePtr();
  }
  return phase->transform(new InlineTypePtrNode(this))->as_InlineTypePtr();
}

// When a call returns multiple values, it has several result
// projections, one per field. Replacing the result of the call by an
// inline type node (after late inlining) requires that for each result
// projection, we find the corresponding inline type field.
void InlineTypeBaseNode::replace_call_results(GraphKit* kit, Node* call, Compile* C) {
  ciInlineKlass* vk = inline_klass();
  for (DUIterator_Fast imax, i = call->fast_outs(imax); i < imax; i++) {
    ProjNode* pn = call->fast_out(i)->as_Proj();
    uint con = pn->_con;
    Node* field = NULL;
    if (con == TypeFunc::Parms) {
      field = get_oop();
    } else if (con > TypeFunc::Parms) {
      uint field_nb = con - (TypeFunc::Parms+1);
      int extra = 0;
      for (uint j = 0; j < field_nb - extra; j++) {
        ciField* f = vk->nonstatic_field_at(j);
        BasicType bt = f->type()->basic_type();
        if (bt == T_LONG || bt == T_DOUBLE) {
          extra++;
        }
      }
      ciField* f = vk->nonstatic_field_at(field_nb - extra);
      field = field_value_by_offset(f->offset(), true);
      if (field->is_InlineType()) {
        assert(field->as_InlineType()->is_allocated(&kit->gvn()), "must be allocated");
        field = field->as_InlineType()->get_oop();
      }
    }
    if (field != NULL) {
      C->gvn_replace_by(pn, field);
      C->initial_gvn()->hash_delete(pn);
      pn->set_req(0, C->top());
      --i; --imax;
    }
  }
}

Node* InlineTypeBaseNode::allocate_fields(GraphKit* kit) {
  InlineTypeBaseNode* vt = clone()->as_InlineTypeBase();
  for (uint i = 0; i < field_count(); i++) {
     InlineTypeNode* value = field_value(i)->isa_InlineType();
     if (field_is_flattened(i)) {
       // Flattened inline type field
       vt->set_field_value(i, value->allocate_fields(kit));
     } else if (value != NULL) {
       // Non-flattened inline type field
       vt->set_field_value(i, value->buffer(kit));
     }
  }
  vt = kit->gvn().transform(vt)->as_InlineTypeBase();
  kit->replace_in_map(this, vt);
  return vt;
}

Node* InlineTypeBaseNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  if (phase->C->scalarize_in_safepoints() && can_reshape) {
    PhaseIterGVN* igvn = phase->is_IterGVN();
    make_scalar_in_safepoints(igvn);
    if (outcnt() == 0) {
      return NULL;
    }
  }
  Node* is_init = get_is_init();
  if (is_init->isa_InlineTypePtr()) {
    set_req(IsInit, is_init->as_InlineTypePtr()->get_is_init());
    return this;
  }
  Node* oop = get_oop();
  if (oop->isa_InlineTypePtr() && !phase->type(oop)->maybe_null()) {
    InlineTypePtrNode* vtptr = oop->as_InlineTypePtr();
    set_oop(vtptr->get_oop());
    set_is_init(*phase);
    for (uint i = Values; i < vtptr->req(); ++i) {
      set_req(i, vtptr->in(i));
    }
    return this;
  }
  return NULL;
}

InlineTypeNode* InlineTypeNode::make_uninitialized(PhaseGVN& gvn, ciInlineKlass* vk) {
  // Create a new InlineTypeNode with uninitialized values and NULL oop
  Node* oop = vk->is_empty() ? default_oop(gvn, vk) : gvn.zerocon(T_INLINE_TYPE);
  InlineTypeNode* vt = new InlineTypeNode(vk, oop);
  vt->set_is_init(gvn);
  return vt;
}

Node* InlineTypeBaseNode::default_oop(PhaseGVN& gvn, ciInlineKlass* vk) {
  // Returns the constant oop of the default inline type allocation
  return gvn.makecon(TypeInstPtr::make(vk->default_instance()));
}

InlineTypeNode* InlineTypeNode::make_default(PhaseGVN& gvn, ciInlineKlass* vk) {
  // Create a new InlineTypeNode with default values
  InlineTypeNode* vt = new InlineTypeNode(vk, default_oop(gvn, vk));
  vt->set_is_init(gvn);
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* field_type = vt->field_type(i);
    Node* value = gvn.zerocon(field_type->basic_type());
    if (field_type->is_inlinetype()) {
      ciInlineKlass* vk = field_type->as_inline_klass();
      if (vt->field_is_null_free(i)) {
        value = make_default(gvn, vk);
      } else {
        value = InlineTypePtrNode::make_null(gvn, vk);
      }
    }
    vt->set_field_value(i, value);
  }
  vt = gvn.transform(vt)->as_InlineType();
  assert(vt->is_default(&gvn), "must be the default inline type");
  return vt;
}

InlineTypeNode* InlineTypeNode::make_null(PhaseGVN& gvn, ciInlineKlass* vk) {
  InlineTypeNode* vt = new InlineTypeNode(vk, gvn.zerocon(T_OBJECT));
  vt->set_req(IsInit, gvn.intcon(0));
  for (uint i = 0; i < vt->field_count(); i++) {
    ciType* field_type = vt->field_type(i);
    Node* value = gvn.zerocon(field_type->basic_type());
    if (field_type->is_inlinetype()) {
      if (vt->field_is_null_free(i)) {
        value = InlineTypeNode::make_null(gvn, field_type->as_inline_klass());
      } else {
        value = InlineTypePtrNode::make_null(gvn, field_type->as_inline_klass());
      }
    }
    vt->set_field_value(i, value);
  }
  return gvn.transform(vt)->as_InlineType();
}

bool InlineTypeBaseNode::is_default(PhaseGVN* gvn) const {
  for (uint i = 0; i < field_count(); ++i) {
    Node* value = field_value(i);
    if (value->is_InlineTypePtr()) {
      value = value->as_InlineTypePtr()->get_oop();
    }
    if (!gvn->type(value)->is_zero_type() &&
        !(field_is_null_free(i) && value->is_InlineType() && value->as_InlineType()->is_default(gvn))) {
      return false;
    }
  }
  return true;
}

Node* InlineTypeNode::make_from_oop(GraphKit* kit, Node* oop, ciInlineKlass* vk, bool null_free) {
  PhaseGVN& gvn = kit->gvn();

  if (vk->is_empty()) {
    Node* def = make_default(gvn, vk);
    if (!null_free) {
      def = gvn.transform(new InlineTypePtrNode(def->as_InlineType(), false));
    }
    kit->record_for_igvn(def);
    return def;
  }
  // Create and initialize an InlineTypeNode by loading all field
  // values from a heap-allocated version and also save the oop.
  InlineTypeBaseNode* vt = NULL;

  if (oop->isa_InlineTypePtr()) {
    InlineTypePtrNode* vtptr = oop->as_InlineTypePtr();
    if (!null_free) {
      return vtptr;
    }
    vt = new InlineTypeNode(vk, vtptr->get_oop());
    vt->set_is_init(gvn);
    for (uint i = Values; i < vtptr->req(); ++i) {
      vt->init_req(i, vtptr->in(i));
    }
    kit->record_for_igvn(vt);
    return gvn.transform(vt);
  } else if (gvn.type(oop)->maybe_null()) {
    // Add a null check because the oop may be null
    Node* null_ctl = kit->top();
    Node* not_null_oop = kit->null_check_oop(oop, &null_ctl);
    if (kit->stopped()) {
      // Constant null
      kit->set_control(null_ctl);
      if (null_free) {
        vt = make_default(gvn, vk);
      } else {
        vt = InlineTypePtrNode::make_null(gvn, vk);
      }
      kit->record_for_igvn(vt);
      return vt;
    }
    if (null_free) {
      vt = new InlineTypeNode(vk, not_null_oop);
    } else {
      vt = new InlineTypePtrNode(vk, not_null_oop);
    }
    vt->set_is_init(gvn);
    vt->load(kit, not_null_oop, not_null_oop, vk, /* holder_offset */ 0);

    if (null_ctl != kit->top()) {
      InlineTypeBaseNode* null_vt = NULL;
      if (null_free) {
        null_vt = make_default(gvn, vk);
      } else {
        null_vt = InlineTypePtrNode::make_null(gvn, vk);
      }
      Node* region = new RegionNode(3);
      region->init_req(1, kit->control());
      region->init_req(2, null_ctl);

      vt = vt->clone_with_phis(&gvn, region);
      vt->merge_with(&gvn, null_vt, 2, true);
      kit->set_control(gvn.transform(region));
    }
  } else {
    if (null_free) {
      vt = new InlineTypeNode(vk, oop);
    } else {
      vt = new InlineTypePtrNode(vk, oop);
    }
    // Oop can never be null
    Node* init_ctl = kit->control();
    vt->set_is_init(gvn);
    vt->load(kit, oop, oop, vk, /* holder_offset */ 0);
    assert(!null_free || vt->as_InlineType()->is_default(&gvn) || init_ctl != kit->control() || !gvn.type(oop)->is_inlinetypeptr() || oop->is_Con() || oop->Opcode() == Op_InlineTypePtr ||
           AllocateNode::Ideal_allocation(oop, &gvn) != NULL || vt->as_InlineType()->is_loaded(&gvn) == oop, "inline type should be loaded");
  }
  assert(!null_free || vt->is_allocated(&gvn), "inline type should be allocated");
  kit->record_for_igvn(vt);
  return gvn.transform(vt);
}

// GraphKit wrapper for the 'make_from_flattened' method
InlineTypeNode* InlineTypeNode::make_from_flattened(GraphKit* kit, ciInlineKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) {
  if (kit->gvn().type(obj)->isa_aryptr()) {
    kit->C->set_flattened_accesses();
  }
  // Create and initialize an InlineTypeNode by loading all field values from
  // a flattened inline type field at 'holder_offset' or from an inline type array.
  InlineTypeNode* vt = make_uninitialized(kit->gvn(), vk);
  // The inline type is flattened into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when loading the values.
  holder_offset -= vk->first_field_offset();
  vt->load(kit, obj, ptr, holder, holder_offset, decorators);
  assert(vt->is_loaded(&kit->gvn()) != obj, "holder oop should not be used as flattened inline type oop");
  return kit->gvn().transform(vt)->as_InlineType();
}

InlineTypeNode* InlineTypeNode::make_from_multi(GraphKit* kit, MultiNode* multi, ciInlineKlass* vk, uint& base_input, bool in) {
  InlineTypeNode* vt = make_uninitialized(kit->gvn(), vk);
  if (!in) {
    // Keep track of the oop. The returned inline type might already be buffered.
    Node* oop = kit->gvn().transform(new ProjNode(multi, base_input++));
    vt->set_oop(oop);
  }
  vt->initialize_fields(kit, multi, base_input, in);
  return kit->gvn().transform(vt)->as_InlineType();
}

InlineTypeNode* InlineTypeNode::make_larval(GraphKit* kit, bool allocate) const {
  ciInlineKlass* vk = inline_klass();
  InlineTypeNode* res = clone()->as_InlineType();
  if (allocate) {
    // Re-execute if buffering triggers deoptimization
    PreserveReexecuteState preexecs(kit);
    kit->jvms()->set_should_reexecute(true);
    Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
    Node* alloc_oop  = kit->new_instance(klass_node, NULL, NULL, true);
    AllocateNode* alloc = AllocateNode::Ideal_allocation(alloc_oop, &kit->gvn());
    alloc->_larval = true;

    store(kit, alloc_oop, alloc_oop, vk);
    res->set_oop(alloc_oop);
  }
  res->set_type(TypeInlineType::make(vk, true));
  res = kit->gvn().transform(res)->as_InlineType();
  assert(!allocate || res->is_allocated(&kit->gvn()), "must be allocated");
  return res;
}

InlineTypeNode* InlineTypeNode::finish_larval(GraphKit* kit) const {
  Node* obj = get_oop();
  Node* mark_addr = kit->basic_plus_adr(obj, oopDesc::mark_offset_in_bytes());
  Node* mark = kit->make_load(NULL, mark_addr, TypeX_X, TypeX_X->basic_type(), MemNode::unordered);
  mark = kit->gvn().transform(new AndXNode(mark, kit->MakeConX(~markWord::larval_bit_in_place)));
  kit->store_to_memory(kit->control(), mark_addr, mark, TypeX_X->basic_type(), kit->gvn().type(mark_addr)->is_ptr(), MemNode::unordered);

  // Do not let stores that initialize this buffer be reordered with a subsequent
  // store that would make this buffer accessible by other threads.
  AllocateNode* alloc = AllocateNode::Ideal_allocation(obj, &kit->gvn());
  assert(alloc != NULL, "must have an allocation node");
  kit->insert_mem_bar(Op_MemBarStoreStore, alloc->proj_out_or_null(AllocateNode::RawAddress));

  ciInlineKlass* vk = inline_klass();
  InlineTypeNode* res = clone()->as_InlineType();
  res->set_type(TypeInlineType::make(vk, false));
  res = kit->gvn().transform(res)->as_InlineType();
  return res;
}

Node* InlineTypeNode::is_loaded(PhaseGVN* phase, ciInlineKlass* vk, Node* base, int holder_offset) {
  if (vk == NULL) {
    vk = inline_klass();
  }
  if (field_count() == 0) {
    assert(is_allocated(phase), "must be allocated");
    return get_oop();
  }
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    if (value->is_InlineTypeBase()) {
      InlineTypeBaseNode* vt = value->as_InlineTypeBase();
      if (vt->type()->inline_klass()->is_empty()) {
        continue;
      } else if (field_is_flattened(i) && vt->is_InlineType()) {
        // Check inline type field load recursively
        base = vt->as_InlineType()->is_loaded(phase, vk, base, offset - vt->type()->inline_klass()->first_field_offset());
        if (base == NULL) {
          return NULL;
        }
        continue;
      } else {
        value = vt->get_oop();
        if (value->Opcode() == Op_CastPP) {
          // Skip CastPP
          value = value->in(1);
        }
      }
    }
    if (value->isa_DecodeN()) {
      // Skip DecodeN
      value = value->in(1);
    }
    if (value->isa_Load()) {
      // Check if base and offset of field load matches inline type layout
      intptr_t loffset = 0;
      Node* lbase = AddPNode::Ideal_base_and_offset(value->in(MemNode::Address), phase, loffset);
      if (lbase == NULL || (lbase != base && base != NULL) || loffset != offset) {
        return NULL;
      } else if (base == NULL) {
        // Set base and check if pointer type matches
        base = lbase;
        const TypeInstPtr* vtptr = phase->type(base)->isa_instptr();
        if (vtptr == NULL || !vtptr->klass()->equals(vk)) {
          return NULL;
        }
      }
    } else {
      return NULL;
    }
  }
  return base;
}

Node* InlineTypeBaseNode::tagged_klass(ciInlineKlass* vk, PhaseGVN& gvn) {
  const TypeKlassPtr* tk = TypeKlassPtr::make(vk);
  intptr_t bits = tk->get_con();
  set_nth_bit(bits, 0);
  return gvn.makecon(TypeRawPtr::make((address)bits));
}

void InlineTypeBaseNode::pass_fields(GraphKit* kit, Node* n, uint& base_input) {
  for (uint i = 0; i < field_count(); i++) {
    int offset = field_offset(i);
    ciType* type = field_type(i);
    Node* arg = field_value(i);

    if (field_is_flattened(i)) {
      // Flattened inline type field
      InlineTypeNode* vt = arg->as_InlineType();
      vt->pass_fields(kit, n, base_input);
    } else {
      if (arg->is_InlineType()) {
        // Non-flattened inline type field
        InlineTypeNode* vt = arg->as_InlineType();
        assert(n->Opcode() != Op_Return || vt->is_allocated(&kit->gvn()), "inline type field should be allocated on return");
        arg = vt->buffer(kit);
      }
      // Initialize call/return arguments
      BasicType bt = field_type(i)->basic_type();
      n->init_req(base_input++, arg);
      if (type2size[bt] == 2) {
        n->init_req(base_input++, kit->top());
      }
    }
  }
}

void InlineTypeNode::initialize_fields(GraphKit* kit, MultiNode* multi, uint& base_input, bool in) {
  PhaseGVN& gvn = kit->gvn();
  for (uint i = 0; i < field_count(); ++i) {
    ciType* type = field_type(i);
    bool null_free = field_is_null_free(i);
    Node* parm = NULL;
    if (field_is_flattened(i)) {
      // Flattened inline type field
      InlineTypeNode* vt = make_uninitialized(gvn, type->as_inline_klass());
      vt->initialize_fields(kit, multi, base_input, in);
      parm = gvn.transform(vt);
    } else {
      if (multi->is_Start()) {
        assert(in, "return from start?");
        parm = gvn.transform(new ParmNode(multi->as_Start(), base_input));
      } else if (in) {
        parm = multi->as_Call()->in(base_input);
      } else {
        parm = gvn.transform(new ProjNode(multi->as_Call(), base_input));
      }
      // Non-flattened inline type field
      if (type->is_inlinetype()) {
        parm = make_from_oop(kit, parm, type->as_inline_klass(), null_free);
      }
      BasicType bt = type->basic_type();
      base_input += type2size[bt];
    }
    assert(parm != NULL, "should never be null");
    assert(field_value(i) == NULL, "already set");
    set_field_value(i, parm);
    gvn.record_for_igvn(parm);
  }
}

// Replace a buffer allocation by a dominating allocation
static void replace_allocation(PhaseIterGVN* igvn, Node* res, Node* dom) {
  // Remove initializing stores and GC barriers
  for (DUIterator_Fast imax, i = res->fast_outs(imax); i < imax; i++) {
    Node* use = res->fast_out(i);
    if (use->is_AddP()) {
      for (DUIterator_Fast jmax, j = use->fast_outs(jmax); j < jmax; j++) {
        Node* store = use->fast_out(j)->isa_Store();
        if (store != NULL) {
          igvn->rehash_node_delayed(store);
          igvn->replace_in_uses(store, store->in(MemNode::Memory));
        }
      }
    } else if (use->Opcode() == Op_CastP2X) {
      if (UseG1GC && use->find_out_with(Op_XorX)->in(1) != use) {
        // The G1 pre-barrier uses a CastP2X both for the pointer of the object
        // we store into, as well as the value we are storing. Skip if this is a
        // barrier for storing 'res' into another object.
        continue;
      }
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      bs->eliminate_gc_barrier(igvn, use);
      --i; --imax;
    }
  }
  igvn->replace_node(res, dom);
}

Node* InlineTypeNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* oop = get_oop();
  if (is_default(phase) && (!oop->is_Con() || phase->type(oop)->is_zero_type())) {
    // Use the pre-allocated oop for default inline types
    set_oop(default_oop(*phase, inline_klass()));
    assert(is_allocated(phase), "should now be allocated");
    return this;
  }

  if (!is_allocated(phase)) {
    // Save base oop if fields are loaded from memory and the inline
    // type is not buffered (in this case we should not use the oop).
    Node* base = is_loaded(phase);
    if (base != NULL && !phase->type(base)->maybe_null()) {
      set_oop(base);
      assert(is_allocated(phase), "should now be allocated");
      return this;
    }
  }

  if (can_reshape) {
    PhaseIterGVN* igvn = phase->is_IterGVN();

    if (is_allocated(phase)) {
      // Search for and remove re-allocations of this inline type. Ignore scalar replaceable ones,
      // they will be removed anyway and changing the memory chain will confuse other optimizations.
      // This can happen with late inlining when we first allocate an inline type argument
      // but later decide to inline the call after the callee code also triggered allocation.
      for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
        AllocateNode* alloc = fast_out(i)->isa_Allocate();
        if (alloc != NULL && alloc->in(AllocateNode::InlineTypeNode) == this && !alloc->_is_scalar_replaceable) {
          // Found a re-allocation
          Node* res = alloc->result_cast();
          if (res != NULL && res->is_CheckCastPP()) {
            // Replace allocation by oop and unlink AllocateNode
            replace_allocation(igvn, res, oop);
            igvn->replace_input_of(alloc, AllocateNode::InlineTypeNode, igvn->C->top());
            --i; --imax;
          }
        }
      }
    }
  }
  return InlineTypeBaseNode::Ideal(phase, can_reshape);
}

// Search for multiple allocations of this inline type and try to replace them by dominating allocations.
void InlineTypeNode::remove_redundant_allocations(PhaseIterGVN* igvn, PhaseIdealLoop* phase) {
  // Search for allocations of this inline type. Ignore scalar replaceable ones, they
  // will be removed anyway and changing the memory chain will confuse other optimizations.
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    AllocateNode* alloc = fast_out(i)->isa_Allocate();
    if (alloc != NULL && alloc->in(AllocateNode::InlineTypeNode) == this && !alloc->_is_scalar_replaceable) {
      Node* res = alloc->result_cast();
      if (res == NULL || !res->is_CheckCastPP()) {
        break; // No unique CheckCastPP
      }
      assert(!is_default(igvn) && !is_allocated(igvn), "re-allocation should be removed by Ideal transformation");
      // Search for a dominating allocation of the same inline type
      Node* res_dom = res;
      for (DUIterator_Fast jmax, j = fast_outs(jmax); j < jmax; j++) {
        AllocateNode* alloc_other = fast_out(j)->isa_Allocate();
        if (alloc_other != NULL && alloc_other->in(AllocateNode::InlineTypeNode) == this && !alloc_other->_is_scalar_replaceable) {
          Node* res_other = alloc_other->result_cast();
          if (res_other != NULL && res_other->is_CheckCastPP() && res_other != res_dom &&
              phase->is_dominator(res_other->in(0), res_dom->in(0))) {
            res_dom = res_other;
          }
        }
      }
      if (res_dom != res) {
        // Replace allocation by dominating one.
        replace_allocation(igvn, res, res_dom);
        // The result of the dominated allocation is now unused and will be removed
        // later in PhaseMacroExpand::eliminate_allocate_node to not confuse loop opts.
        igvn->_worklist.push(alloc);
      }
    }
  }

  // Process users
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    Node* out = fast_out(i);
    if (out->is_InlineType()) {
      // Recursively process inline type users
      igvn->rehash_node_delayed(out);
      out->as_InlineType()->remove_redundant_allocations(igvn, phase);
    } else if (out->isa_Allocate() != NULL) {
      // Unlink AllocateNode
      assert(out->in(AllocateNode::InlineTypeNode) == this, "should be linked");
      igvn->replace_input_of(out, AllocateNode::InlineTypeNode, igvn->C->top());
      --i; --imax;
    }
  }
}

InlineTypePtrNode* InlineTypePtrNode::make_null(PhaseGVN& gvn, ciInlineKlass* vk) {
  InlineTypePtrNode* ptr = new InlineTypePtrNode(vk, gvn.zerocon(T_OBJECT));
  ptr->set_req(IsInit, gvn.intcon(0));
  for (uint i = 0; i < ptr->field_count(); i++) {
    ciType* field_type = ptr->field_type(i);
    Node* value = gvn.zerocon(field_type->basic_type());
    if (field_type->is_inlinetype()) {
      if (ptr->field_is_null_free(i)) {
        value = InlineTypeNode::make_null(gvn, field_type->as_inline_klass());
      } else {
        value = InlineTypePtrNode::make_null(gvn, field_type->as_inline_klass());
      }
    }
    ptr->set_field_value(i, value);
  }
  return gvn.transform(ptr)->as_InlineTypePtr();
}

Node* InlineTypePtrNode::Identity(PhaseGVN* phase) {
  if (get_oop()->is_InlineTypePtr()) {
    return get_oop();
  }
  return this;
}

const Type* InlineTypePtrNode::Value(PhaseGVN* phase) const {
  const Type* t = phase->type(get_oop())->filter_speculative(_type);
  if (t->singleton()) {
    // Don't replace InlineTypePtr by a constant
    t = _type;
  }
  const Type* tinit = phase->type(in(IsInit));
  if (tinit->isa_int() && tinit->is_int()->is_con(1)) {
    t = t->join_speculative(TypePtr::NOTNULL);
  }
  return t;
}
