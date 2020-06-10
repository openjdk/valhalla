/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/shared/gcLogPrecious.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"

stringStream* GCLogPrecious::_lines = NULL;
stringStream* GCLogPrecious::_temp = NULL;
Mutex* GCLogPrecious::_lock = NULL;

void GCLogPrecious::initialize() {
  _lines = new (ResourceObj::C_HEAP, mtGC) stringStream();
  _temp = new (ResourceObj::C_HEAP, mtGC) stringStream();
  _lock = new Mutex(Mutex::tty,
                    "GCLogPrecious Lock",
                    true,
                    Mutex::_safepoint_check_never);
}

void GCLogPrecious::vwrite_inner(LogTargetHandle log, const char* format, va_list args) {
  // Generate the string in the temp buffer
  _temp->reset();
  _temp->vprint(format, args);

  // Save it in the precious lines buffer
  _lines->print_cr(" %s", _temp->base());

  // Log it to UL
  log.print("%s", _temp->base());
}

void GCLogPrecious::vwrite(LogTargetHandle log, const char* format, va_list args) {
  MutexLocker locker(_lock, Mutex::_no_safepoint_check_flag);
  vwrite_inner(log, format, args);
}

void GCLogPrecious::print_on_error(outputStream* st) {
  if (_lines != NULL) {
    MutexLocker locker(_lock, Mutex::_no_safepoint_check_flag);
    if (_lines->size() > 0) {
      st->print_cr("GC Precious Log:");
      st->print_cr("%s", _lines->base());
    }
  }
}
