/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_FIELDSTREAMS_HPP
#define SHARE_OOPS_FIELDSTREAMS_HPP

#include "oops/instanceKlass.hpp"
#include "oops/fieldInfo.hpp"
#include "runtime/fieldDescriptor.hpp"

// The is the base class for iteration over the fields array
// describing the declared fields in the class.  Several subclasses
// are provided depending on the kind of iteration required.  The
// JavaFieldStream is for iterating over regular Java fields and it
// generally the preferred iterator.  InternalFieldStream only
// iterates over fields that have been injected by the JVM.
// AllFieldStream exposes all fields and should only be used in rare
// cases.
// HierarchicalFieldStream allows to also iterate over fields of supertypes.
class FieldStreamBase : public StackObj {

 protected:
  const Array<u1>*    _fieldinfo_stream;
  FieldInfoReader     _reader;
  constantPoolHandle  _constants;
  int                 _index;
  int                 _limit;

  FieldInfo           _fi_buf;
  fieldDescriptor     _fd_buf;

  FieldInfo const * field() const {
    assert(!done(), "no more fields");
    return &_fi_buf;
  }

  inline FieldStreamBase(const Array<u1>* fieldinfo_stream, ConstantPool* constants, int start, int limit);

  inline FieldStreamBase(Array<u1>* fieldinfo_stream, ConstantPool* constants);

  private:
   void initialize() {
    int java_fields_count = _reader.next_uint();
    int injected_fields_count = _reader.next_uint();
    assert( _limit <= java_fields_count + injected_fields_count, "Safety check");
    if (_limit != 0) {
      _reader.read_field_info(_fi_buf);
    }
   }
 public:
  inline FieldStreamBase(InstanceKlass* klass);

  // accessors
  int index() const                 { return _index; }
  InstanceKlass* field_holder() const { return _constants->pool_holder(); }

  void next() {
    _index += 1;
    if (done()) return;
    _reader.read_field_info(_fi_buf);
  }
  bool done() const { return _index >= _limit; }

  // Accessors for current field
  AccessFlags access_flags() const {
    return field()->access_flags();
  }

  FieldInfo::FieldFlags field_flags() const {
    return field()->field_flags();
  }

  Symbol* name() const {
    return field()->name(_constants());
  }

  Symbol* signature() const {
    return field()->signature(_constants());
  }

  Symbol* generic_signature() const {
    if (field()->field_flags().is_generic()) {
      return _constants->symbol_at(field()->generic_signature_index());
    } else {
      return nullptr;
    }
  }

  int offset() const {
    return field()->offset();
  }

  bool is_null_free_inline_type() {
    return field()->field_flags().is_null_free_inline_type();
  }

  bool is_flat() const {
    return field()->field_flags().is_flat();
  }

  bool is_contended() const {
    return field()->is_contended();
  }

  int contended_group() const {
    return field()->contended_group();
  }

  int null_marker_offset() const {
    return field()->null_marker_offset();
  }

  // Convenient methods

  const FieldInfo& to_FieldInfo() const {
    return _fi_buf;
  }

  int num_total_fields() const {
    return FieldInfoStream::num_total_fields(_fieldinfo_stream);
  }

  // bridge to a heavier API:
  fieldDescriptor& field_descriptor() const {
    fieldDescriptor& field = const_cast<fieldDescriptor&>(_fd_buf);
    field.reinitialize(field_holder(), to_FieldInfo());
    return field;
  }
};

// Iterate over only the Java fields
class JavaFieldStream : public FieldStreamBase {
 public:
  JavaFieldStream(const InstanceKlass* k): FieldStreamBase(k->fieldinfo_stream(), k->constants(), 0, k->java_fields_count()) {}

  u2 name_index() const {
    assert(!field()->field_flags().is_injected(), "regular only");
    return field()->name_index();
  }

  u2 signature_index() const {
    assert(!field()->field_flags().is_injected(), "regular only");
    return field()->signature_index();
    return -1;
  }

  u2 generic_signature_index() const {
    assert(!field()->field_flags().is_injected(), "regular only");
    if (field()->field_flags().is_generic()) {
      return field()->generic_signature_index();
    }
    return 0;
  }

  u2 initval_index() const {
    assert(!field()->field_flags().is_injected(), "regular only");
    return field()->initializer_index();
  }
};


// Iterate over only the internal fields
class InternalFieldStream : public FieldStreamBase {
 public:
  InternalFieldStream(InstanceKlass* k):      FieldStreamBase(k->fieldinfo_stream(), k->constants(), k->java_fields_count(), 0) {}
};


class AllFieldStream : public FieldStreamBase {
 public:
  AllFieldStream(Array<u1>* fieldinfo, ConstantPool* constants): FieldStreamBase(fieldinfo, constants) {}
  AllFieldStream(const InstanceKlass* k):      FieldStreamBase(k->fieldinfo_stream(), k->constants()) {}
};

/* Very generally, a base class for a stream adapter, a derived class just implements
 * current_stream that returns a FieldStreamType, and this adapter takes care of providing
 * the methods of FieldStreamBase.
 *
 * In practice, this is used to provide a stream over the fields of a class and its superclasses
 * and interfaces. The derived class of HierarchicalFieldStreamBase decides in which order we iterate
 * on the superclasses (and interfaces), and the template parameter FieldStreamType is the underlying
 * stream we use to iterate over the fields each class. Methods such as done and next are still up to
 * the derived classes, allowing them to iterate over the class hierarchy, but also skip elements that
 * the underlying FieldStreamType would otherwise include.
 */
template<typename FieldStreamType>
class HierarchicalFieldStreamBase : public StackObj {
  virtual FieldStreamType& current_stream() = 0;
  virtual const FieldStreamType& current_stream() const = 0;

public:
  // bridge functions from FieldStreamBase
  int index() const {
    return current_stream().index();
  }

  AccessFlags access_flags() const {
    return current_stream().access_flags();
  }

  FieldInfo::FieldFlags field_flags() const {
    return current_stream().field_flags();
  }

  Symbol* name() const {
    return current_stream().name();
  }

  Symbol* signature() const {
    return current_stream().signature();
  }

  Symbol* generic_signature() const {
    return current_stream().generic_signature();
  }

  int offset() const {
    return current_stream().offset();
  }

  bool is_contended() const {
    return current_stream().is_contended();
  }

  int contended_group() const {
    return current_stream().contended_group();
  }

  FieldInfo to_FieldInfo() {
    return current_stream().to_FieldInfo();
  }

  fieldDescriptor& field_descriptor() const {
    return current_stream().field_descriptor();
  }

  bool is_flat() const {
    return current_stream().is_flat();
  }

  bool is_null_free_inline_type() {
    return current_stream().is_null_free_inline_type();
  }

  int null_marker_offset() {
    return current_stream().null_marker_offset();
  }
};

/* Iterate over fields including the ones declared in supertypes.
 * Derived classes are traversed before base classes, and interfaces
 * at the end.
 */
template<typename FieldStreamType>
class HierarchicalFieldStream final : public HierarchicalFieldStreamBase<FieldStreamType>  {
 private:
  const Array<InstanceKlass*>* _interfaces;
  InstanceKlass* _next_klass; // null indicates no more type to visit
  FieldStreamType _current_stream;
  int _interface_index;

  void prepare() {
    _next_klass = next_klass_with_fields();
    // special case: the initial klass has no fields. If any supertype has any fields, use that directly.
    // if no such supertype exists, done() will return false already.
    next_stream_if_done();
  }

  InstanceKlass* next_klass_with_fields() {
    assert(_next_klass != nullptr, "reached end of types already");
    InstanceKlass* result = _next_klass;
    do  {
      if (!result->is_interface() && result->super() != nullptr) {
        result = result->java_super();
      } else if (_interface_index > 0) {
        result = _interfaces->at(--_interface_index);
      } else {
        return nullptr; // we did not find any more supertypes with fields
      }
    } while (FieldStreamType(result).done());
    return result;
  }

  // sets _current_stream to the next if the current is done and any more is available
  void next_stream_if_done() {
    if (_next_klass != nullptr && _current_stream.done()) {
      _current_stream = FieldStreamType(_next_klass);
      assert(!_current_stream.done(), "created empty stream");
      _next_klass = next_klass_with_fields();
    }
  }

  FieldStreamType& current_stream() override { return _current_stream; }
  const FieldStreamType& current_stream() const override { return _current_stream; }

 public:
  explicit HierarchicalFieldStream(InstanceKlass* klass) :
    _interfaces(klass->transitive_interfaces()),
    _next_klass(klass),
    _current_stream(FieldStreamType(klass)),
    _interface_index(_interfaces->length()) {
      prepare();
  }

  void next() {
    _current_stream.next();
    next_stream_if_done();
  }

  bool done() const { return _next_klass == nullptr && _current_stream.done(); }
};

/* Iterates on the fields of a class and its super-class top-down (java.lang.Object first)
 * Doesn't traverse interfaces for now, because it's not clear which order would make sense
 * Let's decide when or if the need arises. Since we are not traversing interfaces, we
 * wouldn't get all the static fields, and since the current use-case of this stream does not
 * care about static fields, we restrict it to regular non-static fields.
 */
class TopDownHierarchicalNonStaticFieldStreamBase final : public HierarchicalFieldStreamBase<JavaFieldStream> {
  GrowableArray<InstanceKlass*>* _super_types;  // Self and super type, bottom up
  int _current_stream_index;
  JavaFieldStream _current_stream;

  void next_stream_if_needed() {
    precond(_current_stream_index >= 0);
    while (_current_stream.done()) {
      _current_stream_index--;
      if (_current_stream_index < 0) {
        return;
      }
      _current_stream = JavaFieldStream(_super_types->at(_current_stream_index));
    }
  }

  GrowableArray<InstanceKlass*>* get_super_types(InstanceKlass* klass) {
    auto super_types = new GrowableArray<InstanceKlass*>();
    do {
      super_types->push(klass);
    } while ((klass = klass->java_super()) != nullptr);
    return super_types;
  }

  void raw_next() {
    _current_stream.next();
    next_stream_if_needed();
  }

  void closest_non_static() {
    while (!done() && access_flags().is_static()) {
      raw_next();
    }
  }

  JavaFieldStream& current_stream() override { return _current_stream; }
  const JavaFieldStream& current_stream() const override { return _current_stream; }

 public:
  explicit TopDownHierarchicalNonStaticFieldStreamBase(InstanceKlass* klass) :
    _super_types(get_super_types(klass)),
    _current_stream_index(_super_types->length() - 1),
    _current_stream(JavaFieldStream(_super_types->at(_current_stream_index))) {
    next_stream_if_needed();
    closest_non_static();
  }

  void next() {
    raw_next();
    closest_non_static();
  }

  bool done() const { return _current_stream_index < 0; }
};

#endif // SHARE_OOPS_FIELDSTREAMS_HPP
