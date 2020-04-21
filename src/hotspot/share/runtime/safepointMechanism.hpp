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

#ifndef SHARE_RUNTIME_SAFEPOINTMECHANISM_HPP
#define SHARE_RUNTIME_SAFEPOINTMECHANISM_HPP

#include "runtime/globals.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/sizes.hpp"

// This is the abstracted interface for the safepoint implementation
class SafepointMechanism : public AllStatic {
  static void* _poll_armed_value;
  static void* _poll_disarmed_value;
  static address _polling_page;

  static void* poll_armed_value()                     { return _poll_armed_value; }
  static void* poll_disarmed_value()                  { return _poll_disarmed_value; }

  static inline bool local_poll_armed(JavaThread* thread);

  static inline void disarm_local_poll(JavaThread* thread);
  static inline void disarm_local_poll_release(JavaThread* thread);

  static inline bool local_poll(Thread* thread);
  static inline bool global_poll();

  static void block_or_handshake(JavaThread *thread);
  static void block_if_requested_slow(JavaThread *thread);

  static void default_initialize();

  static void pd_initialize() NOT_AIX({ default_initialize(); });

  // By adding 8 to the base address of the protected polling page we can differentiate
  // between the armed and disarmed value by masking out this bit.
  const static intptr_t _poll_bit = 8;
public:
  static intptr_t poll_bit() { return _poll_bit; }

  static address get_polling_page()             { return _polling_page; }
  static bool    is_poll_address(address addr)  { return addr >= _polling_page && addr < (_polling_page + os::vm_page_size()); }

  // Call this method to see if this thread should block for a safepoint or process handshake.
  static inline bool should_block(Thread* thread);

  // Blocks a thread until safepoint/handshake is completed.
  static inline void block_if_requested(JavaThread* thread);

  // Caller is responsible for using a memory barrier if needed.
  static inline void arm_local_poll(JavaThread* thread);
  // Release semantics
  static inline void arm_local_poll_release(JavaThread* thread);
  // Optional release
  static inline void disarm_if_needed(JavaThread* thread, bool memory_order_release);

  // Setup the selected safepoint mechanism
  static void initialize();
  static void initialize_header(JavaThread* thread);
};

#endif // SHARE_RUNTIME_SAFEPOINTMECHANISM_HPP
