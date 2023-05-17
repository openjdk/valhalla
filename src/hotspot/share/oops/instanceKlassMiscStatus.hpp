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

#ifndef SHARE_OOPS_INSTANCEKLASSMISCSTATUS_HPP
#define SHARE_OOPS_INSTANCEKLASSMISCSTATUS_HPP

class ClassLoaderData;

class InstanceKlassMiscStatus {
  friend class VMStructs;
  friend class JVMCIVMStructs;

#define IK_FLAGS_DO(flag)  \
    flag(rewritten                          , 1 << 0) /* methods rewritten. */ \
    flag(has_nonstatic_fields               , 1 << 1) /* for sizing with UseCompressedOops */ \
    flag(should_verify_class                , 1 << 2) /* allow caching of preverification */ \
    flag(unused                             , 1 << 3) /* not currently used */ \
    flag(is_contended                       , 1 << 4) /* marked with contended annotation */ \
    flag(has_nonstatic_concrete_methods     , 1 << 5) /* class/superclass/implemented interfaces has non-static, concrete methods */ \
    flag(declares_nonstatic_concrete_methods, 1 << 6) /* directly declares non-static, concrete methods */ \
    flag(has_been_redefined                 , 1 << 7) /* class has been redefined */ \
    flag(shared_loading_failed              , 1 << 8) /* class has been loaded from shared archive */ \
    flag(is_scratch_class                   , 1 << 9) /* class is the redefined scratch class */ \
    flag(is_shared_boot_class               , 1 << 10) /* defining class loader is boot class loader */ \
    flag(is_shared_platform_class           , 1 << 11) /* defining class loader is platform class loader */ \
    flag(is_shared_app_class                , 1 << 12) /* defining class loader is app class loader */ \
    flag(has_contended_annotations          , 1 << 13) /* has @Contended annotation */ \
    flag(has_localvariable_table            , 1 << 14) /* has localvariable information */ \
    flag(has_inline_type_fields             , 1 << 15) /* has inline fields and related embedded section is not empty */ \
    flag(is_empty_inline_type               , 1 << 16) /* empty inline type (*) */ \
    flag(is_naturally_atomic                , 1 << 17) /* loaded/stored in one instruction */ \
    flag(is_declared_atomic                 , 1 << 18) /* Listed -XX:ForceNonTearable=clist option */ \
    flag(carries_value_modifier             , 1 << 19) /* the class or one of its super types has the ACC_VALUE modifier */ \
    flag(carries_identity_modifier          , 1 << 20) /* the class or one of its super types has the ACC_IDENTITY modifier */

  /* (*) An inline type is considered empty if it contains no non-static fields or
     if it contains only empty inline fields. Note that JITs have a slightly different
     definition: empty inline fields must be flattened otherwise the container won't
     be considered empty */

#define IK_FLAGS_ENUM_NAME(name, value)    _misc_##name = value,
  enum {
    IK_FLAGS_DO(IK_FLAGS_ENUM_NAME)
  };
#undef IK_FLAGS_ENUM_NAME

  u2 shared_loader_type_bits() const {
    return _misc_is_shared_boot_class|_misc_is_shared_platform_class|_misc_is_shared_app_class;
  }

  // These flags are write-once before the class is published and then read-only so don't require atomic updates.
  u4 _flags;

 public:

  InstanceKlassMiscStatus() : _flags(0) {}

  // Create getters and setters for the flag values.
#define IK_FLAGS_GET(name, ignore)          \
  bool name() const { return (_flags & _misc_##name) != 0; }
  IK_FLAGS_DO(IK_FLAGS_GET)
#undef IK_FLAGS_GET

#define IK_FLAGS_SET(name, ignore)   \
  void set_##name(bool b) {         \
    assert_is_safe(name());         \
    if (b) _flags |= _misc_##name; \
  }
  IK_FLAGS_DO(IK_FLAGS_SET)
#undef IK_FLAGS_SET

  bool is_shared_unregistered_class() const {
    return (_flags & shared_loader_type_bits()) == 0;
  }

  void set_shared_class_loader_type(s2 loader_type);

  void assign_class_loader_type(const ClassLoaderData* cld);

  u4 flags() const { return _flags; }

  static u4 is_empty_inline_type_value() {
    return _misc_is_empty_inline_type;
  }

  void assert_is_safe(bool set) NOT_DEBUG_RETURN;
};

#endif // SHARE_OOPS_INSTANCEKLASSMISCSTATUS_HPP
