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
 */

package runtime.valhalla.inlinetypes;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;



import static jdk.test.lib.Asserts.*;

/*
 * @test FlatArraysTest
 * @summary Plain array test for Inline Types
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @library /test/lib
 * @enablePreview
 * @compile --source 24 FlatArraysTest.java
 * @run main/othervm -Xint -XX:FlatArrayElementMaxSize=-1 -XX:InlineFieldMaxFlatSize=-1 -XX:+AtomicFieldFlattening -XX:+NullableFieldFlattening runtime.valhalla.inlinetypes.FlatArraysTest
 * @run main/othervm -Xint -XX:FlatArrayElementMaxSize=0 -XX:+AtomicFieldFlattening -XX:+NullableFieldFlattening runtime.valhalla.inlinetypes.FlatArraysTest
 */

// TODO 8341767 Remove -Xint

public class FlatArraysTest {
  static final int ARRAY_SIZE = 100;

  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class SmallValue {
      byte b;
      short s;

      SmallValue() { b = 0 ;  s = 0; }
      SmallValue(byte b0, short s0) { b = b0; s = s0; }

      public static Object getTestValue() { return new SmallValue(Byte.MIN_VALUE, Short.MIN_VALUE); }

      public static boolean expectingFlatNullRestrictedArray() { return true; }
      public static boolean expectingFlatNullRestrictedAtomicArray() { return true; }
      public static boolean expectingFlatNullableAtomicArray() { return true; }
  }

  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class MediumValue {
      int x;
      int y;

      MediumValue() {
         x = 0;
         y = 0;
      }
      MediumValue(int x0, int y0) {
        x = x0;
        y = y0;
      }

      public static Object getTestValue() {
        return new MediumValue(Integer.MIN_VALUE, Integer.MIN_VALUE);
      }

      public static boolean expectingFlatNullRestrictedArray() { return true; }
      public static boolean expectingFlatNullRestrictedAtomicArray() { return true; }
      public static boolean expectingFlatNullableAtomicArray() { return false; }
  }

  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class BigValue {
      long x;
      long y;
      long z;

      BigValue() {
        x = 0;
        y = 0;
        z = 0;
      }
      BigValue(long x0, long y0, long z0) {
        x = x0;
        y = y0;
        z = z0;
      }

      public static Object getTestValue() {
        return new BigValue(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
      }

      public static boolean expectingFlatNullRestrictedArray() { return true; }
      public static boolean expectingFlatNullRestrictedAtomicArray() { return false; }
      public static boolean expectingFlatNullableAtomicArray() { return false; }
  }

  static void testNullFreeArray(Object[] array, Object value) {
    testErrorCases(array);
    assertNotNull(value, "Test needs a not null value");
    //   Test 1 : check initial element value is not null
    for (int i = 0 ; i < array.length; i++) {
      assertNotNull(array[i], "Initial value must not be null");
    }
    //   Test 2 : try to write null
    for (int i = 0 ; i < array.length; i++) {
      try {
        array[i] = null;
        throw new RuntimeException("Missing NullPointerException");
      } catch (NullPointerException e) { }
    }
    //   Test 3 : overwrite initial value with new value
    for (int i = 0 ; i < array.length; i++) {
      array[i] = value;
    }
    for (int i = 0 ; i < array.length; i++) {
      assertEquals(array[i], value);
    }
  }

  static void testNullableArray(Object[] array, Object value) {
    testErrorCases(array);
    assertNotNull(value, "Test needs a not null value");
    //   Test 1 : check that initial element value is null
    System.gc();
    System.out.println("Test 1");
    for (int i = 0 ; i < array.length; i++) {
      assertNull(array[i], "Initial value should be null");
    }
    //   Test 2 : write new value to all elements
    System.gc();
    System.out.println("Test 2a");
    for (int i = 0 ; i < array.length; i++) {
      array[i] = value;
      assertEquals(array[i], value, "Value mismatch");
    }
    System.gc();
    System.out.println("Test 2b");
    for (int i = 0 ; i < array.length; i++) {
      assertEquals(array[i], value, "Value mismatch");
    }
    //   Test 3 : write null to all elements
    System.gc();
    System.out.println("Test 3a");
    for (int i = 0 ; i < array.length; i++) {
      array[i] = null;
    }
    System.gc();
    System.out.println("Test 3b");
    for (int i = 0 ; i < array.length; i++) {
      assertNull(array[i], "Value mismatch");
    }
    //   Test 4 : write alternate null / not null values
    System.gc();
    System.out.println("Test 4a");
    for (int i = 0 ; i < array.length; i++) {
      if (i%2 == 0) {
        array[i] = null;
      } else {
        array[i] = value;
      }
    }
    System.gc();
    System.out.println("Test 4b");
    for (int i = 0 ; i < array.length; i++) {
      if (i%2 == 0) {
        assertNull(array[i], "Value mismatch");
      } else {
        assertEquals(array[i], value, "Value mismatch");
      }
    }
  }

  static void testErrorCases(Object[] array) {
    try {
      Object o = array[-1];
      throw new RuntimeException("Missing IndexOutOfBoundsException");
    } catch(IndexOutOfBoundsException e) { }

    try {
      Object o = array[array.length];
      throw new RuntimeException("Missing IndexOutOfBoundsException");
    } catch(IndexOutOfBoundsException e) { }

    assertTrue(array.getClass().getComponentType() != String.class, "Must be for the test");
    assertTrue(array.length > 0, "Must be for the test");
    try {
      array[0] = new String("Bad");
      throw new RuntimeException("Missing ArrayStoreException");
    } catch (ArrayStoreException e) { }
  }

  static void testArrayCopy() {

    Object[] objArray = new Object[ARRAY_SIZE];
    for (int i = 0; i < ARRAY_SIZE; i++) {
      objArray[i] = SmallValue.getTestValue();
    }
    SmallValue[] nonAtomicArray = (SmallValue[])ValueClass.newNullRestrictedArray(SmallValue.class, ARRAY_SIZE);
    SmallValue[] atomicArray = (SmallValue[])ValueClass.newNullRestrictedAtomicArray(SmallValue.class, ARRAY_SIZE);
    SmallValue[] nullableArray = (SmallValue[])ValueClass.newNullableAtomicArray(SmallValue.class, ARRAY_SIZE);

    // obj -> non-atomic
    testArrayCopyInternal(objArray, nonAtomicArray);

    // obj -> atomic
    testArrayCopyInternal(objArray, atomicArray);

    // obj -> nullable
    testArrayCopyInternal(objArray, nullableArray);

    objArray[45] = null;
    // obj with null -> non-atomic   => NPE
    try {
      testArrayCopyInternal(objArray, nonAtomicArray);
      throw new RuntimeException("Missing NullPointerException");
    } catch (NullPointerException e) { }

    // obj with null -> atomic       => NPE
    try {
      testArrayCopyInternal(objArray, atomicArray);
      throw new RuntimeException("Missing NullPointerException");
    } catch (NullPointerException e) { }

    // obj with null -> nullable
    try {
      testArrayCopyInternal(objArray, nullableArray);
    } catch (NullPointerException e) {
      throw new RuntimeException("Unexpected NullPointerException");
    }

    objArray[45] = new String("bad");
    // obj with wrong type value -> non-atomic   => ASE
    try {
      testArrayCopyInternal(objArray, nonAtomicArray);
      throw new RuntimeException("Missing ArrayStoreException");
    } catch (ArrayStoreException e) { }

    // obj with wrong type value -> atomic       => ASE
    try {
      testArrayCopyInternal(objArray, atomicArray);
      throw new RuntimeException("Missing ArrayStoreException");
    } catch (ArrayStoreException e) { }

    // obj with wrong type value -> nullable     => ASE
    try {
      testArrayCopyInternal(objArray, nullableArray);
      throw new RuntimeException("Missing ArrayStoreException");
    } catch (ArrayStoreException e) { }

    // Reset all arrays
    objArray = new Object[ARRAY_SIZE];
    nonAtomicArray = (SmallValue[])ValueClass.newNullRestrictedArray(SmallValue.class, ARRAY_SIZE);
    atomicArray = (SmallValue[])ValueClass.newNullRestrictedAtomicArray(SmallValue.class, ARRAY_SIZE);
    nullableArray = (SmallValue[])ValueClass.newNullableAtomicArray(SmallValue.class, ARRAY_SIZE);

    // non-atomic -> obj
    testArrayCopyInternal(nonAtomicArray, objArray);

    // non-atomic -> non-atomic
    SmallValue[] nonAtomicArray2 = (SmallValue[])ValueClass.newNullRestrictedArray(SmallValue.class, ARRAY_SIZE);
    testArrayCopyInternal(nonAtomicArray, nonAtomicArray2);

    // non-atomic -> non-atomic same array
    testArrayCopyInternal(nonAtomicArray, nonAtomicArray);

    // non-atomic -> atomic
    testArrayCopyInternal(nonAtomicArray, atomicArray);

    // non-atomic -> nullable
    testArrayCopyInternal(nonAtomicArray, nullableArray);

    // Reset all arrays
    objArray = new Object[ARRAY_SIZE];
    nonAtomicArray = (SmallValue[])ValueClass.newNullRestrictedArray(SmallValue.class, ARRAY_SIZE);
    atomicArray = (SmallValue[])ValueClass.newNullRestrictedAtomicArray(SmallValue.class, ARRAY_SIZE);
    nullableArray = (SmallValue[])ValueClass.newNullableAtomicArray(SmallValue.class, ARRAY_SIZE);

    for (int i = 0 ; i < ARRAY_SIZE; i++) {
      atomicArray[i] = (SmallValue)SmallValue.getTestValue();
    }

    // atomic -> obj
    testArrayCopyInternal(atomicArray, objArray);

    // atomic -> non-atomic
    testArrayCopyInternal(atomicArray, nonAtomicArray);

    // atomic -> atomic
    SmallValue[] atomicArray2 = (SmallValue[])ValueClass.newNullRestrictedAtomicArray(SmallValue.class, ARRAY_SIZE);
    testArrayCopyInternal(atomicArray, atomicArray2);

    // atomic -> atomic same array
    testArrayCopyInternal(atomicArray, atomicArray);

    // atomic -> nullable
    testArrayCopyInternal(atomicArray, nullableArray);

    // Reset all arrays
    objArray = new Object[ARRAY_SIZE];
    nonAtomicArray = (SmallValue[])ValueClass.newNullRestrictedArray(SmallValue.class, ARRAY_SIZE);
    atomicArray = (SmallValue[])ValueClass.newNullRestrictedAtomicArray(SmallValue.class, ARRAY_SIZE);
    nullableArray = (SmallValue[])ValueClass.newNullableAtomicArray(SmallValue.class, ARRAY_SIZE);

    for (int i = 0 ; i < ARRAY_SIZE; i++) {
      nullableArray[i] = (SmallValue)SmallValue.getTestValue();
    }

    // nullable -> obj
    testArrayCopyInternal(nullableArray, objArray);

    // nullable -> non-atomic
    testArrayCopyInternal(nullableArray, nonAtomicArray);

    // nullable -> atomic
    testArrayCopyInternal(nullableArray, atomicArray);

    // nullable -> nullable
    SmallValue[] nullableArray2 = (SmallValue[])ValueClass.newNullableAtomicArray(SmallValue.class, ARRAY_SIZE);
    testArrayCopyInternal(nullableArray, nullableArray2);

    // nullable -> nullable same array
    testArrayCopyInternal(nullableArray, nullableArray);

    nullableArray[45] = null;

    // nullable with null -> obj
    testArrayCopyInternal(nullableArray, objArray);

    // nullable with null -> non-atomic  => NPE
    try {
      testArrayCopyInternal(nullableArray, nonAtomicArray);
      throw new RuntimeException("Missing NullPointerException");
    } catch (NullPointerException e) { }

    // nullable with null -> atomic      => NPE
    try {
      testArrayCopyInternal(nullableArray, atomicArray);
      throw new RuntimeException("Missing NullPointerException");
    } catch (NullPointerException e) { }

    // nullable with null -> nullable
    nullableArray2 = (SmallValue[])ValueClass.newNullableAtomicArray(SmallValue.class, ARRAY_SIZE);
    testArrayCopyInternal(nullableArray, nullableArray2);

    // nullable with null -> nullable same array
    testArrayCopyInternal(nullableArray, nullableArray);
  }

  static void testArrayCopyInternal(Object[] src, Object[] dst) {
    // When using this method for cases that should trigger a NPE or an ASE,
    // it is recommended to put the faulty value at index 45 in the src array
    assertTrue(src.length >= ARRAY_SIZE, "Must be for the test");
    assertTrue(dst.length >= ARRAY_SIZE, "Must be for the test");
    // Test 1 : good copy without indexes overlap
    System.arraycopy(src, 3, dst, 51, 40);
    for (int i = 0; i < 40; i++) {
      assertEquals(src[3+i], dst[51+i], "Mismatch after copying");
    }
    // Test 2 : good copy with indexes overlap
    System.arraycopy(src, 42, dst, 53, 45);
    if (src != dst) {  // Verification doesn't make sense if src and dst are the same
      for (int i = 0; i < 45; i++) {
        assertEquals(src[42+i], dst[53+i], "Mismatch after copying");
      }
    }
    // Test 3 : IOOB errors
    try {
      System.arraycopy(src, -1, dst, 3, 10);
      throw new RuntimeException("Missing IndexOutOfBoundsException");
    } catch(IndexOutOfBoundsException e) { }
    try {
      System.arraycopy(src, src.length - 5, dst, 3, 10);
      throw new RuntimeException("Missing IndexOutOfBoundsException");
    } catch(IndexOutOfBoundsException e) { }
    try {
      System.arraycopy(src, 10, dst, -1, 10);
      throw new RuntimeException("Missing IndexOutOfBoundsException");
    } catch(IndexOutOfBoundsException e) { }
    try {
      System.arraycopy(src, 10, dst, dst.length - 5, 10);
      throw new RuntimeException("Missing IndexOutOfBoundsException");
    } catch(IndexOutOfBoundsException e) { }
  }

  static void testArrayAccesses() throws NoSuchMethodException, InstantiationException,
  IllegalAccessException, InvocationTargetException {
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = runtimeMxBean.getInputArguments();
    boolean useFlatArray = !arguments.contains("-XX:FlatArrayElementMaxSize=0");
    System.out.println("UseFlatArray: " + useFlatArray);
    Class[] valueClasses = {SmallValue.class, MediumValue.class, BigValue.class};
    for (Class c: valueClasses) {
      System.out.println("Testing class " + c.getName());
      Method gtv = c.getMethod("getTestValue", null);
      Object o = gtv.invoke(null, null);
      assertNotNull(o);

      System.out.println("Regular reference array");
      Object[] array = (Object[])Array.newInstance(c, ARRAY_SIZE);
      assertFalse(ValueClass.isFlatArray(array));
      testNullableArray(array, o);

      System.out.println("NonAtomic NullRestricted array");
      array = ValueClass.newNullRestrictedArray(c, ARRAY_SIZE);
      Method ef = c.getMethod("expectingFlatNullRestrictedArray", null);
      boolean expectFlat = (Boolean)ef.invoke(null, null);
      assertTrue(ValueClass.isFlatArray(array) == (useFlatArray && expectFlat));
      testNullFreeArray(array, o);

      System.out.println("NullRestricted Atomic array");
      array = ValueClass.newNullRestrictedAtomicArray(c, ARRAY_SIZE);
      ef = c.getMethod("expectingFlatNullRestrictedAtomicArray", null);
      expectFlat = (Boolean)ef.invoke(null, null);
      assertTrue(ValueClass.isFlatArray(array) == (useFlatArray && expectFlat));
      testNullFreeArray(array, o);

      System.out.println("Nullable Atomic array");
      array = ValueClass.newNullableAtomicArray(c, ARRAY_SIZE);
      ef = c.getMethod("expectingFlatNullableAtomicArray", null);
      expectFlat = (Boolean)ef.invoke(null, null);
      assertTrue(ValueClass.isFlatArray(array) == (useFlatArray && expectFlat));
      testNullableArray(array, o);
    }
  }

  public static void main(String[] args) throws NoSuchMethodException, InstantiationException,
                                                IllegalAccessException, InvocationTargetException {
    testArrayAccesses();
    testArrayCopy();
  }

 }
