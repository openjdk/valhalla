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

#ifndef SHARE_VM_OPTO_VALUETYPENODE_HPP
#define SHARE_VM_OPTO_VALUETYPENODE_HPP

#include "opto/node.hpp"
#include "opto/connode.hpp"

class GraphKit;

class ValueTypeBaseNode : public TypeNode {
protected:
  ValueTypeBaseNode(const Type* t, int nb_fields)
    : TypeNode(t, nb_fields) {
    init_class_id(Class_ValueTypeBase);
    Compile::current()->add_value_type(this);
  }

  enum { Control,   // Control input
         Oop,       // Oop of TypeInstPtr
         Values     // Nodes corresponding to values of the value type's fields.
                    // Nodes are connected in increasing order of the index of the field they correspond to.
  };

  virtual const TypeInstPtr* value_ptr() const = 0;
  // Get the klass defining the field layout of the value type
  virtual ciValueKlass* value_klass() const = 0;

  int make_scalar_in_safepoint(PhaseIterGVN* igvn, Unique_Node_List& worklist, SafePointNode* sfpt);

  // Initialize the value type fields with the inputs or outputs of a MultiNode
  void initialize(GraphKit* kit, MultiNode* multi, ciValueKlass* vk, int base_offset, uint& base_input, bool in);

  const TypePtr* field_adr_type(Node* base, int offset, ciInstanceKlass* holder, PhaseGVN& gvn) const;

public:
  // Support for control flow merges
  bool has_phi_inputs(Node* region);
  ValueTypeBaseNode* clone_with_phis(PhaseGVN* gvn, Node* region);
  ValueTypeBaseNode* merge_with(PhaseGVN* gvn, const ValueTypeBaseNode* other, int pnum, bool transform);
  void add_new_path(Node* region);

  // Get oop for heap allocated value type (may be TypePtr::NULL_PTR)
  Node* get_oop() const    { return in(Oop); }
  void  set_oop(Node* oop) { set_req(Oop, oop); }

  // Value type fields
  uint          field_count() const { return req() - Values; }
  Node*         field_value(uint index) const;
  Node*         field_value_by_offset(int offset, bool recursive = false) const;
  void      set_field_value(uint index, Node* value);
  void      set_field_value_by_offset(int offset, Node* value);
  int           field_offset(uint index) const;
  ciType*       field_type(uint index) const;
  bool          field_is_flattened(uint index) const;
  bool          field_is_flattenable(uint index) const;

  // Replace ValueTypeNodes in debug info at safepoints with SafePointScalarObjectNodes
  void make_scalar_in_safepoints(PhaseIterGVN* igvn);

  // Store the value type as a flattened (headerless) representation
  void store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0) const;
  // Store the field values to memory
  void store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0, bool deoptimize_on_exception = false) const;
  // Initialize the value type by loading its field values from memory
  void load(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0);

  // Allocates the value type (if not yet allocated)
  ValueTypeBaseNode* allocate(GraphKit* kit, bool deoptimize_on_exception = false);
  bool is_allocated(PhaseGVN* phase) const;

  void replace_call_results(GraphKit* kit, Node* call, Compile* C);
};

//------------------------------ValueTypeNode-------------------------------------
// Node representing a value type in C2 IR
class ValueTypeNode : public ValueTypeBaseNode {
  friend class ValueTypeBaseNode;
  friend class ValueTypePtrNode;
private:
  ValueTypeNode(ciValueKlass* vk, Node* oop)
    : ValueTypeBaseNode(TypeValueType::make(vk), Values + vk->nof_declared_nonstatic_fields()) {
    init_class_id(Class_ValueType);
    init_req(Oop, oop);
  }

  // Checks if the value type is loaded from memory and if so returns the oop
  Node* is_loaded(PhaseGVN* phase, ciValueKlass* vk = NULL, Node* base = NULL, int holder_offset = 0);

  // Checks if the value type fields are all set to default values
  bool is_default(PhaseGVN& gvn) const;

  const TypeInstPtr* value_ptr() const { return TypeInstPtr::make(TypePtr::BotPTR, value_klass()); }
  ciValueKlass* value_klass() const { return type()->is_valuetype()->value_klass(); }

public:
  // Create uninitialized
  static ValueTypeNode* make_uninitialized(PhaseGVN& gvn, ciValueKlass* vk);
  // Create with default field values
  static ValueTypeNode* make_default(PhaseGVN& gvn, ciValueKlass* vk);
  // Create and initialize by loading the field values from an oop
  static ValueTypeNode* make_from_oop(GraphKit* kit, Node* oop, ciValueKlass* vk);
  // Create and initialize by loading the field values from a flattened field or array
  static ValueTypeNode* make_from_flattened(GraphKit* kit, ciValueKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0);
  // Create and initialize with the inputs or outputs of a MultiNode (method entry or call)
  static ValueTypeNode* make_from_multi(GraphKit* kit, MultiNode* multi, ciValueKlass* vk, uint& base_input, bool in);

  // Returns the constant oop of the default value type allocation
  static Node* default_oop(PhaseGVN& gvn, ciValueKlass* vk);

  // Allocate all non-flattened value type fields
  Node* allocate_fields(GraphKit* kit);

  Node* tagged_klass(PhaseGVN& gvn);
  uint pass_fields(Node* call, int base_input, GraphKit& kit, bool assert_allocated = false, ciValueKlass* base_vk = NULL, int base_offset = 0);

  // Allocation optimizations
  void remove_redundant_allocations(PhaseIterGVN* igvn, PhaseIdealLoop* phase);

  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual int Opcode() const;
};

//------------------------------ValueTypePtrNode-------------------------------------
// Node representing a value type as a pointer in C2 IR
class ValueTypePtrNode : public ValueTypeBaseNode {
private:
  const TypeInstPtr* value_ptr() const { return type()->isa_instptr(); }
  ciValueKlass* value_klass() const { return value_ptr()->value_klass(); }

  ValueTypePtrNode(ciValueKlass* vk, Node* oop)
    : ValueTypeBaseNode(TypeInstPtr::make(TypePtr::NotNull, vk), Values + vk->nof_declared_nonstatic_fields()) {
    init_class_id(Class_ValueTypePtr);
    init_req(Oop, oop);
  }

public:
  // Create and initialize with the values of a ValueTypeNode
  static ValueTypePtrNode* make_from_value_type(GraphKit* kit, ValueTypeNode* vt, bool deoptimize_on_exception = false);
  // Create and initialize by loading the field values from an oop
  static ValueTypePtrNode* make_from_oop(GraphKit* kit, Node* oop);

  virtual int Opcode() const;
};

#endif // SHARE_VM_OPTO_VALUETYPENODE_HPP
