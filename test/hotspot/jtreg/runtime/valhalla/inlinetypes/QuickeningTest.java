/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test QuickeningTest
 * @summary Test quickening of getfield and putfield applied to inline fields
 * @library /test/lib
 * @compile -XDallowWithFieldOperator Point.java JumboInline.java QuickeningTest.java
 * @run main/othervm -Xint runtime.valhalla.inlinetypes.QuickeningTest
 * @run main/othervm -Xcomp runtime.valhalla.inlinetypes.QuickeningTest
 */

public class QuickeningTest {

    static class Parent {
    Point.ref nfp;       /* Not flattenable inline field */
    Point fp;         /* Flattenable and flattened inline field */
    JumboInline fj;    /* Flattenable not flattened inline field */

        public void setNfp(Point p) { nfp = p; }
        public void setFp(Point p) { fp = p; }
        public void setFj(JumboInline j) { fj = j; }
    }

    static class Child extends Parent {
        // This class inherited fields from the Parent class
        Point.ref nfp2;      /* Not flattenable inline field */
        Point fp2;        /* Flattenable and flattened inline field */
        JumboInline fj2;   /* Flattenable not flattened inline field */

        public void setNfp2(Point p) { nfp2 = p; }
        public void setFp2(Point p)  { fp2 = p; }
        public void setFj2(JumboInline j) { fj2 = j; }
    }

    static final primitive class Value {
        final Point.ref nfp;       /* Not flattenable inline field */
        final Point fp;         /* Flattenable and flattened inline field */
        final JumboInline fj;    /* Flattenable not flattened inline field */

        private Value() {
            nfp = Point.createPoint(0, 0);
            fp = Point.createPoint(0, 0);
            fj = JumboInline.createJumboInline();
        }

        public static Value create() {
            return Value.default;
        }
    }

    static void testUninitializedFields() {
        Parent p = new Parent();
        Asserts.assertEquals(p.nfp, null, "invalid uninitialized not flattenable");
        Asserts.assertEquals(p.fp.x, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(p.fp.y, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(p.fj.l0, 0L, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(p.fj.l1, 0L, "invalid value for uninitialized flattened field");

        Child c = new Child();
        Asserts.assertEquals(c.nfp, null, "invalid uninitialized not flattenable field");
        Asserts.assertEquals(c.fp.x, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(c.fp.y, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(c.fj.l0, 0L, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(c.fj.l1, 0L, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(c.nfp2, null, "invalid uninitialized not flattenable");
        Asserts.assertEquals(c.fp2.x, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(c.fp2.y, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(c.fj2.l0, 0L, "invalid value for uninitialized not flattened field");
        Asserts.assertEquals(c.fj2.l1, 0L, "invalid value for uninitialized not flattened field");

        Value v = Value.create();
        Asserts.assertEquals(v.nfp, null, "invalid uninitialized not flattenable");
        Asserts.assertEquals(v.fp.x, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(v.fp.y, 0, "invalid value for uninitialized flattened field");
        Asserts.assertEquals(v.fj.l0, 0L, "invalid value for uninitialized not flattened field");
        Asserts.assertEquals(v.fj.l1, 0L, "invalid value for uninitialized not flattened field");
    }

    static void testPutfieldAndGetField() {
        Point p1 = Point.createPoint(16, 47);
        Point p2 = Point.createPoint(32, 64);

        JumboInline j1 = JumboInline.createJumboInline().update(4, 5);
        JumboInline j2 = JumboInline.createJumboInline().update(7, 9);

        Parent p = new Parent();
        // executing each setter twice to test quickened bytecodes
        p.setNfp(p1);
        p.setNfp(p2);
        p.setFp(p2);
        p.setFp(p1);
        p.setFj(j1);
        p.setFj(j2);

        Asserts.assertTrue(p.nfp.equals(p2), "invalid updated not flattenable field");
        Asserts.assertEquals(p.fp.x, 16, "invalid value for updated flattened field");
        Asserts.assertEquals(p.fp.y, 47, "invalid value for updated flattened field");
        Asserts.assertTrue(p.fj.equals(j2), "invalid value for updated not flattened field");

        Child c = new Child();
        c.setNfp(p1);
        c.setNfp(p2);
        c.setFp(p2);
        c.setFp(p1);
        c.setFj(j1);
        c.setFj(j2);
        c.setNfp2(p2);
        c.setNfp2(p1);
        c.setFp2(p1);
        c.setFp2(p2);
        c.setFj2(j2);
        c.setFj2(j1);

        Asserts.assertTrue(c.nfp.equals(p2), "invalid updated not flattenable field");
        Asserts.assertEquals(c.fp.x, 16, "invalid value for updated flattened field");
        Asserts.assertEquals(c.fp.y, 47, "invalid value for updated flattened field");
        Asserts.assertTrue(c.fj.equals(j2), "invalid value for updated not flattened field");

        Asserts.assertTrue(c.nfp2.equals(p1), "invalid updated not flattenable field");
        Asserts.assertEquals(c.fp2.x, 32, "invalid value for updated flattened field");
        Asserts.assertEquals(c.fp2.y, 64, "invalid value for updated flattened field");
        Asserts.assertTrue(c.fj2.equals(j1), "invalid value for updated not flattened field");
    }

    public static void main(String[] args) {
        testUninitializedFields();
        testUninitializedFields(); // run twice to test quickened bytecodes
        testPutfieldAndGetField();
    }
}
