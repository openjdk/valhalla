/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/arrayKlass.hpp"
#include "oops/flatArrayKlass.hpp"
#include "oops/flatArrayOop.inline.hpp"
#include "oops/flatArrayOop.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/instanceOop.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "oops/refArrayKlass.hpp"
#include "oops/typeArrayKlass.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/utf8.hpp"

typeArrayOop oopFactory::new_boolArray(int length, TRAPS) {
  return Universe::boolArrayKlass()->allocate_instance(length, THREAD);
}

typeArrayOop oopFactory::new_charArray(int length, TRAPS) {
  return Universe::charArrayKlass()->allocate_instance(length, THREAD);
}

typeArrayOop oopFactory::new_floatArray(int length, TRAPS) {
  return Universe::floatArrayKlass()->allocate_instance(length, THREAD);
}

typeArrayOop oopFactory::new_doubleArray(int length, TRAPS) {
  return Universe::doubleArrayKlass()->allocate_instance(length, THREAD);
}

typeArrayOop oopFactory::new_byteArray(int length, TRAPS) {
  return Universe::byteArrayKlass()->allocate_instance(length, THREAD);
}

typeArrayOop oopFactory::new_shortArray(int length, TRAPS) {
  return Universe::shortArrayKlass()->allocate_instance(length, THREAD);
}

typeArrayOop oopFactory::new_intArray(int length, TRAPS) {
  return Universe::intArrayKlass()->allocate_instance(length, THREAD);
}

typeArrayOop oopFactory::new_longArray(int length, TRAPS) {
  return Universe::longArrayKlass()->allocate_instance(length, THREAD);
}

// create java.lang.Object[]
objArrayOop oopFactory::new_objectArray(int length, TRAPS)  {
  assert(Universe::objectArrayKlass() != nullptr, "Too early?");
  return Universe::objectArrayKlass()->allocate_instance(length, ArrayKlass::ArrayProperties::DEFAULT, THREAD);
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
  TypeArrayKlass* klass = Universe::typeArrayKlass(type);
  return klass->allocate_instance(length, THREAD);
}

// Create a Java array that points to Symbol.
// As far as Java code is concerned, a Symbol array is either an array of
// int or long depending on pointer size.  Only stack trace elements in Throwable use
// this.  They cast Symbol* into this type.
typeArrayOop oopFactory::new_symbolArray(int length, TRAPS) {
  BasicType type = LP64_ONLY(T_LONG) NOT_LP64(T_INT);
  return new_typeArray(type, length, THREAD);
}

typeArrayOop oopFactory::new_typeArray_nozero(BasicType type, int length, TRAPS) {
  TypeArrayKlass* klass = Universe::typeArrayKlass(type);
  return klass->allocate_common(length, false, THREAD);
}

objArrayOop oopFactory::new_objArray(Klass* klass, int length, ArrayKlass::ArrayProperties properties, TRAPS) {
  assert(!klass->is_array_klass() || properties == ArrayKlass::ArrayProperties::DEFAULT, "properties only apply to single dimension arrays");
  ArrayKlass* ak = klass->array_klass(CHECK_NULL);
  return ObjArrayKlass::cast(ak)->allocate_instance(length, properties, THREAD);
}

objArrayOop oopFactory::new_refArray(Klass* array_klass, int length, TRAPS) {
  RefArrayKlass* rak = RefArrayKlass::cast(array_klass);  // asserts is refArray_klass().
  return rak->allocate_instance(length, rak->properties(), THREAD);
}

objArrayOop oopFactory::new_objArray(Klass* klass, int length, TRAPS) {
  return  new_objArray(klass, length, ArrayKlass::ArrayProperties::DEFAULT, THREAD);
}

flatArrayOop oopFactory::new_flatArray(Klass* k, int length, ArrayKlass::ArrayProperties props, LayoutKind lk, TRAPS) {
  InlineKlass* klass = InlineKlass::cast(k);

  ArrayKlass* array_type = klass->array_klass(CHECK_NULL);
  ObjArrayKlass* oak = ObjArrayKlass::cast(array_type)->klass_with_properties(props, CHECK_NULL);

  assert(oak->is_flatArray_klass(), "Expected to be");
  assert(FlatArrayKlass::cast(oak)->layout_kind() == lk, "Unexpected layout kind");

  flatArrayOop oop = (flatArrayOop)FlatArrayKlass::cast(oak)->allocate_instance(length, props, CHECK_NULL);
  assert(oop == nullptr || oop->is_flatArray(), "sanity");
  assert(oop == nullptr || oop->klass()->is_flatArray_klass(), "sanity");

  return oop;
}

objArrayHandle oopFactory::new_objArray_handle(Klass* klass, int length, TRAPS) {
  objArrayOop obj = new_objArray(klass, length, CHECK_(objArrayHandle()));
  return objArrayHandle(THREAD, obj);
}
