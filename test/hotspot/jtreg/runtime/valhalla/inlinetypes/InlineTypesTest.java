/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.*;
import java.lang.ref.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static jdk.test.lib.Asserts.*;


import jdk.experimental.bytecode.MacroCodeBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;

import javax.tools.*;
import test.java.lang.invoke.lib.InstructionHelper;

/**
 * @test InlineTypesTest
 * @summary Test data movement with inline types
 * @modules java.base/jdk.internal.value
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @modules java.base/jdk.internal.vm.annotation
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile InlineTypesTest.java
 * @run main/othervm -XX:+EnableValhalla
 *                   -Xmx128m -XX:+ExplicitGCInvokesConcurrent
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=false
 *                   runtime.valhalla.inlinetypes.InlineTypesTest
 * @run main/othervm -XX:+EnableValhalla
 *                   -Xmx128m -XX:+ExplicitGCInvokesConcurrent
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=false
 *                   -XX:ForceNonTearable=*
 *                   runtime.valhalla.inlinetypes.InlineTypesTest
 */

 final class ContainerValue1 {
    static TestValue1 staticInlineField;
    @NullRestricted
    TestValue1 nonStaticInlineField;
    TestValue1[] inlineArray;
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class TestValue1 {

    static TestValue1 staticValue = getInstance();

    final int i;
    final String name;

    public TestValue1() {
        i = (int)System.nanoTime();
        name = Integer.valueOf(i).toString();
    }

    public TestValue1(int i) {
        this.i = i;
        name = Integer.valueOf(i).toString();
    }

    public static TestValue1 getInstance() {
        return new TestValue1();
    }

    public static TestValue1 getNonBufferedInstance() {
        return (TestValue1) staticValue;
    }

    public boolean verify() {
        if (name == null) return i == 0;
        return Integer.valueOf(i).toString().compareTo(name) == 0;
    }
}

final class ContainerValue2 {
    static TestValue2 staticInlineField;
    @NullRestricted
    TestValue2 nonStaticInlineField;
    TestValue2[] valueArray;
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class TestValue2 {
    static TestValue2 staticValue = getInstance();

    final long l;
    final double d;
    final String s;

    public TestValue2() {
        l = System.nanoTime();
        s = Long.valueOf(l).toString();
        d = Double.parseDouble(s);
    }

    public TestValue2(long l) {
        this.l = l;
        s = Long.valueOf(l).toString();
        d = Double.parseDouble(s);
    }

    public static TestValue2 getInstance() {
        return new TestValue2();
    }

    public static TestValue2 getNonBufferedInstance() {
        return (TestValue2) staticValue;
    }

    public boolean verify() {
        if (s == null) {
            return d == 0 && l == 0;
        }
        return Long.valueOf(l).toString().compareTo(s) == 0
                && Double.parseDouble(s) == d;
    }
}

final class ContainerValue3 {
    static TestValue3 staticInlineField;
    @NullRestricted
    TestValue3 nonStaticInlineField;
    TestValue3[] valueArray;
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class TestValue3 {

    static TestValue3 staticValue = getInstance();

    final byte b;

    public TestValue3() {
        b = 123;
    }

    public TestValue3(byte b) {
        this.b = b;
    }

    public static TestValue3 getInstance() {
        return new TestValue3();
    }

    public static TestValue3 getNonBufferedInstance() {
        return (TestValue3) staticValue;
    }

    public boolean verify() {
        return b == 0 || b == 123;
    }
}

final class ContainerValue4 {
    static TestValue4 staticInlineField;
    @NullRestricted
    TestValue4 nonStaticInlineField;
    TestValue4[] valueArray;
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class TestValue4 {

    static TestValue4 staticValue = getInstance();

    final byte b1;
    final byte b2;
    final byte b3;
    final byte b4;
    final short s1;
    final short s2;
    final int i;
    final long l;
    final String val;

    public TestValue4() {
        this((int) System.nanoTime());
    }

    public TestValue4(int i) {
        this.i = i;
        val = Integer.valueOf(i).toString();
        ByteBuffer bf = ByteBuffer.allocate(8);
        bf.putInt(0, i);
        bf.putInt(4, i);
        l = bf.getLong(0);
        s1 = bf.getShort(2);
        s2 = bf.getShort(0);
        b1 = bf.get(3);
        b2 = bf.get(2);
        b3 = bf.get(1);
        b4 = bf.get(0);
    }

    public static TestValue4 getInstance() {
        return new TestValue4();
    }

    public static TestValue4 getNonBufferedInstance() {
        return (TestValue4) staticValue;
    }

    public boolean verify() {
        if (val == null) {
            return i == 0 && l == 0 && b1 == 0 && b2 == 0 && b3 == 0 && b4 == 0
                    && s1 == 0 && s2 == 0;
        }
        ByteBuffer bf = ByteBuffer.allocate(8);
        bf.putInt(0, i);
        bf.putInt(4, i);
        long nl =  bf.getLong(0);
        bf.clear();
        bf.putShort(0, s2);
        bf.putShort(2, s1);
        int from_s = bf.getInt(0);
        bf.clear();
        bf.put(0, b4);
        bf.put(1, b3);
        bf.put(2, b2);
        bf.put(3, b1);
        int from_b = bf.getInt(0);
        return l == nl && Integer.valueOf(i).toString().compareTo(val) == 0
                && from_s == i && from_b == i;
    }
}

public class InlineTypesTest {

    public static void main(String[] args) {
        Class<?> inlineClass = runtime.valhalla.inlinetypes.TestValue1.class;
        Class<?> testClasses[] = {
                runtime.valhalla.inlinetypes.TestValue1.class,
                runtime.valhalla.inlinetypes.TestValue2.class,
                runtime.valhalla.inlinetypes.TestValue3.class,
                runtime.valhalla.inlinetypes.TestValue4.class
        };
        Class<?> containerClasses[] = {
                runtime.valhalla.inlinetypes.ContainerValue1.class,
                runtime.valhalla.inlinetypes.ContainerValue2.class,
                runtime.valhalla.inlinetypes.ContainerValue3.class,
                runtime.valhalla.inlinetypes.ContainerValue4.class
        };

        for (int i = 0; i < testClasses.length; i++) {
            try {
                testExecutionStackToLocalVariable(testClasses[i]);
                testExecutionStackToFields(testClasses[i], containerClasses[i]);
                // testExecutionStackToInlineArray(testClasses[i], containerClasses[i]);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        }
    }

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static void testExecutionStackToLocalVariable(Class<?> inlineClass) throws Throwable {
        String sig = "()L" + inlineClass.getName() + ";";
        final String signature = sig.replace('.', '/');
        MethodHandle fromExecStackToLocalVar = InstructionHelper.loadCode(
                LOOKUP,
                "execStackToLocalVar",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE.invokestatic(System.class, "gc", "()V", false);
                    int n = -1;
                    while (n < 1024) {
                        n++;
                        CODE
                        .invokestatic(inlineClass, "getInstance", signature, false)
                        .astore(n);
                        n++;
                        CODE
                        .invokestatic(inlineClass, "getNonBufferedInstance", signature, false)
                        .astore(n);
                    }
                    CODE.invokestatic(System.class, "gc", "()V", false);
                    while (n > 0) {
                        CODE
                        .aload(n)
                        .invokevirtual(inlineClass, "verify", "()Z", false)
                        .iconst_1()
                        .ifcmp(TypeTag.I, CondKind.NE, "end");
                        n--;
                    }
                    CODE
                    .iconst_1()
                    .return_(TypeTag.Z)
                    .label("end")
                    .iconst_0()
                    .return_(TypeTag.Z);
                });
        boolean result = (boolean) fromExecStackToLocalVar.invokeExact();
        System.out.println(result);
        assertTrue(result, "Invariant");
    }

    static void testExecutionStackToFields(Class<?> inlineClass, Class<?> containerClass) throws Throwable {
        final int ITERATIONS = Platform.isDebugBuild() ? 3 : 512;
        String sig = "()L" + inlineClass.getName() + ";";
        final String methodSignature = sig.replace('.', '/');
        final String fieldLSignature = "L" + inlineClass.getName().replace('.', '/') + ";";
        System.out.println(methodSignature);
        MethodHandle fromExecStackToFields = InstructionHelper.loadCode(
                LOOKUP,
                "execStackToFields",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE
                    .invokestatic(System.class, "gc", "()V", false)
                    .new_(containerClass)
                    .dup()
                    .invoke(MacroCodeBuilder.InvocationKind.INVOKESPECIAL, containerClass, "<init>", "()V", false)
                    .astore_1()
                    .iconst_m1()
                    .istore_2()
                    .label("loop")
                    .iload_2()
                    .ldc(ITERATIONS)
                    .ifcmp(TypeTag.I, CondKind.EQ, "end")
                    .aload_1()
                    .invokestatic(inlineClass, "getInstance", methodSignature, false)
                    .putfield(containerClass, "nonStaticInlineField", fieldLSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .aload_1()
                    .getfield(containerClass, "nonStaticInlineField", fieldLSignature)
                    .invokevirtual(inlineClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .aload_1()
                    .invokestatic(inlineClass, "getNonBufferedInstance", methodSignature, false)
                    .putfield(containerClass, "nonStaticInlineField", fieldLSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .aload_1()
                    .getfield(containerClass, "nonStaticInlineField", fieldLSignature)
                    .invokevirtual(inlineClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .invokestatic(inlineClass, "getInstance", methodSignature, false)
                    .putstatic(containerClass, "staticInlineField", fieldLSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .getstatic(containerClass, "staticInlineField", fieldLSignature)
                    .checkcast(inlineClass)
                    .invokevirtual(inlineClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .invokestatic(inlineClass, "getNonBufferedInstance", methodSignature, false)
                    .putstatic(containerClass, "staticInlineField", fieldLSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .getstatic(containerClass, "staticInlineField", fieldLSignature)
                    .checkcast(inlineClass)
                    .invokevirtual(inlineClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .iinc(2, 1)
                    .goto_("loop")
                    .label("end")
                    .iconst_1()
                    .return_(TypeTag.Z)
                    .label("failed")
                    .iconst_0()
                    .return_(TypeTag.Z);
                });
        boolean result = (boolean) fromExecStackToFields.invokeExact();
        System.out.println(result);
        assertTrue(result, "Invariant");
    }

    static void testExecutionStackToInlineArray(Class<?> inlineClass, Class<?> containerClass) throws Throwable {
        final int ITERATIONS = Platform.isDebugBuild() ? 3 : 100;
        String sig = "()L" + inlineClass.getName() + ";";
        final String signature = sig.replace('.', '/');
        final String arraySignature = "[L" + inlineClass.getName().replace('.', '/') + ";";
        System.out.println(arraySignature);
        MethodHandle fromExecStackToInlineArray = InstructionHelper.loadCode(
                LOOKUP,
                "execStackToInlineArray",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE
                    .invokestatic(System.class, "gc", "()V", false)
                    .new_(containerClass)
                    .dup()
                    .invoke(MacroCodeBuilder.InvocationKind.INVOKESTATIC, containerClass, "<vnew>", "()V", false)
                    .astore_1()
                    .ldc(ITERATIONS * 3)
                    .anewarray(inlineClass)
                    .astore_2()
                    .aload_2()
                    .aload_1()
                    .swap()
                    .putfield(containerClass, "valueArray", arraySignature)
                    .iconst_0()
                    .istore_3()
                    .label("loop1")
                    .iload_3()
                    .ldc(ITERATIONS)
                    .ifcmp(TypeTag.I, CondKind.GE, "end1")
                    .aload_2()
                    .iload_3()
                    .invokestatic(inlineClass, "getInstance", signature, false)
                    .aastore()
                    .iinc(3, 1)
                    .aload_2()
                    .iload_3()
                    .invokestatic(inlineClass, "getNonBufferedInstance", signature, false)
                    .aastore()
                    .iinc(3, 1)
                    .aload_2()
                    .iload_3()
                    .aconst_init(inlineClass)
                    .aastore()
                    .iinc(3, 1)
                    .goto_("loop1")
                    .label("end1")
                    .invokestatic(System.class, "gc", "()V", false)
                    .iconst_0()
                    .istore_3()
                    .label("loop2")
                    .iload_3()
                    .ldc(ITERATIONS * 3)
                    .ifcmp(TypeTag.I, CondKind.GE, "end2")
                    .aload_2()
                    .iload_3()
                    .aaload()
                    .invokevirtual(inlineClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .iinc(3, 1)
                    .goto_("loop2")
                    .label("end2")
                    .iconst_1()
                    .return_(TypeTag.Z)
                    .label("failed")
                    .iconst_0()
                    .return_(TypeTag.Z);
                });
        boolean result = (boolean) fromExecStackToInlineArray.invokeExact();
        System.out.println(result);
        assertTrue(result, "Invariant");
    }
}
