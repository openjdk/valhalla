/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/stackMapTable.hpp"
#include "classfile/verifier.hpp"
#include "memory/resourceArea.hpp"
#include "oops/constantPool.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"

StackMapTable::StackMapTable(StackMapReader* reader, StackMapFrame* init_frame,
                             u2 max_locals, u2 max_stack,
                             char* code_data, int code_len, TRAPS) {
  _code_length = code_len;
  _frame_count = reader->get_frame_count();
  int32_t assert_unset_field_entries = 0;
  if (_frame_count > 0) {
    StackMapFrame** tmp_frame_array = NEW_RESOURCE_ARRAY_IN_THREAD(THREAD,
                                                StackMapFrame*, _frame_count);
    StackMapFrame* pre_frame = init_frame;
    bool first = true;
    for (int32_t i = 0; i < _frame_count; i++) {
      StackMapFrame* frame = reader->next(
        pre_frame, first, max_locals, max_stack, &assert_unset_field_entries,
        CHECK_VERIFY(pre_frame->verifier()));
      tmp_frame_array[i] = frame;
      if (frame != nullptr) {
        first = false;
        int offset = frame->offset();
        if (offset >= code_len || code_data[offset] == 0) {
          frame->verifier()->verify_error(
              ErrorContext::bad_stackmap(i, frame),
              "StackMapTable error: bad offset");
          return;
        }
        pre_frame = frame;
      }
    }
    // Remake frame array with correct size
    if (assert_unset_field_entries == 0) {
      _frame_array = tmp_frame_array;
    } else {
      int32_t new_size = _frame_count - assert_unset_field_entries;
      _frame_array = NEW_RESOURCE_ARRAY_IN_THREAD(THREAD,
                                                  StackMapFrame*, new_size);
      int new_index = 0;
      for (int32_t i = 0; i < _frame_count; i++) {
        if (tmp_frame_array[i] != nullptr) {
          _frame_array[new_index] = tmp_frame_array[i];
          new_index++;
        }
      }
      FREE_RESOURCE_ARRAY(StackMapFrame*, tmp_frame_array, _frame_count);
      _frame_count = new_size;
    }
  }
  reader->check_end(CHECK);
}

// This method is only called by method in StackMapTable.
int StackMapTable::get_index_from_offset(int32_t offset) const {
  int i = 0;
  for (; i < _frame_count; i++) {
    if (_frame_array[i]->offset() == offset) {
      return i;
    }
  }
  return i;  // frame with offset doesn't exist in the array
}

bool StackMapTable::match_stackmap(
    StackMapFrame* frame, int32_t target,
    bool match, bool update, ErrorContext* ctx, TRAPS) const {
  int index = get_index_from_offset(target);
  return match_stackmap(frame, target, index, match, update, ctx, THREAD);
}

// Match and/or update current_frame to the frame in stackmap table with
// specified offset and frame index. Return true if the two frames match.
//
// The values of match and update are:                  _match__update
//
// checking a branch target:                             true   false
// checking an exception handler:                        true   false
// linear bytecode verification following an
// unconditional branch:                                 false  true
// linear bytecode verification not following an
// unconditional branch:                                 true   true
bool StackMapTable::match_stackmap(
    StackMapFrame* frame, int32_t target, int32_t frame_index,
    bool match, bool update, ErrorContext* ctx, TRAPS) const {
  if (frame_index < 0 || frame_index >= _frame_count) {
    *ctx = ErrorContext::missing_stackmap(frame->offset());
    frame->verifier()->verify_error(
        *ctx, "Expecting a stackmap frame at branch target %d", target);
    return false;
  }

  StackMapFrame *stackmap_frame = _frame_array[frame_index];
  bool result = true;
  if (match) {
    // Has direct control flow from last instruction, need to match the two
    // frames.
    result = frame->is_assignable_to(stackmap_frame,
        ctx, CHECK_VERIFY_(frame->verifier(), result));
  }
  if (update) {
    // Use the frame in stackmap table as current frame
    int lsize = stackmap_frame->locals_size();
    int ssize = stackmap_frame->stack_size();
    if (frame->locals_size() > lsize || frame->stack_size() > ssize) {
      // Make sure unused type array items are all _bogus_type.
      frame->reset();
    }
    frame->set_locals_size(lsize);
    frame->copy_locals(stackmap_frame);
    frame->set_stack_size(ssize);
    frame->copy_stack(stackmap_frame);
    frame->set_flags(stackmap_frame->flags());
    frame->set_assert_unset_fields(stackmap_frame->assert_unset_fields());
  }
  return result;
}

void StackMapTable::check_jump_target(
    StackMapFrame* frame, int32_t target, TRAPS) const {
  ErrorContext ctx;
  bool match = match_stackmap(
    frame, target, true, false, &ctx, CHECK_VERIFY(frame->verifier()));
  if (!match || (target < 0 || target >= _code_length)) {
    frame->verifier()->verify_error(ctx,
        "Inconsistent stackmap frames at branch target %d", target);
  }
}

void StackMapTable::print_on(outputStream* str) const {
  str->indent().print_cr("StackMapTable: frame_count = %d", _frame_count);
  str->indent().print_cr("table = { ");
  {
    streamIndentor si(str);
    for (int32_t i = 0; i < _frame_count; ++i) {
      _frame_array[i]->print_on(str);
    }
  }
  str->print_cr(" }");
}

StackMapReader::StackMapReader(ClassVerifier* v, StackMapStream* stream, char* code_data,
                               int32_t code_len, StackMapFrame::AssertUnsetFieldTable* initial_strict_fields, TRAPS) :
                               _verifier(v), _stream(stream),
                               _code_data(code_data), _code_length(code_len),
                               _assert_unset_fields_buffer(initial_strict_fields) {
  methodHandle m = v->method();
  if (m->has_stackmap_table()) {
    _cp = constantPoolHandle(THREAD, m->constants());
    _frame_count = _stream->get_u2(CHECK);
  } else {
    // There's no stackmap table present. Frame count and size are 0.
    _frame_count = 0;
  }
}

int32_t StackMapReader::chop(
    VerificationType* locals, int32_t length, int32_t chops) {
  if (locals == nullptr) return -1;
  int32_t pos = length - 1;
  for (int32_t i=0; i<chops; i++) {
    if (locals[pos].is_category2_2nd()) {
      pos -= 2;
    } else {
      pos --;
    }
    if (pos<0 && i<(chops-1)) return -1;
  }
  return pos+1;
}

#define CHECK_NT CHECK_(VerificationType::bogus_type())

VerificationType StackMapReader::parse_verification_type(u1* flags, TRAPS) {
  u1 tag = _stream->get_u1(CHECK_NT);
  if (tag < (u1)ITEM_UninitializedThis) {
    return VerificationType::from_tag(tag);
  }
  if (tag == ITEM_Object) {
    u2 class_index = _stream->get_u2(CHECK_NT);
    int nconstants = _cp->length();
    if ((class_index <= 0 || class_index >= nconstants) ||
        (!_cp->tag_at(class_index).is_klass() &&
         !_cp->tag_at(class_index).is_unresolved_klass())) {
      _stream->stackmap_format_error("bad class index", THREAD);
      return VerificationType::bogus_type();
    }
    Symbol* klass_name = _cp->klass_name_at(class_index);
    return VerificationType::reference_type(klass_name);
  }
  if (tag == ITEM_UninitializedThis) {
    if (flags != nullptr) {
      *flags |= FLAG_THIS_UNINIT;
    }
    return VerificationType::uninitialized_this_type();
  }
  if (tag == ITEM_Uninitialized) {
    u2 offset = _stream->get_u2(CHECK_NT);
    if (offset >= _code_length ||
        _code_data[offset] != ClassVerifier::NEW_OFFSET) {
      _verifier->class_format_error(
        "StackMapTable format error: bad offset for Uninitialized");
      return VerificationType::bogus_type();
    }
    return VerificationType::uninitialized_type(offset);
  }
  _stream->stackmap_format_error("bad verification type", THREAD);
  return VerificationType::bogus_type();
}

StackMapFrame* StackMapReader::next(
    StackMapFrame* pre_frame, bool first, u2 max_locals, u2 max_stack, int32_t* unset_field_entries, TRAPS) {
  StackMapFrame* frame;
  int offset;
  VerificationType* locals = nullptr;
  u1 frame_type = _stream->get_u1(CHECK_NULL);
  if (frame_type == 246) {
    // assert unset fields
    (*unset_field_entries)++;
    u2 num_unset_fields = _stream->get_u2(CHECK_NULL);
    StackMapFrame::AssertUnsetFieldTable* new_fields = new StackMapFrame::AssertUnsetFieldTable();

    for (u2 i = 0; i < num_unset_fields; i++) {
      u2 index = _stream->get_u2(CHECK_NULL);
      Symbol* name = _cp->symbol_at(_cp->name_ref_index_at(index));
      Symbol* sig = _cp->symbol_at(_cp->signature_ref_index_at(index));
      NameAndSig tmp(name, sig);

      if (!pre_frame->assert_unset_fields()->contains(tmp)) {
        log_info(verification)("Field %s%s is not found among initial strict instance fields", name->as_C_string(), sig->as_C_string());
        StackMapFrame::print_strict_fields(pre_frame->assert_unset_fields());
        pre_frame->verifier()->verify_error(
            ErrorContext::bad_strict_fields(pre_frame->offset(), pre_frame),
            "Strict fields not a subset of initial strict instance fields");
      } else {
        new_fields->put(tmp, tmp);
      }
    }

    // Only modify strict instance fields the frame has uninitialized this
    if (pre_frame->flag_this_uninit()) {
      _assert_unset_fields_buffer = pre_frame->merge_unset_fields(new_fields);
    }

    return nullptr;
  }
  if (frame_type < 64) {
    // same_frame
    if (first) {
      offset = frame_type;
      // Can't share the locals array since that is updated by the verifier.
      if (pre_frame->locals_size() > 0) {
        locals = NEW_RESOURCE_ARRAY_IN_THREAD(
          THREAD, VerificationType, pre_frame->locals_size());
      }
    } else {
      offset = pre_frame->offset() + frame_type + 1;
      locals = pre_frame->locals();
    }
    frame = new StackMapFrame(
      offset, pre_frame->flags(), pre_frame->locals_size(), 0,
      max_locals, max_stack, locals, nullptr,
      _assert_unset_fields_buffer, _verifier);
    if (first && locals != nullptr) {
      frame->copy_locals(pre_frame);
    }
    return frame;
  }
  if (frame_type < 128) {
    // same_locals_1_stack_item_frame
    if (first) {
      offset = frame_type - 64;
      // Can't share the locals array since that is updated by the verifier.
      if (pre_frame->locals_size() > 0) {
        locals = NEW_RESOURCE_ARRAY_IN_THREAD(
          THREAD, VerificationType, pre_frame->locals_size());
      }
    } else {
      offset = pre_frame->offset() + frame_type - 63;
      locals = pre_frame->locals();
    }
    VerificationType* stack = NEW_RESOURCE_ARRAY_IN_THREAD(
      THREAD, VerificationType, 2);
    u2 stack_size = 1;
    stack[0] = parse_verification_type(nullptr, CHECK_VERIFY_(_verifier, nullptr));
    if (stack[0].is_category2()) {
      stack[1] = stack[0].to_category2_2nd();
      stack_size = 2;
    }
    check_verification_type_array_size(
      stack_size, max_stack, CHECK_VERIFY_(_verifier, nullptr));
    frame = new StackMapFrame(
      offset, pre_frame->flags(), pre_frame->locals_size(), stack_size,
      max_locals, max_stack, locals, stack,
      _assert_unset_fields_buffer, _verifier);
    if (first && locals != nullptr) {
      frame->copy_locals(pre_frame);
    }
    return frame;
  }

  u2 offset_delta = _stream->get_u2(CHECK_NULL);

  if (frame_type < ASSERT_UNSET_FIELDS) {
    // reserved frame types
    _stream->stackmap_format_error(
      "reserved frame type", CHECK_VERIFY_(_verifier, nullptr));
  }

  if (frame_type == SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
    // same_locals_1_stack_item_frame_extended
    if (first) {
      offset = offset_delta;
      // Can't share the locals array since that is updated by the verifier.
      if (pre_frame->locals_size() > 0) {
        locals = NEW_RESOURCE_ARRAY_IN_THREAD(
          THREAD, VerificationType, pre_frame->locals_size());
      }
    } else {
      offset = pre_frame->offset() + offset_delta + 1;
      locals = pre_frame->locals();
    }
    VerificationType* stack = NEW_RESOURCE_ARRAY_IN_THREAD(
      THREAD, VerificationType, 2);
    u2 stack_size = 1;
    stack[0] = parse_verification_type(nullptr, CHECK_VERIFY_(_verifier, nullptr));
    if (stack[0].is_category2()) {
      stack[1] = stack[0].to_category2_2nd();
      stack_size = 2;
    }
    check_verification_type_array_size(
      stack_size, max_stack, CHECK_VERIFY_(_verifier, nullptr));
    frame = new StackMapFrame(
      offset, pre_frame->flags(), pre_frame->locals_size(), stack_size,
      max_locals, max_stack, locals, stack,
      _assert_unset_fields_buffer, _verifier);
    if (first && locals != nullptr) {
      frame->copy_locals(pre_frame);
    }
    return frame;
  }

  if (frame_type <= SAME_EXTENDED) {
    // chop_frame or same_frame_extended
    locals = pre_frame->locals();
    int length = pre_frame->locals_size();
    int chops = SAME_EXTENDED - frame_type;
    int new_length = length;
    u1 flags = pre_frame->flags();
    if (chops != 0) {
      new_length = chop(locals, length, chops);
      check_verification_type_array_size(
        new_length, max_locals, CHECK_VERIFY_(_verifier, nullptr));
      // Recompute flags since uninitializedThis could have been chopped.
      flags = 0;
      for (int i=0; i<new_length; i++) {
        if (locals[i].is_uninitialized_this()) {
          flags |= FLAG_THIS_UNINIT;
          break;
        }
      }
    }
    if (first) {
      offset = offset_delta;
      // Can't share the locals array since that is updated by the verifier.
      if (new_length > 0) {
        locals = NEW_RESOURCE_ARRAY_IN_THREAD(
          THREAD, VerificationType, new_length);
      } else {
        locals = nullptr;
      }
    } else {
      offset = pre_frame->offset() + offset_delta + 1;
    }
    frame = new StackMapFrame(
      offset, flags, new_length, 0, max_locals, max_stack,
      locals, nullptr,
      _assert_unset_fields_buffer, _verifier);
    if (first && locals != nullptr) {
      frame->copy_locals(pre_frame);
    }
    return frame;
  } else if (frame_type < SAME_EXTENDED + 4) {
    // append_frame
    int appends = frame_type - SAME_EXTENDED;
    int real_length = pre_frame->locals_size();
    int new_length = real_length + appends*2;
    locals = NEW_RESOURCE_ARRAY_IN_THREAD(THREAD, VerificationType, new_length);
    VerificationType* pre_locals = pre_frame->locals();
    int i;
    for (i=0; i<pre_frame->locals_size(); i++) {
      locals[i] = pre_locals[i];
    }
    u1 flags = pre_frame->flags();
    for (i=0; i<appends; i++) {
      locals[real_length] = parse_verification_type(&flags, CHECK_NULL);
      if (locals[real_length].is_category2()) {
        locals[real_length + 1] = locals[real_length].to_category2_2nd();
        ++real_length;
      }
      ++real_length;
    }
    check_verification_type_array_size(
      real_length, max_locals, CHECK_VERIFY_(_verifier, nullptr));
    if (first) {
      offset = offset_delta;
    } else {
      offset = pre_frame->offset() + offset_delta + 1;
    }
    frame = new StackMapFrame(
      offset, flags, real_length, 0, max_locals,
      max_stack, locals, nullptr,
      _assert_unset_fields_buffer, _verifier);
    return frame;
  }
  if (frame_type == FULL) {
    // full_frame
    u1 flags = 0;
    u2 locals_size = _stream->get_u2(CHECK_NULL);
    int real_locals_size = 0;
    if (locals_size > 0) {
      locals = NEW_RESOURCE_ARRAY_IN_THREAD(
        THREAD, VerificationType, locals_size*2);
    }
    int i;
    for (i=0; i<locals_size; i++) {
      locals[real_locals_size] = parse_verification_type(&flags, CHECK_NULL);
      if (locals[real_locals_size].is_category2()) {
        locals[real_locals_size + 1] =
          locals[real_locals_size].to_category2_2nd();
        ++real_locals_size;
      }
      ++real_locals_size;
    }
    check_verification_type_array_size(
      real_locals_size, max_locals, CHECK_VERIFY_(_verifier, nullptr));
    u2 stack_size = _stream->get_u2(CHECK_NULL);
    int real_stack_size = 0;
    VerificationType* stack = nullptr;
    if (stack_size > 0) {
      stack = NEW_RESOURCE_ARRAY_IN_THREAD(
        THREAD, VerificationType, stack_size*2);
    }
    for (i=0; i<stack_size; i++) {
      stack[real_stack_size] = parse_verification_type(nullptr, CHECK_NULL);
      if (stack[real_stack_size].is_category2()) {
        stack[real_stack_size + 1] = stack[real_stack_size].to_category2_2nd();
        ++real_stack_size;
      }
      ++real_stack_size;
    }
    check_verification_type_array_size(
      real_stack_size, max_stack, CHECK_VERIFY_(_verifier, nullptr));
    if (first) {
      offset = offset_delta;
    } else {
      offset = pre_frame->offset() + offset_delta + 1;
    }
    frame = new StackMapFrame(
      offset, flags, real_locals_size, real_stack_size,
      max_locals, max_stack, locals, stack,
      _assert_unset_fields_buffer, _verifier);
    return frame;
  }

  _stream->stackmap_format_error(
    "reserved frame type", CHECK_VERIFY_(pre_frame->verifier(), nullptr));
  return nullptr;
}
