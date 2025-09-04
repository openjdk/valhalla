/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.classfile.*;
import java.lang.constant.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.lang.reflect.InvocationTargetException;

/*
 * @test
 * @summary Invokes eight threads that concurrently have to resolve the same
            set of classes, thereby putting stress on the classloader and
            deadlocks will be noticed. This execution is iterated many times.
 * @enablePreview
 * @run main/othervm ConcurrentClassLoadingTest
 */


public class ConcurrentClassLoadingTest {

  private static final int N_ITER = 1_000;

  // Should crash the VM if it fails/deadlocks.
  public static void main(String[] args) {
    for (int i = 1; i <= N_ITER; i++) {
      if ((i % 100) == 0) {
        System.out.println("Attempt " + i);
      }
      doTest(4, 4); // Four threads on each worker.
    }
  }

  public static void doTest(int nA, int nB) {
    int n = nA + nB;
    // Use a barrier to ensure all threads reach a certain point before calling
    // the method that defines the class (which internally calls native code).
    final CyclicBarrier barrier = new CyclicBarrier(n);
    // Every iteration has a new instance of a class loader, to make sure we
    // create unique (Class, ClassLoader) pairs to force loading.
    CL cl = new CL();
    Thread[] threads = new Thread[n];
    // Spawn all the threads with their respective worker classes.
    for (int i = 0; i < n; i++) {
      String workerClassName = i < nA ? "TestFoo" : "TestBar";
      Thread thread = new Thread(new Worker(barrier, workerClassName, cl));
      threads[i] = thread;
      thread.start();
    }
    // Make sure that all the threads are done by the end.
    try {
      for (Thread thread : threads) {
        thread.join();
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException("test got interrupted", e);
    }
  }

  // Small abstraction around barrier logic.
  // Will also ignore the inevitable NPE that is going to be thrown.
  public static class Worker implements Runnable {
    private final CyclicBarrier barrier;
    private final String workerClassName;
    private final CL loader;

    public Worker(CyclicBarrier barrier, String workerClassName, CL loader) {
      this.barrier = barrier;
      this.workerClassName = workerClassName;
      this.loader = loader;
    }

    public void run() {
      try {
        // Wait for all threads to reach this point.
        barrier.await();
        // This will trigger classloading of two classes concurrently, which are the
        // fields of LoadFoo and LoadBar.
        Class<?> workerClass = Class.forName(workerClassName, false, loader);
        Object worker = workerClass.getDeclaredConstructor().newInstance();
      } catch(InterruptedException | BrokenBarrierException e) {
        throw new IllegalStateException("test bug: waiting for all threads saw error", e);
      } catch (ClassNotFoundException |
               NoSuchMethodException |
               IllegalAccessException |
               InstantiationException |
               InvocationTargetException e) {
        throw new IllegalStateException("test bug: setup of worker saw error", e);
      }
    }
  }

  public static final class CL extends ClassLoader {
    private static final String TEST_CLASS_NAME;

    // Get this test case class via reflection to ensure that if anything
    // gets changed it will trigger a compiler error.
    static {
      TEST_CLASS_NAME = ConcurrentClassLoadingTest.class.getSimpleName();
    }

    public CL() {
      // Needed for parallel classloading.
      if(!registerAsParallelCapable() || !isRegisteredAsParallelCapable()) {
        throw new IllegalStateException("test bug: could not register parallel classloader");
      }
    }

    public Class<?> findClass(String name)
        throws ClassNotFoundException {
      switch (name) {
      case "TestFoo":
        return constructClass(name, "B");
      case "TestBar":
        return constructClass(name, "C");
      default:
        throw new ClassNotFoundException("not one of our special classes");
      }
    }

    public Class<?> constructClass(String className, String fieldInnerClassName) {
      String fieldClassName = TEST_CLASS_NAME + "$" + fieldInnerClassName;
      // class className {
      //   public fieldClassName theField;
      // }
      byte[] bytes = ClassFile.of().build(ClassDesc.of(className),
        clb -> clb.withVersion(ClassFile.JAVA_25_VERSION, ClassFile.PREVIEW_MINOR_VERSION)
                  // Not a value class and no loadable descriptors. Supposed to represent some
                  // generic Java worker/wrapper class.
                  .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_IDENTITY)
                  // From java.base/java/lang/classfile documentation.
                  .withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, ClassFile.ACC_PUBLIC,
                    cob -> cob.aload(0)
                              .invokespecial(ConstantDescs.CD_Object,
                                             ConstantDescs.INIT_NAME, ConstantDescs.MTD_void)
                              .return_())
                  // We just want a basic field.
                  .withField("theField", ClassDesc.of(fieldClassName), ClassFile.ACC_PUBLIC)
      );
      return defineClass(className, bytes, 0, bytes.length);
    }
  }

  // Class hierarchy:
  // A is parent of B and C, C has a field B

  public static abstract value class A {
    public int x = 1;
  }

  public static value class B extends A {
    public int y = 2;
  }

  public static value class C extends A {
    public int z = 3;
    public B b;

    public C(B b) {
      this.b = b;
    }
  }

}
