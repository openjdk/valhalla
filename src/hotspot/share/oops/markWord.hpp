/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "metaprogramming/integralConstant.hpp"
#include "metaprogramming/primitiveConversions.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/globals.hpp"

// The markWord describes the header of an object.
//
// Bit-format of an object header (most significant first, big endian layout below):
//
//  32 bits:
//  --------
//             hash:25 ------------>| age:4    biased_lock:1 lock:2 (normal object)
//             JavaThread*:23 epoch:2 age:4    biased_lock:1 lock:2 (biased object)
//
//  64 bits:
//  --------
//  unused:25 hash:31 -->| unused_gap:1   age:4    biased_lock:1 lock:2 (normal object)
//  JavaThread*:54 epoch:2 unused_gap:1   age:4    biased_lock:1 lock:2 (biased object)
//
//  - hash contains the identity hash value: largest value is
//    31 bits, see os::random().  Also, 64-bit vm's require
//    a hash value no bigger than 32 bits because they will not
//    properly generate a mask larger than that: see library_call.cpp
//
//  - the biased lock pattern is used to bias a lock toward a given
//    thread. When this pattern is set in the low three bits, the lock
//    is either biased toward a given thread or "anonymously" biased,
//    indicating that it is possible for it to be biased. When the
//    lock is biased toward a given thread, locking and unlocking can
//    be performed by that thread without using atomic operations.
//    When a lock's bias is revoked, it reverts back to the normal
//    locking scheme described below.
//
//    Note that we are overloading the meaning of the "unlocked" state
//    of the header. Because we steal a bit from the age we can
//    guarantee that the bias pattern will never be seen for a truly
//    unlocked object.
//
//    Note also that the biased state contains the age bits normally
//    contained in the object header. Large increases in scavenge
//    times were seen when these bits were absent and an arbitrary age
//    assigned to all biased objects, because they tended to consume a
//    significant fraction of the eden semispaces and were not
//    promoted promptly, causing an increase in the amount of copying
//    performed. The runtime system aligns all JavaThread* pointers to
//    a very large value (currently 128 bytes (32bVM) or 256 bytes (64bVM))
//    to make room for the age bits & the epoch bits (used in support of
//    biased locking).
//
//    [JavaThread* | epoch | age | 1 | 01]       lock is biased toward given thread
//    [0           | epoch | age | 1 | 01]       lock is anonymously biased
//
//  - the two lock bits are used to describe three states: locked/unlocked and monitor.
//
//    [ptr             | 00]  locked             ptr points to real header on stack
//    [header      | 0 | 01]  unlocked           regular object header
//    [ptr             | 10]  monitor            inflated lock (header is wapped out)
//    [ptr             | 11]  marked             used to mark an object
//    [0 ............ 0| 00]  inflating          inflation in progress
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
//  UseBiasedLocking / EnableValhalla
//
//  Making the assumption that "biased locking" will be removed before inline types
//  are introduced to mainline. However, to ease future merge conflict work, left
//  bias locking code in place for now. UseBiasedLocking cannot be enabled.
//
//  "biased lock bit" is free to use: using this bit to indicate inline type,
//  combined with "unlocked" lock bits, means we will not interfere with lock
//  encodings (displaced, inflating, and monitor), since inline types can't be
//  locked.
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
//  unused:1 | <-- hash:31 -->| unused:22 larval:1 age:4 flat_array:1 nullfree_array:1 inline_type:1 lock:2
//
//  The "fast" static type bits (flat_array, nullfree_array, and inline_type)
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

  markWord() { /* uninitialized */}

  // It is critical for performance that this class be trivially
  // destructable, copyable, and assignable.

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
  static const int biased_lock_bits               = 1; // Valhalla: unused
  // static prototype header bits (fast path instead of klass layout_helper)
  static const int inline_type_bits               = 1;
  static const int nullfree_array_bits            = LP64_ONLY(1) NOT_LP64(0);
  static const int flat_array_bits                = LP64_ONLY(1) NOT_LP64(0);
  // instance state
  static const int age_bits                       = 4;
  static const int larval_bits                    = 1;
  static const int max_hash_bits                  = BitsPerWord - age_bits - lock_bits - inline_type_bits - larval_bits - flat_array_bits - nullfree_array_bits;
  static const int hash_bits                      = max_hash_bits > 31 ? 31 : max_hash_bits;
  static const int unused_gap_bits                = LP64_ONLY(1) NOT_LP64(0); // Valhalla: unused
  static const int epoch_bits                     = 2; // Valhalla: unused

  // The biased locking code currently requires that the age bits be
  // contiguous to the lock bits.
  static const int lock_shift                     = 0;
  static const int biased_lock_shift              = lock_bits;
  static const int inline_type_shift              = lock_bits;
  static const int nullfree_array_shift           = inline_type_shift + inline_type_bits;
  static const int flat_array_shift               = nullfree_array_shift + nullfree_array_bits;
  static const int age_shift                      = flat_array_shift + flat_array_bits;
  static const int unused_gap_shift               = age_shift + age_bits; // Valhalla: unused
  static const int larval_shift                   = age_shift + age_bits;
  static const int hash_shift                     = LP64_ONLY(32) NOT_LP64(larval_shift + larval_bits);
  static const int epoch_shift                    = unused_gap_shift + unused_gap_bits /*hash_shift*/; // Valhalla: unused

  static const uintptr_t lock_mask                = right_n_bits(lock_bits);
  static const uintptr_t lock_mask_in_place       = lock_mask << lock_shift;
  static const uintptr_t biased_lock_mask         = right_n_bits(lock_bits + biased_lock_bits); // Valhalla: unused
  static const uintptr_t biased_lock_mask_in_place= biased_lock_mask << lock_shift; // Valhalla: unused
  static const uintptr_t biased_lock_bit_in_place = 1 << biased_lock_shift; // Valhalla: unused
  static const uintptr_t inline_type_mask         = right_n_bits(lock_bits + inline_type_bits);
  static const uintptr_t inline_type_mask_in_place = inline_type_mask << lock_shift;
  static const uintptr_t inline_type_bit_in_place = 1 << inline_type_shift;
  static const uintptr_t nullfree_array_mask      = right_n_bits(nullfree_array_bits);
  static const uintptr_t nullfree_array_mask_in_place = (nullfree_array_mask << nullfree_array_shift) | lock_mask_in_place;
  static const uintptr_t nullfree_array_bit_in_place = (1 << nullfree_array_shift);
  static const uintptr_t flat_array_mask          = right_n_bits(flat_array_bits);
  static const uintptr_t flat_array_mask_in_place = (flat_array_mask << flat_array_shift) | nullfree_array_mask_in_place | lock_mask_in_place;
  static const uintptr_t flat_array_bit_in_place  = (1 << flat_array_shift);

  static const uintptr_t age_mask                 = right_n_bits(age_bits);
  static const uintptr_t age_mask_in_place        = age_mask << age_shift;

  static const uintptr_t larval_mask              = right_n_bits(larval_bits);
  static const uintptr_t larval_mask_in_place     = (larval_mask << larval_shift) | inline_type_mask_in_place;
  static const uintptr_t larval_bit_in_place      = (1 << larval_shift);

  static const uintptr_t epoch_mask               = right_n_bits(epoch_bits); // Valhalla: unused
  static const uintptr_t epoch_mask_in_place      = epoch_mask << epoch_shift;// Valhalla: unused

  static const uintptr_t hash_mask                = right_n_bits(hash_bits);
  static const uintptr_t hash_mask_in_place       = hash_mask << hash_shift;

  // Alignment of JavaThread pointers encoded in object header required by biased locking
  static const size_t biased_lock_alignment       = 2 << (epoch_shift + epoch_bits); // Valhalla: unused

  static const uintptr_t locked_value             = 0;
  static const uintptr_t unlocked_value           = 1;
  static const uintptr_t monitor_value            = 2;
  static const uintptr_t marked_value             = 3;
  static const uintptr_t biased_lock_pattern      = 5; // Valhalla: unused

  static const uintptr_t inline_type_pattern      = inline_type_bit_in_place | unlocked_value;
  static const uintptr_t nullfree_array_pattern   = nullfree_array_bit_in_place | unlocked_value;
  static const uintptr_t flat_array_pattern       = flat_array_bit_in_place | nullfree_array_pattern;
  static const uintptr_t static_prototype_mask    = LP64_ONLY(right_n_bits(inline_type_bits + flat_array_bits + nullfree_array_bits)) NOT_LP64(right_n_bits(inline_type_bits));
  static const uintptr_t static_prototype_mask_in_place = static_prototype_mask << lock_bits;
  static const uintptr_t static_prototype_value_max = (1 << age_shift) - 1;

  static const uintptr_t larval_pattern           = larval_bit_in_place | inline_type_pattern;

  static const uintptr_t no_hash                  = 0 ;  // no hash value assigned
  static const uintptr_t no_hash_in_place         = (address_word)no_hash << hash_shift;
  static const uintptr_t no_lock_in_place         = unlocked_value;

  static const uint max_age                       = age_mask;

  static const int max_bias_epoch                 = epoch_mask;

  // Creates a markWord with all bits set to zero.
  static markWord zero() { return markWord(uintptr_t(0)); }

  bool is_inline_type() const {
    return (mask_bits(value(), inline_type_mask_in_place) == inline_type_pattern);
  }

  // Biased Locking accessors.
  // These must be checked by all code which calls into the
  // ObjectSynchronizer and other code. The biasing is not understood
  // by the lower-level CAS-based locking code, although the runtime
  // fixes up biased locks to be compatible with it when a bias is
  // revoked.
  bool has_bias_pattern() const {
    ShouldNotReachHere(); // Valhalla: unused
    return (mask_bits(value(), biased_lock_mask_in_place) == biased_lock_pattern);
  }
  JavaThread* biased_locker() const {
    ShouldNotReachHere(); // Valhalla: unused
    assert(has_bias_pattern(), "should not call this otherwise");
    return (JavaThread*) mask_bits(value(), ~(biased_lock_mask_in_place | age_mask_in_place | epoch_mask_in_place));
  }
  // Indicates that the mark has the bias bit set but that it has not
  // yet been biased toward a particular thread
  bool is_biased_anonymously() const {
    ShouldNotReachHere(); // Valhalla: unused
    return (has_bias_pattern() && (biased_locker() == NULL));
  }
  // Indicates epoch in which this bias was acquired. If the epoch
  // changes due to too many bias revocations occurring, the biases
  // from the previous epochs are all considered invalid.
  int bias_epoch() const {
    ShouldNotReachHere(); // Valhalla: unused
    assert(has_bias_pattern(), "should not call this otherwise");
    return (mask_bits(value(), epoch_mask_in_place) >> epoch_shift);
  }
  markWord set_bias_epoch(int epoch) {
    ShouldNotReachHere(); // Valhalla: unused
    assert(has_bias_pattern(), "should not call this otherwise");
    assert((epoch & (~epoch_mask)) == 0, "epoch overflow");
    return markWord(mask_bits(value(), ~epoch_mask_in_place) | (epoch << epoch_shift));
  }
  markWord incr_bias_epoch() {
    ShouldNotReachHere(); // Valhalla: unused
    return set_bias_epoch((1 + bias_epoch()) & epoch_mask);
  }
  // Prototype mark for initialization
  static markWord biased_locking_prototype() {
    ShouldNotReachHere(); // Valhalla: unused
    return markWord( biased_lock_pattern );
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
  bool is_neutral()  const { return (mask_bits(value(), inline_type_mask_in_place) == unlocked_value); }

  // Special temporary state of the markWord while being inflated.
  // Code that looks at mark outside a lock need to take this into account.
  bool is_being_inflated() const { return (value() == 0); }

  // Distinguished markword value - used when inflating over
  // an existing stack-lock.  0 indicates the markword is "BUSY".
  // Lockword mutators that use a LD...CAS idiom should always
  // check for and avoid overwriting a 0 value installed by some
  // other thread.  (They should spin or block instead.  The 0 value
  // is transient and *should* be short-lived).
  static markWord INFLATING() { return zero(); }    // inflate-in-progress

  // Should this header be preserved during GC?
  template <typename KlassProxy>
  inline bool must_be_preserved(KlassProxy klass) const;

  // Should this header (including its age bits) be preserved in the
  // case of a promotion failure during scavenge?
  // Note that we special case this situation. We want to avoid
  // calling BiasedLocking::preserve_marks()/restore_marks() (which
  // decrease the number of mark words that need to be preserved
  // during GC) during each scavenge. During scavenges in which there
  // is no promotion failure, we actually don't need to call the above
  // routines at all, since we don't mutate and re-initialize the
  // marks of promoted objects using init_mark(). However, during
  // scavenges which result in promotion failure, we do re-initialize
  // the mark words of objects, meaning that we should have called
  // these mark word preservation routines. Currently there's no good
  // place in which to call them in any of the scavengers (although
  // guarded by appropriate locks we could make one), but the
  // observation is that promotion failures are quite rare and
  // reducing the number of mark words preserved during them isn't a
  // high priority.
  template <typename KlassProxy>
  inline bool must_be_preserved_for_promotion_failure(KlassProxy klass) const;

  // WARNING: The following routines are used EXCLUSIVELY by
  // synchronization functions. They are not really gc safe.
  // They must get updated if markWord layout get changed.
  markWord set_unlocked() const {
    return markWord(value() | unlocked_value);
  }
  bool has_locker() const {
    return ((value() & lock_mask_in_place) == locked_value);
  }
  BasicLock* locker() const {
    assert(has_locker(), "check");
    return (BasicLock*) value();
  }
  bool has_monitor() const {
    return ((value() & monitor_value) != 0);
  }
  ObjectMonitor* monitor() const {
    assert(has_monitor(), "check");
    // Use xor instead of &~ to provide one extra tag-bit check.
    return (ObjectMonitor*) (value() ^ monitor_value);
  }
  bool has_displaced_mark_helper() const {
    return ((value() & unlocked_value) == 0);
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
  static markWord encode(JavaThread* thread, uint age, int bias_epoch) {
    uintptr_t tmp = (uintptr_t) thread;
    assert(UseBiasedLocking && ((tmp & (epoch_mask_in_place | age_mask_in_place | biased_lock_mask_in_place)) == 0), "misaligned JavaThread pointer");
    assert(age <= max_age, "age too large");
    assert(bias_epoch <= max_bias_epoch, "bias epoch too large");
    return markWord(tmp | (bias_epoch << epoch_shift) | (age << age_shift) | biased_lock_pattern);
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

  bool is_nullfree_array() const {
    return (mask_bits(value(), nullfree_array_mask_in_place) == nullfree_array_pattern);
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

  static markWord nullfree_array_prototype() {
    return markWord(nullfree_array_pattern);
  }
#endif
    // Helper function for restoration of unmarked mark oops during GC
  static inline markWord prototype_for_klass(const Klass* klass);

  // Debugging
  void print_on(outputStream* st, bool print_monitor_info = true) const;

  // Prepare address of oop for placement into mark
  inline static markWord encode_pointer_as_mark(void* p) { return from_pointer(p).set_marked(); }

  // Recover address of oop from encoded form used in mark
  inline void* decode_pointer() { return (EnableValhalla && _value < static_prototype_value_max) ? NULL : (void*) (clear_lock_bits().value()); }
};

// Support atomic operations.
template<>
struct PrimitiveConversions::Translate<markWord> : public TrueType {
  typedef markWord Value;
  typedef uintptr_t Decayed;

  static Decayed decay(const Value& x) { return x.value(); }
  static Value recover(Decayed x) { return Value(x); }
};

#endif // SHARE_OOPS_MARKWORD_HPP
