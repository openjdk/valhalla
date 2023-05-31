/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZFORWARDINGTABLE_INLINE_HPP
#define SHARE_GC_Z_ZFORWARDINGTABLE_INLINE_HPP

#include "gc/z/zForwardingTable.hpp"

#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zForwarding.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zGranuleMap.inline.hpp"
#include "gc/z/zIndexDistributor.inline.hpp"
#include "utilities/debug.hpp"

inline ZForwardingTable::ZForwardingTable()
  : _map(ZAddressOffsetMax) {}

inline ZForwarding* ZForwardingTable::at(size_t index) const {
  return _map.at(index);
}

inline ZForwarding* ZForwardingTable::get(zaddress_unsafe addr) const {
  assert(!is_null(addr), "Invalid address");
  return _map.get(ZAddress::offset(addr));
}

inline void ZForwardingTable::insert(ZForwarding* forwarding) {
  const zoffset offset = forwarding->start();
  const size_t size = forwarding->size();

  assert(_map.get(offset) == nullptr, "Invalid entry");
  _map.put(offset, size, forwarding);
}

inline void ZForwardingTable::remove(ZForwarding* forwarding) {
  const zoffset offset = forwarding->start();
  const size_t size = forwarding->size();

  assert(_map.get(offset) == forwarding, "Invalid entry");
  _map.put(offset, size, nullptr);
}

#endif // SHARE_GC_Z_ZFORWARDINGTABLE_INLINE_HPP
