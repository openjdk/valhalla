/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.inlinetypes;

import jdk.test.lib.Asserts;

/*
 * @test InlineTypeCreation
 * @summary Inline Type creation test
 * @library /test/lib
 * @compile -XDallowFlattenabilityModifiers InlineTypeCreation.java Point.java Long8Inline.java Person.java
 * @run main runtime.valhalla.inlinetypes.InlineTypeCreation
 */
public class InlineTypeCreation {
    public static void main(String[] args) {
        InlineTypeCreation inlineTypeCreation = new InlineTypeCreation();
        inlineTypeCreation.run();
    }

    public void run() {
        testPoint();
        testLong8();
        testPerson();
        StaticSelf.test();
    }

    void testPoint() {
        Point p = new Point(1, 2);
        Asserts.assertEquals(p.x, 1, "invalid point x value");
        Asserts.assertEquals(p.y, 2, "invalid point y value");
        Point p2 = clonePoint(p);
        Asserts.assertEquals(p2.x, 1, "invalid point clone x value");
        Asserts.assertEquals(p2.y, 2, "invalid point clone y value");
    }

    static Point clonePoint(Point p) {
        Point q = p;
        return q;
    }

    void testLong8() {
        Long8Inline long8Inline = new Long8Inline(1, 2, 3, 4, 5, 6, 7, 8);
        Asserts.assertEquals(long8Inline.getLongField1(), 1L, "Field 1 incorrect");
        Asserts.assertEquals(long8Inline.getLongField8(), 8L, "Field 8 incorrect");
        Long8Inline.check(long8Inline, 1, 2, 3, 4, 5, 6, 7, 8);
    }

    void testPerson() {
        Person person = new Person(1, "John", "Smith");
        Asserts.assertEquals(person.getId(), 1, "Id field incorrect");
        Asserts.assertEquals(person.getFirstName(), "John", "First name incorrect");
        Asserts.assertEquals(person.getLastName(), "Smith", "Last name incorrect");
    }

    static final primitive class StaticSelf {

        static final StaticSelf.ref DEFAULT = new StaticSelf(0);
        final int f1;

        public StaticSelf(int f1) { this.f1 = f1; }
        public String toString() { return "StaticSelf f1=" + f1; }

        public static void test() {
            String s = DEFAULT.toString();
        }

    }
}
