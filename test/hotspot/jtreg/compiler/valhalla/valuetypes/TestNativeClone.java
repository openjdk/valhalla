/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8209702
 * @summary Verify that the native clone intrinsic handles value types.
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /test/lib
 * @compile -XDenableValueTypes TestNativeClone.java
 * @run main/othervm -XX:+EnableValhalla -Xbatch -XX:-UseTypeProfile
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.MyValue::*
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.TestNativeClone::test*
 *                   -XX:CompileCommand=compileonly,jdk.internal.reflect.GeneratedMethodAccessor1::invoke
 *                   -XX:CompileCommand=dontinline,jdk.internal.reflect.GeneratedMethodAccessor1::invoke
 *                   compiler.valhalla.valuetypes.TestNativeClone
 */

package compiler.valhalla.valuetypes;

import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jdk.experimental.value.MethodHandleBuilder;
import jdk.test.lib.Asserts;

__ByValue class MyValue {
    public final int x;

    public MyValue(int x) {
        this.x = x;
    }
}

public class TestNativeClone {

    private static final MethodHandle cloneValue = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "MyValue",
        MethodType.methodType(Object.class, MyValue.class),
        CODE -> {
            CODE.
            aload_0().
            invokevirtual(Object.class, "clone", "()Ljava/lang/Object;", false).
            areturn();
        },
        MyValue.class);

    public static void test1(MyValue vt) throws Throwable {
        try {
            cloneValue.invoke(vt);
            throw new RuntimeException("No exception thrown");
        } catch (CloneNotSupportedException e) {
            // Expected
        }
    }

    public static void test2(Method clone, Object obj) {
        try {
            clone.invoke(obj);
        } catch (InvocationTargetException e) {
            // Expected
            Asserts.assertTrue(e.getCause() instanceof CloneNotSupportedException, "Unexpected exception thrown");
            return;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception thrown", e);
        }
        throw new RuntimeException("No exception thrown");
    }

    public static void main(String[] args) throws Throwable {
        MyValue vt = new MyValue(42);
        Method clone = Object.class.getDeclaredMethod("clone");
        clone.setAccessible(true);
        for (int i = 0; i < 20_000; ++i) {
            test1(vt);
            test2(clone, vt);
        }
    }
}
