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
 *      jdk.jdeps/com.sun.tools.classfile
 * @run junit NullabilityCompilationTests
 */

import java.io.File;
import java.util.function.Consumer;
import java.util.List;
import java.util.Set;

import javax.tools.Diagnostic;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Field;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;

import org.junit.jupiter.api.Test;
import tools.javac.combo.CompilationTestCase;

public class NullabilityCompilationTests extends CompilationTestCase {
    private static String[] PREVIEW_OPTIONS = {
            "--enable-preview", "-source", Integer.toString(Runtime.version().feature())};
    private static String[] PREVIEW_PLUS_LINT_OPTIONS = {
            "--enable-preview", "-source", Integer.toString(Runtime.version().feature()),
            "-Xlint:null" };

    public NullabilityCompilationTests() {
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
    void testErrorNonNullableCantBeAssignedNull() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point! s = null;
                                }
                                """,
                                Result.Error,
                                "compiler.err.prob.found.req"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo! s = null;
                                }
                                """,
                                Result.Error,
                                "compiler.err.prob.found.req"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point[]! s = null;
                                }
                                """,
                                Result.Error,
                                "compiler.err.prob.found.req"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo[]! s = null;
                                }
                                """,
                                Result.Error,
                                "compiler.err.prob.found.req"),
                        new DiagAndCode(
                                """
                                import java.util.function.*;
                                class Test<T> {
                                    void m() {
                                        Supplier<? extends T> factory = nullFactory();
                                    }
                                    Supplier<? extends T!> nullFactory() { return () -> null; }
                                }
                                """,
                                Result.Error,
                                "compiler.err.prob.found.req"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class MyList<T> {
                                    void add(T e) {}
                                }
                                class Test {
                                    void m(MyList<? super Point!> ls) {
                                        ls.add(null);
                                    }
                                }
                                """,
                                Result.Error,
                                "compiler.err.prob.found.req")
                )
        );
    }


    @Test
    void testErrorUninitialized() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point[]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo[]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point![]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo![]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point![]![]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo![]![]! s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point[]![] s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo[]![] s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    Point[]![][] s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
                        new DiagAndCode(
                                """
                                class Foo {
                                    Foo[]![][] s;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized")
                )
        );
    }

    @Test
    void testUncheckedNullnessConversions () {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    void m(Point! s1, Point? s3) {
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
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class Foo {
                                    void m(Point! s1, Point s3) {
                                        s3 = s1;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Foo<T extends String!> {
                                    Foo<String?> f2;
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Foo<T extends String!> {
                                    Foo<String> f2;
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Foo<T extends Object!> {
                                    Foo<String?> f2;
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
                                Result.Clean,
                                ""),
                        // wildcards
                        new DiagAndCode(
                                """
                                import java.util.*;
                                value class Point { public implicit Point(); }
                                class Foo {
                                    void test(List<? extends Point!> ls1, List<? extends Point> ls3) {
                                        ls1 = ls3;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                value class Point { public implicit Point(); }
                                class Foo {
                                    void test(List<? extends Point!> ls1, List<? extends Point?> ls3) {
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
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Foo {
                                    void test(List<? extends Object!> ls1, List<? extends String?> ls3) {
                                        ls1 = ls3;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Test {
                                    static value class Atom {}
                                    static class Box<X> {}
                                    void test(Box<? extends Atom!> t1, Box<Atom> t2) {
                                        t1 = t2;
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    static value class Atom {}
                                    static class Box<X> {}
                                    void test(Box<? extends Atom!> t1, Box<Atom?> t2) {
                                        t1 = t2;
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                class Wrapper<T> {}
                                class Test<T> {
                                    Wrapper<T> newWrapper() { return null; }
                                    void m() {
                                        Wrapper<T!> w = newWrapper();
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Wrapper<T> {}
                                class Test<T> {
                                    Wrapper<T?> newWrapper() { return null; }
                                    void m() {
                                        Wrapper<T!> w = newWrapper();
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.function.*;
                                class Test {
                                    void plot(Function<String, String> f) {}
                                    void m(Function<String!, String> gradient) {
                                        plot(gradient);
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                import java.util.function.*;
                                class Test {
                                    void plot(Function<String?, String> f) {}
                                    void m(Function<String!, String> gradient) {
                                        plot(gradient);
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.function.*;
                                class Test {
                                    void plot(Function<String!, String> f) {}
                                    void m(Function<String, String> gradient) {
                                        plot(gradient);
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                import java.util.function.*;
                                class Test {
                                    void plot(Function<String!, String> f) {}
                                    void m(Function<String?, String> gradient) {
                                        plot(gradient);
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.function.*;
                                class Test<T> {
                                    void m() {
                                        Supplier<? extends T!> factory = nullFactory();
                                    }
                                    Supplier<? extends T> nullFactory() { return () -> null; }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Test<T> {
                                    Set<Map.Entry<String, T>> allEntries() { return null; }
                                    void m() {
                                        Set<Map.Entry<String, T!>> entries = allEntries();
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.function.*;
                                class Test<T> {
                                    T field;
                                    void foo(Consumer<? super T!> action) {
                                        action.accept(field);
                                    }
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.unchecked.nullness.conversion",
                                1),
                        new DiagAndCode(
                                """
                                import java.util.*;
                                class Test<T> {
                                    Set<Map.Entry<String, T!>> allEntries() { return null; }
                                    void m() {
                                        Set<Map.Entry<String, T>> entries = allEntries();
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    class Box<X> {}
                                    static value class Point { public implicit Point(); }
                                    @SafeVarargs
                                    private <Z> Z make_box_uni(Z... bs) {
                                        return bs[0];
                                    }
                                    void test(Box<Point!> bref, Box<Point> bval) {
                                        Box<? extends Point!> res = make_box_uni(bref, bval);
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    class Box<X> {}
                                    static value class Point { public implicit Point(); }
                                    @SafeVarargs
                                    private <Z> Z make_box_uni(Z... bs) {
                                        return bs[0];
                                    }
                                    void test(Box<Point!> bref, Box<Point?> bval) {
                                        Box<? extends Point!> res = make_box_uni(bref, bval);
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

    @Test
    void testNoWarnings() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                interface Shape {}
                                value class Point implements Shape { public implicit Point(); }
                                class Box<T> {}
                                class Test {
                                    void m(Box<Point!> lp) {
                                        foo(lp);
                                    }
                                    void foo(Box<? extends Shape> ls) {}
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                interface Shape {}
                                value class Point implements Shape { public implicit Point(); }
                                class Box<T> {}
                                class Test {
                                    void m(Box<Shape> lp) {
                                        foo(lp);
                                    }
                                    void foo(Box<? super Point!> ls) {}
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
                                value class Point {}
                                class MyList<T> {
                                    static <E> MyList<E> of(E e1) {
                                        return null;
                                    }
                                }
                                class Test {
                                    void m() {
                                        MyList.of(new Point!());
                                    }
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                class MyCollection<T> {}
                                class MyList<T> extends MyCollection<T!> {
                                    static <E> MyList<E> of(E e1) {
                                        return null;
                                    }
                                }
                                class Test {
                                    void m() {
                                        MyCollection<Point> mpc = MyList.of(new Point!());
                                    }
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
                                value class Test {
                                    public implicit Test();
                                    void m(Test t1, Test[] t2, Test[][] t3, Test[][][] t4) {
                                        Test! l1 = (Test!) t1;
                                        Test![] l2 = (Test![]) t2;
                                        Test![][] l3 = (Test![][]) t3;
                                        Test![][][] l4 = (Test![][][]) t4;

                                        Test[]! l5 = (Test[]!) t2;
                                        Test[][]! l6 = (Test[][]!) t3;
                                        Test[][][]! l7 = (Test[][][]!) t4;

                                        Test[]![]! l8 = (Test[]![]!) t3;
                                        Test[]![]![]! l9 = (Test[]![]![]!) t4;
                                    }
                                }
                                """,
                                Result.Clean,
                                "")
                )
        );
    }

    @Test
    void testOverridingWarnings() {
        testList(
                List.of(
                        new DiagAndCode(
                                """
                                abstract class A {
                                    abstract String! lookup(String arg);
                                }

                                abstract class B extends A {
                                    abstract String? lookup(String arg);
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.overrides.with.different.nullness.1"),
                        new DiagAndCode(
                                """
                                value class Point { public implicit Point(); }
                                abstract class A {
                                    abstract String lookup(Point! arg);
                                }

                                abstract class B extends A {
                                    abstract String lookup(Point? arg);
                                }
                                """,
                                Result.Warning,
                                "compiler.warn.overrides.with.different.nullness.2")
                )
        );
    }

    @Test
    void testNonNullableFieldsAreStrict() throws Exception {
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
                                "compiler.err.cant.ref.after.ctor.called"),
                        new DiagAndCode(
                                """
                                class Test {
                                    Object! o;
                                }
                                """,
                                Result.Error,
                                "compiler.err.non.nullable.should.be.initialized"),
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
                                    Object! o = new Object();
                                    Test() {}
                                }
                                """,
                                Result.Clean,
                                ""),
                        new DiagAndCode(
                                """
                                class Test {
                                    Object! o = new Object();
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
                    Object! o = new Object();
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
                ClassFile classFile = ClassFile.read(fileEntry);
                for (Field field : classFile.fields) {
                    if (!field.access_flags.is(Flags.STATIC)) {
                        Set<String> fieldFlags = field.access_flags.getFieldFlags();
                        Assert.check(fieldFlags.size() == 1 && fieldFlags.contains("ACC_STRICT"));
                    } else {
                        Set<String> fieldFlags = field.access_flags.getFieldFlags();
                        Assert.check(fieldFlags.size() == 2 && fieldFlags.contains("ACC_STRICT") && fieldFlags.contains("ACC_STATIC"));
                    }
                }
            }
        }
    }
}
