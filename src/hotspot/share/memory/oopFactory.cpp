/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/flatArrayKlass.hpp"
#include "oops/flatArrayOop.inline.hpp"
#include "oops/flatArrayOop.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/instanceOop.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayKlass.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/utf8.hpp"

typeArrayOop oopFactory::new_boolArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::boolArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_charArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::charArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_floatArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::floatArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_doubleArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::doubleArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_byteArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::byteArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_shortArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::shortArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_intArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::intArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_longArray(int length, TRAPS) {
  return TypeArrayKlass::cast(Universe::longArrayKlassObj())->allocate(length, THREAD);
}

// create java.lang.Object[]
objArrayOop oopFactory::new_objectArray(int length, TRAPS)  {
  assert(Universe::objectArrayKlassObj() != nullptr, "Too early?");
  return ObjArrayKlass::cast(Universe::objectArrayKlassObj())->allocate(length, THREAD);
}

typeArrayOop oopFactory::new_charArray(const char* utf8_str, TRAPS) {
  int length = utf8_str == nullptr ? 0 : UTF8::unicode_length(utf8_str);
  typeArrayOop result = new_charArray(length, CHECK_NULL);
  if (length > 0) {
    UTF8::convert_to_unicode(utf8_str, result->char_at_addr(0), length);
  }
  return result;
}

typeArrayOop oopFactory::new_typeArray(BasicType type, int length, TRAPS) {
  Klass* type_asKlassOop = Universe::typeArrayKlassObj(type);
  TypeArrayKlass* type_asArrayKlass = TypeArrayKlass::cast(type_asKlassOop);
  typeArrayOop result = type_asArrayKlass->allocate(length, THREAD);
  return result;
}

// Create a Java array that points to Symbol.
// As far as Java code is concerned, a Symbol array is either an array of
// int or long depending on pointer size.  Only stack trace elements in Throwable use
// this.  They cast Symbol* into this type.
typeArrayOop oopFactory::new_symbolArray(int length, TRAPS) {
  BasicType type = LP64_ONLY(T_LONG) NOT_LP64(T_INT);
  Klass* type_asKlassOop = Universe::typeArrayKlassObj(type);
  TypeArrayKlass* type_asArrayKlass = TypeArrayKlass::cast(type_asKlassOop);
  typeArrayOop result = type_asArrayKlass->allocate(length, THREAD);
  return result;
}

typeArrayOop oopFactory::new_typeArray_nozero(BasicType type, int length, TRAPS) {
  Klass* type_asKlassOop = Universe::typeArrayKlassObj(type);
  TypeArrayKlass* type_asArrayKlass = TypeArrayKlass::cast(type_asKlassOop);
  typeArrayOop result = type_asArrayKlass->allocate_common(length, false, THREAD);
  return result;
}


objArrayOop oopFactory::new_objArray(Klass* klass, int length, TRAPS) {
  assert(klass->is_klass(), "must be instance class");
  if (klass->is_array_klass()) {
    return ArrayKlass::cast(klass)->allocate_arrayArray(1, length, THREAD);
  } else {
    return InstanceKlass::cast(klass)->allocate_objArray(1, length, THREAD);
  }
}

arrayOop oopFactory::new_valueArray(Klass* k, int length, TRAPS) {
  InlineKlass* klass = InlineKlass::cast(k);
  // Request flattened, but we might not actually get it...either way "null-free" are the aaload/aastore semantics
  Klass* array_klass = klass->value_array_klass(CHECK_NULL);
  assert(array_klass->is_null_free_array_klass(), "Expect a null-free array class here");

  arrayOop oop;
  if (array_klass->is_flatArray_klass()) {
    oop = (arrayOop) FlatArrayKlass::cast(array_klass)->allocate(length, CHECK_NULL);
    assert(oop == NULL || oop->is_flatArray(), "sanity");
    assert(oop == NULL || oop->klass()->is_flatArray_klass(), "sanity");
  } else {
    oop = (arrayOop) ObjArrayKlass::cast(array_klass)->allocate(length, CHECK_NULL);
  }
  assert(oop == NULL || oop->klass()->is_null_free_array_klass(), "sanity");
  assert(oop == NULL || oop->is_null_free_array(), "sanity");
  return oop;
}

objArrayHandle oopFactory::copy_flatArray_to_objArray(flatArrayHandle array, TRAPS) {
  int len = array->length();
  FlatArrayKlass* vak = FlatArrayKlass::cast(array->klass());
  objArrayOop oarray = new_objectArray(array->length(), CHECK_(objArrayHandle()));
  objArrayHandle oarrayh(THREAD, oarray);
  vak->copy_array(array(), 0, oarrayh(), 0, len, CHECK_(objArrayHandle()));
  return oarrayh;
}

objArrayHandle  oopFactory::ensure_objArray(oop array, TRAPS) {
  if (array != NULL && array->is_flatArray()) {
    return copy_flatArray_to_objArray(flatArrayHandle(THREAD, flatArrayOop(array)), THREAD);
  } else {
    return objArrayHandle(THREAD, objArrayOop(array));
  }
}

objArrayHandle oopFactory::new_objArray_handle(Klass* klass, int length, TRAPS) {
  objArrayOop obj = new_objArray(klass, length, CHECK_(objArrayHandle()));
  return objArrayHandle(THREAD, obj);
}
