/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "ci/ciInlineKlass.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "gc/shared/gc_globals.hpp"
#include "oops/accessDecorators.hpp"
#include "opto/addnode.hpp"
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/inlinetypenode.hpp"
#include "opto/movenode.hpp"
#include "opto/narrowptrnode.hpp"
#include "opto/opcodes.hpp"
#include "opto/rootnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/type.hpp"
#include "utilities/globalDefinitions.hpp"

// Clones the inline type to handle control flow merges involving multiple inline types.
// The inputs are replaced by PhiNodes to represent the merged values for the given region.
InlineTypeNode* InlineTypeNode::clone_with_phis(PhaseGVN* gvn, Node* region, SafePointNode* map, bool is_init) {
  InlineTypeNode* vt = clone_if_required(gvn, map);
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
  vt->set_req(NullMarker, is_init_node);

  // Create a PhiNode each for merging the field values
  for (uint i = 0; i < vt->field_count(); ++i) {
    ciType* type = vt->field_type(i);
    Node*  value = vt->field_value(i);
    // We limit scalarization for inline types with circular fields and can therefore observe nodes
    // of the same type but with different scalarization depth during GVN. To avoid inconsistencies
    // during merging, make sure that we only create Phis for fields that are guaranteed to be scalarized.
    bool no_circularity = !gvn->C->has_circular_inline_type() || field_is_flat(i);
    if (type->is_inlinetype() && no_circularity) {
      // Handle inline type fields recursively
      value = value->as_InlineType()->clone_with_phis(gvn, region, map);
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
  assert(inline_klass() == other->inline_klass(), "Merging incompatible types");

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
      set_req(NullMarker, gvn->transform(phi));
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
  // Find the declared field which contains the field we are looking for
  int index = inline_klass()->field_index_by_offset(offset);
  Node* value = field_value(index);
  assert(value != nullptr, "field value not found");

  if (!recursive || !field_is_flat(index)) {
    assert(offset == field_offset(index), "offset mismatch");
    return value;
  }

  // Flat inline type field
  InlineTypeNode* vt = value->as_InlineType();
  if (offset == field_null_marker_offset(index)) {
    return vt->get_is_init();
  } else {
    int sub_offset = offset - field_offset(index); // Offset of the flattened field inside the declared field
    sub_offset += vt->inline_klass()->payload_offset(); // Add header size
    return vt->field_value_by_offset(sub_offset, recursive);
  }
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

bool InlineTypeNode::field_is_volatile(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = inline_klass()->declared_nonstatic_field_at(index);
  assert(!field->is_flat() || field->type()->is_inlinetype(), "must be an inline type");
  return field->is_volatile();
}

int InlineTypeNode::field_null_marker_offset(uint index) const {
  assert(index < field_count(), "index out of bounds");
  ciField* field = inline_klass()->declared_nonstatic_field_at(index);
  assert(field->is_flat(), "must be an inline type");
  return field->null_marker_offset();
}

uint InlineTypeNode::add_fields_to_safepoint(Unique_Node_List& worklist, SafePointNode* sfpt) {
  uint cnt = 0;
  for (uint i = 0; i < field_count(); ++i) {
    Node* value = field_value(i);
    if (field_is_flat(i)) {
      InlineTypeNode* vt = value->as_InlineType();
      cnt += vt->add_fields_to_safepoint(worklist, sfpt);
      if (!field_is_null_free(i)) {
        // The null marker of a flat field is added right after we scalarize that field
        sfpt->add_req(vt->get_is_init());
        cnt++;
      }
      continue;
    }
    if (value->is_InlineType()) {
      // Add inline type to the worklist to process later
      worklist.push(value);
    }
    sfpt->add_req(value);
    cnt++;
  }
  return cnt;
}

void InlineTypeNode::make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt) {
  JVMState* jvms = sfpt->jvms();
  assert(jvms != nullptr, "missing JVMS");
  uint first_ind = (sfpt->req() - jvms->scloff());

  // Iterate over the inline type fields in order of increasing offset and add the
  // field values to the safepoint. Nullable inline types have an IsInit field that
  // needs to be checked before using the field values.
  sfpt->add_req(get_is_init());
  uint nfields = add_fields_to_safepoint(worklist, sfpt);
  jvms->set_endoff(sfpt->req());
  // Replace safepoint edge by SafePointScalarObjectNode
  SafePointScalarObjectNode* sobj = new SafePointScalarObjectNode(type()->isa_instptr(),
                                                                  nullptr,
                                                                  first_ind,
                                                                  sfpt->jvms()->depth(),
                                                                  nfields);
  sobj->init_req(0, igvn->C->root());
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
  bool use_oop = false;
  if (allow_oop && is_allocated(igvn) && oop->is_Phi()) {
    Unique_Node_List worklist;
    VectorSet visited;
    visited.set(oop->_idx);
    worklist.push(oop);
    use_oop = true;
    while (worklist.size() > 0 && use_oop) {
      Node* n = worklist.pop();
      for (uint i = 1; i < n->req(); i++) {
        Node* in = n->in(i);
        if (in->is_Phi() && !visited.test_set(in->_idx)) {
          worklist.push(in);
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
    igvn->record_for_igvn(this);
  }
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
      new_value = make_from_oop_impl(kit, value, ft->as_inline_klass(), visited);
      visited.trunc_to(old_len);
    }
    if (value != new_value) {
      if (val == this) {
        val = clone_if_required(&kit->gvn(), kit->map());
      }
      val->set_field_value(i, new_value);
    }
  }
  return (val == this) ? this : kit->gvn().transform(val)->as_InlineType();
}

void InlineTypeNode::load(GraphKit* kit, Node* base, Node* ptr, bool immutable_memory, bool trust_null_free_oop, DecoratorSet decorators, GrowableArray<ciType*>& visited) {
  // Initialize the inline type by loading its field values from
  // memory and adding the values as input edges to the node.
  ciInlineKlass* vk = inline_klass();
  for (uint i = 0; i < field_count(); ++i) {
    int field_off = field_offset(i) - vk->payload_offset();
    Node* field_ptr = kit->basic_plus_adr(base, ptr, field_off);
    Node* value = nullptr;
    ciType* ft = field_type(i);
    bool field_null_free = field_is_null_free(i);
    if (field_is_flat(i)) {
      // Recursively load the flat inline type field
      ciInlineKlass* fvk = ft->as_inline_klass();
      // Atomic if nullable or not LooselyConsistentValue
      bool atomic = !field_null_free || fvk->must_be_atomic();

      int old_len = visited.length();
      visited.push(ft);
      value = make_from_flat_impl(kit, fvk, base, field_ptr, atomic, immutable_memory,
                                  field_null_free, trust_null_free_oop && field_null_free, decorators, visited);
      visited.trunc_to(old_len);
    } else {
      // Load field value from memory
      BasicType bt = type2field[ft->basic_type()];
      assert(is_java_primitive(bt) || field_ptr->bottom_type()->is_ptr_to_narrowoop() == UseCompressedOops, "inconsistent");
      const Type* val_type = Type::get_const_type(ft);
      if (trust_null_free_oop && field_null_free) {
        val_type = val_type->join_speculative(TypePtr::NOTNULL);
      }
      const TypePtr* field_ptr_type = (decorators & C2_MISMATCHED) == 0 ? kit->gvn().type(field_ptr)->is_ptr() : TypeRawPtr::BOTTOM;
      value = kit->access_load_at(base, field_ptr, field_ptr_type, val_type, bt, decorators);
      // Loading a non-flattened inline type from memory
      if (visited.contains(ft)) {
        kit->C->set_has_circular_inline_type(true);
      } else if (ft->is_inlinetype()) {
        int old_len = visited.length();
        visited.push(ft);
        value = make_from_oop_impl(kit, value, ft->as_inline_klass(), visited);
        visited.trunc_to(old_len);
      }
    }
    set_field_value(i, value);
  }
}

// Get a field value from the payload by shifting it according to the offset
static Node* get_payload_value(PhaseGVN* gvn, Node* payload, BasicType bt, BasicType val_bt, int offset) {
  // Shift to the right position in the long value
  assert((offset + type2aelembytes(val_bt)) <= type2aelembytes(bt), "Value does not fit into payload");
  Node* value = nullptr;
  Node* shift_val = gvn->intcon(offset << LogBitsPerByte);
  if (bt == T_LONG) {
    value = gvn->transform(new URShiftLNode(payload, shift_val));
    value = gvn->transform(new ConvL2INode(value));
  } else {
    value = gvn->transform(new URShiftINode(payload, shift_val));
  }

  if (val_bt == T_INT || val_bt == T_OBJECT || val_bt == T_ARRAY) {
    return value;
  } else {
    // Make sure to zero unused bits in the 32-bit value
    return Compile::narrow_value(val_bt, value, nullptr, gvn, true);
  }
}

// Convert a payload value to field values
void InlineTypeNode::convert_from_payload(GraphKit* kit, BasicType bt, Node* payload, int holder_offset, bool null_free, bool trust_null_free_oop) {
  PhaseGVN* gvn = &kit->gvn();
  ciInlineKlass* vk = inline_klass();
  Node* value = nullptr;
  if (!null_free) {
    // Get the null marker
    value = get_payload_value(gvn, payload, bt, T_BOOLEAN, holder_offset + vk->null_marker_offset_in_payload());
    set_req(NullMarker, value);
  }
  // Iterate over the fields and get their values from the payload
  for (uint i = 0; i < field_count(); ++i) {
    ciType* ft = field_type(i);
    bool field_null_free = field_is_null_free(i);
    int offset = holder_offset + field_offset(i) - vk->payload_offset();
    if (field_is_flat(i)) {
      InlineTypeNode* vt = make_uninitialized(*gvn, ft->as_inline_klass(), field_null_free);
      vt->convert_from_payload(kit, bt, payload, offset, field_null_free, trust_null_free_oop && field_null_free);
      value = gvn->transform(vt);
    } else {
      value = get_payload_value(gvn, payload, bt, ft->basic_type(), offset);
      if (!ft->is_primitive_type()) {
        // Narrow oop field
        assert(UseCompressedOops && bt == T_LONG, "Naturally atomic");
        const Type* val_type = Type::get_const_type(ft);
        if (trust_null_free_oop && field_null_free) {
          val_type = val_type->join_speculative(TypePtr::NOTNULL);
        }
        value = gvn->transform(new CastI2NNode(kit->control(), value));
        value = gvn->transform(new DecodeNNode(value, val_type->make_narrowoop()));
        value = gvn->transform(new CastPPNode(kit->control(), value, val_type, ConstraintCastNode::UnconditionalDependency));

        // Similar to CheckCastPP nodes with raw input, CastI2N nodes require special handling in 'PhaseCFG::schedule_late' to ensure the
        // register allocator does not move the CastI2N below a safepoint. This is necessary to avoid having the raw pointer span a safepoint,
        // making it opaque to the GC. Unlike CheckCastPPs, which need extra handling in 'Scheduling::ComputeRegisterAntidependencies' due to
        // scalarization, CastI2N nodes are always used by a load if scalarization happens which inherently keeps them pinned above the safepoint.

        if (ft->is_inlinetype()) {
          GrowableArray<ciType*> visited;
          value = make_from_oop_impl(kit, value, ft->as_inline_klass(), visited);
        }
      }
    }
    set_field_value(i, value);
  }
}

// Set a field value in the payload by shifting it according to the offset
static Node* set_payload_value(PhaseGVN* gvn, Node* payload, BasicType bt, Node* value, BasicType val_bt, int offset) {
  assert((offset + type2aelembytes(val_bt)) <= type2aelembytes(bt), "Value does not fit into payload");

  // Make sure to zero unused bits in the 32-bit value
  if (val_bt == T_BYTE || val_bt == T_BOOLEAN) {
    value = gvn->transform(new AndINode(value, gvn->intcon(0xFF)));
  } else if (val_bt == T_CHAR || val_bt == T_SHORT) {
    value = gvn->transform(new AndINode(value, gvn->intcon(0xFFFF)));
  } else if (val_bt == T_FLOAT) {
    value = gvn->transform(new MoveF2INode(value));
  } else {
    assert(val_bt == T_INT, "Unsupported type: %s", type2name(val_bt));
  }

  Node* shift_val = gvn->intcon(offset << LogBitsPerByte);
  if (bt == T_LONG) {
    // Convert to long and remove the sign bit (the backend will fold this and emit a zero extend i2l)
    value = gvn->transform(new ConvI2LNode(value));
    value = gvn->transform(new AndLNode(value, gvn->longcon(0xFFFFFFFF)));

    Node* shift_value = gvn->transform(new LShiftLNode(value, shift_val));
    payload = new OrLNode(shift_value, payload);
  } else {
    Node* shift_value = gvn->transform(new LShiftINode(value, shift_val));
    payload = new OrINode(shift_value, payload);
  }
  return gvn->transform(payload);
}

// Convert the field values to a payload value of type 'bt'
Node* InlineTypeNode::convert_to_payload(GraphKit* kit, BasicType bt, Node* payload, int holder_offset, bool null_free, int null_marker_offset, int& oop_off_1, int& oop_off_2) const {
  PhaseGVN* gvn = &kit->gvn();
  Node* value = nullptr;
  if (!null_free) {
    // Set the null marker
    value = get_is_init();
    payload = set_payload_value(gvn, payload, bt, value, T_BOOLEAN, null_marker_offset);
  }
  // Iterate over the fields and add their values to the payload
  for (uint i = 0; i < field_count(); ++i) {
    value = field_value(i);
    int inner_offset = field_offset(i) - inline_klass()->payload_offset();
    int offset = holder_offset + inner_offset;
    if (field_is_flat(i)) {
      null_marker_offset = holder_offset + field_null_marker_offset(i) - inline_klass()->payload_offset();
      payload = value->as_InlineType()->convert_to_payload(kit, bt, payload, offset, field_is_null_free(i), null_marker_offset, oop_off_1, oop_off_2);
    } else {
      ciType* ft = field_type(i);
      BasicType field_bt = ft->basic_type();
      if (!ft->is_primitive_type()) {
        // Narrow oop field
        assert(UseCompressedOops && bt == T_LONG, "Naturally atomic");
        assert(inner_offset != -1, "sanity");
        if (oop_off_1 == -1) {
          oop_off_1 = inner_offset;
        } else {
          assert(oop_off_2 == -1, "already set");
          oop_off_2 = inner_offset;
        }
        const Type* val_type = Type::get_const_type(ft)->make_narrowoop();
        if (value->is_InlineType()) {
          PreserveReexecuteState preexecs(kit);
          kit->jvms()->set_should_reexecute(true);
          value = value->as_InlineType()->buffer(kit, false);
        }
        value = gvn->transform(new EncodePNode(value, val_type));
        value = gvn->transform(new CastP2XNode(kit->control(), value));
        value = gvn->transform(new ConvL2INode(value));
        field_bt = T_INT;
      }
      payload = set_payload_value(gvn, payload, bt, value, field_bt, offset);
    }
  }
  return payload;
}

void InlineTypeNode::store_flat(GraphKit* kit, Node* base, Node* ptr, bool atomic, bool immutable_memory, bool null_free, DecoratorSet decorators) const {
  ciInlineKlass* vk = inline_klass();
  bool do_atomic = atomic;
  // With immutable memory, a non-atomic load and an atomic load are the same
  if (immutable_memory) {
    do_atomic = false;
  }
  // If there is only one flattened field, a non-atomic load and an atomic load are the same
  if (vk->is_naturally_atomic(null_free)) {
    do_atomic = false;
  }

  if (!do_atomic) {
    if (!null_free) {
      int nm_offset = vk->null_marker_offset_in_payload();
      Node* nm_ptr = kit->basic_plus_adr(base, ptr, nm_offset);
      const TypePtr* nm_ptr_type = (decorators & C2_MISMATCHED) == 0 ? kit->gvn().type(nm_ptr)->is_ptr() : TypeRawPtr::BOTTOM;
      kit->access_store_at(base, nm_ptr, nm_ptr_type, get_is_init(), TypeInt::BOOL, T_BOOLEAN, decorators);
    }
    store(kit, base, ptr, immutable_memory, decorators);
    return;
  }

  // Convert to a payload value <= 64-bit and write atomically.
  // The payload might contain at most two oop fields that must be narrow because otherwise they would be 64-bit
  // in size and would then be written by a "normal" oop store. If the payload contains oops, its size is always
  // 64-bit because the next smaller (power-of-two) size would be 32-bit which could only hold one narrow oop that
  // would then be written by a normal narrow oop store. These properties are asserted in 'convert_to_payload'.
  assert(!immutable_memory, "immutable memory does not need explicit atomic access");
  BasicType store_bt = vk->atomic_size_to_basic_type(null_free);
  Node* payload = (store_bt == T_LONG) ? kit->longcon(0) : kit->intcon(0);
  int oop_off_1 = -1;
  int oop_off_2 = -1;
  payload = convert_to_payload(kit, store_bt, payload, 0, null_free, vk->null_marker_offset_in_payload(), oop_off_1, oop_off_2);
  if (!UseG1GC || oop_off_1 == -1) {
    // No oop fields or no late barrier expansion. Emit an atomic store of the payload and add GC barriers if needed.
    assert(oop_off_2 == -1 || !UseG1GC, "sanity");
    // ZGC does not support compressed oops, so only one oop can be in the payload which is written by a "normal" oop store.
    assert((oop_off_1 == -1 && oop_off_2 == -1) || !UseZGC, "ZGC does not support embedded oops in flat fields");
    const Type* val_type = Type::get_const_basic_type(store_bt);
    kit->insert_mem_bar(Op_MemBarCPUOrder);
    kit->access_store_at(base, ptr, TypeRawPtr::BOTTOM, payload, val_type, store_bt, decorators | C2_MISMATCHED, true, this);
    kit->insert_mem_bar(Op_MemBarCPUOrder);
  } else {
    // Contains oops and requires late barrier expansion. Emit a special store node that allows to emit GC barriers in the backend.
    assert(UseG1GC, "Unexpected GC");
    assert(store_bt == T_LONG, "Unexpected payload type");
    // If one oop, set the offset (if no offset is set, two oops are assumed by the backend)
    Node* oop_offset = (oop_off_2 == -1) ? kit->intcon(oop_off_1) : nullptr;
    kit->insert_mem_bar(Op_MemBarCPUOrder);
    Node* mem = kit->reset_memory();
    kit->set_all_memory(mem);
    Node* st = kit->gvn().transform(new StoreLSpecialNode(kit->control(), mem, ptr, TypeRawPtr::BOTTOM, payload, oop_offset, MemNode::unordered));
    kit->set_memory(st, TypeRawPtr::BOTTOM);
    kit->insert_mem_bar(Op_MemBarCPUOrder);
  }
}

void InlineTypeNode::store_flat_array(GraphKit* kit, Node* base, Node* idx) const {
  PhaseGVN& gvn = kit->gvn();
  DecoratorSet decorators = IN_HEAP | IS_ARRAY | MO_UNORDERED;
  kit->C->set_flat_accesses();
  ciInlineKlass* vk = inline_klass();
  assert(vk->maybe_flat_in_array(), "element type %s cannot be flat in array", vk->name()->as_utf8());

  RegionNode* region = new RegionNode(4);
  gvn.set_type(region, Type::CONTROL);
  kit->record_for_igvn(region);

  Node* input_memory_state = kit->reset_memory();
  kit->set_all_memory(input_memory_state);

  PhiNode* mem = PhiNode::make(region, input_memory_state, Type::MEMORY, TypePtr::BOTTOM);
  gvn.set_type(mem, Type::MEMORY);
  kit->record_for_igvn(mem);

  PhiNode* io = PhiNode::make(region, kit->i_o(), Type::ABIO);
  gvn.set_type(io, Type::ABIO);
  kit->record_for_igvn(io);

  Node* bol_null_free = kit->null_free_array_test(base); // Argument evaluation order is undefined in C++ and since this sets control, it needs to come first
  IfNode* iff_null_free = kit->create_and_map_if(kit->control(), bol_null_free, PROB_FAIR, COUNT_UNKNOWN);

  // Nullable
  kit->set_control(kit->IfFalse(iff_null_free));
  if (!kit->stopped()) {
    assert(vk->has_nullable_atomic_layout(), "element type %s does not have a nullable flat layout", vk->name()->as_utf8());
    kit->set_all_memory(input_memory_state);
    Node* cast = kit->cast_to_flat_array(base, vk, false, true, true);
    Node* ptr = kit->array_element_address(cast, idx, T_FLAT_ELEMENT);
    store_flat(kit, cast, ptr, true, false, false, decorators);

    region->init_req(1, kit->control());
    mem->set_req(1, kit->reset_memory());
    io->set_req(1, kit->i_o());
  }

  // Null-free
  kit->set_control(kit->IfTrue(iff_null_free));
  if (!kit->stopped()) {
    kit->set_all_memory(input_memory_state);

    Node* bol_atomic = kit->null_free_atomic_array_test(base, vk);
    IfNode* iff_atomic = kit->create_and_map_if(kit->control(), bol_atomic, PROB_FAIR, COUNT_UNKNOWN);

    // Atomic
    kit->set_control(kit->IfTrue(iff_atomic));
    if (!kit->stopped()) {
      assert(vk->has_atomic_layout(), "element type %s does not have a null-free atomic flat layout", vk->name()->as_utf8());
      kit->set_all_memory(input_memory_state);
      Node* cast = kit->cast_to_flat_array(base, vk, true, false, true);
      Node* ptr = kit->array_element_address(cast, idx, T_FLAT_ELEMENT);
      store_flat(kit, cast, ptr, true, false, true, decorators);

      region->init_req(2, kit->control());
      mem->set_req(2, kit->reset_memory());
      io->set_req(2, kit->i_o());
    }

    // Non-atomic
    kit->set_control(kit->IfFalse(iff_atomic));
    if (!kit->stopped()) {
      assert(vk->has_non_atomic_layout(), "element type %s does not have a null-free non-atomic flat layout", vk->name()->as_utf8());
      kit->set_all_memory(input_memory_state);
      Node* cast = kit->cast_to_flat_array(base, vk, true, false, false);
      Node* ptr = kit->array_element_address(cast, idx, T_FLAT_ELEMENT);
      store_flat(kit, cast, ptr, false, false, true, decorators);

      region->init_req(3, kit->control());
      mem->set_req(3, kit->reset_memory());
      io->set_req(3, kit->i_o());
    }
  }

  kit->set_control(gvn.transform(region));
  kit->set_all_memory(gvn.transform(mem));
  kit->set_i_o(gvn.transform(io));
}

void InlineTypeNode::store(GraphKit* kit, Node* base, Node* ptr, bool immutable_memory, DecoratorSet decorators) const {
  // Write field values to memory
  ciInlineKlass* vk = inline_klass();
  for (uint i = 0; i < field_count(); ++i) {
    int field_off = field_offset(i) - vk->payload_offset();
    Node* field_val = field_value(i);
    bool field_null_free = field_is_null_free(i);
    ciType* ft = field_type(i);
    Node* field_ptr = kit->basic_plus_adr(base, ptr, field_off);
    if (field_is_flat(i)) {
      // Recursively store the flat inline type field
      ciInlineKlass* fvk = ft->as_inline_klass();
      // Atomic if nullable or not LooselyConsistentValue
      bool atomic = !field_null_free || fvk->must_be_atomic();

      field_val->as_InlineType()->store_flat(kit, base, field_ptr, atomic, immutable_memory, field_null_free, decorators);
    } else {
      // Store field value to memory
      BasicType bt = type2field[ft->basic_type()];
      const TypePtr* field_ptr_type = (decorators & C2_MISMATCHED) == 0 ? kit->gvn().type(field_ptr)->is_ptr() : TypeRawPtr::BOTTOM;
      const Type* val_type = Type::get_const_type(ft);
      kit->access_store_at(base, field_ptr, field_ptr_type, field_val, val_type, bt, decorators);
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
    InlineTypeNode* vt = clone_if_required(&kit->gvn(), kit->map(), safe_for_replace);
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

  if (!kit->stopped()) {
    assert(!is_allocated(&kit->gvn()), "already buffered");
    PreserveJVMState pjvms(kit);
    ciInlineKlass* vk = inline_klass();
    // Allocate and initialize buffer, re-execute on deoptimization.
    kit->jvms()->set_bci(kit->bci());
    kit->jvms()->set_should_reexecute(true);
    kit->kill_dead_locals();
    Node* klass_node = kit->makecon(TypeKlassPtr::make(vk));
    Node* alloc_oop  = kit->new_instance(klass_node, nullptr, nullptr, /* deoptimize_on_exception */ true, this);
    Node* payload_alloc_oop = kit->basic_plus_adr(alloc_oop, vk->payload_offset());
    store(kit, alloc_oop, payload_alloc_oop, true, IN_HEAP | MO_UNORDERED | C2_TIGHTLY_COUPLED_ALLOC);

    // Do not let stores that initialize this buffer be reordered with a subsequent
    // store that would make this buffer accessible by other threads.
    AllocateNode* alloc = AllocateNode::Ideal_allocation(alloc_oop);
    assert(alloc != nullptr, "must have an allocation node");
    kit->insert_mem_bar(Op_MemBarStoreStore, alloc->proj_out_or_null(AllocateNode::RawAddress));
    oop->init_req(3, alloc_oop);
    region->init_req(3, kit->control());
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
  InlineTypeNode* vt = clone_if_required(&kit->gvn(), kit->map(), safe_for_replace);
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

static void replace_proj(Compile* C, CallNode* call, uint& proj_idx, Node* value, BasicType bt) {
  ProjNode* pn = call->proj_out_or_null(proj_idx);
  if (pn != nullptr) {
    C->gvn_replace_by(pn, value);
    C->initial_gvn()->hash_delete(pn);
    pn->set_req(0, C->top());
  }
  proj_idx += type2size[bt];
}

// When a call returns multiple values, it has several result
// projections, one per field. Replacing the result of the call by an
// inline type node (after late inlining) requires that for each result
// projection, we find the corresponding inline type field.
void InlineTypeNode::replace_call_results(GraphKit* kit, CallNode* call, Compile* C) {
  uint proj_idx = TypeFunc::Parms;
  // Replace oop projection
  replace_proj(C, call, proj_idx, get_oop(), T_OBJECT);
  // Replace field projections
  replace_field_projs(C, call, proj_idx);
  // Replace is_init projection
  replace_proj(C, call, proj_idx, get_is_init(), T_BOOLEAN);
  assert(proj_idx == call->tf()->range_cc()->cnt(), "missed a projection");
}

void InlineTypeNode::replace_field_projs(Compile* C, CallNode* call, uint& proj_idx) {
  for (uint i = 0; i < field_count(); ++i) {
    Node* value = field_value(i);
    if (field_is_flat(i)) {
      InlineTypeNode* vt = value->as_InlineType();
      // Replace field projections for flat field
      vt->replace_field_projs(C, call, proj_idx);
      if (!field_is_null_free(i)) {
        // Replace is_init projection for nullable field
        replace_proj(C, call, proj_idx, vt->get_is_init(), T_BOOLEAN);
      }
      continue;
    }
    // Replace projection for field value
    replace_proj(C, call, proj_idx, value, field_type(i)->basic_type());
  }
}

Node* InlineTypeNode::allocate_fields(GraphKit* kit) {
  InlineTypeNode* vt = clone_if_required(&kit->gvn(), kit->map());
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
  Node* is_buffered = get_is_buffered();

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

  // Use base oop if fields are loaded from memory, don't do so if base is the CheckCastPP of an
  // allocation because the only case we load from a naked CheckCastPP is when we exit a
  // constructor of an inline type and we want to relinquish the larval oop there. This has a
  // couple of benefits:
  // - The allocation is likely to be elided earlier if it is not an input of an InlineTypeNode.
  // - The InlineTypeNode without an allocation input is more likely to be GVN-ed. This may emerge
  //   when we try to clone a value object.
  // - The buffering, if needed, is delayed until it is required. This new allocation, since it is
  //   created from an InlineTypeNode, is recognized as not having a unique identity and in the
  //   future, we can move them around more freely such as hoisting out of loops. This is not true
  //   for the old allocation since larval value objects do have unique identities.
  Node* base = is_loaded(phase);
  if (base != nullptr && !base->is_InlineType() && !phase->type(base)->maybe_null() && AllocateNode::Ideal_allocation(base) == nullptr) {
    if (oop != base || phase->type(is_buffered) != TypeInt::ONE) {
      set_oop(*phase, base);
      set_is_buffered(*phase);
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
  InlineTypeNode* vt = new InlineTypeNode(vk, gvn.zerocon(T_OBJECT), null_free);
  vt->set_is_buffered(gvn, false);
  vt->set_is_init(gvn);
  return vt;
}

InlineTypeNode* InlineTypeNode::make_all_zero(PhaseGVN& gvn, ciInlineKlass* vk) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_all_zero_impl(gvn, vk, visited);
}

InlineTypeNode* InlineTypeNode::make_all_zero_impl(PhaseGVN& gvn, ciInlineKlass* vk, GrowableArray<ciType*>& visited) {
  // Create a new InlineTypeNode initialized with all zero
  InlineTypeNode* vt = new InlineTypeNode(vk, gvn.zerocon(T_OBJECT), /* null_free= */ true);
  vt->set_is_buffered(gvn, false);
  vt->set_is_init(gvn);
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
        value = make_all_zero_impl(gvn, vk, visited);
      } else {
        value = make_null_impl(gvn, vk, visited);
      }
      visited.trunc_to(old_len);
    }
    vt->set_field_value(i, value);
  }
  vt = gvn.transform(vt)->as_InlineType();
  assert(vt->is_all_zero(&gvn), "must be the all-zero inline type");
  return vt;
}

bool InlineTypeNode::is_all_zero(PhaseGVN* gvn, bool flat) const {
  const TypeInt* tinit = gvn->type(get_is_init())->isa_int();
  if (tinit == nullptr || !tinit->is_con(1)) {
    return false; // May be null
  }
  for (uint i = 0; i < field_count(); ++i) {
    Node* value = field_value(i);
    if (field_is_null_free(i)) {
      // Null-free value class field must have the all-zero value. If 'flat' is set,
      // reject non-flat fields because they need to be initialized with an oop to a buffer.
      if (!value->is_InlineType() || !value->as_InlineType()->is_all_zero(gvn) || (flat && !field_is_flat(i))) {
        return false;
      }
      continue;
    } else if (value->is_InlineType()) {
      // Nullable value class field must be null
      tinit = gvn->type(value->as_InlineType()->get_is_init())->isa_int();
      if (tinit != nullptr && tinit->is_con(0)) {
        continue;
      }
      return false;
    } else if (!gvn->type(value)->is_zero_type()) {
      return false;
    }
  }
  return true;
}

InlineTypeNode* InlineTypeNode::make_from_oop(GraphKit* kit, Node* oop, ciInlineKlass* vk) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_from_oop_impl(kit, oop, vk, visited);
}

InlineTypeNode* InlineTypeNode::make_from_oop_impl(GraphKit* kit, Node* oop, ciInlineKlass* vk, GrowableArray<ciType*>& visited) {
  PhaseGVN& gvn = kit->gvn();

  // Create and initialize an InlineTypeNode by loading all field
  // values from a heap-allocated version and also save the oop.
  InlineTypeNode* vt = nullptr;

  if (oop->isa_InlineType()) {
    return oop->as_InlineType();
  }

  if (gvn.type(oop)->maybe_null()) {
    // Add a null check because the oop may be null
    Node* null_ctl = kit->top();
    Node* not_null_oop = kit->null_check_oop(oop, &null_ctl);
    if (kit->stopped()) {
      // Constant null
      kit->set_control(null_ctl);
      vt = make_null_impl(gvn, vk, visited);
      kit->record_for_igvn(vt);
      return vt;
    }
    vt = new InlineTypeNode(vk, not_null_oop, /* null_free= */ false);
    vt->set_is_buffered(gvn);
    vt->set_is_init(gvn);
    Node* payload_ptr = kit->basic_plus_adr(not_null_oop, vk->payload_offset());
    vt->load(kit, not_null_oop, payload_ptr, true, true, IN_HEAP | MO_UNORDERED, visited);

    if (null_ctl != kit->top()) {
      InlineTypeNode* null_vt = make_null_impl(gvn, vk, visited);
      Node* region = new RegionNode(3);
      region->init_req(1, kit->control());
      region->init_req(2, null_ctl);
      vt = vt->clone_with_phis(&gvn, region, kit->map());
      vt->merge_with(&gvn, null_vt, 2, true);
      vt->set_oop(gvn, oop);
      kit->set_control(gvn.transform(region));
    }
  } else {
    // Oop can never be null
    vt = new InlineTypeNode(vk, oop, /* null_free= */ true);
    Node* init_ctl = kit->control();
    vt->set_is_buffered(gvn);
    vt->set_is_init(gvn);
    Node* payload_ptr = kit->basic_plus_adr(oop, vk->payload_offset());
    vt->load(kit, oop, payload_ptr, true, true, IN_HEAP | MO_UNORDERED, visited);
// TODO 8284443
//    assert(!null_free || vt->as_InlineType()->is_all_zero(&gvn) || init_ctl != kit->control() || !gvn.type(oop)->is_inlinetypeptr() || oop->is_Con() || oop->Opcode() == Op_InlineType ||
//           AllocateNode::Ideal_allocation(oop, &gvn) != nullptr || vt->as_InlineType()->is_loaded(&gvn) == oop, "inline type should be loaded");
  }
  assert(vt->is_allocated(&gvn), "inline type should be allocated");
  kit->record_for_igvn(vt);
  return gvn.transform(vt)->as_InlineType();
}

InlineTypeNode* InlineTypeNode::make_from_flat(GraphKit* kit, ciInlineKlass* vk, Node* base, Node* ptr,
                                               bool atomic, bool immutable_memory, bool null_free, DecoratorSet decorators) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_from_flat_impl(kit, vk, base, ptr, atomic, immutable_memory, null_free, null_free, decorators, visited);
}

// GraphKit wrapper for the 'make_from_flat' method
InlineTypeNode* InlineTypeNode::make_from_flat_impl(GraphKit* kit, ciInlineKlass* vk, Node* base, Node* ptr, bool atomic, bool immutable_memory,
                                                    bool null_free, bool trust_null_free_oop, DecoratorSet decorators, GrowableArray<ciType*>& visited) {
  assert(null_free || !trust_null_free_oop, "cannot trust null-free oop when the holder object is not null-free");
  PhaseGVN& gvn = kit->gvn();
  bool do_atomic = atomic;
  // With immutable memory, a non-atomic load and an atomic load are the same
  if (immutable_memory) {
    do_atomic = false;
  }
  // If there is only one flattened field, a non-atomic load and an atomic load are the same
  if (vk->is_naturally_atomic(null_free)) {
    do_atomic = false;
  }

  if (!do_atomic) {
    InlineTypeNode* vt = make_uninitialized(kit->gvn(), vk, null_free);
    if (!null_free) {
      int nm_offset = vk->null_marker_offset_in_payload();
      Node* nm_ptr = kit->basic_plus_adr(base, ptr, nm_offset);
      const TypePtr* nm_ptr_type = (decorators & C2_MISMATCHED) == 0 ? gvn.type(nm_ptr)->is_ptr() : TypeRawPtr::BOTTOM;
      Node* nm_value = kit->access_load_at(base, nm_ptr, nm_ptr_type, TypeInt::BOOL, T_BOOLEAN, decorators);
      vt->set_req(NullMarker, nm_value);
    }

    vt->load(kit, base, ptr, immutable_memory, trust_null_free_oop, decorators, visited);
    return gvn.transform(vt)->as_InlineType();
  }

  assert(!immutable_memory, "immutable memory does not need explicit atomic access");
  InlineTypeNode* vt = make_uninitialized(kit->gvn(), vk, null_free);
  BasicType load_bt = vk->atomic_size_to_basic_type(null_free);
  decorators |= C2_MISMATCHED | C2_CONTROL_DEPENDENT_LOAD;
  const Type* val_type = Type::get_const_basic_type(load_bt);
  kit->insert_mem_bar(Op_MemBarCPUOrder);
  Node* payload = kit->access_load_at(base, ptr, TypeRawPtr::BOTTOM, val_type, load_bt, decorators, kit->control());
  kit->insert_mem_bar(Op_MemBarCPUOrder);
  vt->convert_from_payload(kit, load_bt, kit->gvn().transform(payload), 0, null_free, trust_null_free_oop);
  return gvn.transform(vt)->as_InlineType();
}

InlineTypeNode* InlineTypeNode::make_from_flat_array(GraphKit* kit, ciInlineKlass* vk, Node* base, Node* idx) {
  assert(vk->maybe_flat_in_array(), "element type %s cannot be flat in array", vk->name()->as_utf8());
  PhaseGVN& gvn = kit->gvn();
  DecoratorSet decorators = IN_HEAP | IS_ARRAY | MO_UNORDERED | C2_CONTROL_DEPENDENT_LOAD;
  kit->C->set_flat_accesses();
  InlineTypeNode* vt_nullable = nullptr;
  InlineTypeNode* vt_null_free = nullptr;
  InlineTypeNode* vt_non_atomic = nullptr;

  RegionNode* region = new RegionNode(4);
  gvn.set_type(region, Type::CONTROL);
  kit->record_for_igvn(region);

  Node* input_memory_state = kit->reset_memory();
  kit->set_all_memory(input_memory_state);

  PhiNode* mem = PhiNode::make(region, input_memory_state, Type::MEMORY, TypePtr::BOTTOM);
  gvn.set_type(mem, Type::MEMORY);
  kit->record_for_igvn(mem);

  PhiNode* io = PhiNode::make(region, kit->i_o(), Type::ABIO);
  gvn.set_type(io, Type::ABIO);
  kit->record_for_igvn(io);

  Node* bol_null_free = kit->null_free_array_test(base); // Argument evaluation order is undefined in C++ and since this sets control, it needs to come first
  IfNode* iff_null_free = kit->create_and_map_if(kit->control(), bol_null_free, PROB_FAIR, COUNT_UNKNOWN);

  // Nullable
  kit->set_control(kit->IfFalse(iff_null_free));
  if (!kit->stopped()) {
    assert(vk->has_nullable_atomic_layout(), "element type %s does not have a nullable flat layout", vk->name()->as_utf8());
    kit->set_all_memory(input_memory_state);
    Node* cast = kit->cast_to_flat_array(base, vk, false, true, true);
    Node* ptr = kit->array_element_address(cast, idx, T_FLAT_ELEMENT);
    vt_nullable = InlineTypeNode::make_from_flat(kit, vk, cast, ptr, true, false, false, decorators);

    region->init_req(1, kit->control());
    mem->set_req(1, kit->reset_memory());
    io->set_req(1, kit->i_o());
  }

  // Null-free
  kit->set_control(kit->IfTrue(iff_null_free));
  if (!kit->stopped()) {
    kit->set_all_memory(input_memory_state);

    Node* bol_atomic = kit->null_free_atomic_array_test(base, vk);
    IfNode* iff_atomic = kit->create_and_map_if(kit->control(), bol_atomic, PROB_FAIR, COUNT_UNKNOWN);

    // Atomic
    kit->set_control(kit->IfTrue(iff_atomic));
    if (!kit->stopped()) {
      assert(vk->has_atomic_layout(), "element type %s does not have a null-free atomic flat layout", vk->name()->as_utf8());
      kit->set_all_memory(input_memory_state);
      Node* cast = kit->cast_to_flat_array(base, vk, true, false, true);
      Node* ptr = kit->array_element_address(cast, idx, T_FLAT_ELEMENT);
      vt_null_free = InlineTypeNode::make_from_flat(kit, vk, cast, ptr, true, false, true, decorators);

      region->init_req(2, kit->control());
      mem->set_req(2, kit->reset_memory());
      io->set_req(2, kit->i_o());
    }

    // Non-Atomic
    kit->set_control(kit->IfFalse(iff_atomic));
    if (!kit->stopped()) {
      assert(vk->has_non_atomic_layout(), "element type %s does not have a null-free non-atomic flat layout", vk->name()->as_utf8());
      kit->set_all_memory(input_memory_state);
      Node* cast = kit->cast_to_flat_array(base, vk, true, false, false);
      Node* ptr = kit->array_element_address(cast, idx, T_FLAT_ELEMENT);
      vt_non_atomic = InlineTypeNode::make_from_flat(kit, vk, cast, ptr, false, false, true, decorators);

      region->init_req(3, kit->control());
      mem->set_req(3, kit->reset_memory());
      io->set_req(3, kit->i_o());
    }
  }

  InlineTypeNode* vt = nullptr;
  if (vt_nullable == nullptr && vt_null_free == nullptr && vt_non_atomic == nullptr) {
    // All paths are dead
    vt = make_null(gvn, vk);
  } else if (vt_nullable == nullptr && vt_null_free == nullptr) {
    vt = vt_non_atomic;
  } else if (vt_nullable == nullptr && vt_non_atomic == nullptr) {
    vt = vt_null_free;
  } else if (vt_null_free == nullptr && vt_non_atomic == nullptr) {
    vt = vt_nullable;
  }
  if (vt != nullptr) {
    kit->set_control(kit->gvn().transform(region));
    kit->set_all_memory(kit->gvn().transform(mem));
    kit->set_i_o(kit->gvn().transform(io));
    return vt;
  }

  InlineTypeNode* zero = InlineTypeNode::make_null(gvn, vk);
  vt = zero->clone_with_phis(&gvn, region);
  if (vt_nullable != nullptr) {
    vt = vt->merge_with(&gvn, vt_nullable, 1, false);
  }
  if (vt_null_free != nullptr) {
    vt = vt->merge_with(&gvn, vt_null_free, 2, false);
  }
  if (vt_non_atomic != nullptr) {
    vt = vt->merge_with(&gvn, vt_non_atomic, 3, false);
  }

  kit->set_control(kit->gvn().transform(region));
  kit->set_all_memory(kit->gvn().transform(mem));
  kit->set_i_o(kit->gvn().transform(io));
  return gvn.transform(vt)->as_InlineType();
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

Node* InlineTypeNode::is_loaded(PhaseGVN* phase, ciInlineKlass* vk, Node* base, int holder_offset) {
  if (vk == nullptr) {
    vk = inline_klass();
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
        base = vt->as_InlineType()->is_loaded(phase, vk, base, offset - vt->type()->inline_klass()->payload_offset());
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
      if (!field_is_null_free(i)) {
        assert(field_null_marker_offset(i) != -1, "inconsistency");
        n->init_req(base_input++, arg->as_InlineType()->get_is_init());
      }
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
      set_req(NullMarker, is_init);
      base_input++;
    }
    // Add a null check to make subsequent loads dependent on
    assert(null_check_region == nullptr, "already set");
    if (is_init == nullptr) {
      // Will only be initialized below, use dummy node for now
      is_init = new Node(1);
      is_init->init_req(0, kit->control()); // Add an input to prevent dummy from being dead
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
      InlineTypeNode* vt = make_uninitialized(gvn, type->as_inline_klass(), field_is_null_free(i));
      vt->initialize_fields(kit, multi, base_input, in, true, null_check_region, visited);
      if (!field_is_null_free(i)) {
        assert(field_null_marker_offset(i) != -1, "inconsistency");
        Node* is_init = nullptr;
        if (multi->is_Start()) {
          is_init = gvn.transform(new ParmNode(multi->as_Start(), base_input));
        } else if (in) {
          is_init = multi->as_Call()->in(base_input);
        } else {
          is_init = gvn.transform(new ProjNode(multi->as_Call(), base_input));
        }
        vt->set_req(NullMarker, is_init);
        base_input++;
      }
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
      bool null_free = field_is_null_free(i);
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
          null_free = false;
        }
        if (visited.contains(type)) {
          kit->C->set_has_circular_inline_type(true);
        } else if (!parm->is_InlineType()) {
          int old_len = visited.length();
          visited.push(type);
          if (null_free) {
            parm = kit->cast_not_null(parm);
          }
          parm = make_from_oop_impl(kit, parm, type->as_inline_klass(), visited);
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
    set_req(NullMarker, is_init);
    gvn.hash_delete(cmp);
    cmp->set_req(1, is_init);
    gvn.hash_find_insert(cmp);
    gvn.record_for_igvn(cmp);
    base_input++;
  }
}

// Search for multiple allocations of this inline type and try to replace them by dominating allocations.
// Equivalent InlineTypeNodes are merged by GVN, so we just need to search for AllocateNode users to find redundant allocations.
void InlineTypeNode::remove_redundant_allocations(PhaseIdealLoop* phase) {
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

InlineTypeNode* InlineTypeNode::make_null(PhaseGVN& gvn, ciInlineKlass* vk, bool transform) {
  GrowableArray<ciType*> visited;
  visited.push(vk);
  return make_null_impl(gvn, vk, visited, transform);
}

InlineTypeNode* InlineTypeNode::make_null_impl(PhaseGVN& gvn, ciInlineKlass* vk, GrowableArray<ciType*>& visited, bool transform) {
  InlineTypeNode* vt = new InlineTypeNode(vk, gvn.zerocon(T_OBJECT), /* null_free= */ false);
  vt->set_is_buffered(gvn);
  vt->set_is_init(gvn, gvn.intcon(0));
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
  return transform ? gvn.transform(vt)->as_InlineType() : vt;
}

InlineTypeNode* InlineTypeNode::clone_if_required(PhaseGVN* gvn, SafePointNode* map, bool safe_for_replace) {
  if (!safe_for_replace || (map == nullptr && outcnt() != 0)) {
    return clone()->as_InlineType();
  }
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    if (fast_out(i) != map) {
      return clone()->as_InlineType();
    }
  }
  gvn->hash_delete(this);
  return this;
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
  const Type* tinit = phase->type(in(NullMarker));
  if (tinit == Type::TOP) {
    return Type::TOP;
  }
  if (tinit->isa_int() && tinit->is_int()->is_con(1)) {
    t = t->join_speculative(TypePtr::NOTNULL);
  }
  return t;
}
