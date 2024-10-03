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
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main RuntimeNullChecks
 */

import java.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;
import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

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
                    public static void main(String... args) {
                        String s = null;
                        String! o;
                        o = s; // NPE at runtime, assignment, it doesn't stress the same code path as the case above
                    }
                }
                """,
                """
                class Test {//
                    public static void main(String... args) {
                        String s = null;
                        String![] sr = new String![10];
                        sr[0] = s; // NPE at runtime, assignment
                    }
                }
                """,
                """
                class Test {
                    static String id(String! arg) { return arg; }
                    public static void main(String... args) {
                        String s = null;
                        Object o = id(s); // NPE at runtime, method invocation
                    }
                }
                """,
                """
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
                """,
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
                    class Inner {
                        class MyPrivilegedAction<T> {
                            MyPrivilegedAction(Object! o) {}
                            T run() {
                                return null;
                            }
                        }
                    }
                    public <T> T doPrivileged(Inner.MyPrivilegedAction<T> action) {
                        return action.run();
                    }
                    boolean isSystemProperty(String key, String value, String def, Object? o) {
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
                    class Inner {
                        class MyPrivilegedAction<T> {
                            MyPrivilegedAction(String s, Object!... o) {}
                        }
                    }
                    public <T> T doPrivileged(Inner.MyPrivilegedAction<T> action) { return null; }
                    boolean isSystemProperty(Inner inner, Object o) {
                        return doPrivileged( inner.new MyPrivilegedAction<Boolean>("", o) {} );
                    }
                    void doTest() {
                        Inner inner = new Inner();
                        isSystemProperty(inner, null);
                    }
                    public static void main(String... args) {
                        Test test = new Test();
                        test.doTest();
                    }
                }
                """,
                """
                class Test {
                    class Inner {
                        class MyPrivilegedAction<T> {
                            MyPrivilegedAction(String s, Object!... o) {}
                        }
                    }
                    public <T> T doPrivileged(Inner.MyPrivilegedAction<T> action) { return null; }
                    boolean isSystemProperty(Inner inner, Object o) {
                        return doPrivileged( inner.new MyPrivilegedAction<Boolean>("", o, o) {} );
                    }
                    void doTest() {
                        Inner inner = new Inner();
                        isSystemProperty(inner, null);
                    }
                    public static void main(String... args) {
                        Test test = new Test();
                        test.doTest();
                    }
                }
                """,
                """
                class Test {
                    class Inner {
                        class MyPrivilegedAction<T> {
                            MyPrivilegedAction(String s, Object!... o) {}
                        }
                    }
                    public <T> T doPrivileged(Inner.MyPrivilegedAction<T> action) { return null; }
                    boolean isSystemProperty(Inner inner, Object o) {
                        return doPrivileged( inner.new MyPrivilegedAction<Boolean>("", new Object(), o) {} );
                    }
                    void doTest() {
                        Inner inner = new Inner();
                        isSystemProperty(inner, null);
                    }
                    public static void main(String... args) {
                        Test test = new Test();
                        test.doTest();
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
            "--enable-preview", "-source", Integer.toString(Runtime.version().feature())};

    private void testHelper(Path base, String testCode, boolean shouldFail, Class<?> expectedError) throws Exception {
        Path src = base.resolve("src");
        Path testSrc = src.resolve("Test");

        tb.writeJavaFiles(testSrc, testCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .options(PREVIEW_OPTIONS)
                .files(findJavaFiles(src))
                .run();

        if (shouldFail) {
            // let's check that we get the expected error
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
        } else {
            new JavaTask(tb)
                    .classpath(out.toString())
                    .vmOptions("--enable-preview")
                    .classArgs("Test")
                    .run(Task.Expect.SUCCESS);
        }
    }
}
