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

LayoutRawBlock::LayoutRawBlock(Kind kind, int size) :
  _next_block(nullptr),
  _prev_block(nullptr),
  _inline_klass(nullptr),
  _kind(kind),
  _offset(-1),
  _alignment(1),
  _size(size),
  _field_index(-1),
  _is_reference(false),
  _multifield_index(-1) {
  assert(kind == EMPTY || kind == RESERVED || kind == PADDING || kind == INHERITED,
         "Otherwise, should use the constructor with a field index argument");
  assert(size > 0, "Sanity check");
}


LayoutRawBlock::LayoutRawBlock(int index, Kind kind, int size, int alignment, bool is_reference, jbyte multifield_index) :
 _next_block(nullptr),
 _prev_block(nullptr),
 _inline_klass(nullptr),
 _kind(kind),
 _offset(-1),
 _alignment(alignment),
 _size(size),
 _field_index(index),
 _is_reference(is_reference),
 _multifield_index(multifield_index) {
  assert(kind == REGULAR || kind == INLINED || kind == INHERITED || kind == MULTIFIELD,
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
  _multifields(nullptr),
  _contended_group(contended_group),  // -1 means no contended group, 0 means default contended group
  _oop_count(0) {}

void FieldGroup::add_primitive_field(int idx, BasicType type) {
  int size = type2aelembytes(type);
  LayoutRawBlock* block = new LayoutRawBlock(idx, LayoutRawBlock::REGULAR, size, size /* alignment == size for primitive types */, false, -1);
  if (size >= oopSize) {
    add_to_big_primitive_list(block);
  } else {
    add_to_small_primitive_list(block);
  }
}

void FieldGroup::add_oop_field(int idx) {
  int size = type2aelembytes(T_OBJECT);
  LayoutRawBlock* block = new LayoutRawBlock(idx, LayoutRawBlock::REGULAR, size, size /* alignment == size for oops */, true, -1);
  if (_oop_fields == nullptr) {
    _oop_fields = new GrowableArray<LayoutRawBlock*>(INITIAL_LIST_SIZE);
  }
  _oop_fields->append(block);
  _oop_count++;
}

void FieldGroup::add_inlined_field(int idx, InlineKlass* vk) {
  LayoutRawBlock* block = new LayoutRawBlock(idx, LayoutRawBlock::INLINED, vk->get_exact_size_in_bytes(), vk->get_alignment(), false, -1);
  block->set_inline_klass(vk);
  if (block->size() >= oopSize) {
    add_to_big_primitive_list(block);
  } else {
    add_to_small_primitive_list(block);
  }
}

void FieldGroup::add_multifield(ConstantPool* cp, FieldInfo* field, Array<MultiFieldInfo>* multifield_info, InlineKlass* vk) {
  assert(field->is_multifield() || field->is_multifield_base(), "Must be");
  u2 base = field->multifield_base(multifield_info);
  BasicType type = Signature::basic_type(field->signature(cp));
  MultiFieldGroup* mfg = NULL;
  if (_multifields != NULL) {
    for (int i = 0; i < _multifields->length(); i++) {
      if (base == _multifields->at(i)->multifield_base()) {
        mfg = _multifields->at(i);
        break;
      }
    }
  } else {
    _multifields = new GrowableArray<MultiFieldGroup*>(INITIAL_LIST_SIZE);
  }
  if (mfg == NULL) {
    mfg = new MultiFieldGroup(base, field->signature(cp));
    mfg->add_field(cp, field, vk, multifield_info);
    _multifields->append(mfg);
  } else {
    mfg->add_field(cp, field, vk, multifield_info);
  }
}

void FieldGroup::sort_by_size() {
  if (_small_primitive_fields != nullptr) {
    _small_primitive_fields->sort(LayoutRawBlock::compare_size_inverted);
  }
  if (_big_primitive_fields != nullptr) {
    _big_primitive_fields->sort(LayoutRawBlock::compare_size_inverted);
  }
  if (_multifields != NULL) {
    for (int i = 0; i < _multifields->length(); i++) {
      int size = _multifields->at(i)->fields()->at(0)->size();
      int alignment =  _multifields->at(i)->fields()->at(0)->alignment();
      int multialignment = size/alignment;
      if (size % multialignment != 0) multialignment+= alignment;
      // Problem: at this point, multialignment can be a weird number, for instance not a power of two
      int multisize = multialignment * alignment * _multifields->at(i)->fields()->length();  // should be stored
      // True alignment should be adjusted to increase chances to be on a single cache line
      int multifield_group_alignment = MIN2(round_up_power_of_2(multisize), HeapWordSize);
      _multifields->at(i)->set_group_size(multisize);
      _multifields->at(i)->set_group_alignment(multifield_group_alignment);
      _multifields->at(i)->fields()->sort(MultiFieldGroup::compare_multifield_index);
    }
    _multifields->sort(MultiFieldGroup::compare_multifield_groups_inverted);
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

MultiFieldGroup::MultiFieldGroup(u2 base, Symbol* signature) :  _multifield_base(base), _signature(signature), _fields(NULL) { }

void MultiFieldGroup::add_field(ConstantPool* cp, FieldInfo* field, InlineKlass* vk, Array<MultiFieldInfo>* multifield_info) {
  assert(field->is_multifield() || field->is_multifield_base(), "Must be");
  assert(field->multifield_base(multifield_info) == multifield_base(), "multifield base mismatch");
  if (_fields == NULL) {
    _fields = new GrowableArray<LayoutRawBlock*>(INITIAL_LIST_SIZE);
  } else {
    guarantee(field->signature(cp) == signature(), "multifield signature mismatch");
  }
  BasicType type = Signature::basic_type(field->signature(cp));
  LayoutRawBlock* block = NULL;
  if (type == T_PRIMITIVE_OBJECT) {
    block = new LayoutRawBlock(field->index(), LayoutRawBlock::MULTIFIELD, vk->get_exact_size_in_bytes(),
                               vk->get_alignment(), false, field->multifield_index(multifield_info));
  } else {
    int size = type2aelembytes(type);
    block = new LayoutRawBlock(field->index(), LayoutRawBlock::MULTIFIELD, size, size, false, field->multifield_index(multifield_info));
  }
  int cursor = 0;
  for (; cursor < _fields->length(); cursor++) {
    assert(_fields->at(cursor)->multifield_index() != field->multifield_index(multifield_info), "multifield index duplicate found");
    if (_fields->at(cursor)->multifield_index() > field->multifield_index(multifield_info)) break;
  }
  _fields->insert_before(cursor, block);
}

FieldLayout::FieldLayout(GrowableArray<FieldInfo>* field_info, ConstantPool* cp, Array<MultiFieldInfo>* multifields) :
  _field_info(field_info),
  _cp(cp),
  _multifield_info(multifields),
  _blocks(nullptr),
  _start(_blocks),
  _last(_blocks) {}

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
    bool has_fields = reconstruct_layout(super_klass);
    fill_holes(super_klass);
    if ((UseEmptySlotsInSupers && !super_klass->has_contended_annotations()) || !has_fields) {
      _start = _blocks; // Setting _start to _blocks instead of _last would allow subclasses
      // to allocate fields in empty slots of their super classes
    } else {
      _start = _last;    // append fields at the end of the reconstructed layout
    }
  }
}

LayoutRawBlock* FieldLayout::first_field_block() {
  LayoutRawBlock* block = _blocks;
  while (block != nullptr
         && block->kind() != LayoutRawBlock::INHERITED
         && block->kind() != LayoutRawBlock::REGULAR
         && block->kind() != LayoutRawBlock::INLINED
         && block->kind() != LayoutRawBlock::MULTIFIELD) {
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
        if (cursor->kind() == LayoutRawBlock::EMPTY && cursor->fit(b->size(), b->alignment())) {
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
      assert(candidate->kind() == LayoutRawBlock::EMPTY, "Candidate must be an empty block");
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
      assert(slot->kind() == LayoutRawBlock::EMPTY, "Matching slot must be an empty slot");
      assert(slot->size() >= block->offset() + block->size() ,"Matching slot must be big enough");
      if (slot->offset() < block->offset()) {
        int adjustment = block->offset() - slot->offset();
        LayoutRawBlock* adj = new LayoutRawBlock(LayoutRawBlock::EMPTY, adjustment);
        insert(slot, adj);
      }
      insert(slot, block);
      if (slot->size() == 0) {
        remove(slot);
      }
      _field_info->adr_at(block->field_index())->set_offset(block->offset());
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
    while (candidate->kind() != LayoutRawBlock::EMPTY || !candidate->fit(size, first->alignment())) {
      if (candidate == start) {
        candidate = last_block();
        break;
      }
      candidate = candidate->prev_block();
    }
    assert(candidate != nullptr, "Candidate must not be null");
    assert(candidate->kind() == LayoutRawBlock::EMPTY, "Candidate must be an empty block");
    assert(candidate->fit(size, first->alignment()), "Candidate must be able to store the whole contiguous block");
  }

  for (int i = 0; i < list->length(); i++) {
    LayoutRawBlock* b = list->at(i);
    insert_field_block(candidate, b);
    assert((candidate->offset() % b->alignment() == 0), "Contiguous blocks must be naturally well aligned");
  }
}

void FieldLayout::add_multifield(MultiFieldGroup* multifield_group, LayoutRawBlock* start) {
  if (start == NULL) {
    start = _start;
  }
  LayoutRawBlock* candidate = NULL;
  if (start == last_block()) {
    candidate = last_block();
  } else {
    candidate = last_block()->prev_block();
    while (candidate->kind() != LayoutRawBlock::EMPTY || !candidate->fit(multifield_group->group_size(), multifield_group->group_alignment())) {
      if (candidate == start) {
        candidate = last_block();
        break;
      }
      candidate = candidate->prev_block();
    }
    assert(candidate != NULL, "Candidate must not be null");
    assert(candidate->kind() == LayoutRawBlock::EMPTY, "Candidate must be an empty block");
  }
  if ((candidate->offset() % multifield_group->group_alignment()) != 0) {
    int adjustment = multifield_group->group_alignment() - (candidate->offset() % multifield_group->group_alignment());
    LayoutRawBlock* adj = new LayoutRawBlock(LayoutRawBlock::EMPTY, adjustment);
    insert(candidate, adj);
  }
  assert(candidate->fit(multifield_group->group_size(), multifield_group->group_alignment()), "Candidate must be able to store the whole contiguous block");
  for (int i = 0; i < multifield_group->fields()->length(); i++) {
    LayoutRawBlock* b = multifield_group->fields()->at(i);
    insert_field_block(candidate, b);
  }
}

LayoutRawBlock* FieldLayout::insert_field_block(LayoutRawBlock* slot, LayoutRawBlock* block) {
  assert(slot->kind() == LayoutRawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
  if (slot->offset() % block->alignment() != 0) {
    int adjustment = block->alignment() - (slot->offset() % block->alignment());
    LayoutRawBlock* adj = new LayoutRawBlock(LayoutRawBlock::EMPTY, adjustment);
    insert(slot, adj);
  }
  insert(slot, block);
  if (slot->size() == 0) {
    remove(slot);
  }
  _field_info->adr_at(block->field_index())->set_offset(block->offset());
  return block;
}

bool FieldLayout::reconstruct_layout(const InstanceKlass* ik) {
  bool has_instance_fields = false;
  GrowableArray<LayoutRawBlock*>* all_fields = new GrowableArray<LayoutRawBlock*>(32);
  while (ik != nullptr) {
    for (AllFieldStream fs(ik->fieldinfo_stream(), ik->constants(), ik->multifield_info()); !fs.done(); fs.next()) {
      BasicType type = Signature::basic_type(fs.signature());
      // distinction between static and non-static fields is missing
      if (fs.access_flags().is_static()) continue;
      has_instance_fields = true;
      LayoutRawBlock* block;
      if (type == T_PRIMITIVE_OBJECT) {
        InlineKlass* vk = InlineKlass::cast(ik->get_inline_type_field_klass(fs.index()));
        block = new LayoutRawBlock(fs.index(), LayoutRawBlock::INHERITED, vk->get_exact_size_in_bytes(),
                                   vk->get_alignment(), false, -1);

      } else {
        int size = type2aelembytes(type);
        // INHERITED blocks are marked as non-reference because oop_maps are handled by their holder class
        block = new LayoutRawBlock(fs.index(), LayoutRawBlock::INHERITED, size, size, false, -1);
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
  assert(b->kind() != LayoutRawBlock::EMPTY, "Sanity check");
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
  if (!UseEmptySlotsInSupers) {
    // Add an empty slots to align fields of the subclass on a heapOopSize boundary
    // in order to emulate the behavior of the previous algorithm
    int align = (b->offset() + b->size()) % heapOopSize;
    if (align != 0) {
      int sz = heapOopSize - align;
      LayoutRawBlock* p = new LayoutRawBlock(LayoutRawBlock::EMPTY, sz);
      p->set_offset(b->offset() + b->size());
      b->set_next_block(p);
      p->set_prev_block(b);
      b = p;
    }
  }
  LayoutRawBlock* last = new LayoutRawBlock(LayoutRawBlock::EMPTY, INT_MAX);
  last->set_offset(b->offset() + b->size());
  assert(last->offset() > 0, "Sanity check");
  b->set_next_block(last);
  last->set_prev_block(b);
  _last = last;
}

LayoutRawBlock* FieldLayout::insert(LayoutRawBlock* slot, LayoutRawBlock* block) {
  assert(slot->kind() == LayoutRawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
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

void FieldLayout::print(outputStream* output, bool is_static, const InstanceKlass* super) {
  ResourceMark rm;
  LayoutRawBlock* b = _blocks;
  while(b != _last) {
    switch(b->kind()) {
      case LayoutRawBlock::REGULAR: {
        FieldInfo* fi = _field_info->adr_at(b->field_index());
        output->print_cr(" @%d \"%s\" %s %d/%d %s",
                         b->offset(),
                         fi->name(_multifield_info, _cp)->as_C_string(),
                         fi->signature(_cp)->as_C_string(),
                         b->size(),
                         b->alignment(),
                         "REGULAR");
        break;
      }
      case LayoutRawBlock::INLINED: {
        FieldInfo* fi = _field_info->adr_at(b->field_index());
        output->print_cr(" @%d \"%s\" %s %d/%d %s",
                         b->offset(),
                         fi->name(_multifield_info, _cp)->as_C_string(),
                         fi->signature(_cp)->as_C_string(),
                         b->size(),
                         b->alignment(),
                         "INLINED");
        break;
      }
      case LayoutRawBlock::RESERVED: {
        output->print_cr(" @%d %d/- %s",
                         b->offset(),
                         b->size(),
                         "RESERVED");
        break;
      }
      case LayoutRawBlock::INHERITED: {
        assert(!is_static, "Static fields are not inherited in layouts");
        assert(super != nullptr, "super klass must be provided to retrieve inherited fields info");
        bool found = false;
        const InstanceKlass* ik = super;
        while (!found && ik != nullptr) {
          for (AllFieldStream fs(ik->fieldinfo_stream(), ik->constants(), ik->multifield_info()); !fs.done(); fs.next()) {
            if (fs.offset() == b->offset()) {
              output->print_cr(" @%d \"%s\" %s %d/%d %s",
                  b->offset(),
                  fs.name()->as_C_string(),
                  fs.signature()->as_C_string(),
                  b->size(),
                  b->size(), // so far, alignment constraint == size, will change with Valhalla
                  "INHERITED");
              found = true;
              break;
            }
          }
        }
        ik = ik->java_super();
        break;
      }
      case LayoutRawBlock::EMPTY:
        output->print_cr(" @%d %d/1 %s",
                         b->offset(),
                         b->size(),
                         "EMPTY");
        break;
      case LayoutRawBlock::PADDING:
        output->print_cr(" @%d %d/1 %s",
                         b->offset(),
                         b->size(),
                         "PADDING");
        break;
      case LayoutRawBlock::MULTIFIELD:
        FieldInfo* fi = _field_info->adr_at(b->field_index());
        output->print_cr(" @%d \"%s\" %s %d/%d %s",
                         b->offset(),
                         fi->name(_multifield_info, _cp)->as_C_string(),
                         fi->signature(_cp)->as_C_string(),
                         b->size(),
                         b->alignment(),
                         "MULTIFIELD");
        break;
    }
    b = b->next_block();
  }
}

FieldLayoutBuilder::FieldLayoutBuilder(const Symbol* classname, const InstanceKlass* super_klass, ConstantPool* constant_pool,
                                       GrowableArray<FieldInfo>* field_info, bool is_contended, bool is_inline_type,
                                       FieldLayoutInfo* info, Array<InlineKlass*>* inline_type_field_klasses,
                                       Array<MultiFieldInfo>* multifields) :
  _classname(classname),
  _super_klass(super_klass),
  _constant_pool(constant_pool),
  _field_info(field_info),
  _info(info),
  _inline_type_field_klasses(inline_type_field_klasses),
  _multifield_info(multifields),
  _root_group(nullptr),
  _contended_groups(GrowableArray<FieldGroup*>(8)),
  _static_fields(nullptr),
  _layout(nullptr),
  _static_layout(nullptr),
  _nonstatic_oopmap_count(0),
  _alignment(-1),
  _first_field_offset(-1),
  _exact_size_in_bytes(-1),
  _has_nonstatic_fields(false),
  _has_inline_type_fields(false),
  _is_contended(is_contended),
  _is_inline_type(is_inline_type),
  _has_flattening_information(is_inline_type),
  _has_nonatomic_values(false),
  _atomic_field_count(0)
 {}

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
  _layout = new FieldLayout(_field_info, _constant_pool, _multifield_info);
  const InstanceKlass* super_klass = _super_klass;
  _layout->initialize_instance_layout(super_klass);
  if (super_klass != nullptr) {
    _has_nonstatic_fields = super_klass->has_nonstatic_fields();
  }
  _static_layout = new FieldLayout(_field_info, _constant_pool, _multifield_info);
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
    FieldInfo ctrl = _field_info->at(0);
    FieldGroup* group = nullptr;
    FieldInfo fieldinfo = *it;
    if (fieldinfo.access_flags().is_static()) {
      group = _static_fields;
    } else {
      _has_nonstatic_fields = true;
      _atomic_field_count++;  // we might decrement this
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
    bool has_multifield_annotation = false;
    if (fieldinfo.is_multifield() || fieldinfo.is_multifield_base()) {
      group->add_multifield(_constant_pool, &fieldinfo, _multifield_info);
    } else {
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
        if (group != _static_fields) _nonstatic_oopmap_count++;
        group->add_oop_field(idx);
        break;
      case T_PRIMITIVE_OBJECT:
        _has_inline_type_fields = true;
        if (group == _static_fields) {
          // static fields are never inlined
          group->add_oop_field(idx);
        } else {
          _has_flattening_information = true;
          // Flattening decision to be taken here
          // This code assumes all verification already have been performed
          // (field's type has been loaded and it is an inline klass)
          JavaThread* THREAD = JavaThread::current();
          Klass* klass =  _inline_type_field_klasses->at(idx);
          assert(klass != nullptr, "Sanity check");
          InlineKlass* vk = InlineKlass::cast(klass);
          bool too_big_to_flatten = (InlineFieldMaxFlatSize >= 0 &&
                                     (vk->size_helper() * HeapWordSize) > InlineFieldMaxFlatSize);
          bool too_atomic_to_flatten = vk->is_declared_atomic() || AlwaysAtomicAccesses;
          bool too_volatile_to_flatten = fieldinfo.access_flags().is_volatile();
          if (vk->is_naturally_atomic()) {
            too_atomic_to_flatten = false;
            //too_volatile_to_flatten = false; //FIXME
            // volatile fields are currently never inlined, this could change in the future
          }
          if (!(too_big_to_flatten | too_atomic_to_flatten | too_volatile_to_flatten) || fieldinfo.access_flags().is_final()) {
            group->add_inlined_field(idx, vk);
            _nonstatic_oopmap_count += vk->nonstatic_oop_map_count();
            _field_info->adr_at(idx)->field_flags_addr()->update_inlined(true);
            if (!vk->is_atomic()) {  // flat and non-atomic: take note
              _has_nonatomic_values = true;
              _atomic_field_count--;  // every other field is atomic but this one
            }
          } else {
            _nonstatic_oopmap_count++;
            group->add_oop_field(idx);
          }
        }
        break;
      default:
        fatal("Something wrong?");
      }
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
 *     currently only based in the size of the fields to be inlined, the size
 *     of the resulting instance is not considered)
 */
void FieldLayoutBuilder::inline_class_field_sorting(TRAPS) {
  assert(_is_inline_type, "Should only be used for inline classes");
  int alignment = 1;
  for (GrowableArrayIterator<FieldInfo> it = _field_info->begin(); it != _field_info->end(); ++it) {
    FieldGroup* group = nullptr;
    FieldInfo fieldinfo = *it;
    int field_alignment = 1;
    if (fieldinfo.access_flags().is_static()) {
      group = _static_fields;
    } else {
      _has_nonstatic_fields = true;
      _atomic_field_count++;  // we might decrement this
      group = _root_group;
    }
    bool has_multifield_annotation = false;
    if (fieldinfo.is_multifield() || fieldinfo.is_multifield_base()) {
      group->add_multifield(_constant_pool, &fieldinfo, _multifield_info);
    } else {
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
        if (group != _static_fields) {
          _nonstatic_oopmap_count++;
          field_alignment = type2aelembytes(type); // alignment == size for oops
        }
        group->add_oop_field(fieldinfo.index());
        break;
      case T_PRIMITIVE_OBJECT: {
  //      fs.set_inline(true);
        _has_inline_type_fields = true;
        if (group == _static_fields) {
          // static fields are never inlined
          group->add_oop_field(fieldinfo.index());
        } else {
          // Flattening decision to be taken here
          // This code assumes all verifications have already been performed
          // (field's type has been loaded and it is an inline klass)
          JavaThread* THREAD = JavaThread::current();
          Klass* klass =  _inline_type_field_klasses->at(fieldinfo.index());
          assert(klass != nullptr, "Sanity check");
          InlineKlass* vk = InlineKlass::cast(klass);
          bool too_big_to_flatten = (InlineFieldMaxFlatSize >= 0 &&
                                     (vk->size_helper() * HeapWordSize) > InlineFieldMaxFlatSize);
          bool too_atomic_to_flatten = vk->is_declared_atomic() || AlwaysAtomicAccesses;
          bool too_volatile_to_flatten = fieldinfo.access_flags().is_volatile();
          if (vk->is_naturally_atomic()) {
            too_atomic_to_flatten = false;
            //too_volatile_to_flatten = false; //FIXME
            // volatile fields are currently never inlined, this could change in the future
          }
          if (!(too_big_to_flatten | too_atomic_to_flatten | too_volatile_to_flatten) || fieldinfo.access_flags().is_final()) {
            group->add_inlined_field(fieldinfo.index(), vk);
            _nonstatic_oopmap_count += vk->nonstatic_oop_map_count();
            field_alignment = vk->get_alignment();
            _field_info->adr_at(fieldinfo.index())->field_flags_addr()->update_inlined(true);
            if (!vk->is_atomic()) {  // flat and non-atomic: take note
              _has_nonatomic_values = true;
              _atomic_field_count--;  // every other field is atomic but this one
            }
          } else {
            _nonstatic_oopmap_count++;
            field_alignment = type2aelembytes(T_OBJECT);
            group->add_oop_field(fieldinfo.index());
          }
        }
        break;
      }
      default:
        fatal("Unexpected BasicType");
      }
      if (!fieldinfo.access_flags().is_static() && field_alignment > alignment) alignment = field_alignment;
    }
  }
  _root_group->sort_by_size();
  if (_root_group->multifields() != NULL) {
    for (int i = 0; i < _root_group->multifields()->length(); i++) {
      if (_root_group->multifields()->at(i)->group_alignment() > alignment) {
        alignment = _root_group->multifields()->at(i)->group_alignment();
      }
    }
  }
  _alignment = alignment;
  if (!_has_nonstatic_fields) {
    // There are a number of fixes required throughout the type system and JIT
    Exceptions::fthrow(THREAD_AND_LOCATION,
                       vmSymbols::java_lang_ClassFormatError(),
                       "Value Types do not support zero instance size yet");
    return;
  }
}

void FieldLayoutBuilder::insert_contended_padding(LayoutRawBlock* slot) {
  if (ContendedPaddingWidth > 0) {
    LayoutRawBlock* padding = new LayoutRawBlock(LayoutRawBlock::PADDING, ContendedPaddingWidth);
    _layout->insert(slot, padding);
  }
}

/* Computation of regular classes layout is an evolution of the previous default layout
 * (FieldAllocationStyle 1):
 *   - primitive fields (both primitive types and flattened inline types) are allocated
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
  if (_root_group->multifields() != NULL) {
    for (int i = 0; i < _root_group->multifields()->length(); i++) {
      _layout->add_multifield(_root_group->multifields()->at(i));
    }
  }
  _layout->add(_root_group->big_primitive_fields());
  _layout->add(_root_group->small_primitive_fields());
  _layout->add(_root_group->oop_fields());

  if (!_contended_groups.is_empty()) {
    for (int i = 0; i < _contended_groups.length(); i++) {
      FieldGroup* cg = _contended_groups.at(i);
      LayoutRawBlock* start = _layout->last_block();
      insert_contended_padding(start);
      if (cg->multifields() != NULL) {
        for (int i = 0; i < cg->multifields()->length(); i++) {
          _layout->add_multifield(cg->multifields()->at(i), start);
        }
      }
      _layout->add(cg->big_primitive_fields(), start);
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
  if (_static_fields->multifields() != NULL) {
    for (int i = 0; i < _static_fields->multifields()->length(); i++) {
      _layout->add_multifield(_static_fields->multifields()->at(i));
    }
  }
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
 *   - big primitive fields (primitive types and flattened inline type smaller
 *     than an oop) are allocated first (from the biggest to the smallest)
 *   - then oop fields
 *   - then small primitive fields (from the biggest to the smallest)
 */
void FieldLayoutBuilder::compute_inline_class_layout(TRAPS) {
  prologue();
  inline_class_field_sorting(CHECK);
  // Inline types are not polymorphic, so they cannot inherit fields.
  // By consequence, at this stage, the layout must be composed of a RESERVED
  // block, followed by an EMPTY block.
  assert(_layout->start()->kind() == LayoutRawBlock::RESERVED, "Unexpected");
  assert(_layout->start()->next_block()->kind() == LayoutRawBlock::EMPTY, "Unexpected");
  LayoutRawBlock* first_empty = _layout->start()->next_block();
  if (first_empty->offset() % _alignment != 0) {
    LayoutRawBlock* padding = new LayoutRawBlock(LayoutRawBlock::PADDING, _alignment - (first_empty->offset() % _alignment));
    _layout->insert(first_empty, padding);
    _layout->set_start(padding->next_block());
  }

  if (_root_group->multifields() != NULL) {
    for (int i = 0; i < _root_group->multifields()->length(); i++) {
      _layout->add_multifield(_root_group->multifields()->at(i));
    }
  }
  _layout->add(_root_group->big_primitive_fields());
  _layout->add(_root_group->oop_fields());
  _layout->add(_root_group->small_primitive_fields());

  LayoutRawBlock* first_field = _layout->first_field_block();
   if (first_field != nullptr) {
     _first_field_offset = _layout->first_field_block()->offset();
     _exact_size_in_bytes = _layout->last_block()->offset() - _layout->first_field_block()->offset();
   } else {
     // special case for empty value types
     _first_field_offset = _layout->blocks()->size();
     _exact_size_in_bytes = 0;
   }

  // Warning:: InstanceMirrorKlass expects static oops to be allocated first
  _static_layout->add_contiguously(_static_fields->oop_fields());
  _static_layout->add(_static_fields->big_primitive_fields());
  _static_layout->add(_static_fields->small_primitive_fields());

  epilogue();
}

void FieldLayoutBuilder::add_inlined_field_oopmap(OopMapBlocksBuilder* nonstatic_oop_maps,
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
  if (list != nullptr) {
    for (int i = 0; i < list->length(); i++) {
      LayoutRawBlock* f = list->at(i);
      if (f->kind() == LayoutRawBlock::INLINED) {
        InlineKlass* vk = f->inline_klass();
        assert(vk != nullptr, "Should have been initialized");
        if (vk->contains_oops()) {
          add_inlined_field_oopmap(nonstatic_oop_maps, vk, f->offset());
        }
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
  int super_oop_map_count = (_super_klass == nullptr) ? 0 :_super_klass->nonstatic_oop_map_count();
  int max_oop_map_count = super_oop_map_count + _nonstatic_oopmap_count;
  OopMapBlocksBuilder* nonstatic_oop_maps =
      new OopMapBlocksBuilder(max_oop_map_count);
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

  // An inline type is naturally atomic if it has just one field, and
  // that field is simple enough.
  _info->_is_naturally_atomic = (_is_inline_type &&
                                 (_atomic_field_count <= 1) &&
                                 !_has_nonatomic_values &&
                                 _contended_groups.is_empty());
  // This may be too restrictive, since if all the fields fit in 64
  // bits we could make the decision to align instances of this class
  // to 64-bit boundaries, and load and store them as single words.
  // And on machines which supported larger atomics we could similarly
  // allow larger values to be atomic, if properly aligned.


  if (PrintFieldLayout || (PrintInlineLayout && _has_flattening_information)) {
    ResourceMark rm;
    tty->print_cr("Layout of class %s", _classname->as_C_string());
    tty->print_cr("Instance fields:");
    _layout->print(tty, false, _super_klass);
    tty->print_cr("Static fields:");
    _static_layout->print(tty, true, nullptr);
    tty->print_cr("Instance size = %d bytes", _info->_instance_size * wordSize);
    if (_is_inline_type) {
      tty->print_cr("First field offset = %d", _first_field_offset);
      tty->print_cr("Alignment = %d bytes", _alignment);
      tty->print_cr("Exact size = %d bytes", _exact_size_in_bytes);
    }
    tty->print_cr("---");
  }
}

void FieldLayoutBuilder::build_layout(TRAPS) {
  if (_is_inline_type) {
    compute_inline_class_layout(CHECK);
  } else {
    compute_regular_layout();
  }
}
