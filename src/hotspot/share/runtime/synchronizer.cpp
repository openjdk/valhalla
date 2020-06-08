/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/vmSymbols.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/metaspaceShared.hpp"
#include "memory/padded.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/markWord.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/biasedLocking.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "runtime/osThread.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/timer.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/align.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/preserveException.hpp"

// The "core" versions of monitor enter and exit reside in this file.
// The interpreter and compilers contain specialized transliterated
// variants of the enter-exit fast-path operations.  See i486.ad fast_lock(),
// for instance.  If you make changes here, make sure to modify the
// interpreter, and both C1 and C2 fast-path inline locking code emission.
//
// -----------------------------------------------------------------------------

#ifdef DTRACE_ENABLED

// Only bother with this argument setup if dtrace is available
// TODO-FIXME: probes should not fire when caller is _blocked.  assert() accordingly.

#define DTRACE_MONITOR_PROBE_COMMON(obj, thread)                           \
  char* bytes = NULL;                                                      \
  int len = 0;                                                             \
  jlong jtid = SharedRuntime::get_java_tid(thread);                        \
  Symbol* klassname = ((oop)(obj))->klass()->name();                       \
  if (klassname != NULL) {                                                 \
    bytes = (char*)klassname->bytes();                                     \
    len = klassname->utf8_length();                                        \
  }

#define DTRACE_MONITOR_WAIT_PROBE(monitor, obj, thread, millis)            \
  {                                                                        \
    if (DTraceMonitorProbes) {                                             \
      DTRACE_MONITOR_PROBE_COMMON(obj, thread);                            \
      HOTSPOT_MONITOR_WAIT(jtid,                                           \
                           (uintptr_t)(monitor), bytes, len, (millis));    \
    }                                                                      \
  }

#define HOTSPOT_MONITOR_PROBE_notify HOTSPOT_MONITOR_NOTIFY
#define HOTSPOT_MONITOR_PROBE_notifyAll HOTSPOT_MONITOR_NOTIFYALL
#define HOTSPOT_MONITOR_PROBE_waited HOTSPOT_MONITOR_WAITED

#define DTRACE_MONITOR_PROBE(probe, monitor, obj, thread)                  \
  {                                                                        \
    if (DTraceMonitorProbes) {                                             \
      DTRACE_MONITOR_PROBE_COMMON(obj, thread);                            \
      HOTSPOT_MONITOR_PROBE_##probe(jtid, /* probe = waited */             \
                                    (uintptr_t)(monitor), bytes, len);     \
    }                                                                      \
  }

#else //  ndef DTRACE_ENABLED

#define DTRACE_MONITOR_WAIT_PROBE(obj, thread, millis, mon)    {;}
#define DTRACE_MONITOR_PROBE(probe, obj, thread, mon)          {;}

#endif // ndef DTRACE_ENABLED

// This exists only as a workaround of dtrace bug 6254741
int dtrace_waited_probe(ObjectMonitor* monitor, Handle obj, Thread* thr) {
  DTRACE_MONITOR_PROBE(waited, monitor, obj(), thr);
  return 0;
}

#define NINFLATIONLOCKS 256
static volatile intptr_t gInflationLocks[NINFLATIONLOCKS];

// global list of blocks of monitors
PaddedObjectMonitor* ObjectSynchronizer::g_block_list = NULL;

struct ObjectMonitorListGlobals {
  char         _pad_prefix[OM_CACHE_LINE_SIZE];
  // These are highly shared list related variables.
  // To avoid false-sharing they need to be the sole occupants of a cache line.

  // Global ObjectMonitor free list. Newly allocated and deflated
  // ObjectMonitors are prepended here.
  ObjectMonitor* _free_list;
  DEFINE_PAD_MINUS_SIZE(1, OM_CACHE_LINE_SIZE, sizeof(ObjectMonitor*));

  // Global ObjectMonitor in-use list. When a JavaThread is exiting,
  // ObjectMonitors on its per-thread in-use list are prepended here.
  ObjectMonitor* _in_use_list;
  DEFINE_PAD_MINUS_SIZE(2, OM_CACHE_LINE_SIZE, sizeof(ObjectMonitor*));

  int _free_count;    // # on free_list
  DEFINE_PAD_MINUS_SIZE(3, OM_CACHE_LINE_SIZE, sizeof(int));

  int _in_use_count;  // # on in_use_list
  DEFINE_PAD_MINUS_SIZE(4, OM_CACHE_LINE_SIZE, sizeof(int));

  int _population;    // # Extant -- in circulation
  DEFINE_PAD_MINUS_SIZE(5, OM_CACHE_LINE_SIZE, sizeof(int));
};
static ObjectMonitorListGlobals om_list_globals;

#define CHECK_THROW_NOSYNC_IMSE(obj)  \
  if ((obj)->mark().is_always_locked()) {  \
    ResourceMark rm(THREAD);                \
    THROW_MSG(vmSymbols::java_lang_IllegalMonitorStateException(), obj->klass()->external_name()); \
  }

#define CHECK_THROW_NOSYNC_IMSE_0(obj)  \
    if ((obj)->mark().is_always_locked()) {  \
    ResourceMark rm(THREAD);                  \
    THROW_MSG_0(vmSymbols::java_lang_IllegalMonitorStateException(), obj->klass()->external_name()); \
  }


#define CHAINMARKER (cast_to_oop<intptr_t>(-1))


// =====================> Spin-lock functions

// ObjectMonitors are not lockable outside of this file. We use spin-locks
// implemented using a bit in the _next_om field instead of the heavier
// weight locking mechanisms for faster list management.

#define OM_LOCK_BIT 0x1

// Return true if the ObjectMonitor is locked.
// Otherwise returns false.
static bool is_locked(ObjectMonitor* om) {
  return ((intptr_t)om->next_om() & OM_LOCK_BIT) == OM_LOCK_BIT;
}

// Mark an ObjectMonitor* with OM_LOCK_BIT and return it.
static ObjectMonitor* mark_om_ptr(ObjectMonitor* om) {
  return (ObjectMonitor*)((intptr_t)om | OM_LOCK_BIT);
}

// Return the unmarked next field in an ObjectMonitor. Note: the next
// field may or may not have been marked with OM_LOCK_BIT originally.
static ObjectMonitor* unmarked_next(ObjectMonitor* om) {
  return (ObjectMonitor*)((intptr_t)om->next_om() & ~OM_LOCK_BIT);
}

// Try to lock an ObjectMonitor. Returns true if locking was successful.
// Otherwise returns false.
static bool try_om_lock(ObjectMonitor* om) {
  // Get current next field without any OM_LOCK_BIT value.
  ObjectMonitor* next = unmarked_next(om);
  if (om->try_set_next_om(next, mark_om_ptr(next)) != next) {
    return false;  // Cannot lock the ObjectMonitor.
  }
  return true;
}

// Lock an ObjectMonitor.
static void om_lock(ObjectMonitor* om) {
  while (true) {
    if (try_om_lock(om)) {
      return;
    }
  }
}

// Unlock an ObjectMonitor.
static void om_unlock(ObjectMonitor* om) {
  ObjectMonitor* next = om->next_om();
  guarantee(((intptr_t)next & OM_LOCK_BIT) == OM_LOCK_BIT, "next=" INTPTR_FORMAT
            " must have OM_LOCK_BIT=%x set.", p2i(next), OM_LOCK_BIT);

  next = (ObjectMonitor*)((intptr_t)next & ~OM_LOCK_BIT);  // Clear OM_LOCK_BIT.
  om->set_next_om(next);
}

// Get the list head after locking it. Returns the list head or NULL
// if the list is empty.
static ObjectMonitor* get_list_head_locked(ObjectMonitor** list_p) {
  while (true) {
    ObjectMonitor* mid = Atomic::load(list_p);
    if (mid == NULL) {
      return NULL;  // The list is empty.
    }
    if (try_om_lock(mid)) {
      if (Atomic::load(list_p) != mid) {
        // The list head changed before we could lock it so we have to retry.
        om_unlock(mid);
        continue;
      }
      return mid;
    }
  }
}

#undef OM_LOCK_BIT


// =====================> List Management functions

// Prepend a list of ObjectMonitors to the specified *list_p. 'tail' is
// the last ObjectMonitor in the list and there are 'count' on the list.
// Also updates the specified *count_p.
static void prepend_list_to_common(ObjectMonitor* list, ObjectMonitor* tail,
                                   int count, ObjectMonitor** list_p,
                                   int* count_p) {
  while (true) {
    ObjectMonitor* cur = Atomic::load(list_p);
    // Prepend list to *list_p.
    if (!try_om_lock(tail)) {
      // Failed to lock tail due to a list walker so try it all again.
      continue;
    }
    tail->set_next_om(cur);  // tail now points to cur (and unlocks tail)
    if (cur == NULL) {
      // No potential race with takers or other prependers since
      // *list_p is empty.
      if (Atomic::cmpxchg(list_p, cur, list) == cur) {
        // Successfully switched *list_p to the list value.
        Atomic::add(count_p, count);
        break;
      }
      // Implied else: try it all again
    } else {
      if (!try_om_lock(cur)) {
        continue;  // failed to lock cur so try it all again
      }
      // We locked cur so try to switch *list_p to the list value.
      if (Atomic::cmpxchg(list_p, cur, list) != cur) {
        // The list head has changed so unlock cur and try again:
        om_unlock(cur);
        continue;
      }
      Atomic::add(count_p, count);
      om_unlock(cur);
      break;
    }
  }
}

// Prepend a newly allocated block of ObjectMonitors to g_block_list and
// om_list_globals._free_list. Also updates om_list_globals._population
// and om_list_globals._free_count.
void ObjectSynchronizer::prepend_block_to_lists(PaddedObjectMonitor* new_blk) {
  // First we handle g_block_list:
  while (true) {
    PaddedObjectMonitor* cur = Atomic::load(&g_block_list);
    // Prepend new_blk to g_block_list. The first ObjectMonitor in
    // a block is reserved for use as linkage to the next block.
    new_blk[0].set_next_om(cur);
    if (Atomic::cmpxchg(&g_block_list, cur, new_blk) == cur) {
      // Successfully switched g_block_list to the new_blk value.
      Atomic::add(&om_list_globals._population, _BLOCKSIZE - 1);
      break;
    }
    // Implied else: try it all again
  }

  // Second we handle om_list_globals._free_list:
  prepend_list_to_common(new_blk + 1, &new_blk[_BLOCKSIZE - 1], _BLOCKSIZE - 1,
                         &om_list_globals._free_list, &om_list_globals._free_count);
}

// Prepend a list of ObjectMonitors to om_list_globals._free_list.
// 'tail' is the last ObjectMonitor in the list and there are 'count'
// on the list. Also updates om_list_globals._free_count.
static void prepend_list_to_global_free_list(ObjectMonitor* list,
                                             ObjectMonitor* tail, int count) {
  prepend_list_to_common(list, tail, count, &om_list_globals._free_list,
                         &om_list_globals._free_count);
}

// Prepend a list of ObjectMonitors to om_list_globals._in_use_list.
// 'tail' is the last ObjectMonitor in the list and there are 'count'
// on the list. Also updates om_list_globals._in_use_list.
static void prepend_list_to_global_in_use_list(ObjectMonitor* list,
                                               ObjectMonitor* tail, int count) {
  prepend_list_to_common(list, tail, count, &om_list_globals._in_use_list,
                         &om_list_globals._in_use_count);
}

// Prepend an ObjectMonitor to the specified list. Also updates
// the specified counter.
static void prepend_to_common(ObjectMonitor* m, ObjectMonitor** list_p,
                              int* count_p) {
  while (true) {
    om_lock(m);  // Lock m so we can safely update its next field.
    ObjectMonitor* cur = NULL;
    // Lock the list head to guard against races with a list walker
    // thread:
    if ((cur = get_list_head_locked(list_p)) != NULL) {
      // List head is now locked so we can safely switch it.
      m->set_next_om(cur);  // m now points to cur (and unlocks m)
      Atomic::store(list_p, m);  // Switch list head to unlocked m.
      om_unlock(cur);
      break;
    }
    // The list is empty so try to set the list head.
    assert(cur == NULL, "cur must be NULL: cur=" INTPTR_FORMAT, p2i(cur));
    m->set_next_om(cur);  // m now points to NULL (and unlocks m)
    if (Atomic::cmpxchg(list_p, cur, m) == cur) {
      // List head is now unlocked m.
      break;
    }
    // Implied else: try it all again
  }
  Atomic::inc(count_p);
}

// Prepend an ObjectMonitor to a per-thread om_free_list.
// Also updates the per-thread om_free_count.
static void prepend_to_om_free_list(Thread* self, ObjectMonitor* m) {
  prepend_to_common(m, &self->om_free_list, &self->om_free_count);
}

// Prepend an ObjectMonitor to a per-thread om_in_use_list.
// Also updates the per-thread om_in_use_count.
static void prepend_to_om_in_use_list(Thread* self, ObjectMonitor* m) {
  prepend_to_common(m, &self->om_in_use_list, &self->om_in_use_count);
}

// Take an ObjectMonitor from the start of the specified list. Also
// decrements the specified counter. Returns NULL if none are available.
static ObjectMonitor* take_from_start_of_common(ObjectMonitor** list_p,
                                                int* count_p) {
  ObjectMonitor* take = NULL;
  // Lock the list head to guard against races with a list walker
  // thread:
  if ((take = get_list_head_locked(list_p)) == NULL) {
    return NULL;  // None are available.
  }
  ObjectMonitor* next = unmarked_next(take);
  // Switch locked list head to next (which unlocks the list head, but
  // leaves take locked):
  Atomic::store(list_p, next);
  Atomic::dec(count_p);
  // Unlock take, but leave the next value for any lagging list
  // walkers. It will get cleaned up when take is prepended to
  // the in-use list:
  om_unlock(take);
  return take;
}

// Take an ObjectMonitor from the start of the om_list_globals._free_list.
// Also updates om_list_globals._free_count. Returns NULL if none are
// available.
static ObjectMonitor* take_from_start_of_global_free_list() {
  return take_from_start_of_common(&om_list_globals._free_list,
                                   &om_list_globals._free_count);
}

// Take an ObjectMonitor from the start of a per-thread free-list.
// Also updates om_free_count. Returns NULL if none are available.
static ObjectMonitor* take_from_start_of_om_free_list(Thread* self) {
  return take_from_start_of_common(&self->om_free_list, &self->om_free_count);
}


// =====================> Quick functions

// The quick_* forms are special fast-path variants used to improve
// performance.  In the simplest case, a "quick_*" implementation could
// simply return false, in which case the caller will perform the necessary
// state transitions and call the slow-path form.
// The fast-path is designed to handle frequently arising cases in an efficient
// manner and is just a degenerate "optimistic" variant of the slow-path.
// returns true  -- to indicate the call was satisfied.
// returns false -- to indicate the call needs the services of the slow-path.
// A no-loitering ordinance is in effect for code in the quick_* family
// operators: safepoints or indefinite blocking (blocking that might span a
// safepoint) are forbidden. Generally the thread_state() is _in_Java upon
// entry.
//
// Consider: An interesting optimization is to have the JIT recognize the
// following common idiom:
//   synchronized (someobj) { .... ; notify(); }
// That is, we find a notify() or notifyAll() call that immediately precedes
// the monitorexit operation.  In that case the JIT could fuse the operations
// into a single notifyAndExit() runtime primitive.

bool ObjectSynchronizer::quick_notify(oopDesc* obj, Thread* self, bool all) {
  assert(!SafepointSynchronize::is_at_safepoint(), "invariant");
  assert(self->is_Java_thread(), "invariant");
  assert(((JavaThread *) self)->thread_state() == _thread_in_Java, "invariant");
  NoSafepointVerifier nsv;
  if (obj == NULL) return false;  // slow-path for invalid obj
  assert(!EnableValhalla || !obj->klass()->is_value(), "monitor op on value type");
  const markWord mark = obj->mark();

  if (mark.has_locker() && self->is_lock_owned((address)mark.locker())) {
    // Degenerate notify
    // stack-locked by caller so by definition the implied waitset is empty.
    return true;
  }

  if (mark.has_monitor()) {
    ObjectMonitor* const mon = mark.monitor();
    assert(mon->object() == obj, "invariant");
    if (mon->owner() != self) return false;  // slow-path for IMS exception

    if (mon->first_waiter() != NULL) {
      // We have one or more waiters. Since this is an inflated monitor
      // that we own, we can transfer one or more threads from the waitset
      // to the entrylist here and now, avoiding the slow-path.
      if (all) {
        DTRACE_MONITOR_PROBE(notifyAll, mon, obj, self);
      } else {
        DTRACE_MONITOR_PROBE(notify, mon, obj, self);
      }
      int free_count = 0;
      do {
        mon->INotify(self);
        ++free_count;
      } while (mon->first_waiter() != NULL && all);
      OM_PERFDATA_OP(Notifications, inc(free_count));
    }
    return true;
  }

  // biased locking and any other IMS exception states take the slow-path
  return false;
}


// The LockNode emitted directly at the synchronization site would have
// been too big if it were to have included support for the cases of inflated
// recursive enter and exit, so they go here instead.
// Note that we can't safely call AsyncPrintJavaStack() from within
// quick_enter() as our thread state remains _in_Java.

bool ObjectSynchronizer::quick_enter(oop obj, Thread* self,
                                     BasicLock * lock) {
  assert(!SafepointSynchronize::is_at_safepoint(), "invariant");
  assert(self->is_Java_thread(), "invariant");
  assert(((JavaThread *) self)->thread_state() == _thread_in_Java, "invariant");
  NoSafepointVerifier nsv;
  if (obj == NULL) return false;       // Need to throw NPE
  assert(!EnableValhalla || !obj->klass()->is_value(), "monitor op on value type");
  const markWord mark = obj->mark();

  if (mark.has_monitor()) {
    ObjectMonitor* const m = mark.monitor();
    assert(m->object() == obj, "invariant");
    Thread* const owner = (Thread *) m->_owner;

    // Lock contention and Transactional Lock Elision (TLE) diagnostics
    // and observability
    // Case: light contention possibly amenable to TLE
    // Case: TLE inimical operations such as nested/recursive synchronization

    if (owner == self) {
      m->_recursions++;
      return true;
    }

    // This Java Monitor is inflated so obj's header will never be
    // displaced to this thread's BasicLock. Make the displaced header
    // non-NULL so this BasicLock is not seen as recursive nor as
    // being locked. We do this unconditionally so that this thread's
    // BasicLock cannot be mis-interpreted by any stack walkers. For
    // performance reasons, stack walkers generally first check for
    // Biased Locking in the object's header, the second check is for
    // stack-locking in the object's header, the third check is for
    // recursive stack-locking in the displaced header in the BasicLock,
    // and last are the inflated Java Monitor (ObjectMonitor) checks.
    lock->set_displaced_header(markWord::unused_mark());

    if (owner == NULL && m->try_set_owner_from(NULL, self) == NULL) {
      assert(m->_recursions == 0, "invariant");
      return true;
    }
  }

  // Note that we could inflate in quick_enter.
  // This is likely a useful optimization
  // Critically, in quick_enter() we must not:
  // -- perform bias revocation, or
  // -- block indefinitely, or
  // -- reach a safepoint

  return false;        // revert to slow-path
}

// -----------------------------------------------------------------------------
// Monitor Enter/Exit
// The interpreter and compiler assembly code tries to lock using the fast path
// of this algorithm. Make sure to update that code if the following function is
// changed. The implementation is extremely sensitive to race condition. Be careful.

void ObjectSynchronizer::enter(Handle obj, BasicLock* lock, TRAPS) {
  CHECK_THROW_NOSYNC_IMSE(obj);
  if (UseBiasedLocking) {
    if (!SafepointSynchronize::is_at_safepoint()) {
      BiasedLocking::revoke(obj, THREAD);
    } else {
      BiasedLocking::revoke_at_safepoint(obj);
    }
  }

  markWord mark = obj->mark();
  assert(!mark.has_bias_pattern(), "should not see bias pattern here");

  if (mark.is_neutral()) {
    // Anticipate successful CAS -- the ST of the displaced mark must
    // be visible <= the ST performed by the CAS.
    lock->set_displaced_header(mark);
    if (mark == obj()->cas_set_mark(markWord::from_pointer(lock), mark)) {
      return;
    }
    // Fall through to inflate() ...
  } else if (mark.has_locker() &&
             THREAD->is_lock_owned((address)mark.locker())) {
    assert(lock != mark.locker(), "must not re-lock the same lock");
    assert(lock != (BasicLock*)obj->mark().value(), "don't relock with same BasicLock");
    lock->set_displaced_header(markWord::from_pointer(NULL));
    return;
  }

  // The object header will never be displaced to this lock,
  // so it does not matter what the value is, except that it
  // must be non-zero to avoid looking like a re-entrant lock,
  // and must not look locked either.
  lock->set_displaced_header(markWord::unused_mark());
  inflate(THREAD, obj(), inflate_cause_monitor_enter)->enter(THREAD);
}

void ObjectSynchronizer::exit(oop object, BasicLock* lock, TRAPS) {
  markWord mark = object->mark();
  if (EnableValhalla && mark.is_always_locked()) {
    return;
  }
  assert(!EnableValhalla || !object->klass()->is_value(), "monitor op on value type");
  // We cannot check for Biased Locking if we are racing an inflation.
  assert(mark == markWord::INFLATING() ||
         !mark.has_bias_pattern(), "should not see bias pattern here");

  markWord dhw = lock->displaced_header();
  if (dhw.value() == 0) {
    // If the displaced header is NULL, then this exit matches up with
    // a recursive enter. No real work to do here except for diagnostics.
#ifndef PRODUCT
    if (mark != markWord::INFLATING()) {
      // Only do diagnostics if we are not racing an inflation. Simply
      // exiting a recursive enter of a Java Monitor that is being
      // inflated is safe; see the has_monitor() comment below.
      assert(!mark.is_neutral(), "invariant");
      assert(!mark.has_locker() ||
             THREAD->is_lock_owned((address)mark.locker()), "invariant");
      if (mark.has_monitor()) {
        // The BasicLock's displaced_header is marked as a recursive
        // enter and we have an inflated Java Monitor (ObjectMonitor).
        // This is a special case where the Java Monitor was inflated
        // after this thread entered the stack-lock recursively. When a
        // Java Monitor is inflated, we cannot safely walk the Java
        // Monitor owner's stack and update the BasicLocks because a
        // Java Monitor can be asynchronously inflated by a thread that
        // does not own the Java Monitor.
        ObjectMonitor* m = mark.monitor();
        assert(((oop)(m->object()))->mark() == mark, "invariant");
        assert(m->is_entered(THREAD), "invariant");
      }
    }
#endif
    return;
  }

  if (mark == markWord::from_pointer(lock)) {
    // If the object is stack-locked by the current thread, try to
    // swing the displaced header from the BasicLock back to the mark.
    assert(dhw.is_neutral(), "invariant");
    if (object->cas_set_mark(dhw, mark) == mark) {
      return;
    }
  }

  // We have to take the slow-path of possible inflation and then exit.
  inflate(THREAD, object, inflate_cause_vm_internal)->exit(true, THREAD);
}

// -----------------------------------------------------------------------------
// Class Loader  support to workaround deadlocks on the class loader lock objects
// Also used by GC
// complete_exit()/reenter() are used to wait on a nested lock
// i.e. to give up an outer lock completely and then re-enter
// Used when holding nested locks - lock acquisition order: lock1 then lock2
//  1) complete_exit lock1 - saving recursion count
//  2) wait on lock2
//  3) when notified on lock2, unlock lock2
//  4) reenter lock1 with original recursion count
//  5) lock lock2
// NOTE: must use heavy weight monitor to handle complete_exit/reenter()
intx ObjectSynchronizer::complete_exit(Handle obj, TRAPS) {
  assert(!EnableValhalla || !obj->klass()->is_value(), "monitor op on value type");
  if (UseBiasedLocking) {
    BiasedLocking::revoke(obj, THREAD);
    assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }

  ObjectMonitor* monitor = inflate(THREAD, obj(), inflate_cause_vm_internal);

  return monitor->complete_exit(THREAD);
}

// NOTE: must use heavy weight monitor to handle complete_exit/reenter()
void ObjectSynchronizer::reenter(Handle obj, intx recursions, TRAPS) {
  assert(!EnableValhalla || !obj->klass()->is_value(), "monitor op on value type");
  if (UseBiasedLocking) {
    BiasedLocking::revoke(obj, THREAD);
    assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }

  ObjectMonitor* monitor = inflate(THREAD, obj(), inflate_cause_vm_internal);

  monitor->reenter(recursions, THREAD);
}
// -----------------------------------------------------------------------------
// JNI locks on java objects
// NOTE: must use heavy weight monitor to handle jni monitor enter
void ObjectSynchronizer::jni_enter(Handle obj, TRAPS) {
  // the current locking is from JNI instead of Java code
  CHECK_THROW_NOSYNC_IMSE(obj);
  if (UseBiasedLocking) {
    BiasedLocking::revoke(obj, THREAD);
    assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }
  THREAD->set_current_pending_monitor_is_from_java(false);
  inflate(THREAD, obj(), inflate_cause_jni_enter)->enter(THREAD);
  THREAD->set_current_pending_monitor_is_from_java(true);
}

// NOTE: must use heavy weight monitor to handle jni monitor exit
void ObjectSynchronizer::jni_exit(oop obj, Thread* THREAD) {
  CHECK_THROW_NOSYNC_IMSE(obj);
  if (UseBiasedLocking) {
    Handle h_obj(THREAD, obj);
    BiasedLocking::revoke(h_obj, THREAD);
    obj = h_obj();
  }
  assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");

  ObjectMonitor* monitor = inflate(THREAD, obj, inflate_cause_jni_exit);
  // If this thread has locked the object, exit the monitor. We
  // intentionally do not use CHECK here because we must exit the
  // monitor even if an exception is pending.
  if (monitor->check_owner(THREAD)) {
    monitor->exit(true, THREAD);
  }
}

// -----------------------------------------------------------------------------
// Internal VM locks on java objects
// standard constructor, allows locking failures
ObjectLocker::ObjectLocker(Handle obj, Thread* thread, bool do_lock) {
  _dolock = do_lock;
  _thread = thread;
  _thread->check_for_valid_safepoint_state();
  _obj = obj;

  if (_dolock) {
    ObjectSynchronizer::enter(_obj, &_lock, _thread);
  }
}

ObjectLocker::~ObjectLocker() {
  if (_dolock) {
    ObjectSynchronizer::exit(_obj(), &_lock, _thread);
  }
}


// -----------------------------------------------------------------------------
//  Wait/Notify/NotifyAll
// NOTE: must use heavy weight monitor to handle wait()
int ObjectSynchronizer::wait(Handle obj, jlong millis, TRAPS) {
  CHECK_THROW_NOSYNC_IMSE_0(obj);
  if (UseBiasedLocking) {
    BiasedLocking::revoke(obj, THREAD);
    assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }
  if (millis < 0) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "timeout value is negative");
  }
  ObjectMonitor* monitor = inflate(THREAD, obj(), inflate_cause_wait);

  DTRACE_MONITOR_WAIT_PROBE(monitor, obj(), THREAD, millis);
  monitor->wait(millis, true, THREAD);

  // This dummy call is in place to get around dtrace bug 6254741.  Once
  // that's fixed we can uncomment the following line, remove the call
  // and change this function back into a "void" func.
  // DTRACE_MONITOR_PROBE(waited, monitor, obj(), THREAD);
  return dtrace_waited_probe(monitor, obj, THREAD);
}

void ObjectSynchronizer::wait_uninterruptibly(Handle obj, jlong millis, TRAPS) {
  CHECK_THROW_NOSYNC_IMSE(obj);
  if (UseBiasedLocking) {
    BiasedLocking::revoke(obj, THREAD);
    assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }
  if (millis < 0) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), "timeout value is negative");
  }
  inflate(THREAD, obj(), inflate_cause_wait)->wait(millis, false, THREAD);
}

void ObjectSynchronizer::notify(Handle obj, TRAPS) {
  CHECK_THROW_NOSYNC_IMSE(obj);
  if (UseBiasedLocking) {
    BiasedLocking::revoke(obj, THREAD);
    assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }

  markWord mark = obj->mark();
  if (mark.has_locker() && THREAD->is_lock_owned((address)mark.locker())) {
    return;
  }
  inflate(THREAD, obj(), inflate_cause_notify)->notify(THREAD);
}

// NOTE: see comment of notify()
void ObjectSynchronizer::notifyall(Handle obj, TRAPS) {
  CHECK_THROW_NOSYNC_IMSE(obj);
  if (UseBiasedLocking) {
    BiasedLocking::revoke(obj, THREAD);
    assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }

  markWord mark = obj->mark();
  if (mark.has_locker() && THREAD->is_lock_owned((address)mark.locker())) {
    return;
  }
  inflate(THREAD, obj(), inflate_cause_notify)->notifyAll(THREAD);
}

// -----------------------------------------------------------------------------
// Hash Code handling
//
// Performance concern:
// OrderAccess::storestore() calls release() which at one time stored 0
// into the global volatile OrderAccess::dummy variable. This store was
// unnecessary for correctness. Many threads storing into a common location
// causes considerable cache migration or "sloshing" on large SMP systems.
// As such, I avoided using OrderAccess::storestore(). In some cases
// OrderAccess::fence() -- which incurs local latency on the executing
// processor -- is a better choice as it scales on SMP systems.
//
// See http://blogs.oracle.com/dave/entry/biased_locking_in_hotspot for
// a discussion of coherency costs. Note that all our current reference
// platforms provide strong ST-ST order, so the issue is moot on IA32,
// x64, and SPARC.
//
// As a general policy we use "volatile" to control compiler-based reordering
// and explicit fences (barriers) to control for architectural reordering
// performed by the CPU(s) or platform.

struct SharedGlobals {
  char         _pad_prefix[OM_CACHE_LINE_SIZE];
  // These are highly shared mostly-read variables.
  // To avoid false-sharing they need to be the sole occupants of a cache line.
  volatile int stw_random;
  volatile int stw_cycle;
  DEFINE_PAD_MINUS_SIZE(1, OM_CACHE_LINE_SIZE, sizeof(volatile int) * 2);
  // Hot RW variable -- Sequester to avoid false-sharing
  volatile int hc_sequence;
  DEFINE_PAD_MINUS_SIZE(2, OM_CACHE_LINE_SIZE, sizeof(volatile int));
};

static SharedGlobals GVars;

static markWord read_stable_mark(oop obj) {
  markWord mark = obj->mark();
  if (!mark.is_being_inflated()) {
    return mark;       // normal fast-path return
  }

  int its = 0;
  for (;;) {
    markWord mark = obj->mark();
    if (!mark.is_being_inflated()) {
      return mark;    // normal fast-path return
    }

    // The object is being inflated by some other thread.
    // The caller of read_stable_mark() must wait for inflation to complete.
    // Avoid live-lock
    // TODO: consider calling SafepointSynchronize::do_call_back() while
    // spinning to see if there's a safepoint pending.  If so, immediately
    // yielding or blocking would be appropriate.  Avoid spinning while
    // there is a safepoint pending.
    // TODO: add inflation contention performance counters.
    // TODO: restrict the aggregate number of spinners.

    ++its;
    if (its > 10000 || !os::is_MP()) {
      if (its & 1) {
        os::naked_yield();
      } else {
        // Note that the following code attenuates the livelock problem but is not
        // a complete remedy.  A more complete solution would require that the inflating
        // thread hold the associated inflation lock.  The following code simply restricts
        // the number of spinners to at most one.  We'll have N-2 threads blocked
        // on the inflationlock, 1 thread holding the inflation lock and using
        // a yield/park strategy, and 1 thread in the midst of inflation.
        // A more refined approach would be to change the encoding of INFLATING
        // to allow encapsulation of a native thread pointer.  Threads waiting for
        // inflation to complete would use CAS to push themselves onto a singly linked
        // list rooted at the markword.  Once enqueued, they'd loop, checking a per-thread flag
        // and calling park().  When inflation was complete the thread that accomplished inflation
        // would detach the list and set the markword to inflated with a single CAS and
        // then for each thread on the list, set the flag and unpark() the thread.
        // This is conceptually similar to muxAcquire-muxRelease, except that muxRelease
        // wakes at most one thread whereas we need to wake the entire list.
        int ix = (cast_from_oop<intptr_t>(obj) >> 5) & (NINFLATIONLOCKS-1);
        int YieldThenBlock = 0;
        assert(ix >= 0 && ix < NINFLATIONLOCKS, "invariant");
        assert((NINFLATIONLOCKS & (NINFLATIONLOCKS-1)) == 0, "invariant");
        Thread::muxAcquire(gInflationLocks + ix, "gInflationLock");
        while (obj->mark() == markWord::INFLATING()) {
          // Beware: NakedYield() is advisory and has almost no effect on some platforms
          // so we periodically call self->_ParkEvent->park(1).
          // We use a mixed spin/yield/block mechanism.
          if ((YieldThenBlock++) >= 16) {
            Thread::current()->_ParkEvent->park(1);
          } else {
            os::naked_yield();
          }
        }
        Thread::muxRelease(gInflationLocks + ix);
      }
    } else {
      SpinPause();       // SMP-polite spinning
    }
  }
}

// hashCode() generation :
//
// Possibilities:
// * MD5Digest of {obj,stw_random}
// * CRC32 of {obj,stw_random} or any linear-feedback shift register function.
// * A DES- or AES-style SBox[] mechanism
// * One of the Phi-based schemes, such as:
//   2654435761 = 2^32 * Phi (golden ratio)
//   HashCodeValue = ((uintptr_t(obj) >> 3) * 2654435761) ^ GVars.stw_random ;
// * A variation of Marsaglia's shift-xor RNG scheme.
// * (obj ^ stw_random) is appealing, but can result
//   in undesirable regularity in the hashCode values of adjacent objects
//   (objects allocated back-to-back, in particular).  This could potentially
//   result in hashtable collisions and reduced hashtable efficiency.
//   There are simple ways to "diffuse" the middle address bits over the
//   generated hashCode values:

static inline intptr_t get_next_hash(Thread* self, oop obj) {
  intptr_t value = 0;
  if (hashCode == 0) {
    // This form uses global Park-Miller RNG.
    // On MP system we'll have lots of RW access to a global, so the
    // mechanism induces lots of coherency traffic.
    value = os::random();
  } else if (hashCode == 1) {
    // This variation has the property of being stable (idempotent)
    // between STW operations.  This can be useful in some of the 1-0
    // synchronization schemes.
    intptr_t addr_bits = cast_from_oop<intptr_t>(obj) >> 3;
    value = addr_bits ^ (addr_bits >> 5) ^ GVars.stw_random;
  } else if (hashCode == 2) {
    value = 1;            // for sensitivity testing
  } else if (hashCode == 3) {
    value = ++GVars.hc_sequence;
  } else if (hashCode == 4) {
    value = cast_from_oop<intptr_t>(obj);
  } else {
    // Marsaglia's xor-shift scheme with thread-specific state
    // This is probably the best overall implementation -- we'll
    // likely make this the default in future releases.
    unsigned t = self->_hashStateX;
    t ^= (t << 11);
    self->_hashStateX = self->_hashStateY;
    self->_hashStateY = self->_hashStateZ;
    self->_hashStateZ = self->_hashStateW;
    unsigned v = self->_hashStateW;
    v = (v ^ (v >> 19)) ^ (t ^ (t >> 8));
    self->_hashStateW = v;
    value = v;
  }

  value &= markWord::hash_mask;
  if (value == 0) value = 0xBAD;
  assert(value != markWord::no_hash, "invariant");
  return value;
}

intptr_t ObjectSynchronizer::FastHashCode(Thread* self, oop obj) {
  if (EnableValhalla && obj->klass()->is_value()) {
    // Expected tooling to override hashCode for value type, just don't crash
    if (log_is_enabled(Debug, monitorinflation)) {
      ResourceMark rm;
      log_debug(monitorinflation)("FastHashCode for value type: %s", obj->klass()->external_name());
    }
    return obj->klass()->java_mirror()->identity_hash();
  }
  if (UseBiasedLocking) {
    // NOTE: many places throughout the JVM do not expect a safepoint
    // to be taken here, in particular most operations on perm gen
    // objects. However, we only ever bias Java instances and all of
    // the call sites of identity_hash that might revoke biases have
    // been checked to make sure they can handle a safepoint. The
    // added check of the bias pattern is to avoid useless calls to
    // thread-local storage.
    if (obj->mark().has_bias_pattern()) {
      // Handle for oop obj in case of STW safepoint
      Handle hobj(self, obj);
      // Relaxing assertion for bug 6320749.
      assert(Universe::verify_in_progress() ||
             !SafepointSynchronize::is_at_safepoint(),
             "biases should not be seen by VM thread here");
      BiasedLocking::revoke(hobj, JavaThread::current());
      obj = hobj();
      assert(!obj->mark().has_bias_pattern(), "biases should be revoked by now");
    }
  }

  // hashCode() is a heap mutator ...
  // Relaxing assertion for bug 6320749.
  assert(Universe::verify_in_progress() || DumpSharedSpaces ||
         !SafepointSynchronize::is_at_safepoint(), "invariant");
  assert(Universe::verify_in_progress() || DumpSharedSpaces ||
         self->is_Java_thread() , "invariant");
  assert(Universe::verify_in_progress() || DumpSharedSpaces ||
         ((JavaThread *)self)->thread_state() != _thread_blocked, "invariant");

  ObjectMonitor* monitor = NULL;
  markWord temp, test;
  intptr_t hash;
  markWord mark = read_stable_mark(obj);

  // object should remain ineligible for biased locking
  assert(!mark.has_bias_pattern(), "invariant");

  if (mark.is_neutral()) {            // if this is a normal header
    hash = mark.hash();
    if (hash != 0) {                  // if it has a hash, just return it
      return hash;
    }
    hash = get_next_hash(self, obj);  // get a new hash
    temp = mark.copy_set_hash(hash);  // merge the hash into header
                                      // try to install the hash
    test = obj->cas_set_mark(temp, mark);
    if (test == mark) {               // if the hash was installed, return it
      return hash;
    }
    // Failed to install the hash. It could be that another thread
    // installed the hash just before our attempt or inflation has
    // occurred or... so we fall thru to inflate the monitor for
    // stability and then install the hash.
  } else if (mark.has_monitor()) {
    monitor = mark.monitor();
    temp = monitor->header();
    assert(temp.is_neutral(), "invariant: header=" INTPTR_FORMAT, temp.value());
    hash = temp.hash();
    if (hash != 0) {                  // if it has a hash, just return it
      return hash;
    }
    // Fall thru so we only have one place that installs the hash in
    // the ObjectMonitor.
  } else if (self->is_lock_owned((address)mark.locker())) {
    // This is a stack lock owned by the calling thread so fetch the
    // displaced markWord from the BasicLock on the stack.
    temp = mark.displaced_mark_helper();
    assert(temp.is_neutral(), "invariant: header=" INTPTR_FORMAT, temp.value());
    hash = temp.hash();
    if (hash != 0) {                  // if it has a hash, just return it
      return hash;
    }
    // WARNING:
    // The displaced header in the BasicLock on a thread's stack
    // is strictly immutable. It CANNOT be changed in ANY cases.
    // So we have to inflate the stack lock into an ObjectMonitor
    // even if the current thread owns the lock. The BasicLock on
    // a thread's stack can be asynchronously read by other threads
    // during an inflate() call so any change to that stack memory
    // may not propagate to other threads correctly.
  }

  // Inflate the monitor to set the hash.
  monitor = inflate(self, obj, inflate_cause_hash_code);
  // Load ObjectMonitor's header/dmw field and see if it has a hash.
  mark = monitor->header();
  assert(mark.is_neutral(), "invariant: header=" INTPTR_FORMAT, mark.value());
  hash = mark.hash();
  if (hash == 0) {                    // if it does not have a hash
    hash = get_next_hash(self, obj);  // get a new hash
    temp = mark.copy_set_hash(hash);  // merge the hash into header
    assert(temp.is_neutral(), "invariant: header=" INTPTR_FORMAT, temp.value());
    uintptr_t v = Atomic::cmpxchg((volatile uintptr_t*)monitor->header_addr(), mark.value(), temp.value());
    test = markWord(v);
    if (test != mark) {
      // The attempt to update the ObjectMonitor's header/dmw field
      // did not work. This can happen if another thread managed to
      // merge in the hash just before our cmpxchg().
      // If we add any new usages of the header/dmw field, this code
      // will need to be updated.
      hash = test.hash();
      assert(test.is_neutral(), "invariant: header=" INTPTR_FORMAT, test.value());
      assert(hash != 0, "should only have lost the race to a thread that set a non-zero hash");
    }
  }
  // We finally get the hash.
  return hash;
}


bool ObjectSynchronizer::current_thread_holds_lock(JavaThread* thread,
                                                   Handle h_obj) {
  if (EnableValhalla && h_obj->mark().is_always_locked()) {
    return false;
  }
  if (UseBiasedLocking) {
    BiasedLocking::revoke(h_obj, thread);
    assert(!h_obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }

  assert(thread == JavaThread::current(), "Can only be called on current thread");
  oop obj = h_obj();

  markWord mark = read_stable_mark(obj);

  // Uncontended case, header points to stack
  if (mark.has_locker()) {
    return thread->is_lock_owned((address)mark.locker());
  }
  // Contended case, header points to ObjectMonitor (tagged pointer)
  if (mark.has_monitor()) {
    ObjectMonitor* monitor = mark.monitor();
    return monitor->is_entered(thread) != 0;
  }
  // Unlocked case, header in place
  assert(mark.is_neutral(), "sanity check");
  return false;
}

// Be aware of this method could revoke bias of the lock object.
// This method queries the ownership of the lock handle specified by 'h_obj'.
// If the current thread owns the lock, it returns owner_self. If no
// thread owns the lock, it returns owner_none. Otherwise, it will return
// owner_other.
ObjectSynchronizer::LockOwnership ObjectSynchronizer::query_lock_ownership
(JavaThread *self, Handle h_obj) {
  // The caller must beware this method can revoke bias, and
  // revocation can result in a safepoint.
  assert(!SafepointSynchronize::is_at_safepoint(), "invariant");
  assert(self->thread_state() != _thread_blocked, "invariant");

  // Possible mark states: neutral, biased, stack-locked, inflated

  if (UseBiasedLocking && h_obj()->mark().has_bias_pattern()) {
    // CASE: biased
    BiasedLocking::revoke(h_obj, self);
    assert(!h_obj->mark().has_bias_pattern(),
           "biases should be revoked by now");
  }

  assert(self == JavaThread::current(), "Can only be called on current thread");
  oop obj = h_obj();
  markWord mark = read_stable_mark(obj);

  // CASE: stack-locked.  Mark points to a BasicLock on the owner's stack.
  if (mark.has_locker()) {
    return self->is_lock_owned((address)mark.locker()) ?
      owner_self : owner_other;
  }

  // CASE: inflated. Mark (tagged pointer) points to an ObjectMonitor.
  // The Object:ObjectMonitor relationship is stable as long as we're
  // not at a safepoint.
  if (mark.has_monitor()) {
    void* owner = mark.monitor()->_owner;
    if (owner == NULL) return owner_none;
    return (owner == self ||
            self->is_lock_owned((address)owner)) ? owner_self : owner_other;
  }

  // CASE: neutral
  assert(mark.is_neutral(), "sanity check");
  return owner_none;           // it's unlocked
}

// FIXME: jvmti should call this
JavaThread* ObjectSynchronizer::get_lock_owner(ThreadsList * t_list, Handle h_obj) {
  if (UseBiasedLocking) {
    if (SafepointSynchronize::is_at_safepoint()) {
      BiasedLocking::revoke_at_safepoint(h_obj);
    } else {
      BiasedLocking::revoke(h_obj, JavaThread::current());
    }
    assert(!h_obj->mark().has_bias_pattern(), "biases should be revoked by now");
  }

  oop obj = h_obj();
  address owner = NULL;

  markWord mark = read_stable_mark(obj);

  // Uncontended case, header points to stack
  if (mark.has_locker()) {
    owner = (address) mark.locker();
  }

  // Contended case, header points to ObjectMonitor (tagged pointer)
  else if (mark.has_monitor()) {
    ObjectMonitor* monitor = mark.monitor();
    assert(monitor != NULL, "monitor should be non-null");
    owner = (address) monitor->owner();
  }

  if (owner != NULL) {
    // owning_thread_from_monitor_owner() may also return NULL here
    return Threads::owning_thread_from_monitor_owner(t_list, owner);
  }

  // Unlocked case, header in place
  // Cannot have assertion since this object may have been
  // locked by another thread when reaching here.
  // assert(mark.is_neutral(), "sanity check");

  return NULL;
}

// Visitors ...

void ObjectSynchronizer::monitors_iterate(MonitorClosure* closure) {
  PaddedObjectMonitor* block = Atomic::load(&g_block_list);
  while (block != NULL) {
    assert(block->object() == CHAINMARKER, "must be a block header");
    for (int i = _BLOCKSIZE - 1; i > 0; i--) {
      ObjectMonitor* mid = (ObjectMonitor *)(block + i);
      oop object = (oop)mid->object();
      if (object != NULL) {
        // Only process with closure if the object is set.
        closure->do_monitor(mid);
      }
    }
    // unmarked_next() is not needed with g_block_list (no locking
    // used with block linkage _next_om fields).
    block = (PaddedObjectMonitor*)block->next_om();
  }
}

static bool monitors_used_above_threshold() {
  int population = Atomic::load(&om_list_globals._population);
  if (population == 0) {
    return false;
  }
  if (MonitorUsedDeflationThreshold > 0) {
    int monitors_used = population - Atomic::load(&om_list_globals._free_count);
    int monitor_usage = (monitors_used * 100LL) / population;
    return monitor_usage > MonitorUsedDeflationThreshold;
  }
  return false;
}

bool ObjectSynchronizer::is_cleanup_needed() {
  return monitors_used_above_threshold();
}

void ObjectSynchronizer::oops_do(OopClosure* f) {
  // We only scan the global used list here (for moribund threads), and
  // the thread-local monitors in Thread::oops_do().
  global_used_oops_do(f);
}

void ObjectSynchronizer::global_used_oops_do(OopClosure* f) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");
  list_oops_do(Atomic::load(&om_list_globals._in_use_list), f);
}

void ObjectSynchronizer::thread_local_used_oops_do(Thread* thread, OopClosure* f) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");
  list_oops_do(thread->om_in_use_list, f);
}

void ObjectSynchronizer::list_oops_do(ObjectMonitor* list, OopClosure* f) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");
  // The oops_do() phase does not overlap with monitor deflation
  // so no need to lock ObjectMonitors for the list traversal.
  for (ObjectMonitor* mid = list; mid != NULL; mid = unmarked_next(mid)) {
    if (mid->object() != NULL) {
      f->do_oop((oop*)mid->object_addr());
    }
  }
}


// -----------------------------------------------------------------------------
// ObjectMonitor Lifecycle
// -----------------------
// Inflation unlinks monitors from om_list_globals._free_list or a per-thread
// free list and associates them with objects. Deflation -- which occurs at
// STW-time -- disassociates idle monitors from objects.
// Such scavenged monitors are returned to the om_list_globals._free_list.
//
// ObjectMonitors reside in type-stable memory (TSM) and are immortal.
//
// Lifecycle:
// --   unassigned and on the om_list_globals._free_list
// --   unassigned and on a per-thread free list
// --   assigned to an object.  The object is inflated and the mark refers
//      to the ObjectMonitor.

ObjectMonitor* ObjectSynchronizer::om_alloc(Thread* self) {
  // A large MAXPRIVATE value reduces both list lock contention
  // and list coherency traffic, but also tends to increase the
  // number of ObjectMonitors in circulation as well as the STW
  // scavenge costs.  As usual, we lean toward time in space-time
  // tradeoffs.
  const int MAXPRIVATE = 1024;
  NoSafepointVerifier nsv;

  for (;;) {
    ObjectMonitor* m;

    // 1: try to allocate from the thread's local om_free_list.
    // Threads will attempt to allocate first from their local list, then
    // from the global list, and only after those attempts fail will the
    // thread attempt to instantiate new monitors. Thread-local free lists
    // improve allocation latency, as well as reducing coherency traffic
    // on the shared global list.
    m = take_from_start_of_om_free_list(self);
    if (m != NULL) {
      guarantee(m->object() == NULL, "invariant");
      prepend_to_om_in_use_list(self, m);
      return m;
    }

    // 2: try to allocate from the global om_list_globals._free_list
    // If we're using thread-local free lists then try
    // to reprovision the caller's free list.
    if (Atomic::load(&om_list_globals._free_list) != NULL) {
      // Reprovision the thread's om_free_list.
      // Use bulk transfers to reduce the allocation rate and heat
      // on various locks.
      for (int i = self->om_free_provision; --i >= 0;) {
        ObjectMonitor* take = take_from_start_of_global_free_list();
        if (take == NULL) {
          break;  // No more are available.
        }
        guarantee(take->object() == NULL, "invariant");
        take->Recycle();
        om_release(self, take, false);
      }
      self->om_free_provision += 1 + (self->om_free_provision / 2);
      if (self->om_free_provision > MAXPRIVATE) self->om_free_provision = MAXPRIVATE;
      continue;
    }

    // 3: allocate a block of new ObjectMonitors
    // Both the local and global free lists are empty -- resort to malloc().
    // In the current implementation ObjectMonitors are TSM - immortal.
    // Ideally, we'd write "new ObjectMonitor[_BLOCKSIZE], but we want
    // each ObjectMonitor to start at the beginning of a cache line,
    // so we use align_up().
    // A better solution would be to use C++ placement-new.
    // BEWARE: As it stands currently, we don't run the ctors!
    assert(_BLOCKSIZE > 1, "invariant");
    size_t neededsize = sizeof(PaddedObjectMonitor) * _BLOCKSIZE;
    PaddedObjectMonitor* temp;
    size_t aligned_size = neededsize + (OM_CACHE_LINE_SIZE - 1);
    void* real_malloc_addr = NEW_C_HEAP_ARRAY(char, aligned_size, mtInternal);
    temp = (PaddedObjectMonitor*)align_up(real_malloc_addr, OM_CACHE_LINE_SIZE);
    (void)memset((void *) temp, 0, neededsize);

    // Format the block.
    // initialize the linked list, each monitor points to its next
    // forming the single linked free list, the very first monitor
    // will points to next block, which forms the block list.
    // The trick of using the 1st element in the block as g_block_list
    // linkage should be reconsidered.  A better implementation would
    // look like: class Block { Block * next; int N; ObjectMonitor Body [N] ; }

    for (int i = 1; i < _BLOCKSIZE; i++) {
      temp[i].set_next_om((ObjectMonitor*)&temp[i + 1]);
    }

    // terminate the last monitor as the end of list
    temp[_BLOCKSIZE - 1].set_next_om((ObjectMonitor*)NULL);

    // Element [0] is reserved for global list linkage
    temp[0].set_object(CHAINMARKER);

    // Consider carving out this thread's current request from the
    // block in hand.  This avoids some lock traffic and redundant
    // list activity.

    prepend_block_to_lists(temp);
  }
}

// Place "m" on the caller's private per-thread om_free_list.
// In practice there's no need to clamp or limit the number of
// monitors on a thread's om_free_list as the only non-allocation time
// we'll call om_release() is to return a monitor to the free list after
// a CAS attempt failed. This doesn't allow unbounded #s of monitors to
// accumulate on a thread's free list.
//
// Key constraint: all ObjectMonitors on a thread's free list and the global
// free list must have their object field set to null. This prevents the
// scavenger -- deflate_monitor_list() -- from reclaiming them while we
// are trying to release them.

void ObjectSynchronizer::om_release(Thread* self, ObjectMonitor* m,
                                    bool from_per_thread_alloc) {
  guarantee(m->header().value() == 0, "invariant");
  guarantee(m->object() == NULL, "invariant");
  NoSafepointVerifier nsv;

  if ((m->is_busy() | m->_recursions) != 0) {
    stringStream ss;
    fatal("freeing in-use monitor: %s, recursions=" INTX_FORMAT,
          m->is_busy_to_string(&ss), m->_recursions);
  }
  // _next_om is used for both per-thread in-use and free lists so
  // we have to remove 'm' from the in-use list first (as needed).
  if (from_per_thread_alloc) {
    // Need to remove 'm' from om_in_use_list.
    ObjectMonitor* mid = NULL;
    ObjectMonitor* next = NULL;

    // This list walk can only race with another list walker since
    // deflation can only happen at a safepoint so we don't have to
    // worry about an ObjectMonitor being removed from this list
    // while we are walking it.

    // Lock the list head to avoid racing with another list walker.
    if ((mid = get_list_head_locked(&self->om_in_use_list)) == NULL) {
      fatal("thread=" INTPTR_FORMAT " in-use list must not be empty.", p2i(self));
    }
    next = unmarked_next(mid);
    if (m == mid) {
      // First special case:
      // 'm' matches mid, is the list head and is locked. Switch the list
      // head to next which unlocks the list head, but leaves the extracted
      // mid locked:
      Atomic::store(&self->om_in_use_list, next);
    } else if (m == next) {
      // Second special case:
      // 'm' matches next after the list head and we already have the list
      // head locked so set mid to what we are extracting:
      mid = next;
      // Lock mid to prevent races with a list walker:
      om_lock(mid);
      // Update next to what follows mid (if anything):
      next = unmarked_next(mid);
      // Switch next after the list head to new next which unlocks the
      // list head, but leaves the extracted mid locked:
      self->om_in_use_list->set_next_om(next);
    } else {
      // We have to search the list to find 'm'.
      om_unlock(mid);  // unlock the list head
      guarantee(next != NULL, "thread=" INTPTR_FORMAT ": om_in_use_list=" INTPTR_FORMAT
                " is too short.", p2i(self), p2i(self->om_in_use_list));
      // Our starting anchor is next after the list head which is the
      // last ObjectMonitor we checked:
      ObjectMonitor* anchor = next;
      while ((mid = unmarked_next(anchor)) != NULL) {
        if (m == mid) {
          // We found 'm' on the per-thread in-use list so extract it.
          om_lock(anchor);  // Lock the anchor so we can safely modify it.
          // Update next to what follows mid (if anything):
          next = unmarked_next(mid);
          // Switch next after the anchor to new next which unlocks the
          // anchor, but leaves the extracted mid locked:
          anchor->set_next_om(next);
          break;
        } else {
          anchor = mid;
        }
      }
    }

    if (mid == NULL) {
      // Reached end of the list and didn't find 'm' so:
      fatal("thread=" INTPTR_FORMAT " must find m=" INTPTR_FORMAT "on om_in_use_list="
            INTPTR_FORMAT, p2i(self), p2i(m), p2i(self->om_in_use_list));
    }

    // At this point mid is disconnected from the in-use list so
    // its lock no longer has any effects on the in-use list.
    Atomic::dec(&self->om_in_use_count);
    // Unlock mid, but leave the next value for any lagging list
    // walkers. It will get cleaned up when mid is prepended to
    // the thread's free list:
    om_unlock(mid);
  }

  prepend_to_om_free_list(self, m);
}

// Return ObjectMonitors on a moribund thread's free and in-use
// lists to the appropriate global lists. The ObjectMonitors on the
// per-thread in-use list may still be in use by other threads.
//
// We currently call om_flush() from Threads::remove() before the
// thread has been excised from the thread list and is no longer a
// mutator. This means that om_flush() cannot run concurrently with
// a safepoint and interleave with deflate_idle_monitors(). In
// particular, this ensures that the thread's in-use monitors are
// scanned by a GC safepoint, either via Thread::oops_do() (before
// om_flush() is called) or via ObjectSynchronizer::oops_do() (after
// om_flush() is called).

void ObjectSynchronizer::om_flush(Thread* self) {
  // Process the per-thread in-use list first to be consistent.
  int in_use_count = 0;
  ObjectMonitor* in_use_list = NULL;
  ObjectMonitor* in_use_tail = NULL;
  NoSafepointVerifier nsv;

  // This function can race with a list walker thread so we lock the
  // list head to prevent confusion.
  if ((in_use_list = get_list_head_locked(&self->om_in_use_list)) != NULL) {
    // At this point, we have locked the in-use list head so a racing
    // thread cannot come in after us. However, a racing thread could
    // be ahead of us; we'll detect that and delay to let it finish.
    //
    // The thread is going away, however the ObjectMonitors on the
    // om_in_use_list may still be in-use by other threads. Link
    // them to in_use_tail, which will be linked into the global
    // in-use list (om_list_globals._in_use_list) below.
    //
    // Account for the in-use list head before the loop since it is
    // already locked (by this thread):
    in_use_tail = in_use_list;
    in_use_count++;
    for (ObjectMonitor* cur_om = unmarked_next(in_use_list); cur_om != NULL; cur_om = unmarked_next(cur_om)) {
      if (is_locked(cur_om)) {
        // cur_om is locked so there must be a racing walker thread ahead
        // of us so we'll give it a chance to finish.
        while (is_locked(cur_om)) {
          os::naked_short_sleep(1);
        }
      }
      in_use_tail = cur_om;
      in_use_count++;
    }
    guarantee(in_use_tail != NULL, "invariant");
    int l_om_in_use_count = Atomic::load(&self->om_in_use_count);
    assert(l_om_in_use_count == in_use_count, "in-use counts don't match: "
          "l_om_in_use_count=%d, in_use_count=%d", l_om_in_use_count, in_use_count);
    Atomic::store(&self->om_in_use_count, 0);
    // Clear the in-use list head (which also unlocks it):
    Atomic::store(&self->om_in_use_list, (ObjectMonitor*)NULL);
    om_unlock(in_use_list);
  }

  int free_count = 0;
  ObjectMonitor* free_list = NULL;
  ObjectMonitor* free_tail = NULL;
  // This function can race with a list walker thread so we lock the
  // list head to prevent confusion.
  if ((free_list = get_list_head_locked(&self->om_free_list)) != NULL) {
    // At this point, we have locked the free list head so a racing
    // thread cannot come in after us. However, a racing thread could
    // be ahead of us; we'll detect that and delay to let it finish.
    //
    // The thread is going away. Set 'free_tail' to the last per-thread free
    // monitor which will be linked to om_list_globals._free_list below.
    //
    // Account for the free list head before the loop since it is
    // already locked (by this thread):
    free_tail = free_list;
    free_count++;
    for (ObjectMonitor* s = unmarked_next(free_list); s != NULL; s = unmarked_next(s)) {
      if (is_locked(s)) {
        // s is locked so there must be a racing walker thread ahead
        // of us so we'll give it a chance to finish.
        while (is_locked(s)) {
          os::naked_short_sleep(1);
        }
      }
      free_tail = s;
      free_count++;
      guarantee(s->object() == NULL, "invariant");
      if (s->is_busy()) {
        stringStream ss;
        fatal("must be !is_busy: %s", s->is_busy_to_string(&ss));
      }
    }
    guarantee(free_tail != NULL, "invariant");
    int l_om_free_count = Atomic::load(&self->om_free_count);
    assert(l_om_free_count == free_count, "free counts don't match: "
           "l_om_free_count=%d, free_count=%d", l_om_free_count, free_count);
    Atomic::store(&self->om_free_count, 0);
    Atomic::store(&self->om_free_list, (ObjectMonitor*)NULL);
    om_unlock(free_list);
  }

  if (free_tail != NULL) {
    prepend_list_to_global_free_list(free_list, free_tail, free_count);
  }

  if (in_use_tail != NULL) {
    prepend_list_to_global_in_use_list(in_use_list, in_use_tail, in_use_count);
  }

  LogStreamHandle(Debug, monitorinflation) lsh_debug;
  LogStreamHandle(Info, monitorinflation) lsh_info;
  LogStream* ls = NULL;
  if (log_is_enabled(Debug, monitorinflation)) {
    ls = &lsh_debug;
  } else if ((free_count != 0 || in_use_count != 0) &&
             log_is_enabled(Info, monitorinflation)) {
    ls = &lsh_info;
  }
  if (ls != NULL) {
    ls->print_cr("om_flush: jt=" INTPTR_FORMAT ", free_count=%d"
                 ", in_use_count=%d" ", om_free_provision=%d",
                 p2i(self), free_count, in_use_count, self->om_free_provision);
  }
}

static void post_monitor_inflate_event(EventJavaMonitorInflate* event,
                                       const oop obj,
                                       ObjectSynchronizer::InflateCause cause) {
  assert(event != NULL, "invariant");
  assert(event->should_commit(), "invariant");
  event->set_monitorClass(obj->klass());
  event->set_address((uintptr_t)(void*)obj);
  event->set_cause((u1)cause);
  event->commit();
}

// Fast path code shared by multiple functions
void ObjectSynchronizer::inflate_helper(oop obj) {
  markWord mark = obj->mark();
  if (mark.has_monitor()) {
    assert(ObjectSynchronizer::verify_objmon_isinpool(mark.monitor()), "monitor is invalid");
    assert(mark.monitor()->header().is_neutral(), "monitor must record a good object header");
    return;
  }
  inflate(Thread::current(), obj, inflate_cause_vm_internal);
}

ObjectMonitor* ObjectSynchronizer::inflate(Thread* self,
                                           oop object, const InflateCause cause) {
  // Inflate mutates the heap ...
  // Relaxing assertion for bug 6320749.
  assert(Universe::verify_in_progress() ||
         !SafepointSynchronize::is_at_safepoint(), "invariant");

  if (EnableValhalla) {
    guarantee(!object->klass()->is_value(), "Attempt to inflate value type");
  }

  EventJavaMonitorInflate event;

  for (;;) {
    const markWord mark = object->mark();
    assert(!mark.has_bias_pattern(), "invariant");

    // The mark can be in one of the following states:
    // *  Inflated     - just return
    // *  Stack-locked - coerce it to inflated
    // *  INFLATING    - busy wait for conversion to complete
    // *  Neutral      - aggressively inflate the object.
    // *  BIASED       - Illegal.  We should never see this

    // CASE: inflated
    if (mark.has_monitor()) {
      ObjectMonitor* inf = mark.monitor();
      markWord dmw = inf->header();
      assert(dmw.is_neutral(), "invariant: header=" INTPTR_FORMAT, dmw.value());
      assert(inf->object() == object, "invariant");
      assert(ObjectSynchronizer::verify_objmon_isinpool(inf), "monitor is invalid");
      return inf;
    }

    // CASE: inflation in progress - inflating over a stack-lock.
    // Some other thread is converting from stack-locked to inflated.
    // Only that thread can complete inflation -- other threads must wait.
    // The INFLATING value is transient.
    // Currently, we spin/yield/park and poll the markword, waiting for inflation to finish.
    // We could always eliminate polling by parking the thread on some auxiliary list.
    if (mark == markWord::INFLATING()) {
      read_stable_mark(object);
      continue;
    }

    // CASE: stack-locked
    // Could be stack-locked either by this thread or by some other thread.
    //
    // Note that we allocate the objectmonitor speculatively, _before_ attempting
    // to install INFLATING into the mark word.  We originally installed INFLATING,
    // allocated the objectmonitor, and then finally STed the address of the
    // objectmonitor into the mark.  This was correct, but artificially lengthened
    // the interval in which INFLATED appeared in the mark, thus increasing
    // the odds of inflation contention.
    //
    // We now use per-thread private objectmonitor free lists.
    // These list are reprovisioned from the global free list outside the
    // critical INFLATING...ST interval.  A thread can transfer
    // multiple objectmonitors en-mass from the global free list to its local free list.
    // This reduces coherency traffic and lock contention on the global free list.
    // Using such local free lists, it doesn't matter if the om_alloc() call appears
    // before or after the CAS(INFLATING) operation.
    // See the comments in om_alloc().

    LogStreamHandle(Trace, monitorinflation) lsh;

    if (mark.has_locker()) {
      ObjectMonitor* m = om_alloc(self);
      // Optimistically prepare the objectmonitor - anticipate successful CAS
      // We do this before the CAS in order to minimize the length of time
      // in which INFLATING appears in the mark.
      m->Recycle();
      m->_Responsible  = NULL;
      m->_SpinDuration = ObjectMonitor::Knob_SpinLimit;   // Consider: maintain by type/class

      markWord cmp = object->cas_set_mark(markWord::INFLATING(), mark);
      if (cmp != mark) {
        om_release(self, m, true);
        continue;       // Interference -- just retry
      }

      // We've successfully installed INFLATING (0) into the mark-word.
      // This is the only case where 0 will appear in a mark-word.
      // Only the singular thread that successfully swings the mark-word
      // to 0 can perform (or more precisely, complete) inflation.
      //
      // Why do we CAS a 0 into the mark-word instead of just CASing the
      // mark-word from the stack-locked value directly to the new inflated state?
      // Consider what happens when a thread unlocks a stack-locked object.
      // It attempts to use CAS to swing the displaced header value from the
      // on-stack BasicLock back into the object header.  Recall also that the
      // header value (hash code, etc) can reside in (a) the object header, or
      // (b) a displaced header associated with the stack-lock, or (c) a displaced
      // header in an ObjectMonitor.  The inflate() routine must copy the header
      // value from the BasicLock on the owner's stack to the ObjectMonitor, all
      // the while preserving the hashCode stability invariants.  If the owner
      // decides to release the lock while the value is 0, the unlock will fail
      // and control will eventually pass from slow_exit() to inflate.  The owner
      // will then spin, waiting for the 0 value to disappear.   Put another way,
      // the 0 causes the owner to stall if the owner happens to try to
      // drop the lock (restoring the header from the BasicLock to the object)
      // while inflation is in-progress.  This protocol avoids races that might
      // would otherwise permit hashCode values to change or "flicker" for an object.
      // Critically, while object->mark is 0 mark.displaced_mark_helper() is stable.
      // 0 serves as a "BUSY" inflate-in-progress indicator.


      // fetch the displaced mark from the owner's stack.
      // The owner can't die or unwind past the lock while our INFLATING
      // object is in the mark.  Furthermore the owner can't complete
      // an unlock on the object, either.
      markWord dmw = mark.displaced_mark_helper();
      // Catch if the object's header is not neutral (not locked and
      // not marked is what we care about here).
      assert(dmw.is_neutral(), "invariant: header=" INTPTR_FORMAT, dmw.value());

      // Setup monitor fields to proper values -- prepare the monitor
      m->set_header(dmw);

      // Optimization: if the mark.locker stack address is associated
      // with this thread we could simply set m->_owner = self.
      // Note that a thread can inflate an object
      // that it has stack-locked -- as might happen in wait() -- directly
      // with CAS.  That is, we can avoid the xchg-NULL .... ST idiom.
      m->set_owner_from(NULL, mark.locker());
      m->set_object(object);
      // TODO-FIXME: assert BasicLock->dhw != 0.

      // Must preserve store ordering. The monitor state must
      // be stable at the time of publishing the monitor address.
      guarantee(object->mark() == markWord::INFLATING(), "invariant");
      object->release_set_mark(markWord::encode(m));

      // Hopefully the performance counters are allocated on distinct cache lines
      // to avoid false sharing on MP systems ...
      OM_PERFDATA_OP(Inflations, inc());
      if (log_is_enabled(Trace, monitorinflation)) {
        ResourceMark rm(self);
        lsh.print_cr("inflate(has_locker): object=" INTPTR_FORMAT ", mark="
                     INTPTR_FORMAT ", type='%s'", p2i(object),
                     object->mark().value(), object->klass()->external_name());
      }
      if (event.should_commit()) {
        post_monitor_inflate_event(&event, object, cause);
      }
      return m;
    }

    // CASE: neutral
    // TODO-FIXME: for entry we currently inflate and then try to CAS _owner.
    // If we know we're inflating for entry it's better to inflate by swinging a
    // pre-locked ObjectMonitor pointer into the object header.   A successful
    // CAS inflates the object *and* confers ownership to the inflating thread.
    // In the current implementation we use a 2-step mechanism where we CAS()
    // to inflate and then CAS() again to try to swing _owner from NULL to self.
    // An inflateTry() method that we could call from enter() would be useful.

    // Catch if the object's header is not neutral (not locked and
    // not marked is what we care about here).
    assert(mark.is_neutral(), "invariant: header=" INTPTR_FORMAT, mark.value());
    ObjectMonitor* m = om_alloc(self);
    // prepare m for installation - set monitor to initial state
    m->Recycle();
    m->set_header(mark);
    m->set_object(object);
    m->_Responsible  = NULL;
    m->_SpinDuration = ObjectMonitor::Knob_SpinLimit;       // consider: keep metastats by type/class

    if (object->cas_set_mark(markWord::encode(m), mark) != mark) {
      m->set_header(markWord::zero());
      m->set_object(NULL);
      m->Recycle();
      om_release(self, m, true);
      m = NULL;
      continue;
      // interference - the markword changed - just retry.
      // The state-transitions are one-way, so there's no chance of
      // live-lock -- "Inflated" is an absorbing state.
    }

    // Hopefully the performance counters are allocated on distinct
    // cache lines to avoid false sharing on MP systems ...
    OM_PERFDATA_OP(Inflations, inc());
    if (log_is_enabled(Trace, monitorinflation)) {
      ResourceMark rm(self);
      lsh.print_cr("inflate(neutral): object=" INTPTR_FORMAT ", mark="
                   INTPTR_FORMAT ", type='%s'", p2i(object),
                   object->mark().value(), object->klass()->external_name());
    }
    if (event.should_commit()) {
      post_monitor_inflate_event(&event, object, cause);
    }
    return m;
  }
}


// We maintain a list of in-use monitors for each thread.
//
// deflate_thread_local_monitors() scans a single thread's in-use list, while
// deflate_idle_monitors() scans only a global list of in-use monitors which
// is populated only as a thread dies (see om_flush()).
//
// These operations are called at all safepoints, immediately after mutators
// are stopped, but before any objects have moved. Collectively they traverse
// the population of in-use monitors, deflating where possible. The scavenged
// monitors are returned to the global monitor free list.
//
// Beware that we scavenge at *every* stop-the-world point. Having a large
// number of monitors in-use could negatively impact performance. We also want
// to minimize the total # of monitors in circulation, as they incur a small
// footprint penalty.
//
// Perversely, the heap size -- and thus the STW safepoint rate --
// typically drives the scavenge rate.  Large heaps can mean infrequent GC,
// which in turn can mean large(r) numbers of ObjectMonitors in circulation.
// This is an unfortunate aspect of this design.

// Deflate a single monitor if not in-use
// Return true if deflated, false if in-use
bool ObjectSynchronizer::deflate_monitor(ObjectMonitor* mid, oop obj,
                                         ObjectMonitor** free_head_p,
                                         ObjectMonitor** free_tail_p) {
  bool deflated;
  // Normal case ... The monitor is associated with obj.
  const markWord mark = obj->mark();
  guarantee(mark == markWord::encode(mid), "should match: mark="
            INTPTR_FORMAT ", encoded mid=" INTPTR_FORMAT, mark.value(),
            markWord::encode(mid).value());
  // Make sure that mark.monitor() and markWord::encode() agree:
  guarantee(mark.monitor() == mid, "should match: monitor()=" INTPTR_FORMAT
            ", mid=" INTPTR_FORMAT, p2i(mark.monitor()), p2i(mid));
  const markWord dmw = mid->header();
  guarantee(dmw.is_neutral(), "invariant: header=" INTPTR_FORMAT, dmw.value());

  if (mid->is_busy()) {
    // Easy checks are first - the ObjectMonitor is busy so no deflation.
    deflated = false;
  } else {
    // Deflate the monitor if it is no longer being used
    // It's idle - scavenge and return to the global free list
    // plain old deflation ...
    if (log_is_enabled(Trace, monitorinflation)) {
      ResourceMark rm;
      log_trace(monitorinflation)("deflate_monitor: "
                                  "object=" INTPTR_FORMAT ", mark="
                                  INTPTR_FORMAT ", type='%s'", p2i(obj),
                                  mark.value(), obj->klass()->external_name());
    }

    // Restore the header back to obj
    obj->release_set_mark(dmw);
    mid->clear();

    assert(mid->object() == NULL, "invariant: object=" INTPTR_FORMAT,
           p2i(mid->object()));

    // Move the deflated ObjectMonitor to the working free list
    // defined by free_head_p and free_tail_p.
    if (*free_head_p == NULL) *free_head_p = mid;
    if (*free_tail_p != NULL) {
      // We append to the list so the caller can use mid->_next_om
      // to fix the linkages in its context.
      ObjectMonitor* prevtail = *free_tail_p;
      // Should have been cleaned up by the caller:
      // Note: Should not have to lock prevtail here since we're at a
      // safepoint and ObjectMonitors on the local free list should
      // not be accessed in parallel.
#ifdef ASSERT
      ObjectMonitor* l_next_om = prevtail->next_om();
#endif
      assert(l_next_om == NULL, "must be NULL: _next_om=" INTPTR_FORMAT, p2i(l_next_om));
      prevtail->set_next_om(mid);
    }
    *free_tail_p = mid;
    // At this point, mid->_next_om still refers to its current
    // value and another ObjectMonitor's _next_om field still
    // refers to this ObjectMonitor. Those linkages have to be
    // cleaned up by the caller who has the complete context.
    deflated = true;
  }
  return deflated;
}

// Walk a given monitor list, and deflate idle monitors.
// The given list could be a per-thread list or a global list.
//
// In the case of parallel processing of thread local monitor lists,
// work is done by Threads::parallel_threads_do() which ensures that
// each Java thread is processed by exactly one worker thread, and
// thus avoid conflicts that would arise when worker threads would
// process the same monitor lists concurrently.
//
// See also ParallelSPCleanupTask and
// SafepointSynchronize::do_cleanup_tasks() in safepoint.cpp and
// Threads::parallel_java_threads_do() in thread.cpp.
int ObjectSynchronizer::deflate_monitor_list(ObjectMonitor** list_p,
                                             int* count_p,
                                             ObjectMonitor** free_head_p,
                                             ObjectMonitor** free_tail_p) {
  ObjectMonitor* cur_mid_in_use = NULL;
  ObjectMonitor* mid = NULL;
  ObjectMonitor* next = NULL;
  int deflated_count = 0;

  // This list walk executes at a safepoint and does not race with any
  // other list walkers.

  for (mid = Atomic::load(list_p); mid != NULL; mid = next) {
    next = unmarked_next(mid);
    oop obj = (oop) mid->object();
    if (obj != NULL && deflate_monitor(mid, obj, free_head_p, free_tail_p)) {
      // Deflation succeeded and already updated free_head_p and
      // free_tail_p as needed. Finish the move to the local free list
      // by unlinking mid from the global or per-thread in-use list.
      if (cur_mid_in_use == NULL) {
        // mid is the list head so switch the list head to next:
        Atomic::store(list_p, next);
      } else {
        // Switch cur_mid_in_use's next field to next:
        cur_mid_in_use->set_next_om(next);
      }
      // At this point mid is disconnected from the in-use list.
      deflated_count++;
      Atomic::dec(count_p);
      // mid is current tail in the free_head_p list so NULL terminate it:
      mid->set_next_om(NULL);
    } else {
      cur_mid_in_use = mid;
    }
  }
  return deflated_count;
}

void ObjectSynchronizer::prepare_deflate_idle_monitors(DeflateMonitorCounters* counters) {
  counters->n_in_use = 0;              // currently associated with objects
  counters->n_in_circulation = 0;      // extant
  counters->n_scavenged = 0;           // reclaimed (global and per-thread)
  counters->per_thread_scavenged = 0;  // per-thread scavenge total
  counters->per_thread_times = 0.0;    // per-thread scavenge times
}

void ObjectSynchronizer::deflate_idle_monitors(DeflateMonitorCounters* counters) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");
  bool deflated = false;

  ObjectMonitor* free_head_p = NULL;  // Local SLL of scavenged monitors
  ObjectMonitor* free_tail_p = NULL;
  elapsedTimer timer;

  if (log_is_enabled(Info, monitorinflation)) {
    timer.start();
  }

  // Note: the thread-local monitors lists get deflated in
  // a separate pass. See deflate_thread_local_monitors().

  // For moribund threads, scan om_list_globals._in_use_list
  int deflated_count = 0;
  if (Atomic::load(&om_list_globals._in_use_list) != NULL) {
    // Update n_in_circulation before om_list_globals._in_use_count is
    // updated by deflation.
    Atomic::add(&counters->n_in_circulation,
                Atomic::load(&om_list_globals._in_use_count));

    deflated_count = deflate_monitor_list(&om_list_globals._in_use_list,
                                          &om_list_globals._in_use_count,
                                          &free_head_p, &free_tail_p);
    Atomic::add(&counters->n_in_use, Atomic::load(&om_list_globals._in_use_count));
  }

  if (free_head_p != NULL) {
    // Move the deflated ObjectMonitors back to the global free list.
    guarantee(free_tail_p != NULL && deflated_count > 0, "invariant");
#ifdef ASSERT
    ObjectMonitor* l_next_om = free_tail_p->next_om();
#endif
    assert(l_next_om == NULL, "must be NULL: _next_om=" INTPTR_FORMAT, p2i(l_next_om));
    prepend_list_to_global_free_list(free_head_p, free_tail_p, deflated_count);
    Atomic::add(&counters->n_scavenged, deflated_count);
  }
  timer.stop();

  LogStreamHandle(Debug, monitorinflation) lsh_debug;
  LogStreamHandle(Info, monitorinflation) lsh_info;
  LogStream* ls = NULL;
  if (log_is_enabled(Debug, monitorinflation)) {
    ls = &lsh_debug;
  } else if (deflated_count != 0 && log_is_enabled(Info, monitorinflation)) {
    ls = &lsh_info;
  }
  if (ls != NULL) {
    ls->print_cr("deflating global idle monitors, %3.7f secs, %d monitors", timer.seconds(), deflated_count);
  }
}

void ObjectSynchronizer::finish_deflate_idle_monitors(DeflateMonitorCounters* counters) {
  // Report the cumulative time for deflating each thread's idle
  // monitors. Note: if the work is split among more than one
  // worker thread, then the reported time will likely be more
  // than a beginning to end measurement of the phase.
  log_info(safepoint, cleanup)("deflating per-thread idle monitors, %3.7f secs, monitors=%d", counters->per_thread_times, counters->per_thread_scavenged);

  if (log_is_enabled(Debug, monitorinflation)) {
    // exit_globals()'s call to audit_and_print_stats() is done
    // at the Info level and not at a safepoint.
    ObjectSynchronizer::audit_and_print_stats(false /* on_exit */);
  } else if (log_is_enabled(Info, monitorinflation)) {
    log_info(monitorinflation)("global_population=%d, global_in_use_count=%d, "
                               "global_free_count=%d",
                               Atomic::load(&om_list_globals._population),
                               Atomic::load(&om_list_globals._in_use_count),
                               Atomic::load(&om_list_globals._free_count));
  }

  OM_PERFDATA_OP(Deflations, inc(counters->n_scavenged));
  OM_PERFDATA_OP(MonExtant, set_value(counters->n_in_circulation));

  GVars.stw_random = os::random();
  GVars.stw_cycle++;
}

void ObjectSynchronizer::deflate_thread_local_monitors(Thread* thread, DeflateMonitorCounters* counters) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");

  ObjectMonitor* free_head_p = NULL;  // Local SLL of scavenged monitors
  ObjectMonitor* free_tail_p = NULL;
  elapsedTimer timer;

  if (log_is_enabled(Info, safepoint, cleanup) ||
      log_is_enabled(Info, monitorinflation)) {
    timer.start();
  }

  // Update n_in_circulation before om_in_use_count is updated by deflation.
  Atomic::add(&counters->n_in_circulation, Atomic::load(&thread->om_in_use_count));

  int deflated_count = deflate_monitor_list(&thread->om_in_use_list, &thread->om_in_use_count, &free_head_p, &free_tail_p);
  Atomic::add(&counters->n_in_use, Atomic::load(&thread->om_in_use_count));

  if (free_head_p != NULL) {
    // Move the deflated ObjectMonitors back to the global free list.
    guarantee(free_tail_p != NULL && deflated_count > 0, "invariant");
#ifdef ASSERT
    ObjectMonitor* l_next_om = free_tail_p->next_om();
#endif
    assert(l_next_om == NULL, "must be NULL: _next_om=" INTPTR_FORMAT, p2i(l_next_om));
    prepend_list_to_global_free_list(free_head_p, free_tail_p, deflated_count);
    Atomic::add(&counters->n_scavenged, deflated_count);
    Atomic::add(&counters->per_thread_scavenged, deflated_count);
  }

  timer.stop();
  counters->per_thread_times += timer.seconds();

  LogStreamHandle(Debug, monitorinflation) lsh_debug;
  LogStreamHandle(Info, monitorinflation) lsh_info;
  LogStream* ls = NULL;
  if (log_is_enabled(Debug, monitorinflation)) {
    ls = &lsh_debug;
  } else if (deflated_count != 0 && log_is_enabled(Info, monitorinflation)) {
    ls = &lsh_info;
  }
  if (ls != NULL) {
    ls->print_cr("jt=" INTPTR_FORMAT ": deflating per-thread idle monitors, %3.7f secs, %d monitors", p2i(thread), timer.seconds(), deflated_count);
  }
}

// Monitor cleanup on JavaThread::exit

// Iterate through monitor cache and attempt to release thread's monitors
// Gives up on a particular monitor if an exception occurs, but continues
// the overall iteration, swallowing the exception.
class ReleaseJavaMonitorsClosure: public MonitorClosure {
 private:
  TRAPS;

 public:
  ReleaseJavaMonitorsClosure(Thread* thread) : THREAD(thread) {}
  void do_monitor(ObjectMonitor* mid) {
    if (mid->owner() == THREAD) {
      (void)mid->complete_exit(CHECK);
    }
  }
};

// Release all inflated monitors owned by THREAD.  Lightweight monitors are
// ignored.  This is meant to be called during JNI thread detach which assumes
// all remaining monitors are heavyweight.  All exceptions are swallowed.
// Scanning the extant monitor list can be time consuming.
// A simple optimization is to add a per-thread flag that indicates a thread
// called jni_monitorenter() during its lifetime.
//
// Instead of No_Savepoint_Verifier it might be cheaper to
// use an idiom of the form:
//   auto int tmp = SafepointSynchronize::_safepoint_counter ;
//   <code that must not run at safepoint>
//   guarantee (((tmp ^ _safepoint_counter) | (tmp & 1)) == 0) ;
// Since the tests are extremely cheap we could leave them enabled
// for normal product builds.

void ObjectSynchronizer::release_monitors_owned_by_thread(TRAPS) {
  assert(THREAD == JavaThread::current(), "must be current Java thread");
  NoSafepointVerifier nsv;
  ReleaseJavaMonitorsClosure rjmc(THREAD);
  ObjectSynchronizer::monitors_iterate(&rjmc);
  THREAD->clear_pending_exception();
}

const char* ObjectSynchronizer::inflate_cause_name(const InflateCause cause) {
  switch (cause) {
    case inflate_cause_vm_internal:    return "VM Internal";
    case inflate_cause_monitor_enter:  return "Monitor Enter";
    case inflate_cause_wait:           return "Monitor Wait";
    case inflate_cause_notify:         return "Monitor Notify";
    case inflate_cause_hash_code:      return "Monitor Hash Code";
    case inflate_cause_jni_enter:      return "JNI Monitor Enter";
    case inflate_cause_jni_exit:       return "JNI Monitor Exit";
    default:
      ShouldNotReachHere();
  }
  return "Unknown";
}

//------------------------------------------------------------------------------
// Debugging code

u_char* ObjectSynchronizer::get_gvars_addr() {
  return (u_char*)&GVars;
}

u_char* ObjectSynchronizer::get_gvars_hc_sequence_addr() {
  return (u_char*)&GVars.hc_sequence;
}

size_t ObjectSynchronizer::get_gvars_size() {
  return sizeof(SharedGlobals);
}

u_char* ObjectSynchronizer::get_gvars_stw_random_addr() {
  return (u_char*)&GVars.stw_random;
}

// This function can be called at a safepoint or it can be called when
// we are trying to exit the VM. When we are trying to exit the VM, the
// list walker functions can run in parallel with the other list
// operations so spin-locking is used for safety.
//
// Calls to this function can be added in various places as a debugging
// aid; pass 'true' for the 'on_exit' parameter to have in-use monitor
// details logged at the Info level and 'false' for the 'on_exit'
// parameter to have in-use monitor details logged at the Trace level.
// deflate_monitor_list() no longer uses spin-locking so be careful
// when adding audit_and_print_stats() calls at a safepoint.
//
void ObjectSynchronizer::audit_and_print_stats(bool on_exit) {
  assert(on_exit || SafepointSynchronize::is_at_safepoint(), "invariant");

  LogStreamHandle(Debug, monitorinflation) lsh_debug;
  LogStreamHandle(Info, monitorinflation) lsh_info;
  LogStreamHandle(Trace, monitorinflation) lsh_trace;
  LogStream* ls = NULL;
  if (log_is_enabled(Trace, monitorinflation)) {
    ls = &lsh_trace;
  } else if (log_is_enabled(Debug, monitorinflation)) {
    ls = &lsh_debug;
  } else if (log_is_enabled(Info, monitorinflation)) {
    ls = &lsh_info;
  }
  assert(ls != NULL, "sanity check");

  // Log counts for the global and per-thread monitor lists:
  int chk_om_population = log_monitor_list_counts(ls);
  int error_cnt = 0;

  ls->print_cr("Checking global lists:");

  // Check om_list_globals._population:
  if (Atomic::load(&om_list_globals._population) == chk_om_population) {
    ls->print_cr("global_population=%d equals chk_om_population=%d",
                 Atomic::load(&om_list_globals._population), chk_om_population);
  } else {
    // With fine grained locks on the monitor lists, it is possible for
    // log_monitor_list_counts() to return a value that doesn't match
    // om_list_globals._population. So far a higher value has been
    // seen in testing so something is being double counted by
    // log_monitor_list_counts().
    ls->print_cr("WARNING: global_population=%d is not equal to "
                 "chk_om_population=%d",
                 Atomic::load(&om_list_globals._population), chk_om_population);
  }

  // Check om_list_globals._in_use_list and om_list_globals._in_use_count:
  chk_global_in_use_list_and_count(ls, &error_cnt);

  // Check om_list_globals._free_list and om_list_globals._free_count:
  chk_global_free_list_and_count(ls, &error_cnt);

  ls->print_cr("Checking per-thread lists:");

  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    // Check om_in_use_list and om_in_use_count:
    chk_per_thread_in_use_list_and_count(jt, ls, &error_cnt);

    // Check om_free_list and om_free_count:
    chk_per_thread_free_list_and_count(jt, ls, &error_cnt);
  }

  if (error_cnt == 0) {
    ls->print_cr("No errors found in monitor list checks.");
  } else {
    log_error(monitorinflation)("found monitor list errors: error_cnt=%d", error_cnt);
  }

  if ((on_exit && log_is_enabled(Info, monitorinflation)) ||
      (!on_exit && log_is_enabled(Trace, monitorinflation))) {
    // When exiting this log output is at the Info level. When called
    // at a safepoint, this log output is at the Trace level since
    // there can be a lot of it.
    log_in_use_monitor_details(ls);
  }

  ls->flush();

  guarantee(error_cnt == 0, "ERROR: found monitor list errors: error_cnt=%d", error_cnt);
}

// Check a free monitor entry; log any errors.
void ObjectSynchronizer::chk_free_entry(JavaThread* jt, ObjectMonitor* n,
                                        outputStream * out, int *error_cnt_p) {
  stringStream ss;
  if (n->is_busy()) {
    if (jt != NULL) {
      out->print_cr("ERROR: jt=" INTPTR_FORMAT ", monitor=" INTPTR_FORMAT
                    ": free per-thread monitor must not be busy: %s", p2i(jt),
                    p2i(n), n->is_busy_to_string(&ss));
    } else {
      out->print_cr("ERROR: monitor=" INTPTR_FORMAT ": free global monitor "
                    "must not be busy: %s", p2i(n), n->is_busy_to_string(&ss));
    }
    *error_cnt_p = *error_cnt_p + 1;
  }
  if (n->header().value() != 0) {
    if (jt != NULL) {
      out->print_cr("ERROR: jt=" INTPTR_FORMAT ", monitor=" INTPTR_FORMAT
                    ": free per-thread monitor must have NULL _header "
                    "field: _header=" INTPTR_FORMAT, p2i(jt), p2i(n),
                    n->header().value());
    } else {
      out->print_cr("ERROR: monitor=" INTPTR_FORMAT ": free global monitor "
                    "must have NULL _header field: _header=" INTPTR_FORMAT,
                    p2i(n), n->header().value());
    }
    *error_cnt_p = *error_cnt_p + 1;
  }
  if (n->object() != NULL) {
    if (jt != NULL) {
      out->print_cr("ERROR: jt=" INTPTR_FORMAT ", monitor=" INTPTR_FORMAT
                    ": free per-thread monitor must have NULL _object "
                    "field: _object=" INTPTR_FORMAT, p2i(jt), p2i(n),
                    p2i(n->object()));
    } else {
      out->print_cr("ERROR: monitor=" INTPTR_FORMAT ": free global monitor "
                    "must have NULL _object field: _object=" INTPTR_FORMAT,
                    p2i(n), p2i(n->object()));
    }
    *error_cnt_p = *error_cnt_p + 1;
  }
}

// Lock the next ObjectMonitor for traversal and unlock the current
// ObjectMonitor. Returns the next ObjectMonitor if there is one.
// Otherwise returns NULL (after unlocking the current ObjectMonitor).
// This function is used by the various list walker functions to
// safely walk a list without allowing an ObjectMonitor to be moved
// to another list in the middle of a walk.
static ObjectMonitor* lock_next_for_traversal(ObjectMonitor* cur) {
  assert(is_locked(cur), "cur=" INTPTR_FORMAT " must be locked", p2i(cur));
  ObjectMonitor* next = unmarked_next(cur);
  if (next == NULL) {  // Reached the end of the list.
    om_unlock(cur);
    return NULL;
  }
  om_lock(next);   // Lock next before unlocking current to keep
  om_unlock(cur);  // from being by-passed by another thread.
  return next;
}

// Check the global free list and count; log the results of the checks.
void ObjectSynchronizer::chk_global_free_list_and_count(outputStream * out,
                                                        int *error_cnt_p) {
  int chk_om_free_count = 0;
  ObjectMonitor* cur = NULL;
  if ((cur = get_list_head_locked(&om_list_globals._free_list)) != NULL) {
    // Marked the global free list head so process the list.
    while (true) {
      chk_free_entry(NULL /* jt */, cur, out, error_cnt_p);
      chk_om_free_count++;

      cur = lock_next_for_traversal(cur);
      if (cur == NULL) {
        break;
      }
    }
  }
  int l_free_count = Atomic::load(&om_list_globals._free_count);
  if (l_free_count == chk_om_free_count) {
    out->print_cr("global_free_count=%d equals chk_om_free_count=%d",
                  l_free_count, chk_om_free_count);
  } else {
    // With fine grained locks on om_list_globals._free_list, it
    // is possible for an ObjectMonitor to be prepended to
    // om_list_globals._free_list after we started calculating
    // chk_om_free_count so om_list_globals._free_count may not
    // match anymore.
    out->print_cr("WARNING: global_free_count=%d is not equal to "
                  "chk_om_free_count=%d", l_free_count, chk_om_free_count);
  }
}

// Check the global in-use list and count; log the results of the checks.
void ObjectSynchronizer::chk_global_in_use_list_and_count(outputStream * out,
                                                          int *error_cnt_p) {
  int chk_om_in_use_count = 0;
  ObjectMonitor* cur = NULL;
  if ((cur = get_list_head_locked(&om_list_globals._in_use_list)) != NULL) {
    // Marked the global in-use list head so process the list.
    while (true) {
      chk_in_use_entry(NULL /* jt */, cur, out, error_cnt_p);
      chk_om_in_use_count++;

      cur = lock_next_for_traversal(cur);
      if (cur == NULL) {
        break;
      }
    }
  }
  int l_in_use_count = Atomic::load(&om_list_globals._in_use_count);
  if (l_in_use_count == chk_om_in_use_count) {
    out->print_cr("global_in_use_count=%d equals chk_om_in_use_count=%d",
                  l_in_use_count, chk_om_in_use_count);
  } else {
    // With fine grained locks on the monitor lists, it is possible for
    // an exiting JavaThread to put its in-use ObjectMonitors on the
    // global in-use list after chk_om_in_use_count is calculated above.
    out->print_cr("WARNING: global_in_use_count=%d is not equal to chk_om_in_use_count=%d",
                  l_in_use_count, chk_om_in_use_count);
  }
}

// Check an in-use monitor entry; log any errors.
void ObjectSynchronizer::chk_in_use_entry(JavaThread* jt, ObjectMonitor* n,
                                          outputStream * out, int *error_cnt_p) {
  if (n->header().value() == 0) {
    if (jt != NULL) {
      out->print_cr("ERROR: jt=" INTPTR_FORMAT ", monitor=" INTPTR_FORMAT
                    ": in-use per-thread monitor must have non-NULL _header "
                    "field.", p2i(jt), p2i(n));
    } else {
      out->print_cr("ERROR: monitor=" INTPTR_FORMAT ": in-use global monitor "
                    "must have non-NULL _header field.", p2i(n));
    }
    *error_cnt_p = *error_cnt_p + 1;
  }
  if (n->object() == NULL) {
    if (jt != NULL) {
      out->print_cr("ERROR: jt=" INTPTR_FORMAT ", monitor=" INTPTR_FORMAT
                    ": in-use per-thread monitor must have non-NULL _object "
                    "field.", p2i(jt), p2i(n));
    } else {
      out->print_cr("ERROR: monitor=" INTPTR_FORMAT ": in-use global monitor "
                    "must have non-NULL _object field.", p2i(n));
    }
    *error_cnt_p = *error_cnt_p + 1;
  }
  const oop obj = (oop)n->object();
  const markWord mark = obj->mark();
  if (!mark.has_monitor()) {
    if (jt != NULL) {
      out->print_cr("ERROR: jt=" INTPTR_FORMAT ", monitor=" INTPTR_FORMAT
                    ": in-use per-thread monitor's object does not think "
                    "it has a monitor: obj=" INTPTR_FORMAT ", mark="
                    INTPTR_FORMAT,  p2i(jt), p2i(n), p2i(obj), mark.value());
    } else {
      out->print_cr("ERROR: monitor=" INTPTR_FORMAT ": in-use global "
                    "monitor's object does not think it has a monitor: obj="
                    INTPTR_FORMAT ", mark=" INTPTR_FORMAT, p2i(n),
                    p2i(obj), mark.value());
    }
    *error_cnt_p = *error_cnt_p + 1;
  }
  ObjectMonitor* const obj_mon = mark.monitor();
  if (n != obj_mon) {
    if (jt != NULL) {
      out->print_cr("ERROR: jt=" INTPTR_FORMAT ", monitor=" INTPTR_FORMAT
                    ": in-use per-thread monitor's object does not refer "
                    "to the same monitor: obj=" INTPTR_FORMAT ", mark="
                    INTPTR_FORMAT ", obj_mon=" INTPTR_FORMAT, p2i(jt),
                    p2i(n), p2i(obj), mark.value(), p2i(obj_mon));
    } else {
      out->print_cr("ERROR: monitor=" INTPTR_FORMAT ": in-use global "
                    "monitor's object does not refer to the same monitor: obj="
                    INTPTR_FORMAT ", mark=" INTPTR_FORMAT ", obj_mon="
                    INTPTR_FORMAT, p2i(n), p2i(obj), mark.value(), p2i(obj_mon));
    }
    *error_cnt_p = *error_cnt_p + 1;
  }
}

// Check the thread's free list and count; log the results of the checks.
void ObjectSynchronizer::chk_per_thread_free_list_and_count(JavaThread *jt,
                                                            outputStream * out,
                                                            int *error_cnt_p) {
  int chk_om_free_count = 0;
  ObjectMonitor* cur = NULL;
  if ((cur = get_list_head_locked(&jt->om_free_list)) != NULL) {
    // Marked the per-thread free list head so process the list.
    while (true) {
      chk_free_entry(jt, cur, out, error_cnt_p);
      chk_om_free_count++;

      cur = lock_next_for_traversal(cur);
      if (cur == NULL) {
        break;
      }
    }
  }
  int l_om_free_count = Atomic::load(&jt->om_free_count);
  if (l_om_free_count == chk_om_free_count) {
    out->print_cr("jt=" INTPTR_FORMAT ": om_free_count=%d equals "
                  "chk_om_free_count=%d", p2i(jt), l_om_free_count, chk_om_free_count);
  } else {
    out->print_cr("ERROR: jt=" INTPTR_FORMAT ": om_free_count=%d is not "
                  "equal to chk_om_free_count=%d", p2i(jt), l_om_free_count,
                  chk_om_free_count);
    *error_cnt_p = *error_cnt_p + 1;
  }
}

// Check the thread's in-use list and count; log the results of the checks.
void ObjectSynchronizer::chk_per_thread_in_use_list_and_count(JavaThread *jt,
                                                              outputStream * out,
                                                              int *error_cnt_p) {
  int chk_om_in_use_count = 0;
  ObjectMonitor* cur = NULL;
  if ((cur = get_list_head_locked(&jt->om_in_use_list)) != NULL) {
    // Marked the per-thread in-use list head so process the list.
    while (true) {
      chk_in_use_entry(jt, cur, out, error_cnt_p);
      chk_om_in_use_count++;

      cur = lock_next_for_traversal(cur);
      if (cur == NULL) {
        break;
      }
    }
  }
  int l_om_in_use_count = Atomic::load(&jt->om_in_use_count);
  if (l_om_in_use_count == chk_om_in_use_count) {
    out->print_cr("jt=" INTPTR_FORMAT ": om_in_use_count=%d equals "
                  "chk_om_in_use_count=%d", p2i(jt), l_om_in_use_count,
                  chk_om_in_use_count);
  } else {
    out->print_cr("ERROR: jt=" INTPTR_FORMAT ": om_in_use_count=%d is not "
                  "equal to chk_om_in_use_count=%d", p2i(jt), l_om_in_use_count,
                  chk_om_in_use_count);
    *error_cnt_p = *error_cnt_p + 1;
  }
}

// Log details about ObjectMonitors on the in-use lists. The 'BHL'
// flags indicate why the entry is in-use, 'object' and 'object type'
// indicate the associated object and its type.
void ObjectSynchronizer::log_in_use_monitor_details(outputStream * out) {
  stringStream ss;
  if (Atomic::load(&om_list_globals._in_use_count) > 0) {
    out->print_cr("In-use global monitor info:");
    out->print_cr("(B -> is_busy, H -> has hash code, L -> lock status)");
    out->print_cr("%18s  %s  %18s  %18s",
                  "monitor", "BHL", "object", "object type");
    out->print_cr("==================  ===  ==================  ==================");
    ObjectMonitor* cur = NULL;
    if ((cur = get_list_head_locked(&om_list_globals._in_use_list)) != NULL) {
      // Marked the global in-use list head so process the list.
      while (true) {
        const oop obj = (oop) cur->object();
        const markWord mark = cur->header();
        ResourceMark rm;
        out->print(INTPTR_FORMAT "  %d%d%d  " INTPTR_FORMAT "  %s", p2i(cur),
                   cur->is_busy() != 0, mark.hash() != 0, cur->owner() != NULL,
                   p2i(obj), obj->klass()->external_name());
        if (cur->is_busy() != 0) {
          out->print(" (%s)", cur->is_busy_to_string(&ss));
          ss.reset();
        }
        out->cr();

        cur = lock_next_for_traversal(cur);
        if (cur == NULL) {
          break;
        }
      }
    }
  }

  out->print_cr("In-use per-thread monitor info:");
  out->print_cr("(B -> is_busy, H -> has hash code, L -> lock status)");
  out->print_cr("%18s  %18s  %s  %18s  %18s",
                "jt", "monitor", "BHL", "object", "object type");
  out->print_cr("==================  ==================  ===  ==================  ==================");
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    ObjectMonitor* cur = NULL;
    if ((cur = get_list_head_locked(&jt->om_in_use_list)) != NULL) {
      // Marked the global in-use list head so process the list.
      while (true) {
        const oop obj = (oop) cur->object();
        const markWord mark = cur->header();
        ResourceMark rm;
        out->print(INTPTR_FORMAT "  " INTPTR_FORMAT "  %d%d%d  " INTPTR_FORMAT
                   "  %s", p2i(jt), p2i(cur), cur->is_busy() != 0,
                   mark.hash() != 0, cur->owner() != NULL, p2i(obj),
                   obj->klass()->external_name());
        if (cur->is_busy() != 0) {
          out->print(" (%s)", cur->is_busy_to_string(&ss));
          ss.reset();
        }
        out->cr();

        cur = lock_next_for_traversal(cur);
        if (cur == NULL) {
          break;
        }
      }
    }
  }

  out->flush();
}

// Log counts for the global and per-thread monitor lists and return
// the population count.
int ObjectSynchronizer::log_monitor_list_counts(outputStream * out) {
  int pop_count = 0;
  out->print_cr("%18s  %10s  %10s  %10s",
                "Global Lists:", "InUse", "Free", "Total");
  out->print_cr("==================  ==========  ==========  ==========");
  int l_in_use_count = Atomic::load(&om_list_globals._in_use_count);
  int l_free_count = Atomic::load(&om_list_globals._free_count);
  out->print_cr("%18s  %10d  %10d  %10d", "", l_in_use_count,
                l_free_count, Atomic::load(&om_list_globals._population));
  pop_count += l_in_use_count + l_free_count;

  out->print_cr("%18s  %10s  %10s  %10s",
                "Per-Thread Lists:", "InUse", "Free", "Provision");
  out->print_cr("==================  ==========  ==========  ==========");

  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    int l_om_in_use_count = Atomic::load(&jt->om_in_use_count);
    int l_om_free_count = Atomic::load(&jt->om_free_count);
    out->print_cr(INTPTR_FORMAT "  %10d  %10d  %10d", p2i(jt),
                  l_om_in_use_count, l_om_free_count, jt->om_free_provision);
    pop_count += l_om_in_use_count + l_om_free_count;
  }
  return pop_count;
}

#ifndef PRODUCT

// Check if monitor belongs to the monitor cache
// The list is grow-only so it's *relatively* safe to traverse
// the list of extant blocks without taking a lock.

int ObjectSynchronizer::verify_objmon_isinpool(ObjectMonitor *monitor) {
  PaddedObjectMonitor* block = Atomic::load(&g_block_list);
  while (block != NULL) {
    assert(block->object() == CHAINMARKER, "must be a block header");
    if (monitor > &block[0] && monitor < &block[_BLOCKSIZE]) {
      address mon = (address)monitor;
      address blk = (address)block;
      size_t diff = mon - blk;
      assert((diff % sizeof(PaddedObjectMonitor)) == 0, "must be aligned");
      return 1;
    }
    // unmarked_next() is not needed with g_block_list (no locking
    // used with block linkage _next_om fields).
    block = (PaddedObjectMonitor*)block->next_om();
  }
  return 0;
}

#endif
