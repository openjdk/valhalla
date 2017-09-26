/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
  if (recursive && value->is_ValueType()) {
    // Flattened value type field
    ValueTypeNode* vt = value->as_ValueType();
    sub_offset += vt->value_klass()->first_field_offset(); // Add header size
    return vt->field_value_by_offset(sub_offset);
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
  return value_klass()->field_offset_by_index(index);
}

ciType* ValueTypeBaseNode::field_type(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return value_klass()->field_type_by_index(index);
}

int ValueTypeBaseNode::make_scalar_in_safepoint(SafePointNode* sfpt, Node* root, PhaseGVN* gvn) {
  ciValueKlass* vk = value_klass();
  uint nfields = vk->flattened_field_count();
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
    assert(value != NULL, "");
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
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    Node* u = fast_out(i);
    if (u->is_SafePoint() && (!u->is_Call() || u->as_Call()->has_debug_use(this))) {
      SafePointNode* sfpt = u->as_SafePoint();
      Node* in_oop = get_oop();
      const Type* oop_type = in_oop->bottom_type();
      assert(Opcode() == Op_ValueTypePtr || TypePtr::NULL_PTR->higher_equal(oop_type), "already heap allocated value type should be linked directly");
      int nb = make_scalar_in_safepoint(sfpt, root, gvn);
      --i; imax -= nb;
    }
  }
}

void ValueTypeBaseNode::make(PhaseGVN* gvn, Node* n, ValueTypeBaseNode* vt, ciValueKlass* base_vk, int base_offset, int base_input, bool in) {
  assert(base_offset >= 0, "offset in value type always positive");
  for (uint i = 0; i < vt->field_count(); i++) {
    ciType* field_type = vt->field_type(i);
    int offset = base_offset + vt->field_offset(i);
    if (field_type->is_valuetype()) {
      ciValueKlass* embedded_vk = field_type->as_value_klass();
      ValueTypeNode* embedded_vt = ValueTypeNode::make(*gvn, embedded_vk);
      ValueTypeBaseNode::make(gvn, n, embedded_vt, base_vk, offset - vt->value_klass()->first_field_offset(), base_input, in);
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
      vt->set_field_value(i, parm);
      // Record all these guys for later GVN.
      gvn->record_for_igvn(parm);
    }
  }
}

void ValueTypeBaseNode::load(PhaseGVN& gvn, Node* mem, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset) {
  // Initialize the value type by loading its field values from
  // memory and adding the values as input edges to the node.
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    ciType* ftype = field_type(i);
    Node* value = NULL;
    if (ftype->is_valuetype()) {
      // Recursively load the flattened value type field
      value = ValueTypeNode::make(gvn, ftype->as_value_klass(), mem, base, ptr, holder, offset);
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
        value = gvn.makecon(con_type);
      } else {
        // Load field value from memory
        const Type* base_type = gvn.type(base);
        const TypePtr* adr_type = NULL;
        if (base_type->isa_aryptr()) {
          // In the case of a flattened value type array, each field
          // has its own slice
          adr_type = base_type->is_aryptr()->with_field_offset(offset)->add_offset(Type::OffsetBot);
        } else {
          ciField* field = holder->get_field_by_offset(offset, false);
          adr_type = gvn.C->alias_type(field)->adr_type();
        }
        Node* adr = gvn.transform(new AddPNode(base, ptr, gvn.MakeConX(offset)));
        BasicType bt = type2field[ftype->basic_type()];
        value = LoadNode::make(gvn, NULL, mem, adr, adr_type, Type::get_const_type(ftype), bt, MemNode::unordered);
      }
    }
    set_field_value(i, gvn.transform(value));
  }
}

void ValueTypeBaseNode::store_flattened(PhaseGVN* gvn, Node* ctl, MergeMemNode* mem, Node* base, ciValueKlass* holder, int holder_offset) const {
  // The value type is embedded into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when storing the values.
  holder_offset -= value_klass()->first_field_offset();
  store(gvn, ctl, mem, base, holder, holder_offset);
}

void ValueTypeBaseNode::store(PhaseGVN* gvn, Node* ctl, MergeMemNode* mem, Node* base, ciValueKlass* holder, int holder_offset) const {
  if (holder == NULL) {
    holder = value_klass();
  }
  // Write field values to memory
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    if (value->is_ValueType()) {
      // Recursively store the flattened value type field
      value->isa_ValueTypeBase()->store_flattened(gvn, ctl, mem, base, holder, offset);
    } else {
      const Type* base_type = gvn->type(base);
      const TypePtr* adr_type = NULL;
      if (base_type->isa_aryptr()) {
        // In the case of a flattened value type array, each field has its own slice
        adr_type = base_type->is_aryptr()->with_field_offset(offset)->add_offset(Type::OffsetBot);
      } else {
        ciField* field = holder->get_field_by_offset(offset, false);
        adr_type = gvn->C->alias_type(field)->adr_type();
      }
      Node* adr = gvn->transform(new AddPNode(base, base, gvn->MakeConX(offset)));
      BasicType bt = type2field[field_type(i)->basic_type()];
      uint alias_idx = gvn->C->get_alias_index(adr_type);
      Node* st = StoreNode::make(*gvn, ctl, mem->memory_at(alias_idx), adr, adr_type, value, bt, MemNode::unordered);
      mem->set_memory_at(alias_idx, gvn->transform(st));
    }
  }
}

// When a call returns multiple values, it has several result
// projections, one per field. Replacing the result of the call by a
// value type node (after late inlining) requires that for each result
// projection, we find the corresponding value type field.
void ValueTypeBaseNode::replace_call_results(Node* call, Compile* C) {
  ciValueKlass* vk = value_klass();
  for (DUIterator_Fast imax, i = call->fast_outs(imax); i < imax; i++) {
    ProjNode *pn = call->fast_out(i)->as_Proj();
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

      C->gvn_replace_by(pn, field);
      C->initial_gvn()->hash_delete(pn);
      pn->set_req(0, C->top());
      --i; --imax;
    }
  }
}

Node* ValueTypeBaseNode::allocate(const Type* type, Node*& ctl, Node*& mem, Node*& io, Node* frameptr, Node*& ex_ctl, Node*& ex_mem, Node*& ex_io, JVMState* jvms, PhaseIterGVN *igvn) {
  ciValueKlass* vk = type->is_valuetypeptr()->value_type()->value_klass();
  Node* initial_mem = mem;
  uint last = igvn->C->unique();
  MergeMemNode* all_mem = MergeMemNode::make(mem);
  jint lhelper = vk->layout_helper();
  assert(lhelper != Klass::_lh_neutral_value, "unsupported");

  AllocateNode* alloc = new AllocateNode(igvn->C,
                                         AllocateNode::alloc_type(Type::TOP),
                                         ctl,
                                         mem,
                                         io,
                                         igvn->MakeConX(Klass::layout_helper_size_in_bytes(lhelper)),
                                         igvn->makecon(TypeKlassPtr::make(vk)),
                                         igvn->intcon(0),
                                         NULL);
  alloc->set_req(TypeFunc::FramePtr, frameptr);
  igvn->C->add_safepoint_edges(alloc, jvms);
  Node* n = igvn->transform(alloc);
  assert(n == alloc, "node shouldn't go away");

  ctl = igvn->transform(new ProjNode(alloc, TypeFunc::Control));
  mem = igvn->transform(new ProjNode(alloc, TypeFunc::Memory, true));
  all_mem->set_memory_at(Compile::AliasIdxRaw, mem);

  io = igvn->transform(new ProjNode(alloc, TypeFunc::I_O, true));
  Node* catc = igvn->transform(new CatchNode(ctl, io, 2));
  Node* norm = igvn->transform(new CatchProjNode(catc, CatchProjNode::fall_through_index, CatchProjNode::no_handler_bci));
  Node* excp = igvn->transform(new CatchProjNode(catc, CatchProjNode::catch_all_index,    CatchProjNode::no_handler_bci));

  ex_ctl = excp;
  ex_mem = igvn->transform(all_mem);
  ex_io = io;

  ctl = norm;
  mem = igvn->transform(new ProjNode(alloc, TypeFunc::Memory));
  io = igvn->transform(new ProjNode(alloc, TypeFunc::I_O, false));
  Node* rawoop = igvn->transform(new ProjNode(alloc, TypeFunc::Parms));

  MemBarNode* membar = MemBarNode::make(igvn->C, Op_Initialize, Compile::AliasIdxRaw, rawoop);
  membar->set_req(TypeFunc::Control, ctl);

  InitializeNode* init = membar->as_Initialize();

  const TypeOopPtr* oop_type = type->is_oopptr();
  MergeMemNode* minit_in = MergeMemNode::make(mem);
  init->set_req(InitializeNode::Memory, minit_in);
  n = igvn->transform(membar);
  assert(n == membar, "node shouldn't go away");
  ctl = igvn->transform(new ProjNode(membar, TypeFunc::Control));
  mem = igvn->transform(new ProjNode(membar, TypeFunc::Memory));

  MergeMemNode* out_mem_merge = MergeMemNode::make(initial_mem);
  for (int i = 0, len = vk->nof_nonstatic_fields(); i < len; i++) {
    ciField* field = vk->nonstatic_field_at(i);
    if (field->offset() >= TrackedInitializationLimit * HeapWordSize)
      continue;
    int fieldidx = igvn->C->alias_type(field)->index();
    minit_in->set_memory_at(fieldidx, initial_mem);
    out_mem_merge->set_memory_at(fieldidx, mem);
  }

  n = igvn->transform(minit_in);
  assert(n == minit_in, "node shouldn't go away");
  out_mem_merge->set_memory_at(Compile::AliasIdxRaw, mem);

  Node* javaoop = igvn->transform(new CheckCastPPNode(ctl, rawoop, oop_type));
  mem = igvn->transform(out_mem_merge);

  return javaoop;
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

Node* ValueTypeNode::make(PhaseGVN& gvn, Node* mem, Node* oop) {
  // Create and initialize a ValueTypeNode by loading all field
  // values from a heap-allocated version and also save the oop.
  const TypeValueType* type = gvn.type(oop)->is_valuetypeptr()->value_type();
  ValueTypeNode* vt = new ValueTypeNode(type, oop);
  vt->load(gvn, mem, oop, oop, type->value_klass());
  assert(vt->is_allocated(&gvn), "value type should be allocated");
  assert(oop->is_Con() || oop->is_CheckCastPP() || oop->Opcode() == Op_ValueTypePtr || vt->is_loaded(&gvn, type) == oop, "value type should be loaded");
  return gvn.transform(vt);
}

Node* ValueTypeNode::make(PhaseGVN& gvn, ciValueKlass* vk, Node* mem, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset) {
  // Create and initialize a ValueTypeNode by loading all field values from
  // a flattened value type field at 'holder_offset' or from a value type array.
  ValueTypeNode* vt = make(gvn, vk);
  // The value type is flattened into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when loading the values.
  holder_offset -= vk->first_field_offset();
  vt->load(gvn, mem, obj, ptr, holder, holder_offset);
  assert(vt->is_loaded(&gvn, vt->type()->isa_valuetype()) != obj, "holder oop should not be used as flattened value type oop");
  return gvn.transform(vt)->as_ValueType();
}

Node* ValueTypeNode::make(PhaseGVN& gvn, Node* n, ciValueKlass* vk, int base_input, bool in) {
  ValueTypeNode* vt = ValueTypeNode::make(gvn, vk);
  ValueTypeBaseNode::make(&gvn, n, vt, vk, 0, base_input, in);
  return gvn.transform(vt);
}

Node* ValueTypeNode::is_loaded(PhaseGVN* phase, const TypeValueType* t, Node* base, int holder_offset) {
  if (field_count() == 0) {
    assert(t->value_klass() == phase->C->env()->___Value_klass(), "unexpected value type klass");
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

void ValueTypeNode::store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset) const {
  // The value type is embedded into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when storing the values.
  holder_offset -= value_klass()->first_field_offset();
  store(kit, base, ptr, holder, holder_offset);
}

void ValueTypeNode::store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset) const {
  // Write field values to memory
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    if (value->is_ValueType()) {
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
        assert(adr->bottom_type()->is_ptr_to_narrowoop() == UseCompressedOops, "inconsistent");
        bool is_array = base_type->isa_aryptr() != NULL;
        kit->store_oop(kit->control(), base, adr, adr_type, value, ft, bt, is_array, MemNode::unordered);
      }
    }
  }
}

Node* ValueTypeNode::allocate(GraphKit* kit) {
  Node* in_oop = get_oop();
  Node* null_ctl = kit->top();
  // Check if value type is already allocated
  Node* not_null_oop = kit->null_check_oop(in_oop, &null_ctl);
  if (null_ctl->is_top()) {
    // Value type is allocated
    return not_null_oop;
  }
  // Not able to prove that value type is allocated.
  // Emit runtime check that may be folded later.
  assert(!is_allocated(&kit->gvn()), "should not be allocated");
  const TypeValueTypePtr* vtptr_type = TypeValueTypePtr::make(bottom_type()->isa_valuetype(), TypePtr::NotNull);
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
  ValueTypeNode* vt = clone()->as_ValueType();
  vt->set_oop(res_oop);
  kit->replace_in_map(this, kit->gvn().transform(vt));
  return res_oop;
}

bool ValueTypeNode::is_allocated(PhaseGVN* phase) const {
  const Type* oop_type = phase->type(get_oop());
  return oop_type->meet(TypePtr::NULL_PTR) != oop_type;
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

uint ValueTypeNode::pass_fields(Node* n, int base_input, const GraphKit& kit, ciValueKlass* base_vk, int base_offset) {
  ciValueKlass* vk = value_klass();
  if (base_vk == NULL) {
    base_vk = vk;
  }
  uint edges = 0;
  for (uint i = 0; i < field_count(); i++) {
    ciType* f_type = field_type(i);
    int offset = base_offset + field_offset(i) - (base_offset > 0 ? vk->first_field_offset() : 0);
    Node* arg = field_value(i);
    if (f_type->is_valuetype()) {
      ciValueKlass* embedded_vk = f_type->as_value_klass();
      edges += arg->as_ValueType()->pass_fields(n, base_input, kit, base_vk, offset);
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
    Node* out1 = fast_out(i);
    if (out1->is_Allocate() && out1->in(AllocateNode::ValueNode) == this) {
      AllocateNode* alloc = out1->as_Allocate();
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
        // Found a dominating allocation
        Node* res = alloc->result_cast();
        assert(res != NULL, "value type allocation should not be dead");
        // Move users to dominating allocation
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

ValueTypePtrNode* ValueTypePtrNode::make(PhaseGVN* gvn, CheckCastPPNode* cast) {
  ciValueKlass* vk = cast->type()->is_valuetypeptr()->value_type()->value_klass();
  ValueTypePtrNode* vt = new ValueTypePtrNode(vk, gvn->C);
  assert(cast->in(1)->is_Proj(), "bad graph shape");
  ValueTypeBaseNode::make(gvn, cast->in(1)->in(0), vt, vk, 0, TypeFunc::Parms+1, false);
  return vt;
}

ValueTypePtrNode* ValueTypePtrNode::make(PhaseGVN& gvn, Node* mem, Node* oop) {
  // Create and initialize a ValueTypePtrNode by loading all field
  // values from a heap-allocated version and also save the oop.
  ciValueKlass* vk = gvn.type(oop)->is_valuetypeptr()->value_type()->value_klass();
  ValueTypePtrNode* vtptr = new ValueTypePtrNode(vk, gvn.C);
  vtptr->set_oop(oop);
  vtptr->load(gvn, mem, oop, oop, vk);
  return vtptr;
}
