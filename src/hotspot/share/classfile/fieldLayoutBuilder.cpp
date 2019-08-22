/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "classfile/classFileParser.hpp"
#include "classfile/fieldLayoutBuilder.hpp"
#include "memory/resourceArea.hpp"
#include "oops/array.hpp"
#include "oops/instanceMirrorKlass.hpp"
#include "oops/valueKlass.hpp"
#include "runtime/fieldDescriptor.inline.hpp"

RawBlock::RawBlock(Kind kind, int size) {
  assert(kind == EMPTY || kind == RESERVED || kind == PADDING || kind == INHERITED,
      "Otherwise, should use the constructor with a field index argument");
  assert(size > 0, "Sanity check");
  _next_field = NULL;
  _prev_field = NULL;
  _next_block = NULL;
  _prev_block = NULL;
  _field_index = -1; // no field
    _kind = kind;
  _size = size;
  _alignment = 1;
  _offset = -1;
  _is_reference = false;
  _value_klass = NULL;
}

RawBlock::RawBlock(int index, Kind kind, int size, int alignment, bool is_reference) {
  assert(kind == REGULAR || kind == FLATTENED || kind == INHERITED,
      "Other kind do not have a field index");
  assert(size > 0, "Sanity check");
  assert(alignment > 0, "Sanity check");
  _next_field = NULL;
  _prev_field = NULL;
  _next_block = NULL;
  _prev_block = NULL;
  _field_index = index;
  _kind = kind;
  _size = size;
  _alignment = alignment;
  _offset = -1;
  _is_reference = is_reference;
  _value_klass = NULL;
}

bool RawBlock::fit(int size, int alignment) {
  int adjustment = _offset % alignment;
  return _size >= size + adjustment;
}

FieldGroup::FieldGroup(int contended_group) {
  _next = NULL;
  _primitive_fields = NULL;
  _oop_fields = NULL;
  _flattened_fields = NULL;
  _contended_group = contended_group; // -1 means no contended group, 0 means default contended group
  _oop_count = 0;
}

void FieldGroup::add_primitive_field(AllFieldStream fs, BasicType type) {
  int size = type2aelembytes(type);
  RawBlock* block = new RawBlock(fs.index(), RawBlock::REGULAR, size, size /* alignment == size for primitive types */, false);
  add_block(&_primitive_fields, block);
}

void FieldGroup::add_oop_field(AllFieldStream fs) {
  int size = type2aelembytes(T_OBJECT);
  RawBlock* block = new RawBlock(fs.index(), RawBlock::REGULAR, size, size /* alignment == size for oops */, true);
  add_block(&_oop_fields, block);
  _oop_count++;
}

void FieldGroup::add_flattened_field(AllFieldStream fs, ValueKlass* vk) {
  // _flattened_fields list might be merged with the _primitive_fields list in the future
  RawBlock* block = new RawBlock(fs.index(), RawBlock::FLATTENED, vk->get_exact_size_in_bytes(), vk->get_alignment(), false);
  block->set_value_klass(vk);
  add_block(&_flattened_fields, block);
}

/* Adds a field to a field group. Inside a field group, fields are sorted by
 * decreasing sizes. Fields with the same size are sorted according to their
 * order of insertion (easy hack to respect field order for classes with
 * hard coded offsets).
 */
void FieldGroup::add_block(RawBlock** list, RawBlock* block) {
  if (*list == NULL) {
    *list = block;
  } else {
    if (block->size() > (*list)->size()) {  // cannot be >= to respect order of field (for classes with hard coded offsets)
      block->set_next_field(*list);
      (*list)->set_prev_field(block);
      *list = block;
    } else {
      RawBlock* b = *list;
      while (b->next_field() != NULL) {
        if (b->next_field()->size() < block->size()) {
          break;
        }
        b = b->next_field();
      }
      block->set_next_field(b->next_field());
      block->set_prev_field(b);
      b->set_next_field(block);
      if (b->next_field() != NULL) {
        b->next_field()->set_prev_field(block);
      }
    }
  }
}

FieldLayout::FieldLayout(Array<u2>* fields, ConstantPool* cp) {
  _fields = fields;
  _cp = cp;
  _blocks = NULL;
  _start = _blocks;
  _last = _blocks;
}

void FieldLayout::initialize_static_layout() {
  _blocks = new RawBlock(RawBlock::EMPTY, INT_MAX);
  _blocks->set_offset(0);
  _last = _blocks;
  _start = _blocks;
  // Note: at this stage, InstanceMirrorKlass::offset_of_static_fields() could be zero, because
  // during bootstrapping, the size of the java.lang.Class is still not known when layout
  // of static field is computed. Field offsets are fixed later when the size is known
  // (see java_lang_Class::fixup_mirror())
  if (InstanceMirrorKlass::offset_of_static_fields() > 0) {
    insert(first_empty_block(), new RawBlock(RawBlock::RESERVED, InstanceMirrorKlass::offset_of_static_fields()));
    _blocks->set_offset(0);
  }
}

void FieldLayout::initialize_instance_layout(const InstanceKlass* super_klasss) {
  if (super_klasss == NULL) {
    _blocks = new RawBlock(RawBlock::EMPTY, INT_MAX);
    _blocks->set_offset(0);
    _last = _blocks;
    _start = _blocks;
    insert(first_empty_block(), new RawBlock(RawBlock::RESERVED, instanceOopDesc::base_offset_in_bytes()));
  } else {
    // The JVM could reconstruct the layouts of the super classes, in order to use the
    // empty slots in these layouts to allocate current class' fields. However, some codes
    // in the JVM are not ready yet to find fields allocated this way, so the optimization
    // is not enabled yet.
#if 0
    reconstruct_layout(super_klasss);
    fill_holes(super_klasss);
    // _start = _last;  // uncomment to fill holes in super classes layouts
#else
    _blocks = new RawBlock(RawBlock::EMPTY, INT_MAX);
    _blocks->set_offset(0);
    _last = _blocks;
    insert(_last, new RawBlock(RawBlock::RESERVED, instanceOopDesc::base_offset_in_bytes()));
    if (super_klasss->nonstatic_field_size() > 0) {
      // To take into account the space allocated to super classes' fields, this code
      // uses the nonstatic_field_size() value to allocate a single INHERITED RawBlock.
      // The drawback is that nonstatic_field_size() expresses the size of non-static
      // fields in heapOopSize, which implies that some space could be lost at the
      // end because of the rounding up of the real size. Using the exact size, with
      // no rounding up, would be possible, but would require modifications to other
      // codes in the JVM performing fields lookup (as they often expect this rounding
      // to be applied).
      RawBlock* inherited = new RawBlock(RawBlock::INHERITED,
          super_klasss->nonstatic_field_size() * heapOopSize);
      insert(_last, inherited);
    }
    _start = _last;
#endif
  }
}

RawBlock* FieldLayout::first_field_block() {
  RawBlock* block = _start;
  // Not sure the condition below will work well when inheriting layout with contented padding
  while (block->kind() != RawBlock::INHERITED && block->kind() != RawBlock::REGULAR
      && block->kind() != RawBlock::FLATTENED && block->kind() != RawBlock::PADDING) {
    block = block->next_block();
  }
  return block;
}

/* The allocation logic uses a first fit strategy: the field is allocated in the
 * first empty slot big enough to contain it (including padding to fit alignment
 * constraints).
 */
void FieldLayout::add(RawBlock* blocks, RawBlock* start) {
  if (start == NULL) {
    // start = this->_blocks;
    start = this->_start;
  }
  RawBlock* b = blocks;
  RawBlock* candidate = NULL;
  while (b != NULL) {
    RawBlock* candidate = start;
    while (candidate->kind() != RawBlock::EMPTY || !candidate->fit(b->size(), b->alignment())) candidate = candidate->next_block();
    assert(candidate != NULL && candidate->fit(b->size(), b->alignment()), "paranoid check");
    insert_field_block(candidate, b);
    b = b->next_field();
  }
}

/* The allocation logic uses a first fit strategy: the set of fields is allocated
 * in the first empty slot big enough to contain the whole set ((including padding
 * to fit alignment constraints).
 */
void FieldLayout::add_contiguously(RawBlock* blocks, RawBlock* start) {
  if (blocks == NULL) return;
  if (start == NULL) {
    start = _start;
  }
  // This code assumes that if the first block is well aligned, the following
  // blocks would naturally be well aligned (no need for adjustment)
  int size = 0;
  RawBlock* b = blocks;
  while (b != NULL) {
    size += b->size();
    b = b->next_field();
  }
  RawBlock* candidate = start;
  while (candidate->kind() != RawBlock::EMPTY || !candidate->fit(size, blocks->alignment())) candidate = candidate->next_block();
  b = blocks;
  while (b != NULL) {
    insert_field_block(candidate, b);
    b = b->next_field();
    assert(b == NULL || (candidate->offset() % b->alignment() == 0), "Contiguous blocks must be naturally well aligned");
  }
}

RawBlock* FieldLayout::insert_field_block(RawBlock* slot, RawBlock* block) {
  assert(slot->kind() == RawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
  if (slot->offset() % block->alignment() != 0) {
    int adjustment = block->alignment() - (slot->offset() % block->alignment());
    RawBlock* adj = new RawBlock(RawBlock::EMPTY, adjustment);
    insert(slot, adj);
  }
  insert(slot, block);
  if (slot->size() == 0) {
    remove(slot);
  }
  if (UseNewLayout) {
    FieldInfo::from_field_array(_fields, block->field_index())->set_offset(block->offset());
  }
  return block;
}

void FieldLayout::reconstruct_layout(const InstanceKlass* ik) {
  // TODO: it makes no sense to support static fields, static fields go to
  // the mirror, and are not impacted by static fields of the parent class
  if (ik->super() != NULL) {
    reconstruct_layout(InstanceKlass::cast(ik->super()));
  } else {
    _blocks = new RawBlock(RawBlock::RESERVED, instanceOopDesc::base_offset_in_bytes());
    _blocks->set_offset(0);
    _last = _blocks;
    _start = _blocks;
  }
  for (AllFieldStream fs(ik->fields(), ik->constants()); !fs.done(); fs.next()) {
    BasicType type = vmSymbols::signature_type(fs.signature());
    // distinction between static and non-static fields is missing
    if (fs.access_flags().is_static()) continue;
    ik->fields_annotations();
    if (type != T_VALUETYPE) {
      int size = type2aelembytes(type);
      // INHERITED blocs are marked as non-reference because oop_maps are handled by their holder class
      RawBlock* block = new RawBlock(fs.index(), RawBlock::INHERITED, size, size, false);
      block->set_offset(fs.offset());
      insert_per_offset(block);
    } else {
      fatal("Not supported yet");
    }
  }
}

void FieldLayout::fill_holes(const InstanceKlass* super_klass) {
  assert(_blocks != NULL, "Sanity check");
  assert(_blocks->offset() == 0, "first block must be at offset zero");
  RawBlock* b = _blocks;
  while (b->next_block() != NULL) {
    if (b->next_block()->offset() > (b->offset() + b->size())) {
      int size = b->next_block()->offset() - (b->offset() + b->size());
      RawBlock* empty = new RawBlock(RawBlock::EMPTY, size);
      empty->set_offset(b->offset() + b->size());
      empty->set_next_block(b->next_block());
      b->next_block()->set_prev_block(empty);
      b->set_next_block(empty);
      empty->set_prev_block(b);
    }
    b = b->next_block();
  }
  assert(b->next_block() == NULL, "Invariant at this point");
  if (b->kind() != RawBlock::EMPTY) {
    RawBlock* last = new RawBlock(RawBlock::EMPTY, INT_MAX);
    last->set_offset(b->offset() + b->size());
    assert(last->offset() > 0, "Sanity check");
    b->set_next_block(last);
    last->set_prev_block(b);
    _last = last;
  }
  // Still doing the padding to have a size that can be expressed in heapOopSize
  int super_end = instanceOopDesc::base_offset_in_bytes() + super_klass->nonstatic_field_size() * heapOopSize;
  if (_last->offset() < super_end) {
    RawBlock* padding = new RawBlock(RawBlock::PADDING, super_end - _last->offset());
    insert(_last, padding);
  }
}

RawBlock* FieldLayout::insert(RawBlock* slot, RawBlock* block) {
  assert(slot->kind() == RawBlock::EMPTY, "Blocks can only be inserted in empty blocks");
  assert(slot->offset() % block->alignment() == 0, "Incompatible alignment");
  block->set_offset(slot->offset());
  slot->set_offset(slot->offset() + block->size());
  slot->set_size(slot->size() - block->size());
  block->set_prev_block(slot->prev_block());
  block->set_next_block(slot);
  slot->set_prev_block(block);
  if (block->prev_block() != NULL) {       // suspicious test
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

void FieldLayout::insert_per_offset(RawBlock* block) {
  if (_blocks == NULL) {
    _blocks = block;
  } else if (_blocks->offset() > block->offset()) {
    block->set_next_block(_blocks);
    _blocks->set_prev_block(block);
    _blocks = block;
  } else {
    RawBlock* b = _blocks;
    while (b->next_block() != NULL && b->next_block()->offset() < block->offset()) b = b->next_block();
    if (b->next_block() == NULL) {
      b->set_next_block(block);
      block->set_prev_block(b);
    } else {
      assert(b->next_block()->offset() >= block->offset(), "Sanity check");
      assert(b->next_block()->offset() > block->offset() || b->next_block()->kind() == RawBlock::EMPTY, "Sanity check");
      block->set_next_block(b->next_block());
      b->next_block()->set_prev_block(block);
      block->set_prev_block(b);
      b->set_next_block(block);
    }
  }
}

void FieldLayout::remove(RawBlock* block) {
  assert(block != NULL, "Sanity check");
  assert(block != _last, "Sanity check");
  if (_blocks == block) {
    _blocks = block->next_block();
    if (_blocks != NULL) {
      _blocks->set_prev_block(NULL);
    }
  } else {
    assert(block->prev_block() != NULL, "_prev should be set for non-head blocks");
    block->prev_block()->set_next_block(block->next_block());
    block->next_block()->set_prev_block(block->prev_block());
  }
  if (block == _start) {
    _start = block->prev_block();
  }
}

void FieldLayout::print(outputStream* output) {
  ResourceMark rm;
  RawBlock* b = _blocks;
  while(b != _last) {
    switch(b->kind()) {
    case RawBlock::REGULAR: {
      FieldInfo* fi = FieldInfo::from_field_array(_fields, b->field_index());
      output->print_cr("  %d %s %d %d %s %s",
          b->offset(),
          "REGULAR",
          b->size(),
          b->alignment(),
          fi->signature(_cp)->as_C_string(),
          fi->name(_cp)->as_C_string());
      break;
    }
    case RawBlock::FLATTENED: {
      FieldInfo* fi = FieldInfo::from_field_array(_fields, b->field_index());
      output->print_cr("  %d %s %d %d %s %s",
          b->offset(),
          "FLATTENED",
          b->size(),
          b->alignment(),
          fi->signature(_cp)->as_C_string(),
          fi->name(_cp)->as_C_string());
      break;
    }
    case RawBlock::RESERVED:
      output->print_cr("  %d %s %d",
          b->offset(),
          "RESERVED",
          b->size());
      break;
    case RawBlock::INHERITED:
      output->print_cr("  %d %s %d",
          b->offset(),
          "INHERITED",
          b->size());
      break;
    case RawBlock::EMPTY:
      output->print_cr("  %d %s %d",
          b->offset(),
          "EMPTY",
          b->size());
      break;
    case RawBlock::PADDING:
      output->print_cr("  %d %s %d",
          b->offset(),
          "PADDING",
          b->size());
      break;
    }
    b = b->next_block();
  }
}


FieldLayoutBuilder::FieldLayoutBuilder(ClassFileParser* cfp, FieldLayoutInfo* info) {
  _cfp = cfp;
  _info = info;
  _fields = NULL;
  _root_group = NULL;
  _contended_groups = NULL;
  _static_fields = NULL;
  _layout = NULL;
  _static_layout = NULL;
  _nonstatic_oopmap_count = 0;
  // Inline class specific information
  _alignment = -1;
  _first_field_offset = -1;
  _exact_size_in_bytes = -1;
  _has_nonstatic_fields = false;
  _has_flattening_information = _cfp->is_value_type();
}

FieldGroup* FieldLayoutBuilder::get_or_create_contended_group(int g) {
  assert(g > 0, "must only be called for named contended groups");
  if (_contended_groups == NULL) {
    _contended_groups = new FieldGroup(g);
    return _contended_groups;
  }
  FieldGroup* group = _contended_groups;
  while(group->next() != NULL) {
    if (group->contended_group() == g) break;
    group = group->next();
  }
  if (group->contended_group() == g) return group;
  group->set_next(new FieldGroup(g));
  return group->next();
}

void FieldLayoutBuilder::prologue() {
  _layout = new FieldLayout(_cfp->_fields, _cfp->_cp);
  const InstanceKlass* super_klass = _cfp->_super_klass;
  _layout->initialize_instance_layout(super_klass);
  if (super_klass != NULL) {
    _has_nonstatic_fields = super_klass->has_nonstatic_fields();
  }
  _static_layout = new FieldLayout(_cfp->_fields, _cfp->_cp);
  _static_layout->initialize_static_layout();
  _static_fields = new FieldGroup();
  _root_group = new FieldGroup();
  _contended_groups = NULL;
}

/* Field sorting for regular (non-inline) classes:
 *   - fields are sorted in static and non-static fields
 *   - non-static fields are also sorted according to their contention group
 *     (support of the @Contended annotation)
 *   - @Contended annotation is ignored for static fields
 *   - field flattening decisions are taken in this method
 */
void FieldLayoutBuilder::regular_field_sorting(TRAPS) {
  assert(!_cfp->is_value_type(), "Should only be used for non-inline classes");
  for (AllFieldStream fs(_cfp->_fields, _cfp->_cp); !fs.done(); fs.next()) {
    FieldGroup* group = NULL;
    if (fs.access_flags().is_static()) {
      group = _static_fields;
    } else {
      _has_nonstatic_fields = true;
      if (fs.is_contended()) {
        int g = fs.contended_group();
        if (g == 0) {
          // default group means the field is alone in its contended group
          group = new FieldGroup(true);
          group->set_next(_contended_groups);
          _contended_groups = group;
        } else {
          group = get_or_create_contended_group(g);
        }
      } else {
        group = _root_group;
      }
    }
    assert(group != NULL, "invariant");
    BasicType type = vmSymbols::signature_type(fs.signature());
    switch(type) {
    case T_BYTE:
    case T_CHAR:
    case T_DOUBLE:
    case T_FLOAT:
    case T_INT:
    case T_LONG:
    case T_SHORT:
    case T_BOOLEAN:
      group->add_primitive_field(fs, type);
      break;
    case T_OBJECT:
    case T_ARRAY:
      if (group != _static_fields) _nonstatic_oopmap_count++;
      group->add_oop_field(fs);
      break;
    case T_VALUETYPE: {
      if (group == _static_fields) {
        // static fields are never flattened
        group->add_oop_field(fs);
      } else {
        _has_flattening_information = true;
        // Flattening decision to be taken here
        // This code assumes all verification have been performed before
        // (field is a flattenable field, field's type has been loaded
        // and it is an inline klass
        Klass* klass =
            SystemDictionary::resolve_flattenable_field_or_fail(&fs,
                Handle(THREAD, _cfp->_loader_data->class_loader()),
                _cfp->_protection_domain, true, CHECK);
        assert(klass != NULL, "Sanity check");
        ValueKlass* vk = ValueKlass::cast(klass);
        bool flattened = (ValueFieldMaxFlatSize < 0)
                         || (vk->size_helper() * HeapWordSize) <= ValueFieldMaxFlatSize;
        if (flattened) {
          group->add_flattened_field(fs, vk);
          _nonstatic_oopmap_count += vk->nonstatic_oop_map_count();
          fs.set_flattened(true);
        } else {
          _nonstatic_oopmap_count++;
          group->add_oop_field(fs);
        }
      }
      break;
    }
    default:
      fatal("Something wrong?");
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
void FieldLayoutBuilder::inline_class_field_sorting(TRAPS) {
  assert(_cfp->is_value_type(), "Should only be used for inline classes");
  int alignment = 1;
  for (AllFieldStream fs(_cfp->_fields, _cfp->_cp); !fs.done(); fs.next()) {
    FieldGroup* group = NULL;
    int field_alignment = 1;
    if (fs.access_flags().is_static()) {
      group = _static_fields;
    } else {
      _has_nonstatic_fields = true;
      group = _root_group;
    }
    assert(group != NULL, "invariant");
    BasicType type = vmSymbols::signature_type(fs.signature());
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
      group->add_primitive_field(fs, type);
      break;
    case T_OBJECT:
    case T_ARRAY:
      if (group != _static_fields) {
        _nonstatic_oopmap_count++;
        field_alignment = type2aelembytes(type); // alignment == size for oops
      }
      group->add_oop_field(fs);
      break;
    case T_VALUETYPE: {
      if (group == _static_fields) {
        // static fields are never flattened
        group->add_oop_field(fs);
      } else {
        // Flattening decision to be taken here
        // This code assumes all verifications have been performed before
        // (field is a flattenable field, field's type has been loaded
        // and it is an inline klass
        Klass* klass =
            SystemDictionary::resolve_flattenable_field_or_fail(&fs,
                Handle(THREAD, _cfp->_loader_data->class_loader()),
                _cfp->_protection_domain, true, CHECK);
        assert(klass != NULL, "Sanity check");
        ValueKlass* vk = ValueKlass::cast(klass);
        bool flattened = (ValueFieldMaxFlatSize < 0)
                         || (vk->size_helper() * HeapWordSize) <= ValueFieldMaxFlatSize;
        if (flattened) {
          group->add_flattened_field(fs, vk);
          _nonstatic_oopmap_count += vk->nonstatic_oop_map_count();
          field_alignment = vk->get_alignment();
          fs.set_flattened(true);
        } else {
          _nonstatic_oopmap_count++;
          field_alignment = type2aelembytes(T_OBJECT);
          group->add_oop_field(fs);
        }
      }
      break;
    }
    default:
      fatal("Unexpected BasicType");
    }
    if (!fs.access_flags().is_static() && field_alignment > alignment) alignment = field_alignment;
  }
  _alignment = alignment;
  if (_cfp->is_value_type() && (!_has_nonstatic_fields)) {
    // There are a number of fixes required throughout the type system and JIT
    _cfp->throwValueTypeLimitation(THREAD_AND_LOCATION, "Value Types do not support zero instance size yet");
    return;
  }
}

void FieldLayoutBuilder::insert_contended_padding(RawBlock* slot) {
  if (ContendedPaddingWidth > 0) {
    RawBlock* padding = new RawBlock(RawBlock::PADDING, ContendedPaddingWidth);
    _layout->insert(slot, padding);
  }
}

/* Computation of regular classes layout is an evolution of the previous default layout
 * (FieldAllocationStyle 1):
 *   - flattened fields are allocated first (because they have potentially the
 *     least regular shapes, and are more likely to create empty slots between them,
 *     which can then be used to allocation primitive or oop fields). Allocation is
 *     performed from the biggest to the smallest flattened field.
 *   - then primitive fields (from the biggest to the smallest)
 *   - then oop fields are allocated contiguously (to reduce the number of oopmaps
 *     and reduce the work of the GC).
 */
void FieldLayoutBuilder::compute_regular_layout(TRAPS) {
  bool need_tail_padding = false;
  prologue();
  regular_field_sorting(CHECK);
  const bool is_contended_class = _cfp->_parsed_annotations->is_contended();
  if (is_contended_class) {
    // insertion is currently easy because the current strategy doesn't try to fill holes
    // in super classes layouts => the _start block is by consequence the _last_block
    insert_contended_padding(_layout->start());
    need_tail_padding = true;
  }
  _layout->add(_root_group->flattened_fields());
  _layout->add(_root_group->primitive_fields());
  _layout->add_contiguously(_root_group->oop_fields());
  FieldGroup* cg = _contended_groups;
  while (cg != NULL) {
    RawBlock* start = _layout->last_block();
    insert_contended_padding(start);
    _layout->add(cg->flattened_fields(), start);
    _layout->add(cg->primitive_fields(), start);
    _layout->add(cg->oop_fields(), start);
    need_tail_padding = true;
    cg = cg->next();
  }
  if (need_tail_padding) {
    insert_contended_padding(_layout->last_block());
  }
  _static_layout->add_contiguously(this->_static_fields->oop_fields());
  _static_layout->add(this->_static_fields->primitive_fields());

  epilogue();
}

/* Computation of inline classes has a slightly different strategy than for
 * regular classes. Regular classes have their oop fields allocated at the end
 * of the layout to increase GC performances. Unfortunately, this strategy
 * increases the number of empty slots inside an instance. Because the purpose
 * of inline classes is to be embedded into other containers, it is critical
 * to keep their size as small as possible. For this reason, the allocation
 * strategy is:
 *   - flattened fields are allocated first (because they have potentially the
 *     least regular shapes, and are more likely to create empty slots between them,
 *     which can then be used to allocation primitive or oop fields). Allocation is
 *     performed from the biggest to the smallest flattened field.
 *   - then oop fields are allocated contiguously (to reduce the number of oopmaps
 *     and reduce the work of the GC)
 *   - then primitive fields (from the biggest to the smallest)
 */
void FieldLayoutBuilder::compute_inline_class_layout(TRAPS) {
  prologue();
  inline_class_field_sorting(CHECK);
  if (_layout->start()->offset() % _alignment != 0) {
    RawBlock* padding = new RawBlock(RawBlock::PADDING, _alignment - (_layout->start()->offset() % _alignment));
    _layout->insert(_layout->start(), padding);
    _layout->set_start(padding->next_block());
  }
  _first_field_offset = _layout->start()->offset();
  _layout->add(_root_group->flattened_fields());
  _layout->add_contiguously(_root_group->oop_fields());
  _layout->add(_root_group->primitive_fields());
  _exact_size_in_bytes = _layout->last_block()->offset() - _layout->start()->offset();

  _static_layout->add_contiguously(this->_static_fields->oop_fields());
  _static_layout->add(this->_static_fields->primitive_fields());

  epilogue();
}

void FieldLayoutBuilder::add_flattened_field_oopmap(OopMapBlocksBuilder* nonstatic_oop_maps,
						    ValueKlass* vklass, int offset) {
  int diff = offset - vklass->first_field_offset();
  const OopMapBlock* map = vklass->start_of_nonstatic_oop_maps();
  const OopMapBlock* last_map = map + vklass->nonstatic_oop_map_count();
  while (map < last_map) {
    nonstatic_oop_maps->add(map->offset() + diff, map->count());
    map++;
  }
}
  

void FieldLayoutBuilder::epilogue() {
  // Computing oopmaps
  int super_oop_map_count = (_cfp->_super_klass == NULL) ? 0 :_cfp->_super_klass->nonstatic_oop_map_count();
  int max_oop_map_count = super_oop_map_count + _nonstatic_oopmap_count;

  OopMapBlocksBuilder* nonstatic_oop_maps =
      new OopMapBlocksBuilder(max_oop_map_count, Thread::current());
  if (super_oop_map_count > 0) {
    nonstatic_oop_maps->initialize_inherited_blocks(_cfp->_super_klass->start_of_nonstatic_oop_maps(),
        _cfp->_super_klass->nonstatic_oop_map_count());
  }
  if (_root_group->oop_fields() != NULL) {
    nonstatic_oop_maps->add(_root_group->oop_fields()->offset(), _root_group->oop_count());
  }
  RawBlock* ff = _root_group->flattened_fields();
  while (ff != NULL) {
    ValueKlass* vklass = ff->value_klass();
    assert(vklass != NULL, "Should have been initialized");
    if (vklass->contains_oops()) {
      add_flattened_field_oopmap(nonstatic_oop_maps, vklass, ff->offset());
    }
    ff = ff->next_field();
  }
  FieldGroup* cg = _contended_groups;
  while (cg != NULL) {
    if (cg->oop_count() > 0) {
      nonstatic_oop_maps->add(cg->oop_fields()->offset(), cg->oop_count());
    }
    RawBlock* ff = cg->flattened_fields();
    while (ff != NULL) {
      ValueKlass* vklass = ff->value_klass();
      assert(vklass != NULL, "Should have been initialized");
      if (vklass->contains_oops()) {
	add_flattened_field_oopmap(nonstatic_oop_maps, vklass, ff->offset());
      }
      ff = ff->next_field();
    }
    cg = cg->next();
  }

  // nonstatic_oop_maps->compact(Thread::current());

  int instance_end = align_up(_layout->last_block()->offset(), wordSize);
  int static_fields_end = align_up(_static_layout->last_block()->offset(), wordSize);
  int static_fields_size = (static_fields_end -
      InstanceMirrorKlass::offset_of_static_fields()) / wordSize;
  int nonstatic_field_end = align_up(_layout->last_block()->offset(), heapOopSize);

  // Pass back information needed for InstanceKlass creation

  _info->oop_map_blocks = nonstatic_oop_maps;
  _info->instance_size = align_object_size(instance_end / wordSize);
  _info->static_field_size = static_fields_size;
  _info->nonstatic_field_size = (nonstatic_field_end - instanceOopDesc::base_offset_in_bytes()) / heapOopSize;
  _info->has_nonstatic_fields = _has_nonstatic_fields;

  if (PrintNewLayout || (PrintFlattenableLayouts && _has_flattening_information)) {
    ResourceMark rm;
    tty->print_cr("Layout of class %s", _cfp->_class_name->as_C_string());
    tty->print_cr("| offset | kind | size | alignment | signature | name |");
    tty->print_cr("Instance fields:");
    _layout->print(tty);
    tty->print_cr("Static fields");
    _static_layout->print(tty);
    nonstatic_oop_maps->print_on(tty);
    tty->print_cr("Instance size = %d * heapWordSize", _info->instance_size);
    tty->print_cr("Non-static field size = %d * heapWordSize", _info->nonstatic_field_size);
    tty->print_cr("Static field size = %d * heapWordSize", _info->static_field_size);
    if (_cfp->is_value_type()) {
      tty->print_cr("alignment = %d", _alignment);
      tty->print_cr("exact_size_in_bytes = %d", _exact_size_in_bytes);
      tty->print_cr("first_field_offset = %d", _first_field_offset);
    }
    tty->print_cr("---");
  }
}
