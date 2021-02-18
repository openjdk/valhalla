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

package compiler.valhalla.inlinetypes;

import java.util.Random;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key randomness
 * @bug 8209009
 * @summary Test bimorphic inlining with inline type receivers.
 * @library /testlibrary /test/lib
 * @run main/othervm -Xbatch -XX:TypeProfileLevel=222
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.inlinetypes.TestBimorphicInlining::test*
 *                   -XX:CompileCommand=quiet -XX:CompileCommand=print,compiler.valhalla.inlinetypes.TestBimorphicInlining::test*
 *                   compiler.valhalla.inlinetypes.TestBimorphicInlining
 * @run main/othervm -Xbatch -XX:TypeProfileLevel=222
 *                   -XX:+UnlockExperimentalVMOptions -XX:PerMethodTrapLimit=0 -XX:PerMethodSpecTrapLimit=0
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.inlinetypes.TestBimorphicInlining::test*
 *                   -XX:CompileCommand=quiet -XX:CompileCommand=print,compiler.valhalla.inlinetypes.TestBimorphicInlining::test*
 *                   compiler.valhalla.inlinetypes.TestBimorphicInlining
 */

interface MyInterface {
    public MyInterface hash(MyInterface arg);
}

primitive final class TestValue1 implements MyInterface {
    final int x;

    public TestValue1(int x) {
        this.x = x;
    }

    public TestValue1 hash(MyInterface arg) {
        return new TestValue1(x + ((TestValue1)arg).x);
    }
}

primitive final class TestValue2 implements MyInterface {
    final int x;

    public TestValue2(int x) {
        this.x = x;
    }

    public TestValue2 hash(MyInterface arg) {
        return new TestValue2(x + ((TestValue2)arg).x);
    }
}

class TestClass implements MyInterface {
    int x;

    public TestClass(int x) {
        this.x = x;
    }

    public MyInterface hash(MyInterface arg) {
        return new TestClass(x + ((TestClass)arg).x);
    }
}

public class TestBimorphicInlining {

    public static MyInterface test1(MyInterface i1, MyInterface i2) {
        MyInterface result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    public static MyInterface test2(MyInterface i1, MyInterface i2) {
        MyInterface result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    public static MyInterface test3(MyInterface i1, MyInterface i2) {
        MyInterface result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    public static MyInterface test4(MyInterface i1, MyInterface i2) {
        MyInterface result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    static public void main(String[] args) {
        Random rand = new Random();
        TestClass  testObject = new TestClass(rand.nextInt());
        TestValue1 testValue1 = new TestValue1(rand.nextInt());
        TestValue2 testValue2 = new TestValue2(rand.nextInt());

        for (int i = 0; i < 10_000; ++i) {
            // Trigger bimorphic inlining by calling test methods with different arguments
            MyInterface arg, res;
            boolean rare = (i % 10 == 0);

            arg = rare ? testValue1 : testObject;
            res = test1(arg, arg);
            Asserts.assertEQ(rare ? ((TestValue1)res).x : ((TestClass)res).x, 2 * (rare ? testValue1.x : testObject.x), "test1 failed");

            arg = rare ? testObject : testValue1;
            res = test2(arg, arg);
            Asserts.assertEQ(rare ? ((TestClass)res).x : ((TestValue1)res).x, 2 * (rare ? testObject.x : testValue1.x), "test2 failed");

            arg = rare ? testValue1 : testValue2;
            res = test3(arg, arg);
            Asserts.assertEQ(rare ? ((TestValue1)res).x : ((TestValue2)res).x, 2 * (rare ? testValue1.x : testValue2.x), "test3 failed");

            arg = rare ? testValue2 : testValue1;
            res = test4(arg, arg);
            Asserts.assertEQ(rare ? ((TestValue2)res).x : ((TestValue1)res).x, 2 * (rare ? testValue2.x : testValue1.x), "test4 failed");
        }
    }
}
