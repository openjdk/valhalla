/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_CI_CITYPE_HPP
#define SHARE_VM_CI_CITYPE_HPP

#include "ci/ciMetadata.hpp"

// ciType
//
// This class represents either a class (T_OBJECT), value (T_VALUETYPE),
// array (T_ARRAY), or one of the primitive types such as T_INT.
class ciType : public ciMetadata {
  CI_PACKAGE_ACCESS
  friend class ciKlass;
  friend class ciReturnAddress;
  friend class ciWrapper;

private:
  BasicType _basic_type;

  ciType(BasicType t);     // for primitive and unloaded types
  ciType(Klass* k);        // for subclasses (reference types)

  const char* type_string() { return "ciType"; }

  void print_impl(outputStream* st);

  // Distinguished instances of primitive ciTypes..
  static ciType* _basic_types[T_CONFLICT+1];

public:
  BasicType basic_type() const              { return _basic_type; }

  // Returns true iff the types are identical, or if both are klasses
  // and the is_subtype_of relation holds between the klasses.
  bool is_subtype_of(ciType* type);

  // Get the instance of java.lang.Class corresponding to this type.
  // There are mirrors for instance, array, and primitive types (incl. void).
  virtual ciInstance*    java_mirror();

  // Get the class which "boxes" (or "wraps") values of this type.
  // Example:  short is boxed by java.lang.Short, etc.
  // Returns self if it is a reference type.
  // Returns NULL for void, since null is used in such cases.
  ciKlass*  box_klass();

  // Returns true if this is not a klass or array (i.e., not a reference type).
  bool is_primitive_type() const            { return basic_type() != T_OBJECT && basic_type() != T_ARRAY && basic_type() != T_VALUETYPE; }
  int size() const                          { return type2size[basic_type()]; }
  bool is_void() const                      { return basic_type() == T_VOID; }
  bool is_one_word() const                  { return size() == 1; }
  bool is_two_word() const                  { return size() == 2; }

  // What kind of ciObject is this?
  bool is_type() const                      { return true; }
  bool is_classless() const                 { return is_primitive_type(); }

  virtual ciType*     unwrap()              { return this; }
  virtual bool is_never_null() const        { return false; }

  const char* name();
  virtual void print_name_on(outputStream* st);
  void print_name() {
    print_name_on(tty);
  }

  static ciType* make(BasicType t);
};


// ciReturnAddress
//
// This class represents the type of a specific return address in the
// bytecodes.
class ciReturnAddress : public ciType {
  CI_PACKAGE_ACCESS

private:
  // The bci of this return address.
  int _bci;

  ciReturnAddress(int bci);

  const char* type_string() { return "ciReturnAddress"; }

  void print_impl(outputStream* st);

public:
  bool is_return_address() const { return true; }

  int  bci() { return _bci; }

  static ciReturnAddress* make(int bci);
};

// ciWrapper
//
// This class wraps another type to carry additional information like nullability.
// Should only be instantiated and used by ciTypeFlow and ciSignature.
class ciWrapper : public ciType {
  CI_PACKAGE_ACCESS
  friend class ciSignature;
  friend class ciTypeFlow;

private:
  ciType* _type;
  bool _never_null;

  ciWrapper(ciType* type, bool never_null) : ciType(type->basic_type()) {
    assert(type->is_valuetype(), "should only be used for value types");
    _type = type;
    _never_null = never_null;
  }

  const char* type_string() { return "ciWrapper"; }

  void print_impl(outputStream* st) { _type->print_impl(st); }

public:
  bool equals(ciMetadata* obj) const {
    return obj->is_wrapper() &&
           obj->as_wrapper()->unwrap()->equals(_type) &&
           obj->as_wrapper()->is_never_null() == _never_null;
  }

  bool    is_wrapper() const { return true; }

  ciType*     unwrap()       { return _type; }
  bool is_never_null() const { return _never_null; }
};

#endif // SHARE_VM_CI_CITYPE_HPP
