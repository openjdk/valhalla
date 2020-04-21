/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/sampling/objectSample.hpp"
#include "oops/access.inline.hpp"

const oop ObjectSample::object() const {
  return NativeAccess<ON_PHANTOM_OOP_REF | AS_NO_KEEPALIVE>::oop_load(&_object);
}

const oop ObjectSample::object_raw() const {
  return RawAccess<>::oop_load(&_object);
}

void ObjectSample::set_object(oop object) {
  NativeAccess<ON_PHANTOM_OOP_REF>::oop_store(&_object, object);
}
