/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_X_XGRANULEMAP_HPP
#define SHARE_GC_X_XGRANULEMAP_HPP

#include "gc/x/xArray.hpp"
#include "memory/allocation.hpp"

class VMStructs;

template <typename T>
class XGranuleMap {
  friend class ::VMStructs;
  template <typename> friend class XGranuleMapIterator;

private:
  const size_t _size;
  T* const     _map;

  size_t index_for_offset(uintptr_t offset) const;

public:
  XGranuleMap(size_t max_offset);
  ~XGranuleMap();

  T get(uintptr_t offset) const;
  void put(uintptr_t offset, T value);
  void put(uintptr_t offset, size_t size, T value);

  T get_acquire(uintptr_t offset) const;
  void release_put(uintptr_t offset, T value);
};

template <typename T>
class XGranuleMapIterator : public XArrayIteratorImpl<T, false /* Parallel */> {
public:
  XGranuleMapIterator(const XGranuleMap<T>* granule_map);
};

#endif // SHARE_GC_X_XGRANULEMAP_HPP
