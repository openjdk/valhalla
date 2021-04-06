/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

class InlineTypeBaseNode : public TypeNode {
protected:
  InlineTypeBaseNode(const Type* t, int nb_fields)
    : TypeNode(t, nb_fields) {
    init_class_id(Class_InlineTypeBase);
    Compile::current()->add_inline_type(this);
  }

  enum { Control,   // Control input
         Oop,       // Oop of TypeInstPtr
         Values     // Nodes corresponding to values of the inline type's fields.
                    // Nodes are connected in increasing order of the index of the field they correspond to.
  };

  virtual const TypeInstPtr* inline_ptr() const = 0;
  // Get the klass defining the field layout of the inline type
  ciInlineKlass* inline_klass() const { return type()->inline_klass(); }

  int make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt);

  const TypePtr* field_adr_type(Node* base, int offset, ciInstanceKlass* holder, DecoratorSet decorators, PhaseGVN& gvn) const;

public:
  // Support for control flow merges
  bool has_phi_inputs(Node* region);
  InlineTypeBaseNode* clone_with_phis(PhaseGVN* gvn, Node* region);
  InlineTypeBaseNode* merge_with(PhaseGVN* gvn, const InlineTypeBaseNode* other, int pnum, bool transform);
  void add_new_path(Node* region);

  // Get oop for heap allocated inline type (may be TypePtr::NULL_PTR)
  Node* get_oop() const    { return in(Oop); }
  void  set_oop(Node* oop) { set_req(Oop, oop); }

  // Inline type fields
  uint          field_count() const { return req() - Values; }
  Node*         field_value(uint index) const;
  Node*         field_value_by_offset(int offset, bool recursive = false) const;
  void      set_field_value(uint index, Node* value);
  void      set_field_value_by_offset(int offset, Node* value);
  int           field_offset(uint index) const;
  uint          field_index(int offset) const;
  ciType*       field_type(uint index) const;
  bool          field_is_flattened(uint index) const;

  // Replace InlineTypeNodes in debug info at safepoints with SafePointScalarObjectNodes
  void make_scalar_in_safepoints(PhaseIterGVN* igvn, bool allow_oop = true);

  // Store the inline type as a flattened (headerless) representation
  void store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED) const;
  // Store the field values to memory
  void store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED) const;
  // Initialize the inline type by loading its field values from memory
  void load(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED);

  // Allocates the inline type (if not yet allocated)
  InlineTypePtrNode* buffer(GraphKit* kit, bool safe_for_replace = true);
  bool is_allocated(PhaseGVN* phase) const;
  InlineTypePtrNode* as_ptr(PhaseGVN* phase) const;

  void replace_call_results(GraphKit* kit, Node* call, Compile* C);

  // Allocate all non-flattened inline type fields
  Node* allocate_fields(GraphKit* kit);

  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
};

//------------------------------InlineTypeNode-------------------------------------
// Node representing an inline type in C2 IR
class InlineTypeNode : public InlineTypeBaseNode {
  friend class InlineTypeBaseNode;
  friend class InlineTypePtrNode;
private:
  InlineTypeNode(ciInlineKlass* vk, Node* oop)
    : InlineTypeBaseNode(TypeInlineType::make(vk), Values + vk->nof_declared_nonstatic_fields()) {
    init_class_id(Class_InlineType);
    init_req(Oop, oop);
  }

  // Checks if the inline type is loaded from memory and if so returns the oop
  Node* is_loaded(PhaseGVN* phase, ciInlineKlass* vk = NULL, Node* base = NULL, int holder_offset = 0);

  // Checks if the inline type fields are all set to default values
  bool is_default(PhaseGVN* gvn) const;

  const TypeInstPtr* inline_ptr() const { return TypeInstPtr::make(TypePtr::BotPTR, inline_klass()); }

public:
  // Create uninitialized
  static InlineTypeNode* make_uninitialized(PhaseGVN& gvn, ciInlineKlass* vk);
  // Create with default field values
  static InlineTypeNode* make_default(PhaseGVN& gvn, ciInlineKlass* vk);
  // Create and initialize by loading the field values from an oop
  static InlineTypeNode* make_from_oop(GraphKit* kit, Node* oop, ciInlineKlass* vk);
  // Create and initialize by loading the field values from a flattened field or array
  static InlineTypeNode* make_from_flattened(GraphKit* kit, ciInlineKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0, DecoratorSet decorators = IN_HEAP | MO_UNORDERED);
  // Create and initialize with the inputs or outputs of a MultiNode (method entry or call)
  static InlineTypeNode* make_from_multi(GraphKit* kit, MultiNode* multi, ciInlineKlass* vk, uint& base_input, bool in);

  InlineTypeNode* make_larval(GraphKit* kit, bool allocate) const;
  InlineTypeNode* finish_larval(GraphKit* kit) const;

  // Returns the constant oop of the default inline type allocation
  static Node* default_oop(PhaseGVN& gvn, ciInlineKlass* vk);

  Node* tagged_klass(PhaseGVN& gvn) {
    return tagged_klass(inline_klass(), gvn);
  }
  static Node* tagged_klass(ciInlineKlass* vk, PhaseGVN& gvn);
  // Pass inline type as fields at a call or return
  void pass_fields(GraphKit* kit, Node* n, uint& base_input);
  // Initialize the inline type fields with the inputs or outputs of a MultiNode
  void initialize_fields(GraphKit* kit, MultiNode* multi, uint& base_input, bool in);

  // Allocation optimizations
  void remove_redundant_allocations(PhaseIterGVN* igvn, PhaseIdealLoop* phase);

  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual int Opcode() const;
};

//------------------------------InlineTypePtrNode-------------------------------------
// Node representing an inline type as a pointer in C2 IR
class InlineTypePtrNode : public InlineTypeBaseNode {
  friend class InlineTypeBaseNode;
private:
  const TypeInstPtr* inline_ptr() const { return type()->isa_instptr(); }

  InlineTypePtrNode(const InlineTypeBaseNode* vt)
    : InlineTypeBaseNode(TypeInstPtr::make(TypePtr::NotNull, vt->type()->inline_klass()), vt->req()) {
    init_class_id(Class_InlineTypePtr);
    init_req(Oop, vt->get_oop());
    for (uint i = Oop+1; i < vt->req(); i++) {
      init_req(i, vt->in(i));
    }
  }

  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);

public:
  virtual int Opcode() const;
};

#endif // SHARE_VM_OPTO_INLINETYPENODE_HPP
