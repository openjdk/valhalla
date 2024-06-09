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
 * @test id=32bits
 * @requires vm.bits == 32
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java ValueFieldInheritanceTest.java
 * @run main/othervm -Xint ValueFieldInheritanceTest 0
 */

/*
 * @test id=64bitsCompressedOops
 * @requires vm.bits == 64
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java ValueFieldInheritanceTest.java
 * @run main/othervm -Xint ValueFieldInheritanceTest 1
 */

/*
 * @test id=64bitsNoCompressedOops
 * @requires vm.bits == 64
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java ValueFieldInheritanceTest.java
 * @run main/othervm -Xint ValueFieldInheritanceTest 2
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.views.AbstractView;

import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


public class ValueFieldInheritanceTest {

  static abstract value class AbstractNoField { };

  static abstract value class AbstractOneField { byte b = 0; }

  static abstract value class SubAbstractOne extends AbstractNoField { }

  static abstract value class SubAbstractTwo extends AbstractOneField { }

  static abstract value class SubAbstractThree extends AbstractNoField  { int i = 0; }

  static abstract value class SubAbstractFour extends AbstractOneField { int i = 0; }

  static class Identity0 extends AbstractNoField { }

  static class Identity1 extends AbstractNoField { short s; }

  static class Identity2 extends SubAbstractOne { }

  static class Identity3 extends SubAbstractOne { char c; }

  static class Identity4 extends SubAbstractTwo { }

  static class Identity5 extends SubAbstractTwo { int i; }

  static class Identity6 extends SubAbstractThree { }

  static class Identity7 extends SubAbstractThree { int i; }

  static class Identity8 extends SubAbstractFour { }

  static class Identity9 extends SubAbstractFour { int i; }

  static class ConcreteValue0 extends AbstractNoField { }

  static class ConcreteValue1 extends AbstractNoField { short s; }

  static class ConcreteValue2 extends SubAbstractOne { }

  static class ConcreteValue3 extends SubAbstractOne { char c; }

  static class ConcreteValue4 extends SubAbstractTwo { }

  static class ConcreteValue5 extends SubAbstractTwo { int i; }

  static class ConcreteValue6 extends SubAbstractThree { }

  static class ConcreteValue7 extends SubAbstractThree { int i; }

  static class ConcreteValue8 extends SubAbstractFour { }

  static class ConcreteValue9 extends SubAbstractFour { int i; }

  static public void test_0() {
    var i0 = new Identity0();
    var i1 = new Identity1();
    var i2 = new Identity2();
    var i3 = new Identity3();
    var i4 = new Identity4();
    var i5 = new Identity5();
    var i6 = new Identity6();
    var i7 = new Identity7();
    var i8 = new Identity8();
    var i9 = new Identity9();
    var c0 = new ConcreteValue0();
    var c1 = new ConcreteValue1();
    var c2 = new ConcreteValue2();
    var c3 = new ConcreteValue3();
    var c4 = new ConcreteValue4();
    var c5 = new ConcreteValue5();
    var c6 = new ConcreteValue6();
    var c7 = new ConcreteValue7();
    var c8 = new ConcreteValue8();
    var c9 = new ConcreteValue9();
  }

  static class TestRunner {
    public static void main(String[] args) throws Exception {
      Class testClass = Class.forName("ValueFieldInheritanceTest");
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

  static ProcessBuilder exec(String compressedOopsArg, String... args) throws Exception {
    List<String> argsList = new ArrayList<>();
    Collections.addAll(argsList, "--enable-preview");
    Collections.addAll(argsList, "-XX:+UnlockDiagnosticVMOptions");
    Collections.addAll(argsList, "-XX:+PrintFieldLayout");
    if (compressedOopsArg != null) {
      Collections.addAll(argsList, compressedOopsArg);
    }
    Collections.addAll(argsList, "-Xmx256m");
    Collections.addAll(argsList, "-cp", System.getProperty("java.class.path") + ":.");
    Collections.addAll(argsList, args);
    return ProcessTools.createTestJavaProcessBuilder(argsList);
  }

  public static void main(String[] args) throws Exception {
    String compressedOopsArg;

    switch(args[0]) {
      case "0": compressedOopsArg = null;
                break;
      case "1": compressedOopsArg = "-XX:+UseCompressedOops";
                break;
      case "2": compressedOopsArg = "-XX:-UseCompressedOops";
                break;
      default: throw new RuntimeException("Unrecognized configuration");
    }

    // Generate test classes
    // NullMarkersTest fat = new NullMarkersTest();

    // Execute the test runner in charge of loading all test classes
    ProcessBuilder pb = exec(compressedOopsArg, "ValueFieldInheritanceTest$TestRunner");
    OutputAnalyzer out = new OutputAnalyzer(pb.start());

    if (out.getExitValue() != 0) {
      out.outputTo(System.out);
    }
    Asserts.assertEquals(out.getExitValue(), 0, "Something went wrong while running the tests");

    // Get and parse the test output
    System.out.print(out.getOutput());
    FieldLayoutAnalyzer.LogOutput lo = new FieldLayoutAnalyzer.LogOutput(out.asLines());
    FieldLayoutAnalyzer fla =  FieldLayoutAnalyzer.createFieldLayoutAnalyzer(lo);

    // Running tests verification method (check that tests produced the right configuration)
    Class testClass = ValueFieldInheritanceTest.class;
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
