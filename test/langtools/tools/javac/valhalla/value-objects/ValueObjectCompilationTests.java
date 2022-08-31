/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * ValueObjectCompilationTests
 *
 * @test
 * @bug 8287136 8292630 8279368 8287136 8287770 8279840 8279672 8292753 8287763 8279901 8287767
 * @summary Negative compilation tests, and positive compilation (smoke) tests for Value Objects
 * @library /lib/combo /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run testng ValueObjectCompilationTests
 */

import java.lang.constant.ClassDesc;

import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;

import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;

import com.sun.tools.javac.util.Assert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;

import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

@Test
public class ValueObjectCompilationTests extends CompilationTestCase {

    ToolBox tb = new ToolBox();

    public ValueObjectCompilationTests() {
        setDefaultFilename("ValueObjectsTest.java");
    }

    public void testAbstractValueClassConstraints() {
        assertFail("compiler.err.super.field.not.allowed",
                """
                abstract value class V {
                    int f;  // Error, abstract value class may not declare an instance field.
                }
                """);
        assertFail("compiler.err.super.class.cannot.be.inner",
                """
                class Outer {
                    abstract value class V {
                        // Error, an abstract value class cant be an inner class
                    }
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                abstract value class V {
                    synchronized void foo() {
                     // Error, abstract value class may not declare a synchronized instance method.
                    }
                }
                """);
        assertFail("compiler.err.super.class.declares.init.block",
                """
                abstract value class V {
                    { int f = 42; } // Error, abstract value class may not declare an instance initializer.
                }
                """);
        assertFail("compiler.err.super.constructor.cannot.take.arguments",
                """
                abstract value class V {
                    V(int x) {}  // Error, abstract value class may not declare a non-trivial constructor.
                }
                """);
    }

    public void testAnnotationsConstraints() {
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                identity @interface IA {}
                """);
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                value @interface IA {}
                """);
    }

    public void testCheckFeatureSourceLevel() {
        setCompileOptions(new String[]{"--release", "13"});
        assertFail("compiler.err.feature.not.supported.in.source.plural",
                """
                value class V {
                    public int v = 42;
                }
                """);
        setCompileOptions(new String[]{});
    }

    public void testSuperClassConstraints() {
        assertFail("compiler.err.super.field.not.allowed",
                """
                abstract class I { // identity class since it declares an instance field.
                    int f;
                }
                value class V extends I {}
                """);

        assertFail("compiler.err.super.class.cannot.be.inner",
                """
                class Outer {
                    abstract class I { /* has identity since is an inner class */ }
                    static value class V extends I
                }
                """);

        assertFail("compiler.err.super.method.cannot.be.synchronized",
                """
                abstract class I { // has identity since it declared a synchronized instance method.
                    synchronized void foo() {}
                }
                value class V extends I {}
                """);

        assertFail("compiler.err.super.class.declares.init.block",
                """
                abstract class I { // has identity since it declares an instance initializer
                    { int f = 42; }
                }
                value class V extends I {}
                """);

        assertFail("compiler.err.super.constructor.cannot.take.arguments",
                """
                abstract class I { // has identity since it declares a non-trivial constructor
                    I(int x) {}
                }
                value class V extends I {}
                """);
    }

    public void testSynchronizeOnValueInterfaceInstance() {
        assertFail("compiler.err.type.found.req",
                """
                value interface VI {
                    default void foo(VI vi) {
                        synchronized (vi) {} // Error
                    }
                }
                """);
    }

    public void testRepeatedModifiers() {
        String[] sources = new String[] {
                "static static class StaticTest {}",
                "native native class NativeTest {}",
                "value value primitive class ValueTest {}",
                "primitive primitive value class PrimitiveTest {}"
        };
        for (String source : sources) {
            assertFail("compiler.err.repeated.modifier", source);
        }
    }

    public void testParserTest() {
        assertOK(
                """
                value class Substring implements CharSequence {
                    private String str;
                    private int start;
                    private int end;

                    public Substring(String str, int start, int end) {
                        checkBounds(start, end, str.length());
                        this.str = str;
                        this.start = start;
                        this.end = end;
                    }

                    public int length() {
                        return end - start;
                    }

                    public char charAt(int i) {
                        checkBounds(0, i, length());
                        return str.charAt(start + i);
                    }

                    public Substring subSequence(int s, int e) {
                        checkBounds(s, e, length());
                        return new Substring(str, start + s, start + e);
                    }

                    public String toString() {
                        return str.substring(start, end);
                    }

                    private static void checkBounds(int start, int end, int length) {
                        if (start < 0 || end < start || length < end)
                            throw new IndexOutOfBoundsException();
                    }
                }
                """
        );
    }

    public void testSemanticsViolations() {
        assertFail("compiler.err.cant.inherit.from.final",
                """
                value class Base {}
                class Subclass extends Base {}
                """);
        assertFail("compiler.err.super.class.cannot.be.inner",
                """
                class Outer {
                    abstract value class AbsValue {}
                }
                """);
        assertFail("compiler.err.cant.assign.val.to.final.var",
                """
                value class Point {
                    int x = 10;
                    int y;
                    Point (int x, int y) {
                        this.x = x; // Error, final field 'x' is already assigned to.
                        this.y = y; // OK.
                    }
                }
                """);
        assertFail("compiler.err.cant.assign.val.to.final.var",
                """
                value class Point {
                    int x;
                    int y;
                    Point (int x, int y) {
                        this.x = x;
                        this.y = y;
                    }

                    void foo(Point p) {
                        this.y = p.y; // Error, y is final and can't be written outside of ctor.
                    }
                }
                """);
        assertFail("compiler.err.var.might.not.have.been.initialized",
                """
                value class Point {
                    int x;
                    int y;
                    Point (int x, int y) {
                        this.x = x;
                        // y hasn't been initialized
                    }
                }
                """);
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                value identity class IdentityValue {}
                """);
        assertFail("compiler.err.call.to.super.not.allowed.in.value.ctor",
                """
                value class V {
                    V() {
                        super();
                    }
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                value class V {
                    synchronized void foo() {}
                }
                """);
        assertOK(
                """
                value class V {
                    synchronized static void soo() {}
                }
                """);
        assertFail("compiler.err.type.found.req",
                """
                value class V {
                    { synchronized(this) {} }
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                value record R() {
                    synchronized void foo() { } // Error;
                    synchronized static void soo() {} // OK.
                }
                """);
        assertFail("compiler.err.this.exposed.prematurely",
                """
                value class V {
                    int x;
                    V() {
                        foo(this); // Error.
                        x = 10;
                        foo(this); // Ok.
                    }
                    void foo(V v) {}
                }
                """);
        assertFail("compiler.err.type.found.req",
                """
                interface I {}
                value interface VI extends I {}
                class C {}
                value class VC<T extends VC> {
                    void m(T t) {
                        synchronized(t) {} // error
                    }
                }
                """);
        assertFail("compiler.err.type.found.req",
                """
                interface I {}
                value interface VI extends I {}
                class C {}
                value class VC<T extends VC> {
                    void foo(Object o) {
                        synchronized ((VC & I)o) {} // error
                    }
                }
                """);
        assertFail("compiler.err.type.found.req",
                """
                interface I {}
                value interface VI extends I {}
                class C {}
                value class VC<T extends VC> {
                    void bar(Object o) {
                        synchronized ((I & VI)o) {} // error
                    }
                }
                """);
    }

    public void testNontrivialConstructor() {
        assertOK(
                """
                abstract value class V {
                    public V() { /* trivial ctor */ }
                }
                """
        );
        assertFail("compiler.err.super.constructor.access.restricted",
                """
                abstract value class V {
                    private V() {} // non-trivial, more restrictive access than the class.
                }
                """);
        assertFail("compiler.err.super.constructor.cannot.take.arguments",
                """
                abstract value class V {
                    public V(int x) {} // non-trivial ctor as it declares formal parameters.
                }
                """);
        assertFail("compiler.err.super.constructor.cannot.be.generic",
                """
                abstract value class V {
                    <T> V() {} // non trivial as it declares type parameters.
                }
                """);
        assertFail("compiler.err.super.constructor.cannot.throw",
                """
                abstract value class V {
                    V() throws Exception {} // non-trivial as it throws
                }
                """);
        assertFail("compiler.err.super.no.arg.constructor.must.be.empty",
                """
                abstract value class V {
                    V() {
                        System.out.println("");
                    } // non-trivial as it has a body.
                }
                """);
    }

    public void testFunctionalInterface() {
        assertFail("compiler.err.bad.functional.intf.anno.1",
                """
                @FunctionalInterface
                identity interface I { // Error
                    void m();
                }
                """);
        assertFail("compiler.err.bad.functional.intf.anno.1",
                """
                @FunctionalInterface
                value interface K { // Error
                    void m();
                }
                """);
        assertFail("compiler.err.prob.found.req",
                """
                identity interface L {
                    void m();
                }
                class Test {
                    void foo() {
                        var t = (L) () -> {}; // Error
                    }
                }
                """);
        assertFail("compiler.err.prob.found.req",
                """
                value interface M {
                    void m();
                }
                class Test {
                    void foo() {
                        var u = (M) () -> {}; // Error
                    }
                }
                """);
        // currently failing but should be accepted
        /*
        assertOK(
                """
                identity interface I {
                    void m();
                }

                @FunctionalInterface
                interface J extends I  {}
                """);
        */
    }

    public void testMutuallyIncompatibleSupers() {
        assertFail("compiler.err.mutually.incompatible.supers",
                """
                identity interface II {}
                value interface VI {}
                abstract class X implements II, VI {}
                """);
        assertFail("compiler.err.value.type.has.identity.super.type",
                """
                identity interface II {}
                interface GII extends II {} // OK.
                value interface BVI extends GII {} // Error
                """);
        assertFail("compiler.err.identity.type.has.value.super.type",
                """
                value interface VI {}
                interface GVI extends VI {} // OK.
                identity interface BII extends GVI {} // Error
                """);
        assertFail("compiler.err.value.type.has.identity.super.type",
                """
                identity interface II {}
                value class BVC implements II {} // Error
                """);
        assertFail("compiler.err.identity.type.has.value.super.type",
                """
                value interface VI {}
                class BIC implements VI {} // Error
                """);
    }
}
