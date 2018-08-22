/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.valuetypes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.*;
import java.lang.ref.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static jdk.test.lib.Asserts.*;

import jdk.experimental.bytecode.MacroCodeBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;

import jdk.experimental.value.MethodHandleBuilder;

import javax.tools.*;

/**
 * @test ValueTypesTest
 * @summary Test data movement with value types
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /test/lib
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers TestValue1.java TestValue2.java TestValue3.java TestValue4.java ValueTypesTest.java
 * @run main/othervm -Xint -Xmx128m -XX:+EnableValhalla -XX:-ShowMessageBoxOnError
 *                   -XX:+ExplicitGCInvokesConcurrent
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=false
 *                   runtime.valhalla.valuetypes.ValueTypesTest
 * @run main/othervm -Xcomp -Xmx128m -XX:+EnableValhalla -XX:-ShowMessageBoxOnError
 *                   -XX:+ExplicitGCInvokesConcurrent
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=false
 *                   runtime.valhalla.valuetypes.ValueTypesTest
 */
public class ValueTypesTest {

    public static void main(String[] args) {
        Class<?> valueClass = runtime.valhalla.valuetypes.TestValue1.class;
        Class<?> testClasses[] = {
                runtime.valhalla.valuetypes.TestValue1.class,
                runtime.valhalla.valuetypes.TestValue2.class,
                runtime.valhalla.valuetypes.TestValue3.class,
                runtime.valhalla.valuetypes.TestValue4.class
        };
        Class<?> containerClasses[] = {
                runtime.valhalla.valuetypes.ContainerValue1.class,
                runtime.valhalla.valuetypes.ContainerValue2.class,
                runtime.valhalla.valuetypes.ContainerValue3.class,
                runtime.valhalla.valuetypes.ContainerValue4.class
        };

        for (int i = 0; i < testClasses.length; i++) {
            try {
                testExecutionStackToLocalVariable(testClasses[i]);
                testExecutionStackToFields(testClasses[i], containerClasses[i]);
                // testExecutionStackToValueArray(testClasses[i], containerClasses[i]);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        }
    }

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static void testExecutionStackToLocalVariable(Class<?> valueClass) throws Throwable {
        String sig = "()L" + valueClass.getName() + ";";
        final String signature = sig.replace('.', '/');
        System.out.println(signature);
        MethodHandle fromExecStackToLocalVar = MethodHandleBuilder.loadCode(
                LOOKUP,
                "execStackToLocalVar",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE.invokestatic(System.class, "gc", "()V", false);
                    int n = -1;
                    while (n < 1024) {
                        n++;
                        CODE
                        .invokestatic(valueClass, "getInstance", signature, false)
                        .astore(n);
                        n++;
                        CODE
                        .invokestatic(valueClass, "getNonBufferedInstance", signature, false)
                        .astore(n);
                    }
                    CODE.invokestatic(System.class, "gc", "()V", false);
                    while (n > 0) {
                        CODE
                        .aload(n)
                        .invokevirtual(valueClass, "verify", "()Z", false)
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
                }, valueClass);
        boolean result = (boolean) fromExecStackToLocalVar.invokeExact();
        System.out.println(result);
        assertTrue(result, "Invariant");
    }

    static void testExecutionStackToFields(Class<?> valueClass, Class<?> containerClass) throws Throwable {
        final int ITERATIONS = Platform.isDebugBuild() ? 3 : 512;
        String sig = "()L" + valueClass.getName() + ";";
        final String methodSignature = sig.replace('.', '/');
        final String fieldSignature = "L" + valueClass.getName().replace('.', '/') + ";";
        System.out.println(methodSignature);
        MethodHandle fromExecStackToFields = MethodHandleBuilder.loadCode(
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
                    .invokestatic(valueClass, "getInstance", methodSignature, false)
                    .putfield(containerClass, "nonStaticValueField", fieldSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .aload_1()
                    .getfield(containerClass, "nonStaticValueField", fieldSignature)
                    .invokevirtual(valueClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .aload_1()
                    .invokestatic(valueClass, "getNonBufferedInstance", methodSignature, false)
                    .putfield(containerClass, "nonStaticValueField", fieldSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .aload_1()
                    .getfield(containerClass, "nonStaticValueField", fieldSignature)
                    .invokevirtual(valueClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .invokestatic(valueClass, "getInstance", methodSignature, false)
                    .putstatic(containerClass, "staticValueField", fieldSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .getstatic(containerClass, "staticValueField", fieldSignature)
                    .invokevirtual(valueClass, "verify", "()Z", false)
                    .iconst_1()
                    .ifcmp(TypeTag.I, CondKind.NE, "failed")
                    .invokestatic(valueClass, "getNonBufferedInstance", methodSignature, false)
                    .putstatic(containerClass, "staticValueField", fieldSignature)
                    .invokestatic(System.class, "gc", "()V", false)
                    .getstatic(containerClass, "staticValueField", fieldSignature)
                    .invokevirtual(valueClass, "verify", "()Z", false)
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
                }, valueClass);
        boolean result = (boolean) fromExecStackToFields.invokeExact();
        System.out.println(result);
        assertTrue(result, "Invariant");
    }

    static void testExecutionStackToValueArray(Class<?> valueClass, Class<?> containerClass) throws Throwable {
        final int ITERATIONS = Platform.isDebugBuild() ? 3 : 100;
        String sig = "()L" + valueClass.getName() + ";";
        final String signature = sig.replace('.', '/');
        final String arraySignature = "[L" + valueClass.getName().replace('.', '/') + ";";
        System.out.println(arraySignature);
        MethodHandle fromExecStackToValueArray = MethodHandleBuilder.loadCode(
                LOOKUP,
                "execStackToValueArray",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE
                    .invokestatic(System.class, "gc", "()V", false)
                    .new_(containerClass)
                    .dup()
                    .invoke(MacroCodeBuilder.InvocationKind.INVOKESPECIAL, containerClass, "<init>", "()V", false)
                    .astore_1()
                    .ldc(ITERATIONS * 3)
                    .anewarray(valueClass)
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
                    .invokestatic(valueClass, "getInstance", signature, false)
                    .aastore()
                    .iinc(3, 1)
                    .aload_2()
                    .iload_3()
                    .invokestatic(valueClass, "getNonBufferedInstance", signature, false)
                    .aastore()
                    .iinc(3, 1)
                    .aload_2()
                    .iload_3()
                    .defaultvalue(valueClass)
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
                    .invokevirtual(valueClass, "verify", "()Z", false)
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
                }, valueClass);
        boolean result = (boolean) fromExecStackToValueArray.invokeExact();
        System.out.println(result);
        assertTrue(result, "Invariant");
    }
}
