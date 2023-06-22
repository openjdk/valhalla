/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef CPU_AARCH64_VMSTORAGE_AARCH64_INLINE_HPP
#define CPU_AARCH64_VMSTORAGE_AARCH64_INLINE_HPP

#include <cstdint>

#include "asm/register.hpp"

// keep in sync with jdk/internal/foreign/abi/aarch64/AArch64Architecture
enum class StorageType : int8_t {
  INTEGER = 0,
  VECTOR = 1,
  STACK = 2,
  PLACEHOLDER = 3,
// special locations used only by native code
  FRAME_DATA = PLACEHOLDER + 1,
  INVALID = -1
};

// need to define this before constructing VMStorage (below)
constexpr inline bool VMStorage::is_reg(StorageType type) {
   return type == StorageType::INTEGER || type == StorageType::VECTOR;
}
constexpr inline StorageType VMStorage::stack_type() { return StorageType::STACK; }
constexpr inline StorageType VMStorage::placeholder_type() { return StorageType::PLACEHOLDER; }
constexpr inline StorageType VMStorage::frame_data_type() { return StorageType::FRAME_DATA; }

constexpr uint16_t REG64_MASK = 0b0000000000000001;
constexpr uint16_t V128_MASK  = 0b0000000000000001;

inline Register as_Register(VMStorage vms) {
  assert(vms.type() == StorageType::INTEGER, "not the right type");
  return ::as_Register(vms.index());
}

inline FloatRegister as_FloatRegister(VMStorage vms) {
  assert(vms.type() == StorageType::VECTOR, "not the right type");
  return ::as_FloatRegister(vms.index());
}

constexpr inline VMStorage as_VMStorage(Register reg) {
  return VMStorage::reg_storage(StorageType::INTEGER, REG64_MASK, reg->encoding());
}

constexpr inline VMStorage as_VMStorage(FloatRegister reg) {
  return VMStorage::reg_storage(StorageType::VECTOR, V128_MASK, reg->encoding());
}

inline VMStorage as_VMStorage(VMReg reg, BasicType bt) {
  if (reg->is_Register()) {
    return as_VMStorage(reg->as_Register());
  } else if (reg->is_FloatRegister()) {
    return as_VMStorage(reg->as_FloatRegister());
  } else if (reg->is_stack()) {
    return VMStorage::stack_storage(reg);
  } else if (!reg->is_valid()) {
    return VMStorage::invalid();
  }

  ShouldNotReachHere();
  return VMStorage::invalid();
}

#endif // CPU_AARCH64_VMSTORAGE_AARCH64_INLINE_HPP