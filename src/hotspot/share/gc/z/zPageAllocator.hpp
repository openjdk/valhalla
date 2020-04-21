/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZPAGEALLOCATOR_HPP
#define SHARE_GC_Z_ZPAGEALLOCATOR_HPP

#include "gc/z/zAllocationFlags.hpp"
#include "gc/z/zList.hpp"
#include "gc/z/zLock.hpp"
#include "gc/z/zPageCache.hpp"
#include "gc/z/zPhysicalMemory.hpp"
#include "gc/z/zSafeDelete.hpp"
#include "gc/z/zVirtualMemory.hpp"
#include "memory/allocation.hpp"

class ZPageAllocRequest;
class ZWorkers;

class ZPageAllocator {
  friend class VMStructs;

private:
  ZLock                      _lock;
  ZVirtualMemoryManager      _virtual;
  ZPhysicalMemoryManager     _physical;
  ZPageCache                 _cache;
  const size_t               _min_capacity;
  const size_t               _max_capacity;
  const size_t               _max_reserve;
  size_t                     _current_max_capacity;
  size_t                     _capacity;
  size_t                     _used_high;
  size_t                     _used_low;
  size_t                     _used;
  size_t                     _allocated;
  ssize_t                    _reclaimed;
  ZList<ZPageAllocRequest>   _queue;
  ZList<ZPageAllocRequest>   _satisfied;
  mutable ZSafeDelete<ZPage> _safe_delete;
  bool                       _uncommit;
  bool                       _initialized;

  static ZPage* const gc_marker;

  void prime_cache(ZWorkers* workers, size_t size);

  void increase_used(size_t size, bool relocation);
  void decrease_used(size_t size, bool reclaimed);

  ZPage* create_page(uint8_t type, size_t size);
  void destroy_page(ZPage* page);

  size_t max_available(bool no_reserve) const;
  bool ensure_available(size_t size, bool no_reserve);
  void ensure_uncached_available(size_t size);

  void check_out_of_memory_during_initialization();

  ZPage* alloc_page_common_inner(uint8_t type, size_t size, bool no_reserve);
  ZPage* alloc_page_common(uint8_t type, size_t size, ZAllocationFlags flags);
  ZPage* alloc_page_blocking(uint8_t type, size_t size, ZAllocationFlags flags);
  ZPage* alloc_page_nonblocking(uint8_t type, size_t size, ZAllocationFlags flags);

  size_t flush_cache(ZPageCacheFlushClosure* cl, bool for_allocation);
  void flush_cache_for_allocation(size_t requested);

  void satisfy_alloc_queue();

public:
  ZPageAllocator(ZWorkers* workers,
                 size_t min_capacity,
                 size_t initial_capacity,
                 size_t max_capacity,
                 size_t max_reserve);

  bool is_initialized() const;

  size_t min_capacity() const;
  size_t max_capacity() const;
  size_t soft_max_capacity() const;
  size_t capacity() const;
  size_t max_reserve() const;
  size_t used_high() const;
  size_t used_low() const;
  size_t used() const;
  size_t unused() const;
  size_t allocated() const;
  size_t reclaimed() const;

  void reset_statistics();

  ZPage* alloc_page(uint8_t type, size_t size, ZAllocationFlags flags);
  void free_page(ZPage* page, bool reclaimed);

  uint64_t uncommit(uint64_t delay);

  void enable_deferred_delete() const;
  void disable_deferred_delete() const;

  void map_page(const ZPage* page) const;

  void debug_map_page(const ZPage* page) const;
  void debug_unmap_page(const ZPage* page) const;

  bool is_alloc_stalled() const;
  void check_out_of_memory();

  void pages_do(ZPageClosure* cl) const;
};

#endif // SHARE_GC_Z_ZPAGEALLOCATOR_HPP
