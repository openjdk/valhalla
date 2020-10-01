/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1HETEROGENEOUSHEAPPOLICY_HPP
#define SHARE_GC_G1_G1HETEROGENEOUSHEAPPOLICY_HPP

#include "gc/g1/g1Policy.hpp"
#include "gc/g1/heterogeneousHeapRegionManager.hpp"

class G1HeterogeneousHeapPolicy : public G1Policy {
  // Stash a pointer to the hrm.
  HeterogeneousHeapRegionManager* _manager;

public:
  G1HeterogeneousHeapPolicy(STWGCTimer* gc_timer);

  // initialize policy
  virtual void init(G1CollectedHeap* g1h, G1CollectionSet* collection_set);
  // Record end of an evacuation pause.
  virtual void record_collection_pause_end(double pause_time_ms, bool concurrent_operation_is_full_mark);
  // Record the end of full collection.
  virtual void record_full_collection_end();

  virtual bool force_upgrade_to_full();
};
#endif // SHARE_GC_G1_G1HETEROGENEOUSHEAPPOLICY_HPP
