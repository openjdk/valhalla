/*
 * Copyright (c) 2016, 2025, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_OPTO_INLINETYPENODE_HPP
#define SHARE_VM_OPTO_INLINETYPENODE_HPP

#include "gc/shared/c2/barrierSetC2.hpp"
#include "opto/connode.hpp"
#include "opto/loopnode.hpp"
#include "opto/node.hpp"

class GraphKit;
class OpaqueInlineTypeLoadNode;

//------------------------------InlineTypeNode-------------------------------------
// Node representing an inline type in C2 IR
class InlineTypeNode final : public TypeNode {
protected:
  InlineTypeNode(ciInlineKlass* vk, Node* oop, bool null_free)
      : TypeNode(TypeInstPtr::make(null_free ? TypePtr::NotNull : TypePtr::BotPTR, vk), Values + vk->nof_declared_nonstatic_fields()) {
    init_class_id(Class_InlineType);
    init_req(Oop, oop);
    Compile::current()->add_inline_type(this);
  }

  enum { Control,    // Control input.
         Oop,        // Oop to heap allocated buffer.
         IsBuffered, // True if inline type is heap allocated (or nullptr), false otherwise.
         IsInit,     // Needs to be checked for nullptr before using the field values.
         Values      // Nodes corresponding to values of the inline type's fields.
                     // Nodes are connected in increasing order of the index of the field they correspond to.
  };

  // Get the klass defining the field layout of the inline type
  ciInlineKlass* inline_klass() const { return type()->inline_klass(); }

  void make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt);
  uint add_fields_to_safepoint(Unique_Node_List& worklist, Node_List& null_markers, SafePointNode* sfpt);

  const TypePtr* field_adr_type(Node* base, int offset, ciInstanceKlass* holder, DecoratorSet decorators, PhaseGVN& gvn) const;

  // Checks if the inline type fields are all set to default values
  bool is_default(const PhaseGVN& gvn) const;
  bool is_default_with_phi(const PhaseGVN& gvn, Node* region, uint phi_idx) const;

  // Checks if the fields are all loaded from an oop and the load is performed by an
  // OpaqueInlineTypeLoadNode
  OpaqueInlineTypeLoadNode* find_opaque_load() const;

  // Checks if the inline type is loaded from memory and if so returns the oop
  Node* is_loaded(PhaseGVN* phase, ciInlineKlass* vk = nullptr, Node* base = nullptr, int holder_offset = 0);

  // Initialize the inline type fields with the inputs or outputs of a MultiNode
  void initialize_fields(GraphKit* kit, MultiNode* multi, uint& base_input, bool in, bool null_free, Node* null_check_region, GrowableArray<ciType*>& visited);

  InlineTypeNode* adjust_scalarization_depth_impl(GraphKit* kit, GrowableArray<ciType*>& visited);

  static InlineTypeNode* make_default_impl(PhaseGVN& gvn, ciInlineKlass* vk, GrowableArray<ciType*>& visited);
  static InlineTypeNode* make_from_oop_impl(GraphKit* kit, Node* oop, ciInlineKlass* vk, bool null_free, GrowableArray<ciType*>& visited);
  static InlineTypeNode* make_null_impl(PhaseGVN& gvn, ciInlineKlass* vk, GrowableArray<ciType*>& visited, bool transform = true);
  static InlineTypeNode* make_from_flat_impl(GraphKit* kit, ciInlineKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder, int holder_offset, bool atomic, int null_marker_offset, DecoratorSet decorators, GrowableArray<ciType*>& visited);

  void convert_from_payload(GraphKit* kit, BasicType bt, Node* payload, int holder_offset, bool null_free, int null_marker_offset);
  Node* convert_to_payload(GraphKit* kit, BasicType bt, Node* payload, int holder_offset, bool null_free, int null_marker_offset, int& oop_off_1, int& oop_off_2) const;

public:
  // Create with default field values
  static InlineTypeNode* make_default(PhaseGVN& gvn, ciInlineKlass* vk);
  // Create uninitialized
  static InlineTypeNode* make_uninitialized(PhaseGVN& gvn, ciInlineKlass* vk, bool null_free = true);
  // Create and initialize by loading the field values from an oop. null_free here means that if we
  // encounter a null pointer, we will treat it as if it is the default oop of the inline type.
  // This is because null-free members are still initialized with null and will be corrected upon
  // loading. null_free should only be true when we load a null-free member of an object, or a
  // null-free member in an array (either a non-flat element or a nested member of a flat element).
  static InlineTypeNode* make_from_oop(GraphKit* kit, Node* oop, ciInlineKlass* vk, bool null_free);
  // Create and initialize by loading the field values from a flat field or array
  static InlineTypeNode* make_from_flat(GraphKit* kit, ciInlineKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder = nullptr, int holder_offset = 0,
                                        bool atomic = false, int null_marker_offset = -1, DecoratorSet decorators = IN_HEAP | MO_UNORDERED);
  // Create and initialize with the inputs or outputs of a MultiNode (method entry or call)
  static InlineTypeNode* make_from_multi(GraphKit* kit, MultiNode* multi, ciInlineKlass* vk, uint& base_input, bool in, bool null_free = true);
  // Create with null field values
  static InlineTypeNode* make_null(PhaseGVN& gvn, ciInlineKlass* vk, bool transform = true);

  // Returns the constant oop of the default inline type allocation
  static Node* default_oop(PhaseGVN& gvn, ciInlineKlass* vk);

  // Support for control flow merges
  bool has_phi_inputs(Node* region);
  InlineTypeNode* clone_with_phis(PhaseGVN* gvn, Node* region, SafePointNode* map = nullptr, bool is_init = false);
  InlineTypeNode* merge_with(PhaseGVN* gvn, const InlineTypeNode* other, int pnum, bool transform);
  void add_new_path(Node* region);

  OpaqueInlineTypeLoadNode* opaque_load() const;

  // Get oop for heap allocated inline type (may be TypePtr::NULL_PTR)
  Node* get_oop() const    { return in(Oop); }
  void  set_oop(PhaseGVN& gvn, Node* oop) { set_req_X(Oop, oop, &gvn); }
  Node* get_is_init() const { return in(IsInit); }
  void  set_is_init(PhaseGVN& gvn, bool init = true) { set_req_X(IsInit, gvn.intcon(init ? 1 : 0), &gvn); }
  Node* get_is_buffered() const { return in(IsBuffered); }
  void  set_is_buffered(PhaseGVN& gvn, bool buffered = true) { set_req_X(IsBuffered, gvn.intcon(buffered ? 1 : 0), &gvn); }

  // Inline type fields
  uint          field_count() const { return req() - Values; }
  Node*         field_value(uint index) const;
  Node*         field_value_by_offset(int offset, bool recursive = false, bool search_null_marker = true) const;
  Node*         null_marker_by_offset(int offset, int holder_offset = 0) const;
  void      set_field_value(uint index, Node* value);
  void      set_field_value_by_offset(int offset, Node* value);
  int           field_offset(uint index) const;
  uint          field_index(int offset) const;
  ciType*       field_type(uint index) const;
  bool          field_is_flat(uint index) const;
  bool          field_is_null_free(uint index) const;
  bool          field_is_volatile(uint index) const;
  int           field_null_marker_offset(uint index) const;

  // Replace InlineTypeNodes in debug info at safepoints with SafePointScalarObjectNodes
  void make_scalar_in_safepoints(PhaseIterGVN* igvn, bool allow_oop = true);

  // Store the inline type as a flat (headerless) representation
  void store_flat(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset, bool atomic, int null_marker_offset, DecoratorSet decorators) const;
  // Store the field values to memory
  void store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0, int offset = -1, DecoratorSet decorators = C2_TIGHTLY_COUPLED_ALLOC | IN_HEAP | MO_UNORDERED) const;
  // Initialize the inline type by loading its field values from memory
  void load(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, GrowableArray<ciType*>& visited, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED);
  // Make sure that inline type is fully scalarized
  InlineTypeNode* adjust_scalarization_depth(GraphKit* kit);

  // Allocates the inline type (if not yet allocated)
  InlineTypeNode* buffer(GraphKit* kit, bool safe_for_replace = true);
  bool is_allocated(PhaseGVN* phase) const;

  void replace_call_results(GraphKit* kit, CallNode* call, Compile* C);
  void replace_field_projs(Compile* C, MultiNode* call, uint& proj_idx);

  // Allocate all non-flat inline type fields
  Node* allocate_fields(GraphKit* kit);

  Node* tagged_klass(PhaseGVN& gvn) {
    return tagged_klass(inline_klass(), gvn);
  }
  static Node* tagged_klass(ciInlineKlass* vk, PhaseGVN& gvn);
  // Pass inline type as fields at a call or return
  void pass_fields(GraphKit* kit, Node* n, uint& base_input, bool in, bool null_free = true);

  // Allocation optimizations
  void remove_redundant_allocations(PhaseIdealLoop* phase);

  InlineTypeNode* clone_if_required(PhaseGVN* gvn, SafePointNode* map, bool safe_for_replace = true);

  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape) override;
  virtual const Type* Value(PhaseGVN* phase) const override;
  virtual Node* Identity(PhaseGVN* phase) override;

  virtual int Opcode() const override;
};

class OpaqueInlineTypeLoadNode final : public MultiNode {
private:
  const TypeTuple* _type;
  ciInlineKlass* _vk;
  bool _null_free;

  OpaqueInlineTypeLoadNode(const TypeTuple* type, ciInlineKlass* vk, bool null_free)
    : MultiNode(TypeFunc::Parms + 1), _type(type), _vk(vk), _null_free(null_free) {
    init_class_id(Class_OpaqueInlineTypeLoad);
  }

public:
  enum  {
    Oop = TypeFunc::Parms,
    IsInit = TypeFunc::Parms + 1,
    Values = TypeFunc::Parms + 2
  };

  // null_free: if this oop is null, it will be treated as if it is the default oop of the inline
  // klass. This is because null-free members are still initialized with null and will be corrected
  // upon loading.
  static MultiNode* make(GraphKit* kit, Node* oop, ciInlineKlass* vk, bool null_free);
  Node* base() const { return in(TypeFunc::Parms); }
  void try_eliminate(PhaseIterGVN& igvn);
  void expand(PhaseIterGVN& igvn);

  virtual uint size_of() const override { return sizeof(this); }
  virtual int Opcode() const override;
  virtual const Type* bottom_type() const override { return _type; }
  virtual const TypePtr* adr_type() const override { return TypePtr::BOTTOM; }

#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
#endif
};

#endif // SHARE_VM_OPTO_INLINETYPENODE_HPP
