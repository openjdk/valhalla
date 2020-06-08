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

#ifndef SHARE_RUNTIME_VMTHREAD_HPP
#define SHARE_RUNTIME_VMTHREAD_HPP

#include "runtime/perfData.hpp"
#include "runtime/thread.hpp"
#include "runtime/task.hpp"
#include "runtime/vmOperations.hpp"

class VM_QueueHead : public VM_None {
 public:
  VM_QueueHead() : VM_None("QueueHead") {}
};

//
// Prioritized queue of VM operations.
//
// Encapsulates both queue management and
// and priority policy
//
class VMOperationQueue : public CHeapObj<mtInternal> {
 private:
  enum Priorities {
     SafepointPriority, // Highest priority (operation executed at a safepoint)
     MediumPriority,    // Medium priority
     nof_priorities
  };

  // We maintain a doubled linked list, with explicit count.
  int           _queue_length[nof_priorities];
  int           _queue_counter;
  VM_Operation* _queue       [nof_priorities];

  static VM_QueueHead _queue_head[nof_priorities];

  // Double-linked non-empty list insert.
  void insert(VM_Operation* q,VM_Operation* n);
  void unlink(VM_Operation* q);

  // Basic queue manipulation
  bool queue_empty                (int prio);
  void queue_add                  (int prio, VM_Operation *op);
  VM_Operation* queue_remove_front(int prio);
  VM_Operation* queue_drain(int prio);
  // lock-free query: may return the wrong answer but must not break
  bool queue_peek(int prio) { return _queue_length[prio] > 0; }

 public:
  VMOperationQueue();

  // Highlevel operations. Encapsulates policy
  void add(VM_Operation *op);
  VM_Operation* remove_next();                        // Returns next or null
  VM_Operation* drain_at_safepoint_priority() { return queue_drain(SafepointPriority); }
  bool peek_at_safepoint_priority() { return queue_peek(SafepointPriority); }
};


// VM operation timeout handling: warn or abort the VM when VM operation takes
// too long. Periodic tasks do not participate in safepoint protocol, and therefore
// can fire when application threads are stopped.

class VMOperationTimeoutTask : public PeriodicTask {
private:
  volatile int _armed;
  jlong _arm_time;

public:
  VMOperationTimeoutTask(size_t interval_time) :
          PeriodicTask(interval_time), _armed(0), _arm_time(0) {}

  virtual void task();

  bool is_armed();
  void arm();
  void disarm();
};

//
// A single VMThread (the primordial thread) spawns all other threads
// and is itself used by other threads to offload heavy vm operations
// like scavenge, garbage_collect etc.
//

class VMThread: public NamedThread {
 private:
  static ThreadPriority _current_priority;

  static bool _should_terminate;
  static bool _terminated;
  static Monitor * _terminate_lock;
  static PerfCounter* _perf_accumulated_vm_operation_time;
  static uint64_t _coalesced_count;

  static VMOperationTimeoutTask* _timeout_task;

  static VM_Operation* no_op_safepoint();

  void evaluate_operation(VM_Operation* op);

 public:
  // Constructor
  VMThread();

  // No destruction allowed
  ~VMThread() {
    guarantee(false, "VMThread deletion must fix the race with VM termination");
  }


  // Tester
  bool is_VM_thread() const                      { return true; }

  // The ever running loop for the VMThread
  void loop();

  // Called to stop the VM thread
  static void wait_for_vm_thread_exit();
  static bool should_terminate()                  { return _should_terminate; }
  static bool is_terminated()                     { return _terminated == true; }

  // Execution of vm operation
  static void execute(VM_Operation* op);

  // Returns the current vm operation if any.
  static VM_Operation* vm_operation()             { return _cur_vm_operation; }
  static VM_Operation::VMOp_Type vm_op_type()     { return _cur_vm_operation->type(); }
  static uint64_t get_coalesced_count()           { return _coalesced_count; }

  // Returns the single instance of VMThread.
  static VMThread* vm_thread()                    { return _vm_thread; }

  void verify();

  // Performance measurement
  static PerfCounter* perf_accumulated_vm_operation_time()               { return _perf_accumulated_vm_operation_time; }

  // Entry for starting vm thread
  virtual void run();

  // Creations/Destructions
  static void create();
  static void destroy();

 private:
  // VM_Operation support
  static VM_Operation*     _cur_vm_operation;   // Current VM operation
  static VMOperationQueue* _vm_queue;           // Queue (w/ policy) of VM operations

  // Pointer to single-instance of VM thread
  static VMThread*     _vm_thread;
};

#endif // SHARE_RUNTIME_VMTHREAD_HPP
