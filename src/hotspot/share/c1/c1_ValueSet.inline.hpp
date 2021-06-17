/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_VALUESET_INLINE_HPP
#define SHARE_C1_C1_VALUESET_INLINE_HPP

#include "c1/c1_ValueSet.hpp"

#include "c1/c1_Instruction.hpp"
#include "utilities/bitMap.inline.hpp"

inline ValueSet::ValueSet() : _map(Instruction::number_of_instructions()) {
}

inline ValueSet* ValueSet::copy() {
  ValueSet* res = new ValueSet();
  res->_map.set_from(_map);
  return res;
}

inline bool ValueSet::contains(Value x) {
  return _map.at(x->id());
}

inline void ValueSet::put(Value x) {
  _map.set_bit(x->id());
}

inline void ValueSet::remove(Value x) {
  _map.clear_bit(x->id());
}

inline bool ValueSet::set_intersect(ValueSet* other) {
  return _map.set_intersection_with_result(other->_map);
}

inline void ValueSet::set_union(ValueSet* other) {
  _map.set_union(other->_map);
}

inline void ValueSet::clear() {
  _map.clear();
}

inline void ValueSet::set_from(ValueSet* other) {
  _map.set_from(other->_map);
}

inline bool ValueSet::equals(ValueSet* other) {
  return _map.is_same(other->_map);
}

#endif // SHARE_C1_C1_VALUESET_INLINE_HPP
