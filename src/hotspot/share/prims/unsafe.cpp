/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoadInfo.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "jfr/jfrEvents.hpp"
#include "jni.h"
#include "jvm.h"
#include "memory/allocation.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "oops/access.inline.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/flatArrayKlass.hpp"
#include "oops/flatArrayOop.inline.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/unsafe.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/globals.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaThread.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/reflection.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vm_version.hpp"
#include "sanitizers/ub.hpp"
#include "services/threadService.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/macros.hpp"

/**
 * Implementation of the jdk.internal.misc.Unsafe class
 */


#define MAX_OBJECT_SIZE \
  ( arrayOopDesc::base_offset_in_bytes(T_DOUBLE) \
    + ((julong)max_jint * sizeof(double)) )

#define UNSAFE_ENTRY(result_type, header) \
  JVM_ENTRY(static result_type, header)

#define UNSAFE_LEAF(result_type, header) \
  JVM_LEAF(static result_type, header)

// All memory access methods (e.g. getInt, copyMemory) must use this macro.
// We call these methods "scoped" methods, as access to these methods is
// typically governed by a "scope" (a MemorySessionImpl object), and no
// access is allowed when the scope is no longer alive.
//
// Closing a scope object (cf. scopedMemoryAccess.cpp) can install
// an async exception during a safepoint. When that happens,
// scoped methods are not allowed to touch the underlying memory (as that
// memory might have been released). Therefore, when entering a scoped method
// we check if an async exception has been installed, and return immediately
// if that is the case.
//
// As a rule, we disallow safepoints in the middle of a scoped method.
// If an async exception handshake were installed in such a safepoint,
// memory access might still occur before the handshake is honored by
// the accessing thread.
//
// Corollary: as threads in native state are considered to be at a safepoint,
// scoped methods must NOT be executed while in the native thread state.
// Because of this, there can be no UNSAFE_LEAF_SCOPED.
#define UNSAFE_ENTRY_SCOPED(result_type, header) \
  JVM_ENTRY(static result_type, header) \
  if (thread->has_async_exception_condition()) {return (result_type)0;}

#define UNSAFE_END JVM_END


static inline void* addr_from_java(jlong addr) {
  // This assert fails in a variety of ways on 32-bit systems.
  // It is impossible to predict whether native code that converts
  // pointers to longs will sign-extend or zero-extend the addresses.
  //assert(addr == (uintptr_t)addr, "must not be odd high bits");
  return (void*)(uintptr_t)addr;
}

static inline jlong addr_to_java(void* p) {
  assert(p == (void*)(uintptr_t)p, "must not be odd high bits");
  return (uintptr_t)p;
}


// Note: The VM's obj_field and related accessors use byte-scaled
// ("unscaled") offsets, just as the unsafe methods do.

// However, the method Unsafe.fieldOffset explicitly declines to
// guarantee this.  The field offset values manipulated by the Java user
// through the Unsafe API are opaque cookies that just happen to be byte
// offsets.  We represent this state of affairs by passing the cookies
// through conversion functions when going between the VM and the Unsafe API.
// The conversion functions just happen to be no-ops at present.

static inline jlong field_offset_to_byte_offset(jlong field_offset) {
  return field_offset;
}

static inline int field_offset_from_byte_offset(int byte_offset) {
  return byte_offset;
}

static inline void assert_field_offset_sane(oop p, jlong field_offset) {
#ifdef ASSERT
  jlong byte_offset = field_offset_to_byte_offset(field_offset);

  if (p != nullptr) {
    assert(byte_offset >= 0 && byte_offset <= (jlong)MAX_OBJECT_SIZE, "sane offset");
    if (byte_offset == (jint)byte_offset) {
      void* ptr_plus_disp = cast_from_oop<address>(p) + byte_offset;
      assert(p->field_addr<void>((jint)byte_offset) == ptr_plus_disp,
             "raw [ptr+disp] must be consistent with oop::field_addr");
    }
    jlong p_size = HeapWordSize * (jlong)(p->size());
    assert(byte_offset < p_size, "Unsafe access: offset " INT64_FORMAT " > object's size " INT64_FORMAT, (int64_t)byte_offset, (int64_t)p_size);
  }
#endif
}

static inline void* index_oop_from_field_offset_long(oop p, jlong field_offset) {
  assert_field_offset_sane(p, field_offset);
  uintptr_t base_address = cast_from_oop<uintptr_t>(p);
  uintptr_t byte_offset  = (uintptr_t)field_offset_to_byte_offset(field_offset);
  return (void*)(base_address + byte_offset);
}

// Externally callable versions:
// (Use these in compiler intrinsics which emulate unsafe primitives.)
jlong Unsafe_field_offset_to_byte_offset(jlong field_offset) {
  return field_offset;
}
jlong Unsafe_field_offset_from_byte_offset(jlong byte_offset) {
  return byte_offset;
}

///// Data read/writes on the Java heap and in native (off-heap) memory

/**
 * Helper class to wrap memory accesses in JavaThread::doing_unsafe_access()
 */
class GuardUnsafeAccess {
  JavaThread* _thread;

public:
  GuardUnsafeAccess(JavaThread* thread) : _thread(thread) {
    // native/off-heap access which may raise SIGBUS if accessing
    // memory mapped file data in a region of the file which has
    // been truncated and is now invalid.
    _thread->set_doing_unsafe_access(true);
  }

  ~GuardUnsafeAccess() {
    _thread->set_doing_unsafe_access(false);
  }
};

/**
 * Helper class for accessing memory.
 *
 * Normalizes values and wraps accesses in
 * JavaThread::doing_unsafe_access() if needed.
 */
template <typename T>
class MemoryAccess : StackObj {
  JavaThread* _thread;
  oop _obj;
  ptrdiff_t _offset;

  // Resolves and returns the address of the memory access.
  // This raw memory access may fault, so we make sure it happens within the
  // guarded scope by making the access volatile at least. Since the store
  // of Thread::set_doing_unsafe_access() is also volatile, these accesses
  // can not be reordered by the compiler. Therefore, if the access triggers
  // a fault, we will know that Thread::doing_unsafe_access() returns true.
  volatile T* addr() {
    void* addr = index_oop_from_field_offset_long(_obj, _offset);
    return static_cast<volatile T*>(addr);
  }

  template <typename U>
  U normalize_for_write(U x) {
    return x;
  }

  jboolean normalize_for_write(jboolean x) {
    return x & 1;
  }

  template <typename U>
  U normalize_for_read(U x) {
    return x;
  }

  jboolean normalize_for_read(jboolean x) {
    return x != 0;
  }

public:
  MemoryAccess(JavaThread* thread, jobject obj, jlong offset)
    : _thread(thread), _obj(JNIHandles::resolve(obj)), _offset((ptrdiff_t)offset) {
    assert_field_offset_sane(_obj, offset);
  }

  T get() {
    GuardUnsafeAccess guard(_thread);
    return normalize_for_read(*addr());
  }

  // we use this method at some places for writing to 0 e.g. to cause a crash;
  // ubsan does not know that this is the desired behavior
  ATTRIBUTE_NO_UBSAN
  void put(T x) {
    GuardUnsafeAccess guard(_thread);
    assert(_obj == nullptr || !_obj->is_inline_type() || _obj->mark().is_larval_state(), "must be an object instance or a larval inline type");
    *addr() = normalize_for_write(x);
  }

  T get_volatile() {
    GuardUnsafeAccess guard(_thread);
    volatile T ret = RawAccess<MO_SEQ_CST>::load(addr());
    return normalize_for_read(ret);
  }

  void put_volatile(T x) {
    GuardUnsafeAccess guard(_thread);
    RawAccess<MO_SEQ_CST>::store(addr(), normalize_for_write(x));
  }
};

#ifdef ASSERT
/*
 * Get the field descriptor of the field of the given object at the given offset.
 */
static bool get_field_descriptor(oop p, jlong offset, fieldDescriptor* fd) {
  bool found = false;
  Klass* k = p->klass();
  if (k->is_instance_klass()) {
    InstanceKlass* ik = InstanceKlass::cast(k);
    found = ik->find_field_from_offset((int)offset, false, fd);
    if (!found && ik->is_mirror_instance_klass()) {
      Klass* k2 = java_lang_Class::as_Klass(p);
      if (k2->is_instance_klass()) {
        ik = InstanceKlass::cast(k2);
        found = ik->find_field_from_offset((int)offset, true, fd);
      }
    }
  }
  return found;
}
#endif // ASSERT

static void assert_and_log_unsafe_value_access(oop p, jlong offset, InlineKlass* vk) {
  Klass* k = p->klass();
#ifdef ASSERT
  if (k->is_instance_klass()) {
    assert_field_offset_sane(p, offset);
    fieldDescriptor fd;
    bool found = get_field_descriptor(p, offset, &fd);
    if (found) {
      assert(found, "value field not found");
      assert(fd.is_flat(), "field not flat");
    } else {
      if (log_is_enabled(Trace, valuetypes)) {
        log_trace(valuetypes)("not a field in %s at offset " UINT64_FORMAT_X,
                              p->klass()->external_name(), (uint64_t)offset);
      }
    }
  } else if (k->is_flatArray_klass()) {
    FlatArrayKlass* vak = FlatArrayKlass::cast(k);
    int index = (offset - vak->array_header_in_bytes()) / vak->element_byte_size();
    address dest = (address)((flatArrayOop)p)->value_at_addr(index, vak->layout_helper());
    assert(dest == (cast_from_oop<address>(p) + offset), "invalid offset");
  } else {
    ShouldNotReachHere();
  }
#endif // ASSERT
  if (log_is_enabled(Trace, valuetypes)) {
    if (k->is_flatArray_klass()) {
      FlatArrayKlass* vak = FlatArrayKlass::cast(k);
      int index = (offset - vak->array_header_in_bytes()) / vak->element_byte_size();
      address dest = (address)((flatArrayOop)p)->value_at_addr(index, vak->layout_helper());
      log_trace(valuetypes)("%s array type %s index %d element size %d offset " UINT64_FORMAT_X " at " INTPTR_FORMAT,
                            p->klass()->external_name(), vak->external_name(),
                            index, vak->element_byte_size(), (uint64_t)offset, p2i(dest));
    } else {
      log_trace(valuetypes)("%s field type %s at offset " UINT64_FORMAT_X,
                            p->klass()->external_name(), vk->external_name(), (uint64_t)offset);
    }
  }
}

// These functions allow a null base pointer with an arbitrary address.
// But if the base pointer is non-null, the offset should make some sense.
// That is, it should be in the range [0, MAX_OBJECT_SIZE].
UNSAFE_ENTRY(jobject, Unsafe_GetReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) {
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop v = HeapAccess<ON_UNKNOWN_OOP_REF>::oop_load_at(p, offset);
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_PutReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  assert(!p->is_inline_type() || p->mark().is_larval_state(), "must be an object instance or a larval inline type");
  HeapAccess<ON_UNKNOWN_OOP_REF>::oop_store_at(p, offset, x);
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_ValueHeaderSize(JNIEnv *env, jobject unsafe, jclass c)) {
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(c));
  InlineKlass* vk = InlineKlass::cast(k);
  return vk->payload_offset();
} UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_IsFlatField(JNIEnv *env, jobject unsafe, jobject o)) {
  oop f = JNIHandles::resolve_non_null(o);
  Klass* k = java_lang_Class::as_Klass(java_lang_reflect_Field::clazz(f));
  int slot = java_lang_reflect_Field::slot(f);
  return InstanceKlass::cast(k)->field_is_flat(slot);
} UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_HasNullMarker(JNIEnv *env, jobject unsafe, jobject o)) {
  oop f = JNIHandles::resolve_non_null(o);
  Klass* k = java_lang_Class::as_Klass(java_lang_reflect_Field::clazz(f));
  int slot = java_lang_reflect_Field::slot(f);
  return InstanceKlass::cast(k)->field_has_null_marker(slot);
} UNSAFE_END

UNSAFE_ENTRY(jint, Unsafe_NullMarkerOffset(JNIEnv *env, jobject unsafe, jobject o)) {
  oop f = JNIHandles::resolve_non_null(o);
  Klass* k = java_lang_Class::as_Klass(java_lang_reflect_Field::clazz(f));
  int slot = java_lang_reflect_Field::slot(f);
  return InstanceKlass::cast(k)->null_marker_offset(slot);
} UNSAFE_END

UNSAFE_ENTRY(jint, Unsafe_ArrayLayout(JNIEnv *env, jobject unsafe, jclass c)) {
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(c));
  if (!k->is_flatArray_klass()) {
    return (jint)LayoutKind::REFERENCE;
  } else {
    return (jint)FlatArrayKlass::cast(k)->layout_kind();
  }
} UNSAFE_END

UNSAFE_ENTRY(jint, Unsafe_FieldLayout(JNIEnv *env, jobject unsafe, jobject field)) {
  assert(field != nullptr, "field must not be null");

  oop reflected   = JNIHandles::resolve_non_null(field);
  oop mirror      = java_lang_reflect_Field::clazz(reflected);
  Klass* k        = java_lang_Class::as_Klass(mirror);
  int slot        = java_lang_reflect_Field::slot(reflected);
  int modifiers   = java_lang_reflect_Field::modifiers(reflected);

  if ((modifiers & JVM_ACC_STATIC) != 0) {
    return (jint)LayoutKind::REFERENCE; // static fields are never flat
  } else {
    InstanceKlass* ik = InstanceKlass::cast(k);
    if (ik->field_is_flat(slot)) {
      return (jint)ik->inline_layout_info(slot).kind();
    } else {
      return (jint)LayoutKind::REFERENCE;
    }
  }
} UNSAFE_END

UNSAFE_ENTRY(jarray, Unsafe_NewSpecialArray(JNIEnv *env, jobject unsafe, jclass elmClass, jint len, jint layoutKind)) {
  oop mirror = JNIHandles::resolve_non_null(elmClass);
  Klass* klass = java_lang_Class::as_Klass(mirror);
  klass->initialize(CHECK_NULL);
  if (len < 0) {
    THROW_MSG_NULL(vmSymbols::java_lang_IllegalArgumentException(), "Array length is negative");
  }
  if (klass->is_array_klass() || klass->is_identity_class()) {
    THROW_MSG_NULL(vmSymbols::java_lang_IllegalArgumentException(), "Element class is not a value class");
  }
  if (klass->is_abstract()) {
    THROW_MSG_NULL(vmSymbols::java_lang_IllegalArgumentException(), "Element class is abstract");
  }
  LayoutKind lk = static_cast<LayoutKind>(layoutKind);
  if (lk <= LayoutKind::REFERENCE || lk >= LayoutKind::UNKNOWN) {
    THROW_MSG_NULL(vmSymbols::java_lang_IllegalArgumentException(), "Invalid layout kind");
  }
  InlineKlass* vk = InlineKlass::cast(klass);
  // WARNING: test below will need modifications when flat layouts supported for fields
  // but not for arrays are introduce (NULLABLE_NON_ATOMIC_FLAT for instance)
  if (!UseArrayFlattening || !vk->is_layout_supported(lk)) {
    THROW_MSG_NULL(vmSymbols::java_lang_UnsupportedOperationException(), "Layout not supported");
  }
  oop array = oopFactory::new_flatArray(vk, len, lk, CHECK_NULL);
  return (jarray) JNIHandles::make_local(THREAD, array);
} UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_IsFlatArray(JNIEnv *env, jobject unsafe, jclass c)) {
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(c));
  return k->is_flatArray_klass();
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_GetValue(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jclass vc)) {
  oop base = JNIHandles::resolve(obj);
  if (base == nullptr) {
    THROW_NULL(vmSymbols::java_lang_NullPointerException());
  }
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(vc));
  InlineKlass* vk = InlineKlass::cast(k);
  assert_and_log_unsafe_value_access(base, offset, vk);
  LayoutKind lk = LayoutKind::UNKNOWN;
  if (base->is_array()) {
    FlatArrayKlass* fak = FlatArrayKlass::cast(base->klass());
    lk = fak->layout_kind();
  } else {
    fieldDescriptor fd;
    InstanceKlass::cast(base->klass())->find_field_from_offset(offset, false, &fd);
    lk = fd.field_holder()->inline_layout_info(fd.index()).kind();
  }
  Handle base_h(THREAD, base);
  oop v = vk->read_payload_from_addr(base_h(), offset, lk, CHECK_NULL);
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_GetFlatValue(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint layoutKind, jclass vc)) {
  assert(layoutKind != (int)LayoutKind::REFERENCE, "This method handles only flat layouts");
  oop base = JNIHandles::resolve(obj);
  if (base == nullptr) {
    THROW_NULL(vmSymbols::java_lang_NullPointerException());
  }
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(vc));
  InlineKlass* vk = InlineKlass::cast(k);
  assert_and_log_unsafe_value_access(base, offset, vk);
  LayoutKind lk = (LayoutKind)layoutKind;
  Handle base_h(THREAD, base);
  oop v = vk->read_payload_from_addr(base_h(), offset, lk, CHECK_NULL);
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_PutValue(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jclass vc, jobject value)) {
  oop base = JNIHandles::resolve(obj);
  if (base == nullptr) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(vc));
  InlineKlass* vk = InlineKlass::cast(k);
  assert(!base->is_inline_type() || base->mark().is_larval_state(), "must be an object instance or a larval inline type");
  assert_and_log_unsafe_value_access(base, offset, vk);
  LayoutKind lk = LayoutKind::UNKNOWN;
  if (base->is_array()) {
    FlatArrayKlass* fak = FlatArrayKlass::cast(base->klass());
    lk = fak->layout_kind();
  } else {
    fieldDescriptor fd;
    InstanceKlass::cast(base->klass())->find_field_from_offset(offset, false, &fd);
    lk = fd.field_holder()->inline_layout_info(fd.index()).kind();
  }
  oop v = JNIHandles::resolve(value);
  vk->write_value_to_addr(v, ((char*)(oopDesc*)base) + offset, lk, true, CHECK);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_PutFlatValue(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint layoutKind, jclass vc, jobject value)) {
  assert(layoutKind != (int)LayoutKind::REFERENCE, "This method handles only flat layouts");
  oop base = JNIHandles::resolve(obj);
  if (base == nullptr) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(vc));
  InlineKlass* vk = InlineKlass::cast(k);
  assert(!base->is_inline_type() || base->mark().is_larval_state(), "must be an object instance or a larval inline type");
  assert_and_log_unsafe_value_access(base, offset, vk);
  LayoutKind lk = (LayoutKind)layoutKind;
  oop v = JNIHandles::resolve(value);
  vk->write_value_to_addr(v, ((char*)(oopDesc*)base) + offset, lk, true, CHECK);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_MakePrivateBuffer(JNIEnv *env, jobject unsafe, jobject value)) {
  oop v = JNIHandles::resolve_non_null(value);
  assert(v->is_inline_type(), "must be an inline type instance");
  Handle vh(THREAD, v);
  InlineKlass* vk = InlineKlass::cast(v->klass());
  instanceOop new_value = vk->allocate_instance_buffer(CHECK_NULL);
  vk->copy_payload_to_addr(vk->payload_addr(vh()), vk->payload_addr(new_value), LayoutKind::BUFFERED, false);
  markWord mark = new_value->mark();
  new_value->set_mark(mark.enter_larval_state());
  return JNIHandles::make_local(THREAD, new_value);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_FinishPrivateBuffer(JNIEnv *env, jobject unsafe, jobject value)) {
  oop v = JNIHandles::resolve(value);
  assert(v->mark().is_larval_state(), "must be a larval value");
  markWord mark = v->mark();
  v->set_mark(mark.exit_larval_state());
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_GetReferenceVolatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) {
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop v = HeapAccess<MO_SEQ_CST | ON_UNKNOWN_OOP_REF>::oop_load_at(p, offset);
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_PutReferenceVolatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  HeapAccess<MO_SEQ_CST | ON_UNKNOWN_OOP_REF>::oop_store_at(p, offset, x);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_GetUncompressedObject(JNIEnv *env, jobject unsafe, jlong addr)) {
  oop v = *(oop*) (address) addr;
  return JNIHandles::make_local(THREAD, v);
} UNSAFE_END

#define DEFINE_GETSETOOP(java_type, Type) \
 \
UNSAFE_ENTRY_SCOPED(java_type, Unsafe_Get##Type(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) { \
  return MemoryAccess<java_type>(thread, obj, offset).get(); \
} UNSAFE_END \
 \
UNSAFE_ENTRY_SCOPED(void, Unsafe_Put##Type(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, java_type x)) { \
  MemoryAccess<java_type>(thread, obj, offset).put(x); \
} UNSAFE_END \
 \
// END DEFINE_GETSETOOP.

DEFINE_GETSETOOP(jboolean, Boolean)
DEFINE_GETSETOOP(jbyte, Byte)
DEFINE_GETSETOOP(jshort, Short);
DEFINE_GETSETOOP(jchar, Char);
DEFINE_GETSETOOP(jint, Int);
DEFINE_GETSETOOP(jlong, Long);
DEFINE_GETSETOOP(jfloat, Float);
DEFINE_GETSETOOP(jdouble, Double);

#undef DEFINE_GETSETOOP

#define DEFINE_GETSETOOP_VOLATILE(java_type, Type) \
 \
UNSAFE_ENTRY_SCOPED(java_type, Unsafe_Get##Type##Volatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset)) { \
  return MemoryAccess<java_type>(thread, obj, offset).get_volatile(); \
} UNSAFE_END \
 \
UNSAFE_ENTRY_SCOPED(void, Unsafe_Put##Type##Volatile(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, java_type x)) { \
  MemoryAccess<java_type>(thread, obj, offset).put_volatile(x); \
} UNSAFE_END \
 \
// END DEFINE_GETSETOOP_VOLATILE.

DEFINE_GETSETOOP_VOLATILE(jboolean, Boolean)
DEFINE_GETSETOOP_VOLATILE(jbyte, Byte)
DEFINE_GETSETOOP_VOLATILE(jshort, Short);
DEFINE_GETSETOOP_VOLATILE(jchar, Char);
DEFINE_GETSETOOP_VOLATILE(jint, Int);
DEFINE_GETSETOOP_VOLATILE(jlong, Long);
DEFINE_GETSETOOP_VOLATILE(jfloat, Float);
DEFINE_GETSETOOP_VOLATILE(jdouble, Double);

#undef DEFINE_GETSETOOP_VOLATILE

UNSAFE_LEAF(void, Unsafe_FullFence(JNIEnv *env, jobject unsafe)) {
  OrderAccess::fence();
} UNSAFE_END

////// Allocation requests

UNSAFE_ENTRY(jobject, Unsafe_AllocateInstance(JNIEnv *env, jobject unsafe, jclass cls)) {
  JvmtiVMObjectAllocEventCollector oam;
  instanceOop i = InstanceKlass::allocate_instance(JNIHandles::resolve_non_null(cls), CHECK_NULL);
  return JNIHandles::make_local(THREAD, i);
} UNSAFE_END

UNSAFE_LEAF(jlong, Unsafe_AllocateMemory0(JNIEnv *env, jobject unsafe, jlong size)) {
  size_t sz = (size_t)size;

  assert(is_aligned(sz, HeapWordSize), "sz not aligned");

  void* x = os::malloc(sz, mtOther);

  return addr_to_java(x);
} UNSAFE_END

UNSAFE_LEAF(jlong, Unsafe_ReallocateMemory0(JNIEnv *env, jobject unsafe, jlong addr, jlong size)) {
  void* p = addr_from_java(addr);
  size_t sz = (size_t)size;

  assert(is_aligned(sz, HeapWordSize), "sz not aligned");

  void* x = os::realloc(p, sz, mtOther);

  return addr_to_java(x);
} UNSAFE_END

UNSAFE_LEAF(void, Unsafe_FreeMemory0(JNIEnv *env, jobject unsafe, jlong addr)) {
  void* p = addr_from_java(addr);

  os::free(p);
} UNSAFE_END

UNSAFE_ENTRY_SCOPED(void, Unsafe_SetMemory0(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jlong size, jbyte value)) {
  size_t sz = (size_t)size;

  oop base = JNIHandles::resolve(obj);
  void* p = index_oop_from_field_offset_long(base, offset);

  {
    GuardUnsafeAccess guard(thread);
    if (StubRoutines::unsafe_setmemory() != nullptr) {
      MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, thread));
      StubRoutines::UnsafeSetMemory_stub()(p, sz, value);
    } else {
      Copy::fill_to_memory_atomic(p, sz, value);
    }
  }
} UNSAFE_END

UNSAFE_ENTRY_SCOPED(void, Unsafe_CopyMemory0(JNIEnv *env, jobject unsafe, jobject srcObj, jlong srcOffset, jobject dstObj, jlong dstOffset, jlong size)) {
  size_t sz = (size_t)size;

  oop srcp = JNIHandles::resolve(srcObj);
  oop dstp = JNIHandles::resolve(dstObj);

  void* src = index_oop_from_field_offset_long(srcp, srcOffset);
  void* dst = index_oop_from_field_offset_long(dstp, dstOffset);
  {
    GuardUnsafeAccess guard(thread);
    if (StubRoutines::unsafe_arraycopy() != nullptr) {
      MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, thread));
      StubRoutines::UnsafeArrayCopy_stub()(src, dst, sz);
    } else {
      Copy::conjoint_memory_atomic(src, dst, sz);
    }
  }
} UNSAFE_END

UNSAFE_ENTRY_SCOPED(void, Unsafe_CopySwapMemory0(JNIEnv *env, jobject unsafe, jobject srcObj, jlong srcOffset, jobject dstObj, jlong dstOffset, jlong size, jlong elemSize)) {
  size_t sz = (size_t)size;
  size_t esz = (size_t)elemSize;

  oop srcp = JNIHandles::resolve(srcObj);
  oop dstp = JNIHandles::resolve(dstObj);

  address src = (address)index_oop_from_field_offset_long(srcp, srcOffset);
  address dst = (address)index_oop_from_field_offset_long(dstp, dstOffset);

  {
    GuardUnsafeAccess guard(thread);
    Copy::conjoint_swap(src, dst, sz, esz);
  }
} UNSAFE_END

UNSAFE_LEAF (void, Unsafe_WriteBack0(JNIEnv *env, jobject unsafe, jlong line)) {
  assert(VM_Version::supports_data_cache_line_flush(), "should not get here");
#ifdef ASSERT
  if (TraceMemoryWriteback) {
    tty->print_cr("Unsafe: writeback 0x%p", addr_from_java(line));
  }
#endif

  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, Thread::current()));
  assert(StubRoutines::data_cache_writeback() != nullptr, "sanity");
  (StubRoutines::DataCacheWriteback_stub())(addr_from_java(line));
} UNSAFE_END

static void doWriteBackSync0(bool is_pre)
{
  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXExec, Thread::current()));
  assert(StubRoutines::data_cache_writeback_sync() != nullptr, "sanity");
  (StubRoutines::DataCacheWritebackSync_stub())(is_pre);
}

UNSAFE_LEAF (void, Unsafe_WriteBackPreSync0(JNIEnv *env, jobject unsafe)) {
  assert(VM_Version::supports_data_cache_line_flush(), "should not get here");
#ifdef ASSERT
  if (TraceMemoryWriteback) {
      tty->print_cr("Unsafe: writeback pre-sync");
  }
#endif

  doWriteBackSync0(true);
} UNSAFE_END

UNSAFE_LEAF (void, Unsafe_WriteBackPostSync0(JNIEnv *env, jobject unsafe)) {
  assert(VM_Version::supports_data_cache_line_flush(), "should not get here");
#ifdef ASSERT
  if (TraceMemoryWriteback) {
    tty->print_cr("Unsafe: writeback pre-sync");
  }
#endif

  doWriteBackSync0(false);
} UNSAFE_END

////// Random queries

static jlong find_field_offset(jclass clazz, jstring name, TRAPS) {
  assert(clazz != nullptr, "clazz must not be null");
  assert(name != nullptr, "name must not be null");

  ResourceMark rm(THREAD);
  char *utf_name = java_lang_String::as_utf8_string(JNIHandles::resolve_non_null(name));

  InstanceKlass* k = InstanceKlass::cast(java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz)));

  jint offset = -1;
  for (JavaFieldStream fs(k); !fs.done(); fs.next()) {
    Symbol *name = fs.name();
    if (name->equals(utf_name)) {
      offset = fs.offset();
      break;
    }
  }
  if (offset < 0) {
    THROW_0(vmSymbols::java_lang_InternalError());
  }
  return field_offset_from_byte_offset(offset);
}

static jlong find_field_offset(jobject field, int must_be_static, TRAPS) {
  assert(field != nullptr, "field must not be null");

  oop reflected   = JNIHandles::resolve_non_null(field);
  oop mirror      = java_lang_reflect_Field::clazz(reflected);
  Klass* k        = java_lang_Class::as_Klass(mirror);
  int slot        = java_lang_reflect_Field::slot(reflected);
  int modifiers   = java_lang_reflect_Field::modifiers(reflected);

  if (must_be_static >= 0) {
    int really_is_static = ((modifiers & JVM_ACC_STATIC) != 0);
    if (must_be_static != really_is_static) {
      THROW_0(vmSymbols::java_lang_IllegalArgumentException());
    }
  }

  int offset = InstanceKlass::cast(k)->field_offset(slot);
  return field_offset_from_byte_offset(offset);
}

UNSAFE_ENTRY(jlong, Unsafe_ObjectFieldOffset0(JNIEnv *env, jobject unsafe, jobject field)) {
  return find_field_offset(field, 0, THREAD);
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_ObjectFieldOffset1(JNIEnv *env, jobject unsafe, jclass c, jstring name)) {
  return find_field_offset(c, name, THREAD);
} UNSAFE_END

UNSAFE_ENTRY(jlong, Unsafe_StaticFieldOffset0(JNIEnv *env, jobject unsafe, jobject field)) {
  return find_field_offset(field, 1, THREAD);
} UNSAFE_END

UNSAFE_ENTRY(jobject, Unsafe_StaticFieldBase0(JNIEnv *env, jobject unsafe, jobject field)) {
  assert(field != nullptr, "field must not be null");

  // Note:  In this VM implementation, a field address is always a short
  // offset from the base of a klass metaobject.  Thus, the full dynamic
  // range of the return type is never used.  However, some implementations
  // might put the static field inside an array shared by many classes,
  // or even at a fixed address, in which case the address could be quite
  // large.  In that last case, this function would return null, since
  // the address would operate alone, without any base pointer.

  oop reflected   = JNIHandles::resolve_non_null(field);
  oop mirror      = java_lang_reflect_Field::clazz(reflected);
  int modifiers   = java_lang_reflect_Field::modifiers(reflected);

  if ((modifiers & JVM_ACC_STATIC) == 0) {
    THROW_NULL(vmSymbols::java_lang_IllegalArgumentException());
  }

  return JNIHandles::make_local(THREAD, mirror);
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_EnsureClassInitialized0(JNIEnv *env, jobject unsafe, jobject clazz)) {
  assert(clazz != nullptr, "clazz must not be null");

  oop mirror = JNIHandles::resolve_non_null(clazz);

  Klass* klass = java_lang_Class::as_Klass(mirror);
  if (klass != nullptr && klass->should_be_initialized()) {
    InstanceKlass* k = InstanceKlass::cast(klass);
    k->initialize(CHECK);
  }
}
UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_ShouldBeInitialized0(JNIEnv *env, jobject unsafe, jobject clazz)) {
  assert(clazz != nullptr, "clazz must not be null");

  oop mirror = JNIHandles::resolve_non_null(clazz);
  Klass* klass = java_lang_Class::as_Klass(mirror);

  if (klass != nullptr && klass->should_be_initialized()) {
    return true;
  }

  return false;
}
UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_NotifyStrictStaticAccess0(JNIEnv *env, jobject unsafe, jobject clazz,
                                                    jlong sfoffset, jboolean writing)) {
  assert(clazz != nullptr, "clazz must not be null");

  oop mirror = JNIHandles::resolve_non_null(clazz);
  Klass* klass = java_lang_Class::as_Klass(mirror);

  if (klass != nullptr && klass->is_instance_klass()) {
    InstanceKlass* ik = InstanceKlass::cast(klass);
    fieldDescriptor fd;
    if (ik->find_local_field_from_offset((int)sfoffset, true, &fd)) {
      // Note: The Unsafe API takes an OFFSET, but the InstanceKlass wants the INDEX.
      // We could surface field indexes into Unsafe, but that's too much churn.
      ik->notify_strict_static_access(fd.index(), writing, CHECK);
      return;
    }
  }
  THROW(vmSymbols::java_lang_InternalError());
}
UNSAFE_END

static void getBaseAndScale(int& base, int& scale, jclass clazz, TRAPS) {
  assert(clazz != nullptr, "clazz must not be null");

  oop mirror = JNIHandles::resolve_non_null(clazz);
  Klass* k = java_lang_Class::as_Klass(mirror);

  if (k == nullptr || !k->is_array_klass()) {
    THROW(vmSymbols::java_lang_InvalidClassException());
  } else if (k->is_objArray_klass()) {
    base  = arrayOopDesc::base_offset_in_bytes(T_OBJECT);
    scale = heapOopSize;
  } else if (k->is_typeArray_klass()) {
    TypeArrayKlass* tak = TypeArrayKlass::cast(k);
    base  = tak->array_header_in_bytes();
    assert(base == arrayOopDesc::base_offset_in_bytes(tak->element_type()), "array_header_size semantics ok");
    scale = (1 << tak->log2_element_size());
  } else if (k->is_flatArray_klass()) {
    FlatArrayKlass* vak = FlatArrayKlass::cast(k);
    InlineKlass* vklass = vak->element_klass();
    base = vak->array_header_in_bytes();
    scale = vak->element_byte_size();
  } else {
    ShouldNotReachHere();
  }
}

UNSAFE_ENTRY(jint, Unsafe_ArrayBaseOffset0(JNIEnv *env, jobject unsafe, jclass clazz)) {
  int base = 0, scale = 0;
  getBaseAndScale(base, scale, clazz, CHECK_0);

  return field_offset_from_byte_offset(base);
} UNSAFE_END


UNSAFE_ENTRY(jint, Unsafe_ArrayIndexScale0(JNIEnv *env, jobject unsafe, jclass clazz)) {
  int base = 0, scale = 0;
  getBaseAndScale(base, scale, clazz, CHECK_0);

  // This VM packs both fields and array elements down to the byte.
  // But watch out:  If this changes, so that array references for
  // a given primitive type (say, T_BOOLEAN) use different memory units
  // than fields, this method MUST return zero for such arrays.
  // For example, the VM used to store sub-word sized fields in full
  // words in the object layout, so that accessors like getByte(Object,int)
  // did not really do what one might expect for arrays.  Therefore,
  // this function used to report a zero scale factor, so that the user
  // would know not to attempt to access sub-word array elements.
  // // Code for unpacked fields:
  // if (scale < wordSize)  return 0;

  // The following allows for a pretty general fieldOffset cookie scheme,
  // but requires it to be linear in byte offset.
  return field_offset_from_byte_offset(scale) - field_offset_from_byte_offset(0);
} UNSAFE_END


UNSAFE_ENTRY(jlong, Unsafe_GetObjectSize0(JNIEnv* env, jobject o, jobject obj))
  oop p = JNIHandles::resolve(obj);
  return p->size() * HeapWordSize;
UNSAFE_END


static inline void throw_new(JNIEnv *env, const char *ename) {
  jclass cls = env->FindClass(ename);
  if (env->ExceptionCheck()) {
    env->ExceptionClear();
    tty->print_cr("Unsafe: cannot throw %s because FindClass has failed", ename);
    return;
  }

  env->ThrowNew(cls, nullptr);
}

static jclass Unsafe_DefineClass_impl(JNIEnv *env, jstring name, jbyteArray data, int offset, int length, jobject loader, jobject pd) {
  // Code lifted from JDK 1.3 ClassLoader.c

  jbyte *body;
  char *utfName = nullptr;
  jclass result = nullptr;
  char buf[128];

  assert(data != nullptr, "Class bytes must not be null");
  assert(length >= 0, "length must not be negative: %d", length);

  if (UsePerfData) {
    ClassLoader::unsafe_defineClassCallCounter()->inc();
  }

  body = NEW_C_HEAP_ARRAY_RETURN_NULL(jbyte, length, mtInternal);
  if (body == nullptr) {
    throw_new(env, "java/lang/OutOfMemoryError");
    return nullptr;
  }

  env->GetByteArrayRegion(data, offset, length, body);
  if (env->ExceptionCheck()) {
    goto free_body;
  }

  if (name != nullptr) {
    uint len = env->GetStringUTFLength(name);
    int unicode_len = env->GetStringLength(name);

    if (len >= sizeof(buf)) {
      utfName = NEW_C_HEAP_ARRAY_RETURN_NULL(char, len + 1, mtInternal);
      if (utfName == nullptr) {
        throw_new(env, "java/lang/OutOfMemoryError");
        goto free_body;
      }
    } else {
      utfName = buf;
    }

    env->GetStringUTFRegion(name, 0, unicode_len, utfName);

    for (uint i = 0; i < len; i++) {
      if (utfName[i] == '.')   utfName[i] = '/';
    }
  }

  result = JVM_DefineClass(env, utfName, loader, body, length, pd);

  if (utfName && utfName != buf) {
    FREE_C_HEAP_ARRAY(char, utfName);
  }

 free_body:
  FREE_C_HEAP_ARRAY(jbyte, body);
  return result;
}


UNSAFE_ENTRY(jclass, Unsafe_DefineClass0(JNIEnv *env, jobject unsafe, jstring name, jbyteArray data, int offset, int length, jobject loader, jobject pd)) {
  ThreadToNativeFromVM ttnfv(thread);

  return Unsafe_DefineClass_impl(env, name, data, offset, length, loader, pd);
} UNSAFE_END


UNSAFE_ENTRY(void, Unsafe_ThrowException(JNIEnv *env, jobject unsafe, jthrowable thr)) {
  ThreadToNativeFromVM ttnfv(thread);
  env->Throw(thr);
} UNSAFE_END

// JSR166 ------------------------------------------------------------------

UNSAFE_ENTRY(jobject, Unsafe_CompareAndExchangeReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject e_h, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop e = JNIHandles::resolve(e_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop res = HeapAccess<ON_UNKNOWN_OOP_REF>::oop_atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x);
  return JNIHandles::make_local(THREAD, res);
} UNSAFE_END

UNSAFE_ENTRY_SCOPED(jint, Unsafe_CompareAndExchangeInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x)) {
  oop p = JNIHandles::resolve(obj);
  volatile jint* addr = (volatile jint*)index_oop_from_field_offset_long(p, offset);
  return Atomic::cmpxchg(addr, e, x);
} UNSAFE_END

UNSAFE_ENTRY_SCOPED(jlong, Unsafe_CompareAndExchangeLong(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jlong e, jlong x)) {
  oop p = JNIHandles::resolve(obj);
  volatile jlong* addr = (volatile jlong*)index_oop_from_field_offset_long(p, offset);
  return Atomic::cmpxchg(addr, e, x);
} UNSAFE_END

UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSetReference(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jobject e_h, jobject x_h)) {
  oop x = JNIHandles::resolve(x_h);
  oop e = JNIHandles::resolve(e_h);
  oop p = JNIHandles::resolve(obj);
  assert_field_offset_sane(p, offset);
  oop ret = HeapAccess<ON_UNKNOWN_OOP_REF>::oop_atomic_cmpxchg_at(p, (ptrdiff_t)offset, e, x);
  return ret == e;
} UNSAFE_END

UNSAFE_ENTRY_SCOPED(jboolean, Unsafe_CompareAndSetInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x)) {
  oop p = JNIHandles::resolve(obj);
  volatile jint* addr = (volatile jint*)index_oop_from_field_offset_long(p, offset);
  return Atomic::cmpxchg(addr, e, x) == e;
} UNSAFE_END

UNSAFE_ENTRY_SCOPED(jboolean, Unsafe_CompareAndSetLong(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jlong e, jlong x)) {
  oop p = JNIHandles::resolve(obj);
  volatile jlong* addr = (volatile jlong*)index_oop_from_field_offset_long(p, offset);
  return Atomic::cmpxchg(addr, e, x) == e;
} UNSAFE_END

static void post_thread_park_event(EventThreadPark* event, const oop obj, jlong timeout_nanos, jlong until_epoch_millis) {
  assert(event != nullptr, "invariant");
  event->set_parkedClass((obj != nullptr) ? obj->klass() : nullptr);
  event->set_timeout(timeout_nanos);
  event->set_until(until_epoch_millis);
  event->set_address((obj != nullptr) ? (u8)cast_from_oop<uintptr_t>(obj) : 0);
  event->commit();
}

UNSAFE_ENTRY(void, Unsafe_Park(JNIEnv *env, jobject unsafe, jboolean isAbsolute, jlong time)) {
  HOTSPOT_THREAD_PARK_BEGIN((uintptr_t) thread->parker(), (int) isAbsolute, time);
  EventThreadPark event;

  JavaThreadParkedState jtps(thread, time != 0);
  thread->parker()->park(isAbsolute != 0, time);
  if (event.should_commit()) {
    const oop obj = thread->current_park_blocker();
    if (time == 0) {
      post_thread_park_event(&event, obj, min_jlong, min_jlong);
    } else {
      if (isAbsolute != 0) {
        post_thread_park_event(&event, obj, min_jlong, time);
      } else {
        post_thread_park_event(&event, obj, time, min_jlong);
      }
    }
  }
  HOTSPOT_THREAD_PARK_END((uintptr_t) thread->parker());
} UNSAFE_END

UNSAFE_ENTRY(void, Unsafe_Unpark(JNIEnv *env, jobject unsafe, jobject jthread)) {
  if (jthread != nullptr) {
    oop thread_oop = JNIHandles::resolve_non_null(jthread);
    // Get the JavaThread* stored in the java.lang.Thread object _before_
    // the embedded ThreadsListHandle is constructed so we know if the
    // early life stage of the JavaThread* is protected. We use acquire
    // here to ensure that if we see a non-nullptr value, then we also
    // see the main ThreadsList updates from the JavaThread* being added.
    FastThreadsListHandle ftlh(thread_oop, java_lang_Thread::thread_acquire(thread_oop));
    JavaThread* thr = ftlh.protected_java_thread();
    if (thr != nullptr) {
      // The still live JavaThread* is protected by the FastThreadsListHandle
      // so it is safe to access.
      Parker* p = thr->parker();
      HOTSPOT_THREAD_UNPARK((uintptr_t) p);
      p->unpark();
    }
  } // FastThreadsListHandle is destroyed here.
} UNSAFE_END

UNSAFE_ENTRY(jint, Unsafe_GetLoadAverage0(JNIEnv *env, jobject unsafe, jdoubleArray loadavg, jint nelem)) {
  const int max_nelem = 3;
  double la[max_nelem];
  jint ret;

  typeArrayOop a = typeArrayOop(JNIHandles::resolve_non_null(loadavg));
  assert(a->is_typeArray(), "must be type array");

  ret = os::loadavg(la, nelem);
  if (ret == -1) {
    return -1;
  }

  // if successful, ret is the number of samples actually retrieved.
  assert(ret >= 0 && ret <= max_nelem, "Unexpected loadavg return value");
  switch(ret) {
    case 3: a->double_at_put(2, (jdouble)la[2]); // fall through
    case 2: a->double_at_put(1, (jdouble)la[1]); // fall through
    case 1: a->double_at_put(0, (jdouble)la[0]); break;
  }

  return ret;
} UNSAFE_END


/// JVM_RegisterUnsafeMethods

#define ADR "J"

#define LANG "Ljava/lang/"

#define OBJ LANG "Object;"
#define CLS LANG "Class;"
#define FLD LANG "reflect/Field;"
#define THR LANG "Throwable;"

#define DC_Args  LANG "String;[BII" LANG "ClassLoader;" "Ljava/security/ProtectionDomain;"
#define DAC_Args CLS "[B[" OBJ

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)

#define DECLARE_GETPUTOOP(Type, Desc) \
    {CC "get"  #Type,      CC "(" OBJ "J)" #Desc,                 FN_PTR(Unsafe_Get##Type)}, \
    {CC "put"  #Type,      CC "(" OBJ "J" #Desc ")V",             FN_PTR(Unsafe_Put##Type)}, \
    {CC "get"  #Type "Volatile",      CC "(" OBJ "J)" #Desc,      FN_PTR(Unsafe_Get##Type##Volatile)}, \
    {CC "put"  #Type "Volatile",      CC "(" OBJ "J" #Desc ")V",  FN_PTR(Unsafe_Put##Type##Volatile)}


static JNINativeMethod jdk_internal_misc_Unsafe_methods[] = {
    {CC "getReference",         CC "(" OBJ "J)" OBJ "",   FN_PTR(Unsafe_GetReference)},
    {CC "putReference",         CC "(" OBJ "J" OBJ ")V",  FN_PTR(Unsafe_PutReference)},
    {CC "getReferenceVolatile", CC "(" OBJ "J)" OBJ,      FN_PTR(Unsafe_GetReferenceVolatile)},
    {CC "putReferenceVolatile", CC "(" OBJ "J" OBJ ")V",  FN_PTR(Unsafe_PutReferenceVolatile)},

    {CC "isFlatArray",          CC "(" CLS ")Z",          FN_PTR(Unsafe_IsFlatArray)},
    {CC "isFlatField0",         CC "(" OBJ ")Z",          FN_PTR(Unsafe_IsFlatField)},
    {CC "hasNullMarker0",       CC "(" OBJ ")Z",          FN_PTR(Unsafe_HasNullMarker)},
    {CC "nullMarkerOffset0",    CC "(" OBJ ")I",          FN_PTR(Unsafe_NullMarkerOffset)},
    {CC "arrayLayout0",         CC "(" OBJ ")I",          FN_PTR(Unsafe_ArrayLayout)},
    {CC "fieldLayout0",         CC "(" OBJ ")I",          FN_PTR(Unsafe_FieldLayout)},
    {CC "newSpecialArray",      CC "(" CLS "II)[" OBJ,    FN_PTR(Unsafe_NewSpecialArray)},
    {CC "getValue",             CC "(" OBJ "J" CLS ")" OBJ, FN_PTR(Unsafe_GetValue)},
    {CC "getFlatValue",         CC "(" OBJ "JI" CLS ")" OBJ, FN_PTR(Unsafe_GetFlatValue)},
    {CC "putValue",             CC "(" OBJ "J" CLS OBJ ")V", FN_PTR(Unsafe_PutValue)},
    {CC "putFlatValue",         CC "(" OBJ "JI" CLS OBJ ")V", FN_PTR(Unsafe_PutFlatValue)},
    {CC "makePrivateBuffer",     CC "(" OBJ ")" OBJ,      FN_PTR(Unsafe_MakePrivateBuffer)},
    {CC "finishPrivateBuffer",   CC "(" OBJ ")" OBJ,      FN_PTR(Unsafe_FinishPrivateBuffer)},
    {CC "valueHeaderSize",       CC "(" CLS ")J",         FN_PTR(Unsafe_ValueHeaderSize)},

    {CC "getUncompressedObject", CC "(" ADR ")" OBJ,  FN_PTR(Unsafe_GetUncompressedObject)},

    DECLARE_GETPUTOOP(Boolean, Z),
    DECLARE_GETPUTOOP(Byte, B),
    DECLARE_GETPUTOOP(Short, S),
    DECLARE_GETPUTOOP(Char, C),
    DECLARE_GETPUTOOP(Int, I),
    DECLARE_GETPUTOOP(Long, J),
    DECLARE_GETPUTOOP(Float, F),
    DECLARE_GETPUTOOP(Double, D),

    {CC "allocateMemory0",    CC "(J)" ADR,              FN_PTR(Unsafe_AllocateMemory0)},
    {CC "reallocateMemory0",  CC "(" ADR "J)" ADR,       FN_PTR(Unsafe_ReallocateMemory0)},
    {CC "freeMemory0",        CC "(" ADR ")V",           FN_PTR(Unsafe_FreeMemory0)},

    {CC "objectFieldOffset0", CC "(" FLD ")J",           FN_PTR(Unsafe_ObjectFieldOffset0)},
    {CC "objectFieldOffset1", CC "(" CLS LANG "String;)J", FN_PTR(Unsafe_ObjectFieldOffset1)},
    {CC "staticFieldOffset0", CC "(" FLD ")J",           FN_PTR(Unsafe_StaticFieldOffset0)},
    {CC "staticFieldBase0",   CC "(" FLD ")" OBJ,        FN_PTR(Unsafe_StaticFieldBase0)},
    {CC "ensureClassInitialized0", CC "(" CLS ")V",      FN_PTR(Unsafe_EnsureClassInitialized0)},
    {CC "arrayBaseOffset0",   CC "(" CLS ")I",           FN_PTR(Unsafe_ArrayBaseOffset0)},
    {CC "arrayIndexScale0",   CC "(" CLS ")I",           FN_PTR(Unsafe_ArrayIndexScale0)},
    {CC "getObjectSize0",     CC "(Ljava/lang/Object;)J", FN_PTR(Unsafe_GetObjectSize0)},

    {CC "defineClass0",       CC "(" DC_Args ")" CLS,    FN_PTR(Unsafe_DefineClass0)},
    {CC "allocateInstance",   CC "(" CLS ")" OBJ,        FN_PTR(Unsafe_AllocateInstance)},
    {CC "throwException",     CC "(" THR ")V",           FN_PTR(Unsafe_ThrowException)},
    {CC "compareAndSetReference",CC "(" OBJ "J" OBJ "" OBJ ")Z", FN_PTR(Unsafe_CompareAndSetReference)},
    {CC "compareAndSetInt",   CC "(" OBJ "J""I""I"")Z",  FN_PTR(Unsafe_CompareAndSetInt)},
    {CC "compareAndSetLong",  CC "(" OBJ "J""J""J"")Z",  FN_PTR(Unsafe_CompareAndSetLong)},
    {CC "compareAndExchangeReference", CC "(" OBJ "J" OBJ "" OBJ ")" OBJ, FN_PTR(Unsafe_CompareAndExchangeReference)},
    {CC "compareAndExchangeInt",  CC "(" OBJ "J""I""I"")I", FN_PTR(Unsafe_CompareAndExchangeInt)},
    {CC "compareAndExchangeLong", CC "(" OBJ "J""J""J"")J", FN_PTR(Unsafe_CompareAndExchangeLong)},

    {CC "park",               CC "(ZJ)V",                FN_PTR(Unsafe_Park)},
    {CC "unpark",             CC "(" OBJ ")V",           FN_PTR(Unsafe_Unpark)},

    {CC "getLoadAverage0",    CC "([DI)I",               FN_PTR(Unsafe_GetLoadAverage0)},

    {CC "copyMemory0",        CC "(" OBJ "J" OBJ "JJ)V", FN_PTR(Unsafe_CopyMemory0)},
    {CC "copySwapMemory0",    CC "(" OBJ "J" OBJ "JJJ)V", FN_PTR(Unsafe_CopySwapMemory0)},
    {CC "writeback0",         CC "(" "J" ")V",           FN_PTR(Unsafe_WriteBack0)},
    {CC "writebackPreSync0",  CC "()V",                  FN_PTR(Unsafe_WriteBackPreSync0)},
    {CC "writebackPostSync0", CC "()V",                  FN_PTR(Unsafe_WriteBackPostSync0)},
    {CC "setMemory0",         CC "(" OBJ "JJB)V",        FN_PTR(Unsafe_SetMemory0)},

    {CC "shouldBeInitialized0", CC "(" CLS ")Z",         FN_PTR(Unsafe_ShouldBeInitialized0)},
    {CC "notifyStrictStaticAccess0", CC "(" CLS "JZ)V",  FN_PTR(Unsafe_NotifyStrictStaticAccess0)},

    {CC "fullFence",          CC "()V",                  FN_PTR(Unsafe_FullFence)},
};

#undef CC
#undef FN_PTR

#undef ADR
#undef LANG
#undef OBJ
#undef CLS
#undef FLD
#undef THR
#undef DC_Args
#undef DAC_Args

#undef DECLARE_GETPUTOOP


// This function is exported, used by NativeLookup.
// The Unsafe_xxx functions above are called only from the interpreter.
// The optimizer looks at names and signatures to recognize
// individual functions.

JVM_ENTRY(void, JVM_RegisterJDKInternalMiscUnsafeMethods(JNIEnv *env, jclass unsafeclass)) {
  ThreadToNativeFromVM ttnfv(thread);

  int ok = env->RegisterNatives(unsafeclass, jdk_internal_misc_Unsafe_methods, sizeof(jdk_internal_misc_Unsafe_methods)/sizeof(JNINativeMethod));
  guarantee(ok == 0, "register jdk.internal.misc.Unsafe natives");
} JVM_END
