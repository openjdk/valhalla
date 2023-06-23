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

/**
 * BangTypesCompilationTests
 *
 * @test
 * @enablePreview
 * @summary compilation tests for bang types
 * @library /lib/combo /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.jdeps/com.sun.tools.classfile
 * @run testng/othervm BangTypesCompilationTests
 */
import java.util.List;
import java.util.function.Consumer;

import javax.tools.Diagnostic;

import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;

import static org.testng.Assert.assertEquals;

@Test
public class BangTypesCompilationTests extends CompilationTestCase {
    private static String[] EMPTY_OPTIONS = {};
    private static String[] LINT_OPTIONS = { "-Xlint:null" };

    public BangTypesCompilationTests() {
        setDefaultFilename("Test.java");
    }

    enum TestResult {
        COMPILE_OK,
        COMPILE_WITH_WARNING,
        ERROR
    }

    void testHelper(String[] compilerOptions, String code) {
        testHelper(compilerOptions, "", 1, TestResult.COMPILE_OK, code, null);
    }

    void testHelper(String[] compilerOptions, String code, Consumer<Diagnostic<?>> diagConsumer) {
        testHelper(compilerOptions, "", 1, TestResult.COMPILE_OK, code, diagConsumer);
    }

    void testHelper(String[] compilerOptions, String diagsMessage, int diagsCount, TestResult testResult, String code,
                    Consumer<Diagnostic<?>> diagConsumer) {
        setCompileOptions(compilerOptions);
        try {
            if (testResult != TestResult.COMPILE_OK) {
                if (testResult == TestResult.COMPILE_WITH_WARNING) {
                    assertOKWithWarning(diagsMessage, diagsCount, code);
                } else {
                    assertFail(diagsMessage, code);
                }
            } else {
                if (diagConsumer == null) {
                    assertOK(code);
                } else {
                    assertOK(diagConsumer, code);
                }
            }
        } catch (Throwable t) {
            System.err.println("error while compiling code:\n" + code);
            throw t;
        }
    }

    void testList(List<DiagAndCode> testList) {
        for (DiagAndCode diagAndCode : testList) {
            if (diagAndCode.result == Result.Clean) {
                testHelper(LINT_OPTIONS, diagAndCode.code);
            } else if (diagAndCode.result == Result.Warning) {
                testHelper(LINT_OPTIONS, diagAndCode.diag, diagAndCode.diagsCount, TestResult.COMPILE_WITH_WARNING, diagAndCode.code, null);
                testHelper(EMPTY_OPTIONS, diagAndCode.code,
                        d -> {
                            if (d.getKind() == Diagnostic.Kind.WARNING) {
                                // shouldn't issue any warnings if the -Xlint:null option is not passed
                                throw new AssertionError("unexpected warning for " + diagAndCode.code);
                            }
                        });
            } else {
                testHelper(EMPTY_OPTIONS, diagAndCode.diag, diagAndCode.diagsCount, TestResult.ERROR, diagAndCode.code, null);
            }
            if (diagAndCode.result != Result.Error) {
                testHelper(EMPTY_OPTIONS, diagAndCode.code);
            }
        }
    }

    enum Result { Warning, Error, Clean}

    record DiagAndCode(String code, Result result, String diag, int diagsCount) {
        DiagAndCode(String code, Result result, String diag) {
            this(code, result, diag, 1);
        }
    }

    public void testErrorNonNullableCantBeAssignedNull() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                class Foo {
                                    String! s = null;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.cannot.be.assigned.null"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String[]! s = null;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.cannot.be.assigned.null")
                )
        );
    }

    public void testWarnUninitialized() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                class Foo {
                                    String! s;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String[]! s;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String![]! s;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String![]![]! s;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String[][]! s;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String[][][]! s;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.non.nullable.should.be.initialized")
                )
        );
    }

    public void testUncheckedNullnessConversions () {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m(String! s1, String s3) {
                                        s1 = s3;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m(Object! s1, String s3) {
                                        s1 = s3;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m(String! s1, String s3) {
                                        s3 = s1;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Foo<T extends String!> {
                                    Foo<String> f2;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo<T extends Object!> {
                                    Foo<String> f2;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),

                        // wildcards
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Foo {
                                    void test(List<? extends String!> ls1, List<? extends String> ls3) {
                                        ls1 = ls3;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Foo {
                                    void test(List<? extends Object!> ls1, List<? extends String> ls3) {
                                        ls1 = ls3;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Foo {
                                    void test(List<? extends String!> ls1, List<? extends String> ls3) {
                                        ls3 = ls1;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Foo {
                                    void test(List<? extends String!> ls1, List<? extends Object> ls3) {
                                        ls3 = ls1;
                                    }
                                }
                                """,
                                Result.Clean,
                                "")
                )
        );
    }

    public void testNoWarnings() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Foo {
                                     void m(List<? super String!> ls1) {}
                                     void test(List<? super String!> ls2) {
                                         m(ls2);
                                     }
                                }
                                """,
                                Result.Clean,
                                "" /* no warnings in this case */)
                )
        );
    }

    public void testOverridingWarnings() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                abstract class A {
                                    abstract String! lookup(String arg);
                                }
                                                                
                                abstract class B extends A {
                                    abstract String lookup(String arg);
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.overrides.with.different.nullness.1"),
                        new DiagAndCode(
                                """
                                abstract class A {
                                    abstract String lookup(String! arg);
                                }

                                abstract class B extends A {
                                    abstract String lookup(String arg);
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.overrides.with.different.nullness.2")
                )
        );
    }
}
