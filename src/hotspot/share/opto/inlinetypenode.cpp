/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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
InlineTypeNode* InlineTypeNode::clone_with_phis(PhaseGVN* gvn, Node* region, bool is_init) {
  InlineTypeNode* vt = clone()->as_InlineType();
  const Type* t = Type::get_const_type(inline_klass());
  gvn->set_type(vt, t);
  vt->as_InlineType()->set_type(t);

  // Create a PhiNode for merging the oop values
  PhiNode* oop = PhiNode::make(region, vt->get_oop(), t);
  gvn->set_type(oop, t);
  gvn->record_for_igvn(oop);
  vt->set_oop(*gvn, oop);

  // Create a PhiNode for merging the is_buffered values
  t = Type::get_const_basic_type(T_BOOLEAN);
  Node* is_buffered_node = PhiNode::make(region, vt->get_is_buffered(), t);
  gvn->set_type(is_buffered_node, t);
  gvn->record_for_igvn(is_buffered_node);
  vt->set_req(IsBuffered, is_buffered_node);

  // Create a PhiNode for merging the is_init values
  Node* is_init_node;
  if (is_init) {
    is_init_node = gvn->intcon(1);
  } else {
    t = Type::get_const_basic_type(T_BOOLEAN);
    is_init_node = PhiNode::make(region, vt->get_is_init(), t);
    gvn->set_type(is_init_node, t);
    gvn->record_for_igvn(is_init_node);
  }
  vt->set_req(IsInit, is_init_node);

  // Create a PhiNode each for merging the field values
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* type = vt->field_type(i);
    Node*  value = vt->field_value(i);
    // We limit scalarization for inline types with circular fields and can therefore observe nodes
    // of the same type but with different scalarization depth during GVN. To avoid inconsistencies
    // during merging, make sure that we only create Phis for fields that are guaranteed to be scalarized.
    bool no_circularity = !gvn->C->has_circular_inline_type() || field_is_flat(i);
    if (value->is_InlineType() && no_circularity) {
      // Handle inline type fields recursively
      value = value->as_InlineType()->clone_with_phis(gvn, region);
    } else {
      t = Type::get_const_type(type);
      value = PhiNode::make(region, value, t);
      gvn->set_type(value, t);
      gvn->record_for_igvn(value);
    }
    vt->set_field_value(i, value);
  }
  gvn->record_for_igvn(vt);
  return vt;
}

// Checks if the inputs of the InlineTypeNode were replaced by PhiNodes
// for the given region (see InlineTypeNode::clone_with_phis).
bool InlineTypeNode::has_phi_inputs(Node* region) {
  // Check oop input
  bool result = get_oop()->is_Phi() && get_oop()->as_Phi()->region() == region;
#ifdef ASSERT
  if (result) {
    // Check all field value inputs for consistency
    for (uint i = Values; i < field_count(); ++i) {
      Node* n = in(i);
      if (n->is_InlineType()) {
        assert(n->as_InlineType()->has_phi_inputs(region), "inconsistent phi inputs");
      } else {
        assert(n->is_Phi() && n->as_Phi()->region() == region, "inconsistent phi inputs");
      }
    }
  }
#endif
  return result;
}

// Merges 'this' with 'other' by updating the input PhiNodes added by 'clone_with_phis'
InlineTypeNode* InlineTypeNode::merge_with(PhaseGVN* gvn, const InlineTypeNode* other, int pnum, bool transform) {
  // Merge oop inputs
  PhiNode* phi = get_oop()->as_Phi();
  phi->set_req(pnum, other->get_oop());
  if (transform) {
    set_oop(*gvn, gvn->transform(phi));
  }

  // Merge is_buffered inputs
  phi = get_is_buffered()->as_Phi();
  phi->set_req(pnum, other->get_is_buffered());
  if (transform) {
    set_req(IsBuffered, gvn->transform(phi));
  }

  // Merge is_init inputs
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
    if (val1->is_InlineType()) {
      if (val2->is_Phi()) {
        val2 = gvn->transform(val2);
      }
      val1->as_InlineType()->merge_with(gvn, val2->as_InlineType(), pnum, transform);
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
void InlineTypeNode::add_new_path(Node* region) {
  assert(has_phi_inputs(region), "must have phi inputs");

  PhiNode* phi = get_oop()->as_Phi();
  phi->add_req(nullptr);
  assert(phi->req() == region->req(), "must be same size as region");

  phi = get_is_buffered()->as_Phi();
  phi->add_req(nullptr);
  assert(phi->req() == region->req(), "must be same size as region");

  phi = get_is_init()->as_Phi();
  phi->add_req(nullptr);
  assert(phi->req() == region->req(), "must be same size as region");

  for (uint i = 0; i < field_count(); ++i) {
    Node* val = field_value(i);
    if (val->is_InlineType()) {
      val->as_InlineType()->add_new_path(region);
    } else {
      val->as_Phi()->add_req(nullptr);
      assert(val->req() == region->req(), "must be same size as region");
    }
  }
}

Node* InlineTypeNode::field_value(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return in(Values + index);
}

// Get the value of the field at the given offset.
// If 'recursive' is true, flat inline type fields will be resolved recursively.
Node* InlineTypeNode::field_value_by_offset(int offset, bool recursive) const {
  // If the field at 'offset' belongs to a flat inline type field, 'index' refers to the
  // corresponding InlineTypeNode input and 'sub_offset' is the offset in flattened inline type.
  int index = inline_klass()->field_index_by_offset(offset);
  int sub_offset = offset - field_offset(index);
  Node* value = field_value(index);
  assert(value != nullptr, "field value not found");
  if (recursive && value->is_InlineType()) {
    if (field_is_flat(index)) {
      // Flat inline type field
      InlineTypeNode* vt = value->as_InlineType();
      sub_offset += vt->inline_klass()->first_field_offset(); // Add header size
      return vt->field_value_by_offset(sub_offset, recursive);
    } else {
      assert(sub_offset == 0, "should not have a sub offset");
      return value;
    }
  }
  assert(!(recursive && value->is_InlineType()), "should not be an inline type");
  assert(sub_offset == 0, "offset mismatch");
  return value;
}

void InlineTypeNode::set_field_value(uint index, Node* value) {
  assert(index < field_count(), "index out of bounds");
  set_req(Values + index, value);
}

void InlineTypeNode::set_field_value_by_offset(int offset, Node* value) {
  set_field_value(field_index(offset), value);
}

int InlineTypeNode::field_offset(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return inline_klass()->declared_nonstatic_field_at(index)->offset_in_bytes();
}

uint InlineTypeNode::field_index(int offset) const {
  uint i = 0;
  for (; i < field_count() && field_offset(i) != offset; i++) { }
  assert(i < field_count(), "field not found");
  return i;
}

ciType* InlineTypeNode::field_type(uint index) const {
  assert(index < field_count(), "index out of bounds");
  return inline_klass()->declared_nonstatic_field_at(index)->type();
}

bool InlineTypeNode::field_is_flat(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = inline_klass()->declared_nonstatic_field_at(index);
  assert(!field->is_flat() || field->type()->is_inlinetype(), "must be an inline type");
  return field->is_flat();
}

bool InlineTypeNode::field_is_null_free(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = inline_klass()->declared_nonstatic_field_at(index);
  assert(!field->is_flat() || field->type()->is_inlinetype(), "must be an inline type");
  return field->is_null_free();
}

void InlineTypeNode::make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt) {
  ciInlineKlass* vk = inline_klass();
  uint nfields = vk->nof_nonstatic_fields();
  JVMState* jvms = sfpt->jvms();
  // Replace safepoint edge by SafePointScalarObjectNode and add field values
  assert(jvms != nullptr, "missing JVMS");
  uint first_ind = (sfpt->req() - jvms->scloff());
  SafePointScalarObjectNode* sobj = new SafePointScalarObjectNode(type()->isa_instptr(),
                                                                  nullptr,
                                                                  first_ind, nfields);
  sobj->init_req(0, igvn->C->root());
  // Nullable inline types have an IsInit field that needs
  // to be checked before using the field values.
  if (!igvn->type(get_is_init())->is_int()->is_con(1)) {
    sfpt->add_req(get_is_init());
  } else {
    sfpt->add_req(igvn->C->top());
  }
  // Iterate over the inline type fields in order of increasing
  // offset and add the field values to the safepoint.
  for (uint j = 0; j < nfields; ++j) {
    int offset = vk->nonstatic_field_at(j)->offset_in_bytes();
    Node* value = field_value_by_offset(offset, true /* include flat inline type fields */);
    if (value->is_InlineType()) {
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
    if (debug != nullptr && debug->uncast() == this) {
      sfpt->set_req(i, sobj);
    }
  }
}

void InlineTypeNode::make_scalar_in_safepoints(PhaseIterGVN* igvn, bool allow_oop) {
  // If the inline type has a constant or loaded oop, use the oop instead of scalarization
  // in the safepoint to avoid keeping field loads live just for the debug info.
  Node* oop = get_oop();
  // TODO 8325106
  // TestBasicFunctionality::test3 fails without this. Add more tests?
  // Add proj nodes here? Recursive handling of phis required? We need a test that fails without.
  bool use_oop = false;
  if (allow_oop && is_allocated(igvn) && oop->is_Phi()) {
    Unique_Node_List worklist;
    worklist.push(oop);
    use_oop = true;
    while (worklist.size() > 0 && use_oop) {
      Node* n = worklist.pop();
      for (uint i = 1; i < n->req(); i++) {
        Node* in = n->in(i);
        if (in->is_Phi()) {
          worklist.push(in);
        // TestNullableArrays.test123 fails when enabling this, probably we should make sure that we don't load from a just allocated object
        //} else if (!(in->is_Con() || in->is_Parm() || in->is_Load() || (in->isa_DecodeN() && in->in(1)->is_Load()))) {
        } else if (!(in->is_Con() || in->is_Parm())) {
          use_oop = false;
          break;
        }
      }
    }
  } else {
    use_oop = allow_oop && is_allocated(igvn) &&
              (oop->is_Con() || oop->is_Parm() || oop->is_Load() || (oop->isa_DecodeN() && oop->in(1)->is_Load()));
  }

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
        if (debug != nullptr && debug->uncast() == this) {
          sfpt->set_req(i, get_oop());
        }
      }
      igvn->rehash_node_delayed(sfpt);
    } else {
      make_scalar_in_safepoint(igvn, vt_worklist, sfpt);
    }
  }
  // Now scalarize non-flat fields
  for (uint i = 0; i < vt_worklist.size(); ++i) {
    InlineTypeNode* vt = vt_worklist.at(i)->isa_InlineType();
    vt->make_scalar_in_safepoints(igvn);
  }
  if (outcnt() == 0) {
    igvn->_worklist.push(this);
  }
}

const TypePtr* InlineTypeNode::field_adr_type(Node* base, int offset, ciInstanceKlass* holder, DecoratorSet decorators, PhaseGVN& gvn) const {
  const TypeAryPtr* ary_type = gvn.type(base)->isa_aryptr();
  const TypePtr* adr_type = nullptr;
  bool is_array = ary_type != nullptr;
  if ((decorators & C2_MISMATCHED) != 0) {
    adr_type = TypeRawPtr::BOTTOM;
  } else if (is_array) {
    // In the case of a flat inline type array, each field has its own slice
    adr_type = ary_type->with_field_offset(offset)->add_offset(Type::OffsetBot);
  } else {
    ciField* field = holder->get_field_by_offset(offset, false);
    assert(field != nullptr, "field not found");
    adr_type = gvn.C->alias_type(field)->adr_type();
  }
  return adr_type;
}

// We limit scalarization for inline types with circular fields and can therefore observe nodes
// of the same type but with different scalarization depth during GVN. This method adjusts the
// scalarization depth to avoid inconsistencies during merging.
InlineTypeNode* InlineTypeNode::adjust_scalarization_depth(GraphKit* kit) {
  if (!kit->C->has_circular_inline_type()) {
    return this;
  }
  GrowableArray<ciType*> visited;
  visited.push(inline_klass());
  return adjust_scalarization_depth_impl(kit, visited);
}

InlineTypeNode* InlineTypeNode::adjust_scalarization_depth_impl(GraphKit* kit, GrowableArray<ciType*>& visited) {
  InlineTypeNode* val = this;
  for (uint i = 0; i < field_count(); ++i) {
    Node* value = field_value(i);
    Node* new_value = value;
    ciType* ft = field_type(i);
    if (value->is_InlineType()) {
      if (!field_is_flat(i) && visited.contains(ft)) {
        new_value = value->as_InlineType()->buffer(kit)->get_oop();
      } else {
        int old_len = visited.length();
        visited.push(ft);
        new_value = value->as_InlineType()->adjust_scalarization_depth_impl(kit, visited);
        visited.trunc_to(old_len);
      }
    } else if (ft->is_inlinetype() && !visited.contains(ft)) {
      int old_len = visited.length();
      visited.push(ft);
      new_value = make_from_oop_impl(kit, value, ft->as_inline_klass(), field_is_null_free(i), visited);
      visited.trunc_to(old_len);
    }
    if (value != new_value) {
      if (val == this) {
        val = clone()->as_InlineType();
      }
      val->set_field_value(i, new_value);
    }
  }
  return (val == this) ? this : kit->gvn().transform(val)->as_InlineType();
}

void InlineTypeNode::load(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, GrowableArray<ciType*>& visited, int holder_offset, DecoratorSet decorators) {
  // Initialize the inline type by loading its field values from
  // memory and adding the values as input edges to the node.
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = nullptr;
    ciType* ft = field_type(i);
    bool null_free = field_is_null_free(i);
    if (null_free && ft->as_inline_klass()->is_empty()) {
      // Loading from a field of an empty inline type. Just return the default instance.
      value = make_default_impl(kit->gvn(), ft->as_inline_klass(), visited);
    } else if (field_is_flat(i)) {
      // Recursively load the flat inline type field
      value = make_from_flat_impl(kit, ft->as_inline_klass(), base, ptr, holder, offset, decorators, visited);
    } else {
      const TypeOopPtr* oop_ptr = kit->gvn().type(base)->isa_oopptr();
      bool is_array = (oop_ptr->isa_aryptr() != nullptr);
      bool mismatched = (decorators & C2_MISMATCHED) != 0;
      if (base->is_Con() && !is_array && !mismatched) {
        // If the oop to the inline type is constant (static final field), we can
        // also treat the fields as constants because the inline type is immutable.
        ciObject* constant_oop = oop_ptr->const_oop();
        ciField* field = holder->get_field_by_offset(offset, false);
        assert(field != nullptr, "field not found");
        ciConstant constant = constant_oop->as_instance()->field_value(field);
        const Type* con_type = Type::make_from_constant(constant, /*require_const=*/ true);
        assert(con_type != nullptr, "type not found");
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
        value = kit->access_load_at(base, adr, adr_type, val_type, bt, is_array ? (decorators | IS_ARRAY) : decorators);
      }
      // Loading a non-flattened inline type from memory
      if (visited.contains(ft)) {
        kit->C->set_has_circular_inline_type(true);
      } else if (ft->is_inlinetype()) {
        int old_len = visited.length();
        visited.push(ft);
        value = make_from_oop_impl(kit, value, ft->as_inline_klass(), null_free, visited);
        visited.trunc_to(old_len);
      }
    }
    set_field_value(i, value);
  }
}

void InlineTypeNode::store_flat(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) const {
  if (kit->gvn().type(base)->isa_aryptr()) {
    kit->C->set_flat_accesses();
  }
  // The inline type is embedded into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when storing the values.
  if (holder == nullptr) {
    holder = inline_klass();
  }
  holder_offset -= inline_klass()->first_field_offset();
  store(kit, base, ptr, holder, holder_offset, decorators);
}

void InlineTypeNode::store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators, int offsetOnly) const {
  // Write field values to memory
  for (uint i = 0; i < field_count(); ++i) {
    if (offsetOnly != -1 && offsetOnly != field_offset(i)) continue;
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    ciType* ft = field_type(i);
    if (field_is_flat(i)) {
      // Recursively store the flat inline type field
      value->as_InlineType()->store_flat(kit, base, ptr, holder, offset, decorators);
    } else {
      // Store field value to memory
      const TypePtr* adr_type = field_adr_type(base, offset, holder, decorators, kit->gvn());
      Node* adr = kit->basic_plus_adr(base, ptr, offset);
      BasicType bt = type2field[ft->basic_type()];
      assert(is_java_primitive(bt) || adr->bottom_type()->is_ptr_to_narrowoop() == UseCompressedOops, "inconsistent");
      const Type* val_type = Type::get_const_type(ft);
      bool is_array = (kit->gvn().type(base)->isa_aryptr() != nullptr);
      kit->access_store_at(base, adr, adr_type, value, val_type, bt, is_array ? (decorators | IS_ARRAY) : decorators);
    }
  }
}

InlineTypeNode* InlineTypeNode::buffer(GraphKit* kit, bool safe_for_replace) {
  if (kit->gvn().find_int_con(get_is_buffered(), 0) == 1) {
    // Already buffered
    return this;
  }

  // Check if inline type is already buffered
  Node* not_buffered_ctl = kit->top();
  Node* not_null_oop = kit->null_check_oop(get_oop(), &not_buffered_ctl, /* never_see_null = */ false, safe_for_replace);
  if (not_buffered_ctl->is_top()) {
    // Already buffered
    InlineTypeNode* vt = clone()->as_InlineType();
    vt->set_is_buffered(kit->gvn());
    vt = kit->gvn().transform(vt)->as_InlineType();
    if (safe_for_replace) {
      kit->replace_in_map(this, vt);
    }
    return vt;
  }
  Node* buffered_ctl = kit->control();
  kit->set_control(not_buffered_ctl);

  // Inline type is not buffered, check if it is null.
  Node* null_ctl = kit->top();
  kit->null_check_common(get_is_init(), T_INT, false, &null_ctl);
  bool null_free = null_ctl->is_top();

  RegionNode* region = new RegionNode(4);
  PhiNode* oop = PhiNode::make(region, not_null_oop, type()->join_speculative(null_free ? TypePtr::NOTNULL : TypePtr::BOTTOM));

  // InlineType is already buffered
  region->init_req(1, buffered_ctl);
  oop->init_req(1, not_null_oop);

  // InlineType is null
  region->init_req(2, null_ctl);
  oop->init_req(2, kit->gvn().zerocon(T_OBJECT));

  PhiNode* io  = PhiNode::make(region, kit->i_o(), Type::ABIO);
  PhiNode* mem = PhiNode::make(region, kit->merged_memory(), Type::MEMORY, TypePtr::BOTTOM);

  int bci = kit->bci();
  bool reexecute = kit->jvms()->should_reexecute();
  if (!kit->stopped()) {
    assert(!is_allocated(&kit->gvn()), "already buffered");

    // Allocate and initialize buffer
    PreserveJVMState pjvms(kit);
    // Propagate re-execution state and bci
    kit->set_bci(bci);
    kit->jvms()->set_bci(bci);
    kit->jvms()->set_should_reexecute(reexecute);

    kit->kill_dead_locals();
    ciInlineKlass* vk = inline_klass();
    Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
    Node* alloc_oop  = kit->new_instance(klass_node, nullptr, nullptr, /* deoptimize_on_exception */ true, this);
    // No need to initialize a larval buffer, we make sure that the oop can not escape
    if (!is_larval()) {
      // Larval will be initialized later
      // TODO 8325106 should this use C2_TIGHTLY_COUPLED_ALLOC?
      store(kit, alloc_oop, alloc_oop, vk);

      // Do not let stores that initialize this buffer be reordered with a subsequent
      // store that would make this buffer accessible by other threads.
      AllocateNode* alloc = AllocateNode::Ideal_allocation(alloc_oop);
      assert(alloc != nullptr, "must have an allocation node");
      // TODO 8325106 MemBarRelease vs. MemBarStoreStore, see set_alloc_with_final
      kit->insert_mem_bar(Op_MemBarStoreStore, alloc->proj_out_or_null(AllocateNode::RawAddress));
    }

    region->init_req(3, kit->control());
    oop   ->init_req(3, alloc_oop);
    io    ->init_req(3, kit->i_o());
    mem   ->init_req(3, kit->merged_memory());
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
  InlineTypeNode* vt = clone()->as_InlineType();
  vt->set_oop(kit->gvn(), res_oop);
  vt->set_is_buffered(kit->gvn());
  vt = kit->gvn().transform(vt)->as_InlineType();
  if (safe_for_replace) {
    kit->replace_in_map(this, vt);
  }
  // InlineTypeNode::remove_redundant_allocations piggybacks on split if.
  // Make sure it gets a chance to remove this allocation.
  kit->C->set_has_split_ifs(true);
  return vt;
}

bool InlineTypeNode::is_allocated(PhaseGVN* phase) const {
  if (phase->find_int_con(get_is_buffered(), 0) == 1) {
    return true;
  }
  Node* oop = get_oop();
  const Type* oop_type = (phase != nullptr) ? phase->type(oop) : oop->bottom_type();
  return !oop_type->maybe_null();
}

// When a call returns multiple values, it has several result
// projections, one per field. Replacing the result of the call by an
// inline type node (after late inlining) requires that for each result
// projection, we find the corresponding inline type field.
void InlineTypeNode::replace_call_results(GraphKit* kit, CallNode* call, Compile* C) {
  ciInlineKlass* vk = inline_klass();
  for (DUIterator_Fast imax, i = call->fast_outs(imax); i < imax; i++) {
    ProjNode* pn = call->fast_out(i)->as_Proj();
    uint con = pn->_con;
    Node* field = nullptr;
    if (con == TypeFunc::Parms) {
      field = get_oop();
    } else if (con == (call->tf()->range_cc()->cnt() - 1)) {
      field = get_is_init();
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
      field = field_value_by_offset(f->offset_in_bytes(), true);
    }
    if (field != nullptr) {
      C->gvn_replace_by(pn, field);
      C->initial_gvn()->hash_delete(pn);
      pn->set_req(0, C->top());
      --i; --imax;
    }
  }
}

Node* InlineTypeNode::allocate_fields(GraphKit* kit) {
  InlineTypeNode* vt = clone()->as_InlineType();
  for (uint i = 0; i < field_count(); i++) {
     Node* value = field_value(i);
     if (field_is_flat(i)) {
       // Flat inline type field
       vt->set_field_value(i, value->as_InlineType()->allocate_fields(kit));
     } else if (value->is_InlineType()) {
       // Non-flat inline type field
       vt->set_field_value(i, value->as_InlineType()->buffer(kit));
     }
  }
  vt = kit->gvn().transform(vt)->as_InlineType();
  kit->replace_in_map(this, vt);
  return vt;
}

// Replace a buffer allocation by a dominating allocation
static void replace_allocation(PhaseIterGVN* igvn, Node* res, Node* dom) {
  // Remove initializing stores and GC barriers
  for (DUIterator_Fast imax, i = res->fast_outs(imax); i < imax; i++) {
    Node* use = res->fast_out(i);
    if (use->is_AddP()) {
      for (DUIterator_Fast jmax, j = use->fast_outs(jmax); j < jmax; j++) {
        Node* store = use->fast_out(j)->isa_Store();
        if (store != nullptr) {
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
  const Type* tinit = phase->type(get_is_init());
  if (!is_larval(phase) && !is_larval() &&
      (tinit->isa_int() && tinit->is_int()->is_con(1)) &&
      (is_default(phase) || inline_klass()->is_empty()) &&
      inline_klass()->is_initialized() &&
      (!oop->is_Con() || phase->type(oop)->is_zero_type())) {
    // Use the pre-allocated oop for null-free default or empty inline types
    set_oop(*phase, default_oop(*phase, inline_klass()));
    assert(is_allocated(phase), "should now be allocated");
    return this;
  }
  if (oop->isa_InlineType() && !phase->type(oop)->maybe_null()) {
    InlineTypeNode* vtptr = oop->as_InlineType();
    set_oop(*phase, vtptr->get_oop());
    set_is_buffered(*phase);
    set_is_init(*phase);
    for (uint i = Values; i < vtptr->req(); ++i) {
      set_req(i, vtptr->in(i));
    }
    return this;
  }
  // TODO 8325106 Re-evaluate this: We prefer a "loaded" oop because it's free. The existing oop might come from a buffering.
  if (!is_larval(phase) && !is_larval()) {
    // Save base oop if fields are loaded from memory and the inline
    // type is not buffered (in this case we should not use the oop).
    Node* base = is_loaded(phase);
    if (base != nullptr && get_oop() != base && !phase->type(base)->maybe_null()) {
      set_oop(*phase, base);
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
        if (alloc != nullptr && alloc->in(AllocateNode::InlineType) == this && !alloc->_is_scalar_replaceable) {
          // Found a re-allocation
          Node* res = alloc->result_cast();
          if (res != nullptr && res->is_CheckCastPP()) {
            // Replace allocation by oop and unlink AllocateNode
            replace_allocation(igvn, res, oop);
            igvn->replace_input_of(alloc, AllocateNode::InlineType, igvn->C->top());
            --i; --imax;
          }
        }
      }
    }
  }

  return nullptr;
}

InlineTypeNode* InlineTypeNode::make_uninitialized(PhaseGVN& gvn, ciInlineKlass* vk, bool null_free) {
  // Create a new InlineTypeNode with uninitialized values and nullptr oop
  bool use_default_oop = vk->is_empty() && vk->is_initialized() && null_free;
  Node* oop = use_default_oop ? default_oop(gvn, vk) : gvn.zerocon(T_OBJECT);
  InlineTypeNode* vt = new InlineTypeNode(vk, oop, null_free);
  vt->set_is_buffered(gvn, use_default_oop);
  vt->set_is_init(gvn);
  return vt;
}

Node* InlineTypeNode::default_oop(PhaseGVN& gvn, ciInlineKlass* vk) {
  // Returns the constant oop of the default inline type allocation
  return gvn.makecon(TypeInstPtr::make(vk->default_instance()));
}

InlineTypeNode* InlineTypeNode::make_default(PhaseGVN& gvn, ciInlineKlass* vk, bool is_larval) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_default_impl(gvn, vk, visited, is_larval);
}

InlineTypeNode* InlineTypeNode::make_default_impl(PhaseGVN& gvn, ciInlineKlass* vk, GrowableArray<ciType*>& visited, bool is_larval) {
  // Create a new InlineTypeNode with default values
  Node* oop = vk->is_initialized() && !is_larval ? default_oop(gvn, vk) : gvn.zerocon(T_OBJECT);
  InlineTypeNode* vt = new InlineTypeNode(vk, oop, /* null_free= */ true);
  // TODO 8325106 we should be able to set buffered here for non-larvals, right?
  //vt->set_is_buffered(gvn, vk->is_initialized());
  vt->set_is_buffered(gvn, false);
  vt->set_is_init(gvn);
  vt->set_is_larval(is_larval);
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* ft = vt->field_type(i);
    Node* value = gvn.zerocon(ft->basic_type());
    if (!vt->field_is_flat(i) && visited.contains(ft)) {
      gvn.C->set_has_circular_inline_type(true);
    } else if (ft->is_inlinetype()) {
      int old_len = visited.length();
      visited.push(ft);
      ciInlineKlass* vk = ft->as_inline_klass();
      if (vt->field_is_null_free(i)) {
        value = make_default_impl(gvn, vk, visited);
      } else {
        value = make_null_impl(gvn, vk, visited);
      }
      visited.trunc_to(old_len);
    }
    vt->set_field_value(i, value);
  }
  vt = gvn.transform(vt)->as_InlineType();
  assert(vt->is_default(&gvn), "must be the default inline type");
  return vt;
}

bool InlineTypeNode::is_default(PhaseGVN* gvn) const {
  const Type* tinit = gvn->type(get_is_init());
  if (!tinit->isa_int() || !tinit->is_int()->is_con(1)) {
    return false; // May be null
  }
  for (uint i = 0; i < field_count(); ++i) {
    ciType* ft = field_type(i);
    Node* value = field_value(i);
    if (field_is_null_free(i)) {
      if (!value->is_InlineType() || !value->as_InlineType()->is_default(gvn)) {
        return false;
      }
      continue;
    } else if (value->is_InlineType()) {
      if (value->as_InlineType()->is_default(gvn)) {
        continue;
      } else {
        const Type* tinit = gvn->type(value->as_InlineType()->get_is_init());
        if (tinit->isa_int() && tinit->is_int()->is_con(0)) {
          continue;
        }
        return false;
      }
    }
    if (!gvn->type(value)->is_zero_type()) {
      return false;
    }
  }
  return true;
}

InlineTypeNode* InlineTypeNode::make_from_oop(GraphKit* kit, Node* oop, ciInlineKlass* vk, bool null_free, bool is_larval) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_from_oop_impl(kit, oop, vk, null_free, visited, is_larval);
}

InlineTypeNode* InlineTypeNode::make_from_oop_impl(GraphKit* kit, Node* oop, ciInlineKlass* vk, bool null_free, GrowableArray<ciType*>& visited, bool is_larval) {
  PhaseGVN& gvn = kit->gvn();

  if (!is_larval && vk->is_empty() && null_free) {
    InlineTypeNode* def = make_default_impl(gvn, vk, visited);
    kit->record_for_igvn(def);
    return def;
  }
  // Create and initialize an InlineTypeNode by loading all field
  // values from a heap-allocated version and also save the oop.
  InlineTypeNode* vt = nullptr;

  if (oop->isa_InlineType()) {
    return oop->as_InlineType();
  } else if (gvn.type(oop)->maybe_null()) {
    // Add a null check because the oop may be null
    Node* null_ctl = kit->top();
    Node* not_null_oop = kit->null_check_oop(oop, &null_ctl);
    if (kit->stopped()) {
      // Constant null
      kit->set_control(null_ctl);
      if (null_free) {
        vt = make_default_impl(gvn, vk, visited);
      } else {
        vt = make_null_impl(gvn, vk, visited);
      }
      kit->record_for_igvn(vt);
      return vt;
    }
    vt = new InlineTypeNode(vk, not_null_oop, null_free);
    vt->set_is_buffered(gvn);
    vt->set_is_init(gvn);
    vt->set_is_larval(is_larval);
    vt->load(kit, not_null_oop, not_null_oop, vk, visited);

    if (null_ctl != kit->top()) {
      InlineTypeNode* null_vt = nullptr;
      if (null_free) {
        null_vt = make_default_impl(gvn, vk, visited);
      } else {
        null_vt = make_null_impl(gvn, vk, visited);
      }
      Node* region = new RegionNode(3);
      region->init_req(1, kit->control());
      region->init_req(2, null_ctl);

      vt = vt->clone_with_phis(&gvn, region);
      vt->merge_with(&gvn, null_vt, 2, true);
      if (!null_free) {
        vt->set_oop(gvn, oop);
      }
      kit->set_control(gvn.transform(region));
    }
  } else {
    // Oop can never be null
    vt = new InlineTypeNode(vk, oop, /* null_free= */ true);
    Node* init_ctl = kit->control();
    vt->set_is_buffered(gvn);
    vt->set_is_init(gvn);
    vt->set_is_larval(is_larval);
    vt->load(kit, oop, oop, vk, visited);
// TODO 8284443
//    assert(!null_free || vt->as_InlineType()->is_default(&gvn) || init_ctl != kit->control() || !gvn.type(oop)->is_inlinetypeptr() || oop->is_Con() || oop->Opcode() == Op_InlineType ||
//           AllocateNode::Ideal_allocation(oop, &gvn) != nullptr || vt->as_InlineType()->is_loaded(&gvn) == oop, "inline type should be loaded");
  }
  assert(vt->is_allocated(&gvn) || (null_free && !vk->is_initialized()), "inline type should be allocated");
  kit->record_for_igvn(vt);
  return gvn.transform(vt)->as_InlineType();
}

InlineTypeNode* InlineTypeNode::make_from_flat(GraphKit* kit, ciInlineKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_from_flat_impl(kit, vk, obj, ptr, holder, holder_offset, decorators, visited);
}

// GraphKit wrapper for the 'make_from_flat' method
InlineTypeNode* InlineTypeNode::make_from_flat_impl(GraphKit* kit, ciInlineKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset, DecoratorSet decorators, GrowableArray<ciType*>& visited) {
  if (kit->gvn().type(obj)->isa_aryptr()) {
    kit->C->set_flat_accesses();
  }
  // Create and initialize an InlineTypeNode by loading all field values from
  // a flat inline type field at 'holder_offset' or from an inline type array.
  InlineTypeNode* vt = make_uninitialized(kit->gvn(), vk);
  // The inline type is flattened into the object without an oop header. Subtract the
  // offset of the first field to account for the missing header when loading the values.
  holder_offset -= vk->first_field_offset();
  vt->load(kit, obj, ptr, holder, visited, holder_offset, decorators);
  assert(vt->is_loaded(&kit->gvn()) != obj, "holder oop should not be used as flattened inline type oop");
  return kit->gvn().transform(vt)->as_InlineType();
}

InlineTypeNode* InlineTypeNode::make_from_multi(GraphKit* kit, MultiNode* multi, ciInlineKlass* vk, uint& base_input, bool in, bool null_free) {
  InlineTypeNode* vt = make_uninitialized(kit->gvn(), vk, null_free);
  if (!in) {
    // Keep track of the oop. The returned inline type might already be buffered.
    Node* oop = kit->gvn().transform(new ProjNode(multi, base_input++));
    vt->set_oop(kit->gvn(), oop);
  }
  GrowableArray<ciType*> visited;
  visited.push(vk);
  vt->initialize_fields(kit, multi, base_input, in, null_free, nullptr, visited);
  return kit->gvn().transform(vt)->as_InlineType();
}

InlineTypeNode* InlineTypeNode::make_larval(GraphKit* kit, bool allocate) const {
  ciInlineKlass* vk = inline_klass();
  InlineTypeNode* res = make_uninitialized(kit->gvn(), vk);
  for (uint i = 1; i < req(); ++i) {
    res->set_req(i, in(i));
  }

  if (allocate) {
    // Re-execute if buffering triggers deoptimization
    PreserveReexecuteState preexecs(kit);
    kit->jvms()->set_should_reexecute(true);
    Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
    Node* alloc_oop  = kit->new_instance(klass_node, nullptr, nullptr, true);
    AllocateNode* alloc = AllocateNode::Ideal_allocation(alloc_oop);
    alloc->_larval = true;

    store(kit, alloc_oop, alloc_oop, vk);
    res->set_oop(kit->gvn(), alloc_oop);
  }
  // TODO 8239003
  //res->set_type(TypeInlineType::make(vk, true));
  res = kit->gvn().transform(res)->as_InlineType();
  assert(!allocate || res->is_allocated(&kit->gvn()), "must be allocated");
  return res;
}

InlineTypeNode* InlineTypeNode::finish_larval(GraphKit* kit) const {
  Node* obj = get_oop();
  Node* mark_addr = kit->basic_plus_adr(obj, oopDesc::mark_offset_in_bytes());
  Node* mark = kit->make_load(nullptr, mark_addr, TypeX_X, TypeX_X->basic_type(), MemNode::unordered);
  mark = kit->gvn().transform(new AndXNode(mark, kit->MakeConX(~markWord::larval_bit_in_place)));
  kit->store_to_memory(kit->control(), mark_addr, mark, TypeX_X->basic_type(), kit->gvn().type(mark_addr)->is_ptr(), MemNode::unordered);

  // Do not let stores that initialize this buffer be reordered with a subsequent
  // store that would make this buffer accessible by other threads.
  AllocateNode* alloc = AllocateNode::Ideal_allocation(obj);
  assert(alloc != nullptr, "must have an allocation node");
  kit->insert_mem_bar(Op_MemBarStoreStore, alloc->proj_out_or_null(AllocateNode::RawAddress));

  ciInlineKlass* vk = inline_klass();
  InlineTypeNode* res = make_uninitialized(kit->gvn(), vk);
  for (uint i = 1; i < req(); ++i) {
    res->set_req(i, in(i));
  }
  // TODO 8239003
  //res->set_type(TypeInlineType::make(vk, false));
  res = kit->gvn().transform(res)->as_InlineType();
  return res;
}

bool InlineTypeNode::is_larval(PhaseGVN* gvn) const {
  if (!is_allocated(gvn)) {
    return false;
  }

  Node* oop = get_oop();
  AllocateNode* alloc = AllocateNode::Ideal_allocation(oop);
  return alloc != nullptr && alloc->_larval;
}

Node* InlineTypeNode::is_loaded(PhaseGVN* phase, ciInlineKlass* vk, Node* base, int holder_offset) {
  if (vk == nullptr) {
    vk = inline_klass();
  }
  if (field_count() == 0 && vk->is_initialized()) {
    const Type* tinit = phase->type(in(IsInit));
    // TODO 8325106
    if (false && !is_larval() && tinit->isa_int() && tinit->is_int()->is_con(1)) {
      assert(is_allocated(phase), "must be allocated");
      return get_oop();
    } else {
      // TODO 8284443
      return nullptr;
    }
  }
  for (uint i = 0; i < field_count(); ++i) {
    int offset = holder_offset + field_offset(i);
    Node* value = field_value(i);
    if (value->is_InlineType()) {
      InlineTypeNode* vt = value->as_InlineType();
      if (vt->type()->inline_klass()->is_empty()) {
        continue;
      } else if (field_is_flat(i) && vt->is_InlineType()) {
        // Check inline type field load recursively
        base = vt->as_InlineType()->is_loaded(phase, vk, base, offset - vt->type()->inline_klass()->first_field_offset());
        if (base == nullptr) {
          return nullptr;
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
      if (lbase == nullptr || (lbase != base && base != nullptr) || loffset != offset) {
        return nullptr;
      } else if (base == nullptr) {
        // Set base and check if pointer type matches
        base = lbase;
        const TypeInstPtr* vtptr = phase->type(base)->isa_instptr();
        if (vtptr == nullptr || !vtptr->instance_klass()->equals(vk)) {
          return nullptr;
        }
      }
    } else {
      return nullptr;
    }
  }
  return base;
}

Node* InlineTypeNode::tagged_klass(ciInlineKlass* vk, PhaseGVN& gvn) {
  const TypeKlassPtr* tk = TypeKlassPtr::make(vk);
  intptr_t bits = tk->get_con();
  set_nth_bit(bits, 0);
  return gvn.longcon((jlong)bits);
}

void InlineTypeNode::pass_fields(GraphKit* kit, Node* n, uint& base_input, bool in, bool null_free) {
  if (!null_free && in) {
    n->init_req(base_input++, get_is_init());
  }
  for (uint i = 0; i < field_count(); i++) {
    Node* arg = field_value(i);
    if (field_is_flat(i)) {
      // Flat inline type field
      arg->as_InlineType()->pass_fields(kit, n, base_input, in);
    } else {
      if (arg->is_InlineType()) {
        // Non-flat inline type field
        InlineTypeNode* vt = arg->as_InlineType();
        assert(n->Opcode() != Op_Return || vt->is_allocated(&kit->gvn()), "inline type field should be allocated on return");
        arg = vt->buffer(kit);
      }
      // Initialize call/return arguments
      n->init_req(base_input++, arg);
      if (field_type(i)->size() == 2) {
        n->init_req(base_input++, kit->top());
      }
    }
  }
  // The last argument is used to pass IsInit information to compiled code and not required here.
  if (!null_free && !in) {
    n->init_req(base_input++, kit->top());
  }
}

void InlineTypeNode::initialize_fields(GraphKit* kit, MultiNode* multi, uint& base_input, bool in, bool null_free, Node* null_check_region, GrowableArray<ciType*>& visited) {
  PhaseGVN& gvn = kit->gvn();
  Node* is_init = nullptr;
  if (!null_free) {
    // Nullable inline type
    if (in) {
      // Set IsInit field
      if (multi->is_Start()) {
        is_init = gvn.transform(new ParmNode(multi->as_Start(), base_input));
      } else {
        is_init = multi->as_Call()->in(base_input);
      }
      set_req(IsInit, is_init);
      base_input++;
    }
    // Add a null check to make subsequent loads dependent on
    assert(null_check_region == nullptr, "already set");
    if (is_init == nullptr) {
      // Will only be initialized below, use dummy node for now
      is_init = new Node(1);
      gvn.set_type_bottom(is_init);
    }
    Node* null_ctrl = kit->top();
    kit->null_check_common(is_init, T_INT, false, &null_ctrl);
    Node* non_null_ctrl = kit->control();
    null_check_region = new RegionNode(3);
    null_check_region->init_req(1, non_null_ctrl);
    null_check_region->init_req(2, null_ctrl);
    null_check_region = gvn.transform(null_check_region);
    kit->set_control(null_check_region);
  }

  for (uint i = 0; i < field_count(); ++i) {
    ciType* type = field_type(i);
    Node* parm = nullptr;
    if (field_is_flat(i)) {
      // Flat inline type field
      InlineTypeNode* vt = make_uninitialized(gvn, type->as_inline_klass());
      vt->initialize_fields(kit, multi, base_input, in, true, null_check_region, visited);
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
      // Non-flat inline type field
      if (type->is_inlinetype()) {
        if (null_check_region != nullptr) {
          // We limit scalarization for inline types with circular fields and can therefore observe nodes
          // of the same type but with different scalarization depth during GVN. To avoid inconsistencies
          // during merging, make sure that we only create Phis for fields that are guaranteed to be scalarized.
          if (parm->is_InlineType() && kit->C->has_circular_inline_type()) {
            parm = parm->as_InlineType()->get_oop();
          }
          // Holder is nullable, set field to nullptr if holder is nullptr to avoid loading from uninitialized memory
          parm = PhiNode::make(null_check_region, parm, TypeInstPtr::make(TypePtr::BotPTR, type->as_inline_klass()));
          parm->set_req(2, kit->zerocon(T_OBJECT));
          parm = gvn.transform(parm);
        }
        if (visited.contains(type)) {
          kit->C->set_has_circular_inline_type(true);
        } else if (!parm->is_InlineType()) {
          int old_len = visited.length();
          visited.push(type);
          parm = make_from_oop_impl(kit, parm, type->as_inline_klass(), field_is_null_free(i), visited);
          visited.trunc_to(old_len);
        }
      }
      base_input += type->size();
    }
    assert(parm != nullptr, "should never be null");
    assert(field_value(i) == nullptr, "already set");
    set_field_value(i, parm);
    gvn.record_for_igvn(parm);
  }
  // The last argument is used to pass IsInit information to compiled code
  if (!null_free && !in) {
    Node* cmp = is_init->raw_out(0);
    is_init = gvn.transform(new ProjNode(multi->as_Call(), base_input));
    set_req(IsInit, is_init);
    gvn.hash_delete(cmp);
    cmp->set_req(1, is_init);
    gvn.hash_find_insert(cmp);
    base_input++;
  }
}

// Search for multiple allocations of this inline type and try to replace them by dominating allocations.
// Equivalent InlineTypeNodes are merged by GVN, so we just need to search for AllocateNode users to find redundant allocations.
void InlineTypeNode::remove_redundant_allocations(PhaseIdealLoop* phase) {
  if (is_larval()) {
    return;
  }
  PhaseIterGVN* igvn = &phase->igvn();
  // Search for allocations of this inline type. Ignore scalar replaceable ones, they
  // will be removed anyway and changing the memory chain will confuse other optimizations.
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    AllocateNode* alloc = fast_out(i)->isa_Allocate();
    if (alloc != nullptr && alloc->in(AllocateNode::InlineType) == this && !alloc->_is_scalar_replaceable) {
      Node* res = alloc->result_cast();
      if (res == nullptr || !res->is_CheckCastPP()) {
        break; // No unique CheckCastPP
      }
      // TODO 8325106
      // assert((!is_default(igvn) || !inline_klass()->is_initialized()) && !is_allocated(igvn), "re-allocation should be removed by Ideal transformation");
      // Search for a dominating allocation of the same inline type
      Node* res_dom = res;
      for (DUIterator_Fast jmax, j = fast_outs(jmax); j < jmax; j++) {
        AllocateNode* alloc_other = fast_out(j)->isa_Allocate();
        if (alloc_other != nullptr && alloc_other->in(AllocateNode::InlineType) == this && !alloc_other->_is_scalar_replaceable) {
          Node* res_other = alloc_other->result_cast();
          if (res_other != nullptr && res_other->is_CheckCastPP() && res_other != res_dom &&
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
}

InlineTypeNode* InlineTypeNode::make_null(PhaseGVN& gvn, ciInlineKlass* vk) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_null_impl(gvn, vk, visited);
}

InlineTypeNode* InlineTypeNode::make_null_impl(PhaseGVN& gvn, ciInlineKlass* vk, GrowableArray<ciType*>& visited) {
  InlineTypeNode* vt = new InlineTypeNode(vk, gvn.zerocon(T_OBJECT), /* null_free= */ false);
  vt->set_is_buffered(gvn);
  vt->set_is_init(gvn, false);
  for (uint i = 0; i < vt->field_count(); i++) {
    ciType* ft = vt->field_type(i);
    Node* value = gvn.zerocon(ft->basic_type());
    if (!vt->field_is_flat(i) && visited.contains(ft)) {
      gvn.C->set_has_circular_inline_type(true);
    } else if (ft->is_inlinetype()) {
      int old_len = visited.length();
      visited.push(ft);
      value = make_null_impl(gvn, ft->as_inline_klass(), visited);
      visited.trunc_to(old_len);
    }
    vt->set_field_value(i, value);
  }
  return gvn.transform(vt)->as_InlineType();
}

const Type* InlineTypeNode::Value(PhaseGVN* phase) const {
  Node* oop = get_oop();
  const Type* toop = phase->type(oop);
#ifdef ASSERT
  if (oop->is_Con() && toop->is_zero_type() && _type->isa_oopptr()->is_known_instance()) {
    // We are not allocated (anymore) and should therefore not have an instance id
    dump(1);
    assert(false, "Unbuffered inline type should not have known instance id");
  }
#endif
  const Type* t = toop->filter_speculative(_type);
  if (t->singleton()) {
    // Don't replace InlineType by a constant
    t = _type;
  }
  const Type* tinit = phase->type(in(IsInit));
  if (tinit == Type::TOP) {
    return Type::TOP;
  }
  if (tinit->isa_int() && tinit->is_int()->is_con(1)) {
    t = t->join_speculative(TypePtr::NOTNULL);
  }
  return t;
}
