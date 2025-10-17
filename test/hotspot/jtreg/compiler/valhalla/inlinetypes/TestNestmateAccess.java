/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import jdk.internal.vm.annotation.Strict;


/**
 * @test
 * @bug 8253416
 * @summary Test nestmate access to flattened field if nest-host is not loaded.
 * @library /test/lib
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main/othervm -Xcomp
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.inlinetypes.Test*::<init>
 *                   compiler.valhalla.inlinetypes.TestNestmateAccess
 * @run main/othervm -Xcomp -XX:TieredStopAtLevel=1
 *                   -XX:CompileCommand=compileonly,compiler.valhalla.inlinetypes.Test*::<init>
 *                   compiler.valhalla.inlinetypes.TestNestmateAccess
 * @run main/othervm compiler.valhalla.inlinetypes.TestNestmateAccess
 */

interface MyInterface_NestmateAccess {
    int hash();
}

@LooselyConsistentValue
value class MyValue_NestmateAccess implements MyInterface_NestmateAccess {
    int x = 42;
    int y = 43;

    @Override
    public int hash() { return x + y; }
}

// Test load from flattened field in nestmate when nest-host is not loaded.
class Test1_NestmateAccess {
    @Strict
    @NullRestricted
    private MyValue_NestmateAccess vt;

    public Test1_NestmateAccess(final MyValue_NestmateAccess vt) {
        this.vt = vt;
    }

    public MyInterface_NestmateAccess test() {
        return new MyInterface_NestmateAccess() {
            // The vt field load does not link.
            private int x = (Test1_NestmateAccess.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Same as Test1_NestmateAccess but outer class is a value class
@LooselyConsistentValue
value class Test2_NestmateAccess {
    @Strict
    @NullRestricted
    private MyValue_NestmateAccess vt;

    public Test2_NestmateAccess(final MyValue_NestmateAccess vt) {
        this.vt = vt;
    }

    public MyInterface_NestmateAccess test() {
        return new MyInterface_NestmateAccess() {
            // Delayed flattened load of Test2_NestmateAccess.this.
            // The vt field load does not link.
            private int x = (Test2_NestmateAccess.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Test store to flattened field in nestmate when nest-host is not loaded.
class Test3_NestmateAccess {
    private MyValue_NestmateAccess vt;

    public MyInterface_NestmateAccess test(MyValue_NestmateAccess init) {
        return new MyInterface_NestmateAccess() {
            // Store to the vt field does not link.
            private MyValue_NestmateAccess tmp = (vt = init);

            @Override
            public int hash() { return tmp.hash() + vt.hash(); }
        };
    }
}

// Same as Test1_NestmateAccess but with static field
class Test4_NestmateAccess {
    private static MyValue_NestmateAccess vt = null;

    public Test4_NestmateAccess(final MyValue_NestmateAccess vt) {
        this.vt = vt;
    }

    public MyInterface_NestmateAccess test() {
        return new MyInterface_NestmateAccess() {
            // The vt field load does not link.
            private int x = (Test4_NestmateAccess.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Same as Test2_NestmateAccess but with static field
@LooselyConsistentValue
value class Test5_NestmateAccess {
    private static MyValue_NestmateAccess vt;

    public Test5_NestmateAccess(final MyValue_NestmateAccess vt) {
        this.vt = vt;
    }

    public MyInterface_NestmateAccess test() {
        return new MyInterface_NestmateAccess() {
            // Delayed flattened load of Test5_NestmateAccess.this.
            // The vt field load does not link.
            private int x = (Test5_NestmateAccess.this).vt.hash();

            @Override
            public int hash() { return x; }
        };
    }
}

// Same as Test3_NestmateAccess but with static field
class Test6_NestmateAccess {
    private static MyValue_NestmateAccess vt;

    public MyInterface_NestmateAccess test(MyValue_NestmateAccess init) {
        return new MyInterface_NestmateAccess() {
            // Store to the vt field does not link.
            private MyValue_NestmateAccess tmp = (vt = init);

            @Override
            public int hash() { return tmp.hash() + vt.hash(); }
        };
    }
}

// Same as Test6_NestmateAccess but outer class is a value class
@LooselyConsistentValue
value class Test7_NestmateAccess {
    private static MyValue_NestmateAccess vt;

    public MyInterface_NestmateAccess test(MyValue_NestmateAccess init) {
        return new MyInterface_NestmateAccess() {
            // Store to the vt field does not link.
            private MyValue_NestmateAccess tmp = (vt = init);

            @Override
            public int hash() { return tmp.hash() + vt.hash(); }
        };
    }
}

public class TestNestmateAccess {

    public static void main(String[] args) {
        Test1_NestmateAccess t1 = new Test1_NestmateAccess(new MyValue_NestmateAccess());
        int res = t1.test().hash();
        Asserts.assertEQ(res, 85);

        Test2_NestmateAccess t2 = new Test2_NestmateAccess(new MyValue_NestmateAccess());
        res = t2.test().hash();
        Asserts.assertEQ(res, 85);

        Test3_NestmateAccess t3 = new Test3_NestmateAccess();
        res = t3.test(new MyValue_NestmateAccess()).hash();
        Asserts.assertEQ(res, 170);

        Test4_NestmateAccess t4 = new Test4_NestmateAccess(new MyValue_NestmateAccess());
        res = t4.test().hash();
        Asserts.assertEQ(res, 85);

        Test5_NestmateAccess t5 = new Test5_NestmateAccess(new MyValue_NestmateAccess());
        res = t5.test().hash();
        Asserts.assertEQ(res, 85);

        Test6_NestmateAccess t6 = new Test6_NestmateAccess();
        res = t6.test(new MyValue_NestmateAccess()).hash();
        Asserts.assertEQ(res, 170);

        Test7_NestmateAccess t7 = new Test7_NestmateAccess();
        res = t7.test(new MyValue_NestmateAccess()).hash();
        Asserts.assertEQ(res, 170);
    }
}
