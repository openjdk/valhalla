/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "memory/allocation.hpp"
#include "memory/universe.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/weakHandle.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiTagMapTable.hpp"


JvmtiTagMapKey::JvmtiTagMapKey(oop obj) : _obj(obj) {}

JvmtiTagMapKey::JvmtiTagMapKey(const JvmtiTagMapKey& src) : _h() {
  // move object into Handle when copying into the table
  if (src._obj != nullptr) {
    _is_weak = !src._obj->klass()->is_inline_klass();

    // obj was read with AS_NO_KEEPALIVE, or equivalent, like during
    // a heap walk.  The object needs to be kept alive when it is published.
    Universe::heap()->keep_alive(src._obj);

    if (_is_weak) {
      _wh = WeakHandle(JvmtiExport::weak_tag_storage(), src._obj);
    } else {
      _h = OopHandle(JvmtiExport::jvmti_oop_storage(), src._obj);
    }
  } else {
    // resizing needs to create a copy.
    if (_is_weak) {
      _wh = src._wh;
    } else {
      _h = src._h;
    }
  }
  // obj is always null after a copy.
  _obj = nullptr;
}

void JvmtiTagMapKey::release_handle() {
  if (_is_weak) {
    _wh.release(JvmtiExport::weak_tag_storage());
  } else {
    _h.release(JvmtiExport::jvmti_oop_storage());
  }
}

oop JvmtiTagMapKey::object() const {
  assert(_obj == nullptr, "Must have a handle and not object");
  return _is_weak ? _wh.resolve() : _h.resolve();
}

oop JvmtiTagMapKey::object_no_keepalive() const {
  assert(_obj == nullptr, "Must have a handle and not object");
  return _is_weak ? _wh.peek() : _h.peek();
}

unsigned JvmtiTagMapKey::get_hash(const JvmtiTagMapKey& entry) {
  oop obj = entry._obj;
  assert(obj != nullptr, "must lookup obj to hash");
  if (obj->is_inline_type()) {
    // For inline types, use the klass as a hash code and let the equals match the obj.
    // It might have a long bucket but TBD to improve this if a customer situation arises.
    return (unsigned)((int64_t)obj->klass() >> 3);
  } else {
    return (unsigned)obj->identity_hash();
  }
}

static bool equal_oops(oop obj1, oop obj2); // forward declaration

static bool equal_fields(char type, oop obj1, oop obj2, int offset) {
  switch (type) {
  case JVM_SIGNATURE_BOOLEAN:
    return obj1->bool_field(offset) == obj2->bool_field(offset);
  case JVM_SIGNATURE_CHAR:
    return obj1->char_field(offset) == obj2->char_field(offset);
  case JVM_SIGNATURE_FLOAT:
    return obj1->float_field(offset) == obj2->float_field(offset);
  case JVM_SIGNATURE_DOUBLE:
    return obj1->double_field(offset) == obj2->double_field(offset);
  case JVM_SIGNATURE_BYTE:
    return obj1->byte_field(offset) == obj2->byte_field(offset);
  case JVM_SIGNATURE_SHORT:
    return obj1->short_field(offset) == obj2->short_field(offset);
  case JVM_SIGNATURE_INT:
    return obj1->int_field(offset) == obj2->int_field(offset);
  case JVM_SIGNATURE_LONG:
    return obj1->long_field(offset) == obj2->long_field(offset);
  case JVM_SIGNATURE_CLASS:
  case JVM_SIGNATURE_ARRAY:
    return equal_oops(obj1->obj_field(offset), obj2->obj_field(offset));
  }
  ShouldNotReachHere();
}

// For heap-allocated objects offset is 0 and 'klass' is obj1->klass() (== obj2->klass()).
// For flattened objects offset is the offset in the holder object, 'klass' is inlined object class.
static bool equal_value_objects(oop obj1, oop obj2, InlineKlass* klass, int offset) {
  for (JavaFieldStream fld(klass); !fld.done(); fld.next()) {
    // ignore static fields
    if (fld.access_flags().is_static()) {
      continue;
    }
    int field_offset = offset + fld.offset() - (offset > 0 ? klass->payload_offset() : 0);
    if (fld.is_flat()) { // flat value field
      InstanceKlass* holder_klass = fld.field_holder();
      InlineKlass* field_klass = holder_klass->get_inline_type_field_klass(fld.index());
      if (!equal_value_objects(obj1, obj2, field_klass, field_offset)) {
        return false;
      }
    } else {
      if (!equal_fields(fld.signature()->char_at(0), obj1, obj2, field_offset)) {
        return false;
      }
    }
  }
  return true;
}

static bool equal_oops(oop obj1, oop obj2) {
  if (obj1 == obj2) {
    return true;
  }

  if (EnableValhalla) {
    if (obj1 != nullptr && obj2 != nullptr && obj1->klass() == obj2->klass() && obj1->is_inline_type()) {
      InlineKlass* vk = InlineKlass::cast(obj1->klass());
      return equal_value_objects(obj1, obj2, vk, 0);
    }
  }
  return false;
}

bool JvmtiTagMapKey::equals(const JvmtiTagMapKey& lhs, const JvmtiTagMapKey& rhs) {
  oop lhs_obj = lhs._obj != nullptr ? lhs._obj : lhs.object_no_keepalive();
  oop rhs_obj = rhs._obj != nullptr ? rhs._obj : rhs.object_no_keepalive();
  return equal_oops(lhs_obj, rhs_obj);
}

// Inline types don't use hash for this table.
static inline bool fast_no_hash_check(oop obj) {
  return (obj->fast_no_hash_check() && !obj->is_inline_type());
}

static const int INITIAL_TABLE_SIZE = 1007;
static const int MAX_TABLE_SIZE     = 0x3fffffff;

JvmtiTagMapTable::JvmtiTagMapTable() : _table(INITIAL_TABLE_SIZE, MAX_TABLE_SIZE) {}

void JvmtiTagMapTable::clear() {
  struct RemoveAll {
    bool do_entry(JvmtiTagMapKey& entry, const jlong& tag) {
      entry.release_handle();
      return true;
    }
  } remove_all;
  // The unlink method of ResourceHashTable gets a pointer to a type whose 'do_entry(K,V)' method is callled
  // while iterating over all the elements of the table. If the do_entry() method returns true the element
  // will be removed.
  // In this case, we always return true from do_entry to clear all the elements.
  _table.unlink(&remove_all);

  assert(_table.number_of_entries() == 0, "should have removed all entries");
}

JvmtiTagMapTable::~JvmtiTagMapTable() {
  clear();
}

jlong JvmtiTagMapTable::find(oop obj) {
  if (is_empty()) {
    return 0;
  }

  if (fast_no_hash_check(obj)) {
    // Objects in the table all have a hashcode, unless inlined types.
    return 0;
  }

  JvmtiTagMapKey jtme(obj);
  jlong* found = _table.get(jtme);
  return found == nullptr ? 0 : *found;
}

void JvmtiTagMapTable::add(oop obj, jlong tag) {
  JvmtiTagMapKey new_entry(obj);
  bool is_added;
  if (fast_no_hash_check(obj)) {
    // Can't be in the table so add it fast.
    is_added = _table.put_when_absent(new_entry, tag);
  } else {
    jlong* value = _table.put_if_absent(new_entry, tag, &is_added);
    *value = tag; // assign the new tag
  }
  if (is_added) {
    if (_table.maybe_grow(5, true /* use_large_table_sizes */)) {
      int max_bucket_size = DEBUG_ONLY(_table.verify()) NOT_DEBUG(0);
      log_info(jvmti, table) ("JvmtiTagMap table resized to %d for %d entries max bucket %d",
                              _table.table_size(), _table.number_of_entries(), max_bucket_size);
    }
  }
}

void JvmtiTagMapTable::remove(oop obj) {
  JvmtiTagMapKey jtme(obj);
  auto clean = [] (JvmtiTagMapKey& entry, jlong tag) {
    entry.release_handle();
  };
  _table.remove(jtme, clean);
}

void JvmtiTagMapTable::entry_iterate(JvmtiTagMapKeyClosure* closure) {
  _table.iterate(closure);
}

void JvmtiTagMapTable::remove_dead_entries(GrowableArray<jlong>* objects) {
  struct IsDead {
    GrowableArray<jlong>* _objects;
    IsDead(GrowableArray<jlong>* objects) : _objects(objects) {}
    bool do_entry(JvmtiTagMapKey& entry, jlong tag) {
      if (entry.object_no_keepalive() == nullptr) {
        if (_objects != nullptr) {
          _objects->append(tag);
        }
        entry.release_handle();
        return true;
      }
      return false;;
    }
  } is_dead(objects);
  _table.unlink(&is_dead);
}
