/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

 /*
 * @test id=ValueCompositionTest_no_atomic_flat_and_no_nullable_flat
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java ValueCompositionTest.java
 * @run main/othervm ValueCompositionTest 0
 */

 /*
 * @test id=ValueCompositionTest_atomic_flat_and_nullable_flat
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java ValueCompositionTest.java
 * @run main/othervm ValueCompositionTest 1
 */

 /* @test id=ValueCompositionTest_no_atomic_flat_and_nullable_flat
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java ValueCompositionTest.java
 * @run main/othervm ValueCompositionTest 2
 */

 /* @test id=ValueCompositionTest_atomic_flat_and_no_nullable_flat
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java ValueCompositionTest.java
 * @run main/othervm ValueCompositionTest 3
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class ValueCompositionTest {

  static class TestRunner {
    public static void main(String[] args) throws Exception {
      Class testClass = Class.forName("ValueCompositionTest");
      Asserts.assertNotNull(testClass);
      Method[] testMethods = testClass.getMethods();
      for (Method test : testMethods) {
        if (test.getName().startsWith("test_")) {
          Asserts.assertTrue(Modifier.isStatic(test.getModifiers()));
          Asserts.assertTrue(test.getReturnType().equals(Void.TYPE));
          System.out.println("Running " + test.getName());
          test.invoke(null);
        }
      }
    }
  }

  @ImplicitlyConstructible
  static value class Value0 {
    byte b0 = 0;
    byte b1 = 0;
  }

  static class Container0 {
    @NullRestricted
    Value0 val0 = new Value0();
    Value0 val1 = new Value0();
  }

  static public void test_0() {
    Container0 c = new Container0();
  }

  static public void check_0(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container0");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    if (useAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.ATOMIC_FLAT, f0.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f0.layoutKind());
    }
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    if (useNullableAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NULLABLE_FLAT, f1.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
    }
  }

  @ImplicitlyConstructible
  static value class Container1 {
    @NullRestricted
    Value0 val0 = new Value0();
    Value0 val1 = new Value0();
  }

  static public void test_1() {
    Container1 c = new Container1();
  }

  static public void check_1(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container1");
    FieldLayoutAnalyzer.FieldBlock f = cl.getFieldFromName("val0", false);
    if (useAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.ATOMIC_FLAT, f.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f.layoutKind());
    }
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    if (useNullableAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NULLABLE_FLAT, f1.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
    }
  }

  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class Container2 {
    @NullRestricted
    Value0 val0 = new Value0();
    Value0 val1 = new Value0();
  }

  static public void test_2() {
    Container2 c = new Container2();
  }

  // An atomic value should not be flattened in a non-atomic value
  static public void check_2(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container2");
    FieldLayoutAnalyzer.FieldBlock f = cl.getFieldFromName("val0", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f.layoutKind());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
  }

  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class Value1 {
    byte b0 = 0;
    byte b1 = 0;
  }

  static class Container3 {
    @NullRestricted
    Value1 val0 = new Value1();
    Value1 val1 = new Value1();
  }

  static public void test_3() {
    Container3 c = new Container3();
  }

  static public void check_3(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container3");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_ATOMIC_FLAT, f0.layoutKind());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    if (useNullableAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NULLABLE_FLAT, f1.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
    }
  }

  @ImplicitlyConstructible
  static value class Container4 {
    @NullRestricted
    Value1 val0 = new Value1();
    Value1 val1 = new Value1();
  }

  static public void test_4() {
    Container4 c = new Container4();
  }

  static public void check_4(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container4");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_ATOMIC_FLAT, f0.layoutKind());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    if (useNullableAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NULLABLE_FLAT, f1.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
    }
  }

  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class Container5 {
    @NullRestricted
    Value1 val0 = new Value1();
    Value1 val1 = new Value1();
  }

  static public void test_5() {
    Container5 c = new Container5();
  }

  static public void check_5(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container5");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_ATOMIC_FLAT, f0.layoutKind());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
  }

  @ImplicitlyConstructible
  static value class Value2 {
    byte b0 = 0;
  }

  static class Container6 {
    @NullRestricted
    Value2 val0 = new Value2();
    Value2 val1 = new Value2();
  }

  static public void test_6() {
    Container6 c = new Container6();
  }

  static public void check_6(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container6");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_ATOMIC_FLAT, f0.layoutKind());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    if (useNullableAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NULLABLE_FLAT, f1.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
    }
  }

  @ImplicitlyConstructible
  static value class Container7 {
    @NullRestricted
    Value2 val0 = new Value2();
    Value2 val1 = new Value2();
  }

  static public void test_7() {
    Container7 c = new Container7();
  }

  static public void check_7(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container7");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_ATOMIC_FLAT, f0.layoutKind());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    if (useNullableAtomicFlat) {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NULLABLE_FLAT, f1.layoutKind());
    } else {
      Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
    }
  }

  @ImplicitlyConstructible
  @LooselyConsistentValue
  static value class Container8 {
    @NullRestricted
    Value2 val0 = new Value2();
    Value2 val1 = new Value2();
  }

  static public void test_8() {
    Container8 c = new Container8();
  }

  static public void check_8(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("ValueCompositionTest$Container8");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_ATOMIC_FLAT, f0.layoutKind());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    Asserts.assertEquals(FieldLayoutAnalyzer.LayoutKind.NON_FLAT, f1.layoutKind());
  }


  static ProcessBuilder exec(String... args) throws Exception {
    List<String> argsList = new ArrayList<>();
    Collections.addAll(argsList, "--enable-preview");
    Collections.addAll(argsList, "-Xint");
    Collections.addAll(argsList, "-XX:+UnlockDiagnosticVMOptions");
    Collections.addAll(argsList, "-XX:+PrintFieldLayout");
    Collections.addAll(argsList, "-Xshare:off");
    Collections.addAll(argsList, "-Xmx256m");
    Collections.addAll(argsList, useAtomicFlat ? "-XX:+AtomicFieldFlattening" : "-XX:-AtomicFieldFlattening");
    Collections.addAll(argsList, useNullableAtomicFlat ?  "-XX:+NullableFieldFlattening" : "-XX:-NullableFieldFlattening");
    Collections.addAll(argsList, "-cp", System.getProperty("java.class.path") + System.getProperty("path.separator") + ".");
    Collections.addAll(argsList, args);
    return ProcessTools.createTestJavaProcessBuilder(argsList);
  }

  static boolean useAtomicFlat;
  static boolean useNullableAtomicFlat;

  public static void main(String[] args) throws Exception {

    switch(args[0]) {
      case "0": useAtomicFlat = false;
                useNullableAtomicFlat = false;
                break;
      case "1": useAtomicFlat = true;
                useNullableAtomicFlat = true;
                break;
      case "2": useAtomicFlat = false;
                useNullableAtomicFlat = true;
                break;
      case "3": useAtomicFlat = true;
                useNullableAtomicFlat = false;
                break;
      default: throw new RuntimeException("Unrecognized configuration");
    }

    // Generate test classes
    ValueCompositionTest vct = new ValueCompositionTest();

    // Execute the test runner in charge of loading all test classes
    ProcessBuilder pb = exec("ValueCompositionTest$TestRunner");
    OutputAnalyzer out = new OutputAnalyzer(pb.start());

    if (out.getExitValue() != 0) {
      System.out.print(out.getOutput());
    }
    Asserts.assertEquals(out.getExitValue(), 0, "Something went wrong while running the tests");

    // To help during test development
    System.out.print(out.getOutput());

    // Get and parse the test output
    FieldLayoutAnalyzer.LogOutput lo = new FieldLayoutAnalyzer.LogOutput(out.asLines());
    FieldLayoutAnalyzer fla =  FieldLayoutAnalyzer.createFieldLayoutAnalyzer(lo);

    // Running tests verification method (check that tests produced the right configuration)
    Class testClass = ValueCompositionTest.class;
      Method[] testMethods = testClass.getMethods();
      for (Method test : testMethods) {
        if (test.getName().startsWith("check_")) {
          Asserts.assertTrue(Modifier.isStatic(test.getModifiers()));
          Asserts.assertTrue(test.getReturnType().equals(Void.TYPE));
          test.invoke(null, fla);
        }
      }

    // Verify that all layouts are correct
    fla.check();
  }

}
