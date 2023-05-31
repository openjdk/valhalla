/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_X_XMARKCACHE_INLINE_HPP
#define SHARE_GC_X_XMARKCACHE_INLINE_HPP

#include "gc/x/xMarkCache.hpp"

#include "gc/x/xPage.inline.hpp"

inline void XMarkCacheEntry::inc_live(XPage* page, size_t bytes) {
  if (_page == page) {
    // Cache hit
    _objects++;
    _bytes += bytes;
  } else {
    // Cache miss
    evict();
    _page = page;
    _objects = 1;
    _bytes = bytes;
  }
}

inline void XMarkCacheEntry::evict() {
  if (_page != nullptr) {
    // Write cached data out to page
    _page->inc_live(_objects, _bytes);
    _page = nullptr;
  }
}

inline void XMarkCache::inc_live(XPage* page, size_t bytes) {
  const size_t mask = XMarkCacheSize - 1;
  const size_t index = (page->start() >> _shift) & mask;
  _cache[index].inc_live(page, bytes);
}

#endif // SHARE_GC_X_XMARKCACHE_INLINE_HPP
