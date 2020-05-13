/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_ARRAYSTORAGEPROPERTIES_HPP
#define SHARE_OOPS_ARRAYSTORAGEPROPERTIES_HPP

#include "oops/symbol.hpp"
#include "runtime/globals.hpp"

class ArrayStorageProperties {
 private:
  uint8_t _flags;

  void clear_flags_bits(uint8_t value) { _flags &= (~value); }
  void set_flags_bits(uint8_t value) { _flags |= value; }
  bool test_flags_bit(int idx) const { return (_flags & (1 << idx)) != 0; }
 public:

  enum {
    empty_value           = 0,
    flattened_bit         = 0,
    flattened_value       = 1 <<  flattened_bit,
    null_free_bit = flattened_bit + 1,
    null_free_value = 1 << null_free_bit,
    nof_oop_properties = null_free_bit + 1
  };

  ArrayStorageProperties() : _flags(empty_value) {};
  ArrayStorageProperties(uint8_t flags): _flags(flags) {};

  bool is_empty() const { return _flags == empty_value; }

  void clear_flattened()    { clear_flags_bits(flattened_value); }

  bool is_flattened() const { return test_flags_bit(flattened_bit); }
  void set_flattened()      { set_flags_bits(flattened_value); }

  bool is_null_free() const { return test_flags_bit(null_free_bit); }
  void set_null_free()      { set_flags_bits(null_free_value); }

  uint8_t value() const { return _flags; }
  template <typename T> T encode(int shift) const { return static_cast<T>(_flags) << shift; }

  // Well-known constants...
  static const ArrayStorageProperties empty;
  static const ArrayStorageProperties flattened;
  static const ArrayStorageProperties null_free;
  static const ArrayStorageProperties flattened_and_null_free;

  static ArrayStorageProperties for_signature(Symbol* sig) {
    return (sig->is_Q_array_signature() || sig->is_Q_signature()) ?
      ArrayStorageProperties::flattened_and_null_free : ArrayStorageProperties::empty;
  }
};


#endif //SHARE_OOPS_ARRAYSTORAGEPROPERTIES_HPP
