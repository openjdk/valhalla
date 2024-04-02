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
 * @test NullMarkersTest
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java NullMarkersTest.java
 * @run main/othervm NullMarkersTest
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    FieldLayoutAnalyzer.ClassLayout cl = fla.getClassLayout("NullMarkersTest$Container0");
    FieldLayoutAnalyzer.FieldBlock f = cl.getFieldWithName("c", false);
    Asserts.assertTrue(f.type() == FieldLayoutAnalyzer.BlockType.FLAT);
    // Asserts.assertTrue(f.nullMarkerOffset() != -1);
  }

  static ProcessBuilder exec(String... args) throws Exception {
    List<String> argsList = new ArrayList<>();
    Collections.addAll(argsList, "--enable-preview");
    Collections.addAll(argsList, "-XX:+PrintFieldLayout");
    Collections.addAll(argsList, "-XX:+EnableNullableFieldFlattening");
    Collections.addAll(argsList, "-cp", System.getProperty("java.class.path") + ":.");
    Collections.addAll(argsList, args);
    return ProcessTools.createTestJavaProcessBuilder(argsList);
  }

  public static void main(String[] args) throws Exception {
    // Generate test classes
    NullMarkersTest fat = new NullMarkersTest();


    // Execute the test runner in charge of loading all test classes
    ProcessBuilder pb = exec("NullMarkersTest$TestRunner");
    OutputAnalyzer out = new OutputAnalyzer(pb.start());

    // Analyze the test runner output
    System.out.print(out.getOutput());
    FieldLayoutAnalyzer.LogOutput lo = new FieldLayoutAnalyzer.LogOutput(out.asLines());
    FieldLayoutAnalyzer fla =  FieldLayoutAnalyzer.createFieldLayoutAnalyzer(lo, true);
    fla.check();
  }
}
