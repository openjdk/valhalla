/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @test id=NullMarker32
 * @ignore
 * @requires vm.bits == 32
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java NullMarkersTest.java
 * @run main NullMarkersTest 0
 */

/*
 * @test id=NullMarker64CompressedOops
 * @ignore
 * @requires vm.bits == 64
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java NullMarkersTest.java
 * @run main NullMarkersTest 1
 */

/*
 * @test id=NullMarker64NoCompressedOops
 * @ignore
 * @requires vm.bits == 64
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java NullMarkersTest.java
 * @run main NullMarkersTest 2
 */

/*
 * @test id=NullMarker64NoCompressedOopsNoCompressedKlassPointers
 * @ignore
 * @requires vm.bits == 64
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java NullMarkersTest.java
 * @run main NullMarkersTest 3
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.internal.vm.annotation.NullRestricted;
import jdk.internal.vm.annotation.Strict;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class NullMarkersTest {


  static class TestRunner {
    public static void main(String[] args) throws Exception {
      Class testClass = Class.forName("NullMarkersTest");
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

  static value class Value0 {
    int i = 0;
  }

  static class Container0 {
    Value0 val;
  }

  // Simple test with a single nullable flattenable field
  static public void test_0() {
    Container0 c = new Container0();
  }

  static public void check_0(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("NullMarkersTest$Container0");
    FieldLayoutAnalyzer.FieldBlock f = cl.getFieldFromName("val", false);
    Asserts.assertTrue(f.isFlat());
    Asserts.assertTrue(f.hasNullMarker());
  }

  static value class Value1 {
    short s = 0;
  }

  static class Container1 {
    Value1 val0;
    Value1 val1;
    @Strict
    @NullRestricted
    Value1 val2 = new Value1();
  }

  static public void test_1() {
    Container1 c = new Container1();
  }

  static public void check_1(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("NullMarkersTest$Container1");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertTrue(f0.isFlat());
    Asserts.assertTrue(f0.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    Asserts.assertTrue(f1.isFlat());
    Asserts.assertTrue(f1.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock f2 = cl.getFieldFromName("val2", false);
    Asserts.assertTrue(f2.isFlat());
    Asserts.assertFalse(f2.hasNullMarker());
  }

  static value class Value2 {
    long l = 0;
  }

  static class Container2a {
    Value2 vala;
  }

  static class Container2b extends Container2a {
    Value2 valb;
  }

  static public void test_2() {
    Container2b c = new Container2b();
  }

  static public void check_2(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cla = fla.getClassLayoutFromName("NullMarkersTest$Container2a");
    FieldLayoutAnalyzer.FieldBlock fa = cla.getFieldFromName("vala", false);
    Asserts.assertTrue(fa.isFlat());
    Asserts.assertTrue(fa.hasNullMarker());
    FieldLayoutAnalyzer.ClassLayout clb = fla.getClassLayoutFromName("NullMarkersTest$Container2b");
    FieldLayoutAnalyzer.FieldBlock fb = clb.getFieldFromName("valb", false);
    Asserts.assertTrue(fb.isFlat());
    Asserts.assertTrue(fb.hasNullMarker());
  }

  static value class Value3 {
    double d = 0.0d;
  }

  static class Container3a {
    @Strict
    @NullRestricted
    Value3 val0 = new Value3();
    Value3 val1;
  }

  static class Container3b extends Container3a {
    Value3 val2;
    @Strict
    @NullRestricted
    Value3 val3 = new Value3();
  }

  static class Container3c extends Container3b {
    Value3 val4;
    Value3 val5;
  }

  static public void test_3() {
    Container3c c = new Container3c();
  }

  static public void check_3(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cla = fla.getClassLayoutFromName("NullMarkersTest$Container3a");
    FieldLayoutAnalyzer.FieldBlock f0 = cla.getFieldFromName("val0", false);
    Asserts.assertTrue(f0.isFlat());
    Asserts.assertFalse(f0.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock f1 = cla.getFieldFromName("val1", false);
    Asserts.assertTrue(f1.isFlat());
    Asserts.assertTrue(f1.hasNullMarker());
    FieldLayoutAnalyzer.ClassLayout clb = fla.getClassLayoutFromName("NullMarkersTest$Container3b");
    FieldLayoutAnalyzer.FieldBlock f2 = clb.getFieldFromName("val2", false);
    Asserts.assertTrue(f2.isFlat());
    Asserts.assertTrue(f2.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock f3 = clb.getFieldFromName("val3", false);
    Asserts.assertTrue(f3.isFlat());
    Asserts.assertFalse(f3.hasNullMarker());
    FieldLayoutAnalyzer.ClassLayout clc = fla.getClassLayoutFromName("NullMarkersTest$Container3c");
    FieldLayoutAnalyzer.FieldBlock f4 = clc.getFieldFromName("val4", false);
    Asserts.assertTrue(f4.isFlat());
    Asserts.assertTrue(f4.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock f5 = clc.getFieldFromName("val5", false);
    Asserts.assertTrue(f5.isFlat());
    Asserts.assertTrue(f5.hasNullMarker());
  }

  @LooselyConsistentValue
  static value class Value4 {
    int i = 0;
    byte b = 0;
  }

  static class Container4 {
    Value4 val0;
    Value4 val1;
  }

  static public void test_4() {
    Container4 c = new Container4();
  }

  static void check_4(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("NullMarkersTest$Container4");
    FieldLayoutAnalyzer.FieldBlock f0 = cl.getFieldFromName("val0", false);
    Asserts.assertTrue(f0.isFlat());
    Asserts.assertTrue(f0.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock f1 = cl.getFieldFromName("val1", false);
    Asserts.assertTrue(f1.isFlat());
    Asserts.assertTrue(f1.hasNullMarker());
  }

  @LooselyConsistentValue
  static value class Value5a {
    short s = 0;
    byte b = 0;
  }

  @LooselyConsistentValue
  static value class Value5b {
    @Strict
    @NullRestricted
    Value5a val0 = new Value5a();
    @Strict
    @NullRestricted
    Value5a val1 = new Value5a();
  }

  static class Container5 {
    Value5a vala;
    Value5b valb0;
    Value5b valb1;
  }

  static public void test_5() {
    Container5 c = new Container5();
  }

  static void check_5(FieldLayoutAnalyzer fla) {
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayoutFromName("NullMarkersTest$Container5");
    FieldLayoutAnalyzer.FieldBlock fa = cl.getFieldFromName("vala", false);
    Asserts.assertTrue(fa.isFlat());
    Asserts.assertTrue(fa.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock fb0 = cl.getFieldFromName("valb0", false);
    Asserts.assertTrue(fb0.isFlat());
    Asserts.assertTrue(fb0.hasNullMarker());
    FieldLayoutAnalyzer.FieldBlock fb1 = cl.getFieldFromName("valb1", false);
    Asserts.assertTrue(fb1.isFlat());
    Asserts.assertTrue(fb1.hasNullMarker());
  }

  static ProcessBuilder exec(String compressedOopsArg, String compressedKlassPointersArg, String... args) throws Exception {
    List<String> argsList = new ArrayList<>();
    Collections.addAll(argsList, "--enable-preview");
    Collections.addAll(argsList, "-XX:+UnlockDiagnosticVMOptions");
    Collections.addAll(argsList, "-XX:+PrintFieldLayout");
    Collections.addAll(argsList, "-Xshare:off");
    if (compressedOopsArg != null) {
      Collections.addAll(argsList, compressedOopsArg);
    }
    if (compressedKlassPointersArg != null) {
      Collections.addAll(argsList, compressedKlassPointersArg);
    }
    Collections.addAll(argsList, "-Xmx256m");
    Collections.addAll(argsList, "-XX:+UseNullableValueFlattening");
    Collections.addAll(argsList, "-cp", System.getProperty("java.class.path") + System.getProperty("path.separator") + ".");
    Collections.addAll(argsList, args);
    return ProcessTools.createTestJavaProcessBuilder(argsList);
  }

  public static void main(String[] args) throws Exception {
    String compressedOopsArg;
    String compressedKlassPointersArg;

    switch(args[0]) {
      case "0": compressedOopsArg = null;
                compressedKlassPointersArg = null;
                break;
      case "1": compressedOopsArg = "-XX:+UseCompressedOops";
                compressedKlassPointersArg =  "-XX:+UseCompressedClassPointers";
                break;
      case "2": compressedOopsArg = "-XX:-UseCompressedOops";
                compressedKlassPointersArg = "-XX:+UseCompressedClassPointers";
                break;
      case "3": compressedOopsArg = "-XX:-UseCompressedOops";
                compressedKlassPointersArg = "-XX:-UseCompressedClassPointers";
                break;
      default: throw new RuntimeException("Unrecognized configuration");
    }

    // Generate test classes
    NullMarkersTest fat = new NullMarkersTest();

    // Execute the test runner in charge of loading all test classes
    ProcessBuilder pb = exec(compressedOopsArg, compressedKlassPointersArg, "NullMarkersTest$TestRunner");
    OutputAnalyzer out = new OutputAnalyzer(pb.start());

    if (out.getExitValue() != 0) {
      System.out.print(out.getOutput());
    }
    Asserts.assertEquals(out.getExitValue(), 0, "Something went wrong while running the tests");

    // Get and parse the test output
    FieldLayoutAnalyzer.LogOutput lo = new FieldLayoutAnalyzer.LogOutput(out.asLines());
    FieldLayoutAnalyzer fla =  FieldLayoutAnalyzer.createFieldLayoutAnalyzer(lo);

    // Running tests verification method (check that tests produced the right configuration)
    Class testClass = NullMarkersTest.class;
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
