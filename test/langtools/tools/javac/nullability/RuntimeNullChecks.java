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

/*
 * @test
 * @enablePreview
 * @summary test runtime null checks
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main RuntimeNullChecks
 */

import java.util.*;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.javac.util.Assert;

import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;

public class RuntimeNullChecks extends TestRunner {
    ToolBox tb;

    RuntimeNullChecks() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    public static void main(String... args) throws Exception {
        RuntimeNullChecks t = new RuntimeNullChecks();
        t.runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testRuntimeChecks(Path base) throws Exception {
        int i = 0;
        for (String code: new String[] {
                // local variables
                """
                import java.util.*;
                class Test {
                    public static void main(String... args) {
                        List<String> list = new ArrayList<>();
                        list.add(null);
                        for (String! s : list) {
                        }
                    }
                }
                """,
                """
                class Test {
                    public static void main(String... args) {
                        String s = null;
                        String! o = s; // NPE at runtime, variable initialization
                    }
                }
                """,
                """
                class Test {
                    void m(String someObject) {
                        String! x = "foo";
                        x = (String)someObject;
                    }
                    public static void main(String... args) {
                        new Test().m(null);
                    }
                }
                """,
                """
                class Test {
                    public static void main(String... args) {
                        String s = null;
                        String! o;
                        o = s; // NPE at runtime, assignment, it doesn't stress the same code path as the case above
                    }
                }
                """,
                /*"""
                class Test { // should fail, needs to be fixed once we redo null-restricted array creation
                    public static void main(String... args) {
                        String s = null;
                        String[]! sr = new String![10];
                        sr[0] = s; // NPE at runtime, assignment
                    }
                }
                """,*/
                """
                class Test {
                    static String id(String! arg) { return arg; }
                    public static void main(String... args) {
                        String s = null;
                        Object o = id(s); // NPE at runtime, method invocation
                    }
                }
                """,
                /*"""
                class Test {
                    static String id(int i, String!... arg) { return ""; }
                    public static void main(String... args) {
                        String s1 = null;
                        String s2 = "";
                        Object o = id(1, s1, s2); // NPE at runtime, method invocation
                    }
                }
                """,
                """
                class Test {
                    static String id(int i, String!... arg) { return ""; }
                    public static void main(String... args) {
                        String s1 = "";
                        String s2 = null;
                        Object o = id(1, s1, s2); // NPE at runtime, method invocation
                    }
                }
                """,*/
                """
                class Test {
                    public static void main(String... args) {
                        String s = null;
                        Object o = (String!) s; // NPE, cast
                    }
                }
                """,
                """
                class Test {
                    public static void main(String... args) {
                        Object s = null;
                        Object o = (String & Runnable!) s; // NPE, cast
                    }
                }
                """,
                """
                class Test {
                    class Inner {
                        class MyPrivilegedAction<T> {
                            // as the outer is inserted after the fact, we are checking the outer class argument not `o`,
                            // need to fix this
                            MyPrivilegedAction(Object! o) {}
                            T run() {
                                return null;
                            }
                        }
                    }
                    public <T> T doPrivileged(Inner.MyPrivilegedAction<T> action) {
                        return action.run();
                    }
                    boolean isSystemProperty(String key, String value, String def, Object o) {
                        return doPrivileged(
                            new Inner().new MyPrivilegedAction<Boolean>(o) {
                                @Override
                                public Boolean run() {
                                    return value.equals(System.getProperty(key, def));
                                }
                            });
                    }
                    public static void main(String... args) {
                        Test test = new Test();
                        test.isSystemProperty("1", "2", "3", null);
                    }
                }
                """,
                """
                class Test {
                    String! m(String someObject) {
                        return (String)someObject;
                    }
                    public static void main(String... args) {
                        new Test().m(null);
                    }
                }
                """,
                """
                class Test {
                    String! m(String someObject) {
                        return someObject;
                    }
                    public static void main(String... args) {
                        new Test().m(null);
                    }
                }
                """
        }) {
            System.err.println("executing test " + i++);
            testHelper(base, code, true, NullPointerException.class);
        }

        // enums are a bit special as the NPE happens inside a static initializer and ExceptionInInitializerError is thrown
        testHelper(base,
                """
                class Test {
                    static Object s = null;
                    enum E {
                        A(s);
                        // same issue as with inner classes
                        E(Object! o) {}
                    }
                    public static void main(String... args) {
                        Test.E a = E.A;
                    }
                }
                """, true, ExceptionInInitializerError.class);

        // similar test cases as above but without null markers, should trivially pass
        i = 0;
        for (String code: new String[] {
                """
                class Test {
                    public static void main(String... args) {
                        String s = null;
                        String o = s;
                    }
                }
                """,
                """
                class Test {
                    public static void main(String... args) {
                        String s = null;
                        String o;
                        o = s;
                    }
                }
                """,
                """
                class Test {
                    public static void main(String... args) {
                        String s = null;
                        String[] sr = new String[10];
                        sr[0] = s;
                    }
                }
                """,
                """
                class Test {
                    static String id(String arg) { return arg; }
                    public static void main(String... args) {
                        String s = null;
                        Object o = id(s);
                    }
                }
                """,
                """
                class Test {
                    public static void main(String... args) {
                        String s = null;
                        Object o = (String) s;
                    }
                }
                """,
                """
                class Test {
                    class Inner {
                        class MyPrivilegedAction<T> {
                            MyPrivilegedAction(Object o) {}
                            T run() {
                                return null;
                            }
                        }
                    }
                    public <T> T doPrivileged(Inner.MyPrivilegedAction<T> action) {
                        return action.run();
                    }
                    boolean isSystemProperty(String key, String value, String def, Object o) {
                        return doPrivileged(
                            new Inner().new MyPrivilegedAction<Boolean>(o) {
                                @Override
                                public Boolean run() {
                                    return value.equals(System.getProperty(key, def));
                                }
                            });
                    }
                    public static void main(String... args) {
                        Test test = new Test();
                        test.isSystemProperty("1", "2", "3", null);
                    }
                }
                """
        }) {
            System.err.println("executing test " + i++);
            testHelper(base, code, false, null);
        }
    }

    private static String[] PREVIEW = {
            "--enable-preview",
            "-source", Integer.toString(Runtime.version().feature())
    };

    private static String[] NO_USE_SITE_CHECKS = {
            "--enable-preview",
            "-source", Integer.toString(Runtime.version().feature()),
            "-XDuseSiteNullChecks=none"
    };

    private static String[] USE_SITE_CHECKS_FOR_METHODS_ONLY = {
            "--enable-preview",
            "-source", Integer.toString(Runtime.version().feature()),
            "-XDuseSiteNullChecks=methods"
    };

    private static String[] USE_SITE_CHECKS_FOR_METHODS_AND_FIELDS = {
            "--enable-preview",
            "-source", Integer.toString(Runtime.version().feature()),
            "-XDuseSiteNullChecks=methods+fields"
    };

    /* returns the path to the output folder in case the caller wants to analyze the produced class files
     */
    private Path testHelper(Path base,
                            String testCode,
                            boolean shouldFail,
                            Class<?> expectedError) throws Exception {
        return testHelper(base, testCode, shouldFail, expectedError, PREVIEW);
    }

    /* returns the path to the output folder in case the caller wants to analyze the produced class files
     */
    private Path testHelper(Path base,
                            String testCode,
                            boolean shouldFail,
                            Class<?> expectedError,
                            String[] compilerOptions) throws Exception {
        Path src = base.resolve("src");
        Path testSrc = src.resolve("Test");

        tb.writeJavaFiles(testSrc, testCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .options(compilerOptions)
                .files(findJavaFiles(src))
                .run();

        if (shouldFail) {
            // let's check that we get the expected error
            try {
                String output = new JavaTask(tb)
                        .classpath(out.toString())
                        .classArgs("Test")
                        .vmOptions("--enable-preview")
                        .run(Task.Expect.FAIL)
                        .writeAll()
                        .getOutput(Task.OutputKind.STDERR);
                if (!output.startsWith("Exception in thread \"main\" " + expectedError.getName())) {
                    throw new AssertionError(expectedError.getName() + " expected");
                }
            } catch (Throwable t) {
                throw new AssertionError("failing for test case " + testCode);
            }
        } else {
            new JavaTask(tb)
                    .classpath(out.toString())
                    .vmOptions("--enable-preview")
                    .classArgs("Test")
                    .run(Task.Expect.SUCCESS);
        }
        return out;
    }

    @Test
    public void testPatternMatching(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");
        tb.writeJavaFiles(src,
                          """
                          public class Test {
                              public static void main(String... args) {
                                  Box box = new Box(null);

                                  if (!(box instanceof Box(String s1))) {
                                      throw new AssertionError();
                                  }

                                  if (box instanceof Box(String! s2)) {
                                      throw new AssertionError();
                                  }

                                  switch (box) {
                                      case Box(String s3) -> {} //OK
                                      default -> throw new AssertionError();
                                  }

                                  switch (box) {
                                      case Box(String! s4) ->
                                          throw new AssertionError();
                                      default -> {}
                                  }

                                  System.out.println("pass");
                              }
                              record Box(String str) {}
                          }
                          """);

        Files.createDirectories(classes);

        new JavacTask(tb)
            .options(PREVIEW)
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .run(Task.Expect.SUCCESS)
            .writeAll();

        var out = new JavaTask(tb)
                .vmOptions("--enable-preview")
                .classpath(classes.toString())
                .className("Test")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);

        var expectedOut = List.of("pass");

        if (!Objects.equals(expectedOut, out)) {
            throw new AssertionError("Incorrect Output, expected: " + expectedOut +
                                      ", actual: " + out);

        }
    }

    static final String nullCheckInvocation =
            "Invoke[OP=INVOKESTATIC, m=java/lang/runtime/Checks.nullCheck(Ljava/lang/Object;)V]";

    @Test
    public void testUseSideChecksForMethods(Path base) throws Exception {
        String src =
                """
                class Test {
                    class Inner {
                        Object m(Object! arg) { return null; }
                    }
                    class Inner2 extends Inner {
                        @Override
                        String m(Object arg) { return null; }
                    }
                    public static void main(String... args) {
                        Inner inner = new Test().new Inner2();
                        inner.m(null);
                    }
                }
                """;
        String expectedInstSequence =
                "Invoke[OP=INVOKESTATIC, m=java/lang/runtime/Checks.nullCheck(Ljava/lang/Object;)V]" +
                        "Invoke[OP=INVOKEVIRTUAL, m=Test$Inner.m(Ljava/lang/Object;)Ljava/lang/Object;]";
        Path out;
        for (String[] options : new String[][] {PREVIEW, USE_SITE_CHECKS_FOR_METHODS_ONLY, USE_SITE_CHECKS_FOR_METHODS_AND_FIELDS}) {
            out = testHelper(base, src, true, NullPointerException.class, options);
            if (!checkInstructionsSequence(out.resolve("Test.class"), "main", expectedInstSequence)) {
                throw new AssertionError("was expecting a null check before Inner::m invocation");
            }
        }
        out = testHelper(base, src, false, null, NO_USE_SITE_CHECKS);
        if (checkInstructionsSequence(out.resolve("Test.class"), "main", expectedInstSequence)) {
            throw new AssertionError("was not expecting a null check before Inner::m invocation");
        }

        // null checks in user site for returns
        src =
                """
                class Test {
                    class Inner {
                        Object! m(Object arg) { return ""; }
                    }
                    class Inner2 extends Inner {
                        @Override
                        String m(Object arg) { return null; }
                    }
                    public static void main(String... args) {
                        Inner inner = new Test().new Inner2();
                        inner.m(null);
                    }
                }
                """;
        expectedInstSequence =
                "Invoke[OP=INVOKEVIRTUAL, m=Test$Inner.m(Ljava/lang/Object;)Ljava/lang/Object;]" +
                        "UnboundStackInstruction[op=DUP]" +
                        "Invoke[OP=INVOKESTATIC, m=java/lang/runtime/Checks.nullCheck(Ljava/lang/Object;)V]";
        for (String[] options : new String[][] {PREVIEW, USE_SITE_CHECKS_FOR_METHODS_ONLY, USE_SITE_CHECKS_FOR_METHODS_AND_FIELDS}) {
            out = testHelper(base, src, true, NullPointerException.class, options);
            if (!checkInstructionsSequence(out.resolve("Test.class"), "main", expectedInstSequence)) {
                List<CodeElement> foundSequence = readInstructions(out.resolve("Test.class"), "main");
                String found = "";
                for (CodeElement ce : foundSequence) {
                    found += ce.toString() + "\n";
                }
                throw new AssertionError("was expecting a null check after Inner::m invocation, found: \n" + found);
            }
        }
        out = testHelper(base, src, false, null, NO_USE_SITE_CHECKS);
        if (checkInstructionsSequence(out.resolve("Test.class"), "main", expectedInstSequence)) {
            throw new AssertionError("was not expecting a null check after Inner::m invocation");
        }

        String[] testCases2 = new String[] {
                """
                class Test {
                    class Inner {
                        private Object m(Object! arg) { return null; }
                    }
                    public static void main(String... args) {
                        Inner inner = new Test().new Inner();
                        inner.m(null);
                    }
                }
                """,
                """
                class Test {
                    class Inner {
                        final Object m(Object! arg) { return null; }
                    }
                    public static void main(String... args) {
                        Inner inner = new Test().new Inner();
                        inner.m(null);
                    }
                }
                """,
                """
                class Test {
                    static class Inner {
                        static Object m(Object! arg) { return null; }
                    }
                    public static void main(String... args) {
                        Inner.m(null);
                    }
                }
                """,
                """
                class Test {
                    final class Inner {
                        Object m(Object! arg) { return null; }
                    }
                    public static void main(String... args) {
                        Inner inner = new Test().new Inner();
                        inner.m(null);
                    }
                }
                """
        };
        for (String code : testCases2) {
            out = testHelper(base, code, true, NullPointerException.class, USE_SITE_CHECKS_FOR_METHODS_AND_FIELDS);
            if (checkInstructionsSequence(out.resolve("Test.class"), "main", nullCheckInvocation)) {
                throw new AssertionError("an invocation to Checks::nullCheck was not expected");
            }
        }
    }

    private boolean checkInstructionsSequence(Path path, String methodName, String sequence) throws Exception {
        List<CodeElement> instructions = readInstructions(path, methodName);
        String foundSequence = "";
        for (CodeElement ce : instructions) {
            foundSequence += ce;
        }
        return foundSequence.contains(sequence);
    }

    private List<CodeElement> readInstructions(Path path, String methodName) throws Exception {
        ClassModel classFile = ClassFile.of().parse(path);
        List<CodeElement> result = new ArrayList<>();
        for (MethodModel method: classFile.methods()) {
            if (method.methodName().stringValue().equals(methodName)) {
                CodeAttribute codeAttr = method.findAttribute(Attributes.code()).orElse(null);
                for (CodeElement ce : codeAttr.elementList()) {
                    result.add(ce);
                }
            }
        }
        return result;
    }

    @Test
    public void testUseSideChecksForMethodsSepCompilation(Path base) throws Exception {
        testUseSiteForMethodsSeparateCompilationHelper(base,
                """
                package pkg;
                class Super {
                    Super(Object! arg) {}
                }
                """,
                """
                package pkg;
                class Super {
                    Super(Object arg) {}
                }
                """,
                """
                package pkg;
                class Client {
                    public static void main(String... args) {
                        Super sup = new Super(null);
                    }
                }
                """);
        testUseSiteForMethodsSeparateCompilationHelper(base,
                """
                package pkg;
                class Super {
                    Super(Object! arg, Object... args) {}
                }
                """,
                """
                package pkg;
                class Super {
                    Super(Object arg, Object... args) {}
                }
                """,
                """
                package pkg;
                class Client {
                    public static void main(String... args) {
                        Super sup = new Super(null, null);
                    }
                }
                """);
        testUseSiteForMethodsSeparateCompilationHelper(base,
                """
                package pkg;
                public class Super {
                    public class Inner extends Super {
                        public Inner(Object! arg) {}
                    }
                }
                """,
                """
                package pkg;
                public class Super {
                    public class Inner extends Super {
                        public Inner(Object arg) {}
                    }
                }
                """,
                """
                package pkg;
                class Client {
                    public static void main(String... args) {
                        Super.Inner inner = new Super().new Inner(null);
                    }
                }
                """);
        testUseSiteForMethodsSeparateCompilationHelper(base,
                """
                package pkg;
                public class Super {
                    public class Inner extends Super {
                        public Inner(Object! arg, Object... args) {}
                    }
                }
                """,
                """
                package pkg;
                public class Super {
                    public class Inner extends Super {
                        public Inner(Object arg, Object... args) {}
                    }
                }
                """,
                """
                package pkg;
                class Client {
                    public static void main(String... args) {
                        Super.Inner inner = new Super().new Inner(null, null);
                    }
                }
                """);
    }

    private void testUseSiteForMethodsSeparateCompilationHelper(
            Path base,
            String code1,
            String code2,
            String clientCode) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path ASrc = pkg.resolve("A");
        Path client = pkg.resolve("Client");

        tb.writeJavaFiles(ASrc, code1);
        tb.writeJavaFiles(client, clientCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .options(PREVIEW)
                .files(findJavaFiles(pkg))
                .run();

        // let's execute to check that it's producing the NPE
        System.err.println("running, this test should fail");
        String output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Client")
                .vmOptions("--enable-preview")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.STDERR);
        if (!output.startsWith("Exception in thread \"main\" java.lang.NullPointerException")) {
            throw new AssertionError("java.lang.NullPointerException expected");
        }

        // now lets change the code
        tb.writeJavaFiles(ASrc, code2);

        new JavacTask(tb)
                .outdir(out)
                .options(NO_USE_SITE_CHECKS)
                .files(findJavaFiles(pkg))
                .run();

        System.err.println("running, this test should pass");
        new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Client")
                .vmOptions("--enable-preview")
                .run(Task.Expect.SUCCESS);
    }

    @Test
    public void testUseSideChecksForFields(Path base) throws Exception {
        // separate compilation
        String ASrc1 =
                """
                package pkg;
                public class A {
                    String! a;
                    public A() {
                        this.a = "test";
                        super();
                    }
                }
                """;
        String ASrc2 =
                """
                package pkg;
                public class A {
                    String a;
                    public A() {
                        this.a = null;
                        super();
                    }
                }
                """;
        String testSrc =
                """
                package pkg;
                class Test {
                    public static void main(String... args) {
                        A a = new A();
                        System.out.println(a.a.toString());
                    }
                }
                """;
        Path out;
        String sequenceWithNullCheck =
                "Field[OP=GETFIELD, field=pkg/A.a:Ljava/lang/String;]" +
                "UnboundStackInstruction[op=DUP]" +
                "Invoke[OP=INVOKESTATIC, m=java/lang/runtime/Checks.nullCheck(Ljava/lang/Object;)V]" +
                        "Invoke[OP=INVOKEVIRTUAL, m=java/lang/String.toString()Ljava/lang/String;]";
        for (String[] options : new String[][] {USE_SITE_CHECKS_FOR_METHODS_AND_FIELDS, PREVIEW}) {
            out = testUseSiteForFieldsSeparateCompilationHelper(base, ASrc1, ASrc2, testSrc, true, options);
            if (!checkInstructionsSequence(out.resolve("pkg").resolve("Test.class"), "main", sequenceWithNullCheck)) {
                throw new AssertionError("was expecting a null check before String::toString invocation");
            }
        }

        String sequenceWithoutNullCheck =
                "Field[OP=GETFIELD, field=pkg/A.a:Ljava/lang/String;]" +
                        "Invoke[OP=INVOKEVIRTUAL, m=java/lang/String.toString()Ljava/lang/String;]";
        for (String[] options : new String[][] {USE_SITE_CHECKS_FOR_METHODS_ONLY, NO_USE_SITE_CHECKS}) {
            out = testUseSiteForFieldsSeparateCompilationHelper(base, ASrc1, ASrc2, testSrc, false, options);
            if (!checkInstructionsSequence(out.resolve("pkg").resolve("Test.class"), "main", sequenceWithoutNullCheck)) {
                throw new AssertionError("was expecting a null check before String::toString invocation");
            }
        }
    }

    private Path testUseSiteForFieldsSeparateCompilationHelper(
            Path base,
            String code1,
            String code2,
            String testCode,
            boolean shouldFailDueToNullCheck,
            String[] options) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path ASrc = pkg.resolve("A");
        Path test = pkg.resolve("Test");

        tb.writeJavaFiles(ASrc, code1);
        tb.writeJavaFiles(test, testCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        // this compilation will generate null checks in Test before accessing field A.a
        new JavacTask(tb)
                .outdir(out)
                .options(options)
                .files(findJavaFiles(pkg))
                .run();

        System.err.println("running, this test should pass");
        String output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Test")
                .vmOptions("--enable-preview")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);
        if (!output.startsWith("test")) {
            throw new AssertionError("unexpected output: " + output);
        }

        // now lets change the code
        tb.writeJavaFiles(ASrc, code2);

        new JavacTask(tb)
                .outdir(out)
                .options(options)
                .files(findJavaFiles(ASrc))
                .run();

        System.err.println("running, this test should fail");
        output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Test")
                .vmOptions("--enable-preview")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.STDERR);

        // we need to check that the NPE is due to an invocation to j.l.r.Checks::nullCheck
        if (shouldFailDueToNullCheck) {
            if (!output.contains("java.lang.NullPointerException") &&
                    !output.contains("java.base/java.lang.runtime.Checks.nullCheck")) {
                throw new AssertionError("unexpected output: " + output);
            }
        } else {
            if (!output.startsWith("Exception in thread \"main\" java.lang.NullPointerException: Cannot invoke \"String.toString()\" because \"<local1>.a\" is null")) {
                throw new AssertionError("unexpected output: " + output);
            }
        }
        return out;
    }

    private Path compile(Path base,
                         String code,
                         String className,
                         String pakageName,
                         String[] options) throws Exception {
        Path src = base.resolve("src");
        Path pkg = pakageName != null ? src.resolve("pakageName") : src;
        Path ASrc = pkg.resolve(className);

        tb.writeJavaFiles(ASrc, code);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .options(options) // equivalent to just using PREVIEW options
                .files(findJavaFiles(pkg))
                .run();
        return out;
    }

    @Test
    public void testTypeCasts(Path base) throws Exception {
        String[] testCases = new String[] {
                """
                class Test {
                    public static void main(String... args) {
                        Object s = null;
                        Object o = (String!)(Object!) s; // NPE, cast
                    }
                }
                """,
                """
                import java.io.*;
                class Test {
                    public static void main(String... args) {
                        Object s = null;
                        Object o = (String!)(CharSequence!)(Serializable!) s; // NPE, cast
                    }
                }
                """,
                """
                class Test {
                    class OtherClass {
                        Object! m() {
                            return "";
                        }
                    }
                    public static void main(String... args) {
                        OtherClass oc = new Test().new OtherClass();
                        String! s = (String!)oc.m();
                    }
                }
                """,
                """
                import java.io.*;
                class Test {
                    public static void main(String... args) {
                        Object s = null;
                        Object! o = (String)(CharSequence)(Serializable!) s;
                    }
                }
                """
        };
        for (String testCase : testCases) {
            Path out = compile(base, testCase, "Test", null, PREVIEW);
            List<CodeElement> instructions = readInstructions(out.resolve("Test.class"), "main");
            int numberOfNullChecks = 0;
            for (CodeElement ce : instructions) {
                if (ce.toString().equals(nullCheckInvocation)) {
                    numberOfNullChecks++;
                }
            }
            if (numberOfNullChecks != 1) {
                throw new AssertionError("was expecting only one invocation to Checks::nullCheck");
            }
        }
    }
}
