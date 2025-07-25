/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "cds/cdsConfig.hpp"
#include "classfile/classFileParser.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "oops/instanceRefKlass.inline.hpp"
#include "oops/oop.inline.hpp"

InstanceRefKlass::InstanceRefKlass() {
  assert(CDSConfig::is_dumping_static_archive() || CDSConfig::is_using_archive(), "only for CDS");
}

static ReferenceType reference_subclass_name_to_type(const Symbol* name) {
  if (       name == vmSymbols::java_lang_ref_SoftReference()) {
    return REF_SOFT;
  } else if (name == vmSymbols::java_lang_ref_WeakReference()) {
    return REF_WEAK;
  } else if (name == vmSymbols::java_lang_ref_FinalReference()) {
    return REF_FINAL;
  } else if (name == vmSymbols::java_lang_ref_PhantomReference()) {
    return REF_PHANTOM;
  } else {
    ShouldNotReachHere();
    return REF_NONE;
  }
}

static ReferenceType determine_reference_type(const ClassFileParser& parser) {
  const ReferenceType rt = parser.super_reference_type();
  if (rt != REF_NONE) {
    // Inherit type from super class
    return rt;
  }

  // Bootstrapping: this is one of the direct subclasses of java.lang.ref.Reference
  const Symbol* const name = parser.class_name();
  return reference_subclass_name_to_type(name);
}

InstanceRefKlass::InstanceRefKlass(const ClassFileParser& parser)
  : InstanceKlass(parser, Kind, markWord::prototype(), determine_reference_type(parser)) {}

void InstanceRefKlass::update_nonstatic_oop_maps(Klass* k) {
  // Clear the nonstatic oop-map entries corresponding to referent
  // and discovered fields.  They are treated specially by the
  // garbage collector.
  InstanceKlass* ik = InstanceKlass::cast(k);

  // Check that we have the right class
  DEBUG_ONLY(static bool first_time = true);
  assert(k == vmClasses::Reference_klass() && first_time,
         "Invalid update of maps");
  DEBUG_ONLY(first_time = false);
  assert(ik->nonstatic_oop_map_count() == 1, "just checking");

  OopMapBlock* map = ik->start_of_nonstatic_oop_maps();

#ifdef ASSERT
  // Verify fields are in the expected places.
  int referent_offset = java_lang_ref_Reference::referent_offset();
  int queue_offset = java_lang_ref_Reference::queue_offset();
  int next_offset = java_lang_ref_Reference::next_offset();
  int discovered_offset = java_lang_ref_Reference::discovered_offset();
  assert(referent_offset < queue_offset, "just checking");
  assert(queue_offset < next_offset, "just checking");
  assert(next_offset < discovered_offset, "just checking");
  const unsigned int count =
    1 + ((discovered_offset - referent_offset) / heapOopSize);
  assert(count == 4, "just checking");
#endif // ASSERT

  // Updated map starts at "queue", covers "queue" and "next".
  const int new_offset = java_lang_ref_Reference::queue_offset();
  const unsigned int new_count = 2; // queue and next

  // Verify existing map is as expected, and update if needed.
  if (CDSConfig::is_using_archive()) {
    assert(map->offset() == new_offset, "just checking");
    assert(map->count() == new_count, "just checking");
  } else {
    assert(map->offset() == referent_offset, "just checking");
    assert(map->count() == count, "just checking");
    map->set_offset(new_offset);
    map->set_count(new_count);
  }
}


// Verification

void InstanceRefKlass::oop_verify_on(oop obj, outputStream* st) {
  InstanceKlass::oop_verify_on(obj, st);
  // Verify referent field
  oop referent = java_lang_ref_Reference::unknown_referent_no_keepalive(obj);
  if (referent != nullptr) {
    guarantee(oopDesc::is_oop(referent), "referent field heap failed");
  }
  // Additional verification for next field, which must be a Reference or null
  oop next = java_lang_ref_Reference::next(obj);
  if (next != nullptr) {
    guarantee(oopDesc::is_oop(next), "next field should be an oop");
    guarantee(next->is_instanceRef(), "next field verify failed");
  }
}
