/*
 * Copyright (c) 1998, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciMethodData.hpp"
#include "ci/ciSymbols.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compileLog.hpp"
#include "interpreter/linkResolver.hpp"
#include "jvm_io.h"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "opto/addnode.hpp"
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/divnode.hpp"
#include "opto/idealGraphPrinter.hpp"
#include "opto/idealKit.hpp"
#include "opto/inlinetypenode.hpp"
#include "opto/matcher.hpp"
#include "opto/memnode.hpp"
#include "opto/mulnode.hpp"
#include "opto/opaquenode.hpp"
#include "opto/parse.hpp"
#include "opto/runtime.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/sharedRuntime.hpp"

#ifndef PRODUCT
extern uint explicit_null_checks_inserted,
            explicit_null_checks_elided;
#endif

Node* Parse::record_profile_for_speculation_at_array_load(Node* ld) {
  // Feed unused profile data to type speculation
  if (UseTypeSpeculation && UseArrayLoadStoreProfile) {
    ciKlass* array_type = nullptr;
    ciKlass* element_type = nullptr;
    ProfilePtrKind element_ptr = ProfileMaybeNull;
    bool flat_array = true;
    bool null_free_array = true;
    method()->array_access_profiled_type(bci(), array_type, element_type, element_ptr, flat_array, null_free_array);
    if (element_type != nullptr || element_ptr != ProfileMaybeNull) {
      ld = record_profile_for_speculation(ld, element_type, element_ptr);
    }
  }
  return ld;
}


//---------------------------------array_load----------------------------------
void Parse::array_load(BasicType bt) {
  const Type* elemtype = Type::TOP;
  Node* adr = array_addressing(bt, 0, elemtype);
  if (stopped())  return;     // guaranteed null or range check

  Node* array_index = pop();
  Node* array = pop();

  // Handle inline type arrays
  const TypeOopPtr* element_ptr = elemtype->make_oopptr();
  const TypeAryPtr* array_type = _gvn.type(array)->is_aryptr();

  if (!array_type->is_not_flat()) {
    // Cannot statically determine if array is a flat array, emit runtime check
    assert(UseArrayFlattening && is_reference_type(bt) && element_ptr->can_be_inline_type() &&
           (!element_ptr->is_inlinetypeptr() || element_ptr->inline_klass()->maybe_flat_in_array()), "array can't be flat");
    IdealKit ideal(this);
    IdealVariable res(ideal);
    ideal.declarations_done();
    ideal.if_then(flat_array_test(array, /* flat = */ false)); {
      // Non-flat array
      sync_kit(ideal);
      if (!array_type->is_flat()) {
        assert(array_type->is_flat() || control()->in(0)->as_If()->is_flat_array_check(&_gvn), "Should be found");
        const TypeAryPtr* adr_type = TypeAryPtr::get_array_body_type(bt);
        DecoratorSet decorator_set = IN_HEAP | IS_ARRAY | C2_CONTROL_DEPENDENT_LOAD;
        if (needs_range_check(array_type->size(), array_index)) {
          // We've emitted a RangeCheck but now insert an additional check between the range check and the actual load.
          // We cannot pin the load to two separate nodes. Instead, we pin it conservatively here such that it cannot
          // possibly float above the range check at any point.
          decorator_set |= C2_UNKNOWN_CONTROL_LOAD;
        }
        Node* ld = access_load_at(array, adr, adr_type, element_ptr, bt, decorator_set);
        if (element_ptr->is_inlinetypeptr()) {
          ld = InlineTypeNode::make_from_oop(this, ld, element_ptr->inline_klass());
        }
        ideal.set(res, ld);
      }
      ideal.sync_kit(this);
    } ideal.else_(); {
      // Flat array
      sync_kit(ideal);
      if (!array_type->is_not_flat()) {
        if (element_ptr->is_inlinetypeptr()) {
          ciInlineKlass* vk = element_ptr->inline_klass();
          Node* flat_array = cast_to_flat_array(array, vk, false, false, false);
          Node* vt = InlineTypeNode::make_from_flat_array(this, vk, flat_array, array_index);
          ideal.set(res, vt);
        } else {
          // Element type is unknown, and thus we cannot statically determine the exact flat array layout. Emit a
          // runtime call to correctly load the inline type element from the flat array.
          Node* inline_type = load_from_unknown_flat_array(array, array_index, element_ptr);
          bool is_null_free = array_type->is_null_free() || !UseNullableValueFlattening;
          if (is_null_free) {
            inline_type = cast_not_null(inline_type);
          }
          ideal.set(res, inline_type);
        }
      }
      ideal.sync_kit(this);
    } ideal.end_if();
    sync_kit(ideal);
    Node* ld = _gvn.transform(ideal.value(res));
    ld = record_profile_for_speculation_at_array_load(ld);
    push_node(bt, ld);
    return;
  }

  if (elemtype == TypeInt::BOOL) {
    bt = T_BOOLEAN;
  }
  const TypeAryPtr* adr_type = TypeAryPtr::get_array_body_type(bt);
  Node* ld = access_load_at(array, adr, adr_type, elemtype, bt,
                            IN_HEAP | IS_ARRAY | C2_CONTROL_DEPENDENT_LOAD);
  ld = record_profile_for_speculation_at_array_load(ld);
  // Loading an inline type from a non-flat array
  if (element_ptr != nullptr && element_ptr->is_inlinetypeptr()) {
    assert(!array_type->is_null_free() || !element_ptr->maybe_null(), "inline type array elements should never be null");
    ld = InlineTypeNode::make_from_oop(this, ld, element_ptr->inline_klass());
  }
  push_node(bt, ld);
}

Node* Parse::load_from_unknown_flat_array(Node* array, Node* array_index, const TypeOopPtr* element_ptr) {
  // Below membars keep this access to an unknown flat array correctly
  // ordered with other unknown and known flat array accesses.
  insert_mem_bar_volatile(Op_MemBarCPUOrder, C->get_alias_index(TypeAryPtr::INLINES));

  Node* call = nullptr;
  {
    // Re-execute flat array load if runtime call triggers deoptimization
    PreserveReexecuteState preexecs(this);
    jvms()->set_bci(_bci);
    jvms()->set_should_reexecute(true);
    inc_sp(2);
    kill_dead_locals();
    call = make_runtime_call(RC_NO_LEAF | RC_NO_IO,
                             OptoRuntime::load_unknown_inline_Type(),
                             OptoRuntime::load_unknown_inline_Java(),
                             nullptr, TypeRawPtr::BOTTOM,
                             array, array_index);
  }
  make_slow_call_ex(call, env()->Throwable_klass(), false);
  Node* buffer = _gvn.transform(new ProjNode(call, TypeFunc::Parms));

  insert_mem_bar_volatile(Op_MemBarCPUOrder, C->get_alias_index(TypeAryPtr::INLINES));

  // Keep track of the information that the inline type is in flat arrays
  const Type* unknown_value = element_ptr->is_instptr()->cast_to_flat_in_array();
  return _gvn.transform(new CheckCastPPNode(control(), buffer, unknown_value));
}

//--------------------------------array_store----------------------------------
void Parse::array_store(BasicType bt) {
  const Type* elemtype = Type::TOP;
  Node* adr = array_addressing(bt, type2size[bt], elemtype);
  if (stopped())  return;     // guaranteed null or range check
  Node* stored_value_casted = nullptr;
  if (bt == T_OBJECT) {
    stored_value_casted = array_store_check(adr, elemtype);
    if (stopped()) {
      return;
    }
  }
  Node* const stored_value = pop_node(bt); // Value to store
  Node* const array_index = pop();         // Index in the array
  Node* array = pop();                     // The array itself

  const TypeAryPtr* array_type = _gvn.type(array)->is_aryptr();
  const TypeAryPtr* adr_type = TypeAryPtr::get_array_body_type(bt);

  if (elemtype == TypeInt::BOOL) {
    bt = T_BOOLEAN;
  } else if (bt == T_OBJECT) {
    elemtype = elemtype->make_oopptr();
    const Type* stored_value_casted_type = _gvn.type(stored_value_casted);
    // Based on the value to be stored, try to determine if the array is not null-free and/or not flat.
    // This is only legal for non-null stores because the array_store_check always passes for null, even
    // if the array is null-free. Null stores are handled in GraphKit::inline_array_null_guard().
    bool not_inline = !stored_value_casted_type->maybe_null() && !stored_value_casted_type->is_oopptr()->can_be_inline_type();
    bool not_null_free = not_inline;
    bool not_flat = not_inline || ( stored_value_casted_type->is_inlinetypeptr() &&
                                   !stored_value_casted_type->inline_klass()->maybe_flat_in_array());
    if (!array_type->is_not_null_free() && not_null_free) {
      // Storing a non-inline type, mark array as not null-free.
      array_type = array_type->cast_to_not_null_free();
      Node* cast = _gvn.transform(new CheckCastPPNode(control(), array, array_type));
      replace_in_map(array, cast);
      array = cast;
    }
    if (!array_type->is_not_flat() && not_flat) {
      // Storing to a non-flat array, mark array as not flat.
      array_type = array_type->cast_to_not_flat();
      Node* cast = _gvn.transform(new CheckCastPPNode(control(), array, array_type));
      replace_in_map(array, cast);
      array = cast;
    }

    if (!array_type->is_flat() && array_type->is_null_free()) {
      // Store to non-flat null-free inline type array (elements can never be null)
      assert(!stored_value_casted_type->maybe_null(), "should be guaranteed by array store check");
      if (elemtype->is_inlinetypeptr() && elemtype->inline_klass()->is_empty()) {
        // Ignore empty inline stores, array is already initialized.
        return;
      }
    } else if (!array_type->is_not_flat()) {
      // Array might be a flat array, emit runtime checks (for nullptr, a simple inline_array_null_guard is sufficient).
      assert(UseArrayFlattening && !not_flat && elemtype->is_oopptr()->can_be_inline_type() &&
             (!array_type->klass_is_exact() || array_type->is_flat()), "array can't be a flat array");
      // TODO 8350865 Depending on the available layouts, we can avoid this check in below flat/not-flat branches. Also the safe_for_replace arg is now always true.
      array = inline_array_null_guard(array, stored_value_casted, 3, true);
      IdealKit ideal(this);
      ideal.if_then(flat_array_test(array, /* flat = */ false)); {
        // Non-flat array
        if (!array_type->is_flat()) {
          sync_kit(ideal);
          assert(array_type->is_flat() || ideal.ctrl()->in(0)->as_If()->is_flat_array_check(&_gvn), "Should be found");
          inc_sp(3);
          access_store_at(array, adr, adr_type, stored_value_casted, elemtype, bt, MO_UNORDERED | IN_HEAP | IS_ARRAY, false);
          dec_sp(3);
          ideal.sync_kit(this);
        }
      } ideal.else_(); {
        // Flat array
        sync_kit(ideal);
        if (!array_type->is_not_flat()) {
          // Try to determine the inline klass type of the stored value
          ciInlineKlass* vk = nullptr;
          if (stored_value_casted_type->is_inlinetypeptr()) {
            vk = stored_value_casted_type->inline_klass();
          } else if (elemtype->is_inlinetypeptr()) {
            vk = elemtype->inline_klass();
          }

          if (vk != nullptr) {
            // Element type is known, cast and store to flat array layout.
            Node* flat_array = cast_to_flat_array(array, vk, false, false, false);

            // Re-execute flat array store if buffering triggers deoptimization
            PreserveReexecuteState preexecs(this);
            jvms()->set_should_reexecute(true);
            inc_sp(3);

            if (!stored_value_casted->is_InlineType()) {
              assert(_gvn.type(stored_value_casted) == TypePtr::NULL_PTR, "Unexpected value");
              stored_value_casted = InlineTypeNode::make_null(_gvn, vk);
            }

            stored_value_casted->as_InlineType()->store_flat_array(this, flat_array, array_index);
          } else {
            // Element type is unknown, emit a runtime call since the flat array layout is not statically known.
            store_to_unknown_flat_array(array, array_index, stored_value_casted);
          }
        }
        ideal.sync_kit(this);
      }
      ideal.end_if();
      sync_kit(ideal);
      return;
    } else if (!array_type->is_not_null_free()) {
      // Array is not flat but may be null free
      assert(elemtype->is_oopptr()->can_be_inline_type(), "array can't be null-free");
      array = inline_array_null_guard(array, stored_value_casted, 3, true);
    }
  }
  inc_sp(3);
  access_store_at(array, adr, adr_type, stored_value, elemtype, bt, MO_UNORDERED | IN_HEAP | IS_ARRAY);
  dec_sp(3);
}

// Emit a runtime call to store to a flat array whose element type is either unknown (i.e. we do not know the flat
// array layout) or not exact (could have different flat array layouts at runtime).
void Parse::store_to_unknown_flat_array(Node* array, Node* const idx, Node* non_null_stored_value) {
  // Below membars keep this access to an unknown flat array correctly
  // ordered with other unknown and known flat array accesses.
  insert_mem_bar_volatile(Op_MemBarCPUOrder, C->get_alias_index(TypeAryPtr::INLINES));

  Node* call = nullptr;
  {
    // Re-execute flat array store if runtime call triggers deoptimization
    PreserveReexecuteState preexecs(this);
    jvms()->set_bci(_bci);
    jvms()->set_should_reexecute(true);
    inc_sp(3);
    kill_dead_locals();
    call = make_runtime_call(RC_NO_LEAF | RC_NO_IO,
                      OptoRuntime::store_unknown_inline_Type(),
                      OptoRuntime::store_unknown_inline_Java(),
                      nullptr, TypeRawPtr::BOTTOM,
                      non_null_stored_value, array, idx);
  }
  make_slow_call_ex(call, env()->Throwable_klass(), false);

  insert_mem_bar_volatile(Op_MemBarCPUOrder, C->get_alias_index(TypeAryPtr::INLINES));
}

//------------------------------array_addressing-------------------------------
// Pull array and index from the stack.  Compute pointer-to-element.
Node* Parse::array_addressing(BasicType type, int vals, const Type*& elemtype) {
  Node *idx   = peek(0+vals);   // Get from stack without popping
  Node *ary   = peek(1+vals);   // in case of exception

  // Null check the array base, with correct stack contents
  ary = null_check(ary, T_ARRAY);
  // Compile-time detect of null-exception?
  if (stopped())  return top();

  const TypeAryPtr* arytype  = _gvn.type(ary)->is_aryptr();
  const TypeInt*    sizetype = arytype->size();
  elemtype = arytype->elem();

  if (UseUniqueSubclasses) {
    const Type* el = elemtype->make_ptr();
    if (el && el->isa_instptr()) {
      const TypeInstPtr* toop = el->is_instptr();
      if (toop->instance_klass()->unique_concrete_subklass()) {
        // If we load from "AbstractClass[]" we must see "ConcreteSubClass".
        const Type* subklass = Type::get_const_type(toop->instance_klass());
        elemtype = subklass->join_speculative(el);
      }
    }
  }

  if (!arytype->is_loaded()) {
    // Only fails for some -Xcomp runs
    // The class is unloaded.  We have to run this bytecode in the interpreter.
    ciKlass* klass = arytype->unloaded_klass();

    uncommon_trap(Deoptimization::Reason_unloaded,
                  Deoptimization::Action_reinterpret,
                  klass, "!loaded array");
    return top();
  }

  ary = create_speculative_inline_type_array_checks(ary, arytype, elemtype);

  if (needs_range_check(sizetype, idx)) {
    create_range_check(idx, ary, sizetype);
  } else if (C->log() != nullptr) {
    C->log()->elem("observe that='!need_range_check'");
  }

  // Check for always knowing you are throwing a range-check exception
  if (stopped())  return top();

  // Make array address computation control dependent to prevent it
  // from floating above the range check during loop optimizations.
  Node* ptr = array_element_address(ary, idx, type, sizetype, control());
  assert(ptr != top(), "top should go hand-in-hand with stopped");

  return ptr;
}

// Check if we need a range check for an array access. This is the case if the index is either negative or if it could
// be greater or equal the smallest possible array size (i.e. out-of-bounds).
bool Parse::needs_range_check(const TypeInt* size_type, const Node* index) const {
  const TypeInt* index_type = _gvn.type(index)->is_int();
  return index_type->_hi >= size_type->_lo || index_type->_lo < 0;
}

void Parse::create_range_check(Node* idx, Node* ary, const TypeInt* sizetype) {
  Node* tst;
  if (sizetype->_hi <= 0) {
    // The greatest array bound is negative, so we can conclude that we're
    // compiling unreachable code, but the unsigned compare trick used below
    // only works with non-negative lengths.  Instead, hack "tst" to be zero so
    // the uncommon_trap path will always be taken.
    tst = _gvn.intcon(0);
  } else {
    // Range is constant in array-oop, so we can use the original state of mem
    Node* len = load_array_length(ary);

    // Test length vs index (standard trick using unsigned compare)
    Node* chk = _gvn.transform(new CmpUNode(idx, len) );
    BoolTest::mask btest = BoolTest::lt;
    tst = _gvn.transform(new BoolNode(chk, btest) );
  }
  RangeCheckNode* rc = new RangeCheckNode(control(), tst, PROB_MAX, COUNT_UNKNOWN);
  _gvn.set_type(rc, rc->Value(&_gvn));
  if (!tst->is_Con()) {
    record_for_igvn(rc);
  }
  set_control(_gvn.transform(new IfTrueNode(rc)));
  // Branch to failure if out of bounds
  {
    PreserveJVMState pjvms(this);
    set_control(_gvn.transform(new IfFalseNode(rc)));
    if (C->allow_range_check_smearing()) {
      // Do not use builtin_throw, since range checks are sometimes
      // made more stringent by an optimistic transformation.
      // This creates "tentative" range checks at this point,
      // which are not guaranteed to throw exceptions.
      // See IfNode::Ideal, is_range_check, adjust_check.
      uncommon_trap(Deoptimization::Reason_range_check,
                    Deoptimization::Action_make_not_entrant,
                    nullptr, "range_check");
    } else {
      // If we have already recompiled with the range-check-widening
      // heroic optimization turned off, then we must really be throwing
      // range check exceptions.
      builtin_throw(Deoptimization::Reason_range_check);
    }
  }
}

// For inline type arrays, we can use the profiling information for array accesses to speculate on the type, flatness,
// and null-freeness. We can either prepare the speculative type for later uses or emit explicit speculative checks with
// traps now. In the latter case, the speculative type guarantees can avoid additional runtime checks later (e.g.
// non-null-free implies non-flat which allows us to remove flatness checks). This makes the graph simpler.
Node* Parse::create_speculative_inline_type_array_checks(Node* array, const TypeAryPtr* array_type,
                                                         const Type*& element_type) {
  if (!array_type->is_flat() && !array_type->is_not_flat()) {
    // For arrays that might be flat, speculate that the array has the exact type reported in the profile data such that
    // we can rely on a fixed memory layout (i.e. either a flat layout or not).
    array = cast_to_speculative_array_type(array, array_type, element_type);
  } else if (UseTypeSpeculation && UseArrayLoadStoreProfile) {
    // Array is known to be either flat or not flat. If possible, update the speculative type by using the profile data
    // at this bci.
    array = cast_to_profiled_array_type(array);
  }

  // Even though the type does not tell us whether we have an inline type array or not, we can still check the profile data
  // whether we have a non-null-free or non-flat array. Speculating on a non-null-free array doesn't help aaload but could
  // be profitable for a subsequent aastore.
  if (!array_type->is_null_free() && !array_type->is_not_null_free()) {
    array = speculate_non_null_free_array(array, array_type);
  }
  if (!array_type->is_flat() && !array_type->is_not_flat()) {
    array = speculate_non_flat_array(array, array_type);
  }
  return array;
}

// Speculate that the array has the exact type reported in the profile data. We emit a trap when this turns out to be
// wrong. On the fast path, we add a CheckCastPP to use the exact type.
Node* Parse::cast_to_speculative_array_type(Node* const array, const TypeAryPtr*& array_type, const Type*& element_type) {
  Deoptimization::DeoptReason reason = Deoptimization::Reason_speculate_class_check;
  ciKlass* speculative_array_type = array_type->speculative_type();
  if (too_many_traps_or_recompiles(reason) || speculative_array_type == nullptr) {
    // No speculative type, check profile data at this bci
    speculative_array_type = nullptr;
    reason = Deoptimization::Reason_class_check;
    if (UseArrayLoadStoreProfile && !too_many_traps_or_recompiles(reason)) {
      ciKlass* profiled_element_type = nullptr;
      ProfilePtrKind element_ptr = ProfileMaybeNull;
      bool flat_array = true;
      bool null_free_array = true;
      method()->array_access_profiled_type(bci(), speculative_array_type, profiled_element_type, element_ptr, flat_array,
                                           null_free_array);
    }
  }
  if (speculative_array_type != nullptr) {
    // Speculate that this array has the exact type reported by profile data
    Node* casted_array = nullptr;
    DEBUG_ONLY(Node* old_control = control();)
    Node* slow_ctl = type_check_receiver(array, speculative_array_type, 1.0, &casted_array);
    if (stopped()) {
      // The check always fails and therefore profile information is incorrect. Don't use it.
      assert(old_control == slow_ctl, "type check should have been removed");
      set_control(slow_ctl);
    } else if (!slow_ctl->is_top()) {
      { PreserveJVMState pjvms(this);
        set_control(slow_ctl);
        uncommon_trap_exact(reason, Deoptimization::Action_maybe_recompile);
      }
      replace_in_map(array, casted_array);
      array_type = _gvn.type(casted_array)->is_aryptr();
      element_type = array_type->elem();
      return casted_array;
    }
  }
  return array;
}

// Create a CheckCastPP when the speculative type can improve the current type.
Node* Parse::cast_to_profiled_array_type(Node* const array) {
  ciKlass* array_type = nullptr;
  ciKlass* element_type = nullptr;
  ProfilePtrKind element_ptr = ProfileMaybeNull;
  bool flat_array = true;
  bool null_free_array = true;
  method()->array_access_profiled_type(bci(), array_type, element_type, element_ptr, flat_array, null_free_array);
  if (array_type != nullptr) {
    return record_profile_for_speculation(array, array_type, ProfileMaybeNull);
  }
  return array;
}

// Speculate that the array is non-null-free. We emit a trap when this turns out to be
// wrong. On the fast path, we add a CheckCastPP to use the non-null-free type.
Node* Parse::speculate_non_null_free_array(Node* const array, const TypeAryPtr*& array_type) {
  bool null_free_array = true;
  Deoptimization::DeoptReason reason = Deoptimization::Reason_none;
  if (array_type->speculative() != nullptr &&
      array_type->speculative()->is_aryptr()->is_not_null_free() &&
      !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_class_check)) {
    null_free_array = false;
    reason = Deoptimization::Reason_speculate_class_check;
  } else if (UseArrayLoadStoreProfile && !too_many_traps_or_recompiles(Deoptimization::Reason_class_check)) {
    ciKlass* profiled_array_type = nullptr;
    ciKlass* profiled_element_type = nullptr;
    ProfilePtrKind element_ptr = ProfileMaybeNull;
    bool flat_array = true;
    method()->array_access_profiled_type(bci(), profiled_array_type, profiled_element_type, element_ptr, flat_array,
                                         null_free_array);
    reason = Deoptimization::Reason_class_check;
  }
  if (!null_free_array) {
    { // Deoptimize if null-free array
      BuildCutout unless(this, null_free_array_test(array, /* null_free = */ false), PROB_MAX);
      uncommon_trap_exact(reason, Deoptimization::Action_maybe_recompile);
    }
    assert(!stopped(), "null-free array should have been caught earlier");
    Node* casted_array = _gvn.transform(new CheckCastPPNode(control(), array, array_type->cast_to_not_null_free()));
    replace_in_map(array, casted_array);
    array_type = _gvn.type(casted_array)->is_aryptr();
    return casted_array;
  }
  return array;
}

// Speculate that the array is non-flat. We emit a trap when this turns out to be wrong.
// On the fast path, we add a CheckCastPP to use the non-flat type.
Node* Parse::speculate_non_flat_array(Node* const array, const TypeAryPtr* const array_type) {
  bool flat_array = true;
  Deoptimization::DeoptReason reason = Deoptimization::Reason_none;
  if (array_type->speculative() != nullptr &&
      array_type->speculative()->is_aryptr()->is_not_flat() &&
      !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_class_check)) {
    flat_array = false;
    reason = Deoptimization::Reason_speculate_class_check;
  } else if (UseArrayLoadStoreProfile && !too_many_traps_or_recompiles(reason)) {
    ciKlass* profiled_array_type = nullptr;
    ciKlass* profiled_element_type = nullptr;
    ProfilePtrKind element_ptr = ProfileMaybeNull;
    bool null_free_array = true;
    method()->array_access_profiled_type(bci(), profiled_array_type, profiled_element_type, element_ptr, flat_array,
                                         null_free_array);
    reason = Deoptimization::Reason_class_check;
  }
  if (!flat_array) {
    { // Deoptimize if flat array
      BuildCutout unless(this, flat_array_test(array, /* flat = */ false), PROB_MAX);
      uncommon_trap_exact(reason, Deoptimization::Action_maybe_recompile);
    }
    assert(!stopped(), "flat array should have been caught earlier");
    Node* casted_array = _gvn.transform(new CheckCastPPNode(control(), array, array_type->cast_to_not_flat()));
    replace_in_map(array, casted_array);
    return casted_array;
  }
  return array;
}

// returns IfNode
IfNode* Parse::jump_if_fork_int(Node* a, Node* b, BoolTest::mask mask, float prob, float cnt) {
  Node   *cmp = _gvn.transform(new CmpINode(a, b)); // two cases: shiftcount > 32 and shiftcount <= 32
  Node   *tst = _gvn.transform(new BoolNode(cmp, mask));
  IfNode *iff = create_and_map_if(control(), tst, prob, cnt);
  return iff;
}


// sentinel value for the target bci to mark never taken branches
// (according to profiling)
static const int never_reached = INT_MAX;

//------------------------------helper for tableswitch-------------------------
void Parse::jump_if_true_fork(IfNode *iff, int dest_bci_if_true, bool unc) {
  // True branch, use existing map info
  { PreserveJVMState pjvms(this);
    Node *iftrue  = _gvn.transform( new IfTrueNode (iff) );
    set_control( iftrue );
    if (unc) {
      repush_if_args();
      uncommon_trap(Deoptimization::Reason_unstable_if,
                    Deoptimization::Action_reinterpret,
                    nullptr,
                    "taken always");
    } else {
      assert(dest_bci_if_true != never_reached, "inconsistent dest");
      merge_new_path(dest_bci_if_true);
    }
  }

  // False branch
  Node *iffalse = _gvn.transform( new IfFalseNode(iff) );
  set_control( iffalse );
}

void Parse::jump_if_false_fork(IfNode *iff, int dest_bci_if_true, bool unc) {
  // True branch, use existing map info
  { PreserveJVMState pjvms(this);
    Node *iffalse  = _gvn.transform( new IfFalseNode (iff) );
    set_control( iffalse );
    if (unc) {
      repush_if_args();
      uncommon_trap(Deoptimization::Reason_unstable_if,
                    Deoptimization::Action_reinterpret,
                    nullptr,
                    "taken never");
    } else {
      assert(dest_bci_if_true != never_reached, "inconsistent dest");
      merge_new_path(dest_bci_if_true);
    }
  }

  // False branch
  Node *iftrue = _gvn.transform( new IfTrueNode(iff) );
  set_control( iftrue );
}

void Parse::jump_if_always_fork(int dest_bci, bool unc) {
  // False branch, use existing map and control()
  if (unc) {
    repush_if_args();
    uncommon_trap(Deoptimization::Reason_unstable_if,
                  Deoptimization::Action_reinterpret,
                  nullptr,
                  "taken never");
  } else {
    assert(dest_bci != never_reached, "inconsistent dest");
    merge_new_path(dest_bci);
  }
}


extern "C" {
  static int jint_cmp(const void *i, const void *j) {
    int a = *(jint *)i;
    int b = *(jint *)j;
    return a > b ? 1 : a < b ? -1 : 0;
  }
}


class SwitchRange : public StackObj {
  // a range of integers coupled with a bci destination
  jint _lo;                     // inclusive lower limit
  jint _hi;                     // inclusive upper limit
  int _dest;
  float _cnt;                   // how many times this range was hit according to profiling

public:
  jint lo() const              { return _lo;   }
  jint hi() const              { return _hi;   }
  int  dest() const            { return _dest; }
  bool is_singleton() const    { return _lo == _hi; }
  float cnt() const            { return _cnt; }

  void setRange(jint lo, jint hi, int dest, float cnt) {
    assert(lo <= hi, "must be a non-empty range");
    _lo = lo, _hi = hi; _dest = dest; _cnt = cnt;
    assert(_cnt >= 0, "");
  }
  bool adjoinRange(jint lo, jint hi, int dest, float cnt, bool trim_ranges) {
    assert(lo <= hi, "must be a non-empty range");
    if (lo == _hi+1) {
      // see merge_ranges() comment below
      if (trim_ranges) {
        if (cnt == 0) {
          if (_cnt != 0) {
            return false;
          }
          if (dest != _dest) {
            _dest = never_reached;
          }
        } else {
          if (_cnt == 0) {
            return false;
          }
          if (dest != _dest) {
            return false;
          }
        }
      } else {
        if (dest != _dest) {
          return false;
        }
      }
      _hi = hi;
      _cnt += cnt;
      return true;
    }
    return false;
  }

  void set (jint value, int dest, float cnt) {
    setRange(value, value, dest, cnt);
  }
  bool adjoin(jint value, int dest, float cnt, bool trim_ranges) {
    return adjoinRange(value, value, dest, cnt, trim_ranges);
  }
  bool adjoin(SwitchRange& other) {
    return adjoinRange(other._lo, other._hi, other._dest, other._cnt, false);
  }

  void print() {
    if (is_singleton())
      tty->print(" {%d}=>%d (cnt=%f)", lo(), dest(), cnt());
    else if (lo() == min_jint)
      tty->print(" {..%d}=>%d (cnt=%f)", hi(), dest(), cnt());
    else if (hi() == max_jint)
      tty->print(" {%d..}=>%d (cnt=%f)", lo(), dest(), cnt());
    else
      tty->print(" {%d..%d}=>%d (cnt=%f)", lo(), hi(), dest(), cnt());
  }
};

// We try to minimize the number of ranges and the size of the taken
// ones using profiling data. When ranges are created,
// SwitchRange::adjoinRange() only allows 2 adjoining ranges to merge
// if both were never hit or both were hit to build longer unreached
// ranges. Here, we now merge adjoining ranges with the same
// destination and finally set destination of unreached ranges to the
// special value never_reached because it can help minimize the number
// of tests that are necessary.
//
// For instance:
// [0, 1] to target1 sometimes taken
// [1, 2] to target1 never taken
// [2, 3] to target2 never taken
// would lead to:
// [0, 1] to target1 sometimes taken
// [1, 3] never taken
//
// (first 2 ranges to target1 are not merged)
static void merge_ranges(SwitchRange* ranges, int& rp) {
  if (rp == 0) {
    return;
  }
  int shift = 0;
  for (int j = 0; j < rp; j++) {
    SwitchRange& r1 = ranges[j-shift];
    SwitchRange& r2 = ranges[j+1];
    if (r1.adjoin(r2)) {
      shift++;
    } else if (shift > 0) {
      ranges[j+1-shift] = r2;
    }
  }
  rp -= shift;
  for (int j = 0; j <= rp; j++) {
    SwitchRange& r = ranges[j];
    if (r.cnt() == 0 && r.dest() != never_reached) {
      r.setRange(r.lo(), r.hi(), never_reached, r.cnt());
    }
  }
}

//-------------------------------do_tableswitch--------------------------------
void Parse::do_tableswitch() {
  // Get information about tableswitch
  int default_dest = iter().get_dest_table(0);
  jint lo_index    = iter().get_int_table(1);
  jint hi_index    = iter().get_int_table(2);
  int len          = hi_index - lo_index + 1;

  if (len < 1) {
    // If this is a backward branch, add safepoint
    maybe_add_safepoint(default_dest);
    pop(); // the effect of the instruction execution on the operand stack
    merge(default_dest);
    return;
  }

  ciMethodData* methodData = method()->method_data();
  ciMultiBranchData* profile = nullptr;
  if (methodData->is_mature() && UseSwitchProfiling) {
    ciProfileData* data = methodData->bci_to_data(bci());
    if (data != nullptr && data->is_MultiBranchData()) {
      profile = (ciMultiBranchData*)data;
    }
  }
  bool trim_ranges = !C->too_many_traps(method(), bci(), Deoptimization::Reason_unstable_if);

  // generate decision tree, using trichotomy when possible
  int rnum = len+2;
  bool makes_backward_branch = (default_dest <= bci());
  SwitchRange* ranges = NEW_RESOURCE_ARRAY(SwitchRange, rnum);
  int rp = -1;
  if (lo_index != min_jint) {
    float cnt = 1.0F;
    if (profile != nullptr) {
      cnt = (float)profile->default_count() / (hi_index != max_jint ? 2.0F : 1.0F);
    }
    ranges[++rp].setRange(min_jint, lo_index-1, default_dest, cnt);
  }
  for (int j = 0; j < len; j++) {
    jint match_int = lo_index+j;
    int  dest      = iter().get_dest_table(j+3);
    makes_backward_branch |= (dest <= bci());
    float cnt = 1.0F;
    if (profile != nullptr) {
      cnt = (float)profile->count_at(j);
    }
    if (rp < 0 || !ranges[rp].adjoin(match_int, dest, cnt, trim_ranges)) {
      ranges[++rp].set(match_int, dest, cnt);
    }
  }
  jint highest = lo_index+(len-1);
  assert(ranges[rp].hi() == highest, "");
  if (highest != max_jint) {
    float cnt = 1.0F;
    if (profile != nullptr) {
      cnt = (float)profile->default_count() / (lo_index != min_jint ? 2.0F : 1.0F);
    }
    if (!ranges[rp].adjoinRange(highest+1, max_jint, default_dest, cnt, trim_ranges)) {
      ranges[++rp].setRange(highest+1, max_jint, default_dest, cnt);
    }
  }
  assert(rp < len+2, "not too many ranges");

  if (trim_ranges) {
    merge_ranges(ranges, rp);
  }

  // Safepoint in case if backward branch observed
  if (makes_backward_branch) {
    add_safepoint();
  }

  Node* lookup = pop(); // lookup value
  jump_switch_ranges(lookup, &ranges[0], &ranges[rp]);
}


//------------------------------do_lookupswitch--------------------------------
void Parse::do_lookupswitch() {
  // Get information about lookupswitch
  int default_dest = iter().get_dest_table(0);
  jint len          = iter().get_int_table(1);

  if (len < 1) {    // If this is a backward branch, add safepoint
    maybe_add_safepoint(default_dest);
    pop(); // the effect of the instruction execution on the operand stack
    merge(default_dest);
    return;
  }

  ciMethodData* methodData = method()->method_data();
  ciMultiBranchData* profile = nullptr;
  if (methodData->is_mature() && UseSwitchProfiling) {
    ciProfileData* data = methodData->bci_to_data(bci());
    if (data != nullptr && data->is_MultiBranchData()) {
      profile = (ciMultiBranchData*)data;
    }
  }
  bool trim_ranges = !C->too_many_traps(method(), bci(), Deoptimization::Reason_unstable_if);

  // generate decision tree, using trichotomy when possible
  jint* table = NEW_RESOURCE_ARRAY(jint, len*3);
  {
    for (int j = 0; j < len; j++) {
      table[3*j+0] = iter().get_int_table(2+2*j);
      table[3*j+1] = iter().get_dest_table(2+2*j+1);
      // Handle overflow when converting from uint to jint
      table[3*j+2] = (profile == nullptr) ? 1 : (jint)MIN2<uint>((uint)max_jint, profile->count_at(j));
    }
    qsort(table, len, 3*sizeof(table[0]), jint_cmp);
  }

  float default_cnt = 1.0F;
  if (profile != nullptr) {
    juint defaults = max_juint - len;
    default_cnt = (float)profile->default_count()/(float)defaults;
  }

  int rnum = len*2+1;
  bool makes_backward_branch = (default_dest <= bci());
  SwitchRange* ranges = NEW_RESOURCE_ARRAY(SwitchRange, rnum);
  int rp = -1;
  for (int j = 0; j < len; j++) {
    jint match_int   = table[3*j+0];
    jint  dest        = table[3*j+1];
    jint  cnt         = table[3*j+2];
    jint  next_lo     = rp < 0 ? min_jint : ranges[rp].hi()+1;
    makes_backward_branch |= (dest <= bci());
    float c = default_cnt * ((float)match_int - (float)next_lo);
    if (match_int != next_lo && (rp < 0 || !ranges[rp].adjoinRange(next_lo, match_int-1, default_dest, c, trim_ranges))) {
      assert(default_dest != never_reached, "sentinel value for dead destinations");
      ranges[++rp].setRange(next_lo, match_int-1, default_dest, c);
    }
    if (rp < 0 || !ranges[rp].adjoin(match_int, dest, (float)cnt, trim_ranges)) {
      assert(dest != never_reached, "sentinel value for dead destinations");
      ranges[++rp].set(match_int, dest,  (float)cnt);
    }
  }
  jint highest = table[3*(len-1)];
  assert(ranges[rp].hi() == highest, "");
  if (highest != max_jint &&
      !ranges[rp].adjoinRange(highest+1, max_jint, default_dest, default_cnt * ((float)max_jint - (float)highest), trim_ranges)) {
    ranges[++rp].setRange(highest+1, max_jint, default_dest, default_cnt * ((float)max_jint - (float)highest));
  }
  assert(rp < rnum, "not too many ranges");

  if (trim_ranges) {
    merge_ranges(ranges, rp);
  }

  // Safepoint in case backward branch observed
  if (makes_backward_branch) {
    add_safepoint();
  }

  Node *lookup = pop(); // lookup value
  jump_switch_ranges(lookup, &ranges[0], &ranges[rp]);
}

static float if_prob(float taken_cnt, float total_cnt) {
  assert(taken_cnt <= total_cnt, "");
  if (total_cnt == 0) {
    return PROB_FAIR;
  }
  float p = taken_cnt / total_cnt;
  return clamp(p, PROB_MIN, PROB_MAX);
}

static float if_cnt(float cnt) {
  if (cnt == 0) {
    return COUNT_UNKNOWN;
  }
  return cnt;
}

static float sum_of_cnts(SwitchRange *lo, SwitchRange *hi) {
  float total_cnt = 0;
  for (SwitchRange* sr = lo; sr <= hi; sr++) {
    total_cnt += sr->cnt();
  }
  return total_cnt;
}

class SwitchRanges : public ResourceObj {
public:
  SwitchRange* _lo;
  SwitchRange* _hi;
  SwitchRange* _mid;
  float _cost;

  enum {
    Start,
    LeftDone,
    RightDone,
    Done
  } _state;

  SwitchRanges(SwitchRange *lo, SwitchRange *hi)
    : _lo(lo), _hi(hi), _mid(nullptr),
      _cost(0), _state(Start) {
  }

  SwitchRanges()
    : _lo(nullptr), _hi(nullptr), _mid(nullptr),
      _cost(0), _state(Start) {}
};

// Estimate cost of performing a binary search on lo..hi
static float compute_tree_cost(SwitchRange *lo, SwitchRange *hi, float total_cnt) {
  GrowableArray<SwitchRanges> tree;
  SwitchRanges root(lo, hi);
  tree.push(root);

  float cost = 0;
  do {
    SwitchRanges& r = *tree.adr_at(tree.length()-1);
    if (r._hi != r._lo) {
      if (r._mid == nullptr) {
        float r_cnt = sum_of_cnts(r._lo, r._hi);

        if (r_cnt == 0) {
          tree.pop();
          cost = 0;
          continue;
        }

        SwitchRange* mid = nullptr;
        mid = r._lo;
        for (float cnt = 0; ; ) {
          assert(mid <= r._hi, "out of bounds");
          cnt += mid->cnt();
          if (cnt > r_cnt / 2) {
            break;
          }
          mid++;
        }
        assert(mid <= r._hi, "out of bounds");
        r._mid = mid;
        r._cost = r_cnt / total_cnt;
      }
      r._cost += cost;
      if (r._state < SwitchRanges::LeftDone && r._mid > r._lo) {
        cost = 0;
        r._state = SwitchRanges::LeftDone;
        tree.push(SwitchRanges(r._lo, r._mid-1));
      } else if (r._state < SwitchRanges::RightDone) {
        cost = 0;
        r._state = SwitchRanges::RightDone;
        tree.push(SwitchRanges(r._mid == r._lo ? r._mid+1 : r._mid, r._hi));
      } else {
        tree.pop();
        cost = r._cost;
      }
    } else {
      tree.pop();
      cost = r._cost;
    }
  } while (tree.length() > 0);


  return cost;
}

// It sometimes pays off to test most common ranges before the binary search
void Parse::linear_search_switch_ranges(Node* key_val, SwitchRange*& lo, SwitchRange*& hi) {
  uint nr = hi - lo + 1;
  float total_cnt = sum_of_cnts(lo, hi);

  float min = compute_tree_cost(lo, hi, total_cnt);
  float extra = 1;
  float sub = 0;

  SwitchRange* array1 = lo;
  SwitchRange* array2 = NEW_RESOURCE_ARRAY(SwitchRange, nr);

  SwitchRange* ranges = nullptr;

  while (nr >= 2) {
    assert(lo == array1 || lo == array2, "one the 2 already allocated arrays");
    ranges = (lo == array1) ? array2 : array1;

    // Find highest frequency range
    SwitchRange* candidate = lo;
    for (SwitchRange* sr = lo+1; sr <= hi; sr++) {
      if (sr->cnt() > candidate->cnt()) {
        candidate = sr;
      }
    }
    SwitchRange most_freq = *candidate;
    if (most_freq.cnt() == 0) {
      break;
    }

    // Copy remaining ranges into another array
    int shift = 0;
    for (uint i = 0; i < nr; i++) {
      SwitchRange* sr = &lo[i];
      if (sr != candidate) {
        ranges[i-shift] = *sr;
      } else {
        shift++;
        if (i > 0 && i < nr-1) {
          SwitchRange prev = lo[i-1];
          prev.setRange(prev.lo(), sr->hi(), prev.dest(), prev.cnt());
          if (prev.adjoin(lo[i+1])) {
            shift++;
            i++;
          }
          ranges[i-shift] = prev;
        }
      }
    }
    nr -= shift;

    // Evaluate cost of testing the most common range and performing a
    // binary search on the other ranges
    float cost = extra + compute_tree_cost(&ranges[0], &ranges[nr-1], total_cnt);
    if (cost >= min) {
      break;
    }
    // swap arrays
    lo = &ranges[0];
    hi = &ranges[nr-1];

    // It pays off: emit the test for the most common range
    assert(most_freq.cnt() > 0, "must be taken");
    Node* val = _gvn.transform(new SubINode(key_val, _gvn.intcon(most_freq.lo())));
    Node* cmp = _gvn.transform(new CmpUNode(val, _gvn.intcon(java_subtract(most_freq.hi(), most_freq.lo()))));
    Node* tst = _gvn.transform(new BoolNode(cmp, BoolTest::le));
    IfNode* iff = create_and_map_if(control(), tst, if_prob(most_freq.cnt(), total_cnt), if_cnt(most_freq.cnt()));
    jump_if_true_fork(iff, most_freq.dest(), false);

    sub += most_freq.cnt() / total_cnt;
    extra += 1 - sub;
    min = cost;
  }
}

//----------------------------create_jump_tables-------------------------------
bool Parse::create_jump_tables(Node* key_val, SwitchRange* lo, SwitchRange* hi) {
  // Are jumptables enabled
  if (!UseJumpTables)  return false;

  // Are jumptables supported
  if (!Matcher::has_match_rule(Op_Jump))  return false;

  bool trim_ranges = !C->too_many_traps(method(), bci(), Deoptimization::Reason_unstable_if);

  // Decide if a guard is needed to lop off big ranges at either (or
  // both) end(s) of the input set. We'll call this the default target
  // even though we can't be sure that it is the true "default".

  bool needs_guard = false;
  int default_dest;
  int64_t total_outlier_size = 0;
  int64_t hi_size = ((int64_t)hi->hi()) - ((int64_t)hi->lo()) + 1;
  int64_t lo_size = ((int64_t)lo->hi()) - ((int64_t)lo->lo()) + 1;

  if (lo->dest() == hi->dest()) {
    total_outlier_size = hi_size + lo_size;
    default_dest = lo->dest();
  } else if (lo_size > hi_size) {
    total_outlier_size = lo_size;
    default_dest = lo->dest();
  } else {
    total_outlier_size = hi_size;
    default_dest = hi->dest();
  }

  float total = sum_of_cnts(lo, hi);
  float cost = compute_tree_cost(lo, hi, total);

  // If a guard test will eliminate very sparse end ranges, then
  // it is worth the cost of an extra jump.
  float trimmed_cnt = 0;
  if (total_outlier_size > (MaxJumpTableSparseness * 4)) {
    needs_guard = true;
    if (default_dest == lo->dest()) {
      trimmed_cnt += lo->cnt();
      lo++;
    }
    if (default_dest == hi->dest()) {
      trimmed_cnt += hi->cnt();
      hi--;
    }
  }

  // Find the total number of cases and ranges
  int64_t num_cases = ((int64_t)hi->hi()) - ((int64_t)lo->lo()) + 1;
  int num_range = hi - lo + 1;

  // Don't create table if: too large, too small, or too sparse.
  if (num_cases > MaxJumpTableSize)
    return false;
  if (UseSwitchProfiling) {
    // MinJumpTableSize is set so with a well balanced binary tree,
    // when the number of ranges is MinJumpTableSize, it's cheaper to
    // go through a JumpNode that a tree of IfNodes. Average cost of a
    // tree of IfNodes with MinJumpTableSize is
    // log2f(MinJumpTableSize) comparisons. So if the cost computed
    // from profile data is less than log2f(MinJumpTableSize) then
    // going with the binary search is cheaper.
    if (cost < log2f(MinJumpTableSize)) {
      return false;
    }
  } else {
    if (num_cases < MinJumpTableSize)
      return false;
  }
  if (num_cases > (MaxJumpTableSparseness * num_range))
    return false;

  // Normalize table lookups to zero
  int lowval = lo->lo();
  key_val = _gvn.transform( new SubINode(key_val, _gvn.intcon(lowval)) );

  // Generate a guard to protect against input keyvals that aren't
  // in the switch domain.
  if (needs_guard) {
    Node*   size = _gvn.intcon(num_cases);
    Node*   cmp = _gvn.transform(new CmpUNode(key_val, size));
    Node*   tst = _gvn.transform(new BoolNode(cmp, BoolTest::ge));
    IfNode* iff = create_and_map_if(control(), tst, if_prob(trimmed_cnt, total), if_cnt(trimmed_cnt));
    jump_if_true_fork(iff, default_dest, trim_ranges && trimmed_cnt == 0);

    total -= trimmed_cnt;
  }

  // Create an ideal node JumpTable that has projections
  // of all possible ranges for a switch statement
  // The key_val input must be converted to a pointer offset and scaled.
  // Compare Parse::array_addressing above.

  // Clean the 32-bit int into a real 64-bit offset.
  // Otherwise, the jint value 0 might turn into an offset of 0x0800000000.
  // Make I2L conversion control dependent to prevent it from
  // floating above the range check during loop optimizations.
  // Do not use a narrow int type here to prevent the data path from dying
  // while the control path is not removed. This can happen if the type of key_val
  // is later known to be out of bounds of [0, num_cases] and therefore a narrow cast
  // would be replaced by TOP while C2 is not able to fold the corresponding range checks.
  // Set _carry_dependency for the cast to avoid being removed by IGVN.
#ifdef _LP64
  key_val = C->constrained_convI2L(&_gvn, key_val, TypeInt::INT, control(), true /* carry_dependency */);
#endif

  // Shift the value by wordsize so we have an index into the table, rather
  // than a switch value
  Node *shiftWord = _gvn.MakeConX(wordSize);
  key_val = _gvn.transform( new MulXNode( key_val, shiftWord));

  // Create the JumpNode
  Arena* arena = C->comp_arena();
  float* probs = (float*)arena->Amalloc(sizeof(float)*num_cases);
  int i = 0;
  if (total == 0) {
    for (SwitchRange* r = lo; r <= hi; r++) {
      for (int64_t j = r->lo(); j <= r->hi(); j++, i++) {
        probs[i] = 1.0F / num_cases;
      }
    }
  } else {
    for (SwitchRange* r = lo; r <= hi; r++) {
      float prob = r->cnt()/total;
      for (int64_t j = r->lo(); j <= r->hi(); j++, i++) {
        probs[i] = prob / (r->hi() - r->lo() + 1);
      }
    }
  }

  ciMethodData* methodData = method()->method_data();
  ciMultiBranchData* profile = nullptr;
  if (methodData->is_mature()) {
    ciProfileData* data = methodData->bci_to_data(bci());
    if (data != nullptr && data->is_MultiBranchData()) {
      profile = (ciMultiBranchData*)data;
    }
  }

  Node* jtn = _gvn.transform(new JumpNode(control(), key_val, num_cases, probs, profile == nullptr ? COUNT_UNKNOWN : total));

  // These are the switch destinations hanging off the jumpnode
  i = 0;
  for (SwitchRange* r = lo; r <= hi; r++) {
    for (int64_t j = r->lo(); j <= r->hi(); j++, i++) {
      Node* input = _gvn.transform(new JumpProjNode(jtn, i, r->dest(), (int)(j - lowval)));
      {
        PreserveJVMState pjvms(this);
        set_control(input);
        jump_if_always_fork(r->dest(), trim_ranges && r->cnt() == 0);
      }
    }
  }
  assert(i == num_cases, "miscount of cases");
  stop_and_kill_map();  // no more uses for this JVMS
  return true;
}

//----------------------------jump_switch_ranges-------------------------------
void Parse::jump_switch_ranges(Node* key_val, SwitchRange *lo, SwitchRange *hi, int switch_depth) {
  Block* switch_block = block();
  bool trim_ranges = !C->too_many_traps(method(), bci(), Deoptimization::Reason_unstable_if);

  if (switch_depth == 0) {
    // Do special processing for the top-level call.
    assert(lo->lo() == min_jint, "initial range must exhaust Type::INT");
    assert(hi->hi() == max_jint, "initial range must exhaust Type::INT");

    // Decrement pred-numbers for the unique set of nodes.
#ifdef ASSERT
    if (!trim_ranges) {
      // Ensure that the block's successors are a (duplicate-free) set.
      int successors_counted = 0;  // block occurrences in [hi..lo]
      int unique_successors = switch_block->num_successors();
      for (int i = 0; i < unique_successors; i++) {
        Block* target = switch_block->successor_at(i);

        // Check that the set of successors is the same in both places.
        int successors_found = 0;
        for (SwitchRange* p = lo; p <= hi; p++) {
          if (p->dest() == target->start())  successors_found++;
        }
        assert(successors_found > 0, "successor must be known");
        successors_counted += successors_found;
      }
      assert(successors_counted == (hi-lo)+1, "no unexpected successors");
    }
#endif

    // Maybe prune the inputs, based on the type of key_val.
    jint min_val = min_jint;
    jint max_val = max_jint;
    const TypeInt* ti = key_val->bottom_type()->isa_int();
    if (ti != nullptr) {
      min_val = ti->_lo;
      max_val = ti->_hi;
      assert(min_val <= max_val, "invalid int type");
    }
    while (lo->hi() < min_val) {
      lo++;
    }
    if (lo->lo() < min_val)  {
      lo->setRange(min_val, lo->hi(), lo->dest(), lo->cnt());
    }
    while (hi->lo() > max_val) {
      hi--;
    }
    if (hi->hi() > max_val) {
      hi->setRange(hi->lo(), max_val, hi->dest(), hi->cnt());
    }

    linear_search_switch_ranges(key_val, lo, hi);
  }

#ifndef PRODUCT
  if (switch_depth == 0) {
    _max_switch_depth = 0;
    _est_switch_depth = log2i_graceful((hi - lo + 1) - 1) + 1;
  }
#endif

  assert(lo <= hi, "must be a non-empty set of ranges");
  if (lo == hi) {
    jump_if_always_fork(lo->dest(), trim_ranges && lo->cnt() == 0);
  } else {
    assert(lo->hi() == (lo+1)->lo()-1, "contiguous ranges");
    assert(hi->lo() == (hi-1)->hi()+1, "contiguous ranges");

    if (create_jump_tables(key_val, lo, hi)) return;

    SwitchRange* mid = nullptr;
    float total_cnt = sum_of_cnts(lo, hi);

    int nr = hi - lo + 1;
    if (UseSwitchProfiling) {
      // Don't keep the binary search tree balanced: pick up mid point
      // that split frequencies in half.
      float cnt = 0;
      for (SwitchRange* sr = lo; sr <= hi; sr++) {
        cnt += sr->cnt();
        if (cnt >= total_cnt / 2) {
          mid = sr;
          break;
        }
      }
    } else {
      mid = lo + nr/2;

      // if there is an easy choice, pivot at a singleton:
      if (nr > 3 && !mid->is_singleton() && (mid-1)->is_singleton())  mid--;

      assert(lo < mid && mid <= hi, "good pivot choice");
      assert(nr != 2 || mid == hi,   "should pick higher of 2");
      assert(nr != 3 || mid == hi-1, "should pick middle of 3");
    }


    Node *test_val = _gvn.intcon(mid == lo ? mid->hi() : mid->lo());

    if (mid->is_singleton()) {
      IfNode *iff_ne = jump_if_fork_int(key_val, test_val, BoolTest::ne, 1-if_prob(mid->cnt(), total_cnt), if_cnt(mid->cnt()));
      jump_if_false_fork(iff_ne, mid->dest(), trim_ranges && mid->cnt() == 0);

      // Special Case:  If there are exactly three ranges, and the high
      // and low range each go to the same place, omit the "gt" test,
      // since it will not discriminate anything.
      bool eq_test_only = (hi == lo+2 && hi->dest() == lo->dest() && mid == hi-1) || mid == lo;

      // if there is a higher range, test for it and process it:
      if (mid < hi && !eq_test_only) {
        // two comparisons of same values--should enable 1 test for 2 branches
        // Use BoolTest::lt instead of BoolTest::gt
        float cnt = sum_of_cnts(lo, mid-1);
        IfNode *iff_lt  = jump_if_fork_int(key_val, test_val, BoolTest::lt, if_prob(cnt, total_cnt), if_cnt(cnt));
        Node   *iftrue  = _gvn.transform( new IfTrueNode(iff_lt) );
        Node   *iffalse = _gvn.transform( new IfFalseNode(iff_lt) );
        { PreserveJVMState pjvms(this);
          set_control(iffalse);
          jump_switch_ranges(key_val, mid+1, hi, switch_depth+1);
        }
        set_control(iftrue);
      }

    } else {
      // mid is a range, not a singleton, so treat mid..hi as a unit
      float cnt = sum_of_cnts(mid == lo ? mid+1 : mid, hi);
      IfNode *iff_ge = jump_if_fork_int(key_val, test_val, mid == lo ? BoolTest::gt : BoolTest::ge, if_prob(cnt, total_cnt), if_cnt(cnt));

      // if there is a higher range, test for it and process it:
      if (mid == hi) {
        jump_if_true_fork(iff_ge, mid->dest(), trim_ranges && cnt == 0);
      } else {
        Node *iftrue  = _gvn.transform( new IfTrueNode(iff_ge) );
        Node *iffalse = _gvn.transform( new IfFalseNode(iff_ge) );
        { PreserveJVMState pjvms(this);
          set_control(iftrue);
          jump_switch_ranges(key_val, mid == lo ? mid+1 : mid, hi, switch_depth+1);
        }
        set_control(iffalse);
      }
    }

    // in any case, process the lower range
    if (mid == lo) {
      if (mid->is_singleton()) {
        jump_switch_ranges(key_val, lo+1, hi, switch_depth+1);
      } else {
        jump_if_always_fork(lo->dest(), trim_ranges && lo->cnt() == 0);
      }
    } else {
      jump_switch_ranges(key_val, lo, mid-1, switch_depth+1);
    }
  }

  // Decrease pred_count for each successor after all is done.
  if (switch_depth == 0) {
    int unique_successors = switch_block->num_successors();
    for (int i = 0; i < unique_successors; i++) {
      Block* target = switch_block->successor_at(i);
      // Throw away the pre-allocated path for each unique successor.
      target->next_path_num();
    }
  }

#ifndef PRODUCT
  _max_switch_depth = MAX2(switch_depth, _max_switch_depth);
  if (TraceOptoParse && Verbose && WizardMode && switch_depth == 0) {
    SwitchRange* r;
    int nsing = 0;
    for( r = lo; r <= hi; r++ ) {
      if( r->is_singleton() )  nsing++;
    }
    tty->print(">>> ");
    _method->print_short_name();
    tty->print_cr(" switch decision tree");
    tty->print_cr("    %d ranges (%d singletons), max_depth=%d, est_depth=%d",
                  (int) (hi-lo+1), nsing, _max_switch_depth, _est_switch_depth);
    if (_max_switch_depth > _est_switch_depth) {
      tty->print_cr("******** BAD SWITCH DEPTH ********");
    }
    tty->print("   ");
    for( r = lo; r <= hi; r++ ) {
      r->print();
    }
    tty->cr();
  }
#endif
}

Node* Parse::floating_point_mod(Node* a, Node* b, BasicType type) {
  assert(type == BasicType::T_FLOAT || type == BasicType::T_DOUBLE, "only float and double are floating points");
  CallNode* mod = type == BasicType::T_DOUBLE ? static_cast<CallNode*>(new ModDNode(C, a, b)) : new ModFNode(C, a, b);

  Node* prev_mem = set_predefined_input_for_runtime_call(mod);
  mod = _gvn.transform(mod)->as_Call();
  set_predefined_output_for_runtime_call(mod, prev_mem, TypeRawPtr::BOTTOM);
  Node* result = _gvn.transform(new ProjNode(mod, TypeFunc::Parms + 0));
  record_for_igvn(mod);
  return result;
}

void Parse::l2f() {
  Node* f2 = pop();
  Node* f1 = pop();
  Node* c = make_runtime_call(RC_LEAF, OptoRuntime::l2f_Type(),
                              CAST_FROM_FN_PTR(address, SharedRuntime::l2f),
                              "l2f", nullptr, //no memory effects
                              f1, f2);
  Node* res = _gvn.transform(new ProjNode(c, TypeFunc::Parms + 0));

  push(res);
}

// Handle jsr and jsr_w bytecode
void Parse::do_jsr() {
  assert(bc() == Bytecodes::_jsr || bc() == Bytecodes::_jsr_w, "wrong bytecode");

  // Store information about current state, tagged with new _jsr_bci
  int return_bci = iter().next_bci();
  int jsr_bci    = (bc() == Bytecodes::_jsr) ? iter().get_dest() : iter().get_far_dest();

  // The way we do things now, there is only one successor block
  // for the jsr, because the target code is cloned by ciTypeFlow.
  Block* target = successor_for_bci(jsr_bci);

  // What got pushed?
  const Type* ret_addr = target->peek();
  assert(ret_addr->singleton(), "must be a constant (cloned jsr body)");

  // Effect on jsr on stack
  push(_gvn.makecon(ret_addr));

  // Flow to the jsr.
  merge(jsr_bci);
}

// Handle ret bytecode
void Parse::do_ret() {
  // Find to whom we return.
  assert(block()->num_successors() == 1, "a ret can only go one place now");
  Block* target = block()->successor_at(0);
  assert(!target->is_ready(), "our arrival must be expected");
  int pnum = target->next_path_num();
  merge_common(target, pnum);
}

static bool has_injected_profile(BoolTest::mask btest, Node* test, int& taken, int& not_taken) {
  if (btest != BoolTest::eq && btest != BoolTest::ne) {
    // Only ::eq and ::ne are supported for profile injection.
    return false;
  }
  if (test->is_Cmp() &&
      test->in(1)->Opcode() == Op_ProfileBoolean) {
    ProfileBooleanNode* profile = (ProfileBooleanNode*)test->in(1);
    int false_cnt = profile->false_count();
    int  true_cnt = profile->true_count();

    // Counts matching depends on the actual test operation (::eq or ::ne).
    // No need to scale the counts because profile injection was designed
    // to feed exact counts into VM.
    taken     = (btest == BoolTest::eq) ? false_cnt :  true_cnt;
    not_taken = (btest == BoolTest::eq) ?  true_cnt : false_cnt;

    profile->consume();
    return true;
  }
  return false;
}

// Give up if too few (or too many, in which case the sum will overflow) counts to be meaningful.
// We also check that individual counters are positive first, otherwise the sum can become positive.
// (check for saturation, integer overflow, and immature counts)
static bool counters_are_meaningful(int counter1, int counter2, int min) {
  // check for saturation, including "uint" values too big to fit in "int"
  if (counter1 < 0 || counter2 < 0) {
    return false;
  }
  // check for integer overflow of the sum
  int64_t sum = (int64_t)counter1 + (int64_t)counter2;
  STATIC_ASSERT(sizeof(counter1) < sizeof(sum));
  if (sum > INT_MAX) {
    return false;
  }
  // check if mature
  return (counter1 + counter2) >= min;
}

//--------------------------dynamic_branch_prediction--------------------------
// Try to gather dynamic branch prediction behavior.  Return a probability
// of the branch being taken and set the "cnt" field.  Returns a -1.0
// if we need to use static prediction for some reason.
float Parse::dynamic_branch_prediction(float &cnt, BoolTest::mask btest, Node* test) {
  ResourceMark rm;

  cnt  = COUNT_UNKNOWN;

  int     taken = 0;
  int not_taken = 0;

  bool use_mdo = !has_injected_profile(btest, test, taken, not_taken);

  if (use_mdo) {
    // Use MethodData information if it is available
    // FIXME: free the ProfileData structure
    ciMethodData* methodData = method()->method_data();
    if (!methodData->is_mature())  return PROB_UNKNOWN;
    ciProfileData* data = methodData->bci_to_data(bci());
    if (data == nullptr) {
      return PROB_UNKNOWN;
    }
    if (!data->is_JumpData())  return PROB_UNKNOWN;

    // get taken and not taken values
    // NOTE: saturated UINT_MAX values become negative,
    // as do counts above INT_MAX.
    taken = data->as_JumpData()->taken();
    not_taken = 0;
    if (data->is_BranchData()) {
      not_taken = data->as_BranchData()->not_taken();
    }

    // scale the counts to be commensurate with invocation counts:
    // NOTE: overflow for positive values is clamped at INT_MAX
    taken = method()->scale_count(taken);
    not_taken = method()->scale_count(not_taken);
  }
  // At this point, saturation or overflow is indicated by INT_MAX
  // or a negative value.

  // Give up if too few (or too many, in which case the sum will overflow) counts to be meaningful.
  // We also check that individual counters are positive first, otherwise the sum can become positive.
  if (!counters_are_meaningful(taken, not_taken, 40)) {
    if (C->log() != nullptr) {
      C->log()->elem("branch target_bci='%d' taken='%d' not_taken='%d'", iter().get_dest(), taken, not_taken);
    }
    return PROB_UNKNOWN;
  }

  // Compute frequency that we arrive here
  float sum = taken + not_taken;
  // Adjust, if this block is a cloned private block but the
  // Jump counts are shared.  Taken the private counts for
  // just this path instead of the shared counts.
  if( block()->count() > 0 )
    sum = block()->count();
  cnt = sum / FreqCountInvocations;

  // Pin probability to sane limits
  float prob;
  if( !taken )
    prob = (0+PROB_MIN) / 2;
  else if( !not_taken )
    prob = (1+PROB_MAX) / 2;
  else {                         // Compute probability of true path
    prob = (float)taken / (float)(taken + not_taken);
    if (prob > PROB_MAX)  prob = PROB_MAX;
    if (prob < PROB_MIN)   prob = PROB_MIN;
  }

  assert((cnt > 0.0f) && (prob > 0.0f),
         "Bad frequency assignment in if cnt=%g prob=%g taken=%d not_taken=%d", cnt, prob, taken, not_taken);

  if (C->log() != nullptr) {
    const char* prob_str = nullptr;
    if (prob >= PROB_MAX)  prob_str = (prob == PROB_MAX) ? "max" : "always";
    if (prob <= PROB_MIN)  prob_str = (prob == PROB_MIN) ? "min" : "never";
    char prob_str_buf[30];
    if (prob_str == nullptr) {
      jio_snprintf(prob_str_buf, sizeof(prob_str_buf), "%20.2f", prob);
      prob_str = prob_str_buf;
    }
    C->log()->elem("branch target_bci='%d' taken='%d' not_taken='%d' cnt='%f' prob='%s'",
                   iter().get_dest(), taken, not_taken, cnt, prob_str);
  }
  return prob;
}

//-----------------------------branch_prediction-------------------------------
float Parse::branch_prediction(float& cnt,
                               BoolTest::mask btest,
                               int target_bci,
                               Node* test) {
  float prob = dynamic_branch_prediction(cnt, btest, test);
  // If prob is unknown, switch to static prediction
  if (prob != PROB_UNKNOWN)  return prob;

  prob = PROB_FAIR;                   // Set default value
  if (btest == BoolTest::eq)          // Exactly equal test?
    prob = PROB_STATIC_INFREQUENT;    // Assume its relatively infrequent
  else if (btest == BoolTest::ne)
    prob = PROB_STATIC_FREQUENT;      // Assume its relatively frequent

  // If this is a conditional test guarding a backwards branch,
  // assume its a loop-back edge.  Make it a likely taken branch.
  if (target_bci < bci()) {
    if (is_osr_parse()) {    // Could be a hot OSR'd loop; force deopt
      // Since it's an OSR, we probably have profile data, but since
      // branch_prediction returned PROB_UNKNOWN, the counts are too small.
      // Let's make a special check here for completely zero counts.
      ciMethodData* methodData = method()->method_data();
      if (!methodData->is_empty()) {
        ciProfileData* data = methodData->bci_to_data(bci());
        // Only stop for truly zero counts, which mean an unknown part
        // of the OSR-ed method, and we want to deopt to gather more stats.
        // If you have ANY counts, then this loop is simply 'cold' relative
        // to the OSR loop.
        if (data == nullptr ||
            (data->as_BranchData()->taken() +  data->as_BranchData()->not_taken() == 0)) {
          // This is the only way to return PROB_UNKNOWN:
          return PROB_UNKNOWN;
        }
      }
    }
    prob = PROB_STATIC_FREQUENT;     // Likely to take backwards branch
  }

  assert(prob != PROB_UNKNOWN, "must have some guess at this point");
  return prob;
}

// The magic constants are chosen so as to match the output of
// branch_prediction() when the profile reports a zero taken count.
// It is important to distinguish zero counts unambiguously, because
// some branches (e.g., _213_javac.Assembler.eliminate) validly produce
// very small but nonzero probabilities, which if confused with zero
// counts would keep the program recompiling indefinitely.
bool Parse::seems_never_taken(float prob) const {
  return prob < PROB_MIN;
}

//-------------------------------repush_if_args--------------------------------
// Push arguments of an "if" bytecode back onto the stack by adjusting _sp.
inline int Parse::repush_if_args() {
  if (PrintOpto && WizardMode) {
    tty->print("defending against excessive implicit null exceptions on %s @%d in ",
               Bytecodes::name(iter().cur_bc()), iter().cur_bci());
    method()->print_name(); tty->cr();
  }
  int bc_depth = - Bytecodes::depth(iter().cur_bc());
  assert(bc_depth == 1 || bc_depth == 2, "only two kinds of branches");
  DEBUG_ONLY(sync_jvms());   // argument(n) requires a synced jvms
  assert(argument(0) != nullptr, "must exist");
  assert(bc_depth == 1 || argument(1) != nullptr, "two must exist");
  inc_sp(bc_depth);
  return bc_depth;
}

// Used by StressUnstableIfTraps
static volatile int _trap_stress_counter = 0;

void Parse::increment_trap_stress_counter(Node*& counter, Node*& incr_store) {
  Node* counter_addr = makecon(TypeRawPtr::make((address)&_trap_stress_counter));
  counter = make_load(control(), counter_addr, TypeInt::INT, T_INT, MemNode::unordered);
  counter = _gvn.transform(new AddINode(counter, intcon(1)));
  incr_store = store_to_memory(control(), counter_addr, counter, T_INT, MemNode::unordered);
}

//----------------------------------do_ifnull----------------------------------
void Parse::do_ifnull(BoolTest::mask btest, Node *c) {
  int target_bci = iter().get_dest();

  Node* counter = nullptr;
  Node* incr_store = nullptr;
  bool do_stress_trap = StressUnstableIfTraps && ((C->random() % 2) == 0);
  if (do_stress_trap) {
    increment_trap_stress_counter(counter, incr_store);
  }

  Block* branch_block = successor_for_bci(target_bci);
  Block* next_block   = successor_for_bci(iter().next_bci());

  float cnt;
  float prob = branch_prediction(cnt, btest, target_bci, c);
  if (prob == PROB_UNKNOWN) {
    // (An earlier version of do_ifnull omitted this trap for OSR methods.)
    if (PrintOpto && Verbose) {
      tty->print_cr("Never-taken edge stops compilation at bci %d", bci());
    }
    repush_if_args(); // to gather stats on loop
    uncommon_trap(Deoptimization::Reason_unreached,
                  Deoptimization::Action_reinterpret,
                  nullptr, "cold");
    if (C->eliminate_boxing()) {
      // Mark the successor blocks as parsed
      branch_block->next_path_num();
      next_block->next_path_num();
    }
    return;
  }

  NOT_PRODUCT(explicit_null_checks_inserted++);

  // Generate real control flow
  Node   *tst = _gvn.transform( new BoolNode( c, btest ) );

  // Sanity check the probability value
  assert(prob > 0.0f,"Bad probability in Parser");
 // Need xform to put node in hash table
  IfNode *iff = create_and_xform_if( control(), tst, prob, cnt );
  assert(iff->_prob > 0.0f,"Optimizer made bad probability in parser");
  // True branch
  { PreserveJVMState pjvms(this);
    Node* iftrue  = _gvn.transform( new IfTrueNode (iff) );
    set_control(iftrue);

    if (stopped()) {            // Path is dead?
      NOT_PRODUCT(explicit_null_checks_elided++);
      if (C->eliminate_boxing()) {
        // Mark the successor block as parsed
        branch_block->next_path_num();
      }
    } else {                    // Path is live.
      adjust_map_after_if(btest, c, prob, branch_block);
      if (!stopped()) {
        merge(target_bci);
      }
    }
  }

  // False branch
  Node* iffalse = _gvn.transform( new IfFalseNode(iff) );
  set_control(iffalse);

  if (stopped()) {              // Path is dead?
    NOT_PRODUCT(explicit_null_checks_elided++);
    if (C->eliminate_boxing()) {
      // Mark the successor block as parsed
      next_block->next_path_num();
    }
  } else  {                     // Path is live.
    adjust_map_after_if(BoolTest(btest).negate(), c, 1.0-prob, next_block);
  }

  if (do_stress_trap) {
    stress_trap(iff, counter, incr_store);
  }
}

//------------------------------------do_if------------------------------------
void Parse::do_if(BoolTest::mask btest, Node* c, bool can_trap, bool new_path, Node** ctrl_taken) {
  int target_bci = iter().get_dest();

  Block* branch_block = successor_for_bci(target_bci);
  Block* next_block   = successor_for_bci(iter().next_bci());

  float cnt;
  float prob = branch_prediction(cnt, btest, target_bci, c);
  float untaken_prob = 1.0 - prob;

  if (prob == PROB_UNKNOWN) {
    if (PrintOpto && Verbose) {
      tty->print_cr("Never-taken edge stops compilation at bci %d", bci());
    }
    repush_if_args(); // to gather stats on loop
    uncommon_trap(Deoptimization::Reason_unreached,
                  Deoptimization::Action_reinterpret,
                  nullptr, "cold");
    if (C->eliminate_boxing()) {
      // Mark the successor blocks as parsed
      branch_block->next_path_num();
      next_block->next_path_num();
    }
    return;
  }

  Node* counter = nullptr;
  Node* incr_store = nullptr;
  bool do_stress_trap = StressUnstableIfTraps && ((C->random() % 2) == 0);
  if (do_stress_trap) {
    increment_trap_stress_counter(counter, incr_store);
  }

  // Sanity check the probability value
  assert(0.0f < prob && prob < 1.0f,"Bad probability in Parser");

  bool taken_if_true = true;
  // Convert BoolTest to canonical form:
  if (!BoolTest(btest).is_canonical()) {
    btest         = BoolTest(btest).negate();
    taken_if_true = false;
    // prob is NOT updated here; it remains the probability of the taken
    // path (as opposed to the prob of the path guarded by an 'IfTrueNode').
  }
  assert(btest != BoolTest::eq, "!= is the only canonical exact test");

  Node* tst0 = new BoolNode(c, btest);
  Node* tst = _gvn.transform(tst0);
  BoolTest::mask taken_btest   = BoolTest::illegal;
  BoolTest::mask untaken_btest = BoolTest::illegal;

  if (tst->is_Bool()) {
    // Refresh c from the transformed bool node, since it may be
    // simpler than the original c.  Also re-canonicalize btest.
    // This wins when (Bool ne (Conv2B p) 0) => (Bool ne (CmpP p null)).
    // That can arise from statements like: if (x instanceof C) ...
    if (tst != tst0) {
      // Canonicalize one more time since transform can change it.
      btest = tst->as_Bool()->_test._test;
      if (!BoolTest(btest).is_canonical()) {
        // Reverse edges one more time...
        tst   = _gvn.transform( tst->as_Bool()->negate(&_gvn) );
        btest = tst->as_Bool()->_test._test;
        assert(BoolTest(btest).is_canonical(), "sanity");
        taken_if_true = !taken_if_true;
      }
      c = tst->in(1);
    }
    BoolTest::mask neg_btest = BoolTest(btest).negate();
    taken_btest   = taken_if_true ?     btest : neg_btest;
    untaken_btest = taken_if_true ? neg_btest :     btest;
  }

  // Generate real control flow
  float true_prob = (taken_if_true ? prob : untaken_prob);
  IfNode* iff = create_and_map_if(control(), tst, true_prob, cnt);
  assert(iff->_prob > 0.0f,"Optimizer made bad probability in parser");
  Node* taken_branch   = new IfTrueNode(iff);
  Node* untaken_branch = new IfFalseNode(iff);
  if (!taken_if_true) {  // Finish conversion to canonical form
    Node* tmp      = taken_branch;
    taken_branch   = untaken_branch;
    untaken_branch = tmp;
  }

  // Branch is taken:
  { PreserveJVMState pjvms(this);
    taken_branch = _gvn.transform(taken_branch);
    set_control(taken_branch);

    if (stopped()) {
      if (C->eliminate_boxing() && !new_path) {
        // Mark the successor block as parsed (if we haven't created a new path)
        branch_block->next_path_num();
      }
    } else {
      adjust_map_after_if(taken_btest, c, prob, branch_block, can_trap);
      if (!stopped()) {
        if (new_path) {
          // Merge by using a new path
          merge_new_path(target_bci);
        } else if (ctrl_taken != nullptr) {
          // Don't merge but save taken branch to be wired by caller
          *ctrl_taken = control();
        } else {
          merge(target_bci);
        }
      }
    }
  }

  untaken_branch = _gvn.transform(untaken_branch);
  set_control(untaken_branch);

  // Branch not taken.
  if (stopped() && ctrl_taken == nullptr) {
    if (C->eliminate_boxing()) {
      // Mark the successor block as parsed (if caller does not re-wire control flow)
      next_block->next_path_num();
    }
  } else {
    adjust_map_after_if(untaken_btest, c, untaken_prob, next_block, can_trap);
  }

  if (do_stress_trap) {
    stress_trap(iff, counter, incr_store);
  }
}


static ProfilePtrKind speculative_ptr_kind(const TypeOopPtr* t) {
  if (t->speculative() == nullptr) {
    return ProfileUnknownNull;
  }
  if (t->speculative_always_null()) {
    return ProfileAlwaysNull;
  }
  if (t->speculative_maybe_null()) {
    return ProfileMaybeNull;
  }
  return ProfileNeverNull;
}

void Parse::acmp_always_null_input(Node* input, const TypeOopPtr* tinput, BoolTest::mask btest, Node* eq_region) {
  inc_sp(2);
  Node* cast = null_check_common(input, T_OBJECT, true, nullptr,
                                 !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_null_check) &&
                                 speculative_ptr_kind(tinput) == ProfileAlwaysNull);
  dec_sp(2);
  if (btest == BoolTest::ne) {
    {
      PreserveJVMState pjvms(this);
      replace_in_map(input, cast);
      int target_bci = iter().get_dest();
      merge(target_bci);
    }
    record_for_igvn(eq_region);
    set_control(_gvn.transform(eq_region));
  } else {
    replace_in_map(input, cast);
  }
}

Node* Parse::acmp_null_check(Node* input, const TypeOopPtr* tinput, ProfilePtrKind input_ptr, Node*& null_ctl) {
  inc_sp(2);
  null_ctl = top();
  Node* cast = null_check_oop(input, &null_ctl,
                              input_ptr == ProfileNeverNull || (input_ptr == ProfileUnknownNull && !too_many_traps_or_recompiles(Deoptimization::Reason_null_check)),
                              false,
                              speculative_ptr_kind(tinput) == ProfileNeverNull &&
                              !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_null_check));
  dec_sp(2);
  assert(!stopped(), "null input should have been caught earlier");
  return cast;
}

void Parse::acmp_known_non_inline_type_input(Node* input, const TypeOopPtr* tinput, ProfilePtrKind input_ptr, ciKlass* input_type, BoolTest::mask btest, Node* eq_region) {
  Node* ne_region = new RegionNode(1);
  Node* null_ctl;
  Node* cast = acmp_null_check(input, tinput, input_ptr, null_ctl);
  ne_region->add_req(null_ctl);

  Node* slow_ctl = type_check_receiver(cast, input_type, 1.0, &cast);
  {
    PreserveJVMState pjvms(this);
    inc_sp(2);
    set_control(slow_ctl);
    Deoptimization::DeoptReason reason;
    if (tinput->speculative_type() != nullptr && !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_class_check)) {
      reason = Deoptimization::Reason_speculate_class_check;
    } else {
      reason = Deoptimization::Reason_class_check;
    }
    uncommon_trap_exact(reason, Deoptimization::Action_maybe_recompile);
  }
  ne_region->add_req(control());

  record_for_igvn(ne_region);
  set_control(_gvn.transform(ne_region));
  if (btest == BoolTest::ne) {
    {
      PreserveJVMState pjvms(this);
      if (null_ctl == top()) {
        replace_in_map(input, cast);
      }
      int target_bci = iter().get_dest();
      merge(target_bci);
    }
    record_for_igvn(eq_region);
    set_control(_gvn.transform(eq_region));
  } else {
    if (null_ctl == top()) {
      replace_in_map(input, cast);
    }
    set_control(_gvn.transform(ne_region));
  }
}

void Parse::acmp_unknown_non_inline_type_input(Node* input, const TypeOopPtr* tinput, ProfilePtrKind input_ptr, BoolTest::mask btest, Node* eq_region) {
  Node* ne_region = new RegionNode(1);
  Node* null_ctl;
  Node* cast = acmp_null_check(input, tinput, input_ptr, null_ctl);
  ne_region->add_req(null_ctl);

  {
    BuildCutout unless(this, inline_type_test(cast, /* is_inline = */ false), PROB_MAX);
    inc_sp(2);
    uncommon_trap_exact(Deoptimization::Reason_class_check, Deoptimization::Action_maybe_recompile);
  }

  ne_region->add_req(control());

  record_for_igvn(ne_region);
  set_control(_gvn.transform(ne_region));
  if (btest == BoolTest::ne) {
    {
      PreserveJVMState pjvms(this);
      if (null_ctl == top()) {
        replace_in_map(input, cast);
      }
      int target_bci = iter().get_dest();
      merge(target_bci);
    }
    record_for_igvn(eq_region);
    set_control(_gvn.transform(eq_region));
  } else {
    if (null_ctl == top()) {
      replace_in_map(input, cast);
    }
    set_control(_gvn.transform(ne_region));
  }
}

void Parse::do_acmp(BoolTest::mask btest, Node* left, Node* right) {
  ciKlass* left_type = nullptr;
  ciKlass* right_type = nullptr;
  ProfilePtrKind left_ptr = ProfileUnknownNull;
  ProfilePtrKind right_ptr = ProfileUnknownNull;
  bool left_inline_type = true;
  bool right_inline_type = true;

  // Leverage profiling at acmp
  if (UseACmpProfile) {
    method()->acmp_profiled_type(bci(), left_type, right_type, left_ptr, right_ptr, left_inline_type, right_inline_type);
    if (too_many_traps_or_recompiles(Deoptimization::Reason_class_check)) {
      left_type = nullptr;
      right_type = nullptr;
      left_inline_type = true;
      right_inline_type = true;
    }
    if (too_many_traps_or_recompiles(Deoptimization::Reason_null_check)) {
      left_ptr = ProfileUnknownNull;
      right_ptr = ProfileUnknownNull;
    }
  }

  if (UseTypeSpeculation) {
    record_profile_for_speculation(left, left_type, left_ptr);
    record_profile_for_speculation(right, right_type, right_ptr);
  }

  if (!EnableValhalla) {
    Node* cmp = CmpP(left, right);
    cmp = optimize_cmp_with_klass(cmp);
    do_if(btest, cmp);
    return;
  }

  // Check for equality before potentially allocating
  if (left == right) {
    do_if(btest, makecon(TypeInt::CC_EQ));
    return;
  }

  // Allocate inline type operands and re-execute on deoptimization
  if (left->is_InlineType()) {
    if (_gvn.type(right)->is_zero_type() ||
        (right->is_InlineType() && _gvn.type(right->as_InlineType()->get_null_marker())->is_zero_type())) {
      // Null checking a scalarized but nullable inline type. Check the null marker
      // input instead of the oop input to avoid keeping buffer allocations alive.
      Node* cmp = CmpI(left->as_InlineType()->get_null_marker(), intcon(0));
      do_if(btest, cmp);
      return;
    } else {
      PreserveReexecuteState preexecs(this);
      inc_sp(2);
      jvms()->set_should_reexecute(true);
      left = left->as_InlineType()->buffer(this)->get_oop();
    }
  }
  if (right->is_InlineType()) {
    PreserveReexecuteState preexecs(this);
    inc_sp(2);
    jvms()->set_should_reexecute(true);
    right = right->as_InlineType()->buffer(this)->get_oop();
  }

  // First, do a normal pointer comparison
  const TypeOopPtr* tleft = _gvn.type(left)->isa_oopptr();
  const TypeOopPtr* tright = _gvn.type(right)->isa_oopptr();
  Node* cmp = CmpP(left, right);
  cmp = optimize_cmp_with_klass(cmp);
  if (tleft == nullptr || !tleft->can_be_inline_type() ||
      tright == nullptr || !tright->can_be_inline_type()) {
    // This is sufficient, if one of the operands can't be an inline type
    do_if(btest, cmp);
    return;
  }

  // Don't add traps to unstable if branches because additional checks are required to
  // decide if the operands are equal/substitutable and we therefore shouldn't prune
  // branches for one if based on the profiling of the acmp branches.
  // Also, OptimizeUnstableIf would set an incorrect re-rexecution state because it
  // assumes that there is a 1-1 mapping between the if and the acmp branches and that
  // hitting a trap means that we will take the corresponding acmp branch on re-execution.
  const bool can_trap = true;

  Node* eq_region = nullptr;
  if (btest == BoolTest::eq) {
    do_if(btest, cmp, !can_trap, true);
    if (stopped()) {
      // Pointers are equal, operands must be equal
      return;
    }
  } else {
    assert(btest == BoolTest::ne, "only eq or ne");
    Node* is_not_equal = nullptr;
    eq_region = new RegionNode(3);
    {
      PreserveJVMState pjvms(this);
      // Pointers are not equal, but more checks are needed to determine if the operands are (not) substitutable
      do_if(btest, cmp, !can_trap, false, &is_not_equal);
      if (!stopped()) {
        eq_region->init_req(1, control());
      }
    }
    if (is_not_equal == nullptr || is_not_equal->is_top()) {
      record_for_igvn(eq_region);
      set_control(_gvn.transform(eq_region));
      return;
    }
    set_control(is_not_equal);
  }

  // Prefer speculative types if available
  if (!too_many_traps_or_recompiles(Deoptimization::Reason_speculate_class_check)) {
    if (tleft->speculative_type() != nullptr) {
      left_type = tleft->speculative_type();
    }
    if (tright->speculative_type() != nullptr) {
      right_type = tright->speculative_type();
    }
  }

  if (speculative_ptr_kind(tleft) != ProfileMaybeNull && speculative_ptr_kind(tleft) != ProfileUnknownNull) {
    ProfilePtrKind speculative_left_ptr = speculative_ptr_kind(tleft);
    if (speculative_left_ptr == ProfileAlwaysNull && !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_null_assert)) {
      left_ptr = speculative_left_ptr;
    } else if (speculative_left_ptr == ProfileNeverNull && !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_null_check)) {
      left_ptr = speculative_left_ptr;
    }
  }
  if (speculative_ptr_kind(tright) != ProfileMaybeNull && speculative_ptr_kind(tright) != ProfileUnknownNull) {
    ProfilePtrKind speculative_right_ptr = speculative_ptr_kind(tright);
    if (speculative_right_ptr == ProfileAlwaysNull && !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_null_assert)) {
      right_ptr = speculative_right_ptr;
    } else if (speculative_right_ptr == ProfileNeverNull && !too_many_traps_or_recompiles(Deoptimization::Reason_speculate_null_check)) {
      right_ptr = speculative_right_ptr;
    }
  }

  if (left_ptr == ProfileAlwaysNull) {
    // Comparison with null. Assert the input is indeed null and we're done.
    acmp_always_null_input(left, tleft, btest, eq_region);
    return;
  }
  if (right_ptr == ProfileAlwaysNull) {
    // Comparison with null. Assert the input is indeed null and we're done.
    acmp_always_null_input(right, tright, btest, eq_region);
    return;
  }
  if (left_type != nullptr && !left_type->is_inlinetype()) {
    // Comparison with an object of known type
    acmp_known_non_inline_type_input(left, tleft, left_ptr, left_type, btest, eq_region);
    return;
  }
  if (right_type != nullptr && !right_type->is_inlinetype()) {
    // Comparison with an object of known type
    acmp_known_non_inline_type_input(right, tright, right_ptr, right_type, btest, eq_region);
    return;
  }
  if (!left_inline_type) {
    // Comparison with an object known not to be an inline type
    acmp_unknown_non_inline_type_input(left, tleft, left_ptr, btest, eq_region);
    return;
  }
  if (!right_inline_type) {
    // Comparison with an object known not to be an inline type
    acmp_unknown_non_inline_type_input(right, tright, right_ptr, btest, eq_region);
    return;
  }

  // Pointers are not equal, check if first operand is non-null
  Node* ne_region = new RegionNode(6);
  Node* null_ctl;
  Node* not_null_right = acmp_null_check(right, tright, right_ptr, null_ctl);
  ne_region->init_req(1, null_ctl);

  // First operand is non-null, check if it is an inline type
  Node* is_value = inline_type_test(not_null_right);
  IfNode* is_value_iff = create_and_map_if(control(), is_value, PROB_FAIR, COUNT_UNKNOWN);
  Node* not_value = _gvn.transform(new IfFalseNode(is_value_iff));
  ne_region->init_req(2, not_value);
  set_control(_gvn.transform(new IfTrueNode(is_value_iff)));

  // The first operand is an inline type, check if the second operand is non-null
  Node* not_null_left = acmp_null_check(left, tleft, left_ptr, null_ctl);
  ne_region->init_req(3, null_ctl);

  // Check if both operands are of the same class.
  Node* kls_left = load_object_klass(not_null_left);
  Node* kls_right = load_object_klass(not_null_right);
  Node* kls_cmp = CmpP(kls_left, kls_right);
  Node* kls_bol = _gvn.transform(new BoolNode(kls_cmp, BoolTest::ne));
  IfNode* kls_iff = create_and_map_if(control(), kls_bol, PROB_FAIR, COUNT_UNKNOWN);
  Node* kls_ne = _gvn.transform(new IfTrueNode(kls_iff));
  set_control(_gvn.transform(new IfFalseNode(kls_iff)));
  ne_region->init_req(4, kls_ne);

  if (stopped()) {
    record_for_igvn(ne_region);
    set_control(_gvn.transform(ne_region));
    if (btest == BoolTest::ne) {
      {
        PreserveJVMState pjvms(this);
        int target_bci = iter().get_dest();
        merge(target_bci);
      }
      record_for_igvn(eq_region);
      set_control(_gvn.transform(eq_region));
    }
    return;
  }

  // Both operands are values types of the same class, we need to perform a
  // substitutability test. Delegate to ValueObjectMethods::isSubstitutable().
  Node* ne_io_phi = PhiNode::make(ne_region, i_o());
  Node* mem = reset_memory();
  Node* ne_mem_phi = PhiNode::make(ne_region, mem);

  Node* eq_io_phi = nullptr;
  Node* eq_mem_phi = nullptr;
  if (eq_region != nullptr) {
    eq_io_phi = PhiNode::make(eq_region, i_o());
    eq_mem_phi = PhiNode::make(eq_region, mem);
  }

  set_all_memory(mem);

  kill_dead_locals();
  ciMethod* subst_method = ciEnv::current()->ValueObjectMethods_klass()->find_method(ciSymbols::isSubstitutable_name(), ciSymbols::object_object_boolean_signature());
  CallStaticJavaNode *call = new CallStaticJavaNode(C, TypeFunc::make(subst_method), SharedRuntime::get_resolve_static_call_stub(), subst_method);
  call->set_override_symbolic_info(true);
  call->init_req(TypeFunc::Parms, not_null_left);
  call->init_req(TypeFunc::Parms+1, not_null_right);
  inc_sp(2);
  set_edges_for_java_call(call, false, false);
  Node* ret = set_results_for_java_call(call, false, true);
  dec_sp(2);

  // Test the return value of ValueObjectMethods::isSubstitutable()
  // This is the last check, do_if can emit traps now.
  Node* subst_cmp = _gvn.transform(new CmpINode(ret, intcon(1)));
  Node* ctl = C->top();
  if (btest == BoolTest::eq) {
    PreserveJVMState pjvms(this);
    do_if(btest, subst_cmp, can_trap);
    if (!stopped()) {
      ctl = control();
    }
  } else {
    assert(btest == BoolTest::ne, "only eq or ne");
    PreserveJVMState pjvms(this);
    do_if(btest, subst_cmp, can_trap, false, &ctl);
    if (!stopped()) {
      eq_region->init_req(2, control());
      eq_io_phi->init_req(2, i_o());
      eq_mem_phi->init_req(2, reset_memory());
    }
  }
  ne_region->init_req(5, ctl);
  ne_io_phi->init_req(5, i_o());
  ne_mem_phi->init_req(5, reset_memory());

  record_for_igvn(ne_region);
  set_control(_gvn.transform(ne_region));
  set_i_o(_gvn.transform(ne_io_phi));
  set_all_memory(_gvn.transform(ne_mem_phi));

  if (btest == BoolTest::ne) {
    {
      PreserveJVMState pjvms(this);
      int target_bci = iter().get_dest();
      merge(target_bci);
    }

    record_for_igvn(eq_region);
    set_control(_gvn.transform(eq_region));
    set_i_o(_gvn.transform(eq_io_phi));
    set_all_memory(_gvn.transform(eq_mem_phi));
  }
}

// Force unstable if traps to be taken randomly to trigger intermittent bugs such as incorrect debug information.
// Add another if before the unstable if that checks a "random" condition at runtime (a simple shared counter) and
// then either takes the trap or executes the original, unstable if.
void Parse::stress_trap(IfNode* orig_iff, Node* counter, Node* incr_store) {
  // Search for an unstable if trap
  CallStaticJavaNode* trap = nullptr;
  assert(orig_iff->Opcode() == Op_If && orig_iff->outcnt() == 2, "malformed if");
  ProjNode* trap_proj = orig_iff->uncommon_trap_proj(trap, Deoptimization::Reason_unstable_if);
  if (trap == nullptr || !trap->jvms()->should_reexecute()) {
    // No suitable trap found. Remove unused counter load and increment.
    C->gvn_replace_by(incr_store, incr_store->in(MemNode::Memory));
    return;
  }

  // Remove trap from optimization list since we add another path to the trap.
  bool success = C->remove_unstable_if_trap(trap, true);
  assert(success, "Trap already modified");

  // Add a check before the original if that will trap with a certain frequency and execute the original if otherwise
  int freq_log = (C->random() % 31) + 1; // Random logarithmic frequency in [1, 31]
  Node* mask = intcon(right_n_bits(freq_log));
  counter = _gvn.transform(new AndINode(counter, mask));
  Node* cmp = _gvn.transform(new CmpINode(counter, intcon(0)));
  Node* bol = _gvn.transform(new BoolNode(cmp, BoolTest::mask::eq));
  IfNode* iff = _gvn.transform(new IfNode(orig_iff->in(0), bol, orig_iff->_prob, orig_iff->_fcnt))->as_If();
  Node* if_true = _gvn.transform(new IfTrueNode(iff));
  Node* if_false = _gvn.transform(new IfFalseNode(iff));
  assert(!if_true->is_top() && !if_false->is_top(), "trap always / never taken");

  // Trap
  assert(trap_proj->outcnt() == 1, "some other nodes are dependent on the trap projection");

  Node* trap_region = new RegionNode(3);
  trap_region->set_req(1, trap_proj);
  trap_region->set_req(2, if_true);
  trap->set_req(0, _gvn.transform(trap_region));

  // Don't trap, execute original if
  orig_iff->set_req(0, if_false);
}

bool Parse::path_is_suitable_for_uncommon_trap(float prob) const {
  // Randomly skip emitting an uncommon trap
  if (StressUnstableIfTraps && ((C->random() % 2) == 0)) {
    return false;
  }
  // Don't want to speculate on uncommon traps when running with -Xcomp
  if (!UseInterpreter) {
    return false;
  }
  return seems_never_taken(prob) &&
         !C->too_many_traps(method(), bci(), Deoptimization::Reason_unstable_if);
}

void Parse::maybe_add_predicate_after_if(Block* path) {
  if (path->is_SEL_head() && path->preds_parsed() == 0) {
    // Add predicates at bci of if dominating the loop so traps can be
    // recorded on the if's profile data
    int bc_depth = repush_if_args();
    add_parse_predicates();
    dec_sp(bc_depth);
    path->set_has_predicates();
  }
}


//----------------------------adjust_map_after_if------------------------------
// Adjust the JVM state to reflect the result of taking this path.
// Basically, it means inspecting the CmpNode controlling this
// branch, seeing how it constrains a tested value, and then
// deciding if it's worth our while to encode this constraint
// as graph nodes in the current abstract interpretation map.
void Parse::adjust_map_after_if(BoolTest::mask btest, Node* c, float prob, Block* path, bool can_trap) {
  if (!c->is_Cmp()) {
    maybe_add_predicate_after_if(path);
    return;
  }

  if (stopped() || btest == BoolTest::illegal) {
    return;                             // nothing to do
  }

  bool is_fallthrough = (path == successor_for_bci(iter().next_bci()));

  if (can_trap && path_is_suitable_for_uncommon_trap(prob)) {
    repush_if_args();
    Node* call = uncommon_trap(Deoptimization::Reason_unstable_if,
                  Deoptimization::Action_reinterpret,
                  nullptr,
                  (is_fallthrough ? "taken always" : "taken never"));

    if (call != nullptr) {
      C->record_unstable_if_trap(new UnstableIfTrap(call->as_CallStaticJava(), path));
    }
    return;
  }

  Node* val = c->in(1);
  Node* con = c->in(2);
  const Type* tcon = _gvn.type(con);
  const Type* tval = _gvn.type(val);
  bool have_con = tcon->singleton();
  if (tval->singleton()) {
    if (!have_con) {
      // Swap, so constant is in con.
      con  = val;
      tcon = tval;
      val  = c->in(2);
      tval = _gvn.type(val);
      btest = BoolTest(btest).commute();
      have_con = true;
    } else {
      // Do we have two constants?  Then leave well enough alone.
      have_con = false;
    }
  }
  if (!have_con) {                        // remaining adjustments need a con
    maybe_add_predicate_after_if(path);
    return;
  }

  sharpen_type_after_if(btest, con, tcon, val, tval);
  maybe_add_predicate_after_if(path);
}


static Node* extract_obj_from_klass_load(PhaseGVN* gvn, Node* n) {
  Node* ldk;
  if (n->is_DecodeNKlass()) {
    if (n->in(1)->Opcode() != Op_LoadNKlass) {
      return nullptr;
    } else {
      ldk = n->in(1);
    }
  } else if (n->Opcode() != Op_LoadKlass) {
    return nullptr;
  } else {
    ldk = n;
  }
  assert(ldk != nullptr && ldk->is_Load(), "should have found a LoadKlass or LoadNKlass node");

  Node* adr = ldk->in(MemNode::Address);
  intptr_t off = 0;
  Node* obj = AddPNode::Ideal_base_and_offset(adr, gvn, off);
  if (obj == nullptr || off != oopDesc::klass_offset_in_bytes()) // loading oopDesc::_klass?
    return nullptr;
  const TypePtr* tp = gvn->type(obj)->is_ptr();
  if (tp == nullptr || !(tp->isa_instptr() || tp->isa_aryptr())) // is obj a Java object ptr?
    return nullptr;

  return obj;
}

void Parse::sharpen_type_after_if(BoolTest::mask btest,
                                  Node* con, const Type* tcon,
                                  Node* val, const Type* tval) {
  // Look for opportunities to sharpen the type of a node
  // whose klass is compared with a constant klass.
  if (btest == BoolTest::eq && tcon->isa_klassptr()) {
    Node* obj = extract_obj_from_klass_load(&_gvn, val);
    const TypeOopPtr* con_type = tcon->isa_klassptr()->as_instance_type();
    if (obj != nullptr && (con_type->isa_instptr() || con_type->isa_aryptr())) {
       // Found:
       //   Bool(CmpP(LoadKlass(obj._klass), ConP(Foo.klass)), [eq])
       // or the narrowOop equivalent.
       const Type* obj_type = _gvn.type(obj);
       const TypeOopPtr* tboth = obj_type->join_speculative(con_type)->isa_oopptr();
       if (tboth != nullptr && tboth->klass_is_exact() && tboth != obj_type &&
           tboth->higher_equal(obj_type)) {
          // obj has to be of the exact type Foo if the CmpP succeeds.
          int obj_in_map = map()->find_edge(obj);
          JVMState* jvms = this->jvms();
          if (obj_in_map >= 0 &&
              (jvms->is_loc(obj_in_map) || jvms->is_stk(obj_in_map))) {
            TypeNode* ccast = new CheckCastPPNode(control(), obj, tboth);
            const Type* tcc = ccast->as_Type()->type();
            assert(tcc != obj_type && tcc->higher_equal(obj_type), "must improve");
            // Delay transform() call to allow recovery of pre-cast value
            // at the control merge.
            _gvn.set_type_bottom(ccast);
            record_for_igvn(ccast);
            if (tboth->is_inlinetypeptr()) {
              ccast = InlineTypeNode::make_from_oop(this, ccast, tboth->exact_klass(true)->as_inline_klass());
            }
            // Here's the payoff.
            replace_in_map(obj, ccast);
          }
       }
    }
  }

  int val_in_map = map()->find_edge(val);
  if (val_in_map < 0)  return;          // replace_in_map would be useless
  {
    JVMState* jvms = this->jvms();
    if (!(jvms->is_loc(val_in_map) ||
          jvms->is_stk(val_in_map)))
      return;                           // again, it would be useless
  }

  // Check for a comparison to a constant, and "know" that the compared
  // value is constrained on this path.
  assert(tcon->singleton(), "");
  ConstraintCastNode* ccast = nullptr;
  Node* cast = nullptr;

  switch (btest) {
  case BoolTest::eq:                    // Constant test?
    {
      const Type* tboth = tcon->join_speculative(tval);
      if (tboth == tval)  break;        // Nothing to gain.
      if (tcon->isa_int()) {
        ccast = new CastIINode(control(), val, tboth);
      } else if (tcon == TypePtr::NULL_PTR) {
        // Cast to null, but keep the pointer identity temporarily live.
        ccast = new CastPPNode(control(), val, tboth);
      } else {
        const TypeF* tf = tcon->isa_float_constant();
        const TypeD* td = tcon->isa_double_constant();
        // Exclude tests vs float/double 0 as these could be
        // either +0 or -0.  Just because you are equal to +0
        // doesn't mean you ARE +0!
        // Note, following code also replaces Long and Oop values.
        if ((!tf || tf->_f != 0.0) &&
            (!td || td->_d != 0.0))
          cast = con;                   // Replace non-constant val by con.
      }
    }
    break;

  case BoolTest::ne:
    if (tcon == TypePtr::NULL_PTR) {
      cast = cast_not_null(val, false);
    }
    break;

  default:
    // (At this point we could record int range types with CastII.)
    break;
  }

  if (ccast != nullptr) {
    const Type* tcc = ccast->as_Type()->type();
    assert(tcc != tval && tcc->higher_equal(tval), "must improve");
    // Delay transform() call to allow recovery of pre-cast value
    // at the control merge.
    _gvn.set_type_bottom(ccast);
    record_for_igvn(ccast);
    cast = ccast;
  }

  if (cast != nullptr) {                   // Here's the payoff.
    replace_in_map(val, cast);
  }
}

/**
 * Use speculative type to optimize CmpP node: if comparison is
 * against the low level class, cast the object to the speculative
 * type if any. CmpP should then go away.
 *
 * @param c  expected CmpP node
 * @return   result of CmpP on object casted to speculative type
 *
 */
Node* Parse::optimize_cmp_with_klass(Node* c) {
  // If this is transformed by the _gvn to a comparison with the low
  // level klass then we may be able to use speculation
  if (c->Opcode() == Op_CmpP &&
      (c->in(1)->Opcode() == Op_LoadKlass || c->in(1)->Opcode() == Op_DecodeNKlass) &&
      c->in(2)->is_Con()) {
    Node* load_klass = nullptr;
    Node* decode = nullptr;
    if (c->in(1)->Opcode() == Op_DecodeNKlass) {
      decode = c->in(1);
      load_klass = c->in(1)->in(1);
    } else {
      load_klass = c->in(1);
    }
    if (load_klass->in(2)->is_AddP()) {
      Node* addp = load_klass->in(2);
      Node* obj = addp->in(AddPNode::Address);
      const TypeOopPtr* obj_type = _gvn.type(obj)->is_oopptr();
      if (obj_type->speculative_type_not_null() != nullptr) {
        ciKlass* k = obj_type->speculative_type();
        inc_sp(2);
        obj = maybe_cast_profiled_obj(obj, k);
        dec_sp(2);
        if (obj->is_InlineType()) {
          assert(obj->as_InlineType()->is_allocated(&_gvn), "must be allocated");
          obj = obj->as_InlineType()->get_oop();
        }
        // Make the CmpP use the casted obj
        addp = basic_plus_adr(obj, addp->in(AddPNode::Offset));
        load_klass = load_klass->clone();
        load_klass->set_req(2, addp);
        load_klass = _gvn.transform(load_klass);
        if (decode != nullptr) {
          decode = decode->clone();
          decode->set_req(1, load_klass);
          load_klass = _gvn.transform(decode);
        }
        c = c->clone();
        c->set_req(1, load_klass);
        c = _gvn.transform(c);
      }
    }
  }
  return c;
}

//------------------------------do_one_bytecode--------------------------------
// Parse this bytecode, and alter the Parsers JVM->Node mapping
void Parse::do_one_bytecode() {
  Node *a, *b, *c, *d;          // Handy temps
  BoolTest::mask btest;
  int i;

  assert(!has_exceptions(), "bytecode entry state must be clear of throws");

  if (C->check_node_count(NodeLimitFudgeFactor * 5,
                          "out of nodes parsing method")) {
    return;
  }

#ifdef ASSERT
  // for setting breakpoints
  if (TraceOptoParse) {
    tty->print(" @");
    dump_bci(bci());
    tty->print(" %s", Bytecodes::name(bc()));
    tty->cr();
  }
#endif

  switch (bc()) {
  case Bytecodes::_nop:
    // do nothing
    break;
  case Bytecodes::_lconst_0:
    push_pair(longcon(0));
    break;

  case Bytecodes::_lconst_1:
    push_pair(longcon(1));
    break;

  case Bytecodes::_fconst_0:
    push(zerocon(T_FLOAT));
    break;

  case Bytecodes::_fconst_1:
    push(makecon(TypeF::ONE));
    break;

  case Bytecodes::_fconst_2:
    push(makecon(TypeF::make(2.0f)));
    break;

  case Bytecodes::_dconst_0:
    push_pair(zerocon(T_DOUBLE));
    break;

  case Bytecodes::_dconst_1:
    push_pair(makecon(TypeD::ONE));
    break;

  case Bytecodes::_iconst_m1:push(intcon(-1)); break;
  case Bytecodes::_iconst_0: push(intcon( 0)); break;
  case Bytecodes::_iconst_1: push(intcon( 1)); break;
  case Bytecodes::_iconst_2: push(intcon( 2)); break;
  case Bytecodes::_iconst_3: push(intcon( 3)); break;
  case Bytecodes::_iconst_4: push(intcon( 4)); break;
  case Bytecodes::_iconst_5: push(intcon( 5)); break;
  case Bytecodes::_bipush:   push(intcon(iter().get_constant_u1())); break;
  case Bytecodes::_sipush:   push(intcon(iter().get_constant_u2())); break;
  case Bytecodes::_aconst_null: push(null());  break;

  case Bytecodes::_ldc:
  case Bytecodes::_ldc_w:
  case Bytecodes::_ldc2_w: {
    // ciTypeFlow should trap if the ldc is in error state or if the constant is not loaded
    assert(!iter().is_in_error(), "ldc is in error state");
    ciConstant constant = iter().get_constant();
    assert(constant.is_loaded(), "constant is not loaded");
    const Type* con_type = Type::make_from_constant(constant);
    if (con_type != nullptr) {
      push_node(con_type->basic_type(), makecon(con_type));
    }
    break;
  }

  case Bytecodes::_aload_0:
    push( local(0) );
    break;
  case Bytecodes::_aload_1:
    push( local(1) );
    break;
  case Bytecodes::_aload_2:
    push( local(2) );
    break;
  case Bytecodes::_aload_3:
    push( local(3) );
    break;
  case Bytecodes::_aload:
    push( local(iter().get_index()) );
    break;

  case Bytecodes::_fload_0:
  case Bytecodes::_iload_0:
    push( local(0) );
    break;
  case Bytecodes::_fload_1:
  case Bytecodes::_iload_1:
    push( local(1) );
    break;
  case Bytecodes::_fload_2:
  case Bytecodes::_iload_2:
    push( local(2) );
    break;
  case Bytecodes::_fload_3:
  case Bytecodes::_iload_3:
    push( local(3) );
    break;
  case Bytecodes::_fload:
  case Bytecodes::_iload:
    push( local(iter().get_index()) );
    break;
  case Bytecodes::_lload_0:
    push_pair_local( 0 );
    break;
  case Bytecodes::_lload_1:
    push_pair_local( 1 );
    break;
  case Bytecodes::_lload_2:
    push_pair_local( 2 );
    break;
  case Bytecodes::_lload_3:
    push_pair_local( 3 );
    break;
  case Bytecodes::_lload:
    push_pair_local( iter().get_index() );
    break;

  case Bytecodes::_dload_0:
    push_pair_local(0);
    break;
  case Bytecodes::_dload_1:
    push_pair_local(1);
    break;
  case Bytecodes::_dload_2:
    push_pair_local(2);
    break;
  case Bytecodes::_dload_3:
    push_pair_local(3);
    break;
  case Bytecodes::_dload:
    push_pair_local(iter().get_index());
    break;
  case Bytecodes::_fstore_0:
  case Bytecodes::_istore_0:
  case Bytecodes::_astore_0:
    set_local( 0, pop() );
    break;
  case Bytecodes::_fstore_1:
  case Bytecodes::_istore_1:
  case Bytecodes::_astore_1:
    set_local( 1, pop() );
    break;
  case Bytecodes::_fstore_2:
  case Bytecodes::_istore_2:
  case Bytecodes::_astore_2:
    set_local( 2, pop() );
    break;
  case Bytecodes::_fstore_3:
  case Bytecodes::_istore_3:
  case Bytecodes::_astore_3:
    set_local( 3, pop() );
    break;
  case Bytecodes::_fstore:
  case Bytecodes::_istore:
  case Bytecodes::_astore:
    set_local( iter().get_index(), pop() );
    break;
  // long stores
  case Bytecodes::_lstore_0:
    set_pair_local( 0, pop_pair() );
    break;
  case Bytecodes::_lstore_1:
    set_pair_local( 1, pop_pair() );
    break;
  case Bytecodes::_lstore_2:
    set_pair_local( 2, pop_pair() );
    break;
  case Bytecodes::_lstore_3:
    set_pair_local( 3, pop_pair() );
    break;
  case Bytecodes::_lstore:
    set_pair_local( iter().get_index(), pop_pair() );
    break;

  // double stores
  case Bytecodes::_dstore_0:
    set_pair_local( 0, pop_pair() );
    break;
  case Bytecodes::_dstore_1:
    set_pair_local( 1, pop_pair() );
    break;
  case Bytecodes::_dstore_2:
    set_pair_local( 2, pop_pair() );
    break;
  case Bytecodes::_dstore_3:
    set_pair_local( 3, pop_pair() );
    break;
  case Bytecodes::_dstore:
    set_pair_local( iter().get_index(), pop_pair() );
    break;

  case Bytecodes::_pop:  dec_sp(1);   break;
  case Bytecodes::_pop2: dec_sp(2);   break;
  case Bytecodes::_swap:
    a = pop();
    b = pop();
    push(a);
    push(b);
    break;
  case Bytecodes::_dup:
    a = pop();
    push(a);
    push(a);
    break;
  case Bytecodes::_dup_x1:
    a = pop();
    b = pop();
    push( a );
    push( b );
    push( a );
    break;
  case Bytecodes::_dup_x2:
    a = pop();
    b = pop();
    c = pop();
    push( a );
    push( c );
    push( b );
    push( a );
    break;
  case Bytecodes::_dup2:
    a = pop();
    b = pop();
    push( b );
    push( a );
    push( b );
    push( a );
    break;

  case Bytecodes::_dup2_x1:
    // before: .. c, b, a
    // after:  .. b, a, c, b, a
    // not tested
    a = pop();
    b = pop();
    c = pop();
    push( b );
    push( a );
    push( c );
    push( b );
    push( a );
    break;
  case Bytecodes::_dup2_x2:
    // before: .. d, c, b, a
    // after:  .. b, a, d, c, b, a
    // not tested
    a = pop();
    b = pop();
    c = pop();
    d = pop();
    push( b );
    push( a );
    push( d );
    push( c );
    push( b );
    push( a );
    break;

  case Bytecodes::_arraylength: {
    // Must do null-check with value on expression stack
    Node *ary = null_check(peek(), T_ARRAY);
    // Compile-time detect of null-exception?
    if (stopped())  return;
    a = pop();
    push(load_array_length(a));
    break;
  }

  case Bytecodes::_baload:  array_load(T_BYTE);    break;
  case Bytecodes::_caload:  array_load(T_CHAR);    break;
  case Bytecodes::_iaload:  array_load(T_INT);     break;
  case Bytecodes::_saload:  array_load(T_SHORT);   break;
  case Bytecodes::_faload:  array_load(T_FLOAT);   break;
  case Bytecodes::_aaload:  array_load(T_OBJECT);  break;
  case Bytecodes::_laload:  array_load(T_LONG);    break;
  case Bytecodes::_daload:  array_load(T_DOUBLE);  break;
  case Bytecodes::_bastore: array_store(T_BYTE);   break;
  case Bytecodes::_castore: array_store(T_CHAR);   break;
  case Bytecodes::_iastore: array_store(T_INT);    break;
  case Bytecodes::_sastore: array_store(T_SHORT);  break;
  case Bytecodes::_fastore: array_store(T_FLOAT);  break;
  case Bytecodes::_aastore: array_store(T_OBJECT); break;
  case Bytecodes::_lastore: array_store(T_LONG);   break;
  case Bytecodes::_dastore: array_store(T_DOUBLE); break;

  case Bytecodes::_getfield:
    do_getfield();
    break;

  case Bytecodes::_getstatic:
    do_getstatic();
    break;

  case Bytecodes::_putfield:
    do_putfield();
    break;

  case Bytecodes::_putstatic:
    do_putstatic();
    break;

  case Bytecodes::_irem:
    // Must keep both values on the expression-stack during null-check
    zero_check_int(peek());
    // Compile-time detect of null-exception?
    if (stopped())  return;
    b = pop();
    a = pop();
    push(_gvn.transform(new ModINode(control(), a, b)));
    break;
  case Bytecodes::_idiv:
    // Must keep both values on the expression-stack during null-check
    zero_check_int(peek());
    // Compile-time detect of null-exception?
    if (stopped())  return;
    b = pop();
    a = pop();
    push( _gvn.transform( new DivINode(control(),a,b) ) );
    break;
  case Bytecodes::_imul:
    b = pop(); a = pop();
    push( _gvn.transform( new MulINode(a,b) ) );
    break;
  case Bytecodes::_iadd:
    b = pop(); a = pop();
    push( _gvn.transform( new AddINode(a,b) ) );
    break;
  case Bytecodes::_ineg:
    a = pop();
    push( _gvn.transform( new SubINode(_gvn.intcon(0),a)) );
    break;
  case Bytecodes::_isub:
    b = pop(); a = pop();
    push( _gvn.transform( new SubINode(a,b) ) );
    break;
  case Bytecodes::_iand:
    b = pop(); a = pop();
    push( _gvn.transform( new AndINode(a,b) ) );
    break;
  case Bytecodes::_ior:
    b = pop(); a = pop();
    push( _gvn.transform( new OrINode(a,b) ) );
    break;
  case Bytecodes::_ixor:
    b = pop(); a = pop();
    push( _gvn.transform( new XorINode(a,b) ) );
    break;
  case Bytecodes::_ishl:
    b = pop(); a = pop();
    push( _gvn.transform( new LShiftINode(a,b) ) );
    break;
  case Bytecodes::_ishr:
    b = pop(); a = pop();
    push( _gvn.transform( new RShiftINode(a,b) ) );
    break;
  case Bytecodes::_iushr:
    b = pop(); a = pop();
    push( _gvn.transform( new URShiftINode(a,b) ) );
    break;

  case Bytecodes::_fneg:
    a = pop();
    b = _gvn.transform(new NegFNode (a));
    push(b);
    break;

  case Bytecodes::_fsub:
    b = pop();
    a = pop();
    c = _gvn.transform( new SubFNode(a,b) );
    push(c);
    break;

  case Bytecodes::_fadd:
    b = pop();
    a = pop();
    c = _gvn.transform( new AddFNode(a,b) );
    push(c);
    break;

  case Bytecodes::_fmul:
    b = pop();
    a = pop();
    c = _gvn.transform( new MulFNode(a,b) );
    push(c);
    break;

  case Bytecodes::_fdiv:
    b = pop();
    a = pop();
    c = _gvn.transform( new DivFNode(nullptr,a,b) );
    push(c);
    break;

  case Bytecodes::_frem:
    // Generate a ModF node.
    b = pop();
    a = pop();
    push(floating_point_mod(a, b, BasicType::T_FLOAT));
    break;

  case Bytecodes::_fcmpl:
    b = pop();
    a = pop();
    c = _gvn.transform( new CmpF3Node( a, b));
    push(c);
    break;
  case Bytecodes::_fcmpg:
    b = pop();
    a = pop();

    // Same as fcmpl but need to flip the unordered case.  Swap the inputs,
    // which negates the result sign except for unordered.  Flip the unordered
    // as well by using CmpF3 which implements unordered-lesser instead of
    // unordered-greater semantics.  Finally, commute the result bits.  Result
    // is same as using a CmpF3Greater except we did it with CmpF3 alone.
    c = _gvn.transform( new CmpF3Node( b, a));
    c = _gvn.transform( new SubINode(_gvn.intcon(0),c) );
    push(c);
    break;

  case Bytecodes::_f2i:
    a = pop();
    push(_gvn.transform(new ConvF2INode(a)));
    break;

  case Bytecodes::_d2i:
    a = pop_pair();
    b = _gvn.transform(new ConvD2INode(a));
    push( b );
    break;

  case Bytecodes::_f2d:
    a = pop();
    b = _gvn.transform( new ConvF2DNode(a));
    push_pair( b );
    break;

  case Bytecodes::_d2f:
    a = pop_pair();
    b = _gvn.transform( new ConvD2FNode(a));
    push( b );
    break;

  case Bytecodes::_l2f:
    if (Matcher::convL2FSupported()) {
      a = pop_pair();
      b = _gvn.transform( new ConvL2FNode(a));
      push(b);
    } else {
      l2f();
    }
    break;

  case Bytecodes::_l2d:
    a = pop_pair();
    b = _gvn.transform( new ConvL2DNode(a));
    push_pair(b);
    break;

  case Bytecodes::_f2l:
    a = pop();
    b = _gvn.transform( new ConvF2LNode(a));
    push_pair(b);
    break;

  case Bytecodes::_d2l:
    a = pop_pair();
    b = _gvn.transform( new ConvD2LNode(a));
    push_pair(b);
    break;

  case Bytecodes::_dsub:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new SubDNode(a,b) );
    push_pair(c);
    break;

  case Bytecodes::_dadd:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new AddDNode(a,b) );
    push_pair(c);
    break;

  case Bytecodes::_dmul:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new MulDNode(a,b) );
    push_pair(c);
    break;

  case Bytecodes::_ddiv:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new DivDNode(nullptr,a,b) );
    push_pair(c);
    break;

  case Bytecodes::_dneg:
    a = pop_pair();
    b = _gvn.transform(new NegDNode (a));
    push_pair(b);
    break;

  case Bytecodes::_drem:
    // Generate a ModD node.
    b = pop_pair();
    a = pop_pair();
    push_pair(floating_point_mod(a, b, BasicType::T_DOUBLE));
    break;

  case Bytecodes::_dcmpl:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new CmpD3Node( a, b));
    push(c);
    break;

  case Bytecodes::_dcmpg:
    b = pop_pair();
    a = pop_pair();
    // Same as dcmpl but need to flip the unordered case.
    // Commute the inputs, which negates the result sign except for unordered.
    // Flip the unordered as well by using CmpD3 which implements
    // unordered-lesser instead of unordered-greater semantics.
    // Finally, negate the result bits.  Result is same as using a
    // CmpD3Greater except we did it with CmpD3 alone.
    c = _gvn.transform( new CmpD3Node( b, a));
    c = _gvn.transform( new SubINode(_gvn.intcon(0),c) );
    push(c);
    break;


    // Note for longs -> lo word is on TOS, hi word is on TOS - 1
  case Bytecodes::_land:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new AndLNode(a,b) );
    push_pair(c);
    break;
  case Bytecodes::_lor:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new OrLNode(a,b) );
    push_pair(c);
    break;
  case Bytecodes::_lxor:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new XorLNode(a,b) );
    push_pair(c);
    break;

  case Bytecodes::_lshl:
    b = pop();                  // the shift count
    a = pop_pair();             // value to be shifted
    c = _gvn.transform( new LShiftLNode(a,b) );
    push_pair(c);
    break;
  case Bytecodes::_lshr:
    b = pop();                  // the shift count
    a = pop_pair();             // value to be shifted
    c = _gvn.transform( new RShiftLNode(a,b) );
    push_pair(c);
    break;
  case Bytecodes::_lushr:
    b = pop();                  // the shift count
    a = pop_pair();             // value to be shifted
    c = _gvn.transform( new URShiftLNode(a,b) );
    push_pair(c);
    break;
  case Bytecodes::_lmul:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new MulLNode(a,b) );
    push_pair(c);
    break;

  case Bytecodes::_lrem:
    // Must keep both values on the expression-stack during null-check
    assert(peek(0) == top(), "long word order");
    zero_check_long(peek(1));
    // Compile-time detect of null-exception?
    if (stopped())  return;
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new ModLNode(control(),a,b) );
    push_pair(c);
    break;

  case Bytecodes::_ldiv:
    // Must keep both values on the expression-stack during null-check
    assert(peek(0) == top(), "long word order");
    zero_check_long(peek(1));
    // Compile-time detect of null-exception?
    if (stopped())  return;
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new DivLNode(control(),a,b) );
    push_pair(c);
    break;

  case Bytecodes::_ladd:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new AddLNode(a,b) );
    push_pair(c);
    break;
  case Bytecodes::_lsub:
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new SubLNode(a,b) );
    push_pair(c);
    break;
  case Bytecodes::_lcmp:
    // Safepoints are now inserted _before_ branches.  The long-compare
    // bytecode painfully produces a 3-way value (-1,0,+1) which requires a
    // slew of control flow.  These are usually followed by a CmpI vs zero and
    // a branch; this pattern then optimizes to the obvious long-compare and
    // branch.  However, if the branch is backwards there's a Safepoint
    // inserted.  The inserted Safepoint captures the JVM state at the
    // pre-branch point, i.e. it captures the 3-way value.  Thus if a
    // long-compare is used to control a loop the debug info will force
    // computation of the 3-way value, even though the generated code uses a
    // long-compare and branch.  We try to rectify the situation by inserting
    // a SafePoint here and have it dominate and kill the safepoint added at a
    // following backwards branch.  At this point the JVM state merely holds 2
    // longs but not the 3-way value.
    switch (iter().next_bc()) {
      case Bytecodes::_ifgt:
      case Bytecodes::_iflt:
      case Bytecodes::_ifge:
      case Bytecodes::_ifle:
      case Bytecodes::_ifne:
      case Bytecodes::_ifeq:
        // If this is a backwards branch in the bytecodes, add Safepoint
        maybe_add_safepoint(iter().next_get_dest());
      default:
        break;
    }
    b = pop_pair();
    a = pop_pair();
    c = _gvn.transform( new CmpL3Node( a, b ));
    push(c);
    break;

  case Bytecodes::_lneg:
    a = pop_pair();
    b = _gvn.transform( new SubLNode(longcon(0),a));
    push_pair(b);
    break;
  case Bytecodes::_l2i:
    a = pop_pair();
    push( _gvn.transform( new ConvL2INode(a)));
    break;
  case Bytecodes::_i2l:
    a = pop();
    b = _gvn.transform( new ConvI2LNode(a));
    push_pair(b);
    break;
  case Bytecodes::_i2b:
    // Sign extend
    a = pop();
    a = Compile::narrow_value(T_BYTE, a, nullptr, &_gvn, true);
    push(a);
    break;
  case Bytecodes::_i2s:
    a = pop();
    a = Compile::narrow_value(T_SHORT, a, nullptr, &_gvn, true);
    push(a);
    break;
  case Bytecodes::_i2c:
    a = pop();
    a = Compile::narrow_value(T_CHAR, a, nullptr, &_gvn, true);
    push(a);
    break;

  case Bytecodes::_i2f:
    a = pop();
    b = _gvn.transform( new ConvI2FNode(a) ) ;
    push(b);
    break;

  case Bytecodes::_i2d:
    a = pop();
    b = _gvn.transform( new ConvI2DNode(a));
    push_pair(b);
    break;

  case Bytecodes::_iinc:        // Increment local
    i = iter().get_index();     // Get local index
    set_local( i, _gvn.transform( new AddINode( _gvn.intcon(iter().get_iinc_con()), local(i) ) ) );
    break;

  // Exit points of synchronized methods must have an unlock node
  case Bytecodes::_return:
    return_current(nullptr);
    break;

  case Bytecodes::_ireturn:
  case Bytecodes::_areturn:
  case Bytecodes::_freturn:
    return_current(cast_to_non_larval(pop()));
    break;
  case Bytecodes::_lreturn:
  case Bytecodes::_dreturn:
    return_current(pop_pair());
    break;

  case Bytecodes::_athrow:
    // null exception oop throws null pointer exception
    null_check(peek());
    if (stopped())  return;
    // Hook the thrown exception directly to subsequent handlers.
    if (BailoutToInterpreterForThrows) {
      // Keep method interpreted from now on.
      uncommon_trap(Deoptimization::Reason_unhandled,
                    Deoptimization::Action_make_not_compilable);
      return;
    }
    if (env()->jvmti_can_post_on_exceptions()) {
      // check if we must post exception events, take uncommon trap if so (with must_throw = false)
      uncommon_trap_if_should_post_on_exceptions(Deoptimization::Reason_unhandled, false);
    }
    // Here if either can_post_on_exceptions or should_post_on_exceptions is false
    add_exception_state(make_exception_state(peek()));
    break;

  case Bytecodes::_goto:   // fall through
  case Bytecodes::_goto_w: {
    int target_bci = (bc() == Bytecodes::_goto) ? iter().get_dest() : iter().get_far_dest();

    // If this is a backwards branch in the bytecodes, add Safepoint
    maybe_add_safepoint(target_bci);

    // Merge the current control into the target basic block
    merge(target_bci);

    // See if we can get some profile data and hand it off to the next block
    Block *target_block = block()->successor_for_bci(target_bci);
    if (target_block->pred_count() != 1)  break;
    ciMethodData* methodData = method()->method_data();
    if (!methodData->is_mature())  break;
    ciProfileData* data = methodData->bci_to_data(bci());
    assert(data != nullptr && data->is_JumpData(), "need JumpData for taken branch");
    int taken = ((ciJumpData*)data)->taken();
    taken = method()->scale_count(taken);
    target_block->set_count(taken);
    break;
  }

  case Bytecodes::_ifnull:    btest = BoolTest::eq; goto handle_if_null;
  case Bytecodes::_ifnonnull: btest = BoolTest::ne; goto handle_if_null;
  handle_if_null:
    // If this is a backwards branch in the bytecodes, add Safepoint
    maybe_add_safepoint(iter().get_dest());
    a = null();
    b = cast_to_non_larval(pop());
    if (b->is_InlineType()) {
      // Null checking a scalarized but nullable inline type. Check the null marker
      // input instead of the oop input to avoid keeping buffer allocations alive
      c = _gvn.transform(new CmpINode(b->as_InlineType()->get_null_marker(), zerocon(T_INT)));
    } else {
      if (!_gvn.type(b)->speculative_maybe_null() &&
          !too_many_traps(Deoptimization::Reason_speculate_null_check)) {
        inc_sp(1);
        Node* null_ctl = top();
        b = null_check_oop(b, &null_ctl, true, true, true);
        assert(null_ctl->is_top(), "no null control here");
        dec_sp(1);
      } else if (_gvn.type(b)->speculative_always_null() &&
                 !too_many_traps(Deoptimization::Reason_speculate_null_assert)) {
        inc_sp(1);
        b = null_assert(b);
        dec_sp(1);
      }
      c = _gvn.transform( new CmpPNode(b, a) );
    }
    do_ifnull(btest, c);
    break;

  case Bytecodes::_if_acmpeq: btest = BoolTest::eq; goto handle_if_acmp;
  case Bytecodes::_if_acmpne: btest = BoolTest::ne; goto handle_if_acmp;
  handle_if_acmp:
    // If this is a backwards branch in the bytecodes, add Safepoint
    maybe_add_safepoint(iter().get_dest());
    a = cast_to_non_larval(pop());
    b = cast_to_non_larval(pop());
    do_acmp(btest, b, a);
    break;

  case Bytecodes::_ifeq: btest = BoolTest::eq; goto handle_ifxx;
  case Bytecodes::_ifne: btest = BoolTest::ne; goto handle_ifxx;
  case Bytecodes::_iflt: btest = BoolTest::lt; goto handle_ifxx;
  case Bytecodes::_ifle: btest = BoolTest::le; goto handle_ifxx;
  case Bytecodes::_ifgt: btest = BoolTest::gt; goto handle_ifxx;
  case Bytecodes::_ifge: btest = BoolTest::ge; goto handle_ifxx;
  handle_ifxx:
    // If this is a backwards branch in the bytecodes, add Safepoint
    maybe_add_safepoint(iter().get_dest());
    a = _gvn.intcon(0);
    b = pop();
    c = _gvn.transform( new CmpINode(b, a) );
    do_if(btest, c);
    break;

  case Bytecodes::_if_icmpeq: btest = BoolTest::eq; goto handle_if_icmp;
  case Bytecodes::_if_icmpne: btest = BoolTest::ne; goto handle_if_icmp;
  case Bytecodes::_if_icmplt: btest = BoolTest::lt; goto handle_if_icmp;
  case Bytecodes::_if_icmple: btest = BoolTest::le; goto handle_if_icmp;
  case Bytecodes::_if_icmpgt: btest = BoolTest::gt; goto handle_if_icmp;
  case Bytecodes::_if_icmpge: btest = BoolTest::ge; goto handle_if_icmp;
  handle_if_icmp:
    // If this is a backwards branch in the bytecodes, add Safepoint
    maybe_add_safepoint(iter().get_dest());
    a = pop();
    b = pop();
    c = _gvn.transform( new CmpINode( b, a ) );
    do_if(btest, c);
    break;

  case Bytecodes::_tableswitch:
    do_tableswitch();
    break;

  case Bytecodes::_lookupswitch:
    do_lookupswitch();
    break;

  case Bytecodes::_invokestatic:
  case Bytecodes::_invokedynamic:
  case Bytecodes::_invokespecial:
  case Bytecodes::_invokevirtual:
  case Bytecodes::_invokeinterface:
    do_call();
    break;
  case Bytecodes::_checkcast:
    do_checkcast();
    break;
  case Bytecodes::_instanceof:
    do_instanceof();
    break;
  case Bytecodes::_anewarray:
    do_newarray();
    break;
  case Bytecodes::_newarray:
    do_newarray((BasicType)iter().get_index());
    break;
  case Bytecodes::_multianewarray:
    do_multianewarray();
    break;
  case Bytecodes::_new:
    do_new();
    break;

  case Bytecodes::_jsr:
  case Bytecodes::_jsr_w:
    do_jsr();
    break;

  case Bytecodes::_ret:
    do_ret();
    break;


  case Bytecodes::_monitorenter:
    do_monitor_enter();
    break;

  case Bytecodes::_monitorexit:
    do_monitor_exit();
    break;

  case Bytecodes::_breakpoint:
    // Breakpoint set concurrently to compile
    // %%% use an uncommon trap?
    C->record_failure("breakpoint in method");
    return;

  default:
#ifndef PRODUCT
    map()->dump(99);
#endif
    tty->print("\nUnhandled bytecode %s\n", Bytecodes::name(bc()) );
    ShouldNotReachHere();
  }

#ifndef PRODUCT
  if (failing()) { return; }
  constexpr int perBytecode = 6;
  if (C->should_print_igv(perBytecode)) {
    IdealGraphPrinter* printer = C->igv_printer();
    char buffer[256];
    jio_snprintf(buffer, sizeof(buffer), "Bytecode %d: %s, map: %d", bci(), Bytecodes::name(bc()), map() == nullptr ? -1 : map()->_idx);
    bool old = printer->traverse_outs();
    printer->set_traverse_outs(true);
    printer->print_graph(buffer);
    printer->set_traverse_outs(old);
  }
#endif
}
