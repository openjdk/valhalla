/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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
 * NullabilityCompilationTests
 *
 * @test
 * @bug 8339357 8340027
 * @enablePreview
 * @summary compilation tests for bang types
 * @library /lib/combo /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run junit NullabilityCompilationTests
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.List;
import java.util.Set;

import javax.tools.Diagnostic;

import java.lang.classfile.ClassFile;
import java.lang.reflect.AccessFlag;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;

import org.junit.jupiter.api.Test;
import tools.javac.combo.CompilationTestCase;

import toolbox.ToolBox;
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;

public class NullabilityCompilationTests extends CompilationTestCase {
    private static String[] PREVIEW_OPTIONS = {
            "--enable-preview", "-source", Integer.toString(Runtime.version().feature())};
    private static String[] PREVIEW_PLUS_LINT_OPTIONS = {
            "--enable-preview", "-source", Integer.toString(Runtime.version().feature()),
            "-Xlint:null", "-XDrawDiagnostics" };

    ToolBox tb;

    public NullabilityCompilationTests() {
        setDefaultFilename("Test.java");
        tb = new ToolBox();
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
                testHelper(PREVIEW_PLUS_LINT_OPTIONS, diagAndCode.code);
            } else if (diagAndCode.result == Result.Warning) {
                testHelper(PREVIEW_PLUS_LINT_OPTIONS, diagAndCode.diag, diagAndCode.diagsCount, TestResult.COMPILE_WITH_WARNING, diagAndCode.code, null);
                testHelper(PREVIEW_OPTIONS, diagAndCode.code,
                        d -> {
                            if (d.getKind() == Diagnostic.Kind.WARNING) {
                                // shouldn't issue any warnings if the -Xlint:null option is not passed
                                throw new AssertionError("unexpected warning for " + diagAndCode.code);
                            }
                        });
            } else {
                testHelper(PREVIEW_OPTIONS, diagAndCode.diag, diagAndCode.diagsCount, TestResult.ERROR, diagAndCode.code, null);
            }
            if (diagAndCode.result != Result.Error) {
                testHelper(PREVIEW_OPTIONS, diagAndCode.code);
            } else {
                testHelper(PREVIEW_OPTIONS, diagAndCode.diag, diagAndCode.diagsCount, TestResult.ERROR, diagAndCode.code, null);
            }
        }
    }

    enum Result { Warning, Error, Clean}

    record DiagAndCode(String code, Result result, String diag, int diagsCount) {
        DiagAndCode(String code, Result result, String diag) {
            this(code, result, diag, 1);
        }
    }


    @Test
    void testErrorUninitialized() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                value class Point { }
                                class Foo {
                                    Point! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                value class Point { }
                                class Foo {
                                    Point[]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo[]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                value class Point { }
                                class Foo {
                                    Point[]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                value class Point { }
                                class Foo {
                                    Point[][]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo[][]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super")
                )
        );
    }

    @Test
    void testLintWarnings() throws Exception {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m() {
                                        String! s = null;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m() {
                                        String[]! s = null;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m() {
                                        String! s = (String!)null;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m() {
                                        String[]! s = (String[]!)null;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m() {
                                        g(null);
                                    }
                                    void g(String! s) { }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    void m() {
                                        g(null);
                                    }
                                    void g(String[]! s) { }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String! m() {
                                        return null;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo {
                                    String[]! m() {
                                        return null;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.suspicious.nullness.conversion",
                                1),

                        // override warnings
                        new DiagAndCode(
                                """
                                class Test {
                                    String! m(String s) { return ""; }
                                }
                                class Sub extends Test {
                                    @Override
                                    String m(String s) { return null; }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.incompatible.null.restrictions",
                                1),
                        new DiagAndCode(
                                """
                                class Test {
                                    String m(String! s, Integer! i) { return ""; }
                                }
                                class Sub extends Test {
                                    @Override
                                    String m(String s, Integer i) { return null; }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.incompatible.null.restrictions",
                                2),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Test {
                                    String m(List<String>! s, List<Integer>! i) { return ""; }
                                }
                                class Sub extends Test {
                                    @Override
                                    String m(List s, List i) { return null; }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.incompatible.null.restrictions",
                                2),
                        new DiagAndCode(
                                """
                                interface I {
                                    void m(String! s, Integer! i);
                                }
                                class Test {
                                    I i = (String s, Integer i) -> {};
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.incompatible.null.restrictions",
                                2),
                        new DiagAndCode(
                                """
                                interface Getter {
                                    String! name();
                                }
                                // the generated accessor will return String not String!
                                record R(String name) implements Getter {}
                                """,
                                Result.Warning,
                                "compiler.warn.incompatible.null.restrictions",
                                1)
                )
        );

        // some separate compilation tests
        Path base = Paths.get(System.getProperty("user.dir")).resolve("testLintWarnings");
        testSeparateCompilationHelper(
                base,
                """
                class Super {
                    String! m(String s) { return ""; }
                }
                """,
                """
                class Sub extends Super {
                    @Override
                    String m(String s) { return null; }
                }
                """,
                "Sub.java:3:5: compiler.warn.incompatible.null.restrictions: (compiler.misc.return.type.nullability.mismatch: java.lang.String, java.lang.String)\n" +
                "1 warning\n"
        );
        testSeparateCompilationHelper(
                base,
                """
                class Super {
                    String m(String! s, Integer! i) { return ""; }
                }
                """,
                """
                class Sub extends Super {
                    @Override
                    String m(String s, Integer i) { return null; }
                }
                """,
                "Sub.java:3:14: compiler.warn.incompatible.null.restrictions: (compiler.misc.argument.type.nullability.mismatch: java.lang.String, java.lang.String)\n" +
                "Sub.java:3:24: compiler.warn.incompatible.null.restrictions: (compiler.misc.argument.type.nullability.mismatch: java.lang.Integer, java.lang.Integer)\n" +
                "2 warnings\n"
        );
        testSeparateCompilationHelper(
                base,
                """
                import java.util.*;
                class Super {
                    String m(List<String>! s, List<Integer>! i) { return ""; }
                }
                """,
                """
                import java.util.*;
                class Sub extends Super {
                    @Override
                    String m(List s, List i) { return null; }
                }
                """,
                "Sub.java:4:14: compiler.warn.incompatible.null.restrictions: (compiler.misc.argument.type.nullability.mismatch: java.util.List, java.util.List<java.lang.String>)\n" +
                "Sub.java:4:22: compiler.warn.incompatible.null.restrictions: (compiler.misc.argument.type.nullability.mismatch: java.util.List, java.util.List<java.lang.Integer>)\n" +
                "2 warnings\n"
        );
        testSeparateCompilationHelper(
                base,
                """
                interface Super {
                    void m(String! s, Integer! i);
                }
                """,
                """
                class Sub {
                    Super i = (String s, Integer i) -> {};
                }
                """,
                "Sub.java:2:16: compiler.warn.incompatible.null.restrictions: (compiler.misc.lambda.argument.type.nullability.mismatch: java.lang.String, java.lang.String)\n" +
                "Sub.java:2:26: compiler.warn.incompatible.null.restrictions: (compiler.misc.lambda.argument.type.nullability.mismatch: java.lang.Integer, java.lang.Integer)\n" +
                "2 warnings\n"
        );
    }

    private void testSeparateCompilationHelper(
            Path base,
            String superClass,
            String subClass,
            String expectedOutput) throws Exception {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src, superClass);
        tb.writeJavaFiles(src, subClass);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        // first, let's compile the super class only
        new JavacTask(tb)
                .outdir(out)
                .options(PREVIEW_PLUS_LINT_OPTIONS)
                .files(tb.findFiles("Super.java", src))
                .run();

        // now the subclass only
        String output = new JavacTask(tb)
                .outdir(out)
                .classpath(out.toString())
                .options(PREVIEW_PLUS_LINT_OPTIONS)
                .files(tb.findFiles("Sub.java", src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        Assert.check(output.equals(expectedOutput), "unexpected output, found:\n" + output);
    }

    @Test
    void testNoWarnings() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                value class Point {}
                                class C<T> {
                                    T x = null;
                                    void m() {
                                        String r = new C<String>().x;
                                        Point p = new C<Point>().x;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                value class Point {}
                                class C<T> {
                                    T x = null;
                                    void m() {
                                        String r = new C<String>().x;
                                        Point p = new C<Point>().x;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class C<T> {
                                    T x = null;
                                    void set(T arg) { x = arg; }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test<T> {
                                    T field;
                                    void foo(T t) {
                                        field = t;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                import java.lang.invoke.*;
                                class Cell {
                                    final void reset() {
                                        // we are testing that the compiler won't infer the arguments of
                                        // VarHandle::setVolatile as (Cell, String!)
                                        VALUE.setVolatile(this, "");
                                    }
                                    final void reset(String identity) {
                                        // if that were the case, see comment above, then this invocation would generate
                                        // a warning, VarHandle::setVolatile is a polymorphic signature method
                                        VALUE.setVolatile(this, identity);
                                    }

                                    private static final VarHandle VALUE;
                                    static {
                                        try {
                                            MethodHandles.Lookup l = MethodHandles.lookup();
                                            VALUE = l.findVarHandle(Cell.class, "value", long.class);
                                        } catch (ReflectiveOperationException e) {
                                            throw new ExceptionInInitializerError(e);
                                        }
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                import java.lang.invoke.*;
                                class Cell {
                                    final void reset() {
                                        VALUE.setVolatile(this, 0L);
                                    }
                                    final void reset(long identity) {
                                        VALUE.setVolatile(this, identity);
                                    }

                                    private static final VarHandle VALUE;
                                    static {
                                        try {
                                            MethodHandles.Lookup l = MethodHandles.lookup();
                                            VALUE = l.findVarHandle(Cell.class, "value", long.class);
                                        } catch (ReflectiveOperationException e) {
                                            throw new ExceptionInInitializerError(e);
                                        }
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    void test1() {
                                        String[][] arr_local = null;
                                        arr_local = new String[3][4];
                                        arr_local = new String![][] { { "" } };
                                    }

                                    void test2() {
                                        String[][]! arr_local = new String[0][0];
                                        arr_local = new String[3][4];
                                        arr_local = new String![][] { { "" } };
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                value class Test {
                                    void m(Test t1, Test[] t2, Test[][] t3, Test[][][] t4) {
                                        Test! l1 = (Test!) t1;
                                        Test[]! l2 = (Test[]!) t2;
                                        Test[][]! l3 = (Test[][]!) t3;
                                        Test[][][]! l4 = (Test[][][]!) t4;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    void test() {
                                        String! s_nonnull = "";
                                        String s = null;
                                        s = s_nonnull;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    void test(Box b) {
                                        switch (b) {
                                            case Box(String! nonNull) -> {}
                                            case Box(String isNull) -> {}
                                        }
                                    }
                                    record Box(String str) {}
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    void test(Box b) {
                                        switch (b) {
                                            case Box(String! nonNull) -> {}
                                            case Box(var v) -> {}
                                        }
                                    }
                                    record Box(String str) {}
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    void test(Box b) {
                                        switch (b) {
                                            case Box(String! nonNull) -> {}
                                            case Box(_) -> {}
                                        }
                                    }
                                    record Box(String str) {}
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                String! str = "";
                                void main() {
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                String! test() {
                                    return "";
                                }
                                void main() {
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                interface I {
                                    void m(String! s); // should no crash javac
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                abstract class A {
                                    abstract void m(String! s); // should no crash javac
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    void work(String! a, int b, boolean c) {
                                    }
                                    public void run(Test t, String[] args, int a, boolean b) {
                                        t.work(args[0], a, b);
                                    }
                                }
                                """,
                                Result.Clean,
                                ""
                        )
                )
        );
    }

    @Test
    void testNonNullableFieldsAreInitializedBeforeSuper() throws Exception {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                class Test {
                                    Object! o;
                                    Test() {
                                        super();
                                        o = new Object();
                                    }
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                class Test {
                                    Object! o;
                                }
                                """,
                                Result.Error,
                                "compiler.err.null.restricted.field.not.have.been.initialized.before.super"),
                        new DiagAndCode(
                                """
                                class Test {
                                    Object! o;
                                    Test() {
                                        o = new Object();
                                        super();
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    Object! o;
                                    Test() {
                                        o = new Object();
                                        super();
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    Object! o;
                                    Test() {
                                        o = new Object();
                                        super();
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        // static fields
                        new DiagAndCode(
                                """
                                class Test {
                                    static Object! o;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Test {
                                    static Object! o = new Object();
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    static Object! o;
                                    static {
                                        o = new Object();
                                    }
                                }
                                """,
                                Result.Clean,
                                "")
                )
        );

        for (String source : List.of(
                """
                class Test {
                    Object! o;
                    Test() {
                        o = new Object();
                        super();
                    }
                }
                """,
                """
                class Test {
                    static Object! o = new Object();
                }
                """
        )) {
            File dir = assertOK(true, source);
            for (final File fileEntry : dir.listFiles()) {
                var classFile = ClassFile.of().parse(fileEntry.toPath());
                for (var field : classFile.fields()) {
                    if (!field.flags().has(AccessFlag.STATIC)) {
                        Set<AccessFlag> fieldFlags = field.flags().flags();
                        Assert.check(fieldFlags.size() == 2 &&
                                fieldFlags.contains(AccessFlag.STRICT_INIT) &&
                                fieldFlags.contains(AccessFlag.NULL_CHECKED));
                    } else {
                        Set<AccessFlag> fieldFlags = field.flags().flags();
                        Assert.check(fieldFlags.size() == 3 &&
                                fieldFlags.contains(AccessFlag.STRICT_INIT) &&
                                fieldFlags.contains(AccessFlag.STATIC) &&
                                fieldFlags.contains(AccessFlag.NULL_CHECKED));
                    }
                }
            }
        }
    }

    @Test
    void testPatternDominance() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                class Test {
                                    void test(Box b) {
                                        switch (b) {
                                            case Box(String nullAllowed) -> {}
                                            case Box(String! notNull) -> {}
                                        }
                                    }
                                    record Box(String str) {}
                                }
                                """,
                                Result.Error,
                                "compiler.err.pattern.dominated"),
                        new DiagAndCode(
                                """
                                class Test {
                                    void test(Box b) {
                                        switch (b) {
                                            case Box(var v) -> {}
                                            case Box(String! notNull) -> {}
                                        }
                                    }
                                    record Box(String str) {}
                                }
                                """,
                                Result.Error,
                                "compiler.err.pattern.dominated"),
                        new DiagAndCode(
                                """
                                class Test {
                                    void test(Box b) {
                                        switch (b) {
                                            case Box(_) -> {}
                                            case Box(String! notNull) -> {}
                                        }
                                    }
                                    record Box(String str) {}
                                }
                                """,
                                Result.Error,
                                "compiler.err.pattern.dominated")
                )
        );
    }

    @Test
    void testPatternExhaustiveness() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                class Test {
                                    void test(Box b) {
                                        switch (b) {
                                            case Box(String! notNull) -> {}
                                        }
                                    }
                                    record Box(String str) {}
                                }
                                """,
                                Result.Clean,
                                "") //is exhaustive
                )
        );
    }
}
