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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private static String[] PREVIEW_OPTIONS = {
            "--enable-preview",
            "-source", Integer.toString(Runtime.version().feature())
    };

    private static String[] PREVIEW_PLUS_NO_USE_SITE_OPTIONS = {
            "--enable-preview",
            "-source", Integer.toString(Runtime.version().feature()),
            "-XDnoUseSiteNullChecks"
    };

    private void testHelper(Path base, String testCode, boolean shouldFail, Class<?> expectedError) throws Exception {
        testHelper(base, testCode, shouldFail, expectedError, PREVIEW_OPTIONS);
    }

    private void testHelper(Path base, String testCode, boolean shouldFail, Class<?> expectedError, String[] compilerOptions) throws Exception {
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
            .options(PREVIEW_OPTIONS)
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

    @Test
    public void testUseSideChecksForMethods(Path base) throws Exception {
        String[] testCases = new String[] {
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
                """,
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
                """,
                """
                class Test {
                    class Inner {
                        Object m(Object! arg, Object... args) { return null; }
                    }
                    class Inner2 extends Inner {
                        @Override
                        String m(Object arg, Object... args) { return null; }
                    }
                    public static void main(String... args) {
                        Inner inner = new Test().new Inner2();
                        inner.m(null, null);
                    }
                }
                """,
                """
                class Test {
                    class Inner {
                        Object! m(Object arg, Object... args) { return ""; }
                    }
                    class Inner2 extends Inner {
                        @Override
                        String m(Object arg, Object... args) { return null; }
                    }
                    public static void main(String... args) {
                        Inner inner = new Test().new Inner2();
                        inner.m(null, null);
                    }
                }
                """
        };
        for (String code : testCases) {
            testHelper(base, code, true, NullPointerException.class);
            testHelper(base, code, false, null, PREVIEW_PLUS_NO_USE_SITE_OPTIONS);
        }
    }

    @Test
    public void testUseSideChecksForMethodsSepCompilation(Path base) throws Exception {
        testSeparateCompilationHelper(base,
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
        testSeparateCompilationHelper(base,
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
        testSeparateCompilationHelper(base,
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
        testSeparateCompilationHelper(base,
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

    private void testSeparateCompilationHelper(
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
                .options(PREVIEW_OPTIONS)
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
                .options(PREVIEW_PLUS_NO_USE_SITE_OPTIONS)
                .files(findJavaFiles(pkg))
                .run();

        System.err.println("running, this test should pass");
        new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Client")
                .vmOptions("--enable-preview")
                .run(Task.Expect.SUCCESS);
    }
}
