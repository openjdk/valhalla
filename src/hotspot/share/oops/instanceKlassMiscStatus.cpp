/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "oops/instanceKlassMiscStatus.hpp"
#include "utilities/macros.hpp"

#if INCLUDE_CDS
void InstanceKlassMiscStatus::set_shared_class_loader_type(s2 loader_type) {
  switch (loader_type) {
  case ClassLoader::BOOT_LOADER:
    _flags |= _misc_is_shared_boot_class;
    break;
  case ClassLoader::PLATFORM_LOADER:
    _flags |= _misc_is_shared_platform_class;
    break;
  case ClassLoader::APP_LOADER:
    _flags |= _misc_is_shared_app_class;
    break;
  default:
    ShouldNotReachHere();
    break;
  }
}

void InstanceKlassMiscStatus::assign_class_loader_type(const ClassLoaderData* cld) {
  if (cld->is_boot_class_loader_data()) {
    set_shared_class_loader_type(ClassLoader::BOOT_LOADER);
  }
  else if (cld->is_platform_class_loader_data()) {
    set_shared_class_loader_type(ClassLoader::PLATFORM_LOADER);
  }
  else if (cld->is_system_class_loader_data()) {
    set_shared_class_loader_type(ClassLoader::APP_LOADER);
  }
}
#endif // INCLUDE_CDS
