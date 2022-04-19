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
 * @test
 * @summary Test scalarization in returns with unloaded return types.
 * @library /test/lib /compiler/whitebox /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -Xbatch -XX:CompileCommand=dontinline,*::test*
 *                   TestUnloadedReturnTypes
 */

import java.lang.reflect.Method;

import sun.hotspot.WhiteBox;

primitive class MyPrimitive {
    int x;

    public MyPrimitive(int x) {
        this.x = x;
    }
}

value class MyValue {
    int x;

    public MyValue(int x) {
        this.x = x;
    }
}

class MyClass {

    static MyPrimitive test1() {
        return new MyPrimitive(42);
    }

    static MyPrimitive.ref test2(boolean b) {
        return b ? new MyPrimitive(42) : null;
    }

    static MyValue test3(boolean b) {
        return b ? new MyValue(42) : null;
    }
}

public class TestUnloadedReturnTypes {
    public static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    static Object res1 = null;

    public static void test1() {
        res1 = MyClass.test1();
    }

    static Object res2 = null;

    public static void test2(boolean b) {
        res2 = MyClass.test2(b);
    }

    static Object res3 = null;

    public static void test3(boolean b) {
        res3 = MyClass.test3(b);
    }

    public static void main(String[] args) throws Exception {
        // C1 compile all caller methods
        Method m = TestUnloadedReturnTypes.class.getMethod("test1");
        WHITE_BOX.enqueueMethodForCompilation(m, 3);

        m = TestUnloadedReturnTypes.class.getMethod("test2", boolean.class);
        WHITE_BOX.enqueueMethodForCompilation(m, 3);

        m = TestUnloadedReturnTypes.class.getMethod("test3", boolean.class);
        WHITE_BOX.enqueueMethodForCompilation(m, 3);

        // Make sure the callee methods are C2 compiled
        for (int i = 0; i < 100_000; ++i) {
            MyClass.test1();
            MyClass.test2((i % 2) == 0);
            MyClass.test3((i % 2) == 0);
        }

        test1();
        if (((MyPrimitive)res1).x != 42) {
            throw new RuntimeException("Test1 failed");
        }

        test2(true);
        if (((MyPrimitive)res2).x != 42) {
            throw new RuntimeException("Test2 failed");
        }

        test2(false);
        if (res2 != null) {
            throw new RuntimeException("Test2 failed");
        }
        test3(true);
        if (((MyValue)res3).x != 42) {
            throw new RuntimeException("Test3 failed");
        }

        test3(false);
        if (res3 != null) {
            throw new RuntimeException("Test3 failed");
        }
    }
}
