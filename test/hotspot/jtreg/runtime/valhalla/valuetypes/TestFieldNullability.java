/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestFieldNullability
 * @library /test/lib
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowWithFieldOperator TestFieldNullability.java
 * @run main/othervm -Xint -Xmx128m -XX:-ShowMessageBoxOnError -XX:ValueFieldMaxFlatSize=32
 *                   runtime.valhalla.valuetypes.TestFieldNullability
 */

package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

public class TestFieldNullability {
    static inline class MyValue {
        int x;

        public MyValue() {
            x = 314;
        }
    }

    static inline class MyBigValue {
        long l0, l1, l2, l3, l4, l5, l6, l7, l8, l9;
        long l10, l11, l12, l13, l14, l15, l16, l17, l18, l19;

        public MyBigValue() {
            l0 = l1 = l2 = l3 = l4 = l5 = l6 = l7 = l8 = l9 = 271;
            l10 = l11 = l12 = l13 = l14 = l15 = l16 = l17 = l18 = l19 = 271;
        }
    }

    static inline class TestValue {
        final MyValue? nullableField;
        final MyValue nullfreeField;        // flattened
        final MyValue? nullField;           // src of null
        final MyBigValue nullfreeBigField;  // not flattened
        final MyBigValue? nullBigField;     // src of null

        public void test() {
            Asserts.assertNull(nullField, "Invalid non null value for uninitialized non flattenable field");
            Asserts.assertNull(nullBigField, "Invalid non null value for uninitialized non flattenable field");
            boolean NPE = false;
            try {
                TestValue tv = __WithField(this.nullableField, nullField);
            } catch(NullPointerException e) {
                NPE = true;
            }
            Asserts.assertFalse(NPE, "Invalid NPE when assigning null to a non flattenable field");
            try {
                TestValue tv = __WithField(this.nullfreeField, (MyValue) nullField);
            } catch(NullPointerException e) {
                NPE = true;
            }
            Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattened field");
            try {
                TestValue tv = __WithField(this.nullfreeBigField, (MyBigValue) nullBigField);
            } catch(NullPointerException e) {
                NPE = true;
            }
            Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattenable field");
        }

        public TestValue() {
            nullableField = MyValue.default;
            nullfreeField = MyValue.default;
            nullField = MyValue.default;           // fake assignment
            nullfreeBigField = MyBigValue.default;
            nullBigField = MyBigValue.default;     // fake assignment

        }
    }

    static class TestClass {
        MyValue? nullableField;
        MyValue nullfreeField;       // flattened
        MyValue? nullField;
        MyBigValue nullfreeBigField; // not flattened
        MyBigValue? nullBigField;

        public void test() {
            Asserts.assertNull(nullField, "Invalid non null value for uninitialized non flattenable field");
            Asserts.assertNull(nullBigField, "Invalid non null value for uninitialized non flattenable field");
            boolean NPE = false;
            try {
                nullableField = nullField;
            } catch(NullPointerException e) {
                NPE = true;
            }
            Asserts.assertFalse(NPE, "Invalid NPE when assigning null to a non flattenable field");
            try {
                this.nullfreeField = (MyValue) nullField;
            } catch(NullPointerException e) {
                NPE = true;
            }
            Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattened field");
            try {
                this.nullfreeBigField = (MyBigValue) nullBigField;
            } catch(NullPointerException e) {
                NPE = true;
            }
            Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattenable field");
        }
    }

    public static void main(String[] args) {
        TestClass tc = new TestClass();
        tc.test();
        TestValue tv =
            TestValue.default;
        tv.test();
    }

}
