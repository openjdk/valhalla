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
#include "ci/ciValueKlass.hpp"
#include "opto/addnode.hpp"
#include "opto/castnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/rootnode.hpp"
#include "opto/valuetypenode.hpp"
#include "opto/phaseX.hpp"

// Clones the values type to handle control flow merges involving multiple value types.
// The inputs are replaced by PhiNodes to represent the merged values for the given region.
ValueTypeBaseNode* ValueTypeBaseNode::clone_with_phis(PhaseGVN* gvn, Node* region) {
  assert(!has_phi_inputs(region), "already cloned with phis");
  ValueTypeBaseNode* vt = clone()->as_ValueTypeBase();

  // Create a PhiNode for merging the oop values
  const Type* phi_type = Type::get_const_type(value_klass());
  PhiNode* oop = PhiNode::make(region, vt->get_oop(), phi_type);
  gvn->set_type(oop, phi_type);
  vt->set_oop(oop);

  // Create a PhiNode each for merging the field values
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* type = vt->field_type(i);
    Node*  value = vt->field_value(i);
    if (value->is_ValueTypeBase()) {
      // Handle flattened value type fields recursively
      value = value->as_ValueTypeBase()->clone_with_phis(gvn, region);
    } else {
      phi_type = Type::get_const_type(type);
      value = PhiNode::make(region, value, phi_type);
      gvn->set_type(value, phi_type);
    }
    vt->set_field_value(i, value);
  }
  gvn->set_type(vt, vt->bottom_type());
  return vt;
}

// Checks if the inputs of the ValueBaseTypeNode were replaced by PhiNodes
// for the given region (see ValueBaseTypeNode::clone_with_phis).
bool ValueTypeBaseNode::has_phi_inputs(Node* region) {
  // Check oop input
  bool result = get_oop()->is_Phi() && get_oop()->as_Phi()->region() == region;
#ifdef ASSERT
  if (result) {
    // Check all field value inputs for consistency
    for (uint i = Oop; i < field_count(); ++i) {
      Node* n = in(i);
      if (n->is_ValueTypeBase()) {
        assert(n->as_ValueTypeBase()->has_phi_inputs(region), "inconsistent phi inputs");
      } else {
        assert(n->is_Phi() && n->as_Phi()->region() == region, "inconsistent phi inputs");
      }
    }
  }
#endif
  return result;
}

// Merges 'this' with 'other' by updating the input PhiNodes added by 'clone_with_phis'
ValueTypeBaseNode* ValueTypeBaseNode::merge_with(PhaseGVN* gvn, const ValueTypeBaseNode* other, int pnum, bool transform) {
  // Merge oop inputs
  PhiNode* phi = get_oop()->as_Phi();
  phi->set_req(pnum, other->get_oop());
  if (transform) {
    set_oop(gvn->transform(phi));
    gvn->record_for_igvn(phi);
  }
  // Merge field values
  for (uint i = 0; i < field_count(); ++i) {
    Node* val1 =        field_value(i);
    Node* val2 = other->field_value(i);
    if (val1->is_ValueTypeBase()) {
      val1->as_ValueTypeBase()->merge_with(gvn, val2->as_ValueTypeBase(), pnum, transform);
    } else {
      assert(val1->is_Phi(), "must be a phi node");
      assert(!val2->is_ValueType(), "inconsistent merge values");
      val1->set_req(pnum, val2);
    }
    if (transform) {
      set_field_value(i, gvn->transform(val1));
      gvn->record_for_igvn(val1);
    }
  }
  return this;
}

// Adds a new merge path to a valuetype node with phi inputs
void ValueTypeBaseNode::add_new_path(Node* region) {
  assert(has_phi_inputs(region), "must have phi inputs");

  PhiNode* phi = get_oop()->as_Phi();
  phi->add_req(NULL);
  assert(phi->req() == region->req(), "must be same size as region");

  for (uint i = 0; i < field_count(); ++i) {
    Node* val = field_value(i);
    if (val->is_ValueType()) {
      val->as_ValueType()->add_new_path(region);
    } else {
      val->as_Phi()->add_req(NULL);
      assert(val->req() == region->req(), "must be same size as region");
    }
  }
}

Node* ValueTypeBaseNode::field_value(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return in(Values + index);
}

// Get the value of the field at the given offset.
// If 'recursive' is true, flattened value type fields will be resolved recursively.
Node* ValueTypeBaseNode::field_value_by_offset(int offset, bool recursive) const {
  // If the field at 'offset' belongs to a flattened value type field, 'index' refers to the
  // corresponding ValueTypeNode input and 'sub_offset' is the offset in flattened value type.
  int index = value_klass()->field_index_by_offset(offset);
  int sub_offset = offset - field_offset(index);
  Node* value = field_value(index);
  assert(value != NULL, "field value not found");
  if (recursive && value->is_ValueType()) {
    ValueTypeNode* vt = value->as_ValueType();
    if (field_is_flattened(index)) {
      // Flattened value type field
      sub_offset += vt->value_klass()->first_field_offset(); // Add header size
      return vt->field_value_by_offset(sub_offset, recursive);
    } else {
      assert(sub_offset == 0, "should not have a sub offset");
      return vt;
    }
  }
  assert(!(recursive && value->is_ValueType()), "should not be a value type");
  assert(sub_offset == 0, "offset mismatch");
  return value;
}

void ValueTypeBaseNode::set_field_value(uint index, Node* value) {
  assert(index < field_count(), "index out of bounds");
  set_req(Values + index, value);
}

void ValueTypeBaseNode::set_field_value_by_offset(int offset, Node* value) {
  set_field_value(field_index(offset), value);
}

int ValueTypeBaseNode::field_offset(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return value_klass()->declared_nonstatic_field_at(index)->offset();
}

uint ValueTypeBaseNode::field_index(int offset) const {
  uint i = 0;
  for (; i < field_count() && field_offset(i) != offset; i++) { }
  assert(i < field_count(), "field not found");
  return i;
}

ciType* ValueTypeBaseNode::field_type(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return value_klass()->declared_nonstatic_field_at(index)->type();
}

bool ValueTypeBaseNode::field_is_flattened(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = value_klass()->declared_nonstatic_field_at(index);
  assert(!field->is_flattened() || field->type()->is_valuetype(), "must be a value type");
  return field->is_flattened();
}

bool ValueTypeBaseNode::field_is_flattenable(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = value_klass()->declared_nonstatic_field_at(index);
  assert(!field->is_flattenable() || field->type()->is_valuetype(), "must be a value type");
  return field->is_flattenable();
}

int ValueTypeBaseNode::make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt) {
  ciValueKlass* vk = value_klass();
  uint nfields = vk->nof_nonstatic_fields();
  JVMState* jvms = sfpt->jvms();
  int start = jvms->debug_start();
  int end   = jvms->debug_end();
  // Replace safepoint edge by SafePointScalarObjectNode and add field values
  assert(jvms != NULL, "missing JVMS");
  uint first_ind = (sfpt->req() - jvms->scloff());
  SafePointScalarObjectNode* sobj = new SafePointScalarObjectNode(value_ptr(),
#ifdef ASSERT
                                                                  NULL,
#endif
                                                                  first_ind, nfields);
  sobj->init_req(0, igvn->C->root());
  // Iterate over the value type fields in order of increasing
  // offset and add the field values to the safepoint.
  for (uint j = 0; j < nfields; ++j) {
    int offset = vk->nonstatic_field_at(j)->offset();
    Node* value = field_value_by_offset(offset, true /* include flattened value type fields */);
    if (value->is_ValueType()) {
      // Add value type field to the worklist to process later
      worklist.push(value);
    }
    sfpt->add_req(value);
  }
  jvms->set_endoff(sfpt->req());
  sobj = igvn->transform(sobj)->as_SafePointScalarObject();
  igvn->rehash_node_delayed(sfpt);
  return sfpt->replace_edges_in_range(this, sobj, start, end);
}

void ValueTypeBaseNode::make_scalar_in_safepoints(PhaseIterGVN* igvn) {
  // Process all safepoint uses and scalarize value type
  Unique_Node_List worklist;
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    SafePointNode* sfpt = fast_out(i)->isa_SafePoint();
    if (sfpt != NULL && !sfpt->is_CallLeaf() && (!sfpt->is_Call() || sfpt->as_Call()->has_debug_use(this))) {
      int nb = 0;
      if (is_allocated(igvn) && get_oop()->is_Con()) {
        // Value type is allocated with a constant oop, link it directly
        nb = sfpt->replace_edges_in_range(this, get_oop(), sfpt->jvms()->debug_start(), sfpt->jvms()->debug_end());
        igvn->rehash_node_delayed(sfpt);
      } else {
        nb = make_scalar_in_safepoint(igvn, worklist, sfpt);
      }
      --i; imax -= nb;
    }
  }
  // Now scalarize non-flattened fields
  for (uint i = 0; i < worklist.size(); ++i) {
    Node* vt = worklist.at(i);
    vt->as_ValueType()->make_scalar_in_safepoints(igvn);
  }
  igvn->record_for_igvn(this);
}

const TypePtr* ValueTypeBaseNode::field_adr_type(Node* base, int offset, ciInstanceKlass* holder, DecoratorSet decorators, PhaseGVN& gvn) const {
  const TypeAryPtr* ary_type = gvn.type(base)->isa_aryptr();
  const TypePtr* adr_type = NULL;
  bool is_array = ary_type != NULL;
  if ((decorators & C2_MISMATCHED) != 0) {
    adr_type = TypeRawPtr::BOTTOM;
  } else if (is_array) {
    // In the case of a flattened value type array, each field has its own slice
    adr_type = ary_type->with_field_offset(offset)->add_offset(Type::OffsetBot);
  } else {
    ciField* field = holder->get_field_by_offset(offset, false);
    assert(field != NULL, "field not found");
    adr_type = gvn.C->alias_type(field)->adr_type();
  }
  return adr_type;
}

void ValueTypeBaseNode::load(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) {
  // Initialize the value type by loading its field values from
  // memory and adding the values as input edges to the node.
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = NULL;
    ciType* ft = field_type(i);
    bool is_flattenable = field_is_flattenable(i);
    if (field_is_flattened(i)) {
      // Recursively load the flattened value type field
      value = ValueTypeNode::make_from_flattened(kit, ft->as_value_klass(), base, ptr, holder, offset, decorators);
    } else {
      const TypeOopPtr* oop_ptr = kit->gvn().type(base)->isa_oopptr();
      bool is_array = (oop_ptr->isa_aryptr() != NULL);
      if (base->is_Con() && !is_array) {
        // If the oop to the value type is constant (static final field), we can
        // also treat the fields as constants because the value type is immutable.
        ciObject* constant_oop = oop_ptr->const_oop();
        ciField* field = holder->get_field_by_offset(offset, false);
        assert(field != NULL, "field not found");
        ciConstant constant = constant_oop->as_instance()->field_value(field);
        const Type* con_type = Type::make_from_constant(constant, /*require_const=*/ true);
        assert(con_type != NULL, "type not found");
        value = kit->gvn().transform(kit->makecon(con_type));
        // Check type of constant which might be more precise
        if (con_type->is_valuetypeptr() && !con_type->is_zero_type()) {
          // Null-free, treat as flattenable
          ft = con_type->value_klass();
          is_flattenable = true;
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
      if (is_flattenable) {
        // Loading a non-flattened but flattenable value type from memory
        if (ft->as_value_klass()->is_scalarizable()) {
          value = ValueTypeNode::make_from_oop(kit, value, ft->as_value_klass());
        } else {
          value = kit->null2default(value, ft->as_value_klass());
        }
      }
    }
    set_field_value(i, value);
  }
}

void ValueTypeBaseNode::store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) const {
  // The value type is embedded into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when storing the values.
  if (holder == NULL) {
    holder = value_klass();
  }
  holder_offset -= value_klass()->first_field_offset();
  store(kit, base, ptr, holder, holder_offset, decorators);
}

void ValueTypeBaseNode::store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) const {
  // Write field values to memory
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    ciType* ft = field_type(i);
    if (field_is_flattened(i)) {
      // Recursively store the flattened value type field
      if (!value->is_ValueType()) {
        assert(!kit->gvn().type(value)->maybe_null(), "should never be null");
        value = ValueTypeNode::make_from_oop(kit, value, ft->as_value_klass());
      }
      value->as_ValueType()->store_flattened(kit, base, ptr, holder, offset, decorators);
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

ValueTypePtrNode* ValueTypeBaseNode::buffer(GraphKit* kit, bool safe_for_replace) {
  assert(is_ValueType(), "sanity");
  // Check if value type is already allocated
  Node* null_ctl = kit->top();
  Node* not_null_oop = kit->null_check_oop(get_oop(), &null_ctl);
  if (null_ctl->is_top()) {
    // Value type is allocated
    return kit->gvn().transform(new ValueTypePtrNode(this))->as_ValueTypePtr();
  }
  assert(!is_allocated(&kit->gvn()), "should not be allocated");
  RegionNode* region = new RegionNode(3);

  // Oop is non-NULL, use it
  region->init_req(1, kit->control());
  PhiNode* oop = PhiNode::make(region, not_null_oop, value_ptr());
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
    ciValueKlass* vk = value_klass();
    Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
    Node* alloc_oop  = kit->new_instance(klass_node, NULL, NULL, /* deoptimize_on_exception */ true, this);
    store(kit, alloc_oop, alloc_oop, vk, 0);

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

  // Use cloned ValueTypeNode to propagate oop from now on
  Node* res_oop = kit->gvn().transform(oop);
  ValueTypeBaseNode* vt = clone()->as_ValueTypeBase();
  vt->set_oop(res_oop);
  vt = kit->gvn().transform(vt)->as_ValueTypeBase();
  if (safe_for_replace) {
    kit->replace_in_map(this, vt);
  }
  // ValueTypeNode::remove_redundant_allocations piggybacks on split if.
  // Make sure it gets a chance to remove this allocation.
  kit->C->set_has_split_ifs(true);
  assert(vt->is_allocated(&kit->gvn()), "must be allocated");
  return kit->gvn().transform(new ValueTypePtrNode(vt))->as_ValueTypePtr();
}

bool ValueTypeBaseNode::is_allocated(PhaseGVN* phase) const {
  Node* oop = get_oop();
  const Type* oop_type = (phase != NULL) ? phase->type(oop) : oop->bottom_type();
  return !oop_type->maybe_null();
}

// When a call returns multiple values, it has several result
// projections, one per field. Replacing the result of the call by a
// value type node (after late inlining) requires that for each result
// projection, we find the corresponding value type field.
void ValueTypeBaseNode::replace_call_results(GraphKit* kit, Node* call, Compile* C) {
  ciValueKlass* vk = value_klass();
  for (DUIterator_Fast imax, i = call->fast_outs(imax); i < imax; i++) {
    ProjNode* pn = call->fast_out(i)->as_Proj();
    uint con = pn->_con;
    if (con >= TypeFunc::Parms+1) {
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
      Node* field = field_value_by_offset(f->offset(), true);
      if (field->is_ValueType()) {
        assert(field->as_ValueType()->is_allocated(&kit->gvn()), "must be allocated");
        field = field->as_ValueType()->get_oop();
      }
      C->gvn_replace_by(pn, field);
      C->initial_gvn()->hash_delete(pn);
      pn->set_req(0, C->top());
      --i; --imax;
    }
  }
}

Node* ValueTypeBaseNode::allocate_fields(GraphKit* kit) {
  ValueTypeBaseNode* vt = clone()->as_ValueTypeBase();
  for (uint i = 0; i < field_count(); i++) {
     ValueTypeNode* value = field_value(i)->isa_ValueType();
     if (field_is_flattened(i)) {
       // Flattened value type field
       vt->set_field_value(i, value->allocate_fields(kit));
     } else if (value != NULL) {
       // Non-flattened value type field
       vt->set_field_value(i, value->buffer(kit));
     }
  }
  vt = kit->gvn().transform(vt)->as_ValueTypeBase();
  kit->replace_in_map(this, vt);
  return vt;
}

ValueTypeNode* ValueTypeNode::make_uninitialized(PhaseGVN& gvn, ciValueKlass* vk) {
  // Create a new ValueTypeNode with uninitialized values and NULL oop
  return new ValueTypeNode(vk, gvn.zerocon(T_VALUETYPE));
}

Node* ValueTypeNode::default_oop(PhaseGVN& gvn, ciValueKlass* vk) {
  // Returns the constant oop of the default value type allocation
  return gvn.makecon(TypeInstPtr::make(vk->default_value_instance()));
}

ValueTypeNode* ValueTypeNode::make_default(PhaseGVN& gvn, ciValueKlass* vk) {
  // Create a new ValueTypeNode with default values
  ValueTypeNode* vt = new ValueTypeNode(vk, default_oop(gvn, vk));
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* field_type = vt->field_type(i);
    Node* value = NULL;
    if (field_type->is_valuetype() && vt->field_is_flattenable(i)) {
      ciValueKlass* field_klass = field_type->as_value_klass();
      if (field_klass->is_scalarizable() || vt->field_is_flattened(i)) {
        value = ValueTypeNode::make_default(gvn, field_klass);
      } else {
        value = default_oop(gvn, field_klass);
      }
    } else {
      value = gvn.zerocon(field_type->basic_type());
    }
    vt->set_field_value(i, value);
  }
  vt = gvn.transform(vt)->as_ValueType();
  assert(vt->is_default(gvn), "must be the default value type");
  return vt;
}

bool ValueTypeNode::is_default(PhaseGVN& gvn) const {
  for (uint i = 0; i < field_count(); ++i) {
    Node* value = field_value(i);
    if (!gvn.type(value)->is_zero_type() &&
        !(value->is_ValueType() && value->as_ValueType()->is_default(gvn)) &&
        !(field_type(i)->is_valuetype() && value == default_oop(gvn, field_type(i)->as_value_klass()))) {
      return false;
    }
  }
  return true;
}

ValueTypeNode* ValueTypeNode::make_from_oop(GraphKit* kit, Node* oop, ciValueKlass* vk) {
  PhaseGVN& gvn = kit->gvn();

  // Create and initialize a ValueTypeNode by loading all field
  // values from a heap-allocated version and also save the oop.
  ValueTypeNode* vt = new ValueTypeNode(vk, oop);

  if (oop->isa_ValueTypePtr()) {
    // Can happen with late inlining
    ValueTypePtrNode* vtptr = oop->as_ValueTypePtr();
    vt->set_oop(vtptr->get_oop());
    for (uint i = Oop+1; i < vtptr->req(); ++i) {
      vt->init_req(i, vtptr->in(i));
    }
  } else if (gvn.type(oop)->maybe_null()) {
    // Add a null check because the oop may be null
    Node* null_ctl = kit->top();
    Node* not_null_oop = kit->null_check_oop(oop, &null_ctl);
    if (kit->stopped()) {
      // Constant null
      kit->set_control(null_ctl);
      return make_default(gvn, vk);
    }
    vt->set_oop(not_null_oop);
    vt->load(kit, not_null_oop, not_null_oop, vk, /* holder_offset */ 0);

    if (null_ctl != kit->top()) {
      // Return default value type if oop is null
      ValueTypeNode* def = make_default(gvn, vk);
      Node* region = new RegionNode(3);
      region->init_req(1, kit->control());
      region->init_req(2, null_ctl);

      vt = vt->clone_with_phis(&gvn, region)->as_ValueType();
      vt->merge_with(&gvn, def, 2, true);
      kit->set_control(gvn.transform(region));
    }
  } else {
    // Oop can never be null
    Node* init_ctl = kit->control();
    vt->load(kit, oop, oop, vk, /* holder_offset */ 0);
    assert(init_ctl != kit->control() || !gvn.type(oop)->is_valuetypeptr() || oop->is_Con() || oop->Opcode() == Op_ValueTypePtr ||
           AllocateNode::Ideal_allocation(oop, &gvn) != NULL || vt->is_loaded(&gvn) == oop, "value type should be loaded");
  }

  assert(vt->is_allocated(&gvn), "value type should be allocated");
  return gvn.transform(vt)->as_ValueType();
}

// GraphKit wrapper for the 'make_from_flattened' method
ValueTypeNode* ValueTypeNode::make_from_flattened(GraphKit* kit, ciValueKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) {
  // Create and initialize a ValueTypeNode by loading all field values from
  // a flattened value type field at 'holder_offset' or from a value type array.
  ValueTypeNode* vt = make_uninitialized(kit->gvn(), vk);
  // The value type is flattened into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when loading the values.
  holder_offset -= vk->first_field_offset();
  vt->load(kit, obj, ptr, holder, holder_offset, decorators);
  assert(vt->is_loaded(&kit->gvn()) != obj, "holder oop should not be used as flattened value type oop");
  return kit->gvn().transform(vt)->as_ValueType();
}

ValueTypeNode* ValueTypeNode::make_from_multi(GraphKit* kit, MultiNode* multi, ExtendedSignature& sig, ciValueKlass* vk, uint& base_input, bool in) {
  ValueTypeNode* vt = ValueTypeNode::make_uninitialized(kit->gvn(), vk);
  vt->initialize_fields(kit, multi, sig, base_input, 0, in);
  return kit->gvn().transform(vt)->as_ValueType();
}

ValueTypeNode* ValueTypeNode::make_larval(GraphKit* kit, bool allocate) const {
  ciValueKlass* vk = value_klass();
  ValueTypeNode* res = clone()->as_ValueType();
  if (allocate) {
    // Re-execute if buffering triggers deoptimization
    PreserveReexecuteState preexecs(kit);
    kit->jvms()->set_should_reexecute(true);
    Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
    Node* alloc_oop  = kit->new_instance(klass_node, NULL, NULL, true);
    AllocateNode* alloc = AllocateNode::Ideal_allocation(alloc_oop, &kit->gvn());
    alloc->_larval = true;

    store(kit, alloc_oop, alloc_oop, vk, 0);
    res->set_oop(alloc_oop);
  }
  res->set_type(TypeValueType::make(vk, true));
  res = kit->gvn().transform(res)->as_ValueType();
  assert(!allocate || res->is_allocated(&kit->gvn()), "must be allocated");
  return res;
}

ValueTypeNode* ValueTypeNode::finish_larval(GraphKit* kit) const {
  Node* obj = get_oop();
  Node* mark_addr = kit->basic_plus_adr(obj, oopDesc::mark_offset_in_bytes());
  Node* mark = kit->make_load(NULL, mark_addr, TypeX_X, TypeX_X->basic_type(), MemNode::unordered);
  mark = kit->gvn().transform(new AndXNode(mark, kit->MakeConX(~markWord::larval_mask_in_place)));
  kit->store_to_memory(kit->control(), mark_addr, mark, TypeX_X->basic_type(), kit->gvn().type(mark_addr)->is_ptr(), MemNode::unordered);

  // Do not let stores that initialize this buffer be reordered with a subsequent
  // store that would make this buffer accessible by other threads.
  AllocateNode* alloc = AllocateNode::Ideal_allocation(obj, &kit->gvn());
  assert(alloc != NULL, "must have an allocation node");
  kit->insert_mem_bar(Op_MemBarStoreStore, alloc->proj_out_or_null(AllocateNode::RawAddress));

  ciValueKlass* vk = value_klass();
  ValueTypeNode* res = clone()->as_ValueType();
  res->set_type(TypeValueType::make(vk, false));
  res = kit->gvn().transform(res)->as_ValueType();
  return res;
}

Node* ValueTypeNode::is_loaded(PhaseGVN* phase, ciValueKlass* vk, Node* base, int holder_offset) {
  if (vk == NULL) {
    vk = value_klass();
  }
  if (field_count() == 0) {
    assert(is_allocated(phase), "must be allocated");
    return get_oop();
  }
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    if (value->is_ValueType()) {
      ValueTypeNode* vt = value->as_ValueType();
      if (field_is_flattened(i)) {
        // Check value type field load recursively
        base = vt->is_loaded(phase, vk, base, offset - vt->value_klass()->first_field_offset());
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
      // Check if base and offset of field load matches value type layout
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

Node* ValueTypeNode::tagged_klass(ciValueKlass* vk, PhaseGVN& gvn) {
  const TypeKlassPtr* tk = TypeKlassPtr::make(vk);
  intptr_t bits = tk->get_con();
  set_nth_bit(bits, 0);
  return gvn.makecon(TypeRawPtr::make((address)bits));
}

void ValueTypeNode::pass_fields(GraphKit* kit, Node* n, ExtendedSignature& sig, uint& base_input, int base_offset) {
  for (uint i = 0; i < field_count(); i++) {
    int sig_offset = (*sig)._offset;
    uint idx = field_index(sig_offset - base_offset);
    Node* arg = field_value(idx);

    if (field_is_flattened(idx)) {
      // Flattened value type field
      ValueTypeNode* vt = arg->as_ValueType();
      vt->pass_fields(kit, n, sig, base_input, sig_offset - vt->value_klass()->first_field_offset());
    } else {
      if (arg->is_ValueType()) {
        // Non-flattened value type field
        ValueTypeNode* vt = arg->as_ValueType();
        assert(n->Opcode() != Op_Return || vt->is_allocated(&kit->gvn()), "value type field should be allocated on return");
        arg = vt->buffer(kit);
      }
      // Initialize call/return arguments
      BasicType bt = field_type(i)->basic_type();
      n->init_req(base_input++, arg);
      if (type2size[bt] == 2) {
        n->init_req(base_input++, kit->top());
      }
      // Skip reserved arguments
      while (SigEntry::next_is_reserved(sig, bt)) {
        n->init_req(base_input++, kit->top());
        if (type2size[bt] == 2) {
          n->init_req(base_input++, kit->top());
        }
      }
    }
  }
}

void ValueTypeNode::initialize_fields(GraphKit* kit, MultiNode* multi, ExtendedSignature& sig, uint& base_input, int base_offset, bool in) {
  PhaseGVN& gvn = kit->gvn();
  for (uint i = 0; i < field_count(); i++) {
    int sig_offset = (*sig)._offset;
    uint idx = field_index(sig_offset - base_offset);
    ciType* type = field_type(idx);

    Node* parm = NULL;
    if (field_is_flattened(idx)) {
      // Flattened value type field
      ValueTypeNode* vt = ValueTypeNode::make_uninitialized(gvn, type->as_value_klass());
      vt->initialize_fields(kit, multi, sig, base_input, sig_offset - type->as_value_klass()->first_field_offset(), in);
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
      if (field_is_flattenable(idx)) {
        // Non-flattened but flattenable value type
        if (type->as_value_klass()->is_scalarizable()) {
          parm = ValueTypeNode::make_from_oop(kit, parm, type->as_value_klass());
        } else {
          parm = kit->null2default(parm, type->as_value_klass());
        }
      }
      base_input += type2size[type->basic_type()];
      // Skip reserved arguments
      BasicType bt = type->basic_type();
      while (SigEntry::next_is_reserved(sig, bt)) {
        base_input += type2size[bt];
      }
    }
    assert(parm != NULL, "should never be null");
    set_field_value(idx, parm);
    gvn.record_for_igvn(parm);
  }
}

Node* ValueTypeNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* oop = get_oop();
  if (is_default(*phase) && (!oop->is_Con() || phase->type(oop)->is_zero_type())) {
    // Use the pre-allocated oop for default value types
    set_oop(default_oop(*phase, value_klass()));
    return this;
  } else if (oop->isa_ValueTypePtr()) {
    // Can happen with late inlining
    ValueTypePtrNode* vtptr = oop->as_ValueTypePtr();
    set_oop(vtptr->get_oop());
    for (uint i = Oop+1; i < vtptr->req(); ++i) {
      set_req(i, vtptr->in(i));
    }
    return this;
  }

  if (!is_allocated(phase)) {
    // Save base oop if fields are loaded from memory and the value
    // type is not buffered (in this case we should not use the oop).
    Node* base = is_loaded(phase);
    if (base != NULL) {
      set_oop(base);
      assert(is_allocated(phase), "should now be allocated");
      return this;
    }
  }

  if (can_reshape) {
    PhaseIterGVN* igvn = phase->is_IterGVN();

    if (is_default(*phase)) {
      // Search for users of the default value type
      for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
        Node* user = fast_out(i);
        AllocateNode* alloc = user->isa_Allocate();
        if (alloc != NULL && alloc->result_cast() != NULL && alloc->in(AllocateNode::ValueNode) == this) {
          // Found an allocation of the default value type.
          // If the code in StoreNode::Identity() that removes useless stores was not yet
          // executed or ReduceFieldZeroing is disabled, there can still be initializing
          // stores (only zero-type or default value stores, because value types are immutable).
          Node* res = alloc->result_cast();
          for (DUIterator_Fast jmax, j = res->fast_outs(jmax); j < jmax; j++) {
            AddPNode* addp = res->fast_out(j)->isa_AddP();
            if (addp != NULL) {
              for (DUIterator_Fast kmax, k = addp->fast_outs(kmax); k < kmax; k++) {
                StoreNode* store = addp->fast_out(k)->isa_Store();
                if (store != NULL && store->outcnt() != 0) {
                  // Remove the useless store
                  igvn->replace_in_uses(store, store->in(MemNode::Memory));
                }
              }
            }
          }
          // Replace allocation by pre-allocated oop
          igvn->replace_node(res, default_oop(*phase, value_klass()));
        } else if (user->is_ValueType()) {
          // Add value type user to worklist to give it a chance to get optimized as well
          igvn->_worklist.push(user);
        }
      }
    }

    if (is_allocated(igvn)) {
      // Value type is heap allocated, search for safepoint uses
      for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
        Node* out = fast_out(i);
        if (out->is_SafePoint()) {
          // Let SafePointNode::Ideal() take care of re-wiring the
          // safepoint to the oop input instead of the value type node.
          igvn->rehash_node_delayed(out);
        }
      }
    }
  }
  return NULL;
}

// Search for multiple allocations of this value type
// and try to replace them by dominating allocations.
// Then unlink the value type node and remove it.
void ValueTypeNode::remove_redundant_allocations(PhaseIterGVN* igvn, PhaseIdealLoop* phase) {
  // Search for allocations of this value type
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    AllocateNode* alloc = fast_out(i)->isa_Allocate();
    if (alloc != NULL && alloc->in(AllocateNode::ValueNode) == this) {
      assert(!is_default(*igvn), "default value type allocation");
      Node* res = alloc->result_cast();
      if (res == NULL || !res->is_CheckCastPP()) {
        break; // No unique CheckCastPP
      }
      Node* res_dom = res;
      if (is_allocated(igvn)) {
        // The value type is already allocated but still connected to an AllocateNode.
        // This can happen with late inlining when we first allocate a value type argument
        // but later decide to inline the call with the callee code also allocating.
        res_dom = get_oop();
      } else {
        // Search for a dominating allocation of the same value type
        for (DUIterator_Fast jmax, j = fast_outs(jmax); j < jmax; j++) {
          AllocateNode* alloc_other = fast_out(j)->isa_Allocate();
          if (alloc_other != NULL && alloc_other->in(AllocateNode::ValueNode) == this) {
            Node* res_other = alloc_other->result_cast();
            if (res_other != NULL && res_other->is_CheckCastPP() && res_other != res_dom &&
                phase->is_dominator(res_other->in(0), res_dom->in(0))) {
              res_dom = res_other;
            }
          }
        }
      }
      if (res_dom != res) {
        // Move users to dominating allocation
        igvn->replace_node(res, res_dom);
        // The result of the dominated allocation is now unused and will be removed
        // later in PhaseMacroExpand::eliminate_allocate_node to not confuse loop opts.
        igvn->record_for_igvn(alloc);
      }
    }
  }

  // Process users
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    Node* out = fast_out(i);
    if (out->is_ValueType()) {
      // Recursively process value type users
      out->as_ValueType()->remove_redundant_allocations(igvn, phase);
      --i; --imax;
    } else if (out->isa_Allocate() != NULL) {
      // Unlink AllocateNode
      assert(out->in(AllocateNode::ValueNode) == this, "should be linked");
      igvn->replace_input_of(out, AllocateNode::ValueNode, igvn->C->top());
      --i; --imax;
    } else {
#ifdef ASSERT
      // The value type should not have any other users at this time
      out->dump();
      assert(false, "unexpected user of value type");
#endif
    }
  }
  igvn->remove_dead_node(this);
}
