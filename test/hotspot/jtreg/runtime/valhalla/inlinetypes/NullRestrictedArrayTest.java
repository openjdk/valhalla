/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 */



import jdk.test.lib.Asserts;
import java.lang.reflect.Method;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;


/*
 * @test
 * @summary Test of VM.newNullRestrictedArray API
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED NullRestrictedArrayTest.java
 * @run main/othervm --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED -XX:+EnableValhalla NullRestrictedArrayTest
 */


public class NullRestrictedArrayTest {

  private static final Unsafe UNSAFE = Unsafe.getUnsafe();


  public static void main(String[] args) {
      NullRestrictedArrayTest tests = new NullRestrictedArrayTest();
      Class c = tests.getClass();
      for (Method m : c.getDeclaredMethods()) {
        if (m.getName().startsWith("test_")) {
          try {
            System.out.println("Running " + m.getName());
            m.invoke(tests);
          } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
          }
        }
      }
  }

  // Test illegal attempt to create a null restricted array with an identity class
  public void test_0() {
      Throwable exception = null;
      try {
        VM.newNullRestrictedArray(String.class, 4);
      } catch (IllegalArgumentException e) {
        System.out.println("Received: " + e);
        exception = e;
      }
      Asserts.assertNotNull(exception, "Expected IllegalArgumentException not received");
  }

  // Test illegal array length
  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class ValueClass1 {
    int i = 0;
    int j = 0;
  }

  public void test_1() {
      Throwable exception = null;
      try {
        VM.newNullRestrictedArray(ValueClass1.class, -1);
      } catch (IllegalArgumentException e) {
        System.out.println("Received: " + e);
        exception = e;
      }
      Asserts.assertNotNull(exception, "Expected IllegalArgumentException not received");
  }

  // Test illegal attempt to create a null restricted array with a value class not annotated with @ImplicitlyConstructible
  static value class ValueClass2 {
    int i = 0;
    int j = 0;
  }

  public void test_2() {
      Throwable exception = null;
      try {
        VM.newNullRestrictedArray(ValueClass2.class, 8);
      } catch (IllegalArgumentException e) {
        System.out.println("Received: " + e);
        exception = e;
      }
      Asserts.assertNotNull(exception, "Expected IllegalArgumentException not received");
  }

  // Test valid creation of a flat array
  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class ValueClass3 {
    int i = 0;
    int j = 0;
  }

  public void test_3() {
      Throwable exception = null;
      try {
        Object array = VM.newNullRestrictedArray(ValueClass3.class, 8);
        Asserts.assertTrue(UNSAFE.isFlattenedArray(array.getClass()), "Expecting flat array but array is not flat");
      } catch (Throwable e) {
        System.out.println("Received: " + e);
        exception = e;
      }
      Asserts.assertNull(exception, "Unexpected exception: " + exception);
  }

  // Test that elements are not null
  @ImplicitlyConstructible
  @LooselyConsistentValue

  static value class ValueClass4 {
    int i = 0;
    int j = 0;
  }

  public void test_4() {
      Throwable exception = null;
      try {
        Object[] array = VM.newNullRestrictedArray(ValueClass4.class, 8);
        Asserts.assertNotNull(array[1], "Expecting non null element but null found instead");
      } catch (Throwable e) {
        System.out.println("Received: " + e);
        exception = e;
      }
      Asserts.assertNull(exception, "Unexpected exception: " + exception);
  }

  // Test that writing null to a null restricted array throws an exception
  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class ValueClass5 {
    int i = 0;
    int j = 0;
  }

  public void test_5() {
      Throwable exception = null;
      try {
        Object[] array = VM.newNullRestrictedArray(ValueClass4.class, 8);
        array[1] = null;
      } catch (NullPointerException e) {
        System.out.println("Received: " + e);
        exception = e;
      }
      Asserts.assertNotNull(exception, "Expected NullPointerException not received");
  }

}