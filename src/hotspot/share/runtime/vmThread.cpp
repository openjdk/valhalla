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
#include "compiler/compileBroker.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/support/jfrThreadId.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "logging/logConfiguration.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "oops/verifyOopClosure.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "services/runtimeService.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/vmError.hpp"
#include "utilities/xmlstream.hpp"

VM_QueueHead VMOperationQueue::_queue_head[VMOperationQueue::nof_priorities];

VMOperationQueue::VMOperationQueue() {
  // The queue is a circular doubled-linked list, which always contains
  // one element (i.e., one element means empty).
  for(int i = 0; i < nof_priorities; i++) {
    _queue_length[i] = 0;
    _queue_counter = 0;
    _queue[i] = &_queue_head[i];
    _queue[i]->set_next(_queue[i]);
    _queue[i]->set_prev(_queue[i]);
  }
}


bool VMOperationQueue::queue_empty(int prio) {
  // It is empty if there is exactly one element
  bool empty = (_queue[prio] == _queue[prio]->next());
  assert( (_queue_length[prio] == 0 && empty) ||
          (_queue_length[prio] > 0  && !empty), "sanity check");
  return _queue_length[prio] == 0;
}

// Inserts an element to the right of the q element
void VMOperationQueue::insert(VM_Operation* q, VM_Operation* n) {
  assert(q->next()->prev() == q && q->prev()->next() == q, "sanity check");
  n->set_prev(q);
  n->set_next(q->next());
  q->next()->set_prev(n);
  q->set_next(n);
}

void VMOperationQueue::queue_add(int prio, VM_Operation *op) {
  _queue_length[prio]++;
  insert(_queue[prio]->prev(), op);
}


void VMOperationQueue::unlink(VM_Operation* q) {
  assert(q->next()->prev() == q && q->prev()->next() == q, "sanity check");
  q->prev()->set_next(q->next());
  q->next()->set_prev(q->prev());
}

VM_Operation* VMOperationQueue::queue_remove_front(int prio) {
  if (queue_empty(prio)) return NULL;
  assert(_queue_length[prio] >= 0, "sanity check");
  _queue_length[prio]--;
  VM_Operation* r = _queue[prio]->next();
  assert(r != _queue[prio], "cannot remove base element");
  unlink(r);
  return r;
}

VM_Operation* VMOperationQueue::queue_drain(int prio) {
  if (queue_empty(prio)) return NULL;
  DEBUG_ONLY(int length = _queue_length[prio];);
  assert(length >= 0, "sanity check");
  _queue_length[prio] = 0;
  VM_Operation* r = _queue[prio]->next();
  assert(r != _queue[prio], "cannot remove base element");
  // remove links to base element from head and tail
  r->set_prev(NULL);
  _queue[prio]->prev()->set_next(NULL);
  // restore queue to empty state
  _queue[prio]->set_next(_queue[prio]);
  _queue[prio]->set_prev(_queue[prio]);
  assert(queue_empty(prio), "drain corrupted queue");
#ifdef ASSERT
  int len = 0;
  VM_Operation* cur;
  for(cur = r; cur != NULL; cur=cur->next()) len++;
  assert(len == length, "drain lost some ops");
#endif
  return r;
}

//-----------------------------------------------------------------
// High-level interface
void VMOperationQueue::add(VM_Operation *op) {

  HOTSPOT_VMOPS_REQUEST(
                   (char *) op->name(), strlen(op->name()),
                   op->evaluate_at_safepoint() ? 0 : 1);

  // Encapsulates VM queue policy. Currently, that
  // only involves putting them on the right list
  queue_add(op->evaluate_at_safepoint() ? SafepointPriority : MediumPriority, op);
}

VM_Operation* VMOperationQueue::remove_next() {
  // Assuming VMOperation queue is two-level priority queue. If there are
  // more than two priorities, we need a different scheduling algorithm.
  assert(SafepointPriority == 0 && MediumPriority == 1 && nof_priorities == 2,
         "current algorithm does not work");

  // simple counter based scheduling to prevent starvation of lower priority
  // queue. -- see 4390175
  int high_prio, low_prio;
  if (_queue_counter++ < 10) {
      high_prio = SafepointPriority;
      low_prio  = MediumPriority;
  } else {
      _queue_counter = 0;
      high_prio = MediumPriority;
      low_prio  = SafepointPriority;
  }

  return queue_remove_front(queue_empty(high_prio) ? low_prio : high_prio);
}

//------------------------------------------------------------------------------------------------------------------
// Timeout machinery

void VMOperationTimeoutTask::task() {
  assert(AbortVMOnVMOperationTimeout, "only if enabled");
  if (is_armed()) {
    jlong delay = nanos_to_millis(os::javaTimeNanos() - _arm_time);
    if (delay > AbortVMOnVMOperationTimeoutDelay) {
      fatal("VM operation took too long: " JLONG_FORMAT " ms (timeout: " INTX_FORMAT " ms)",
            delay, AbortVMOnVMOperationTimeoutDelay);
    }
  }
}

bool VMOperationTimeoutTask::is_armed() {
  return Atomic::load_acquire(&_armed) != 0;
}

void VMOperationTimeoutTask::arm() {
  _arm_time = os::javaTimeNanos();
  Atomic::release_store_fence(&_armed, 1);
}

void VMOperationTimeoutTask::disarm() {
  Atomic::release_store_fence(&_armed, 0);
}

//------------------------------------------------------------------------------------------------------------------
// Implementation of VMThread stuff

bool              VMThread::_should_terminate   = false;
bool              VMThread::_terminated         = false;
Monitor*          VMThread::_terminate_lock     = NULL;
VMThread*         VMThread::_vm_thread          = NULL;
VM_Operation*     VMThread::_cur_vm_operation   = NULL;
VMOperationQueue* VMThread::_vm_queue           = NULL;
PerfCounter*      VMThread::_perf_accumulated_vm_operation_time = NULL;
uint64_t          VMThread::_coalesced_count = 0;
VMOperationTimeoutTask* VMThread::_timeout_task = NULL;


void VMThread::create() {
  assert(vm_thread() == NULL, "we can only allocate one VMThread");
  _vm_thread = new VMThread();

  if (AbortVMOnVMOperationTimeout) {
    // Make sure we call the timeout task frequently enough, but not too frequent.
    // Try to make the interval 10% of the timeout delay, so that we miss the timeout
    // by those 10% at max. Periodic task also expects it to fit min/max intervals.
    size_t interval = (size_t)AbortVMOnVMOperationTimeoutDelay / 10;
    interval = interval / PeriodicTask::interval_gran * PeriodicTask::interval_gran;
    interval = MAX2<size_t>(interval, PeriodicTask::min_interval);
    interval = MIN2<size_t>(interval, PeriodicTask::max_interval);

    _timeout_task = new VMOperationTimeoutTask(interval);
    _timeout_task->enroll();
  } else {
    assert(_timeout_task == NULL, "sanity");
  }

  // Create VM operation queue
  _vm_queue = new VMOperationQueue();
  guarantee(_vm_queue != NULL, "just checking");

  _terminate_lock = new Monitor(Mutex::safepoint, "VMThread::_terminate_lock", true,
                                Monitor::_safepoint_check_never);

  if (UsePerfData) {
    // jvmstat performance counters
    Thread* THREAD = Thread::current();
    _perf_accumulated_vm_operation_time =
                 PerfDataManager::create_counter(SUN_THREADS, "vmOperationTime",
                                                 PerfData::U_Ticks, CHECK);
  }
}

VMThread::VMThread() : NamedThread() {
  set_name("VM Thread");
}

void VMThread::destroy() {
  _vm_thread = NULL;      // VM thread is gone
}

static VM_None halt_op("Halt");

void VMThread::run() {
  assert(this == vm_thread(), "check");

  // Notify_lock wait checks on active_handles() to rewait in
  // case of spurious wakeup, it should wait on the last
  // value set prior to the notify
  this->set_active_handles(JNIHandleBlock::allocate_block());

  {
    MutexLocker ml(Notify_lock);
    Notify_lock->notify();
  }
  // Notify_lock is destroyed by Threads::create_vm()

  int prio = (VMThreadPriority == -1)
    ? os::java_to_os_priority[NearMaxPriority]
    : VMThreadPriority;
  // Note that I cannot call os::set_priority because it expects Java
  // priorities and I am *explicitly* using OS priorities so that it's
  // possible to set the VM thread priority higher than any Java thread.
  os::set_native_priority( this, prio );

  // Wait for VM_Operations until termination
  this->loop();

  // Note the intention to exit before safepointing.
  // 6295565  This has the effect of waiting for any large tty
  // outputs to finish.
  if (xtty != NULL) {
    ttyLocker ttyl;
    xtty->begin_elem("destroy_vm");
    xtty->stamp();
    xtty->end_elem();
    assert(should_terminate(), "termination flag must be set");
  }

  // 4526887 let VM thread exit at Safepoint
  _cur_vm_operation = &halt_op;
  SafepointSynchronize::begin();

  if (VerifyBeforeExit) {
    HandleMark hm(VMThread::vm_thread());
    // Among other things, this ensures that Eden top is correct.
    Universe::heap()->prepare_for_verify();
    // Silent verification so as not to pollute normal output,
    // unless we really asked for it.
    Universe::verify();
  }

  CompileBroker::set_should_block();

  // wait for threads (compiler threads or daemon threads) in the
  // _thread_in_native state to block.
  VM_Exit::wait_for_threads_in_native_to_block();

  // signal other threads that VM process is gone
  {
    // Note: we must have the _no_safepoint_check_flag. Mutex::lock() allows
    // VM thread to enter any lock at Safepoint as long as its _owner is NULL.
    // If that happens after _terminate_lock->wait() has unset _owner
    // but before it actually drops the lock and waits, the notification below
    // may get lost and we will have a hang. To avoid this, we need to use
    // Mutex::lock_without_safepoint_check().
    MonitorLocker ml(_terminate_lock, Mutex::_no_safepoint_check_flag);
    _terminated = true;
    ml.notify();
  }

  // We are now racing with the VM termination being carried out in
  // another thread, so we don't "delete this". Numerous threads don't
  // get deleted when the VM terminates

}


// Notify the VMThread that the last non-daemon JavaThread has terminated,
// and wait until operation is performed.
void VMThread::wait_for_vm_thread_exit() {
  assert(Thread::current()->is_Java_thread(), "Should be a JavaThread");
  assert(((JavaThread*)Thread::current())->is_terminated(), "Should be terminated");
  { MonitorLocker mu(VMOperationQueue_lock, Mutex::_no_safepoint_check_flag);
    _should_terminate = true;
    mu.notify();
  }

  // Note: VM thread leaves at Safepoint. We are not stopped by Safepoint
  // because this thread has been removed from the threads list. But anything
  // that could get blocked by Safepoint should not be used after this point,
  // otherwise we will hang, since there is no one can end the safepoint.

  // Wait until VM thread is terminated
  // Note: it should be OK to use Terminator_lock here. But this is called
  // at a very delicate time (VM shutdown) and we are operating in non- VM
  // thread at Safepoint. It's safer to not share lock with other threads.
  { MonitorLocker ml(_terminate_lock, Mutex::_no_safepoint_check_flag);
    while(!VMThread::is_terminated()) {
      ml.wait();
    }
  }
}

static void post_vm_operation_event(EventExecuteVMOperation* event, VM_Operation* op) {
  assert(event != NULL, "invariant");
  assert(event->should_commit(), "invariant");
  assert(op != NULL, "invariant");
  const bool evaluate_at_safepoint = op->evaluate_at_safepoint();
  event->set_operation(op->type());
  event->set_safepoint(evaluate_at_safepoint);
  event->set_blocking(true);
  event->set_caller(JFR_THREAD_ID(op->calling_thread()));
  event->set_safepointId(evaluate_at_safepoint ? SafepointSynchronize::safepoint_id() : 0);
  event->commit();
}

void VMThread::evaluate_operation(VM_Operation* op) {
  ResourceMark rm;

  {
    PerfTraceTime vm_op_timer(perf_accumulated_vm_operation_time());
    HOTSPOT_VMOPS_BEGIN(
                     (char *) op->name(), strlen(op->name()),
                     op->evaluate_at_safepoint() ? 0 : 1);

    EventExecuteVMOperation event;
    op->evaluate();
    if (event.should_commit()) {
      post_vm_operation_event(&event, op);
    }

    HOTSPOT_VMOPS_END(
                     (char *) op->name(), strlen(op->name()),
                     op->evaluate_at_safepoint() ? 0 : 1);
  }

  // Mark as completed
  op->calling_thread()->increment_vm_operation_completed_count();
}

static VM_None    safepointALot_op("SafepointALot");
static VM_Cleanup cleanup_op;

class HandshakeALotClosure : public HandshakeClosure {
 public:
  HandshakeALotClosure() : HandshakeClosure("HandshakeALot") {}
  void do_thread(Thread* thread) {
#ifdef ASSERT
    assert(thread->is_Java_thread(), "must be");
    JavaThread* jt = (JavaThread*)thread;
    jt->verify_states_for_handshake();
#endif
  }
};

VM_Operation* VMThread::no_op_safepoint() {
  // Check for handshakes first since we may need to return a VMop.
  if (HandshakeALot) {
    HandshakeALotClosure hal_cl;
    Handshake::execute(&hal_cl);
  }
  // Check for a cleanup before SafepointALot to keep stats correct.
  long interval_ms = SafepointTracing::time_since_last_safepoint_ms();
  bool max_time_exceeded = GuaranteedSafepointInterval != 0 &&
                           (interval_ms >= GuaranteedSafepointInterval);
  if (max_time_exceeded && SafepointSynchronize::is_cleanup_needed()) {
    return &cleanup_op;
  }
  if (SafepointALot) {
    return &safepointALot_op;
  }
  // Nothing to be done.
  return NULL;
}

void VMThread::loop() {
  assert(_cur_vm_operation == NULL, "no current one should be executing");

  SafepointSynchronize::init(_vm_thread);

  while(true) {
    VM_Operation* safepoint_ops = NULL;
    //
    // Wait for VM operation
    //
    // use no_safepoint_check to get lock without attempting to "sneak"
    { MonitorLocker mu_queue(VMOperationQueue_lock,
                             Mutex::_no_safepoint_check_flag);

      // Look for new operation
      assert(_cur_vm_operation == NULL, "no current one should be executing");
      _cur_vm_operation = _vm_queue->remove_next();

      // Stall time tracking code
      if (PrintVMQWaitTime && _cur_vm_operation != NULL) {
        jlong stall = nanos_to_millis(os::javaTimeNanos() - _cur_vm_operation->timestamp());
        if (stall > 0)
          tty->print_cr("%s stall: " JLONG_FORMAT,  _cur_vm_operation->name(), stall);
      }

      while (!should_terminate() && _cur_vm_operation == NULL) {
        // wait with a timeout to guarantee safepoints at regular intervals
        // (if there is cleanup work to do)
        (void)mu_queue.wait(GuaranteedSafepointInterval);

        // Support for self destruction
        if ((SelfDestructTimer != 0) && !VMError::is_error_reported() &&
            (os::elapsedTime() > (double)SelfDestructTimer * 60.0)) {
          tty->print_cr("VM self-destructed");
          exit(-1);
        }

        // If the queue contains a safepoint VM op,
        // clean up will be done so we can skip this part.
        if (!_vm_queue->peek_at_safepoint_priority()) {

          // Have to unlock VMOperationQueue_lock just in case no_op_safepoint()
          // has to do a handshake when HandshakeALot is enabled.
          MutexUnlocker mul(VMOperationQueue_lock, Mutex::_no_safepoint_check_flag);
          if ((_cur_vm_operation = VMThread::no_op_safepoint()) != NULL) {
            // Force a safepoint since we have not had one for at least
            // 'GuaranteedSafepointInterval' milliseconds and we need to clean
            // something. This will run all the clean-up processing that needs
            // to be done at a safepoint.
            SafepointSynchronize::begin();
            #ifdef ASSERT
            if (GCALotAtAllSafepoints) InterfaceSupport::check_gc_alot();
            #endif
            SafepointSynchronize::end();
            _cur_vm_operation = NULL;
          }
        }
        _cur_vm_operation = _vm_queue->remove_next();

        // If we are at a safepoint we will evaluate all the operations that
        // follow that also require a safepoint
        if (_cur_vm_operation != NULL &&
            _cur_vm_operation->evaluate_at_safepoint()) {
          safepoint_ops = _vm_queue->drain_at_safepoint_priority();
        }
      }

      if (should_terminate()) break;
    } // Release mu_queue_lock

    //
    // Execute VM operation
    //
    { HandleMark hm(VMThread::vm_thread());

      EventMark em("Executing VM operation: %s", vm_operation()->name());
      assert(_cur_vm_operation != NULL, "we should have found an operation to execute");

      // If we are at a safepoint we will evaluate all the operations that
      // follow that also require a safepoint
      if (_cur_vm_operation->evaluate_at_safepoint()) {
        log_debug(vmthread)("Evaluating safepoint VM operation: %s", _cur_vm_operation->name());

        SafepointSynchronize::begin();

        if (_timeout_task != NULL) {
          _timeout_task->arm();
        }

        evaluate_operation(_cur_vm_operation);
        // now process all queued safepoint ops, iteratively draining
        // the queue until there are none left
        do {
          _cur_vm_operation = safepoint_ops;
          if (_cur_vm_operation != NULL) {
            do {
              EventMark em("Executing coalesced safepoint VM operation: %s", _cur_vm_operation->name());
              log_debug(vmthread)("Evaluating coalesced safepoint VM operation: %s", _cur_vm_operation->name());
              // evaluate_operation deletes the op object so we have
              // to grab the next op now
              VM_Operation* next = _cur_vm_operation->next();
              evaluate_operation(_cur_vm_operation);
              _cur_vm_operation = next;
              _coalesced_count++;
            } while (_cur_vm_operation != NULL);
          }
          // There is a chance that a thread enqueued a safepoint op
          // since we released the op-queue lock and initiated the safepoint.
          // So we drain the queue again if there is anything there, as an
          // optimization to try and reduce the number of safepoints.
          // As the safepoint synchronizes us with JavaThreads we will see
          // any enqueue made by a JavaThread, but the peek will not
          // necessarily detect a concurrent enqueue by a GC thread, but
          // that simply means the op will wait for the next major cycle of the
          // VMThread - just as it would if the GC thread lost the race for
          // the lock.
          if (_vm_queue->peek_at_safepoint_priority()) {
            // must hold lock while draining queue
            MutexLocker mu_queue(VMOperationQueue_lock,
                                 Mutex::_no_safepoint_check_flag);
            safepoint_ops = _vm_queue->drain_at_safepoint_priority();
          } else {
            safepoint_ops = NULL;
          }
        } while(safepoint_ops != NULL);

        if (_timeout_task != NULL) {
          _timeout_task->disarm();
        }

        // Complete safepoint synchronization
        SafepointSynchronize::end();

      } else {  // not a safepoint operation
        log_debug(vmthread)("Evaluating non-safepoint VM operation: %s", _cur_vm_operation->name());
        if (TraceLongCompiles) {
          elapsedTimer t;
          t.start();
          evaluate_operation(_cur_vm_operation);
          t.stop();
          double secs = t.seconds();
          if (secs * 1e3 > LongCompileThreshold) {
            // XXX - _cur_vm_operation should not be accessed after
            // the completed count has been incremented; the waiting
            // thread may have already freed this memory.
            tty->print_cr("vm %s: %3.7f secs]", _cur_vm_operation->name(), secs);
          }
        } else {
          evaluate_operation(_cur_vm_operation);
        }

        _cur_vm_operation = NULL;
      }
    }

    //
    //  Notify (potential) waiting Java thread(s)
    { MonitorLocker mu(VMOperationRequest_lock, Mutex::_no_safepoint_check_flag);
      mu.notify_all();
    }
  }
}

// A SkipGCALot object is used to elide the usual effect of gc-a-lot
// over a section of execution by a thread. Currently, it's used only to
// prevent re-entrant calls to GC.
class SkipGCALot : public StackObj {
  private:
   bool _saved;
   Thread* _t;

  public:
#ifdef ASSERT
    SkipGCALot(Thread* t) : _t(t) {
      _saved = _t->skip_gcalot();
      _t->set_skip_gcalot(true);
    }

    ~SkipGCALot() {
      assert(_t->skip_gcalot(), "Save-restore protocol invariant");
      _t->set_skip_gcalot(_saved);
    }
#else
    SkipGCALot(Thread* t) { }
    ~SkipGCALot() { }
#endif
};

void VMThread::execute(VM_Operation* op) {
  Thread* t = Thread::current();

  if (!t->is_VM_thread()) {
    SkipGCALot sgcalot(t);    // avoid re-entrant attempts to gc-a-lot
    // JavaThread or WatcherThread
    t->check_for_valid_safepoint_state();

    // New request from Java thread, evaluate prologue
    if (!op->doit_prologue()) {
      return;   // op was cancelled
    }

    // Setup VM_operations for execution
    op->set_calling_thread(t);

    // Get ticket number for the VM operation
    int ticket = t->vm_operation_ticket();

    // Add VM operation to list of waiting threads. We are guaranteed not to block while holding the
    // VMOperationQueue_lock, so we can block without a safepoint check. This allows vm operation requests
    // to be queued up during a safepoint synchronization.
    {
      MonitorLocker ml(VMOperationQueue_lock, Mutex::_no_safepoint_check_flag);
      log_debug(vmthread)("Adding VM operation: %s", op->name());
      _vm_queue->add(op);
      op->set_timestamp(os::javaTimeNanos());
      ml.notify();
    }
    {
      // Wait for completion of request
      // Note: only a JavaThread triggers the safepoint check when locking
      MonitorLocker ml(VMOperationRequest_lock,
                       t->is_Java_thread() ? Mutex::_safepoint_check_flag : Mutex::_no_safepoint_check_flag);
      while(t->vm_operation_completed_count() < ticket) {
        ml.wait();
      }
    }
    op->doit_epilogue();
  } else {
    // invoked by VM thread; usually nested VM operation
    assert(t->is_VM_thread(), "must be a VM thread");
    VM_Operation* prev_vm_operation = vm_operation();
    if (prev_vm_operation != NULL) {
      // Check the VM operation allows nested VM operation. This normally not the case, e.g., the compiler
      // does not allow nested scavenges or compiles.
      if (!prev_vm_operation->allow_nested_vm_operations()) {
        fatal("Nested VM operation %s requested by operation %s",
              op->name(), vm_operation()->name());
      }
      op->set_calling_thread(prev_vm_operation->calling_thread());
    }

    EventMark em("Executing %s VM operation: %s", prev_vm_operation ? "nested" : "", op->name());

    // Release all internal handles after operation is evaluated
    HandleMark hm(t);
    _cur_vm_operation = op;

    if (op->evaluate_at_safepoint() && !SafepointSynchronize::is_at_safepoint()) {
      SafepointSynchronize::begin();
      op->evaluate();
      SafepointSynchronize::end();
    } else {
      op->evaluate();
    }

    _cur_vm_operation = prev_vm_operation;
  }
}

void VMThread::verify() {
  oops_do(&VerifyOopClosure::verify_oop, NULL);
}
