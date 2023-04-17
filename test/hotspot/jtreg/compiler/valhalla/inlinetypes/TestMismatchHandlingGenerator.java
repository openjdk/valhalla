/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

value class MyValue1 {
    int x = 42;

    void verify() {
        if (x != 42) {
            throw new RuntimeException("Verification failed");
        }
    }
}

value class MyValue2 {
    int x = 42;

    void verify() {
        if (x != 42) {
            throw new RuntimeException("Verification failed");
        }
    }
}

value class MyValue3 {
    int x = 42;

    void verify() {
        if (x != 42) {
            throw new RuntimeException("Verification failed");
        }
    }
}

value class MyValue4 {
    int x = 42;

    public void verify() {
        if (x != 42) {
            throw new RuntimeException("Verification failed");
        }
    }

    static MyValue4 make() {
        return new MyValue4();
    }
}

interface Verifiable {
    public void verify();
}

value class MyValue5 implements Verifiable {
    int x = 42;

    @Override
    public void verify() {
        if (x != 42) {
            throw new RuntimeException("Verification failed");
        }
    }

    static MyValue5 make() {
        return new MyValue5();
    }
}

value class MyValue6 implements Verifiable {
    int x = 42;

    @Override
    public void verify() {
        if (x != 42) {
            throw new RuntimeException("Verification failed");
        }
    }

    static MyValue6 make() {
        return new MyValue6();
    }
}

value class MyValue7 {
    int x = 42;

    void verify() {
        if (x != 42) {
            throw new RuntimeException("Verification failed");
        }
    }
}

class A {
    public MyValue1 method(MyValue1 arg) {
        arg.verify();
        return arg;
    }
}

class B extends A {
    @Override
    public MyValue1 method(MyValue1 arg) {
        arg.verify();
        return arg;
    }
}

class C extends B {
    @Override
    public MyValue1 method(MyValue1 arg) {
        arg.verify();
        return arg;
    }
}

interface I1 {
    public MyValue2 method(MyValue2 arg);
}

interface I2 extends I1 {
    public MyValue2 method(MyValue2 arg);
}

interface I3 {
    public MyValue2 method(MyValue2 arg);
}

interface I4 extends I3 {
    public MyValue2 method(MyValue2 arg);
}


class D implements I2 {
    @Override
    public MyValue2 method(MyValue2 arg) {
        arg.verify();
        return arg;
    }
}

class E implements I4 {
    @Override
    public MyValue2 method(MyValue2 arg) {
        arg.verify();
        return arg;
    }
}


class F implements I2, I4 {
    @Override
    public MyValue2 method(MyValue2 arg) {
        arg.verify();
        return arg;
    }
}

class G implements I2, I4 {
    @Override
    public MyValue2 method(MyValue2 arg) {
        arg.verify();
        return arg;
    }
}

interface I5 {
    public MyValue3 method(MyValue3 arg);
}

class H implements I5 {
    @Override
    public MyValue3 method(MyValue3 arg) {
        arg.verify();
        return arg;
    }
}

class J {
    public MyValue3 method(MyValue3 arg) {
        arg.verify();
        return arg;
    }
}

class K extends J {
    @Override
    public MyValue3 method(MyValue3 arg) {
        arg.verify();
        return arg;
    }
}

class L extends K implements I5 {
    @Override
    public MyValue3 method(MyValue3 arg) {
        arg.verify();
        return arg;
    }
}

class M {
    int val = 0;

    public MyValue4 method(boolean warmup) {
        if (warmup) {
            return null;
        } else {
            MyValue4 res = MyValue4.make();
            // Do something here to "corrupt" registers
            for (int i = 0; i < 10; ++i) {
                val++;
            }
            return res;
        }
    }
}

class N {
    public MyValue5 method(boolean warmup) {
        if (warmup) {
            return null;
        } else {
            return MyValue5.make();
        }
    }
}

class O {
    public MyValue6 method(boolean warmup) {
        if (warmup) {
            return null;
        } else {
            return MyValue6.make();
        }
    }
}

interface I6 {
    default MyValue7 method(MyValue7 arg) {
        return null;
    }
}

class P implements I6 {
    @Override
    public MyValue7 method(MyValue7 arg) {
        arg.verify();
        return arg;
    }
}

class Q {
    MyValue7 method(MyValue7 arg) {
        arg.verify();
        return arg;
    }
}

class R extends Q {
    @Override
    MyValue7 method(MyValue7 arg) {
        arg.verify();
        return arg;
    }
}

class S extends R implements I6 {
    @Override
    public MyValue7 method(MyValue7 arg) {
        arg.verify();
        return arg;
    }
}

class TestMismatchHandlingHelper {
    // * = has preload attribute for MyValue*

    // With C <: B* <: A
    public static void test1(A a1, A a2, A a3, A a4, A a5, B b1, B b2, C c) {
        // Non-scalarized virtual call site, mismatching on B
        a1.method(new MyValue1()).verify();
        a2.method(new MyValue1()).verify();
        a3.method(new MyValue1()).verify();
        a4.method(new MyValue1()).verify();
        a5.method(new MyValue1()).verify();
        // Scalarized virtual call sites, mismatching on C
        b1.method(new MyValue1()).verify();
        b2.method(new MyValue1()).verify();
        c.method(new MyValue1()).verify();
    }

    // D  <: I2  <: I1
    // E* <: I4* <: I3*
    // Loaded later, combine both hierachies and introduce a mismatch:
    // F  <: I2, I4*
    // G* <: I2, I4*
    public static void test2(I1 i11, I1 i12, I1 i13, I1 i14, I1 i15, I1 i16, I2 i21, I2 i22, I2 i23, I2 i24, I2 i25, I2 i26, I3 i31, I3 i32, I3 i33, I3 i34, I3 i35, I3 i36, I4 i41, I4 i42, I4 i43, I4 i44, I4 i45, I4 i46, D d, E e) {
        // Non-scalarized virtual call sites, mismatching on E
        i11.method(new MyValue2()).verify();
        i12.method(new MyValue2()).verify();
        i13.method(new MyValue2()).verify();
        i14.method(new MyValue2()).verify();
        i15.method(new MyValue2()).verify();
        i16.method(new MyValue2()).verify();
        i21.method(new MyValue2()).verify();
        i22.method(new MyValue2()).verify();
        i23.method(new MyValue2()).verify();
        i24.method(new MyValue2()).verify();
        i25.method(new MyValue2()).verify();
        i26.method(new MyValue2()).verify();
        d.method(new MyValue2()).verify();
        // Scalarized virtual call sites, mismatching on D
        i31.method(new MyValue2()).verify();
        i32.method(new MyValue2()).verify();
        i33.method(new MyValue2()).verify();
        i34.method(new MyValue2()).verify();
        i35.method(new MyValue2()).verify();
        i36.method(new MyValue2()).verify();
        i41.method(new MyValue2()).verify();
        i42.method(new MyValue2()).verify();
        i43.method(new MyValue2()).verify();
        i44.method(new MyValue2()).verify();
        i45.method(new MyValue2()).verify();
        i46.method(new MyValue2()).verify();
        e.method(new MyValue2()).verify();
    }

    // H  <: I5
    // K* <: J*
    // Loaded later, combines both hierachies and introduces a mismatch:
    // L* <: K*, I5
    public static void test3(I5 i51, I5 i52, I5 i53, J j1, J j2, J j3, J j4, J j5, H h, K k) {
        // Non-scalarized virtual call sites, mismatching on L
        i51.method(new MyValue3()).verify();
        i52.method(new MyValue3()).verify();
        i53.method(new MyValue3()).verify();
        h.method(new MyValue3()).verify();
        // Scalarized virtual call sites
        j1.method(new MyValue3()).verify();
        j2.method(new MyValue3()).verify();
        j3.method(new MyValue3()).verify();
        j4.method(new MyValue3()).verify();
        j5.method(new MyValue3()).verify();
        k.method(new MyValue3()).verify();
    }

    // Test that a C1 compiled method returns in scalarized form if the method holder class M
    // is loaded but the value class return type is not due to a missing preload attribute.
    public static void test4(M m, boolean warmup) {
        if (warmup) {
            m.method(warmup);
        } else {
            if (m.method(warmup).x != 42) {
                throw new RuntimeException("Verification failed");
            }
        }
    }

    // Test that C1 correctly handles scalarized returns at calls if the method holder class N
    // is loaded but the value class return type is not due to a missing preload attribute.
    public static void test5(N n, boolean warmup) {
        Verifiable res = n.method(warmup);
        if (!warmup) {
            res.verify();
        }
    }

    // Test direct calls
    public static void test6(F f, G g, L l) {
        f.method(new MyValue2());
        g.method(new MyValue2());
        l.method(new MyValue3());
    }

    // Test scalarized return from C2 compiled callee to C2 compiled caller with an unloaded
    // return type at caller compile time due to a missing preload attribute.
    public static Verifiable test7(O o, boolean warmup) {
        return o.method(warmup);
    }

    public static void test7TriggerCalleeCompilation(O o) {
        o.method(true);
        o.method(false).verify();
    }

    // Same as test3 but with default method in interface and package private methods
    // P  <: I6
    // R* <: Q*
    // Loaded later, combines both hierachies and introduces a mismatch:
    // S* <: R*, I6
    public static void test8(I6 i61, I6 i62, I6 i63, Q q1, Q q2, Q q3, Q q4, Q q5, P p, R r) {
        // Non-scalarized virtual call sites, mismatching on S
        i61.method(new MyValue7()).verify();
        i62.method(new MyValue7()).verify();
        i63.method(new MyValue7()).verify();
        p.method(new MyValue7()).verify();
        // Scalarized virtual call sites
        q1.method(new MyValue7()).verify();
        q2.method(new MyValue7()).verify();
        q3.method(new MyValue7()).verify();
        q4.method(new MyValue7()).verify();
        q5.method(new MyValue7()).verify();
        r.method(new MyValue7()).verify();
    }
}
