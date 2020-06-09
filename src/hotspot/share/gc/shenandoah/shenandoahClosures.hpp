/*
 * Copyright (c) 2019, 2020, Red Hat, Inc. All rights reserved.
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
#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHCLOSURES_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHCLOSURES_HPP

#include "memory/iterator.hpp"
#include "oops/accessDecorators.hpp"
#include "runtime/handshake.hpp"

class ShenandoahHeap;
class ShenandoahMarkingContext;
class ShenandoahHeapRegionSet;
class Thread;

class ShenandoahForwardedIsAliveClosure: public BoolObjectClosure {
private:
  ShenandoahMarkingContext* const _mark_context;
public:
  inline ShenandoahForwardedIsAliveClosure();
  inline bool do_object_b(oop obj);
};

class ShenandoahIsAliveClosure: public BoolObjectClosure {
private:
  ShenandoahMarkingContext* const _mark_context;
public:
  inline ShenandoahIsAliveClosure();
  inline bool do_object_b(oop obj);
};

class ShenandoahIsAliveSelector : public StackObj {
private:
  ShenandoahIsAliveClosure _alive_cl;
  ShenandoahForwardedIsAliveClosure _fwd_alive_cl;
public:
  inline BoolObjectClosure* is_alive_closure();
};

class ShenandoahUpdateRefsClosure: public OopClosure {
private:
  ShenandoahHeap* _heap;
public:
  inline ShenandoahUpdateRefsClosure();
  inline void do_oop(oop* p);
  inline void do_oop(narrowOop* p);
private:
  template <class T>
  inline void do_oop_work(T* p);
};

template <DecoratorSet MO = MO_UNORDERED>
class ShenandoahEvacuateUpdateRootsClosure: public BasicOopIterateClosure {
private:
  ShenandoahHeap* _heap;
  Thread* _thread;
public:
  inline ShenandoahEvacuateUpdateRootsClosure();
  inline void do_oop(oop* p);
  inline void do_oop(narrowOop* p);

private:
  template <class T>
  inline void do_oop_work(T* p);
};

class ShenandoahEvacUpdateOopStorageRootsClosure : public BasicOopIterateClosure {
private:
  ShenandoahHeap* _heap;
  Thread* _thread;
public:
  inline ShenandoahEvacUpdateOopStorageRootsClosure();
  inline void do_oop(oop* p);
  inline void do_oop(narrowOop* p);
};

class ShenandoahCodeBlobAndDisarmClosure: public CodeBlobToOopClosure {
private:
  BarrierSetNMethod* const _bs;

public:
  inline ShenandoahCodeBlobAndDisarmClosure(OopClosure* cl);
  inline void do_code_blob(CodeBlob* cb);
};

class ShenandoahRendezvousClosure : public HandshakeClosure {
public:
  inline ShenandoahRendezvousClosure();
  inline void do_thread(Thread* thread);
};

#ifdef ASSERT
class ShenandoahAssertNotForwardedClosure : public OopClosure {
private:
  template <class T>
  inline void do_oop_work(T* p);

public:
  inline void do_oop(narrowOop* p);
  inline void do_oop(oop* p);
};
#endif // ASSERT

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHCLOSURES_HPP
