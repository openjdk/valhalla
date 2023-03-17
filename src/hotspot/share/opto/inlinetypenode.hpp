/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

#include "opto/connode.hpp"
#include "opto/loopnode.hpp"
#include "opto/node.hpp"

class GraphKit;

//------------------------------InlineTypeNode-------------------------------------
// Node representing an inline type in C2 IR
class InlineTypeNode : public TypeNode {
protected:
  virtual uint hash() const;
  virtual bool cmp(const Node &n) const;
  virtual uint size_of() const;
  bool _is_buffered;

  InlineTypeNode(ciInlineKlass* vk, Node* oop, bool null_free, bool is_buffered)
      : TypeNode(TypeInstPtr::make(null_free ? TypePtr::NotNull : TypePtr::BotPTR, vk), Values + vk->nof_declared_nonstatic_fields()), _is_buffered(is_buffered) {
    init_class_id(Class_InlineType);
    init_req(Oop, oop);
    Compile::current()->add_inline_type(this);
  }

  enum { Control,   // Control input.
         Oop,       // Oop to heap allocated buffer (NULL if not buffered).
         IsInit,    // Needs to be checked for NULL before using the field values.
         Values     // Nodes corresponding to values of the inline type's fields.
                    // Nodes are connected in increasing order of the index of the field they correspond to.
  };

  void make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt);

  const TypePtr* field_adr_type(Node* base, int offset, ciInstanceKlass* holder, DecoratorSet decorators, PhaseGVN& gvn) const;

  // Checks if the inline type fields are all set to default values
  bool is_default(PhaseGVN* gvn) const;

  // Checks if the inline type is loaded from memory and if so returns the oop
  Node* is_loaded(PhaseGVN* phase, ciInlineKlass* vk = NULL, Node* base = NULL, int holder_offset = 0);

  // Initialize the inline type fields with the inputs or outputs of a MultiNode
  void initialize_fields(GraphKit* kit, MultiNode* multi, uint& base_input, bool in, bool null_free = true, Node* null_check_region = NULL);

public:
  // Get the klass defining the field layout of the inline type
  ciInlineKlass* inline_klass() const { return type()->inline_klass(); }

  // Create with default field values
  static InlineTypeNode* make_default(PhaseGVN& gvn, ciInlineKlass* vk);
  // Create uninitialized
  static InlineTypeNode* make_uninitialized(PhaseGVN& gvn, ciInlineKlass* vk, bool null_free = true);
  // Create and initialize by loading the field values from an oop
  static InlineTypeNode* make_from_oop(GraphKit* kit, Node* oop, ciInlineKlass* vk, bool null_free = true);
  // Create and initialize by loading the field values from a flattened field or array
  static InlineTypeNode* make_from_flattened(GraphKit* kit, ciInlineKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED);
  // Create and initialize with the inputs or outputs of a MultiNode (method entry or call)
  static InlineTypeNode* make_from_multi(GraphKit* kit, MultiNode* multi, ciInlineKlass* vk, uint& base_input, bool in, bool null_free = true);

  static InlineTypeNode* make_null(PhaseGVN& gvn, ciInlineKlass* vk);

  // Returns the constant oop of the default inline type allocation
  static Node* default_oop(PhaseGVN& gvn, ciInlineKlass* vk);

  static Node* default_value(PhaseGVN& gvn, ciType* field_type);

  // Support for control flow merges
  bool has_phi_inputs(Node* region);
  InlineTypeNode* clone_with_phis(PhaseGVN* gvn, Node* region, bool is_init = false);
  InlineTypeNode* merge_with(PhaseGVN* gvn, const InlineTypeNode* other, int pnum, bool transform);
  void add_new_path(Node* region);

  // Get oop for heap allocated inline type (may be TypePtr::NULL_PTR)
  Node* get_oop() const    { return in(Oop); }
  void  set_oop(Node* oop) { set_req(Oop, oop); }
  Node* get_is_init() const { return in(IsInit); }
  void  set_is_init(PhaseGVN& gvn) { set_req(IsInit, gvn.intcon(1)); }
  void  set_is_buffered() { _is_buffered = true; }
  bool  is_buffered() { return _is_buffered; }

  // Inline type fields
  virtual uint  field_count() const { return req() - Values; }
  virtual Node* field_value(uint index) const;
  uint          field_index(int offset) const;

  Node*         field_value_by_offset(int offset, bool recursive = false) const;
  void          set_field_value(uint index, Node* value);
  void          set_field_value_by_offset(int offset, Node* value);
  int           field_offset(uint index) const;
  bool          is_multifield(uint index) const;
  bool          is_multifield_base(uint index) const;
  int           secondary_field_count(uint index) const;
  bool          is_multifield() const;
  ciType*       field_type(uint index) const;
  bool          field_is_flattened(uint index) const;
  bool          field_is_null_free(uint index) const;

  // Replace InlineTypeNodes in debug info at safepoints with SafePointScalarObjectNodes
  void make_scalar_in_safepoints(PhaseIterGVN* igvn, bool allow_oop = true);

  // Store the inline type as a flattened (headerless) representation
  void store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED) const;
  // Store the field values to memory
  void store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED) const;
  // Initialize the inline type by loading its field values from memory
  void load(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED);

  // Allocates the inline type (if not yet allocated)
  InlineTypeNode* buffer(GraphKit* kit, bool safe_for_replace = true);
  bool is_allocated(PhaseGVN* phase) const;

  void replace_call_results(GraphKit* kit, CallNode* call, Compile* C, bool null_free = true);

  // Allocate all non-flattened inline type fields
  Node* allocate_fields(GraphKit* kit);

  Node* tagged_klass(PhaseGVN& gvn) {
    return tagged_klass(inline_klass(), gvn);
  }
  static Node* tagged_klass(ciInlineKlass* vk, PhaseGVN& gvn);
  // Pass inline type as fields at a call or return
  void pass_fields(GraphKit* kit, Node* n, uint& base_input, bool in, bool null_free = true);

  InlineTypeNode* make_larval(GraphKit* kit, bool allocate) const;
  InlineTypeNode* finish_larval(GraphKit* kit) const;

  // Allocation optimizations
  void remove_redundant_allocations(PhaseIdealLoop* phase);

  virtual Node* Identity(PhaseGVN* phase);

  virtual const Type* Value(PhaseGVN* phase) const;

  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);

  virtual int Opcode() const;
};

#endif // SHARE_VM_OPTO_INLINETYPENODE_HPP
