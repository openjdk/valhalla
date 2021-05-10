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

#ifndef SHARE_VM_OOPS_FLATARRAYKLASS_HPP
#define SHARE_VM_OOPS_FLATARRAYKLASS_HPP

#include "classfile/classLoaderData.hpp"
#include "oops/arrayKlass.hpp"
#include "oops/inlineKlass.hpp"
#include "utilities/macros.hpp"

/**
 * Array of inline types, gives a layout of typeArrayOop, but needs oops iterators
 */
class FlatArrayKlass : public ArrayKlass {
  friend class VMStructs;

 public:
  static const KlassID ID = FlatArrayKlassID;

 private:
  // Constructor
  FlatArrayKlass(Klass* element_klass, Symbol* name);

 public:

  FlatArrayKlass() {}

  // Returns the ObjArrayKlass for n'th dimension.
  virtual Klass* array_klass(int n, TRAPS);
  virtual Klass* array_klass_or_null(int n);

  // Returns the array class with this class as element type.
  virtual Klass* array_klass(TRAPS);
  virtual Klass* array_klass_or_null();

  virtual InlineKlass* element_klass() const;
  virtual void set_element_klass(Klass* k);

  // Casting from Klass*
  static FlatArrayKlass* cast(Klass* k) {
    assert(k->is_flatArray_klass(), "cast to FlatArrayKlass");
    return (FlatArrayKlass*) k;
  }

  // klass allocation
  static FlatArrayKlass* allocate_klass(Klass* element_klass, TRAPS);

  void initialize(TRAPS);

  ModuleEntry* module() const;
  PackageEntry* package() const;

  bool can_be_primary_super_slow() const;
  GrowableArray<Klass*>* compute_secondary_supers(int num_extra_slots,
                                                  Array<InstanceKlass*>* transitive_interfaces);

  int element_byte_size() const { return 1 << layout_helper_log2_element_size(_layout_helper); }

  bool is_flatArray_klass_slow() const { return true; }

  bool contains_oops() {
    return element_klass()->contains_oops();
  }

  // Override.
  bool element_access_is_atomic() {
    return element_klass()->is_atomic();
  }

  oop protection_domain() const;

  static jint array_layout_helper(InlineKlass* vklass); // layout helper for values

  // sizing
  static int header_size()  { return sizeof(FlatArrayKlass)/HeapWordSize; }
  int size() const          { return ArrayKlass::static_size(header_size()); }

  jint max_elements() const;

  int oop_size(oop obj) const;

  // Oop Allocation
  flatArrayOop allocate(int length, TRAPS);
  oop multi_allocate(int rank, jint* sizes, TRAPS);

  // Naming
  const char* internal_name() const { return external_name(); }

  // Copying
  void copy_array(arrayOop s, int src_pos, arrayOop d, int dst_pos, int length, TRAPS);

  // GC specific object visitors
  //
  // Mark Sweep
  int oop_ms_adjust_pointers(oop obj);


  template <typename T, typename OopClosureType>
  inline void oop_oop_iterate(oop obj, OopClosureType* closure);

  template <typename T, typename OopClosureType>
  inline void oop_oop_iterate_reverse(oop obj, OopClosureType* closure);

  template <typename T, typename OopClosureType>
  inline void oop_oop_iterate_bounded(oop obj, OopClosureType* closure, MemRegion mr);

  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_elements(flatArrayOop a, OopClosureType* closure);

private:
  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_elements_specialized(flatArrayOop a, OopClosureType* closure);

  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_elements_bounded(flatArrayOop a, OopClosureType* closure, MemRegion mr);

  template <typename T, class OopClosureType>
  inline void oop_oop_iterate_elements_specialized_bounded(flatArrayOop a, OopClosureType* closure, void* low, void* high);

 public:
  // Printing
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;

  void oop_print_value_on(oop obj, outputStream* st);
#ifndef PRODUCT
  void oop_print_on(oop obj, outputStream* st);
#endif

  // Verification
  void verify_on(outputStream* st);
  void oop_verify_on(oop obj, outputStream* st);
};

#endif
