/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * UniversalTVarsCompilationTests
 *
 * @test
 * @summary Negative compilation tests, and positive compilation (smoke) tests for universal type variables
 * @library /lib/combo /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.jdeps/com.sun.tools.classfile
 * @build JavacTestingAbstractProcessor
 * @run testng/othervm UniversalTVarsCompilationTests
 */

import com.sun.tools.javac.util.Assert;

import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;

import static org.testng.Assert.assertEquals;

@Test
public class UniversalTVarsCompilationTests extends CompilationTestCase {
    private static String[] EMPTY_OPTIONS = {};

    private static String[] LINT_OPTIONS = {
        "-Xlint:universal"
    };

    public UniversalTVarsCompilationTests() {
        setDefaultFilename("Test.java");
    }

    public void testWarningNullAssigment() {
        setCompileOptions(LINT_OPTIONS);
        assertOKWithWarning("compiler.warn.universal.variable.cannot.be.assigned.null",
                """
                class Box<__universal T> {
                    T t;
                    void m() { t = null; }
                }
                """
        );
        assertOKWithWarning("compiler.warn.universal.variable.cannot.be.assigned.null",
                """
                class Box<__universal T> {
                    T m() { return null; }
                }
                """
        );
        assertOKWithWarning("compiler.warn.universal.variable.cannot.be.assigned.null",
                """
                class C<__universal T> {
                    T.ref x = null;
                    T get() { return x; } // warning: possible null value conversion
                    T.ref getRef() { return x; } // OK
                }
                """
        );
        assertOKWithWarning("compiler.warn.universal.variable.cannot.be.assigned.null",
                """
                class Box<__universal T> {
                    T t;
                    Box(T t) {
                        this.t = t;
                    }
                    void m() { t = null; }
                }
                """
        );

        setCompileOptions(EMPTY_OPTIONS);
        assertOK(
                """
                import java.io.*;
                class C<__universal T extends Reader> { T x = null; /* ok */ }
                """
        );
        assertOK(
                """
                import java.io.*;
                class C<T extends Reader> { T x = null; /* ok */ }
                """
        );
        assertOK(
                """
                import java.io.*;
                class C<__universal T extends Reader> { T.ref x = null; /* ok */ }
                """
        );
    }

    public void testPosCompilations() {
        setCompileOptions(EMPTY_OPTIONS);
        assertOK(
                """
                primitive class Point {}

                class C<__universal T> {
                    C<Point> cp;
                }
                """
        );
        assertOK(
                """
                interface Shape {}

                primitive class Point implements Shape {}

                class Box<__universal T> {}

                class Test {
                    void m(Box<Point> lp) {
                        /* this invocation will provoke a subtype checking, basically a check testing if:
                         * `Box<Point> <: Box<? extends Shape>`, this should stress the new `isBoundedBy` relation,
                         * in particular it should check if `Point` is bounded by `Shape`, which is true as
                         * `Point.ref` isBoundedBy Shape
                         */
                        foo(lp);
                    }

                    void foo(Box<? extends Shape> ls) {}
                }
                """
        );
        assertOK(
                """
                interface Shape {}

                primitive class Point implements Shape {}

                class Box<__universal T> {}

                class Test {
                    void m(Box<Shape> lp) {
                        foo(lp);
                    }

                    void foo(Box<? super Point> ls) {}
                }
                """
        );
        assertOK(
                """
                import java.io.*;

                primitive class Point {}

                class C<T> {
                    T x = null;

                    void m() {
                        FileReader r = new C<FileReader>().x;
                        Point.ref p = new C<Point.ref>().x;
                    }
                }
                """
        );
        assertOK(
                """
                import java.io.*;

                primitive class Point {}

                class C<__universal T> {
                    T.ref x = null;

                    void m() {
                        FileReader r = new C<FileReader>().x;
                        Point.ref p = new C<Point.ref>().x;
                        Point.ref p2 = new C<Point>().x;
                    }
                }
                """
        );
        assertOK(
                """
                class C<__universal T> {
                    T.ref x = null;
                    void set(T arg) { x = arg; /* ok */ }
                }
                """
        );
        assertOK(
                """
                primitive class Point {}

                class MyList<__universal T> {
                    static <__universal E> MyList<E> of(E e1) {
                        return null;
                    }
                }

                class Test {
                    void m() {
                        MyList.of(new Point());
                    }
                }
                """
        );

        assertOK(
                """
                primitive class Point {}

                class MyCollection<__universal T> {}

                class MyList<__universal T> extends MyCollection<T> {
                    static <__universal E> MyList<E> of(E e1) {
                        return null;
                    }
                }

                class Test {
                    void m() {
                         MyCollection<Point.ref> mpc = MyList.of(new Point());
                    }
                }
                """
        );
    }

    public void testUniversalTVarFieldMustBeInit() {
        setCompileOptions(EMPTY_OPTIONS);
        assertOKWithWarning("compiler.warn.var.might.not.have.been.initialized",
                """
                class Box<__universal T> {
                    T t;
                    Box() {}
                }
                """
        );

        assertOK(
                """
                class Box<__universal T> {
                    T.ref t;
                    Box() {}
                }
                """
        );
    }

    public void testForbiddenMethods() {
        setCompileOptions(EMPTY_OPTIONS);
        assertFail("compiler.err.primitive.class.does.not.support",
                """
                primitive class Point {}

                class Test {
                    static <__universal Z> Z id(Z z) {
                        return z;
                    }

                    static void main(String... args) throws Throwable {
                        Point p = new Point();
                        id(p).wait();
                    }
                }
                """
        );
        // this one will probably be a lint warning
        /*
        assertOKWithWarning("compiler.warn.method.should.not.be.invoked.on.universal.tvars",
                """
                primitive class Point {}

                class Test<__universal T> {
                    void m(T t) throws Throwable {
                        t.wait();
                    }
                }
                """
        );
        */
    }

    public void testPosCompilations2() {
        setCompileOptions(EMPTY_OPTIONS);
        assertOK(
                """
                interface MyComparable<__universal T> {
                    public int compareTo(T o);
                }

                primitive class Int128 implements MyComparable<Int128> {
                    public int compareTo(Int128 i128) {
                        return 0;
                    }
                }

                class Test {
                    <__universal Z extends MyComparable<Z>> void m(Z z) {}
                    void foo() {
                        Int128 i = new Int128();
                        m(i);
                    }
                }
                """
        );
        assertOK(
                """
                interface MyComparable<__universal T> {}

                primitive class Int128 implements MyComparable<Int128> {
                    public int compareTo(Int128 i128) {
                        return 0;
                    }
                }

                class Test {
                    <__universal Z extends MyComparable<Z>> void m(Z z) {}
                    void foo() {
                        Int128 i = new Int128();
                        m(i);
                    }
                }
                """
        );
        assertOK(
                """
                import java.util.*;

                interface I {}

                class Test {
                    <__universal T> T.ref bar() {
                        return null;
                    }

                    void foo() {
                        List<? extends I> values = bar();
                    }
                }
                """
        );

        assertOK(
                """
                class Test<__universal T> {
                    T.ref t;

                    void m() {
                        this.t = null;
                    }
                }
                """
        );

        assertOK(
                """
                import java.util.*;

                class Test {
                    Map<String, String> types = new HashMap<>();
                }
                """
        );

        assertOK(
                """
                class C1 {
                    <__universal T> void foo(T t) {}
                }

                class C2 extends C1 {
                    <__universal T> void foo(T.ref t) { }
                }
                """
        );

        assertOK(
                """
                class C1 {
                    <__universal T> void foo(T.ref t) {}
                }

                class C2 extends C1 {
                    <__universal T> void foo(T t) { }
                }
                """
        );

        assertOK(
                """
                    import java.util.function.*;
                    class Test<__universal T> {
                        T.ref field;
                        void foo(T t, Consumer<? super T> action) {
                            action.accept(field = t);
                        }
                    }
                """
        );
    }

    public void testPrimitiveValueConversion() {
        setCompileOptions(LINT_OPTIONS);
        assertOKWithWarning("compiler.warn.primitive.value.conversion",
                """
                primitive class Point {}

                class Test {
                    void m() {
                        Point.ref pr = null;
                        Point p = pr;
                    }
                }
                """);
    }

    public void testUncheckedWarning() {
        /* this one should generate unchecked warning
        interface MyList<__universal E> {}

        class MyArrays {
            @SafeVarargs
            @SuppressWarnings("varargs")
            public static <__universal T> MyList<T> asList(T... a) {
                return null;
            }
        }

        class Test<__universal T> {
            MyList<T.ref> newList() {
                return MyArrays.asList(null, null);
            }

            void foo() {
                MyList<T> list = newList(); // unchecked warning
            }
        }
        */
    }
}
