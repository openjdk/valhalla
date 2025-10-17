/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test bimorphic inlining with value object receivers.
 * @library /testlibrary /test/lib
 * @enablePreview
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

interface MyInterface_BimorphicInlining {
    public MyInterface_BimorphicInlining hash(MyInterface_BimorphicInlining arg);
}

value class TestValue1_BimorphicInlining implements MyInterface_BimorphicInlining {
    int x;

    public TestValue1_BimorphicInlining(int x) {
        this.x = x;
    }

    public TestValue1_BimorphicInlining hash(MyInterface_BimorphicInlining arg) {
        return new TestValue1_BimorphicInlining(x + ((TestValue1_BimorphicInlining)arg).x);
    }
}

value class TestValue2_BimorphicInlining implements MyInterface_BimorphicInlining {
    int x;

    public TestValue2_BimorphicInlining(int x) {
        this.x = x;
    }

    public TestValue2_BimorphicInlining hash(MyInterface_BimorphicInlining arg) {
        return new TestValue2_BimorphicInlining(x + ((TestValue2_BimorphicInlining)arg).x);
    }
}

class TestClass_BimorphicInlining implements MyInterface_BimorphicInlining {
    int x;

    public TestClass_BimorphicInlining(int x) {
        this.x = x;
    }

    public MyInterface_BimorphicInlining hash(MyInterface_BimorphicInlining arg) {
        return new TestClass_BimorphicInlining(x + ((TestClass_BimorphicInlining)arg).x);
    }
}

public class TestBimorphicInlining {

    public static MyInterface_BimorphicInlining test1(MyInterface_BimorphicInlining i1, MyInterface_BimorphicInlining i2) {
        MyInterface_BimorphicInlining result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    public static MyInterface_BimorphicInlining test2(MyInterface_BimorphicInlining i1, MyInterface_BimorphicInlining i2) {
        MyInterface_BimorphicInlining result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    public static MyInterface_BimorphicInlining test3(MyInterface_BimorphicInlining i1, MyInterface_BimorphicInlining i2) {
        MyInterface_BimorphicInlining result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    public static MyInterface_BimorphicInlining test4(MyInterface_BimorphicInlining i1, MyInterface_BimorphicInlining i2) {
        MyInterface_BimorphicInlining result = i1.hash(i2);
        i1.hash(i2);
        return result;
    }

    static public void main(String[] args) {
        Random rand = new Random();
        TestClass_BimorphicInlining  testObject = new TestClass_BimorphicInlining(rand.nextInt());
        TestValue1_BimorphicInlining TestValue1_BimorphicInlining = new TestValue1_BimorphicInlining(rand.nextInt());
        TestValue2_BimorphicInlining TestValue2_BimorphicInlining = new TestValue2_BimorphicInlining(rand.nextInt());

        for (int i = 0; i < 10_000; ++i) {
            // Trigger bimorphic inlining by calling test methods with different arguments
            MyInterface_BimorphicInlining arg, res;
            boolean rare = (i % 10 == 0);

            arg = rare ? TestValue1_BimorphicInlining : testObject;
            res = test1(arg, arg);
            Asserts.assertEQ(rare ? ((TestValue1_BimorphicInlining)res).x : ((TestClass_BimorphicInlining)res).x, 2 * (rare ? TestValue1_BimorphicInlining.x : testObject.x), "test1 failed");

            arg = rare ? testObject : TestValue1_BimorphicInlining;
            res = test2(arg, arg);
            Asserts.assertEQ(rare ? ((TestClass_BimorphicInlining)res).x : ((TestValue1_BimorphicInlining)res).x, 2 * (rare ? testObject.x : TestValue1_BimorphicInlining.x), "test2 failed");

            arg = rare ? TestValue1_BimorphicInlining : TestValue2_BimorphicInlining;
            res = test3(arg, arg);
            Asserts.assertEQ(rare ? ((TestValue1_BimorphicInlining)res).x : ((TestValue2_BimorphicInlining)res).x, 2 * (rare ? TestValue1_BimorphicInlining.x : TestValue2_BimorphicInlining.x), "test3 failed");

            arg = rare ? TestValue2_BimorphicInlining : TestValue1_BimorphicInlining;
            res = test4(arg, arg);
            Asserts.assertEQ(rare ? ((TestValue2_BimorphicInlining)res).x : ((TestValue1_BimorphicInlining)res).x, 2 * (rare ? TestValue2_BimorphicInlining.x : TestValue1_BimorphicInlining.x), "test4 failed");
        }
    }
}
