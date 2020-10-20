/*
 * Copyright (c) 2017, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHSTRDEDUPQUEUE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHSTRDEDUPQUEUE_HPP

#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shenandoah/shenandoahHeap.hpp"
#include "oops/oop.hpp"

template <uint buffer_size>
class ShenandoahOopBuffer : public CHeapObj<mtGC> {
private:
  oop           _buf[buffer_size];
  volatile uint _index;
  ShenandoahOopBuffer<buffer_size>* _next;

public:
  ShenandoahOopBuffer();

  bool is_full()  const;
  bool is_empty() const;
  uint size()     const;

  void push(oop obj);
  oop pop();

  void reset();

  void set_next(ShenandoahOopBuffer<buffer_size>* next);
  ShenandoahOopBuffer<buffer_size>* next() const;

  void unlink_or_oops_do(StringDedupUnlinkOrOopsDoClosure* cl);
  void oops_do(OopClosure* cl);

private:
  uint index_acquire() const;
  void set_index_release(uint index);
};

typedef ShenandoahOopBuffer<64> ShenandoahQueueBuffer;

// Muti-producer and single consumer queue set
class ShenandoahStrDedupQueue : public StringDedupQueue {
private:
  ShenandoahQueueBuffer** _producer_queues;
  ShenandoahQueueBuffer*  _consumer_queue;
  size_t                  _num_producer_queue;

  // The queue is used for producers to publish completed buffers
  ShenandoahQueueBuffer* _published_queues;

  // Cached free buffers
  ShenandoahQueueBuffer* _free_list;
  size_t                 _num_free_buffer;
  const size_t           _max_free_buffer;

  bool                   _cancel;

  // statistics
  size_t                 _total_buffers;

private:
  ~ShenandoahStrDedupQueue();

public:
  ShenandoahStrDedupQueue();

  void wait_impl();
  void cancel_wait_impl();

  void push_impl(uint worker_id, oop string_oop);
  oop  pop_impl();

  void unlink_or_oops_do_impl(StringDedupUnlinkOrOopsDoClosure* cl, size_t queue);

  void print_statistics_impl();
  void verify_impl();

protected:
  size_t num_queues() const { return num_queues_nv(); }

private:
  inline size_t num_queues_nv() const { return (_num_producer_queue + 2); }

  ShenandoahQueueBuffer* new_buffer();

  void release_buffers(ShenandoahQueueBuffer* list);

  ShenandoahQueueBuffer* queue_at(size_t queue_id) const;

  bool pop_candidate(oop& obj);

  void set_producer_buffer(ShenandoahQueueBuffer* buf, size_t queue_id);
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHSTRDEDUPQUEUE_HPP
