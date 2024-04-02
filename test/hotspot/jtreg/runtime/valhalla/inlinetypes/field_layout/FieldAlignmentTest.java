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
 * @test FieldAlignmentTest
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile FieldLayoutAnalyzer.java FieldAlignmentTest.java
 * @run main/othervm FieldAlignmentTest
 */

 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;

 import jdk.internal.vm.annotation.ImplicitlyConstructible;

 import jdk.test.lib.Asserts;
 import jdk.test.lib.ByteCodeLoader;
 import jdk.test.lib.helpers.ClassFileInstaller;
 import jdk.test.lib.compiler.InMemoryJavaCompiler;
 import jdk.test.lib.process.OutputAnalyzer;
 import jdk.test.lib.process.ProcessTools;

 public class FieldAlignmentTest {
  public static class ZeroByte { }
  public static class OneByte { byte b; }
  public static class TwoByte { byte b0; byte b1; }
  public static class ThreeByte { byte b0; byte b1; byte b2; }
  public static class FourByte { byte b0; byte b1; byte b2; byte b3; }
  public static class FiveByte { byte b0; byte b1; byte b2; byte b3; byte b4; }
  public static class SixByte { byte b0; byte b1; byte b2; byte b3; byte b4; byte b5; }
  public static class SevenByte { byte b0; byte b1; byte b2; byte b3; byte b4; byte b5; byte b6; }
  public static final String[] superNames = { ZeroByte.class.getCanonicalName(),
                                              OneByte.class.getCanonicalName(),
                                              TwoByte.class.getCanonicalName(),
                                              ThreeByte.class.getCanonicalName(),
                                              FourByte.class.getCanonicalName(),
                                              FiveByte.class.getCanonicalName(),
                                              SixByte.class.getCanonicalName(),
                                              SevenByte.class.getCanonicalName() };
  public static final String[] valueNames = { ValueOneByte.class.getCanonicalName(),
                                              ValueOneChar.class.getCanonicalName(),
                                              ValueOneShort.class.getCanonicalName(),
                                              ValueOneInt.class.getCanonicalName(),
                                              ValueOneLong.class.getCanonicalName(),
                                              ValueOneFloat.class.getCanonicalName(),
                                              ValueOneDouble.class.getCanonicalName(),
                                              ValueByteLong.class.getCanonicalName(),
                                              ValueByteInt.class.getCanonicalName() };

  List<String> testNames = new ArrayList<String>();

  @ImplicitlyConstructible static value class ValueOneByte { byte val = 0; }
  @ImplicitlyConstructible static value class ValueOneChar { char val = 0; }
  @ImplicitlyConstructible static value class ValueOneShort { short val = 0; }
  @ImplicitlyConstructible static value class ValueOneInt { int val = 0; }
  @ImplicitlyConstructible static value class ValueOneLong { long val = 0; }
  @ImplicitlyConstructible static value class ValueOneFloat { float val = 0f; }
  @ImplicitlyConstructible static value class ValueOneDouble { double val = 0d; }

  @ImplicitlyConstructible static value class ValueByteLong { byte b = 0; long l = 0; }
  @ImplicitlyConstructible static value class ValueByteInt { byte b = 0; int i = 0; }

  void generateTests() throws Exception {
    for (String vName : valueNames) {
      for (String sName : superNames) {
        String vNameShort = vName.substring(vName.lastIndexOf('.') + 1);
        String sNameShort = sName.substring(sName.lastIndexOf('.') + 1);
        String className = "Test" + vNameShort + "With" + sNameShort;
        String sourceCode = "import jdk.internal.vm.annotation.NullRestricted; " +
                            "public class " + className + " extends " + sName + " { " +
                            "    @NullRestricted" +
                            "    " + vName + " v1;" +
                            "}";
        byte[] byteCode = InMemoryJavaCompiler.compile(className, sourceCode,
                                                      "-source", "22", "--enable-preview",
                                                      "--add-exports", "java.base/jdk.internal.vm.annotation=ALL-UNNAMED");
        jdk.test.lib.helpers.ClassFileInstaller.writeClassToDisk(className, byteCode);
        testNames.add(className);
      }
    }
  }

  void generateTestRunner() throws Exception {
    String className = "TestRunner";
    StringBuilder sb = new StringBuilder();
    sb.append("public class ").append(className).append(" {");
    sb.append("    public void main(String[] args) {");
    for (String name : testNames) {
      sb.append("        ").append(name).append(" var").append(name).append(" = new ").append(name).append("();");
    }
    sb.append("    }");
    sb.append("}");
    byte[] byteCode = InMemoryJavaCompiler.compile(className, sb.toString(),
                                                   "-source", "22", "--enable-preview",
                                                   "-cp", ".");
    jdk.test.lib.helpers.ClassFileInstaller.writeClassToDisk(className, byteCode);
  }

  static ProcessBuilder exec(String... args) throws Exception {
    List<String> argsList = new ArrayList<>();
    Collections.addAll(argsList, "--enable-preview");
    Collections.addAll(argsList, "-XX:+PrintFieldLayout");
    Collections.addAll(argsList, "-cp", System.getProperty("java.class.path") + ":.");
    Collections.addAll(argsList, args);
    return ProcessTools.createTestJavaProcessBuilder(argsList);
  }

  public static void main(String[] args) throws Exception {
    // Generate test classes
    FieldAlignmentTest fat = new FieldAlignmentTest();
    fat.generateTests();
    fat.generateTestRunner();

    // Execute the test runner in charge of loading all test classes
    ProcessBuilder pb = exec("TestRunner");
    OutputAnalyzer out = new OutputAnalyzer(pb.start());

    // Analyze the test runner output
    System.out.print(out.getOutput());
    FieldLayoutAnalyzer.LogOutput lo = new FieldLayoutAnalyzer.LogOutput(out.asLines());
    FieldLayoutAnalyzer fla =  FieldLayoutAnalyzer.createFieldLayoutAnalyzer(lo, true);
    fla.check();
  }
 }