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
 *
 */

#include "precompiled.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "services/memTracker.hpp"
#include "utilities/globalDefinitions.hpp"

void* SafepointMechanism::_poll_armed_value;
void* SafepointMechanism::_poll_disarmed_value;
address SafepointMechanism::_polling_page;

void SafepointMechanism::default_initialize() {
  // Poll bit values
  intptr_t poll_armed_value = poll_bit();
  intptr_t poll_disarmed_value = 0;

#ifdef USE_POLL_BIT_ONLY
  if (!USE_POLL_BIT_ONLY)
#endif
  {
    // Polling page
    const size_t page_size = os::vm_page_size();
    const size_t allocation_size = 2 * page_size;
    char* polling_page = os::reserve_memory(allocation_size, NULL, page_size);
    os::commit_memory_or_exit(polling_page, allocation_size, false, "Unable to commit Safepoint polling page");
    MemTracker::record_virtual_memory_type((address)polling_page, mtSafepoint);

    char* bad_page  = polling_page;
    char* good_page = polling_page + page_size;

    os::protect_memory(bad_page,  page_size, os::MEM_PROT_NONE);
    os::protect_memory(good_page, page_size, os::MEM_PROT_READ);

    log_info(os)("SafePoint Polling address, bad (protected) page:" INTPTR_FORMAT ", good (unprotected) page:" INTPTR_FORMAT, p2i(bad_page), p2i(good_page));
    _polling_page = (address)(bad_page);

    // Poll address values
    intptr_t bad_page_val  = reinterpret_cast<intptr_t>(bad_page),
             good_page_val = reinterpret_cast<intptr_t>(good_page);
    poll_armed_value    |= bad_page_val;
    poll_disarmed_value |= good_page_val;
  }

  _poll_armed_value    = reinterpret_cast<void*>(poll_armed_value);
  _poll_disarmed_value = reinterpret_cast<void*>(poll_disarmed_value);
}

void SafepointMechanism::block_or_handshake(JavaThread *thread) {
  if (global_poll()) {
    // Any load in ::block must not pass the global poll load.
    // Otherwise we might load an old safepoint counter (for example).
    OrderAccess::loadload();
    SafepointSynchronize::block(thread);
  }
  if (thread->has_handshake()) {
    thread->handshake_process_by_self();
  }
}

void SafepointMechanism::block_if_requested_slow(JavaThread *thread) {
  // Read global poll and has_handshake after local poll
  OrderAccess::loadload();

  // local poll already checked, if used.
  block_or_handshake(thread);

  OrderAccess::loadload();

  if (local_poll_armed(thread)) {
    disarm_local_poll_release(thread);
    // We might have disarmed next safepoint/handshake
    OrderAccess::storeload();
    if (global_poll() || thread->has_handshake()) {
      arm_local_poll(thread);
    }
  }

  OrderAccess::cross_modify_fence();
}

void SafepointMechanism::initialize_header(JavaThread* thread) {
  disarm_local_poll(thread);
}

void SafepointMechanism::initialize() {
  pd_initialize();
}
