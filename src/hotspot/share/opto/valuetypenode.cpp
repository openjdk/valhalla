/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
  const TypeValueTypePtr* vtptr = value_type_ptr();
  vtptr = vtptr->cast_to_ptr_type(TypePtr::BotPTR)->is_valuetypeptr();
  PhiNode* oop = PhiNode::make(region, vt->get_oop(), vtptr);
  gvn->set_type(oop, vtptr);
  vt->set_oop(oop);

  // Create a PhiNode each for merging the field values
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* type = vt->field_type(i);
    Node*  value = vt->field_value(i);
    if (type->is_valuetype()) {
      // Handle flattened value type fields recursively
      value = value->as_ValueType()->clone_with_phis(gvn, region);
    } else {
      const Type* phi_type = Type::get_const_type(type);
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
    if (val1->isa_ValueType()) {
      val1->as_ValueType()->merge_with(gvn, val2->as_ValueType(), pnum, transform);
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

int ValueTypeBaseNode::field_offset(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return value_klass()->declared_nonstatic_field_at(index)->offset();
}

ciType* ValueTypeBaseNode::field_type(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return value_klass()->declared_nonstatic_field_at(index)->type();
}

bool ValueTypeBaseNode::field_is_flattened(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return value_klass()->declared_nonstatic_field_at(index)->is_flattened();
}

int ValueTypeBaseNode::make_scalar_in_safepoint(Unique_Node_List& worklist, SafePointNode* sfpt, Node* root, PhaseGVN* gvn) {
  ciValueKlass* vk = value_klass();
  uint nfields = vk->nof_nonstatic_fields();
  JVMState* jvms = sfpt->jvms();
  int start = jvms->debug_start();
  int end   = jvms->debug_end();
  // Replace safepoint edge by SafePointScalarObjectNode and add field values
  assert(jvms != NULL, "missing JVMS");
  uint first_ind = (sfpt->req() - jvms->scloff());
  const TypeValueTypePtr* res_type = value_type_ptr();
  SafePointScalarObjectNode* sobj = new SafePointScalarObjectNode(res_type,
#ifdef ASSERT
                                                                  NULL,
#endif
                                                                  first_ind, nfields);
  sobj->init_req(0, root);
  // Iterate over the value type fields in order of increasing
  // offset and add the field values to the safepoint.
  for (uint j = 0; j < nfields; ++j) {
    int offset = vk->nonstatic_field_at(j)->offset();
    Node* value = field_value_by_offset(offset, true /* include flattened value type fields */);
    if (value->is_ValueType()) {
      if (value->as_ValueType()->is_allocated(gvn)) {
        value = value->as_ValueType()->get_oop();
      } else {
        // Add non-flattened value type field to the worklist to process later
        worklist.push(value);
      }
    }
    sfpt->add_req(value);
  }
  jvms->set_endoff(sfpt->req());
  if (gvn != NULL) {
    sobj = gvn->transform(sobj)->as_SafePointScalarObject();
    gvn->igvn_rehash_node_delayed(sfpt);
  }
  return sfpt->replace_edges_in_range(this, sobj, start, end);
}

void ValueTypeBaseNode::make_scalar_in_safepoints(Node* root, PhaseGVN* gvn) {
  Unique_Node_List worklist;
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    Node* u = fast_out(i);
    if (u->is_SafePoint() && (!u->is_Call() || u->as_Call()->has_debug_use(this))) {
      SafePointNode* sfpt = u->as_SafePoint();
      Node* in_oop = get_oop();
      const Type* oop_type = in_oop->bottom_type();
      assert(Opcode() == Op_ValueTypePtr || !isa_ValueType()->is_allocated(gvn), "already heap allocated value types should be linked directly");
      int nb = make_scalar_in_safepoint(worklist, sfpt, root, gvn);
      --i; imax -= nb;
    }
  }

  for (uint next = 0; next < worklist.size(); ++next) {
    Node* vt = worklist.at(next);
    vt->as_ValueType()->make_scalar_in_safepoints(root, gvn);
  }
}

void ValueTypeBaseNode::make(PhaseGVN* gvn, Node*& ctl, Node* mem, Node* n, ValueTypeBaseNode* vt, ciValueKlass* base_vk, int base_offset, int base_input, bool in) {
  assert(base_offset >= 0, "offset in value type always positive");
  for (uint i = 0; i < vt->field_count(); i++) {
    ciType* field_type = vt->field_type(i);
    int offset = base_offset + vt->field_offset(i);
    if (field_type->is_valuetype() && vt->field_is_flattened(i)) {
      ciValueKlass* embedded_vk = field_type->as_value_klass();
      ValueTypeNode* embedded_vt = ValueTypeNode::make(*gvn, embedded_vk);
      ValueTypeBaseNode::make(gvn, ctl, mem, n, embedded_vt, base_vk, offset - vt->value_klass()->first_field_offset(), base_input, in);
      vt->set_field_value(i, gvn->transform(embedded_vt));
    } else {
      int j = 0; int extra = 0;
      for (; j < base_vk->nof_nonstatic_fields(); j++) {
        ciField* f = base_vk->nonstatic_field_at(j);
        if (offset == f->offset()) {
          assert(f->type() == field_type, "inconsistent field type");
          break;
        }
        BasicType bt = f->type()->basic_type();
        if (bt == T_LONG || bt == T_DOUBLE) {
          extra++;
        }
      }
      assert(j != base_vk->nof_nonstatic_fields(), "must find");
      Node* parm = NULL;
      if (n->is_Start()) {
        assert(in, "return from start?");
        parm = gvn->transform(new ParmNode(n->as_Start(), base_input + j + extra));
      } else {
        if (in) {
          assert(n->is_Call(), "nothing else here");
          parm = n->in(base_input + j + extra);
        } else {
          parm = gvn->transform(new ProjNode(n->as_Call(), base_input + j + extra));
        }
      }
      if (field_type->is_valuetype()) {
        // Non-flattened value type field, check for null
        parm = ValueTypeNode::make(*gvn, ctl, mem, parm, /* null_check */ true);

      }
      vt->set_field_value(i, parm);
      // Record all these guys for later GVN.
      gvn->record_for_igvn(parm);
    }
  }
}

void ValueTypeBaseNode::load(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset) {
  // Initialize the value type by loading its field values from
  // memory and adding the values as input edges to the node.
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    ciType* ftype = field_type(i);
    Node* value = NULL;
    if (ftype->is_valuetype() && field_is_flattened(i)) {
      // Recursively load the flattened value type field
      value = ValueTypeNode::make(gvn, ftype->as_value_klass(), ctl, mem, base, ptr, holder, offset);
    } else {
      const Type* con_type = NULL;
      if (base->is_Con()) {
        // If the oop to the value type is constant (static final field), we can
        // also treat the fields as constants because the value type is immutable.
        const TypeOopPtr* oop_ptr = base->bottom_type()->isa_oopptr();
        ciObject* constant_oop = oop_ptr->const_oop();
        ciField* field = holder->get_field_by_offset(offset, false);
        ciConstant constant = constant_oop->as_instance()->field_value(field);
        con_type = Type::make_from_constant(constant, /*require_const=*/ true);
      }
      if (con_type != NULL) {
        // Found a constant field value
        value = gvn.transform(gvn.makecon(con_type));
        if (con_type->isa_valuetypeptr()) {
          // Constant, non-flattened value type field
          value = ValueTypeNode::make(gvn, ctl, mem, value);
        }
      } else {
        // Load field value from memory
        const Type* base_type = gvn.type(base);
        const TypePtr* adr_type = NULL;
        if (base_type->isa_aryptr()) {
          // In the case of a flattened value type array, each field has its own slice
          adr_type = base_type->is_aryptr()->with_field_offset(offset)->add_offset(Type::OffsetBot);
        } else {
          ciField* field = holder->get_field_by_offset(offset, false);
          adr_type = gvn.C->alias_type(field)->adr_type();
        }
        Node* adr = gvn.transform(new AddPNode(base, ptr, gvn.MakeConX(offset)));
        BasicType bt = type2field[ftype->basic_type()];
        const Type* ft = Type::get_const_type(ftype);
        if (bt == T_VALUETYPE) {
          ft = ft->is_valuetypeptr()->cast_to_ptr_type(TypePtr::BotPTR);
        }
        assert(is_java_primitive(bt) || adr->bottom_type()->is_ptr_to_narrowoop() == UseCompressedOops, "inconsistent");
        value = gvn.transform(LoadNode::make(gvn, NULL, mem, adr, adr_type, ft, bt, MemNode::unordered));
        if (bt == T_VALUETYPE) {
          // Non-flattened value type field, check for null
          value = ValueTypeNode::make(gvn, ctl, mem, value, /* null_check */ true);
        }
      }
    }
    set_field_value(i, value);
  }
}

void ValueTypeBaseNode::store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset) const {
  // The value type is embedded into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when storing the values.
  holder_offset -= value_klass()->first_field_offset();
  store(kit, base, ptr, holder, holder_offset);
}

void ValueTypeBaseNode::store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset) const {
  if (holder == NULL) {
    holder = value_klass();
  }
  // Write field values to memory
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    if (value->is_ValueType() && field_is_flattened(i)) {
      // Recursively store the flattened value type field
      value->isa_ValueType()->store_flattened(kit, base, ptr, holder, offset);
    } else {
      const Type* base_type = kit->gvn().type(base);
      const TypePtr* adr_type = NULL;
      if (base_type->isa_aryptr()) {
        // In the case of a flattened value type array, each field has its own slice
        adr_type = base_type->is_aryptr()->with_field_offset(offset)->add_offset(Type::OffsetBot);
      } else {
        ciField* field = holder->get_field_by_offset(offset, false);
        adr_type = kit->C->alias_type(field)->adr_type();
      }
      Node* adr = kit->basic_plus_adr(base, ptr, offset);
      BasicType bt = type2field[field_type(i)->basic_type()];
      if (is_java_primitive(bt)) {
        kit->store_to_memory(kit->control(), adr, value, bt, adr_type, MemNode::unordered);
      } else {
        const TypeOopPtr* ft = TypeOopPtr::make_from_klass(field_type(i)->as_klass());
        // Field may be NULL
        ft = ft->cast_to_ptr_type(TypePtr::BotPTR)->is_oopptr();
        assert(adr->bottom_type()->is_ptr_to_narrowoop() == UseCompressedOops, "inconsistent");
        bool is_array = base_type->isa_aryptr() != NULL;
        kit->store_oop(kit->control(), base, adr, adr_type, value, ft, bt, is_array, MemNode::unordered);
      }
    }
  }
}

ValueTypeBaseNode* ValueTypeBaseNode::allocate(GraphKit* kit) {
  Node* in_oop = get_oop();
  Node* null_ctl = kit->top();
  // Check if value type is already allocated
  Node* not_null_oop = kit->null_check_oop(in_oop, &null_ctl);
  if (null_ctl->is_top()) {
    // Value type is allocated
    return this;
  }
  // Not able to prove that value type is allocated.
  // Emit runtime check that may be folded later.
  assert(!is_allocated(&kit->gvn()), "should not be allocated");
  const TypeValueTypePtr* vtptr_type = bottom_type()->isa_valuetypeptr();
  if (vtptr_type == NULL) {
    vtptr_type = TypeValueTypePtr::make(bottom_type()->isa_valuetype(), TypePtr::NotNull);
  }
  RegionNode* region = new RegionNode(3);
  PhiNode* oop = new PhiNode(region, vtptr_type);
  PhiNode* io  = new PhiNode(region, Type::ABIO);
  PhiNode* mem = new PhiNode(region, Type::MEMORY, TypePtr::BOTTOM);

  // Oop is non-NULL, use it
  region->init_req(1, kit->control());
  oop   ->init_req(1, not_null_oop);
  io    ->init_req(1, kit->i_o());
  mem   ->init_req(1, kit->merged_memory());

  // Oop is NULL, allocate value type
  kit->set_control(null_ctl);
  kit->kill_dead_locals();
  ciValueKlass* vk = value_klass();
  Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
  Node* alloc_oop  = kit->new_instance(klass_node, NULL, NULL, false, this);
  // Write field values to memory
  store(kit, alloc_oop, alloc_oop, vk);
  region->init_req(2, kit->control());
  oop   ->init_req(2, alloc_oop);
  io    ->init_req(2, kit->i_o());
  mem   ->init_req(2, kit->merged_memory());

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
  kit->replace_in_map(this, vt);
  return vt;
}

bool ValueTypeBaseNode::is_allocated(PhaseGVN* phase) const {
  Node* oop = get_oop();
  const Type* oop_type = (phase != NULL) ? phase->type(oop) : oop->bottom_type();
  return oop_type->meet(TypePtr::NULL_PTR) != oop_type;
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
        assert(f->is_flattened(), "should be flattened");
        field = field->as_ValueType()->allocate(kit)->get_oop();
      }
      C->gvn_replace_by(pn, field);
      C->initial_gvn()->hash_delete(pn);
      pn->set_req(0, C->top());
      --i; --imax;
    }
  }
}

ValueTypeNode* ValueTypeNode::make(PhaseGVN& gvn, ciValueKlass* klass) {
  // Create a new ValueTypeNode with uninitialized values and NULL oop
  const TypeValueType* type = TypeValueType::make(klass);
  return new ValueTypeNode(type, gvn.zerocon(T_VALUETYPE));
}

Node* ValueTypeNode::make_default(PhaseGVN& gvn, ciValueKlass* vk) {
  // TODO re-use constant oop of pre-allocated default value type here?
  // Create a new ValueTypeNode with default values
  ValueTypeNode* vt = ValueTypeNode::make(gvn, vk);
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* field_type = vt->field_type(i);
    Node* value = NULL;
    if (field_type->is_valuetype()) {
      value = ValueTypeNode::make_default(gvn, field_type->as_value_klass());
    } else {
      value = gvn.zerocon(field_type->basic_type());
    }
    vt->set_field_value(i, value);
  }
  return gvn.transform(vt);
}

Node* ValueTypeNode::make(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* oop, bool null_check) {
  // Create and initialize a ValueTypeNode by loading all field
  // values from a heap-allocated version and also save the oop.
  const TypeValueType* type = gvn.type(oop)->is_valuetypeptr()->value_type();
  ValueTypeNode* vt = new ValueTypeNode(type, oop);

  if (null_check && !vt->is_allocated(&gvn)) {
    // Add oop null check
    Node* chk = gvn.transform(new CmpPNode(oop, gvn.zerocon(T_VALUETYPE)));
    Node* tst = gvn.transform(new BoolNode(chk, BoolTest::ne));
    IfNode* iff = gvn.transform(new IfNode(ctl, tst, PROB_MAX, COUNT_UNKNOWN))->as_If();
    Node* not_null = gvn.transform(new IfTrueNode(iff));
    Node* null = gvn.transform(new IfFalseNode(iff));
    Node* region = new RegionNode(3);

    // Load value type from memory if oop is non-null
    oop = new CastPPNode(oop, TypePtr::NOTNULL);
    oop->set_req(0, not_null);
    oop = gvn.transform(oop);
    vt->load(gvn, not_null, mem, oop, oop, type->value_klass());
    region->init_req(1, not_null);

    // Use default value type if oop is null
    Node* def = make_default(gvn, type->value_klass());
    region->init_req(2, null);

    // Merge the two value types and update control
    vt = vt->clone_with_phis(&gvn, region)->as_ValueType();
    vt->merge_with(&gvn, def->as_ValueType(), 2, true);
    ctl = gvn.transform(region);
  } else {
    Node* init_ctl = ctl;
    vt->load(gvn, ctl, mem, oop, oop, type->value_klass());
    vt = gvn.transform(vt)->as_ValueType();
    assert(vt->is_allocated(&gvn), "value type should be allocated");
    assert(init_ctl != ctl || oop->is_Con() || oop->is_CheckCastPP() || oop->Opcode() == Op_ValueTypePtr ||
           vt->is_loaded(&gvn, type) == oop, "value type should be loaded");
  }
  return vt;
}

Node* ValueTypeNode::make(GraphKit* kit, Node* oop, bool null_check) {
  Node* ctl = kit->control();
  Node* vt = make(kit->gvn(), ctl, kit->merged_memory(), oop, null_check);
  kit->set_control(ctl);
  return vt;
}

Node* ValueTypeNode::make(PhaseGVN& gvn, ciValueKlass* vk, Node*& ctl, Node* mem, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset) {
  // Create and initialize a ValueTypeNode by loading all field values from
  // a flattened value type field at 'holder_offset' or from a value type array.
  ValueTypeNode* vt = make(gvn, vk);
  // The value type is flattened into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when loading the values.
  holder_offset -= vk->first_field_offset();
  vt->load(gvn, ctl, mem, obj, ptr, holder, holder_offset);
  assert(vt->is_loaded(&gvn, vt->type()->isa_valuetype()) != obj, "holder oop should not be used as flattened value type oop");
  return gvn.transform(vt)->as_ValueType();
}

Node* ValueTypeNode::make(GraphKit* kit, ciValueKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset) {
  Node* ctl = kit->control();
  Node* vt = make(kit->gvn(), vk, ctl, kit->merged_memory(), obj, ptr, holder, holder_offset);
  kit->set_control(ctl);
  return vt;
}

Node* ValueTypeNode::make(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* n, ciValueKlass* vk, int base_input, bool in) {
  ValueTypeNode* vt = ValueTypeNode::make(gvn, vk);
  ValueTypeBaseNode::make(&gvn, ctl, mem, n, vt, vk, 0, base_input, in);
  return gvn.transform(vt);
}

Node* ValueTypeNode::is_loaded(PhaseGVN* phase, const TypeValueType* t, Node* base, int holder_offset) {
  if (field_count() == 0) {
    assert(t->value_klass()->is__Value(), "unexpected value type klass");
    assert(is_allocated(phase), "must be allocated");
    return get_oop();
  }
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
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
        const TypeValueTypePtr* vtptr = phase->type(base)->isa_valuetypeptr();
        if (vtptr == NULL || !vtptr->value_type()->eq(t)) {
          return NULL;
        }
      }
    } else if (value->isa_ValueType()) {
      // Check value type field load recursively
      ValueTypeNode* vt = value->as_ValueType();
      base = vt->is_loaded(phase, t, base, offset - vt->value_klass()->first_field_offset());
      if (base == NULL) {
        return NULL;
      }
    } else {
      return NULL;
    }
  }
  return base;
}

Node* ValueTypeNode::allocate_fields(GraphKit* kit) {
  ValueTypeNode* vt = clone()->as_ValueType();
  for (uint i = 0; i < field_count(); i++) {
    Node* value = field_value(i);
    if (value->is_ValueType()) {
      if (field_is_flattened(i)) {
        value = value->as_ValueType()->allocate_fields(kit);
      } else {
        // Non-flattened value type field
        value = value->as_ValueType()->allocate(kit);
      }
      vt->set_field_value(i, value);
    }
  }
  vt = kit->gvn().transform(vt)->as_ValueType();
  kit->replace_in_map(this, vt);
  return vt;
}

Node* ValueTypeNode::tagged_klass(PhaseGVN& gvn) {
  ciValueKlass* vk = value_klass();
  const TypeKlassPtr* tk = TypeKlassPtr::make(vk);
  intptr_t bits = tk->get_con();
  set_nth_bit(bits, 0);
  return gvn.makecon(TypeRawPtr::make((address)bits));
}

void ValueTypeNode::pass_klass(Node* n, uint pos, const GraphKit& kit) {
  n->init_req(pos, tagged_klass(kit.gvn()));
}

uint ValueTypeNode::pass_fields(Node* n, int base_input, GraphKit& kit, bool assert_allocated, ciValueKlass* base_vk, int base_offset) {
  ciValueKlass* vk = value_klass();
  if (base_vk == NULL) {
    base_vk = vk;
  }
  uint edges = 0;
  for (uint i = 0; i < field_count(); i++) {
    ciType* f_type = field_type(i);
    int offset = base_offset + field_offset(i) - (base_offset > 0 ? vk->first_field_offset() : 0);
    Node* arg = field_value(i);
    if (f_type->is_valuetype() && field_is_flattened(i)) {
      ciValueKlass* embedded_vk = f_type->as_value_klass();
      edges += arg->as_ValueType()->pass_fields(n, base_input, kit, assert_allocated, base_vk, offset);
    } else {
      int j = 0; int extra = 0;
      for (; j < base_vk->nof_nonstatic_fields(); j++) {
        ciField* f = base_vk->nonstatic_field_at(j);
        if (offset == f->offset()) {
          assert(f->type() == f_type, "inconsistent field type");
          break;
        }
        BasicType bt = f->type()->basic_type();
        if (bt == T_LONG || bt == T_DOUBLE) {
          extra++;
        }
      }
      if (arg->is_ValueType()) {
        // non-flattened value type field
        ValueTypeNode* vt = arg->as_ValueType();
        assert(!assert_allocated || vt->is_allocated(&kit.gvn()), "value type field should be allocated");
        arg = vt->allocate(&kit)->get_oop();
      }
      n->init_req(base_input + j + extra, arg);
      edges++;
      BasicType bt = f_type->basic_type();
      if (bt == T_LONG || bt == T_DOUBLE) {
        n->init_req(base_input + j + extra + 1, kit.top());
        edges++;
      }
    }
  }
  return edges;
}

Node* ValueTypeNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  if (!is_allocated(phase)) {
    // Check if this value type is loaded from memory
    Node* base = is_loaded(phase, type()->is_valuetype());
    if (base != NULL) {
      // Save the oop
      set_oop(base);
      assert(is_allocated(phase), "should now be allocated");
      return this;
    }
  }

  if (can_reshape) {
    PhaseIterGVN* igvn = phase->is_IterGVN();
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
void ValueTypeNode::remove_redundant_allocations(PhaseIterGVN* igvn, PhaseIdealLoop* phase) {
  assert(EliminateAllocations, "allocation elimination should be enabled");
  Node_List dead_allocations;
  // Search for allocations of this value type
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    AllocateNode* alloc = fast_out(i)->isa_Allocate();
    if (alloc != NULL && alloc->result_cast() != NULL && alloc->in(AllocateNode::ValueNode) == this) {
      Node* res_dom = NULL;
      if (is_allocated(igvn)) {
        // The value type is already allocated but still connected to an AllocateNode.
        // This can happen with late inlining when we first allocate a value type argument
        // but later decide to inline the call with the callee code also allocating.
        res_dom = get_oop();
      } else {
        // Search for a dominating allocation of the same value type
        for (DUIterator_Fast jmax, j = fast_outs(jmax); j < jmax; j++) {
          Node* out2 = fast_out(j);
          if (alloc != out2 && out2->is_Allocate() && out2->in(AllocateNode::ValueNode) == this &&
              phase->is_dominator(out2, alloc)) {
            AllocateNode* alloc_dom =  out2->as_Allocate();
            assert(alloc->in(AllocateNode::KlassNode) == alloc_dom->in(AllocateNode::KlassNode), "klasses should match");
            res_dom = alloc_dom->result_cast();
            break;
          }
        }
      }
      if (res_dom != NULL) {
        // Move users to dominating allocation
        Node* res = alloc->result_cast();
        igvn->replace_node(res, res_dom);
        // The dominated allocation is now dead, remove the
        // value type node connection and adjust the iterator.
        dead_allocations.push(alloc);
        igvn->replace_input_of(alloc, AllocateNode::ValueNode, NULL);
        --i; --imax;
#ifdef ASSERT
        if (PrintEliminateAllocations) {
          tty->print("++++ Eliminated: %d Allocate ", alloc->_idx);
          dump_spec(tty);
          tty->cr();
        }
#endif
      }
    }
  }

  // Remove dead value type allocations by replacing the projection nodes
  for (uint i = 0; i < dead_allocations.size(); ++i) {
    CallProjections projs;
    AllocateNode* alloc = dead_allocations.at(i)->as_Allocate();
    alloc->extract_projections(&projs, true);
    // Use lazy_replace to avoid corrupting the dominator tree of PhaseIdealLoop
    phase->lazy_replace(projs.fallthrough_catchproj, alloc->in(TypeFunc::Control));
    phase->lazy_replace(projs.fallthrough_memproj, alloc->in(TypeFunc::Memory));
    phase->lazy_replace(projs.catchall_memproj, phase->C->top());
    phase->lazy_replace(projs.fallthrough_ioproj, alloc->in(TypeFunc::I_O));
    phase->lazy_replace(projs.catchall_ioproj, phase->C->top());
    phase->lazy_replace(projs.catchall_catchproj, phase->C->top());
    phase->lazy_replace(projs.resproj, phase->C->top());
  }
}

#ifndef PRODUCT

void ValueTypeNode::dump_spec(outputStream* st) const {
  TypeNode::dump_spec(st);
}

#endif

ValueTypePtrNode* ValueTypePtrNode::make(GraphKit* kit, ciValueKlass* vk, CallNode* call) {
  ValueTypePtrNode* vt = new ValueTypePtrNode(vk, kit->zerocon(T_VALUETYPE), kit->C);
  Node* ctl = kit->control();
  ValueTypeBaseNode::make(&kit->gvn(), ctl, kit->merged_memory(), call, vt, vk, 0, TypeFunc::Parms+1, false);
  kit->set_control(ctl);
  return vt;
}

ValueTypePtrNode* ValueTypePtrNode::make(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* oop) {
  // Create and initialize a ValueTypePtrNode by loading all field
  // values from a heap-allocated version and also save the oop.
  ciValueKlass* vk = gvn.type(oop)->is_valuetypeptr()->value_type()->value_klass();
  ValueTypePtrNode* vtptr = new ValueTypePtrNode(vk, oop, gvn.C);
  vtptr->load(gvn, ctl, mem, oop, oop, vk);
  return vtptr;
}
