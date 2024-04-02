/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_FIELDLAYOUTBUILDER_HPP
#define SHARE_CLASSFILE_FIELDLAYOUTBUILDER_HPP

#include "classfile/classFileParser.hpp"
#include "classfile/classLoaderData.hpp"
#include "memory/allocation.hpp"
#include "oops/fieldStreams.hpp"
#include "utilities/growableArray.hpp"

// Classes below are used to compute the field layout of classes.

// A LayoutRawBlock describes an element of a layout.
// Each field is represented by a LayoutRawBlock.
// LayoutRawBlocks can also represent elements injected by the JVM:
// padding, empty blocks, inherited fields, etc.
// All LayoutRawBlocks must have a size and an alignment. The size is the
// exact size of the field expressed in bytes. The alignment is
// the alignment constraint of the field (1 for byte, 2 for short,
// 4 for int, 8 for long, etc.)
//
// LayoutRawBlock are designed to be used in two data structures:
//   - a linked list in a layout (using _next_block, _prev_block)
//   - a GrowableArray in field group (the growable array contains pointers to LayoutRawBlocks)
//
//  next/prev pointers are included in the LayoutRawBlock class to narrow
//  the number of allocation required during the computation of a layout.
//
class LayoutRawBlock : public ResourceObj {
 public:
  // Some code relies on the order of values below.
  enum Kind {
    EMPTY,            // empty slot, space is taken from this to allocate fields
    RESERVED,         // reserved for JVM usage (for instance object header)
    PADDING,          // padding (because of alignment constraints or @Contended)
    REGULAR,          // primitive or oop field (including not flat inline type fields)
    FLAT,             // flat field
    INHERITED,        // field(s) inherited from super classes
    NULL_MARKER       // stores the null marker for a flat field
  };

 private:
  LayoutRawBlock* _next_block;
  LayoutRawBlock* _prev_block;
  InlineKlass* _inline_klass;
  Kind _kind;
  int _offset;
  int _alignment;
  int _size;
  int _field_index;
  int _null_marker_offset;
  bool _is_reference;
  bool _needs_null_marker;

 public:
  LayoutRawBlock(Kind kind, int size);

  LayoutRawBlock(int index, Kind kind, int size, int alignment, bool is_reference = false);
  LayoutRawBlock* next_block() const { return _next_block; }
  void set_next_block(LayoutRawBlock* next) { _next_block = next; }
  LayoutRawBlock* prev_block() const { return _prev_block; }
  void set_prev_block(LayoutRawBlock* prev) { _prev_block = prev; }
  Kind kind() const { return _kind; }
  int offset() const {
    assert(_offset >= 0, "Must be initialized");
    return _offset;
  }
  void set_offset(int offset) { _offset = offset; }
  int alignment() const { return _alignment; }
  int size() const { return _size; }
  void set_size(int size) { _size = size; }
  int field_index() const {
    assert(_field_index != -1, "Must be initialized");
    return _field_index;
  }
  void set_field_index(int field_index) {
    assert(_field_index == -1, "Must not be initialized");
    _field_index = field_index;
  }
  bool is_reference() const { return _is_reference; }
  InlineKlass* inline_klass() const {
    assert(_inline_klass != nullptr, "Must be initialized");
    return _inline_klass;
  }
  void set_inline_klass(InlineKlass* inline_klass) { _inline_klass = inline_klass; }
  void set_needs_null_marker() { _needs_null_marker = true; }
  bool needs_null_marker() const { return _needs_null_marker; }
  void set_null_marker_offset(int offset) {
    assert(_needs_null_marker, "");
    _null_marker_offset = offset;
    _needs_null_marker = false;
  }
  int null_marker_offset() const { return _null_marker_offset; }

  bool fit(int size, int alignment);

  static int compare_offset(LayoutRawBlock** x, LayoutRawBlock** y)  { return (*x)->offset() - (*y)->offset(); }
  // compare_size_inverted() returns the opposite of a regular compare method in order to
  // sort fields in decreasing order.
  // Note: with line types, the comparison should include alignment constraint if sizes are equals
  static int compare_size_inverted(LayoutRawBlock** x, LayoutRawBlock** y)  {
#ifdef _WINDOWS
    // qsort() on Windows reverse the order of fields with the same size
    // the extension of the comparison function below preserves this order
    int diff = (*y)->size() - (*x)->size();
    if (diff == 0) {
      diff = (*x)->field_index() - (*y)->field_index();
    }
    return diff;
#else
    return (*y)->size() - (*x)->size();
#endif // _WINDOWS
  }
};

// A Field group represents a set of fields that have to be allocated together,
// this is the way the @Contended annotation is supported.
// Inside a FieldGroup, fields are sorted based on their kind: primitive,
// oop, or flat.
//
class FieldGroup : public ResourceObj {

 private:
  FieldGroup* _next;

  GrowableArray<LayoutRawBlock*>* _small_primitive_fields;
  GrowableArray<LayoutRawBlock*>* _big_primitive_fields;
  GrowableArray<LayoutRawBlock*>* _oop_fields;
  int _contended_group;
  int _oop_count;
  static const int INITIAL_LIST_SIZE = 16;

 public:
  FieldGroup(int contended_group = -1);

  FieldGroup* next() const { return _next; }
  void set_next(FieldGroup* next) { _next = next; }
  GrowableArray<LayoutRawBlock*>* small_primitive_fields() const { return _small_primitive_fields; }
  GrowableArray<LayoutRawBlock*>* big_primitive_fields() const { return _big_primitive_fields; }
  GrowableArray<LayoutRawBlock*>* oop_fields() const { return _oop_fields; }
  int contended_group() const { return _contended_group; }
  int oop_count() const { return _oop_count; }

  void add_primitive_field(int idx, BasicType type);
  void add_oop_field(int idx);
  void add_flat_field(int idx, InlineKlass* vk, bool needs_null_marker);
  void add_block(LayoutRawBlock** list, LayoutRawBlock* block);
  void sort_by_size();
 private:
  void add_to_small_primitive_list(LayoutRawBlock* block);
  void add_to_big_primitive_list(LayoutRawBlock* block);
};

// The FieldLayout class represents a set of fields organized
// in a layout.
// An instance of FieldLayout can either represent the layout
// of non-static fields (used in an instance object) or the
// layout of static fields (to be included in the class mirror).
//
// _block is a pointer to a list of LayoutRawBlock ordered by increasing
// offsets.
// _start points to the LayoutRawBlock with the first offset that can
// be used to allocate fields of the current class
// _last points to the last LayoutRawBlock of the list. In order to
// simplify the code, the LayoutRawBlock list always ends with an
// EMPTY block (the kind of LayoutRawBlock from which space is taken
// to allocate fields) with a size big enough to satisfy all
// field allocations.
//
class FieldLayout : public ResourceObj {
 private:
  GrowableArray<FieldInfo>* _field_info;
  ConstantPool* _cp;
  LayoutRawBlock* _blocks;  // the layout being computed
  LayoutRawBlock* _start;   // points to the first block where a field can be inserted
  LayoutRawBlock* _last;    // points to the last block of the layout (big empty block)
  bool _has_missing_null_markers;

 public:
  FieldLayout(GrowableArray<FieldInfo>* field_info, ConstantPool* cp);
  void initialize_static_layout();
  void initialize_instance_layout(const InstanceKlass* ik);

  LayoutRawBlock* first_empty_block() {
    LayoutRawBlock* block = _start;
    while (block->kind() != LayoutRawBlock::EMPTY) {
      block = block->next_block();
    }
    return block;
  }

  LayoutRawBlock* blocks() const { return _blocks; }

  LayoutRawBlock* start() const { return _start; }
  void set_start(LayoutRawBlock* start) { _start = start; }
  LayoutRawBlock* last_block() const { return _last; }
  bool has_missing_null_markers() const { return _has_missing_null_markers; }

  LayoutRawBlock* first_field_block();
  void add(GrowableArray<LayoutRawBlock*>* list, LayoutRawBlock* start = nullptr);
  void add_field_at_offset(LayoutRawBlock* blocks, int offset, LayoutRawBlock* start = nullptr);
  void add_contiguously(GrowableArray<LayoutRawBlock*>* list, LayoutRawBlock* start = nullptr);
  LayoutRawBlock* insert_field_block(LayoutRawBlock* slot, LayoutRawBlock* block);
  bool reconstruct_layout(const InstanceKlass* ik);
  void fill_holes(const InstanceKlass* ik);
  LayoutRawBlock* insert(LayoutRawBlock* slot, LayoutRawBlock* block);
  void remove(LayoutRawBlock* block);
  void print(outputStream* output, bool is_static, const InstanceKlass* super, Array<InlineKlass*>* inline_fields);
};


// FieldLayoutBuilder is the main entry point for layout computation.
// This class has two methods to generate layout: one for identity classes
// and one for inline classes. The rational for having two methods
// is that each kind of classes has a different set goals regarding
// its layout, so instead of mixing two layout strategies into a
// single method, each kind has its own method (see comments below
// for more details about the allocation strategies).
//
// Computing the layout of a class always goes through 4 steps:
//   1 - Prologue: preparation of data structure and gathering of
//       layout information inherited from super classes
//   2 - Field sorting: fields are sorted according to their
//       kind (oop, primitive, inline class) and their contention
//       annotation (if any)
//   3 - Layout is computed from the set of lists generated during
//       step 2
//   4 - Epilogue: oopmaps are generated, layout information is
//       prepared so other VM components can use it (instance size,
//       static field size, non-static field size, etc.)
//
//  Steps 1 and 4 are common to all layout computations. Step 2 and 3
//  differ for inline classes and identity classes.
//
class FieldLayoutBuilder : public ResourceObj {
 private:
  const Symbol* _classname;
  ClassLoaderData* _loader_data;
  const InstanceKlass* _super_klass;
  ConstantPool* _constant_pool;
  GrowableArray<FieldInfo>* _field_info;
  FieldLayoutInfo* _info;
  Array<InlineKlass*>* _inline_type_field_klasses;
  FieldGroup* _root_group;
  GrowableArray<FieldGroup*> _contended_groups;
  FieldGroup* _static_fields;
  FieldLayout* _layout;
  FieldLayout* _static_layout;
  int _nonstatic_oopmap_count;
  int _alignment;
  int _first_field_offset;
  int _internal_null_marker_offset; // if any, -1 means no internal null marker
  int _payload_size_in_bytes;
  int _atomic_field_count;
  int _fields_size_sum;
  bool _has_nonstatic_fields;
  bool _has_inline_type_fields;
  bool _is_contended;
  bool _is_inline_type;
  bool _has_flattening_information;
  bool _has_nonatomic_values;
  bool _nullable_atomic_flat_candidate;
  bool _has_null_markers;

  FieldGroup* get_or_create_contended_group(int g);

 public:
  FieldLayoutBuilder(const Symbol* classname, ClassLoaderData* loader_data, const InstanceKlass* super_klass, ConstantPool* constant_pool,
                     GrowableArray<FieldInfo>* field_info, bool is_contended, bool is_inline_type, FieldLayoutInfo* info,
                     Array<InlineKlass*>* inline_type_field_klasses);

  int get_alignment() {
    assert(_alignment != -1, "Uninitialized");
    return _alignment;
  }

  int get_first_field_offset() {
    assert(_first_field_offset != -1, "Uninitialized");
    return _first_field_offset;
  }

  int get_payload_size_in_byte() {
    assert(_payload_size_in_bytes != -1, "Uninitialized");
    return _payload_size_in_bytes;
  }

  int get_internal_null_marker_offset() {
    return _internal_null_marker_offset;
  }

  void build_layout();
  void compute_regular_layout();
  void compute_inline_class_layout();
  void insert_contended_padding(LayoutRawBlock* slot);
  void insert_null_markers();

 protected:
  void prologue();
  void epilogue();
  void regular_field_sorting();
  void inline_class_field_sorting();
  void add_flat_field_oopmap(OopMapBlocksBuilder* nonstatic_oop_map, InlineKlass* vk, int offset);
  void register_embedded_oops_from_list(OopMapBlocksBuilder* nonstatic_oop_maps, GrowableArray<LayoutRawBlock*>* list);
  void register_embedded_oops(OopMapBlocksBuilder* nonstatic_oop_maps, FieldGroup* group);
};

#endif // SHARE_CLASSFILE_FIELDLAYOUTBUILDER_HPP
