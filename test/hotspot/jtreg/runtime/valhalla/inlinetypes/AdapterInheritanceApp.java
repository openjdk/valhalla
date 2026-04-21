/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

public class AdapterInheritanceApp {
    public static void main(String[] args) {
        A a = new A();
        B b = new B();
        C c = new C();

        for (int i = 0; i < 100000; i++) {
            // A.foo() should be non-scalarized because Point is not present in the
            // LoadableDescriptors attribute for either class A or interface I
            a.foo(new Point(0, 0));

            // B.foo() will inherit the adapter from A.foo() and will also be non-scalarized.
            b.foo(new Point(0, 1));

            // C.foo() will try to inherit the non-scalarizeda dapter from B.foo() but it
            // will run into a mismatch when checking J.foo() which has scalarized calling
            // conventions for foo(). The calling conventions must then be recalculated.
            c.foo(new Point(1, 0));
        }
    }
}

value class Point {
    int x;
    int y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

// For this test, these classes are derived from JCOD files which remove Point
// from the LoadableDescriptors attribute so that Point will not be loaded when
// trying to link foo()
//
// interface I {
//     void foo(Point i);
// }

// class A implements I {
//     void foo(Point i) {}
// }

interface J {
    void foo(Point i);
}

class B extends A {
    public void foo(Point i) {}
}

class C extends B implements J {
    public void foo(Point i) {}
}
