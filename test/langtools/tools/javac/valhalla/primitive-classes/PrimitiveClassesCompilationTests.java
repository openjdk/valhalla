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
 * PrimitiveClassesCompilationTests
 *
 * @test
 * @bug 8297207
 * @summary Negative compilation tests, and positive compilation (smoke) tests for Primitive Classes
 * @library /lib/combo /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.compiler/com.sun.tools.javac.code
 *     jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run testng PrimitiveClassesCompilationTests
 */

import java.io.File;

import java.util.List;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Fieldref_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Methodref_info;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;

import com.sun.tools.javac.code.Flags;

import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import tools.javac.combo.CompilationTestCase;

import toolbox.ToolBox;

@Test
public class PrimitiveClassesCompilationTests extends CompilationTestCase {

    private static String[] DEFAULT_OPTIONS = {"-XDenablePrimitiveClasses"};

    ToolBox tb = new ToolBox();

    public PrimitiveClassesCompilationTests() {
        setDefaultFilename("PrimitiveClassTest.java");
        setCompileOptions(DEFAULT_OPTIONS);
    }

    public void testSupers() {
        assertOK(
                """
                interface GoodSuperInterface {}
                abstract class GoodSuper extends Object {}
                primitive class PC extends GoodSuper implements GoodSuperInterface {}
                """);

        assertOK(
                """
                abstract class Integer extends Number {
                    public double doubleValue() { return 0; }
                    public float floatValue() { return 0; }
                    public long longValue() { return 0; }
                    public int intValue() { return 0; }
                }
                primitive class PC extends Integer {}
                """);

        assertOK(
                """
                primitive class PC extends Number {
                    public double doubleValue() { return 0; }
                    public float floatValue() { return 0; }
                    public long longValue() { return 0; }
                    public int intValue() { return 0; }
                }
                """);

        assertOK(
                """
                abstract class SuperWithStaticField {
                    static int x;
                }
                primitive class PC extends SuperWithStaticField {}
                """);

        assertOK(
                """
                abstract class SuperWithEmptyNoArgCtor {
                    public SuperWithEmptyNoArgCtor() {
                        // Programmer supplied ctor but injected super call
                    }
                }
                abstract class SuperWithEmptyNoArgCtor_01 extends SuperWithEmptyNoArgCtor {
                    public SuperWithEmptyNoArgCtor_01() {
                        super();  // programmer coded chaining no-arg constructor
                    }
                }
                abstract class SuperWithEmptyNoArgCtor_02 extends SuperWithEmptyNoArgCtor_01 {
                    // Synthesized chaining no-arg constructor
                }
                primitive class PC extends SuperWithEmptyNoArgCtor_02 {}
                """);

        assertFail("compiler.err.concrete.supertype.for.value.class",
                """
                class BadSuper {}
                primitive class PC extends BadSuper {}
                """);

        assertFail("compiler.err.instance.field.not.allowed",
                """
                abstract class SuperWithInstanceField {
                    int x;
                }
                abstract class SuperWithInstanceField_01 extends SuperWithInstanceField {}
                primitive class PC extends SuperWithInstanceField_01 {}
                """);

        assertFail("compiler.err.abstract.value.class.no.arg.constructor.must.be.empty",
                """
                abstract class SuperWithNonEmptyNoArgCtor {
                    public SuperWithNonEmptyNoArgCtor() {
                        System.out.println("Non-Empty");
                    }
                }
                abstract class SuperWithNonEmptyNoArgCtor_01 extends SuperWithNonEmptyNoArgCtor {}
                primitive class PC extends SuperWithNonEmptyNoArgCtor_01 {}
                """);

        assertFail("compiler.err.abstract.value.class.constructor.cannot.take.arguments",
                """
                abstract class SuperWithArgedCtor {
                    public SuperWithArgedCtor() {}
                    public SuperWithArgedCtor(String s) {
                    }
                }
                abstract class SuperWithArgedCtor_01 extends SuperWithArgedCtor {}
                primitive class PC extends SuperWithArgedCtor_01 {}
                """);

        assertFail("compiler.err.abstract.value.class.declares.init.block",
                """
                abstract class SuperWithInstanceInit {
                    {
                        System.out.println("Disqualified from being super");
                    }
                }
                abstract class SuperWithInstanceInit_01 extends SuperWithInstanceInit {
                    {
                        // Not disqualified since it is a meaningless empty block.
                    }
                }
                primitive class PC extends SuperWithInstanceInit_01 {}
                """);

        assertFail("compiler.err.super.class.method.cannot.be.synchronized",
                """
                abstract class SuperWithSynchronizedMethod {
                    synchronized void foo() {}
                }
                abstract class SuperWithSynchronizedMethod_1 extends SuperWithSynchronizedMethod {}
                primitive class PC extends SuperWithSynchronizedMethod_1 {}
                """);

        assertFail("compiler.err.abstract.value.class.cannot.be.inner",
                """
                class Outer {
                    abstract class InnerSuper {}
                }
                primitive class PC extends Outer.InnerSuper {}
                """);
    }

    public void testFinalFields() {
        String[] sources = new String[] {
                """
                primitive class Test {
                    final int x = 10;
                    Test() {
                        x = 10;
                    }
                }
                """,
                """
                primitive class Test {
                    final int x = 10;
                    void foo() {
                        x = 10;
                    }
                }
                """
        };
        for (String source : sources) {
            assertFail("compiler.err.cant.assign.val.to.final.var", source);
        }

        assertFail("compiler.err.var.might.already.be.assigned",
                """
                primitive class Test {
                    final int x;
                    Test() {
                        x = 10;
                        x = 10;
                    }
                }
                """
        );
    }

    public void testWithFieldNeg() {
        String[] sources = new String[] {
                """
                primitive final class A {
                    final int x = 10;
                    primitive final class B {
                        final A a = A.default;
                        void foo(A a) {
                            a.x = 100;
                        }
                    }
                }
                """,
                """
                primitive final class A {
                    static final int sx = 10;
                    primitive final class B {
                        final A a = A.default;
                        void foo(A a) {
                            a.sx = 100;
                        }
                    }
                }
                """,
                """
                primitive final class A {
                    final int x = 10;
                    primitive final class B {
                        final A a = A.default;
                    }
                    void withfield(B b) {
                            b.a.x = 11;
                    }
                }
                """,
                """
                primitive final class A {
                    final int x = 10;
                    void foo(A a) {
                        a.x = 100;
                    }
                }
                """,
                """
                primitive final class A {
                    final int x = 10;
                    void foo(A a) {
                        (a).x = 100;
                    }
                }
                """,
                """
                primitive final class A {
                    final int x = 10;
                    void foo(final A fa) {
                        fa.x = 100;
                    }
                }
                """,
                """
                primitive final class A {
                    final int x = 10;
                    void foo() {
                        x = 100;
                    }
                }
                """,
                """
                primitive final class A {
                    final int x = 10;
                    void foo() {
                        this.x = 100;
                    }
                }
                """,
                """
                primitive final class A {
                    final int x = 10;
                    void foo() {
                        A.this.x = 100;
                    }
                }
                """,
        };
        for (String source : sources) {
            assertFail("compiler.err.cant.assign.val.to.final.var", source);
        }
    }
}
