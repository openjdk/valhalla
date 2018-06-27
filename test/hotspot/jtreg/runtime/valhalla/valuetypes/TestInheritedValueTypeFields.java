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

import jdk.test.lib.Asserts;

/*
 * @test TestInheritedValueTypeFields
 * @summary Test if value field klasses are correctly retrieved for inherited fields
 * @library /test/lib
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers Point.java TestInheritedValueTypeFields.java
 * @run main/othervm -XX:+EnableValhalla runtime.valhalla.valuetypes.TestInheritedValueTypeFields
 */

class A {
    __Flattenable Point p;
}

class B extends A {

}

class C extends B {
    int i;
}

class D {
    long l;
}

class E extends D {
    __Flattenable Point p1;
}

class F extends E {

}

class G extends F {
    __Flattenable Point p2;
}

public class TestInheritedValueTypeFields {

    public static void main(String[] args) {
        for (int i = 0; i < 100000; i++) {
            run();
        }
    }

    public static void run() {
        B b = new B();
        Asserts.assertEquals(b.p.x, 0);
        Asserts.assertEquals(b.p.y, 0);
        b.p = Point.createPoint(1,2);
        Asserts.assertEquals(b.p.x, 1);
        Asserts.assertEquals(b.p.y, 2);

        G g = new G();
        Asserts.assertEquals(g.p1.x, 0);
        Asserts.assertEquals(g.p1.y, 0);
        Asserts.assertEquals(g.p2.x, 0);
        Asserts.assertEquals(g.p2.y, 0);
        g.p1 = Point.createPoint(1,2);
        g.p2 = Point.createPoint(3,4);
        Asserts.assertEquals(g.p1.x, 1);
        Asserts.assertEquals(g.p1.y, 2);
        Asserts.assertEquals(g.p2.x, 3);
        Asserts.assertEquals(g.p2.y, 4);
    }
}
