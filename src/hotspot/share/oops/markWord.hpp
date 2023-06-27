/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_MARKWORD_HPP
#define SHARE_OOPS_MARKWORD_HPP

#include "metaprogramming/primitiveConversions.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/globals.hpp"

#include <type_traits>

// The markWord describes the header of an object.
//
// Bit-format of an object header (most significant first, big endian layout below):
//
//  32 bits:
//  --------
//             hash:25 ------------>| age:4  unused_gap:1  lock:2 (normal object)
//
//  64 bits:
//  --------
//  unused:25 hash:31 -->| unused_gap:1  age:4  unused_gap:1  lock:2 (normal object)
//
//  - hash contains the identity hash value: largest value is
//    31 bits, see os::random().  Also, 64-bit vm's require
//    a hash value no bigger than 32 bits because they will not
//    properly generate a mask larger than that: see library_call.cpp
//
//  - the two lock bits are used to describe three states: locked/unlocked and monitor.
//
//    [ptr             | 00]  locked             ptr points to real header on stack (stack-locking in use)
//    [header          | 00]  locked             locked regular object header (fast-locking in use)
//    [header          | 01]  unlocked           regular object header
//    [ptr             | 10]  monitor            inflated lock (header is swapped out)
//    [ptr             | 11]  marked             used to mark an object
//    [0 ............ 0| 00]  inflating          inflation in progress (stack-locking in use)
//
//    We assume that stack/thread pointers have the lowest two bits cleared.
//
//
//  - INFLATING() is a distinguished markword value of all zeros that is
//    used when inflating an existing stack-lock into an ObjectMonitor.
//    See below for is_being_inflated() and INFLATING().
//
//
//
//  Valhalla
//
//  <CMH: merge this doc into the text above>
//
//  Project Valhalla has mark word encoding requirements for the following oops:
//
//  * inline types: have alternative bytecode behavior, e.g. can not be locked
//    - "larval state": mutable state, but only during object init, observable
//      by only by a single thread (generally do not mutate markWord)
//
//  * flat arrays: load/decode of klass layout helper is expensive for aaload
//
//  * "null free" arrays: load/decode of klass layout helper again for aaload
//
//  EnableValhalla
//
//  Formerly known as "biased lock bit", "unused_gap" is free to use: using this
//  bit to indicate inline type, combined with "unlocked" lock bits, means we
//  will not interfere with lock encodings (displaced, inflating, and monitor),
//  since inline types can't be locked.
//
//  Further state encoding
//
//  32 bit plaforms currently have no further room for encoding. No room for
//  "denormalized layout helper bits", these fast mark word tests can only be made on
//  64 bit platforms. 32-bit platforms need to load the klass->_layout_helper. This
//  said, the larval state bit is still required for operation, stealing from the hash
//  code is simplest mechanism.
//
//  Valhalla specific encodings
//
//  Revised Bit-format of an object header (most significant first, big endian layout below):
//
//  32 bits:
//  --------
//  hash:24 ------------>| larval:1 age:4 inline_type:1 lock:2
//
//  64 bits:
//  --------
//  unused:1 | <-- hash:31 -->| unused:22 larval:1 age:4 flat_array:1 null_free_array:1 inline_type:1 lock:2
//
//  The "fast" static type bits (flat_array, null_free_array, and inline_type)
//  are placed lowest next to lock bits to more easily decode forwarding pointers.
//  G1 for example, implicitly clears age bits ("G1FullGCCompactionPoint::forward()")
//  using "oopDesc->forwardee()", so it necessary for "markWord::decode_pointer()"
//  to return a non-NULL for this case, but not confuse the static type bits for
//  a pointer.
//
//  Static types bits are recorded in the "klass->prototype_header()", displaced
//  mark should simply use the prototype header as "slow path", rather chasing
//  monitor or stack lock races.
//
//  Lock patterns (note inline types can't be locked/monitor/inflating)...
//
//  [ptr            | 000]  locked             ptr points to real header on stack
//  [header         | ?01]  unlocked           regular object header
//  [ptr            | 010]  monitor            inflated lock (header is wapped out)
//  [ptr            | ?11]  marked             used to mark an object
//  [0 ............ | 000]  inflating          inflation in progress
//
//

class BasicLock;
class ObjectMonitor;
class JavaThread;
class outputStream;

class markWord {
 private:
  uintptr_t _value;

 public:
  explicit markWord(uintptr_t value) : _value(value) {}

  markWord() = default;         // Doesn't initialize _value.

  // It is critical for performance that this class be trivially
  // destructable, copyable, and assignable.
  ~markWord() = default;
  markWord(const markWord&) = default;
  markWord& operator=(const markWord&) = default;

  static markWord from_pointer(void* ptr) {
    return markWord((uintptr_t)ptr);
  }
  void* to_pointer() const {
    return (void*)_value;
  }

  bool operator==(const markWord& other) const {
    return _value == other._value;
  }
  bool operator!=(const markWord& other) const {
    return !operator==(other);
  }

  // Conversion
  uintptr_t value() const { return _value; }

  // Constants, in least significant bit order
  static const int lock_bits                      = 2;
  static const int first_unused_gap_bits          = 1; // When !EnableValhalla
  // EnableValhalla: static prototype header bits (fast path instead of klass layout_helper)
  static const int inline_type_bits               = 1;
  static const int null_free_array_bits           = LP64_ONLY(1) NOT_LP64(0);
  static const int flat_array_bits                = LP64_ONLY(1) NOT_LP64(0);
  // instance state
  static const int age_bits                       = 4;
  static const int larval_bits                    = 1;
  static const int max_hash_bits                  = BitsPerWord - age_bits - lock_bits - inline_type_bits - larval_bits - flat_array_bits - null_free_array_bits;
  static const int hash_bits                      = max_hash_bits > 31 ? 31 : max_hash_bits;
  static const int second_unused_gap_bits         = LP64_ONLY(1) NOT_LP64(0); // !EnableValhalla: unused

  static const int lock_shift                     = 0;
  static const int inline_type_shift              = lock_bits;
  static const int null_free_array_shift          = inline_type_shift + inline_type_bits;
  static const int flat_array_shift               = null_free_array_shift + null_free_array_bits;
  static const int age_shift                      = flat_array_shift + flat_array_bits;
  static const int unused_gap_shift               = age_shift + age_bits; // !EnableValhalla: unused
  static const int larval_shift                   = age_shift + age_bits;
  static const int hash_shift                     = LP64_ONLY(32) NOT_LP64(larval_shift + larval_bits);

  static const uintptr_t lock_mask                = right_n_bits(lock_bits);
  static const uintptr_t lock_mask_in_place       = lock_mask << lock_shift;
  static const uintptr_t inline_type_mask         = right_n_bits(lock_bits + inline_type_bits);
  static const uintptr_t inline_type_mask_in_place = inline_type_mask << lock_shift;
  static const uintptr_t inline_type_bit_in_place = 1 << inline_type_shift;
  static const uintptr_t null_free_array_mask     = right_n_bits(null_free_array_bits);
  static const uintptr_t null_free_array_mask_in_place = (null_free_array_mask << null_free_array_shift) | lock_mask_in_place;
  static const uintptr_t null_free_array_bit_in_place  = (1 << null_free_array_shift);
  static const uintptr_t flat_array_mask          = right_n_bits(flat_array_bits);
  static const uintptr_t flat_array_mask_in_place = (flat_array_mask << flat_array_shift) | null_free_array_mask_in_place | lock_mask_in_place;
  static const uintptr_t flat_array_bit_in_place  = (1 << flat_array_shift);

  static const uintptr_t age_mask                 = right_n_bits(age_bits);
  static const uintptr_t age_mask_in_place        = age_mask << age_shift;

  static const uintptr_t larval_mask              = right_n_bits(larval_bits);
  static const uintptr_t larval_mask_in_place     = (larval_mask << larval_shift) | inline_type_mask_in_place;
  static const uintptr_t larval_bit_in_place      = (1 << larval_shift);

  static const uintptr_t hash_mask                = right_n_bits(hash_bits);
  static const uintptr_t hash_mask_in_place       = hash_mask << hash_shift;

  static const uintptr_t locked_value             = 0;
  static const uintptr_t unlocked_value           = 1;
  static const uintptr_t monitor_value            = 2;
  static const uintptr_t marked_value             = 3;

  static const uintptr_t inline_type_pattern      = inline_type_bit_in_place | unlocked_value;
  static const uintptr_t null_free_array_pattern  = null_free_array_bit_in_place | unlocked_value;
  static const uintptr_t flat_array_pattern       = flat_array_bit_in_place | null_free_array_pattern;
  // Has static klass prototype, used for decode/encode pointer
  static const uintptr_t static_prototype_mask    = LP64_ONLY(right_n_bits(inline_type_bits + flat_array_bits + null_free_array_bits)) NOT_LP64(right_n_bits(inline_type_bits));
  static const uintptr_t static_prototype_mask_in_place = static_prototype_mask << lock_bits;
  static const uintptr_t static_prototype_value_max = (1 << age_shift) - 1;

  static const uintptr_t larval_pattern           = larval_bit_in_place | inline_type_pattern;

  static const uintptr_t no_hash                  = 0 ;  // no hash value assigned
  static const uintptr_t no_hash_in_place         = (address_word)no_hash << hash_shift;
  static const uintptr_t no_lock_in_place         = unlocked_value;

  static const uint max_age                       = age_mask;

  // Creates a markWord with all bits set to zero.
  static markWord zero() { return markWord(uintptr_t(0)); }

  bool is_inline_type() const {
    return (mask_bits(value(), inline_type_mask_in_place) == inline_type_pattern);
  }

  // lock accessors (note that these assume lock_shift == 0)
  bool is_locked()   const {
    return (mask_bits(value(), lock_mask_in_place) != unlocked_value);
  }
  bool is_unlocked() const {
    return (mask_bits(value(), lock_mask_in_place) == unlocked_value);
  }
  bool is_marked()   const {
    return (mask_bits(value(), lock_mask_in_place) == marked_value);
  }

  // is unlocked and not an inline type (which cannot be involved in locking, displacement or inflation)
  // i.e. test both lock bits and the inline type bit together
  bool is_neutral()  const {
    return (mask_bits(value(), inline_type_mask_in_place) == unlocked_value);
  }

  // Special temporary state of the markWord while being inflated.
  // Code that looks at mark outside a lock need to take this into account.
  bool is_being_inflated() const { return (value() == 0); }

  // Distinguished markword value - used when inflating over
  // an existing stack-lock.  0 indicates the markword is "BUSY".
  // Lockword mutators that use a LD...CAS idiom should always
  // check for and avoid overwriting a 0 value installed by some
  // other thread.  (They should spin or block instead.  The 0 value
  // is transient and *should* be short-lived).
  // Fast-locking does not use INFLATING.
  static markWord INFLATING() { return zero(); }    // inflate-in-progress

  // Should this header be preserved during GC?
  bool must_be_preserved(const oopDesc* obj) const {
    return (!is_unlocked() || !has_no_hash() || (EnableValhalla && is_larval_state()));
  }

  // WARNING: The following routines are used EXCLUSIVELY by
  // synchronization functions. They are not really gc safe.
  // They must get updated if markWord layout get changed.
  markWord set_unlocked() const {
    return markWord(value() | unlocked_value);
  }
  bool has_locker() const {
    assert(LockingMode == LM_LEGACY, "should only be called with legacy stack locking");
    return (value() & lock_mask_in_place) == locked_value;
  }
  BasicLock* locker() const {
    assert(has_locker(), "check");
    return (BasicLock*) value();
  }

  bool is_fast_locked() const {
    assert(LockingMode == LM_LIGHTWEIGHT, "should only be called with new lightweight locking");
    return (value() & lock_mask_in_place) == locked_value;
  }
  markWord set_fast_locked() const {
    // Clear the lock_mask_in_place bits to set locked_value:
    return markWord(value() & ~lock_mask_in_place);
  }

  bool has_monitor() const {
    return ((value() & lock_mask_in_place) == monitor_value);
  }
  ObjectMonitor* monitor() const {
    assert(has_monitor(), "check");
    // Use xor instead of &~ to provide one extra tag-bit check.
    return (ObjectMonitor*) (value() ^ monitor_value);
  }
  bool has_displaced_mark_helper() const {
    intptr_t lockbits = value() & lock_mask_in_place;
    return LockingMode == LM_LIGHTWEIGHT  ? lockbits == monitor_value   // monitor?
                                          : (lockbits & unlocked_value) == 0; // monitor | stack-locked?
  }
  markWord displaced_mark_helper() const;
  void set_displaced_mark_helper(markWord m) const;
  markWord copy_set_hash(intptr_t hash) const {
    uintptr_t tmp = value() & (~hash_mask_in_place);
    tmp |= ((hash & hash_mask) << hash_shift);
    return markWord(tmp);
  }
  // it is only used to be stored into BasicLock as the
  // indicator that the lock is using heavyweight monitor
  static markWord unused_mark() {
    return markWord(marked_value);
  }
  // the following two functions create the markWord to be
  // stored into object header, it encodes monitor info
  static markWord encode(BasicLock* lock) {
    return from_pointer(lock);
  }
  static markWord encode(ObjectMonitor* monitor) {
    uintptr_t tmp = (uintptr_t) monitor;
    return markWord(tmp | monitor_value);
  }

  // used to encode pointers during GC
  markWord clear_lock_bits() { return markWord(value() & ~lock_mask_in_place); }

  // age operations
  markWord set_marked()   { return markWord((value() & ~lock_mask_in_place) | marked_value); }
  markWord set_unmarked() { return markWord((value() & ~lock_mask_in_place) | unlocked_value); }

  uint     age()           const { return mask_bits(value() >> age_shift, age_mask); }
  markWord set_age(uint v) const {
    assert((v & ~age_mask) == 0, "shouldn't overflow age field");
    return markWord((value() & ~age_mask_in_place) | ((v & age_mask) << age_shift));
  }
  markWord incr_age()      const { return age() == max_age ? markWord(_value) : set_age(age() + 1); }

  // hash operations
  intptr_t hash() const {
    return mask_bits(value() >> hash_shift, hash_mask);
  }

  bool has_no_hash() const {
    return hash() == no_hash;
  }

  // private buffered value operations
  markWord enter_larval_state() const {
    return markWord(value() | larval_bit_in_place);
  }
  markWord exit_larval_state() const {
    return markWord(value() & ~larval_bit_in_place);
  }
  bool is_larval_state() const {
    return (mask_bits(value(), larval_mask_in_place) == larval_pattern);
  }

#ifdef _LP64 // 64 bit encodings only
  bool is_flat_array() const {
    return (mask_bits(value(), flat_array_mask_in_place) == flat_array_pattern);
  }

  bool is_null_free_array() const {
    return (mask_bits(value(), null_free_array_mask_in_place) == null_free_array_pattern);
  }
#else
  bool is_flat_array() const {
    fatal("Should not ask this for mark word, ask oopDesc");
    return false;
  }

  bool is_null_free_array() const {
    fatal("Should not ask this for mark word, ask oopDesc");
    return false;
  }
#endif
  // Prototype mark for initialization
  static markWord prototype() {
    return markWord( no_hash_in_place | no_lock_in_place );
  }

  static markWord inline_type_prototype() {
    return markWord(inline_type_pattern);
  }

#ifdef _LP64 // 64 bit encodings only
  static markWord flat_array_prototype() {
    return markWord(flat_array_pattern);
  }

  static markWord null_free_array_prototype() {
    return markWord(null_free_array_pattern);
  }
#endif

  // Debugging
  void print_on(outputStream* st, bool print_monitor_info = true) const;

  // Prepare address of oop for placement into mark
  inline static markWord encode_pointer_as_mark(void* p) { return from_pointer(p).set_marked(); }

  // Recover address of oop from encoded form used in mark
  inline void* decode_pointer() {
    return (EnableValhalla && _value < static_prototype_value_max) ? NULL :
      (void*) (clear_lock_bits().value());
  }
};

// Support atomic operations.
template<>
struct PrimitiveConversions::Translate<markWord> : public std::true_type {
  typedef markWord Value;
  typedef uintptr_t Decayed;

  static Decayed decay(const Value& x) { return x.value(); }
  static Value recover(Decayed x) { return Value(x); }
};

#endif // SHARE_OOPS_MARKWORD_HPP
