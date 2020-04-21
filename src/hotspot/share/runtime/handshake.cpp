/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handshake.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/osThread.hpp"
#include "runtime/semaphore.inline.hpp"
#include "runtime/task.hpp"
#include "runtime/thread.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/preserveException.hpp"

class HandshakeOperation: public StackObj {
public:
  virtual void do_handshake(JavaThread* thread) = 0;
};

class HandshakeThreadsOperation: public HandshakeOperation {
  static Semaphore _done;
  HandshakeClosure* _handshake_cl;
  bool _executed;
public:
  HandshakeThreadsOperation(HandshakeClosure* cl) : _handshake_cl(cl), _executed(false) {}
  void do_handshake(JavaThread* thread);
  bool thread_has_completed() { return _done.trywait(); }
  bool executed() const { return _executed; }
  const char* name() { return _handshake_cl->name(); }

#ifdef ASSERT
  void check_state() {
    assert(!_done.trywait(), "Must be zero");
  }
#endif
};

Semaphore HandshakeThreadsOperation::_done(0);

class VM_Handshake: public VM_Operation {
  const jlong _handshake_timeout;
 public:
  bool evaluate_at_safepoint() const { return false; }

 protected:
  HandshakeThreadsOperation* const _op;

  VM_Handshake(HandshakeThreadsOperation* op) :
      _handshake_timeout(TimeHelper::millis_to_counter(HandshakeTimeout)), _op(op) {}

  void set_handshake(JavaThread* target) {
    target->set_handshake_operation(_op);
  }

  // This method returns true for threads completed their operation
  // and true for threads canceled their operation.
  // A cancellation can happen if the thread is exiting.
  bool poll_for_completed_thread() { return _op->thread_has_completed(); }

  bool handshake_has_timed_out(jlong start_time);
  static void handle_timeout();
};

bool VM_Handshake::handshake_has_timed_out(jlong start_time) {
  // Check if handshake operation has timed out
  if (_handshake_timeout > 0) {
    return os::elapsed_counter() >= (start_time + _handshake_timeout);
  }
  return false;
}

void VM_Handshake::handle_timeout() {
  LogStreamHandle(Warning, handshake) log_stream;
  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *thr = jtiwh.next(); ) {
    if (thr->has_handshake()) {
      log_stream.print("Thread " PTR_FORMAT " has not cleared its handshake op", p2i(thr));
      thr->print_thread_state_on(&log_stream);
    }
  }
  log_stream.flush();
  fatal("Handshake operation timed out");
}

static void log_handshake_info(jlong start_time_ns, const char* name, int targets, int vmt_executed, const char* extra = NULL) {
  if (start_time_ns != 0) {
    jlong completion_time = os::javaTimeNanos() - start_time_ns;
    log_info(handshake)("Handshake \"%s\", Targeted threads: %d, Executed by targeted threads: %d, Total completion time: " JLONG_FORMAT " ns%s%s",
                        name, targets,
                        targets - vmt_executed,
                        completion_time,
                        extra != NULL ? ", " : "",
                        extra != NULL ? extra : "");
  }
}

class VM_HandshakeOneThread: public VM_Handshake {
  JavaThread* _target;
 public:
  VM_HandshakeOneThread(HandshakeThreadsOperation* op, JavaThread* target) :
    VM_Handshake(op), _target(target) {}

  void doit() {
    DEBUG_ONLY(_op->check_state();)

    jlong start_time_ns = 0;
    if (log_is_enabled(Info, handshake)) {
      start_time_ns = os::javaTimeNanos();
    }

    ThreadsListHandle tlh;
    if (tlh.includes(_target)) {
      set_handshake(_target);
    } else {
      log_handshake_info(start_time_ns, _op->name(), 0, 0, "(thread dead)");
      return;
    }

    log_trace(handshake)("JavaThread " INTPTR_FORMAT " signaled, begin attempt to process by VMThtread", p2i(_target));
    jlong timeout_start_time = os::elapsed_counter();
    bool by_vm_thread = false;
    do {
      if (handshake_has_timed_out(timeout_start_time)) {
        handle_timeout();
      }
      by_vm_thread = _target->handshake_try_process_by_vmThread();
    } while (!poll_for_completed_thread());
    DEBUG_ONLY(_op->check_state();)
    log_handshake_info(start_time_ns, _op->name(), 1, by_vm_thread ? 1 : 0);
  }

  VMOp_Type type() const { return VMOp_HandshakeOneThread; }

  bool executed() const { return _op->executed(); }
};

class VM_HandshakeAllThreads: public VM_Handshake {
 public:
  VM_HandshakeAllThreads(HandshakeThreadsOperation* op) : VM_Handshake(op) {}

  void doit() {
    DEBUG_ONLY(_op->check_state();)

    jlong start_time_ns = 0;
    if (log_is_enabled(Info, handshake)) {
      start_time_ns = os::javaTimeNanos();
    }
    int handshake_executed_by_vm_thread = 0;

    JavaThreadIteratorWithHandle jtiwh;
    int number_of_threads_issued = 0;
    for (JavaThread *thr = jtiwh.next(); thr != NULL; thr = jtiwh.next()) {
      set_handshake(thr);
      number_of_threads_issued++;
    }

    if (number_of_threads_issued < 1) {
      log_handshake_info(start_time_ns, _op->name(), 0, 0);
      return;
    }

    log_trace(handshake)("Threads signaled, begin processing blocked threads by VMThread");
    const jlong start_time = os::elapsed_counter();
    int number_of_threads_completed = 0;
    do {
      // Check if handshake operation has timed out
      if (handshake_has_timed_out(start_time)) {
        handle_timeout();
      }

      // Have VM thread perform the handshake operation for blocked threads.
      // Observing a blocked state may of course be transient but the processing is guarded
      // by semaphores and we optimistically begin by working on the blocked threads
      jtiwh.rewind();
      for (JavaThread *thr = jtiwh.next(); thr != NULL; thr = jtiwh.next()) {
        // A new thread on the ThreadsList will not have an operation,
        // hence it is skipped in handshake_process_by_vmthread.
        if (thr->handshake_try_process_by_vmThread()) {
          handshake_executed_by_vm_thread++;
        }
      }
      while (poll_for_completed_thread()) {
        // Includes canceled operations by exiting threads.
        number_of_threads_completed++;
      }

    } while (number_of_threads_issued > number_of_threads_completed);
    assert(number_of_threads_issued == number_of_threads_completed, "Must be the same");
    DEBUG_ONLY(_op->check_state();)

    log_handshake_info(start_time_ns, _op->name(), number_of_threads_issued, handshake_executed_by_vm_thread);
  }

  VMOp_Type type() const { return VMOp_HandshakeAllThreads; }
};

void HandshakeThreadsOperation::do_handshake(JavaThread* thread) {
  jlong start_time_ns = 0;
  if (log_is_enabled(Debug, handshake, task)) {
    start_time_ns = os::javaTimeNanos();
  }

  // Only actually execute the operation for non terminated threads.
  if (!thread->is_terminated()) {
    _handshake_cl->do_thread(thread);
    _executed = true;
  }

  if (start_time_ns != 0) {
    jlong completion_time = os::javaTimeNanos() - start_time_ns;
    log_debug(handshake, task)("Operation: %s for thread " PTR_FORMAT ", is_vm_thread: %s, completed in " JLONG_FORMAT " ns",
                               name(), p2i(thread), BOOL_TO_STR(Thread::current()->is_VM_thread()), completion_time);
  }

  // Use the semaphore to inform the VM thread that we have completed the operation
  _done.signal();

  // It is no longer safe to refer to 'this' as the VMThread may have destroyed this operation
}

void Handshake::execute(HandshakeClosure* thread_cl) {
  HandshakeThreadsOperation cto(thread_cl);
  VM_HandshakeAllThreads handshake(&cto);
  VMThread::execute(&handshake);
}

bool Handshake::execute(HandshakeClosure* thread_cl, JavaThread* target) {
  HandshakeThreadsOperation cto(thread_cl);
  VM_HandshakeOneThread handshake(&cto, target);
  VMThread::execute(&handshake);
  return handshake.executed();
}

HandshakeState::HandshakeState() : _operation(NULL), _semaphore(1), _thread_in_process_handshake(false) {
  DEBUG_ONLY(_vmthread_processing_handshake = false;)
}

void HandshakeState::set_operation(JavaThread* target, HandshakeOperation* op) {
  _operation = op;
  SafepointMechanism::arm_local_poll_release(target);
}

void HandshakeState::clear_handshake(JavaThread* target) {
  _operation = NULL;
  SafepointMechanism::disarm_if_needed(target, true /* release */);
}

void HandshakeState::process_self_inner(JavaThread* thread) {
  assert(Thread::current() == thread, "should call from thread");
  assert(!thread->is_terminated(), "should not be a terminated thread");
  assert(thread->thread_state() != _thread_blocked, "should not be in a blocked state");
  assert(thread->thread_state() != _thread_in_native, "should not be in native");

  do {
    ThreadInVMForHandshake tivm(thread);
    if (!_semaphore.trywait()) {
      _semaphore.wait_with_safepoint_check(thread);
    }
    HandshakeOperation* op = Atomic::load_acquire(&_operation);
    if (op != NULL) {
      HandleMark hm(thread);
      CautiouslyPreserveExceptionMark pem(thread);
      // Disarm before execute the operation
      clear_handshake(thread);
      op->do_handshake(thread);
    }
    _semaphore.signal();
  } while (has_operation());
}

bool HandshakeState::vmthread_can_process_handshake(JavaThread* target) {
  // handshake_safe may only be called with polls armed.
  // VM thread controls this by first claiming the handshake via claim_handshake_for_vmthread.
  return SafepointSynchronize::handshake_safe(target);
}

static bool possibly_vmthread_can_process_handshake(JavaThread* target) {
  // Note that this method is allowed to produce false positives.
  if (target->is_ext_suspended()) {
    return true;
  }
  if (target->is_terminated()) {
    return true;
  }
  switch (target->thread_state()) {
  case _thread_in_native:
    // native threads are safe if they have no java stack or have walkable stack
    return !target->has_last_Java_frame() || target->frame_anchor()->walkable();

  case _thread_blocked:
    return true;

  default:
    return false;
  }
}

bool HandshakeState::claim_handshake_for_vmthread() {
  if (!_semaphore.trywait()) {
    return false;
  }
  if (has_operation()) {
    return true;
  }
  _semaphore.signal();
  return false;
}

bool HandshakeState::try_process_by_vmThread(JavaThread* target) {
  assert(Thread::current()->is_VM_thread(), "should call from vm thread");

  if (!has_operation()) {
    // JT has already cleared its handshake
    return false;
  }

  if (!possibly_vmthread_can_process_handshake(target)) {
    // JT is observed in an unsafe state, it must notice the handshake itself
    return false;
  }

  // Claim the semaphore if there still an operation to be executed.
  if (!claim_handshake_for_vmthread()) {
    return false;
  }

  // If we own the semaphore at this point and while owning the semaphore
  // can observe a safe state the thread cannot possibly continue without
  // getting caught by the semaphore.
  bool executed = false;
  if (vmthread_can_process_handshake(target)) {
    guarantee(!_semaphore.trywait(), "we should already own the semaphore");
    log_trace(handshake)("Processing handshake by VMThtread");
    DEBUG_ONLY(_vmthread_processing_handshake = true;)
    _operation->do_handshake(target);
    DEBUG_ONLY(_vmthread_processing_handshake = false;)
    // Disarm after VM thread have executed the operation.
    clear_handshake(target);
    executed = true;
  }

  // Release the thread
  _semaphore.signal();

  return executed;
}
