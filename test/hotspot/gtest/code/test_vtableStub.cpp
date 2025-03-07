/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "code/vtableStubs.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "unittest.hpp"

#ifndef ZERO

TEST_VM(code, vtableStubs) {
  // Should be in VM to use locks
  ThreadInVMfromNative ThreadInVMfromNative(JavaThread::current());

  VtableStubs::find_vtable_stub(0, false); // min vtable index
  for (int i = 0; i < 15; i++) {
    VtableStubs::find_vtable_stub((1 << i) - 1, false);
    VtableStubs::find_vtable_stub((1 << i), false);
  }
  VtableStubs::find_vtable_stub((1 << 15) - 1, false); // max vtable index
}

TEST_VM(code, itableStubs) {
  // Should be in VM to use locks
  ThreadInVMfromNative ThreadInVMfromNative(JavaThread::current());

  VtableStubs::find_itable_stub(0, false); // min itable index
  for (int i = 0; i < 15; i++) {
    VtableStubs::find_itable_stub((1 << i) - 1, false);
    VtableStubs::find_itable_stub((1 << i), false);
  }
  VtableStubs::find_itable_stub((1 << 15) - 1, false); // max itable index
}

#endif
