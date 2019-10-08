/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8199386
 * @run testng/othervm MyThreadLocal
 * @summary verify a bridge method is generated to access super::initialValue
 *          in a different package.
 */

import java.util.function.*;
import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Arrays;

import org.testng.annotations.Test;

import static java.lang.invoke.MethodType.methodType;
import static org.testng.Assert.*;

public class MyThreadLocal extends ThreadLocal<String> {
    static Lookup lookup() {
        return MethodHandles.lookup();
    }

    @Test
    public void test() throws Throwable {
        MyThreadLocal tl = new MyThreadLocal();
        assertTrue(tl.thisInit().get().equals("subclass " + null));
        assertTrue(tl.superInit().get() == null);
        assertTrue(tl.new Inner().thisInitInner().get().equals("subclass " + null));
        assertTrue(tl.new Inner().superInitInner().get() == null);
    }

    /*
     * The Java language allows a devirtualized access to ThreadLocal.initialValue.
     * The JVM uses "invokespecial" to perform the call. The JVM-level constraints
     * on invokespecial are very severe, more severe than the language.
     * This leads to bridges in places where the source compiler is not clever
     * enough to place the invokespecial instruction precisely in the classfile
     * for MyThreadLocal.
     */
    @Test
    public void syntheticMethod() {
        long bridges = Arrays.stream(MyThreadLocal.class.getDeclaredMethods())
                             .filter(m -> m.getName().startsWith("access$"))
                             .count();
        assertTrue(bridges == 1);
    }

    @Override
    protected String initialValue() {
        // invokespecial ThreadLocal.initialValue:()Object
        String x = super.initialValue();
        return "subclass " + x;
    }

    // this boils down to invokevirtual MyThreadLocal.initialValue:()String
    Supplier<String> thisInit() {
        return () -> this.initialValue();
    }

    // this boils down to invokespecial ThreadLocal.initialValue:()Object
    Supplier<String> superInit() {
        return () -> super.initialValue();
    }

    // see also this::initialValue, super::initialValue
    class Inner {
        Supplier<String> thisInitInner() {
            return () -> MyThreadLocal.this.initialValue();
        }

        // VM required bridge invokestatic MyThreadLocal.access$001:(MyThreadLocal)Object
        // (this is because we are in MyThreadLocal$Inner.class)
        Supplier<String> superInitInner() {
            return () -> MyThreadLocal.super.initialValue();
        }
    }

    private static void testNestmateAccess() { }

    // dynamic version of superInit with MH, and no bridge
    MethodHandle superInitMH() throws ReflectiveOperationException {
        return lookup().findSpecial(ThreadLocal.class, "initialValue",
                                    methodType(Object.class),
                                    MyThreadLocal.class);
    }

    static class SuperMethodHandleFactory {
        // dynamic version of superInitInner with MH, and no bridge
        static MethodHandle superInitInnerMH() throws ReflectiveOperationException {
            Lookup L = lookup();
            // REQUIRED TELEPORT:  Any place where the JVM requires a bridge,
            // the corresponding dynamic access via method handles requires Lookup.in:
            L = L.in(MyThreadLocal.class);
            return L.findSpecial(ThreadLocal.class, "initialValue",
                                 methodType(Object.class),
                                 MyThreadLocal.class);
        }
    }
}
