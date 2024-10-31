/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classFileParser.hpp"
#include "classfile/fieldLayoutBuilder.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "jvm.h"
#include "memory/resourceArea.hpp"
#include "oops/array.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/instanceMirrorKlass.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "utilities/powerOfTwo.hpp"

static LayoutKind field_layout_selection(FieldInfo field_info, Array<InlineLayoutInfo>* inline_layout_info_array) {

  if (field_info.field_flags().is_injected()) {
    // don't flatten injected fields
    return LayoutKind::REFERENCE;
  }

  if (inline_layout_info_array == nullptr || inline_layout_info_array->adr_at(field_info.index())->klass() == nullptr) {
    // field's type is not a known value class, using a reference
    return LayoutKind::REFERENCE;
  }

  InlineLayoutInfo* inline_field_info = inline_layout_info_array->adr_at(field_info.index());
  InlineKlass* vk = inline_field_info->klass();

  if (field_info.field_flags().is_null_free_inline_type()) {
    assert(vk->is_implicitly_constructible(), "null-free fields must be implicitly constructible");
    if (vk->must_be_atomic() || field_info.access_flags().is_volatile() ||  AlwaysAtomicAccesses) {
      return vk->has_atomic_layout() ? LayoutKind::ATOMIC_FLAT : LayoutKind::REFERENCE;
    } else {
      return vk->has_non_atomic_layout() ? LayoutKind::NON_ATOMIC_FLAT : LayoutKind::REFERENCE;
    }
  } else {
    if (NullableFieldFlattening && vk->has_nullable_layout()) {
      return LayoutKind::NULLABLE_ATOMIC_FLAT;
    } else {
      return LayoutKind::REFERENCE;
    }
  }
}

static void get_size_and_alignment(InlineKlass* vk, LayoutKind kind, int* size, int* alignment) {
  switch(kind) {
    case LayoutKind::NON_ATOMIC_FLAT:
      *size = vk->non_atomic_size_in_bytes();
      *alignment = vk->non_atomic_alignment();
      break;
    case LayoutKind::ATOMIC_FLAT:
      *size = vk->atomic_size_in_bytes();
      *alignment = *size;
      break;
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
      *size = vk->nullable_size_in_bytes();
      *alignment = *size;
    break;
    default:
      ShouldNotReachHere();
  }
}

LayoutRawBlock::LayoutRawBlock(Kind kind, int size) :
  _next_block(nullptr),
  _prev_block(nullptr),
  _inline_klass(nullptr),
  _block_kind(kind),
  _offset(-1),
  _alignment(1),
  _size(size),
  _field_index(-1) {
  assert(kind == EMPTY || kind == RESERVED || kind == PADDING || kind == INHERITED || kind == NULL_MARKER,
         "Otherwise, should use the constructor with a field index argument");
  assert(size > 0, "Sanity check");
}


LayoutRawBlock::LayoutRawBlock(int index, Kind kind, int size, int alignment) :
 _next_block(nullptr),
 _prev_block(nullptr),
 _inline_klass(nullptr),
 _block_kind(kind),
 _offset(-1),
 _alignment(alignment),
 _size(size),
 _field_index(index) {
  assert(kind == REGULAR || kind == FLAT || kind == INHERITED,
         "Other kind do not have a field index");
  assert(size > 0, "Sanity check");
  assert(alignment > 0, "Sanity check");
}

bool LayoutRawBlock::fit(int size, int alignment) {
  int adjustment = 0;
  if ((_offset % alignment) != 0) {
    adjustment = alignment - (_offset % alignment);
  }
  return _size >= size + adjustment;
}

FieldGroup::FieldGroup(int contended_group) :
  _next(nullptr),
  _small_primitive_fields(nullptr),
  _big_primitive_fields(nullptr),
  _oop_fields(nullptr),
  _contended_group(contended_group),  // -1 means no contended group, 0 means default contended group
  _oop_count(0) {}

void FieldGroup::add_primitive_field(int idx, BasicType type) {
  int size = type2aelembytes(type);
  LayoutRawBlock* block = new LayoutRawBlock(idx, LayoutRawBlock::REGULAR, size, size /* alignment == size for primitive types */);
  if (size >= oopSize) {
    add_to_big_primitive_list(block);
  } else {
    add_to_small_primitive_list(block);
  }
}

void FieldGroup::add_oop_field(int idx) {
  int size = type2aelembytes(T_OBJECT);
  LayoutRawBlock* block = new LayoutRawBlock(idx, LayoutRawBlock::REGULAR, size, size /* alignment == size for oops */);
  if (_oop_fields == nullptr) {
    _oop_fields = new GrowableArray<LayoutRawBlock*>(INITIAL_LIST_SIZE);
  }
  _oop_fields->append(block);
  _oop_count++;
}

void FieldGroup::add_flat_field(int idx, InlineKlass* vk, LayoutKind lk, int size, int alignment) {
  LayoutRawBlock* block = new LayoutRawBlock(idx, LayoutRawBlock::FLAT, size, alignment);
  block->set_inline_klass(vk);
  block->set_layout_kind(lk);
  if (block->size() >= oopSize) {
    add_to_big_primitive_list(block);
  } else {
    add_to_small_primitive_list(block);
  }
}

void FieldGroup::sort_by_size() {
  if (_small_primitive_fields != nullptr) {
    _small_primitive_fields->sort(LayoutRawBlock::compare_size_inverted);
  }
  if (_big_primitive_fields != nullptr) {
    _big_primitive_fields->sort(LayoutRawBlock::compare_size_inverted);
  }
}

void FieldGroup::add_to_small_primitive_list(LayoutRawBlock* block) {
  if (_small_primitive_fields == nullptr) {
    _small_primitive_fields = new GrowableArray<LayoutRawBlock*>(INITIAL_LIST_SIZE);
  }
  _small_primitive_fields->append(block);
}

void FieldGroup::add_to_big_primitive_list(LayoutRawBlock* block) {
  if (_big_primitive_fields == nullptr) {
    _big_primitive_fields = new GrowableArray<LayoutRawBlock*>(INITIAL_LIST_SIZE);
  }
  _big_primitive_fields->append(block);
}

FieldLayout::FieldLayout(GrowableArray<FieldInfo>* field_info, Array<InlineLayoutInfo>* inline_layout_info_array, ConstantPool* cp) :
  _field_info(field_info),
  _inline_layout_info_array(inline_layout_info_array),
  _cp(cp),
  _blocks(nullptr),
  _start(_blocks),
  _last(_blocks),
  _super_first_field_offset(-1),
  _super_alignment(-1),
  _super_min_align_required(-1),
  _default_value_offset(-1),
  _null_reset_value_offset(-1),
  _super_has_fields(false),
  _has_inherited_fields(false) {}

void FieldLayout::initialize_static_layout() {
  _blocks = new LayoutRawBlock(LayoutRawBlock::EMPTY, INT_MAX);
  _blocks->set_offset(0);
  _last = _blocks;
  _start = _blocks;
  // Note: at this stage, InstanceMirrorKlass::offset_of_static_fields() could be zero, because
  // during bootstrapping, the size of the java.lang.Class is still not known when layout
  // of static field is computed. Field offsets are fixed later when the size is known
  // (see java_lang_Class::fixup_mirror())
  if (InstanceMirrorKlass::offset_of_static_fields() > 0) {
    insert(first_empty_block(), new LayoutRawBlock(LayoutRawBlock::RESERVED, InstanceMirrorKlass::offset_of_static_fields()));
    _blocks->set_offset(0);
  }
}

void FieldLayout::initialize_instance_layout(const InstanceKlass* super_klass) {
  if (super_klass == nullptr) {
    _blocks = new LayoutRawBlock(LayoutRawBlock::EMPTY, INT_MAX);
    _blocks->set_offset(0);
    _last = _blocks;
    _start = _blocks;
    insert(first_empty_block(), new LayoutRawBlock(LayoutRawBlock::RESERVED, instanceOopDesc::base_offset_in_bytes()));
  } else {
    _super_has_fields = reconstruct_layout(super_klass);
    fill_holes(super_klass);
    if ((!super_klass->has_contended_annotations()) || !_super_has_fields) {
      _start = _blocks;  // start allocating fields from the first empty block
    } else {
      _start = _last;    // append fields at the end of the reconstructed layout
    }
  }
}

LayoutRawBlock* FieldLayout::first_field_block() {
  LayoutRawBlock* block = _blocks;
  while (block != nullptr
         && block->block_kind() != LayoutRawBlock::INHERITED
         && block->block_kind() != LayoutRawBlock::REGULAR
         && block->block_kind() != LayoutRawBlock::FLAT
         && block->block_kind() != LayoutRawBlock::NULL_MARKER) {
    block = block->next_block();
  }
  return block;
}

// Insert a set of fields into a layout.
// For each field, search for an empty slot able to fit the field
// (satisfying both size and alignment requirements), if none is found,
// add the field at the end of the layout.
// Fields cannot be inserted before the block specified in the "start" argument
void FieldLayout::add(GrowableArray<LayoutRawBlock*>* list, LayoutRawBlock* start) {
  if (list == nullptr) return;
  if (start == nullptr) start = this->_start;
  bool last_search_success = false;
  int last_size = 0;
  int last_alignment = 0;
  for (int i = 0; i < list->length(); i ++) {
    LayoutRawBlock* b = list->at(i);
    LayoutRawBlock* cursor = nullptr;
    LayoutRawBlock* candidate = nullptr;
    // if start is the last block, just append the field
    if (start == last_block()) {
      candidate = last_block();
    }
    // Before iterating over the layout to find an empty slot fitting the field's requirements,
    // check if the previous field had the same requirements and if the search for a fitting slot
    // was successful. If the requirements were the same but the search failed, a new search will
    // fail the same way, so just append the field at the of the layout.
    else  if (b->size() == last_size && b->alignment() == last_alignment && !last_search_success) {
      candidate = last_block();
    } else {
      // Iterate over the layout to find an empty slot fitting the field's requirements
      last_size = b->size();
      last_alignment = b->alignment();
      cursor = last_block()->prev_block();
      assert(cursor != nullptr, "Sanity check");
      last_search_success = true;

      while (cursor != start) {
        if (cursor->block_kind() == LayoutRawBlock::EMPTY && cursor->fit(b->size(), b->alignment())) {
          if (candidate == nullptr || cursor->size() < candidate->size()) {
            candidate = cursor;
          }
        }
        cursor = cursor->prev_block();
      }
      if (candidate == nullptr) {
        candidate = last_block();
        last_search_success = false;
      }
      assert(candidate != nullptr, "Candidate must not be null");
      assert(candidate->block_kind() == LayoutRawBlock::EMPTY, "Candidate must be an empty block");
      assert(candidate->fit(b->size(), b->alignment()), "Candidate must be able to store the block");
    }
    insert_field_block(candidate, b);
  }
}

// Used for classes with hard coded field offsets, insert a field at the specified offset */
void FieldLayout::add_field_at_offset(LayoutRawBlock* block, int offset, LayoutRawBlock* start) {
  assert(block != nullptr, "Sanity check");
  block->set_offset(offset);
  if (start == nullptr) {
    start = this->_start;
  }
  LayoutRawBlock* slot = start;
  while (slot != nullptr) {
    if ((slot->offset() <= block->offset() && (slot->offset() + slot->size()) > block->offset()) ||
        slot == _last){
      assert(slot->block_kind() == LayoutRawBlock::EMPTY, "Matching slot must be an empty slot");
      assert(slot->size() >= block->offset() - slot->offset() + block->size() ,"Matching slot must be big enough");
      if (slot->offset() < block->offset()) {
        int adjustment = block->offset() - slot->offset();
        LayoutRawBlock* adj = new LayoutRawBlock(LayoutRawBlock::EMPTY, adjustment);
        insert(slot, adj);
      }
      insert(slot, block);
      if (slot->size() == 0) {
        remove(slot);
      }
      if (block->block_kind() == LayoutRawBlock::REGULAR || block->block_kind() == LayoutRawBlock::FLAT) {
        _field_info->adr_at(block->field_index())->set_offset(block->offset());
      }
      return;
    }
    slot = slot->next_block();
  }
  fatal("Should have found a matching slot above, corrupted layout or invalid offset");
}

// The allocation logic uses a best fit strategy: the set of fields is allocated
// in the first empty slot big enough to contain the whole set ((including padding
// to fit alignment constraints).
void FieldLayout::add_contiguously(GrowableArray<LayoutRawBlock*>* list, LayoutRawBlock* start) {
  if (list == nullptr) return;
  if (start == nullptr) {
    start = _start;
  }
  // This code assumes that if the first block is well aligned, the following
  // blocks would naturally be well aligned (no need for adjustment)
  int size = 0;
  for (int i = 0; i < list->length(); i++) {
    size += list->at(i)->size();
  }

  LayoutRawBlock* candidate = nullptr;
  if (start == last_block()) {
    candidate = last_block();
  } else {
    LayoutRawBlock* first = list->at(0);
    candidate = last_block()->prev_block();
    while (candidate->block_kind() != LayoutRawBlock::EMPTY || !candidate->fit(size, first->alignment())) {
      if (candidate == start) {
        candidate = last_block();
        break;
      }
      candidate = candidate->prev_block();
    }
    assert(candidate != nullptr, "Candidate must not be null");
    assert(candidate->block_kind() == LayoutRawBlock::EMPTY, "Candidate must be an empty block");
    assert(candidate->fit(size, first->alignment()), "Candidate must be able to store the whole contiguous block");
  }

  for (int i = 0; i < list->length(); i++) {
    LayoutRawBlock* b = list->at(i);
    insert_field_block(candidate, b);
    assert((candidate->offset() % b->alignment() == 0), "Contiguous blocks must be naturally well aligned");
  }
}

LayoutRawBlock* FieldLayout::insert_field_block(LayoutRawBlock* slot, LayoutRawBlock* block) {
  assert(slot->block_kind() == LayoutRawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
  if (slot->offset() % block->alignment() != 0) {
    int adjustment = block->alignment() - (slot->offset() % block->alignment());
    LayoutRawBlock* adj = new LayoutRawBlock(LayoutRawBlock::EMPTY, adjustment);
    insert(slot, adj);
  }
  assert(block->size() >= block->size(), "Enough space must remain afte adjustment");
  insert(slot, block);
  if (slot->size() == 0) {
    remove(slot);
  }
  // NULL_MARKER blocks are not real fields, so they don't have an entry in the FieldInfo array
  if (block->block_kind() != LayoutRawBlock::NULL_MARKER) {
    _field_info->adr_at(block->field_index())->set_offset(block->offset());
    if (_field_info->adr_at(block->field_index())->name(_cp) == vmSymbols::default_value_name()) {
      _default_value_offset = block->offset();
    }
    if (_field_info->adr_at(block->field_index())->name(_cp) == vmSymbols::null_reset_value_name()) {
      _null_reset_value_offset = block->offset();
    }
  }
  if (block->block_kind() == LayoutRawBlock::FLAT && block->layout_kind() == LayoutKind::NULLABLE_ATOMIC_FLAT) {
    int nm_offset = block->inline_klass()->null_marker_offset() - block->inline_klass()->first_field_offset() + block->offset();
    _field_info->adr_at(block->field_index())->set_null_marker_offset(nm_offset);
    _inline_layout_info_array->adr_at(block->field_index())->set_null_marker_offset(nm_offset);
  }

  return block;
}

bool FieldLayout::reconstruct_layout(const InstanceKlass* ik) {
  bool has_instance_fields = false;
  if (ik->is_abstract() && !ik->is_identity_class()) {
    _super_alignment = type2aelembytes(BasicType::T_LONG);
  }
  GrowableArray<LayoutRawBlock*>* all_fields = new GrowableArray<LayoutRawBlock*>(32);
  while (ik != nullptr) {
    for (AllFieldStream fs(ik->fieldinfo_stream(), ik->constants()); !fs.done(); fs.next()) {
      BasicType type = Signature::basic_type(fs.signature());
      // distinction between static and non-static fields is missing
      if (fs.access_flags().is_static()) continue;
      has_instance_fields = true;
      _has_inherited_fields = true;
      if (_super_first_field_offset == -1 || fs.offset() < _super_first_field_offset) _super_first_field_offset = fs.offset();
      LayoutRawBlock* block;
      if (fs.is_flat()) {
        InlineLayoutInfo layout_info = ik->inline_layout_info(fs.index());
        InlineKlass* vk = layout_info.klass();
        block = new LayoutRawBlock(fs.index(), LayoutRawBlock::INHERITED,
                                   vk->layout_size_in_bytes(layout_info.kind()),
                                   vk->layout_alignment(layout_info.kind()));
        assert(_super_alignment == -1 || _super_alignment >=  vk->payload_alignment(), "Invalid value alignment");
        _super_min_align_required = _super_min_align_required > vk->payload_alignment() ? _super_min_align_required : vk->payload_alignment();
      } else {
        int size = type2aelembytes(type);
        // INHERITED blocks are marked as non-reference because oop_maps are handled by their holder class
        block = new LayoutRawBlock(fs.index(), LayoutRawBlock::INHERITED, size, size);
        // For primitive types, the alignment is equal to the size
        assert(_super_alignment == -1 || _super_alignment >=  size, "Invalid value alignment");
        _super_min_align_required = _super_min_align_required > size ? _super_min_align_required : size;
      }
      block->set_offset(fs.offset());
      all_fields->append(block);
    }
    ik = ik->super() == nullptr ? nullptr : InstanceKlass::cast(ik->super());
  }
  all_fields->sort(LayoutRawBlock::compare_offset);
  _blocks = new LayoutRawBlock(LayoutRawBlock::RESERVED, instanceOopDesc::base_offset_in_bytes());
  _blocks->set_offset(0);
  _last = _blocks;
  for(int i = 0; i < all_fields->length(); i++) {
    LayoutRawBlock* b = all_fields->at(i);
    _last->set_next_block(b);
    b->set_prev_block(_last);
    _last = b;
  }
  _start = _blocks;
  return has_instance_fields;
}

// Called during the reconstruction of a layout, after fields from super
// classes have been inserted. It fills unused slots between inserted fields
// with EMPTY blocks, so the regular field insertion methods would work.
// This method handles classes with @Contended annotations differently
// by inserting PADDING blocks instead of EMPTY block to prevent subclasses'
// fields to interfere with contended fields/classes.
void FieldLayout::fill_holes(const InstanceKlass* super_klass) {
  assert(_blocks != nullptr, "Sanity check");
  assert(_blocks->offset() == 0, "first block must be at offset zero");
  LayoutRawBlock::Kind filling_type = super_klass->has_contended_annotations() ? LayoutRawBlock::PADDING: LayoutRawBlock::EMPTY;
  LayoutRawBlock* b = _blocks;
  while (b->next_block() != nullptr) {
    if (b->next_block()->offset() > (b->offset() + b->size())) {
      int size = b->next_block()->offset() - (b->offset() + b->size());
      // FIXME it would be better if initial empty block where tagged as PADDING for value classes
      LayoutRawBlock* empty = new LayoutRawBlock(filling_type, size);
      empty->set_offset(b->offset() + b->size());
      empty->set_next_block(b->next_block());
      b->next_block()->set_prev_block(empty);
      b->set_next_block(empty);
      empty->set_prev_block(b);
    }
    b = b->next_block();
  }
  assert(b->next_block() == nullptr, "Invariant at this point");
  assert(b->block_kind() != LayoutRawBlock::EMPTY, "Sanity check");
  // If the super class has @Contended annotation, a padding block is
  // inserted at the end to ensure that fields from the subclasses won't share
  // the cache line of the last field of the contended class
  if (super_klass->has_contended_annotations() && ContendedPaddingWidth > 0) {
    LayoutRawBlock* p = new LayoutRawBlock(LayoutRawBlock::PADDING, ContendedPaddingWidth);
    p->set_offset(b->offset() + b->size());
    b->set_next_block(p);
    p->set_prev_block(b);
    b = p;
  }

  LayoutRawBlock* last = new LayoutRawBlock(LayoutRawBlock::EMPTY, INT_MAX);
  last->set_offset(b->offset() + b->size());
  assert(last->offset() > 0, "Sanity check");
  b->set_next_block(last);
  last->set_prev_block(b);
  _last = last;
}

LayoutRawBlock* FieldLayout::insert(LayoutRawBlock* slot, LayoutRawBlock* block) {
  assert(slot->block_kind() == LayoutRawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
  assert(slot->offset() % block->alignment() == 0, "Incompatible alignment");
  block->set_offset(slot->offset());
  slot->set_offset(slot->offset() + block->size());
  assert((slot->size() - block->size()) < slot->size(), "underflow checking");
  assert(slot->size() - block->size() >= 0, "no negative size allowed");
  slot->set_size(slot->size() - block->size());
  block->set_prev_block(slot->prev_block());
  block->set_next_block(slot);
  slot->set_prev_block(block);
  if (block->prev_block() != nullptr) {
    block->prev_block()->set_next_block(block);
  }
  if (_blocks == slot) {
    _blocks = block;
  }
  if (_start == slot) {
    _start = block;
  }
  return block;
}

void FieldLayout::remove(LayoutRawBlock* block) {
  assert(block != nullptr, "Sanity check");
  assert(block != _last, "Sanity check");
  if (_blocks == block) {
    _blocks = block->next_block();
    if (_blocks != nullptr) {
      _blocks->set_prev_block(nullptr);
    }
  } else {
    assert(block->prev_block() != nullptr, "_prev should be set for non-head blocks");
    block->prev_block()->set_next_block(block->next_block());
    block->next_block()->set_prev_block(block->prev_block());
  }
  if (block == _start) {
    _start = block->prev_block();
  }
}

void FieldLayout::shift_fields(int shift) {
  LayoutRawBlock* b = first_field_block();
  LayoutRawBlock* previous = b->prev_block();
  if (previous->block_kind() == LayoutRawBlock::EMPTY) {
    previous->set_size(previous->size() + shift);
  } else {
    LayoutRawBlock* nb = new LayoutRawBlock(LayoutRawBlock::PADDING, shift);
    nb->set_offset(b->offset());
    previous->set_next_block(nb);
    nb->set_prev_block(previous);
    b->set_prev_block(nb);
    nb->set_next_block(b);
  }
  while (b != nullptr) {
    b->set_offset(b->offset() + shift);
    if (b->block_kind() == LayoutRawBlock::REGULAR || b->block_kind() == LayoutRawBlock::FLAT) {
      _field_info->adr_at(b->field_index())->set_offset(b->offset());
    }
    assert(b->block_kind() == LayoutRawBlock::EMPTY || b->offset() % b->alignment() == 0, "Must still be correctly aligned");
    b = b->next_block();
  }
}

LayoutRawBlock* FieldLayout::find_null_marker() {
  LayoutRawBlock* b = _blocks;
  while (b != nullptr) {
    if (b->block_kind() == LayoutRawBlock::NULL_MARKER) {
      return b;
    }
    b = b->next_block();
  }
  ShouldNotReachHere();
}

void FieldLayout::remove_null_marker() {
  LayoutRawBlock* b = first_field_block();
  while (b != nullptr) {
    if (b->block_kind() == LayoutRawBlock::NULL_MARKER) {
      if (b->next_block()->block_kind() == LayoutRawBlock::EMPTY) {
        LayoutRawBlock* n = b->next_block();
        remove(b);
        n->set_offset(b->offset());
        n->set_size(n->size() + b->size());
      } else {
        b->set_block_kind(LayoutRawBlock::EMPTY);
      }
      return;
    }
    b = b->next_block();
  }
  ShouldNotReachHere(); // if we reach this point, the null marker was not found!
}

static const char* layout_kind_to_string(LayoutKind lk) {
  switch(lk) {
    case LayoutKind::REFERENCE:
      return "REFERENCE";
    case LayoutKind::NON_ATOMIC_FLAT:
      return "NON_ATOMIC_FLAT";
    case LayoutKind::ATOMIC_FLAT:
      return "ATOMIC_FLAT";
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
      return "NULLABLE_ATOMIC_FLAT";
    case LayoutKind::UNKNOWN:
      return "UNKNOWN";
    default:
      ShouldNotReachHere();
  }
}

void FieldLayout::print(outputStream* output, bool is_static, const InstanceKlass* super, Array<InlineLayoutInfo>* inline_fields) {
  ResourceMark rm;
  LayoutRawBlock* b = _blocks;
  while(b != _last) {
    switch(b->block_kind()) {
      case LayoutRawBlock::REGULAR: {
        FieldInfo* fi = _field_info->adr_at(b->field_index());
        output->print_cr(" @%d %s %d/%d \"%s\" %s",
                         b->offset(),
                         "REGULAR",
                         b->size(),
                         b->alignment(),
                         fi->name(_cp)->as_C_string(),
                         fi->signature(_cp)->as_C_string());
        break;
      }
      case LayoutRawBlock::FLAT: {
        FieldInfo* fi = _field_info->adr_at(b->field_index());
        InlineKlass* ik = inline_fields->adr_at(fi->index())->klass();
        assert(ik != nullptr, "");
        output->print_cr(" @%d %s %d/%d \"%s\" %s %s@%p %s",
                         b->offset(),
                         "FLAT",
                         b->size(),
                         b->alignment(),
                         fi->name(_cp)->as_C_string(),
                         fi->signature(_cp)->as_C_string(),
                         ik->name()->as_C_string(),
                         ik->class_loader_data(), layout_kind_to_string(b->layout_kind()));
        break;
      }
      case LayoutRawBlock::RESERVED: {
        output->print_cr(" @%d %s %d/-",
                         b->offset(),
                         "RESERVED",
                         b->size());
        break;
      }
      case LayoutRawBlock::INHERITED: {
        assert(!is_static, "Static fields are not inherited in layouts");
        assert(super != nullptr, "super klass must be provided to retrieve inherited fields info");
        bool found = false;
        const InstanceKlass* ik = super;
        while (!found && ik != nullptr) {
          for (AllFieldStream fs(ik->fieldinfo_stream(), ik->constants()); !fs.done(); fs.next()) {
            if (fs.offset() == b->offset() && fs.access_flags().is_static() == is_static) {
              output->print_cr(" @%d %s %d/%d \"%s\" %s",
                  b->offset(),
                  "INHERITED",
                  b->size(),
                  b->size(), // so far, alignment constraint == size, will change with Valhalla => FIXME
                  fs.name()->as_C_string(),
                  fs.signature()->as_C_string());
              found = true;
              break;
            }
        }
        ik = ik->java_super();
      }
      break;
    }
    case LayoutRawBlock::EMPTY:
      output->print_cr(" @%d %s %d/1",
                       b->offset(),
                      "EMPTY",
                       b->size());
      break;
    case LayoutRawBlock::PADDING:
      output->print_cr(" @%d %s %d/1",
                      b->offset(),
                      "PADDING",
                      b->size());
      break;
    case LayoutRawBlock::NULL_MARKER:
    {
      output->print_cr(" @%d %s %d/1 ",
                      b->offset(),
                      "NULL_MARKER",
                      b->size());
      break;
    }
    default:
      fatal("Unknown block type");
    }
    b = b->next_block();
  }
}

FieldLayoutBuilder::FieldLayoutBuilder(const Symbol* classname, ClassLoaderData* loader_data, const InstanceKlass* super_klass, ConstantPool* constant_pool,
                                       GrowableArray<FieldInfo>* field_info, bool is_contended, bool is_inline_type,bool is_abstract_value,
                                       bool must_be_atomic, FieldLayoutInfo* info, Array<InlineLayoutInfo>* inline_layout_info_array) :
  _classname(classname),
  _loader_data(loader_data),
  _super_klass(super_klass),
  _constant_pool(constant_pool),
  _field_info(field_info),
  _info(info),
  _inline_layout_info_array(inline_layout_info_array),
  _root_group(nullptr),
  _contended_groups(GrowableArray<FieldGroup*>(8)),
  _static_fields(nullptr),
  _layout(nullptr),
  _static_layout(nullptr),
  _nonstatic_oopmap_count(0),
  _payload_alignment(-1),
  _first_field_offset(-1),
  _null_marker_offset(-1),
  _payload_size_in_bytes(-1),
  _non_atomic_layout_size_in_bytes(-1),
  _non_atomic_layout_alignment(-1),
  _atomic_layout_size_in_bytes(-1),
  _nullable_layout_size_in_bytes(-1),
  _fields_size_sum(0),
  _declared_non_static_fields_count(0),
  _has_non_naturally_atomic_fields(false),
  _is_naturally_atomic(false),
  _must_be_atomic(must_be_atomic),
  _has_nonstatic_fields(false),
  _has_inline_type_fields(false),
  _is_contended(is_contended),
  _is_inline_type(is_inline_type),
  _is_abstract_value(is_abstract_value),
  _has_flattening_information(is_inline_type),
  _is_empty_inline_class(false) {}

FieldGroup* FieldLayoutBuilder::get_or_create_contended_group(int g) {
  assert(g > 0, "must only be called for named contended groups");
  FieldGroup* fg = nullptr;
  for (int i = 0; i < _contended_groups.length(); i++) {
    fg = _contended_groups.at(i);
    if (fg->contended_group() == g) return fg;
  }
  fg = new FieldGroup(g);
  _contended_groups.append(fg);
  return fg;
}

void FieldLayoutBuilder::prologue() {
  _layout = new FieldLayout(_field_info, _inline_layout_info_array, _constant_pool);
  const InstanceKlass* super_klass = _super_klass;
  _layout->initialize_instance_layout(super_klass);
  _nonstatic_oopmap_count = super_klass == nullptr ? 0 : super_klass->nonstatic_oop_map_count();
  if (super_klass != nullptr) {
    _has_nonstatic_fields = super_klass->has_nonstatic_fields();
  }
  _static_layout = new FieldLayout(_field_info, _inline_layout_info_array, _constant_pool);
  _static_layout->initialize_static_layout();
  _static_fields = new FieldGroup();
  _root_group = new FieldGroup();
}

// Field sorting for regular (non-inline) classes:
//   - fields are sorted in static and non-static fields
//   - non-static fields are also sorted according to their contention group
//     (support of the @Contended annotation)
//   - @Contended annotation is ignored for static fields
//   - field flattening decisions are taken in this method
void FieldLayoutBuilder::regular_field_sorting() {
  int idx = 0;
  for (GrowableArrayIterator<FieldInfo> it = _field_info->begin(); it != _field_info->end(); ++it, ++idx) {
    FieldGroup* group = nullptr;
    FieldInfo fieldinfo = *it;
    if (fieldinfo.access_flags().is_static()) {
      group = _static_fields;
    } else {
      _has_nonstatic_fields = true;
      if (fieldinfo.field_flags().is_contended()) {
        int g = fieldinfo.contended_group();
        if (g == 0) {
          group = new FieldGroup(true);
          _contended_groups.append(group);
        } else {
          group = get_or_create_contended_group(g);
        }
      } else {
        group = _root_group;
      }
    }
    assert(group != nullptr, "invariant");
    BasicType type = Signature::basic_type(fieldinfo.signature(_constant_pool));
    switch(type) {
    case T_BYTE:
    case T_CHAR:
    case T_DOUBLE:
    case T_FLOAT:
    case T_INT:
    case T_LONG:
    case T_SHORT:
    case T_BOOLEAN:
      group->add_primitive_field(idx, type);
      break;
    case T_OBJECT:
    case T_ARRAY:
    {
      LayoutKind lk = field_layout_selection(fieldinfo, _inline_layout_info_array);
      if (fieldinfo.field_flags().is_null_free_inline_type() || lk != LayoutKind::REFERENCE
          || (!fieldinfo.field_flags().is_injected()
              && _inline_layout_info_array != nullptr && _inline_layout_info_array->adr_at(fieldinfo.index())->klass() != nullptr
              && !_inline_layout_info_array->adr_at(fieldinfo.index())->klass()->is_identity_class())) {
        _has_inline_type_fields = true;
        _has_flattening_information = true;
      }
      if (lk == LayoutKind::REFERENCE) {
        if (group != _static_fields) _nonstatic_oopmap_count++;
        group->add_oop_field(idx);
      } else {
        _has_flattening_information = true;
        InlineKlass* vk = _inline_layout_info_array->adr_at(fieldinfo.index())->klass();
        int size, alignment;
        get_size_and_alignment(vk, lk, &size, &alignment);
        group->add_flat_field(idx, vk, lk, size, alignment);
        _inline_layout_info_array->adr_at(fieldinfo.index())->set_kind(lk);
        _nonstatic_oopmap_count += vk->nonstatic_oop_map_count();
        _field_info->adr_at(idx)->field_flags_addr()->update_flat(true);
        _field_info->adr_at(idx)->set_layout_kind(lk);
        // no need to update _must_be_atomic if vk->must_be_atomic() is true because current class is not an inline class
      }
      break;
    }
    default:
      fatal("Something wrong?");
    }
  }
  _root_group->sort_by_size();
  _static_fields->sort_by_size();
  if (!_contended_groups.is_empty()) {
    for (int i = 0; i < _contended_groups.length(); i++) {
      _contended_groups.at(i)->sort_by_size();
    }
  }
}

/* Field sorting for inline classes:
 *   - because inline classes are immutable, the @Contended annotation is ignored
 *     when computing their layout (with only read operation, there's no false
 *     sharing issue)
 *   - this method also records the alignment of the field with the most
 *     constraining alignment, this value is then used as the alignment
 *     constraint when flattening this inline type into another container
 *   - field flattening decisions are taken in this method (those decisions are
 *     currently only based in the size of the fields to be flattened, the size
 *     of the resulting instance is not considered)
 */
void FieldLayoutBuilder::inline_class_field_sorting() {
  assert(_is_inline_type || _is_abstract_value, "Should only be used for inline classes");
  int alignment = -1;
  int idx = 0;
  for (GrowableArrayIterator<FieldInfo> it = _field_info->begin(); it != _field_info->end(); ++it, ++idx) {
    FieldGroup* group = nullptr;
    FieldInfo fieldinfo = *it;
    int field_alignment = 1;
    if (fieldinfo.access_flags().is_static()) {
      group = _static_fields;
    } else {
      _has_nonstatic_fields = true;
      _declared_non_static_fields_count++;
      group = _root_group;
    }
    assert(group != nullptr, "invariant");
    BasicType type = Signature::basic_type(fieldinfo.signature(_constant_pool));
    switch(type) {
    case T_BYTE:
    case T_CHAR:
    case T_DOUBLE:
    case T_FLOAT:
    case T_INT:
    case T_LONG:
    case T_SHORT:
    case T_BOOLEAN:
      if (group != _static_fields) {
        field_alignment = type2aelembytes(type); // alignment == size for primitive types
      }
      group->add_primitive_field(fieldinfo.index(), type);
      break;
    case T_OBJECT:
    case T_ARRAY:
    {
      LayoutKind lk = field_layout_selection(fieldinfo, _inline_layout_info_array);
      if (fieldinfo.field_flags().is_null_free_inline_type() || lk != LayoutKind::REFERENCE
          || (!fieldinfo.field_flags().is_injected()
              && _inline_layout_info_array != nullptr && _inline_layout_info_array->adr_at(fieldinfo.index())->klass() != nullptr
              && !_inline_layout_info_array->adr_at(fieldinfo.index())->klass()->is_identity_class())) {
        _has_inline_type_fields = true;
        _has_flattening_information = true;
      }
      if (lk == LayoutKind::REFERENCE) {
        if (group != _static_fields) {
          _nonstatic_oopmap_count++;
          field_alignment = type2aelembytes(type); // alignment == size for oops
        }
        group->add_oop_field(idx);
      } else {
        _has_flattening_information = true;
        InlineKlass* vk = _inline_layout_info_array->adr_at(fieldinfo.index())->klass();
        if (!vk->is_naturally_atomic()) _has_non_naturally_atomic_fields = true;
        int size, alignment;
        get_size_and_alignment(vk, lk, &size, &alignment);
        group->add_flat_field(idx, vk, lk, size, alignment);
        _inline_layout_info_array->adr_at(fieldinfo.index())->set_kind(lk);
        _nonstatic_oopmap_count += vk->nonstatic_oop_map_count();
        field_alignment = alignment;
        _field_info->adr_at(idx)->field_flags_addr()->update_flat(true);
        _field_info->adr_at(idx)->set_layout_kind(lk);
        // default is atomic, but class file parsing could have set _must_be_atomic to false (@LooselyConsistentValue + checks)
        // Presence of a must_be_atomic field must revert the _must_be_atomic flag of the holder to true
        if (vk->must_be_atomic()) {
          _must_be_atomic = true;
        }
      }
      break;
    }
    default:
      fatal("Unexpected BasicType");
    }
    if (!fieldinfo.access_flags().is_static() && field_alignment > alignment) alignment = field_alignment;
  }
  _payload_alignment = alignment;
  assert(_has_nonstatic_fields || _is_abstract_value, "Concrete value types do not support zero instance size yet");
}

void FieldLayoutBuilder::insert_contended_padding(LayoutRawBlock* slot) {
  if (ContendedPaddingWidth > 0) {
    LayoutRawBlock* padding = new LayoutRawBlock(LayoutRawBlock::PADDING, ContendedPaddingWidth);
    _layout->insert(slot, padding);
  }
}

/* Computation of regular classes layout is an evolution of the previous default layout
 * (FieldAllocationStyle 1):
 *   - primitive fields (both primitive types and flat inline types) are allocated
 *     first, from the biggest to the smallest
 *   - then oop fields are allocated (to increase chances to have contiguous oops and
 *     a simpler oopmap).
 */
void FieldLayoutBuilder::compute_regular_layout() {
  bool need_tail_padding = false;
  prologue();
  regular_field_sorting();
  if (_is_contended) {
    _layout->set_start(_layout->last_block());
    // insertion is currently easy because the current strategy doesn't try to fill holes
    // in super classes layouts => the _start block is by consequence the _last_block
    insert_contended_padding(_layout->start());
    need_tail_padding = true;
  }
  _layout->add(_root_group->big_primitive_fields());
  _layout->add(_root_group->small_primitive_fields());
  _layout->add(_root_group->oop_fields());

  if (!_contended_groups.is_empty()) {
    for (int i = 0; i < _contended_groups.length(); i++) {
      FieldGroup* cg = _contended_groups.at(i);
      LayoutRawBlock* start = _layout->last_block();
      insert_contended_padding(start);
      _layout->add(cg->big_primitive_fields());
      _layout->add(cg->small_primitive_fields(), start);
      _layout->add(cg->oop_fields(), start);
      need_tail_padding = true;
    }
  }

  if (need_tail_padding) {
    insert_contended_padding(_layout->last_block());
  }

  // Warning: IntanceMirrorKlass expects static oops to be allocated first
  _static_layout->add_contiguously(_static_fields->oop_fields());
  _static_layout->add(_static_fields->big_primitive_fields());
  _static_layout->add(_static_fields->small_primitive_fields());

  epilogue();
}

/* Computation of inline classes has a slightly different strategy than for
 * regular classes. Regular classes have their oop fields allocated at the end
 * of the layout to increase GC performances. Unfortunately, this strategy
 * increases the number of empty slots inside an instance. Because the purpose
 * of inline classes is to be embedded into other containers, it is critical
 * to keep their size as small as possible. For this reason, the allocation
 * strategy is:
 *   - big primitive fields (primitive types and flat inline type smaller
 *     than an oop) are allocated first (from the biggest to the smallest)
 *   - then oop fields
 *   - then small primitive fields (from the biggest to the smallest)
 */
void FieldLayoutBuilder::compute_inline_class_layout() {

  // Test if the concrete inline class is an empty class (no instance fields)
  // and insert a dummy field if needed
  if (!_is_abstract_value) {
    bool declares_non_static_fields = false;
    for (GrowableArrayIterator<FieldInfo> it = _field_info->begin(); it != _field_info->end(); ++it) {
      FieldInfo fieldinfo = *it;
      if (!fieldinfo.access_flags().is_static()) {
        declares_non_static_fields = true;
        break;
      }
    }
    if (!declares_non_static_fields) {
      bool has_inherited_fields = false;
      const InstanceKlass* super = _super_klass;
      while(super != nullptr) {
        if (super->has_nonstatic_fields()) {
          has_inherited_fields = true;
          break;
        }
        super = super->super() == nullptr ? nullptr : InstanceKlass::cast(super->super());
      }

      if (!has_inherited_fields) {
        // Inject ".empty" dummy field
        _is_empty_inline_class = true;
        FieldInfo::FieldFlags fflags(0);
        fflags.update_injected(true);
        AccessFlags aflags;
        FieldInfo fi(aflags,
                    (u2)vmSymbols::as_int(VM_SYMBOL_ENUM_NAME(empty_marker_name)),
                    (u2)vmSymbols::as_int(VM_SYMBOL_ENUM_NAME(byte_signature)),
                    0,
                    fflags);
        int idx = _field_info->append(fi);
        _field_info->adr_at(idx)->set_index(idx);
      }
    }
  }

  prologue();
  inline_class_field_sorting();

  assert(_layout->start()->block_kind() == LayoutRawBlock::RESERVED, "Unexpected");

  if (_layout->super_has_fields() && !_is_abstract_value) {  // non-static field layout
    if (!_has_nonstatic_fields) {
      assert(_is_abstract_value, "Concrete value types have at least one field");
      // Nothing to do
    } else {
      // decide which alignment to use, then set first allowed field offset

      assert(_layout->super_alignment() >= _payload_alignment, "Incompatible alignment");
      assert(_layout->super_alignment() % _payload_alignment == 0, "Incompatible alignment");

      if (_payload_alignment < _layout->super_alignment()) {
        int new_alignment = _payload_alignment > _layout->super_min_align_required() ? _payload_alignment : _layout->super_min_align_required();
        assert(new_alignment % _payload_alignment == 0, "Must be");
        assert(new_alignment % _layout->super_min_align_required() == 0, "Must be");
        _payload_alignment = new_alignment;
      }
      if (_layout->first_empty_block()->offset() < _layout->first_field_block()->offset()) {
        LayoutRawBlock* first_empty = _layout->start()->next_block();
        if (first_empty->offset() % _payload_alignment != 0) {
          int size =  _payload_alignment - (first_empty->offset() % _payload_alignment);
          LayoutRawBlock* padding = new LayoutRawBlock(LayoutRawBlock::PADDING, size);
          _layout->insert(first_empty, padding);
          _layout->set_start(padding);
        } else {
          _layout->set_start( _layout->start());
        }
      } else {
        _layout->set_start(_layout->first_field_block());
      }
    }
  } else {
    if (_is_abstract_value && _has_nonstatic_fields) {
      _payload_alignment = type2aelembytes(BasicType::T_LONG);
    }
    assert(_layout->start()->next_block()->block_kind() == LayoutRawBlock::EMPTY || !UseCompressedClassPointers, "Unexpected");
    LayoutRawBlock* first_empty = _layout->start()->next_block();
    if (first_empty->offset() % _payload_alignment != 0) {
      LayoutRawBlock* padding = new LayoutRawBlock(LayoutRawBlock::PADDING, _payload_alignment - (first_empty->offset() % _payload_alignment));
      _layout->insert(first_empty, padding);
      if (first_empty->size() == 0) {
        _layout->remove(first_empty);
      }
      _layout->set_start(padding);
    }
  }

  _layout->add(_root_group->big_primitive_fields());
  _layout->add(_root_group->oop_fields());
  _layout->add(_root_group->small_primitive_fields());

  LayoutRawBlock* first_field = _layout->first_field_block();
  if (first_field != nullptr) {
    _first_field_offset = _layout->first_field_block()->offset();
    _payload_size_in_bytes = _layout->last_block()->offset() - _layout->first_field_block()->offset();
  } else {
    assert(_is_abstract_value, "Concrete inline types must have at least one field");
    _first_field_offset = _layout->blocks()->size();
    _payload_size_in_bytes = 0;
  }

  // Determining if the value class is naturally atomic:
  if ((!_layout->super_has_fields() && _declared_non_static_fields_count <= 1 && !_has_non_naturally_atomic_fields)
      || (_layout->super_has_fields() && _super_klass->is_naturally_atomic() && _declared_non_static_fields_count == 0)) {
        _is_naturally_atomic = true;
  }

  // At this point, the characteristics of the raw layout (used in standalone instances) are known.
  // From this, additional layouts will be computed: atomic and nullable layouts
  // Once those additional layouts are computed, the raw layout might need some adjustments

  if (!_is_abstract_value) { // Flat layouts are only for concrete value classes
    // Validation of the non atomic layout
    if ((InlineFieldMaxFlatSize < 0 || _payload_size_in_bytes * BitsPerByte <= InlineFieldMaxFlatSize)
         && (!_must_be_atomic || _is_naturally_atomic)) {
      _non_atomic_layout_size_in_bytes = _payload_size_in_bytes;
      _non_atomic_layout_alignment = _payload_alignment;
    }

    // Next step is to compute the characteristics for a layout enabling atomic updates
    if (AtomicFieldFlattening) {
      int atomic_size = _payload_size_in_bytes == 0 ? 0 : round_up_power_of_2(_payload_size_in_bytes);
      if (  atomic_size <= (int)MAX_ATOMIC_OP_SIZE
          && (InlineFieldMaxFlatSize < 0 || atomic_size * BitsPerByte <= InlineFieldMaxFlatSize)) {
        _atomic_layout_size_in_bytes = atomic_size;
      }
    }

    // Next step is the nullable layout: the layout must include a null marker and must also be atomic
    if (NullableFieldFlattening) {
      // Looking if there's an empty slot inside the layout that could be used to store a null marker
      // FIXME: could it be possible to re-use the .empty field as a null marker for empty values?
      LayoutRawBlock* b = _layout->first_field_block();
      assert(b != nullptr, "A concrete value class must have at least one (possible dummy) field");
      int null_marker_offset = -1;
      if (_is_empty_inline_class) {
        // Reusing the dummy field as a field marker
        assert(_field_info->adr_at(b->field_index())->name(_constant_pool) == vmSymbols::empty_marker_name(), "b must be the dummy field");
        null_marker_offset = b->offset();
      } else {
        while (b != _layout->last_block()) {
          if (b->block_kind() == LayoutRawBlock::EMPTY) {
            break;
          }
          b = b->next_block();
        }
        if (b != _layout->last_block()) {
          // found an empty slot, register its offset from the beginning of the payload
          null_marker_offset = b->offset();
          LayoutRawBlock* marker = new LayoutRawBlock(LayoutRawBlock::NULL_MARKER, 1);
          _layout->add_field_at_offset(marker, b->offset());
        }
        if (null_marker_offset == -1) { // no empty slot available to store the null marker, need to inject one
          int last_offset = _layout->last_block()->offset();
          LayoutRawBlock* marker = new LayoutRawBlock(LayoutRawBlock::NULL_MARKER, 1);
          _layout->insert_field_block(_layout->last_block(), marker);
          assert(marker->offset() == last_offset, "Null marker should have been inserted at the end");
          null_marker_offset = marker->offset();
        }
      }

      // Now that the null marker is there, the size of the nullable layout must computed (remember, must be atomic too)
      int new_raw_size = _layout->last_block()->offset() - _layout->first_field_block()->offset();
      int nullable_size = round_up_power_of_2(new_raw_size);
      if (nullable_size <= (int)MAX_ATOMIC_OP_SIZE
        && (InlineFieldMaxFlatSize < 0 || nullable_size * BitsPerByte <= InlineFieldMaxFlatSize)) {
        _nullable_layout_size_in_bytes = nullable_size;
        _null_marker_offset = null_marker_offset;
      } else {
        // If the nullable layout is rejected, the NULL_MARKER block should be removed
        // from the layout, otherwise it will appear anyway if the layout is printer
        _layout->remove_null_marker();
        _null_marker_offset = -1;
      }
    }
    // If the inline class has an atomic or nullable (which is also atomic) layout,
    // we want the raw layout to have the same alignment as those atomic layouts so access codes
    // could remain  simple (single instruction without intermediate copy). This might required
    // to shift all fields in the raw layout, but this operation is possible only if the class
    // doesn't have inherited fields (offsets of inherited fields cannot be changed). If a
    // field shift is needed but not possible, all atomic layouts are disabled and only reference
    // and loosely consistent are supported.
    int required_alignment = _payload_alignment;
    if (has_atomic_layout() && _payload_alignment < atomic_layout_size_in_bytes()) {
      required_alignment = atomic_layout_size_in_bytes();
    }
    if (has_nullable_layout() && _payload_alignment < nullable_layout_size_in_bytes()) {
      required_alignment = nullable_layout_size_in_bytes();
    }
    int shift = first_field->offset() % required_alignment;
    if (shift != 0) {
      if (required_alignment > _payload_alignment && !_layout->has_inherited_fields()) {
        assert(_layout->first_field_block() != nullptr, "A concrete value class must have at least one (possible dummy) field");
        _layout->shift_fields(shift);
        _first_field_offset = _layout->first_field_block()->offset();
        if (has_nullable_layout()) {
          assert(!_is_empty_inline_class, "Should not get here with empty values");
          _null_marker_offset = _layout->find_null_marker()->offset();
        }
        _payload_alignment = required_alignment;
      } else {
        _atomic_layout_size_in_bytes = -1;
        if (has_nullable_layout() && !_is_empty_inline_class) {  // empty values don't have a dedicated NULL_MARKER block
          _layout->remove_null_marker();
        }
        _nullable_layout_size_in_bytes = -1;
        _null_marker_offset = -1;
      }
    } else {
      _payload_alignment = required_alignment;
    }

    // If the inline class has a nullable layout, the layout used in heap allocated standalone
    // instances must also be the nullable layout, in order to be able to set the null marker to
    // non-null before copying the payload to other containers.
    if (has_nullable_layout() && payload_layout_size_in_bytes() < nullable_layout_size_in_bytes()) {
      _payload_size_in_bytes = nullable_layout_size_in_bytes();
    }
  }
  // Warning:: InstanceMirrorKlass expects static oops to be allocated first
  _static_layout->add_contiguously(_static_fields->oop_fields());
  _static_layout->add(_static_fields->big_primitive_fields());
  _static_layout->add(_static_fields->small_primitive_fields());

  epilogue();
}

void FieldLayoutBuilder::add_flat_field_oopmap(OopMapBlocksBuilder* nonstatic_oop_maps,
                InlineKlass* vklass, int offset) {
  int diff = offset - vklass->first_field_offset();
  const OopMapBlock* map = vklass->start_of_nonstatic_oop_maps();
  const OopMapBlock* last_map = map + vklass->nonstatic_oop_map_count();
  while (map < last_map) {
    nonstatic_oop_maps->add(map->offset() + diff, map->count());
    map++;
  }
}

void FieldLayoutBuilder::register_embedded_oops_from_list(OopMapBlocksBuilder* nonstatic_oop_maps, GrowableArray<LayoutRawBlock*>* list) {
  if (list == nullptr) return;
  for (int i = 0; i < list->length(); i++) {
    LayoutRawBlock* f = list->at(i);
    if (f->block_kind() == LayoutRawBlock::FLAT) {
      InlineKlass* vk = f->inline_klass();
      assert(vk != nullptr, "Should have been initialized");
      if (vk->contains_oops()) {
        add_flat_field_oopmap(nonstatic_oop_maps, vk, f->offset());
      }
    }
  }
}

void FieldLayoutBuilder::register_embedded_oops(OopMapBlocksBuilder* nonstatic_oop_maps, FieldGroup* group) {
  if (group->oop_fields() != nullptr) {
    for (int i = 0; i < group->oop_fields()->length(); i++) {
      LayoutRawBlock* b = group->oop_fields()->at(i);
      nonstatic_oop_maps->add(b->offset(), 1);
    }
  }
  register_embedded_oops_from_list(nonstatic_oop_maps, group->big_primitive_fields());
  register_embedded_oops_from_list(nonstatic_oop_maps, group->small_primitive_fields());
}

void FieldLayoutBuilder::epilogue() {
  // Computing oopmaps
  OopMapBlocksBuilder* nonstatic_oop_maps =
      new OopMapBlocksBuilder(_nonstatic_oopmap_count);
  int super_oop_map_count = (_super_klass == nullptr) ? 0 :_super_klass->nonstatic_oop_map_count();
  if (super_oop_map_count > 0) {
    nonstatic_oop_maps->initialize_inherited_blocks(_super_klass->start_of_nonstatic_oop_maps(),
    _super_klass->nonstatic_oop_map_count());
  }
  register_embedded_oops(nonstatic_oop_maps, _root_group);
  if (!_contended_groups.is_empty()) {
    for (int i = 0; i < _contended_groups.length(); i++) {
      FieldGroup* cg = _contended_groups.at(i);
      if (cg->oop_count() > 0) {
        assert(cg->oop_fields() != nullptr && cg->oop_fields()->at(0) != nullptr, "oop_count > 0 but no oop fields found");
        register_embedded_oops(nonstatic_oop_maps, cg);
      }
    }
  }
  nonstatic_oop_maps->compact();

  int instance_end = align_up(_layout->last_block()->offset(), wordSize);
  int static_fields_end = align_up(_static_layout->last_block()->offset(), wordSize);
  int static_fields_size = (static_fields_end -
      InstanceMirrorKlass::offset_of_static_fields()) / wordSize;
  int nonstatic_field_end = align_up(_layout->last_block()->offset(), heapOopSize);

  // Pass back information needed for InstanceKlass creation

  _info->oop_map_blocks = nonstatic_oop_maps;
  _info->_instance_size = align_object_size(instance_end / wordSize);
  _info->_static_field_size = static_fields_size;
  _info->_nonstatic_field_size = (nonstatic_field_end - instanceOopDesc::base_offset_in_bytes()) / heapOopSize;
  _info->_has_nonstatic_fields = _has_nonstatic_fields;
  _info->_has_inline_fields = _has_inline_type_fields;
  _info->_is_naturally_atomic = _is_naturally_atomic;
  if (_is_inline_type) {
    _info->_must_be_atomic = _must_be_atomic;
    _info->_payload_alignment = _payload_alignment;
    _info->_first_field_offset = _first_field_offset;
    _info->_payload_size_in_bytes = _payload_size_in_bytes;
    _info->_non_atomic_size_in_bytes = _non_atomic_layout_size_in_bytes;
    _info->_non_atomic_alignment = _non_atomic_layout_alignment;
    _info->_atomic_layout_size_in_bytes = _atomic_layout_size_in_bytes;
    _info->_nullable_layout_size_in_bytes = _nullable_layout_size_in_bytes;
    _info->_null_marker_offset = _null_marker_offset;
    _info->_default_value_offset = _static_layout->default_value_offset();
    _info->_null_reset_value_offset = _static_layout->null_reset_value_offset();
    _info->_is_empty_inline_klass = _is_empty_inline_class;
  }

  // This may be too restrictive, since if all the fields fit in 64
  // bits we could make the decision to align instances of this class
  // to 64-bit boundaries, and load and store them as single words.
  // And on machines which supported larger atomics we could similarly
  // allow larger values to be atomic, if properly aligned.

#ifdef ASSERT
  // Tests verifying integrity of field layouts are using the output of -XX:+PrintFieldLayout
  // which prints the details of LayoutRawBlocks used to compute the layout.
  // The code below checks that offsets in the _field_info meta-data match offsets
  // in the LayoutRawBlocks
  LayoutRawBlock* b = _layout->blocks();
  while(b != _layout->last_block()) {
    if (b->block_kind() == LayoutRawBlock::REGULAR || b->block_kind() == LayoutRawBlock::FLAT) {
      if (_field_info->adr_at(b->field_index())->offset() != (u4)b->offset()) {
        tty->print_cr("Offset from field info = %d, offset from block = %d", (int)_field_info->adr_at(b->field_index())->offset(), b->offset());
      }
      assert(_field_info->adr_at(b->field_index())->offset() == (u4)b->offset()," Must match");
    }
    b = b->next_block();
  }
  b = _static_layout->blocks();
  while(b != _static_layout->last_block()) {
    if (b->block_kind() == LayoutRawBlock::REGULAR || b->block_kind() == LayoutRawBlock::FLAT) {
      assert(_field_info->adr_at(b->field_index())->offset() == (u4)b->offset()," Must match");
    }
    b = b->next_block();
  }
#endif // ASSERT

  static bool first_layout_print = true;


  if (PrintFieldLayout || (PrintInlineLayout && _has_flattening_information)) {
    ResourceMark rm;
    stringStream st;
    if (first_layout_print) {
      st.print_cr("Field layout log format: @offset size/alignment [name] [signature] [comment]");
      st.print_cr("Heap oop size = %d", heapOopSize);
      first_layout_print = false;
    }
    if (_super_klass != nullptr) {
      st.print_cr("Layout of class %s@%p extends %s@%p", _classname->as_C_string(),
                    _loader_data, _super_klass->name()->as_C_string(), _super_klass->class_loader_data());
    } else {
      st.print_cr("Layout of class %s@%p", _classname->as_C_string(), _loader_data);
    }
    st.print_cr("Instance fields:");
    _layout->print(&st, false, _super_klass, _inline_layout_info_array);
    st.print_cr("Static fields:");
    _static_layout->print(&st, true, nullptr, _inline_layout_info_array);
    st.print_cr("Instance size = %d bytes", _info->_instance_size * wordSize);
    if (_is_inline_type) {
      st.print_cr("First field offset = %d", _first_field_offset);
      st.print_cr("Payload layout: %d/%d", _payload_size_in_bytes, _payload_alignment);
      if (has_non_atomic_flat_layout()) {
        st.print_cr("Non atomic flat layout: %d/%d", _non_atomic_layout_size_in_bytes, _non_atomic_layout_alignment);
      } else {
        st.print_cr("Non atomic flat layout: -/-");
      }
      if (has_atomic_layout()) {
        st.print_cr("Atomic flat layout: %d/%d", _atomic_layout_size_in_bytes, _atomic_layout_size_in_bytes);
      } else {
        st.print_cr("Atomic flat layout: -/-");
      }
      if (has_nullable_layout()) {
        st.print_cr("Nullable flat layout: %d/%d", _nullable_layout_size_in_bytes, _nullable_layout_size_in_bytes);
      } else {
        st.print_cr("Nullable flat layout: -/-");
      }
      if (_null_marker_offset != -1) {
        st.print_cr("Null marker offset = %d", _null_marker_offset);
      }
    }
    st.print_cr("---");
    // Print output all together.
    tty->print_raw(st.as_string());
  }
}

void FieldLayoutBuilder::build_layout() {
  if (_is_inline_type || _is_abstract_value) {
    compute_inline_class_layout();
  } else {
    compute_regular_layout();
  }
}
