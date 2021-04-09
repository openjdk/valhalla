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
    public UniversalTVarsCompilationTests() {
        setDefaultFilename("Test.java");
    }

    public void testNegCompilations() {
        assertFail("compiler.err.prob.found.req",
                """
                class Box<__universal T> {
                    T t;
                    void m() { t = null; }
                }
                """
        );
        assertFail("compiler.err.primitive.class.does.not.support",
                """
                primitive class Point {}

                class Test {
                    static void main(String... args) throws Throwable {
                        Point p = new Point();
                        p.wait();
                    }
                }
                """
        );
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
    }

    public void testUniversalTVarFieldMustBeInit() {
        assertFail("compiler.err.var.might.not.have.been.initialized",
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

    public void testPosCompilations() {
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
                primitive class Int128 implements Comparable<Int128> {
                    public int compareTo(Int128 i128) {
                        return 0;
                    }
                }

                class Test {
                    <__universal Z extends Comparable<Z>> void m(Z z) {}
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
}
