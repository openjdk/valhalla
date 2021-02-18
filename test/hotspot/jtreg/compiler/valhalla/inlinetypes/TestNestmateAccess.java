/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import jdk.test.lib.Asserts;

/**
 * @test
 * @bug 8253416
 * @summary Test nestmate access to flattened field if nest-host is not loaded.
 * @library /test/lib
 * @run main/othervm -Xcomp
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.inlinetypes.Test*::<init>
 *                   compiler.valhalla.inlinetypes.TestNestmateAccess
 * @run main/othervm -Xcomp -XX:TieredStopAtLevel=1
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.inlinetypes.Test*::<init>
 *                   compiler.valhalla.inlinetypes.TestNestmateAccess
 */

interface MyInterface {
    int hash();
}

primitive class MyValue implements MyInterface {
    int x = 42;
    int y = 43;

    @Override
    public int hash() { return x + y; }
}

// Test load from flattened field in nestmate when nest-host is not loaded.
class Test1 {
    private MyValue vt;

    public Test1(final MyValue vt) {
        this.vt = vt;
    }

    public MyInterface test() {
        return new MyInterface() {
            // The vt field load does not link.
            private int x = (Test1.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Same as Test1 but outer class is an inline type
primitive class Test2 {
    private MyValue vt;

    public Test2(final MyValue vt) {
        this.vt = vt;
    }

    public MyInterface test() {
        return new MyInterface() {
            // Delayed flattened load of Test2.this.
            // The vt field load does not link.
            private int x = (Test2.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Test store to flattened field in nestmate when nest-host is not loaded.
class Test3 {
    private MyValue vt;

    public MyInterface test(MyValue init) {
        return new MyInterface() {
            // Store to the vt field does not link.
            private MyValue tmp = (vt = init);

            @Override
            public int hash() { return tmp.hash() + vt.hash(); }
        };
    }
}

// Same as Test1 but with static field
class Test4 {
    private static MyValue vt;

    public Test4(final MyValue vt) {
        this.vt = vt;
    }

    public MyInterface test() {
        return new MyInterface() {
            // The vt field load does not link.
            private int x = (Test4.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Same as Test2 but with static field
primitive class Test5 {
    private static MyValue vt;

    public Test5(final MyValue vt) {
        this.vt = vt;
    }

    public MyInterface test() {
        return new MyInterface() {
            // Delayed flattened load of Test5.this.
            // The vt field load does not link.
            private int x = (Test5.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Same as Test3 but with static field
class Test6 {
    private static MyValue vt;

    public MyInterface test(MyValue init) {
        return new MyInterface() {
            // Store to the vt field does not link.
            private MyValue tmp = (vt = init);

            @Override
            public int hash() { return tmp.hash() + vt.hash(); }
        };
    }
}

// Same as Test6 but outer class is an inline type
primitive class Test7 {
    private static MyValue vt;

    public MyInterface test(MyValue init) {
        return new MyInterface() {
            // Store to the vt field does not link.
            private MyValue tmp = (vt = init);

            @Override
            public int hash() { return tmp.hash() + vt.hash(); }
        };
    }
}

public class TestNestmateAccess {

    public static void main(String[] args) {
        Test1 t1 = new Test1(new MyValue());
        int res = t1.test().hash();
        Asserts.assertEQ(res, 85);

        Test2 t2 = new Test2(new MyValue());
        res = t2.test().hash();
        Asserts.assertEQ(res, 85);

        Test3 t3 = new Test3();
        res = t3.test(new MyValue()).hash();
        Asserts.assertEQ(res, 170);

        Test4 t4 = new Test4(new MyValue());
        res = t4.test().hash();
        Asserts.assertEQ(res, 85);

        Test5 t5 = new Test5(new MyValue());
        res = t5.test().hash();
        Asserts.assertEQ(res, 85);

        Test6 t6 = new Test6();
        res = t6.test(new MyValue()).hash();
        Asserts.assertEQ(res, 170);

        Test7 t7 = new Test7();
        res = t7.test(new MyValue()).hash();
        Asserts.assertEQ(res, 170);
    }
}
