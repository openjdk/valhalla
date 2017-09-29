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
  ValueTypeBaseNode(const Type* t, int nb_fields, Compile* C)
    : TypeNode(t, nb_fields) {
    init_class_id(Class_ValueTypeBase);
    C->add_value_type(this);
  }

  enum { Control,   // Control input
         Oop,       // Oop of TypeValueTypePtr
         Values     // Nodes corresponding to values of the value type's fields.
                    // Nodes are connected in increasing order of the index of the field
                    // they correspond to. Field indeces are defined in ciValueKlass::_field_index_map.
  };

  virtual const TypeValueTypePtr* value_type_ptr() const = 0;
  // Get the klass defining the field layout of the value type
  virtual ciValueKlass* value_klass() const = 0;
  int make_scalar_in_safepoint(Unique_Node_List& worklist, SafePointNode* sfpt, Node* root, PhaseGVN* gvn);

  static void make(PhaseGVN* gvn, Node*& ctl, Node* mem, Node* n, ValueTypeBaseNode* vt, ciValueKlass* base_vk, int base_offset, int base_input, bool in);

public:
  // Support for control flow merges
  bool has_phi_inputs(Node* region);
  ValueTypeBaseNode* clone_with_phis(PhaseGVN* gvn, Node* region);
  ValueTypeBaseNode* merge_with(PhaseGVN* gvn, const ValueTypeBaseNode* other, int pnum, bool transform);

  // Get oop for heap allocated value type (may be TypePtr::NULL_PTR)
  Node* get_oop() const    { return in(Oop); }
  void  set_oop(Node* oop) { set_req(Oop, oop); }

  // Value type fields
  uint          field_count() const { return req() - Values; }
  Node*         field_value(uint index) const;
  Node*         field_value_by_offset(int offset, bool recursive = false) const;
  void      set_field_value(uint index, Node* value);
  int           field_offset(uint index) const;
  ciType*       field_type(uint index) const;
  bool          field_is_flattened(uint index) const;

  // Replace ValueTypeNodes in debug info at safepoints with SafePointScalarObjectNodes
  void make_scalar_in_safepoints(Node* root, PhaseGVN* gvn);

  // Store the value type as a flattened (headerless) representation
  void store_flattened(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0) const;
  // Store the field values to memory
  void store(GraphKit* kit, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0) const;

  // Initialize the value type by loading its field values from memory
  void load(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* base, Node* ptr, ciInstanceKlass* holder, int holder_offset = 0);

  // Allocates the value type (if not yet allocated) and returns the oop
  ValueTypeBaseNode* allocate(GraphKit* kit);
  bool is_allocated(PhaseGVN* phase) const;

  void replace_call_results(GraphKit* kit, Node* call, Compile* C);
};

//------------------------------ValueTypeNode-------------------------------------
// Node representing a value type in C2 IR
class ValueTypeNode : public ValueTypeBaseNode {
  friend class ValueTypeBaseNode;
private:
  ValueTypeNode(const TypeValueType* t, Node* oop, Compile* C)
    : ValueTypeBaseNode(t, Values + t->value_klass()->nof_declared_nonstatic_fields(), C) {
    init_class_id(Class_ValueType);
    init_req(Oop, oop);
  }

  // Checks if the value type is loaded from memory and if so returns the oop
  Node* is_loaded(PhaseGVN* phase, const TypeValueType* t, Node* base = NULL, int holder_offset = 0);

  const TypeValueTypePtr* value_type_ptr() const { return TypeValueTypePtr::make(bottom_type()->isa_valuetype()); }
  // Get the klass defining the field layout of the value type
  ciValueKlass* value_klass() const { return type()->is_valuetype()->value_klass(); }

public:
  // Create a new ValueTypeNode with uninitialized values
  static ValueTypeNode* make(PhaseGVN& gvn, ciValueKlass* klass);
  // Create a new ValueTypeNode with default values
  static Node* make_default(PhaseGVN& gvn, ciValueKlass* vk);
  // Create a new ValueTypeNode and load its values from an oop
  static Node* make(GraphKit* kit, Node* oop, bool null_check = false);
  static Node* make(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* oop, bool null_check = false);
  // Create a new ValueTypeNode and load its values from a flattened value type field or array
  static Node* make(GraphKit* kit, ciValueKlass* vk, Node* obj, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0);
  static Node* make(PhaseGVN& gvn, ciValueKlass* vk, Node*& ctl, Node* mem, Node* obj, Node* ptr, ciInstanceKlass* holder = NULL, int holder_offset = 0);
  // Create value type node from arguments at method entry and calls
  static Node* make(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* n, ciValueKlass* vk, int base_input, bool in);

  // Allocate all non-flattened value type fields
  Node* allocate_fields(GraphKit* kit);

  Node* tagged_klass(PhaseGVN& gvn);
  void pass_klass(Node* n, uint pos, const GraphKit& kit);
  uint pass_fields(Node* call, int base_input, GraphKit& kit, bool assert_allocated = false, ciValueKlass* base_vk = NULL, int base_offset = 0);

  // Allocation optimizations
  void remove_redundant_allocations(PhaseIterGVN* igvn, PhaseIdealLoop* phase);

  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual int Opcode() const;

#ifndef PRODUCT
  virtual void dump_spec(outputStream* st) const;
#endif
};

//------------------------------ValueTypePtrNode-------------------------------------
// Node representing a value type as a pointer in C2 IR
class ValueTypePtrNode : public ValueTypeBaseNode {
private:
  ciValueKlass* value_klass() const { return type()->is_valuetypeptr()->value_type()->value_klass(); }
  const TypeValueTypePtr* value_type_ptr() const { return bottom_type()->isa_valuetypeptr(); }

  ValueTypePtrNode(ciValueKlass* vk, Node* oop, Compile* C)
    : ValueTypeBaseNode(TypeValueTypePtr::make(TypePtr::NotNull, vk), Values + vk->nof_declared_nonstatic_fields(), C) {
    init_class_id(Class_ValueTypePtr);
    init_req(Oop, oop);
  }
public:

  ValueTypePtrNode(ValueTypeNode* vt, Node* oop, Compile* C)
    : ValueTypeBaseNode(TypeValueTypePtr::make(vt->type()->is_valuetype())->cast_to_ptr_type(TypePtr::NotNull), vt->req(), C) {
    init_class_id(Class_ValueTypePtr);
    for (uint i = Oop+1; i < vt->req(); i++) {
      init_req(i, vt->in(i));
    }
    init_req(Oop, oop);
  }

  static ValueTypePtrNode* make(GraphKit* kit, ciValueKlass* vk, CallNode* call);
  static ValueTypePtrNode* make(PhaseGVN& gvn, Node*& ctl, Node* mem, Node* oop);
  virtual int Opcode() const;
};

#endif // SHARE_VM_OPTO_VALUETYPENODE_HPP
