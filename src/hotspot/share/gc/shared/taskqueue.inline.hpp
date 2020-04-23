/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_TASKQUEUE_INLINE_HPP
#define SHARE_GC_SHARED_TASKQUEUE_INLINE_HPP

#include "gc/shared/taskqueue.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/debug.hpp"
#include "utilities/stack.inline.hpp"

template <class T, MEMFLAGS F>
inline GenericTaskQueueSet<T, F>::GenericTaskQueueSet(uint n) : _n(n) {
  typedef T* GenericTaskQueuePtr;
  _queues = NEW_C_HEAP_ARRAY(GenericTaskQueuePtr, n, F);
  for (uint i = 0; i < n; i++) {
    _queues[i] = NULL;
  }
}

template <class T, MEMFLAGS F>
inline GenericTaskQueueSet<T, F>::~GenericTaskQueueSet() {
  FREE_C_HEAP_ARRAY(T*, _queues);
}

template<class E, MEMFLAGS F, unsigned int N>
inline void GenericTaskQueue<E, F, N>::initialize() {
  _elems = ArrayAllocator<E>::allocate(N, F);
}

template<class E, MEMFLAGS F, unsigned int N>
inline GenericTaskQueue<E, F, N>::~GenericTaskQueue() {
  ArrayAllocator<E>::free(const_cast<E*>(_elems), N);
}

template<class E, MEMFLAGS F, unsigned int N> inline bool
GenericTaskQueue<E, F, N>::push(E t) {
  uint localBot = _bottom;
  assert(localBot < N, "_bottom out of range.");
  idx_t top = _age.top();
  uint dirty_n_elems = dirty_size(localBot, top);
  // A dirty_size of N-1 cannot happen in push.  Considering only push:
  // (1) dirty_n_elems is initially 0.
  // (2) push adds an element iff dirty_n_elems < max_elems(), which is N - 2.
  // (3) only push adding an element can increase dirty_n_elems.
  // => dirty_n_elems <= N - 2, by induction
  // => dirty_n_elems < N - 1, invariant
  //
  // A pop_global that is concurrent with push cannot produce a state where
  // dirty_size == N-1.  pop_global only removes an element if dirty_elems > 0,
  // so can't underflow to -1 (== N-1) with push.
  assert(dirty_n_elems <= max_elems(), "n_elems out of range.");
  if (dirty_n_elems < max_elems()) {
    // g++ complains if the volatile result of the assignment is
    // unused, so we cast the volatile away.  We cannot cast directly
    // to void, because gcc treats that as not using the result of the
    // assignment.  However, casting to E& means that we trigger an
    // unused-value warning.  So, we cast the E& to void.
    (void) const_cast<E&>(_elems[localBot] = t);
    Atomic::release_store(&_bottom, increment_index(localBot));
    TASKQUEUE_STATS_ONLY(stats.record_push());
    return true;
  }
  return false;                 // Queue is full.
}

template <class E, MEMFLAGS F, unsigned int N>
inline bool OverflowTaskQueue<E, F, N>::push(E t)
{
  if (!taskqueue_t::push(t)) {
    overflow_stack()->push(t);
    TASKQUEUE_STATS_ONLY(stats.record_overflow(overflow_stack()->size()));
  }
  return true;
}

template <class E, MEMFLAGS F, unsigned int N>
inline bool OverflowTaskQueue<E, F, N>::try_push_to_taskqueue(E t) {
  return taskqueue_t::push(t);
}

// pop_local_slow() is done by the owning thread and is trying to
// get the last task in the queue.  It will compete with pop_global()
// that will be used by other threads.  The tag age is incremented
// whenever the queue goes empty which it will do here if this thread
// gets the last task or in pop_global() if the queue wraps (top == 0
// and pop_global() succeeds, see pop_global()).
template<class E, MEMFLAGS F, unsigned int N>
bool GenericTaskQueue<E, F, N>::pop_local_slow(uint localBot, Age oldAge) {
  // This queue was observed to contain exactly one element; either this
  // thread will claim it, or a competing "pop_global".  In either case,
  // the queue will be logically empty afterwards.  Create a new Age value
  // that represents the empty queue for the given value of "_bottom".  (We
  // must also increment "tag" because of the case where "bottom == 1",
  // "top == 0".  A pop_global could read the queue element in that case,
  // then have the owner thread do a pop followed by another push.  Without
  // the incrementing of "tag", the pop_global's CAS could succeed,
  // allowing it to believe it has claimed the stale element.)
  Age newAge((idx_t)localBot, oldAge.tag() + 1);
  // Perhaps a competing pop_global has already incremented "top", in which
  // case it wins the element.
  if (localBot == oldAge.top()) {
    // No competing pop_global has yet incremented "top"; we'll try to
    // install new_age, thus claiming the element.
    Age tempAge = _age.cmpxchg(newAge, oldAge);
    if (tempAge == oldAge) {
      // We win.
      assert(dirty_size(localBot, _age.top()) != N - 1, "sanity");
      TASKQUEUE_STATS_ONLY(stats.record_pop_slow());
      return true;
    }
  }
  // We lose; a completing pop_global gets the element.  But the queue is empty
  // and top is greater than bottom.  Fix this representation of the empty queue
  // to become the canonical one.
  _age.set(newAge);
  assert(dirty_size(localBot, _age.top()) != N - 1, "sanity");
  return false;
}

template<class E, MEMFLAGS F, unsigned int N> inline bool
GenericTaskQueue<E, F, N>::pop_local(volatile E& t, uint threshold) {
  uint localBot = _bottom;
  // This value cannot be N-1.  That can only occur as a result of
  // the assignment to bottom in this method.  If it does, this method
  // resets the size to 0 before the next call (which is sequential,
  // since this is pop_local.)
  uint dirty_n_elems = dirty_size(localBot, _age.top());
  assert(dirty_n_elems != N - 1, "Shouldn't be possible...");
  if (dirty_n_elems <= threshold) return false;
  localBot = decrement_index(localBot);
  _bottom = localBot;
  // This is necessary to prevent any read below from being reordered
  // before the store just above.
  OrderAccess::fence();
  // g++ complains if the volatile result of the assignment is
  // unused, so we cast the volatile away.  We cannot cast directly
  // to void, because gcc treats that as not using the result of the
  // assignment.  However, casting to E& means that we trigger an
  // unused-value warning.  So, we cast the E& to void.
  (void) const_cast<E&>(t = _elems[localBot]);
  // This is a second read of "age"; the "size()" above is the first.
  // If there's still at least one element in the queue, based on the
  // "_bottom" and "age" we've read, then there can be no interference with
  // a "pop_global" operation, and we're done.
  idx_t tp = _age.top();    // XXX
  if (size(localBot, tp) > 0) {
    assert(dirty_size(localBot, tp) != N - 1, "sanity");
    TASKQUEUE_STATS_ONLY(stats.record_pop());
    return true;
  } else {
    // Otherwise, the queue contained exactly one element; we take the slow
    // path.

    // The barrier is required to prevent reordering the two reads of _age:
    // one is the _age.get() below, and the other is _age.top() above the if-stmt.
    // The algorithm may fail if _age.get() reads an older value than _age.top().
    OrderAccess::loadload();
    return pop_local_slow(localBot, _age.get());
  }
}

template <class E, MEMFLAGS F, unsigned int N>
bool OverflowTaskQueue<E, F, N>::pop_overflow(E& t)
{
  if (overflow_empty()) return false;
  t = overflow_stack()->pop();
  return true;
}

template<class E, MEMFLAGS F, unsigned int N>
bool GenericTaskQueue<E, F, N>::pop_global(volatile E& t) {
  Age oldAge = _age.get();
#ifndef CPU_MULTI_COPY_ATOMIC
  // Architectures with non-multi-copy-atomic memory model require a
  // full fence here to guarantee that bottom is not older than age,
  // which is crucial for the correctness of the algorithm.
  //
  // We need a full fence here for this case:
  //
  // Thread1: set bottom (push)
  // Thread2: read age, read bottom, set age (pop_global)
  // Thread3: read age, read bottom (pop_global)
  //
  // The requirement is that Thread3 must never read an older bottom
  // value than Thread2 after Thread3 has seen the age value from
  // Thread2.
  OrderAccess::fence();
#else
  // Everyone else can make do with a LoadLoad barrier to keep reads
  // from _age and _bottom in order.
  OrderAccess::loadload();
#endif
  uint localBot = Atomic::load_acquire(&_bottom);
  uint n_elems = size(localBot, oldAge.top());
  if (n_elems == 0) {
    return false;
  }

  // g++ complains if the volatile result of the assignment is
  // unused, so we cast the volatile away.  We cannot cast directly
  // to void, because gcc treats that as not using the result of the
  // assignment.  However, casting to E& means that we trigger an
  // unused-value warning.  So, we cast the E& to void.
  (void) const_cast<E&>(t = _elems[oldAge.top()]);
  Age newAge(oldAge);
  newAge.increment();
  Age resAge = _age.cmpxchg(newAge, oldAge);

  // Note that using "_bottom" here might fail, since a pop_local might
  // have decremented it.
  assert(dirty_size(localBot, newAge.top()) != N - 1, "sanity");
  return resAge == oldAge;
}

inline int randomParkAndMiller(int *seed0) {
  const int a =      16807;
  const int m = 2147483647;
  const int q =     127773;  /* m div a */
  const int r =       2836;  /* m mod a */
  STATIC_ASSERT(sizeof(int) == 4);
  int seed = *seed0;
  int hi   = seed / q;
  int lo   = seed % q;
  int test = a * lo - r * hi;
  if (test > 0) {
    seed = test;
  } else {
    seed = test + m;
  }
  *seed0 = seed;
  return seed;
}

template<class E, MEMFLAGS F, unsigned int N>
int GenericTaskQueue<E, F, N>::next_random_queue_id() {
  return randomParkAndMiller(&_seed);
}

template<class T, MEMFLAGS F> bool
GenericTaskQueueSet<T, F>::steal_best_of_2(uint queue_num, E& t) {
  if (_n > 2) {
    T* const local_queue = _queues[queue_num];
    uint k1 = queue_num;

    if (local_queue->is_last_stolen_queue_id_valid()) {
      k1 = local_queue->last_stolen_queue_id();
      assert(k1 != queue_num, "Should not be the same");
    } else {
      while (k1 == queue_num) {
        k1 = local_queue->next_random_queue_id() % _n;
      }
    }

    uint k2 = queue_num;
    while (k2 == queue_num || k2 == k1) {
      k2 = local_queue->next_random_queue_id() % _n;
    }
    // Sample both and try the larger.
    uint sz1 = _queues[k1]->size();
    uint sz2 = _queues[k2]->size();

    uint sel_k = 0;
    bool suc = false;

    if (sz2 > sz1) {
      sel_k = k2;
      suc = _queues[k2]->pop_global(t);
    } else if (sz1 > 0) {
      sel_k = k1;
      suc = _queues[k1]->pop_global(t);
    }

    if (suc) {
      local_queue->set_last_stolen_queue_id(sel_k);
    } else {
      local_queue->invalidate_last_stolen_queue_id();
    }

    return suc;
  } else if (_n == 2) {
    // Just try the other one.
    uint k = (queue_num + 1) % 2;
    return _queues[k]->pop_global(t);
  } else {
    assert(_n == 1, "can't be zero.");
    return false;
  }
}

template<class T, MEMFLAGS F> bool
GenericTaskQueueSet<T, F>::steal(uint queue_num, E& t) {
  for (uint i = 0; i < 2 * _n; i++) {
    TASKQUEUE_STATS_ONLY(queue(queue_num)->stats.record_steal_attempt());
    if (steal_best_of_2(queue_num, t)) {
      TASKQUEUE_STATS_ONLY(queue(queue_num)->stats.record_steal());
      return true;
    }
  }
  return false;
}

template <unsigned int N, MEMFLAGS F>
inline typename TaskQueueSuper<N, F>::Age TaskQueueSuper<N, F>::Age::cmpxchg(const Age new_age, const Age old_age) volatile {
  return Atomic::cmpxchg(&_data, old_age._data, new_age._data);
}

template<class E, MEMFLAGS F, unsigned int N>
template<class Fn>
inline void GenericTaskQueue<E, F, N>::iterate(Fn fn) {
  uint iters = size();
  uint index = _bottom;
  for (uint i = 0; i < iters; ++i) {
    index = decrement_index(index);
    fn(const_cast<E&>(_elems[index])); // cast away volatility
  }
}


#endif // SHARE_GC_SHARED_TASKQUEUE_INLINE_HPP
