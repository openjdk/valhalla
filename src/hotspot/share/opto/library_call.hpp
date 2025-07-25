/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "ci/ciMethod.hpp"
#include "classfile/javaClasses.hpp"
#include "opto/callGenerator.hpp"
#include "opto/graphKit.hpp"
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/inlinetypenode.hpp"
#include "opto/intrinsicnode.hpp"
#include "opto/movenode.hpp"

class LibraryIntrinsic : public InlineCallGenerator {
  // Extend the set of intrinsics known to the runtime:
 public:
 private:
  bool             _is_virtual;
  bool             _does_virtual_dispatch;
  int8_t           _predicates_count;  // Intrinsic is predicated by several conditions
  int8_t           _last_predicate; // Last generated predicate
  vmIntrinsics::ID _intrinsic_id;

 public:
  LibraryIntrinsic(ciMethod* m, bool is_virtual, int predicates_count, bool does_virtual_dispatch, vmIntrinsics::ID id)
    : InlineCallGenerator(m),
      _is_virtual(is_virtual),
      _does_virtual_dispatch(does_virtual_dispatch),
      _predicates_count((int8_t)predicates_count),
      _last_predicate((int8_t)-1),
      _intrinsic_id(id)
  {
  }
  virtual bool is_intrinsic() const { return true; }
  virtual bool is_virtual()   const { return _is_virtual; }
  virtual bool is_predicated() const { return _predicates_count > 0; }
  virtual int  predicates_count() const { return _predicates_count; }
  virtual bool does_virtual_dispatch()   const { return _does_virtual_dispatch; }
  virtual JVMState* generate(JVMState* jvms);
  virtual Node* generate_predicate(JVMState* jvms, int predicate);
  vmIntrinsics::ID intrinsic_id() const { return _intrinsic_id; }
};


// Local helper class for LibraryIntrinsic:
class LibraryCallKit : public GraphKit {
 private:
  LibraryIntrinsic* _intrinsic;     // the library intrinsic being called
  Node*             _result;        // the result node, if any
  int               _reexecute_sp;  // the stack pointer when bytecode needs to be reexecuted

  const TypeOopPtr* sharpen_unsafe_type(Compile::AliasType* alias_type, const TypePtr *adr_type);

 public:
  LibraryCallKit(JVMState* jvms, LibraryIntrinsic* intrinsic)
    : GraphKit(jvms),
      _intrinsic(intrinsic),
      _result(nullptr)
  {
    // Check if this is a root compile.  In that case we don't have a caller.
    if (!jvms->has_method()) {
      _reexecute_sp = sp();
    } else {
      // Find out how many arguments the interpreter needs when deoptimizing
      // and save the stack pointer value so it can used by uncommon_trap.
      // We find the argument count by looking at the declared signature.
      bool ignored_will_link;
      ciSignature* declared_signature = nullptr;
      ciMethod* ignored_callee = caller()->get_method_at_bci(bci(), ignored_will_link, &declared_signature);
      const int nargs = declared_signature->arg_size_for_bc(caller()->java_code_at_bci(bci()));
      _reexecute_sp = sp() + nargs;  // "push" arguments back on stack
    }
  }

  virtual LibraryCallKit* is_LibraryCallKit() const { return (LibraryCallKit*)this; }

  ciMethod*         caller()    const    { return jvms()->method(); }
  int               bci()       const    { return jvms()->bci(); }
  LibraryIntrinsic* intrinsic() const    { return _intrinsic; }
  vmIntrinsics::ID  intrinsic_id() const { return _intrinsic->intrinsic_id(); }
  ciMethod*         callee()    const    { return _intrinsic->method(); }

  bool  try_to_inline(int predicate);
  Node* try_to_predicate(int predicate);

  void push_result() {
    // Push the result onto the stack.
    Node* res = result();
    if (!stopped() && res != nullptr) {
      if (res->is_top()) {
        assert(false, "Can't determine return value.");
        C->record_method_not_compilable("Can't determine return value.");
      }
      BasicType bt = res->bottom_type()->basic_type();
      if (C->inlining_incrementally() && res->is_InlineType()) {
        // The caller expects an oop when incrementally inlining an intrinsic that returns an
        // inline type. Make sure the call is re-executed if the allocation triggers a deoptimization.
        PreserveReexecuteState preexecs(this);
        jvms()->set_should_reexecute(true);
        res = res->as_InlineType()->buffer(this);
      }
      push_node(bt, res);
    }
  }

 private:
  void fatal_unexpected_iid(vmIntrinsics::ID iid) {
    fatal("unexpected intrinsic %d: %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
  }

  void  set_result(Node* n) { assert(_result == nullptr, "only set once"); _result = n; }
  void  set_result(RegionNode* region, PhiNode* value);
  Node*     result() { return _result; }

  virtual int reexecute_sp() { return _reexecute_sp; }

  // Helper functions to inline natives
  Node* generate_guard(Node* test, RegionNode* region, float true_prob);
  Node* generate_slow_guard(Node* test, RegionNode* region);
  Node* generate_fair_guard(Node* test, RegionNode* region);
  Node* generate_negative_guard(Node* index, RegionNode* region,
                                // resulting CastII of index:
                                Node* *pos_index = nullptr);
  Node* generate_limit_guard(Node* offset, Node* subseq_length,
                             Node* array_length,
                             RegionNode* region);
  void  generate_string_range_check(Node* array, Node* offset,
                                    Node* length, bool char_count);
  Node* current_thread_helper(Node* &tls_output, ByteSize handle_offset,
                              bool is_immutable);
  Node* generate_current_thread(Node* &tls_output);
  Node* generate_virtual_thread(Node* threadObj);
  Node* load_klass_from_mirror_common(Node* mirror, bool never_see_null,
                                      RegionNode* region, int null_path,
                                      int offset);
  Node* load_klass_from_mirror(Node* mirror, bool never_see_null,
                               RegionNode* region, int null_path) {
    int offset = java_lang_Class::klass_offset();
    return load_klass_from_mirror_common(mirror, never_see_null,
                                         region, null_path,
                                         offset);
  }
  Node* load_array_klass_from_mirror(Node* mirror, bool never_see_null,
                                     RegionNode* region, int null_path) {
    int offset = java_lang_Class::array_klass_offset();
    return load_klass_from_mirror_common(mirror, never_see_null,
                                         region, null_path,
                                         offset);
  }
  Node* generate_klass_flags_guard(Node* kls, int modifier_mask, int modifier_bits, RegionNode* region,
                                   ByteSize offset, const Type* type, BasicType bt);
  Node* generate_misc_flags_guard(Node* kls,
                                  int modifier_mask, int modifier_bits,
                                  RegionNode* region);
  Node* generate_interface_guard(Node* kls, RegionNode* region);

  enum ArrayKind {
    AnyArray,
    NonArray,
    ObjectArray,
    NonObjectArray,
    TypeArray
  };

  Node* generate_hidden_class_guard(Node* kls, RegionNode* region);

  Node* generate_array_guard(Node* kls, RegionNode* region, Node** obj = nullptr) {
    return generate_array_guard_common(kls, region, AnyArray, obj);
  }
  Node* generate_non_array_guard(Node* kls, RegionNode* region, Node** obj = nullptr) {
    return generate_array_guard_common(kls, region, NonArray, obj);
  }
  Node* generate_objArray_guard(Node* kls, RegionNode* region, Node** obj = nullptr) {
    return generate_array_guard_common(kls, region, ObjectArray, obj);
  }
  Node* generate_non_objArray_guard(Node* kls, RegionNode* region, Node** obj = nullptr) {
    return generate_array_guard_common(kls, region, NonObjectArray, obj);
  }
  Node* generate_typeArray_guard(Node* kls, RegionNode* region, Node** obj = nullptr) {
    return generate_array_guard_common(kls, region, TypeArray, obj);
  }
  Node* generate_array_guard_common(Node* kls, RegionNode* region, ArrayKind kind, Node** obj = nullptr);
  Node* generate_virtual_guard(Node* obj_klass, RegionNode* slow_region);
  CallJavaNode* generate_method_call(vmIntrinsicID method_id, bool is_virtual, bool is_static, bool res_not_null);
  CallJavaNode* generate_method_call_static(vmIntrinsicID method_id, bool res_not_null) {
    return generate_method_call(method_id, false, true, res_not_null);
  }
  Node* load_field_from_object(Node* fromObj, const char* fieldName, const char* fieldTypeString, DecoratorSet decorators = IN_HEAP, bool is_static = false, ciInstanceKlass* fromKls = nullptr);
  Node* field_address_from_object(Node* fromObj, const char* fieldName, const char* fieldTypeString, bool is_exact = true, bool is_static = false, ciInstanceKlass* fromKls = nullptr);

  Node* make_string_method_node(int opcode, Node* str1_start, Node* cnt1, Node* str2_start, Node* cnt2, StrIntrinsicNode::ArgEnc ae);
  bool inline_string_compareTo(StrIntrinsicNode::ArgEnc ae);
  bool inline_string_indexOf(StrIntrinsicNode::ArgEnc ae);
  bool inline_string_indexOfI(StrIntrinsicNode::ArgEnc ae);
  Node* make_indexOf_node(Node* src_start, Node* src_count, Node* tgt_start, Node* tgt_count,
                          RegionNode* region, Node* phi, StrIntrinsicNode::ArgEnc ae);
  bool inline_string_indexOfChar(StrIntrinsicNode::ArgEnc ae);
  bool inline_string_equals(StrIntrinsicNode::ArgEnc ae);
  bool inline_vectorizedHashCode();
  bool inline_string_toBytesU();
  bool inline_string_getCharsU();
  bool inline_string_copy(bool compress);
  bool inline_string_char_access(bool is_store);
  bool runtime_math(const TypeFunc* call_type, address funcAddr, const char* funcName);
  bool inline_math_native(vmIntrinsics::ID id);
  bool inline_math(vmIntrinsics::ID id);
  bool inline_double_math(vmIntrinsics::ID id);
  bool inline_math_pow();
  template <typename OverflowOp>
  bool inline_math_overflow(Node* arg1, Node* arg2);
  bool inline_math_mathExact(Node* math, Node* test);
  bool inline_math_addExactI(bool is_increment);
  bool inline_math_addExactL(bool is_increment);
  bool inline_math_multiplyExactI();
  bool inline_math_multiplyExactL();
  bool inline_math_multiplyHigh();
  bool inline_math_unsignedMultiplyHigh();
  bool inline_math_negateExactI();
  bool inline_math_negateExactL();
  bool inline_math_subtractExactI(bool is_decrement);
  bool inline_math_subtractExactL(bool is_decrement);
  bool inline_min_max(vmIntrinsics::ID id);
  bool inline_notify(vmIntrinsics::ID id);
  // This returns Type::AnyPtr, RawPtr, or OopPtr.
  int classify_unsafe_addr(Node* &base, Node* &offset, BasicType type);
  Node* make_unsafe_address(Node*& base, Node* offset, BasicType type = T_ILLEGAL, bool can_cast = false);

  typedef enum { Relaxed, Opaque, Volatile, Acquire, Release } AccessKind;
  DecoratorSet mo_decorator_for_access_kind(AccessKind kind);
  bool inline_unsafe_access(bool is_store, BasicType type, AccessKind kind, bool is_unaligned, bool is_flat = false);
  bool inline_unsafe_flat_access(bool is_store, AccessKind kind);
  static bool klass_needs_init_guard(Node* kls);
  bool inline_unsafe_allocate();
  bool inline_unsafe_newArray(bool uninitialized);
  bool inline_newArray(bool null_free, bool atomic);
  bool inline_unsafe_writeback0();
  bool inline_unsafe_writebackSync0(bool is_pre);
  bool inline_unsafe_copyMemory();
  bool inline_unsafe_isFlatArray();
  bool inline_unsafe_make_private_buffer();
  bool inline_unsafe_finish_private_buffer();
  bool inline_unsafe_setMemory();

  bool inline_native_currentCarrierThread();
  bool inline_native_currentThread();
  bool inline_native_setCurrentThread();

  bool inline_native_scopedValueCache();
  const Type* scopedValueCache_type();
  Node* scopedValueCache_helper();
  bool inline_native_setScopedValueCache();
  bool inline_native_Continuation_pinning(bool unpin);

  bool inline_native_time_funcs(address method, const char* funcName);
#if INCLUDE_JVMTI
  bool inline_native_notify_jvmti_funcs(address funcAddr, const char* funcName, bool is_start, bool is_end);
  bool inline_native_notify_jvmti_hide();
  bool inline_native_notify_jvmti_sync();
#endif

#ifdef JFR_HAVE_INTRINSICS
  bool inline_native_classID();
  bool inline_native_getEventWriter();
  bool inline_native_jvm_commit();
  void extend_setCurrentThread(Node* jt, Node* thread);
#endif
  bool inline_native_Class_query(vmIntrinsics::ID id);
  bool inline_primitive_Class_conversion(vmIntrinsics::ID id);
  bool inline_native_subtype_check();
  bool inline_native_getLength();
  bool inline_array_copyOf(bool is_copyOfRange);
  bool inline_array_equals(StrIntrinsicNode::ArgEnc ae);
  bool inline_preconditions_checkIndex(BasicType bt);
  void copy_to_clone(Node* obj, Node* alloc_obj, Node* obj_size, bool is_array);
  bool inline_native_clone(bool is_virtual);
  bool inline_native_Reflection_getCallerClass();
  // Helper function for inlining native object hash method
  bool inline_native_hashcode(bool is_virtual, bool is_static);
  bool inline_native_getClass();

  // Helper functions for inlining arraycopy
  bool inline_arraycopy();
  AllocateArrayNode* tightly_coupled_allocation(Node* ptr);
  static CallStaticJavaNode* get_uncommon_trap_from_success_proj(Node* node);
  SafePointNode* create_safepoint_with_state_before_array_allocation(const AllocateArrayNode* alloc) const;
  void replace_unrelated_uncommon_traps_with_alloc_state(AllocateArrayNode* alloc, JVMState* saved_jvms_before_guards);
  void replace_unrelated_uncommon_traps_with_alloc_state(JVMState* saved_jvms_before_guards);
  void create_new_uncommon_trap(CallStaticJavaNode* uncommon_trap_call);
  JVMState* arraycopy_restore_alloc_state(AllocateArrayNode* alloc, int& saved_reexecute_sp);
  void arraycopy_move_allocation_here(AllocateArrayNode* alloc, Node* dest, JVMState* saved_jvms_before_guards, int saved_reexecute_sp,
                                      uint new_idx);
  bool check_array_sort_arguments(Node* elementType, Node* obj, BasicType& bt);
  bool inline_array_sort();
  bool inline_array_partition();
  typedef enum { LS_get_add, LS_get_set, LS_cmp_swap, LS_cmp_swap_weak, LS_cmp_exchange } LoadStoreKind;
  bool inline_unsafe_load_store(BasicType type,  LoadStoreKind kind, AccessKind access_kind);
  bool inline_unsafe_fence(vmIntrinsics::ID id);
  bool inline_onspinwait();
  bool inline_fp_conversions(vmIntrinsics::ID id);
  bool inline_fp_range_check(vmIntrinsics::ID id);
  bool inline_fp16_operations(vmIntrinsics::ID id, int num_args);
  Node* unbox_fp16_value(const TypeInstPtr* box_class, ciField* field, Node* box);
  Node* box_fp16_value(const TypeInstPtr* box_class, ciField* field, Node* value);
  bool inline_number_methods(vmIntrinsics::ID id);
  bool inline_bitshuffle_methods(vmIntrinsics::ID id);
  bool inline_compare_unsigned(vmIntrinsics::ID id);
  bool inline_divmod_methods(vmIntrinsics::ID id);
  bool inline_reference_get();
  bool inline_reference_refersTo0(bool is_phantom);
  bool inline_reference_clear0(bool is_phantom);
  bool inline_Class_cast();
  bool inline_aescrypt_Block(vmIntrinsics::ID id);
  bool inline_cipherBlockChaining_AESCrypt(vmIntrinsics::ID id);
  bool inline_electronicCodeBook_AESCrypt(vmIntrinsics::ID id);
  bool inline_counterMode_AESCrypt(vmIntrinsics::ID id);
  Node* inline_cipherBlockChaining_AESCrypt_predicate(bool decrypting);
  Node* inline_electronicCodeBook_AESCrypt_predicate(bool decrypting);
  Node* inline_counterMode_AESCrypt_predicate();
  Node* get_key_start_from_aescrypt_object(Node* aescrypt_object);
  bool inline_ghash_processBlocks();
  bool inline_chacha20Block();
  bool inline_kyberNtt();
  bool inline_kyberInverseNtt();
  bool inline_kyberNttMult();
  bool inline_kyberAddPoly_2();
  bool inline_kyberAddPoly_3();
  bool inline_kyber12To16();
  bool inline_kyberBarrettReduce();
  bool inline_dilithiumAlmostNtt();
  bool inline_dilithiumAlmostInverseNtt();
  bool inline_dilithiumNttMult();
  bool inline_dilithiumMontMulByConstant();
  bool inline_dilithiumDecomposePoly();
  bool inline_base64_encodeBlock();
  bool inline_base64_decodeBlock();
  bool inline_poly1305_processBlocks();
  bool inline_intpoly_montgomeryMult_P256();
  bool inline_intpoly_assign();
  bool inline_digestBase_implCompress(vmIntrinsics::ID id);
  bool inline_double_keccak();
  bool inline_digestBase_implCompressMB(int predicate);
  bool inline_digestBase_implCompressMB(Node* digestBaseObj, ciInstanceKlass* instklass,
                                        BasicType elem_type, address stubAddr, const char *stubName,
                                        Node* src_start, Node* ofs, Node* limit);
  Node* get_state_from_digest_object(Node *digestBase_object, BasicType elem_type);
  Node* get_block_size_from_digest_object(Node *digestBase_object);
  Node* inline_digestBase_implCompressMB_predicate(int predicate);
  bool inline_encodeISOArray(bool ascii);
  bool inline_updateCRC32();
  bool inline_updateBytesCRC32();
  bool inline_updateByteBufferCRC32();
  Node* get_table_from_crc32c_class(ciInstanceKlass *crc32c_class);
  bool inline_updateBytesCRC32C();
  bool inline_updateDirectByteBufferCRC32C();
  bool inline_updateBytesAdler32();
  bool inline_updateByteBufferAdler32();
  bool inline_multiplyToLen();
  bool inline_countPositives();
  bool inline_squareToLen();
  bool inline_mulAdd();
  bool inline_montgomeryMultiply();
  bool inline_montgomerySquare();
  bool inline_bigIntegerShift(bool isRightShift);
  bool inline_vectorizedMismatch();
  bool inline_fma(vmIntrinsics::ID id);
  bool inline_character_compare(vmIntrinsics::ID id);
  bool inline_galoisCounterMode_AESCrypt();
  Node* inline_galoisCounterMode_AESCrypt_predicate();

  bool inline_profileBoolean();
  bool inline_isCompileConstant();

  bool inline_continuation_do_yield();

  // Vector API support
  bool inline_vector_nary_operation(int n);
  bool inline_vector_call(int arity);
  bool inline_vector_frombits_coerced();
  bool inline_vector_mask_operation();
  bool inline_vector_mem_operation(bool is_store);
  bool inline_vector_mem_masked_operation(bool is_store);
  bool inline_vector_gather_scatter(bool is_scatter);
  bool inline_vector_reduction();
  bool inline_vector_test();
  bool inline_vector_blend();
  bool inline_vector_rearrange();
  bool inline_vector_select_from();
  bool inline_vector_compare();
  bool inline_vector_broadcast_int();
  bool inline_vector_convert();
  bool inline_vector_extract();
  bool inline_vector_insert();
  bool inline_vector_compress_expand();
  bool inline_index_vector();
  bool inline_index_partially_in_upper_range();
  bool inline_vector_select_from_two_vectors();

  Node* gen_call_to_vector_math(int vector_api_op_id, BasicType bt, int num_elem, Node* opd1, Node* opd2);

  enum VectorMaskUseType {
    VecMaskUseLoad  = 1 << 0,
    VecMaskUseStore = 1 << 1,
    VecMaskUseAll   = VecMaskUseLoad | VecMaskUseStore,
    VecMaskUsePred  = 1 << 2,
    VecMaskNotUsed  = 1 << 3
  };

  bool arch_supports_vector(int op, int num_elem, BasicType type, VectorMaskUseType mask_use_type, bool has_scalar_args = false);
  bool arch_supports_vector_rotate(int opc, int num_elem, BasicType elem_bt, VectorMaskUseType mask_use_type, bool has_scalar_args = false);

  void clear_upper_avx() {
#ifdef X86
    if (UseAVX >= 2) {
      C->set_clear_upper_avx(true);
    }
#endif
  }

  bool inline_getObjectSize();

  bool inline_blackhole();
};

