/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_BARRIERSETRUNTIME_HPP
#define SHARE_GC_SHARED_BARRIERSETRUNTIME_HPP

#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"
#include "oops/inlineKlass.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class oopDesc;
class JavaThread;

class BarrierSetRuntime: public AllStatic {
public:
  // Template interpreter...
  static void value_copy(void* src, void* dst, InlineLayoutInfo* layout_info);
  static void value_copy_is_dest_uninitialized(void* src, void* dst, InlineLayoutInfo* layout_info);
};

#endif // SHARE_GC_SHARED_BARRIERSETRUNTIME_HPP
