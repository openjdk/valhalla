/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

import jdk.test.lib.Asserts;

// TODO test with floats/doubles, also add oops

/*
 * @test
 * @key randomness
 * @summary Test support for null markers in flat fields.
 * @library /test/lib /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @enablePreview
 * @modules java.base/jdk.internal.vm.annotation
 * @run main/othervm -Xbatch -XX:+NullableFieldFlattening
 *                   compiler.valhalla.inlinetypes.TestFieldNullMarkers
 * @run main/othervm -Xbatch -XX:+NullableFieldFlattening
 *                   -XX:CompileCommand=dontinline,*::testHelper*
 *                   compiler.valhalla.inlinetypes.TestFieldNullMarkers
 * @run main/othervm -Xbatch -XX:+NullableFieldFlattening
 *                   -XX:+InlineTypeReturnedAsFields -XX:+InlineTypePassFieldsAsArgs
 *                   compiler.valhalla.inlinetypes.TestFieldNullMarkers
 * @run main/othervm -Xbatch -XX:+NullableFieldFlattening
 *                   -XX:-InlineTypeReturnedAsFields -XX:-InlineTypePassFieldsAsArgs
 *                   compiler.valhalla.inlinetypes.TestFieldNullMarkers
 * @run main/othervm -Xbatch -XX:+NullableFieldFlattening
 *                   -XX:+InlineTypeReturnedAsFields -XX:-InlineTypePassFieldsAsArgs
 *                   compiler.valhalla.inlinetypes.TestFieldNullMarkers
 * @run main/othervm -Xbatch -XX:+NullableFieldFlattening
 *                   -XX:-InlineTypeReturnedAsFields -XX:+InlineTypePassFieldsAsArgs
 *                   compiler.valhalla.inlinetypes.TestFieldNullMarkers
 */

/*
TODO
-XX:-AlwaysIncrementalInline
-XX:CompileCommand=compileonly,*::* -XX:CompileCommand=quiet
-XX:+Verbose -XX:CompileCommand=print,*::* -XX:+PrintIdeal -XX:+PrintFieldLayout -XX:+TraceDeoptimization -XX:+PrintDeoptimizationDetails
*/
 
public class TestFieldNullMarkers {

    // Value class with two nullable flat fields 
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue1 {
        byte x;
        MyValue2 val1;
        MyValue2 val2;

        public MyValue1(byte x, MyValue2 val1, MyValue2 val2) {
            this.x = x;
            this.val1 = val1;
            this.val2 = val2;
        }

        public String toString() {
            return "x = " + x + ", val1 = [" + val1 + "], val2 = [" + val2 + "]";
        }
    }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static abstract value class MyAbstract1 {
        byte x;

        public MyAbstract1(byte x) {
            this.x = x; 
        }
    }

    // Empty value class inheriting single field from abstract super class
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue2 extends MyAbstract1 {
        public MyValue2(byte x) {
            super(x);
        }

        public String toString() {
            return "x = " + x;
        }
    }

    // Value class with a hole in the payload that will be used for the null marker
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue3 {
        byte x;
        // Hole that will be used by the null marker
        int i;

        public MyValue3(byte x) {
            this.x = x;
            this.i = x;
        }

        public String toString() {
            return "x = " + x + ", i = " + i;
        }
    }

    // Value class with two nullable flat fields that have their null markers *not* at the end of the payload
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue4 {
        MyValue3 val1;
        MyValue3 val2;

        public MyValue4(MyValue3 val1, MyValue3 val2) {
            this.val1 = val1;
            this.val2 = val2;
        }

        public String toString() {
            return "val1 = [" + val1 + "], val2 = [" + val2 + "]";
        }
    }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue5_3 {
        byte x;

        public MyValue5_3(byte x) {
            this.x = x;
        }
    }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue5_2 {
        byte x;
        MyValue5_3 val;

        public MyValue5_2(byte x, MyValue5_3 val) {
            this.x = x;
            this.val = val;
        }
    }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue5_1 {
        byte x;
        MyValue5_2 val;

        public MyValue5_1(byte x, MyValue5_2 val) {
            this.x = x;
            this.val = val;
        }
    }

    // Value class with deep nesting of nullable flat fields
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue5 {
        byte x;
        MyValue5_1 val;

        public MyValue5(byte x, MyValue5_1 val) {
            this.x = x;
            this.val = val;
        }
    }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValueEmpty {

    }

    // Value class with flat field of empty value class
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue6 {
        MyValueEmpty val;

        public MyValue6(MyValueEmpty val) {
            this.val = val;
        }
    }

    // Same as MyValue6 but one more level of nested flat fields
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class MyValue7 {
        MyValue6 val;

        public MyValue7(MyValue6 val) {
            this.val = val;
        }
    }

    MyValue1 field1; // Flat
    MyValue4 field2; // Not flat
    MyValue5 field3; // Not flat
    MyValue6 field4; // Flat
    MyValue7 field5; // Flat

    static final MyValue1 VAL1 = new MyValue1((byte)42, new MyValue2((byte)43), null);
    static final MyValue4 VAL4 = new MyValue4(new MyValue3((byte)42), null);
    static final MyValue5 VAL5 = new MyValue5((byte)42, new MyValue5_1((byte)43, new MyValue5_2((byte)44, new MyValue5_3((byte)45))));
    static final MyValue6 VAL6 = new MyValue6(new MyValueEmpty());
    static final MyValue7 VAL7 = new MyValue7(new MyValue6(new MyValueEmpty()));

    // Test that the calling convention is keeping track of the null marker
    public MyValue1 testHelper1(MyValue1 val) {
        return val;
    }

    public void testSet1(MyValue1 val) {
        field1 = testHelper1(val);
    }
    
    public MyValue1 testGet1() {
        return field1;
    }

    public void testDeopt1(byte x, MyValue1 neverNull, MyValue1 alwaysNull, boolean deopt) {
        MyValue2 val2 = new MyValue2(x);
        MyValue1 val1 = new MyValue1(x, val2, val2);
        if (deopt) {
            Asserts.assertEQ(val1.x, x);
            Asserts.assertEQ(val1.val1, val2);
            Asserts.assertEQ(val1.val2, val2);
            Asserts.assertEQ(neverNull.x, x);
            Asserts.assertEQ(neverNull.val1, val2);
            Asserts.assertEQ(neverNull.val2, val2);
            Asserts.assertEQ(alwaysNull.x, x);
            Asserts.assertEQ(alwaysNull.val1, null);
            Asserts.assertEQ(alwaysNull.val2, null);
        }
    }

    public void testOSR() {
        // Trigger OSR
        for (int i = 0; i < 100_000; ++i) {
            field1 = null;
            Asserts.assertEQ(field1, null);
            MyValue2 val2 = new MyValue2((byte)i);
            MyValue1 val = new MyValue1((byte)i, val2, null);
            field1 = val;
            Asserts.assertEQ(field1.x, (byte)i);
            Asserts.assertEQ(field1.val1, val2);
            Asserts.assertEQ(field1.val2, null);
        }
    }

    public boolean testACmp(MyValue2 val2) {
        return field1.val1 == val2;
    }

    // Test that the calling convention is keeping track of the null marker
    public MyValue4 testHelper2(MyValue4 val) {
        return val;
    }

    public void testSet2(MyValue4 val) {
        field2 = testHelper2(val);
    }

    public MyValue4 testGet2() {
        return field2;
    }

    public void testDeopt2(byte x, MyValue4 neverNull, MyValue4 alwaysNull, boolean deopt) {
        MyValue3 val3 = new MyValue3(x);
        MyValue4 val4 = new MyValue4(val3, null);
        if (deopt) {
            Asserts.assertEQ(val4.val1, val3);
            Asserts.assertEQ(val4.val2, null);
            Asserts.assertEQ(neverNull.val1, val3);
            Asserts.assertEQ(neverNull.val2, val3);
            Asserts.assertEQ(alwaysNull.val1, null);
            Asserts.assertEQ(alwaysNull.val2, null);
        }
    }

    // Test that the calling convention is keeping track of the null marker
    public MyValue5 testHelper3(MyValue5 val) {
        return val;
    }

    public void testSet3(MyValue5 val) {
        field3 = testHelper3(val);
    }

    public MyValue5 testGet3() {
        return field3;
    }

    public void testDeopt3(byte x, MyValue5 val6, MyValue5 val7, MyValue5 val8, MyValue5 val9, boolean deopt) {
        MyValue5 val1 = new MyValue5(x, new MyValue5_1(x, new MyValue5_2(x, new MyValue5_3(x))));
        MyValue5 val2 = new MyValue5(x, new MyValue5_1(x, new MyValue5_2(x, null)));
        MyValue5 val3 = new MyValue5(x, new MyValue5_1(x, null));
        MyValue5 val4 = new MyValue5(x, null);
        MyValue5 val5 = null;
        if (deopt) {
            Asserts.assertEQ(val1.x, x);
            Asserts.assertEQ(val1.val.x, x);
            Asserts.assertEQ(val1.val.val.x, x);
            Asserts.assertEQ(val1.val.val.val.x, x);
            Asserts.assertEQ(val2.x, x);
            Asserts.assertEQ(val2.val.x, x);
            Asserts.assertEQ(val2.val.val.x, x);
            Asserts.assertEQ(val2.val.val.val, null);
            Asserts.assertEQ(val3.x, x);
            Asserts.assertEQ(val3.val.x, x);
            Asserts.assertEQ(val3.val.val, null);
            Asserts.assertEQ(val4.x, x);
            Asserts.assertEQ(val4.val, null);
            Asserts.assertEQ(val5, null);

            Asserts.assertEQ(val6.x, x);
            Asserts.assertEQ(val6.val.x, x);
            Asserts.assertEQ(val6.val.val.x, x);
            Asserts.assertEQ(val6.val.val.val.x, x);
            Asserts.assertEQ(val7.x, x);
            Asserts.assertEQ(val7.val.x, x);
            Asserts.assertEQ(val7.val.val.x, x);
            Asserts.assertEQ(val7.val.val.val, null);
            Asserts.assertEQ(val8.x, x);
            Asserts.assertEQ(val8.val.x, x);
            Asserts.assertEQ(val8.val.val, null);
            Asserts.assertEQ(val9.x, x);
            Asserts.assertEQ(val9.val, null);
        }
    }

    // Test that the calling convention is keeping track of the null marker
    public MyValue6 testHelper4(MyValue6 val) {
        return val;
    }

    public void testSet4(MyValue6 val) {
        field4 = testHelper4(val);
    }

    public MyValue6 testGet4() {
        return field4;
    }

    public void testDeopt4(MyValue6 val4, MyValue6 val5, MyValue6 val6, boolean deopt) {
        MyValue6 val1 = new MyValue6(new MyValueEmpty());
        MyValue6 val2 = new MyValue6(null);
        MyValue6 val3 = null;
        if (deopt) {
            Asserts.assertEQ(val1.val, new MyValueEmpty());
            Asserts.assertEQ(val2.val, null);
            Asserts.assertEQ(val3, null);

            Asserts.assertEQ(val4.val, new MyValueEmpty());
            Asserts.assertEQ(val5.val, null);
            Asserts.assertEQ(val6, null);
        }
    }

    // Test that the calling convention is keeping track of the null marker
    public MyValue7 testHelper5(MyValue7 val) {
        return val;
    }

    public void testSet5(MyValue7 val) {
        field5 = testHelper5(val);
    }

    public MyValue7 testGet5() {
        return field5;
    }

    public void testDeopt5(MyValue7 val5, MyValue7 val6, MyValue7 val7, MyValue7 val8, boolean deopt) {
        MyValue7 val1 = new MyValue7(new MyValue6(new MyValueEmpty()));
        MyValue7 val2 = new MyValue7(new MyValue6(null));
        MyValue7 val3 = new MyValue7(null);
        MyValue7 val4 = null;
        if (deopt) {
            Asserts.assertEQ(val1.val, new MyValue6(new MyValueEmpty()));
            Asserts.assertEQ(val2.val, new MyValue6(null));
            Asserts.assertEQ(val3.val, null);
            Asserts.assertEQ(val4, null);

            Asserts.assertEQ(val5.val, new MyValue6(new MyValueEmpty()));
            Asserts.assertEQ(val6.val, new MyValue6(null));
            Asserts.assertEQ(val7.val, null);
            Asserts.assertEQ(val8, null);
        }
    }

    public static void main(String[] args) {
        TestFieldNullMarkers t = new TestFieldNullMarkers();
        t.testOSR();
        for (int i = 0; i < 100_000; ++i) {
            t.field1 = null;
            Asserts.assertEQ(t.testGet1(), null);

            boolean useNull = (i % 2) == 0;
            MyValue2 val2 = useNull ? null : new MyValue2((byte)i);
            MyValue1 val = new MyValue1((byte)i, val2, val2);
            t.field1 = val;
            Asserts.assertEQ(t.testGet1().x, val.x);
            Asserts.assertEQ(t.testGet1().val1, val2);
            Asserts.assertEQ(t.testGet1().val2, val2);
            // TODO The substitutability test uses Unsafe
            // Asserts.assertEQ(t.testGet1(), val);

            Asserts.assertTrue(t.testACmp(val2));

            t.testSet1(null);
            Asserts.assertEQ(t.field1, null);

            t.testSet1(val);
            Asserts.assertEQ(t.field1.x, val.x);
            Asserts.assertEQ(t.field1.val1, val2);
            Asserts.assertEQ(t.field1.val2, val2);
            // TODO The substitutability test uses Unsafe
            // Asserts.assertEQ(t.field1, val);

            t.testDeopt1((byte)i, null, null, false);

            t.field2 = null;
            Asserts.assertEQ(t.testGet2(), null);

            MyValue3 val3 = useNull ? null : new MyValue3((byte)i);
            MyValue4 val4 = new MyValue4(val3, val3);
            t.field2 = val4;
            Asserts.assertEQ(t.testGet2().val1, val3);
            Asserts.assertEQ(t.testGet2().val2, val3);

            t.testSet2(null);
            Asserts.assertEQ(t.testGet2(), null);

            t.testSet2(val4);
            Asserts.assertEQ(t.testGet2().val1, val3);
            Asserts.assertEQ(t.testGet2().val2, val3);

            t.testDeopt2((byte)i, null, null, false);

            t.field3 = null;
            Asserts.assertEQ(t.testGet3(), null);

            boolean useNull_1 = (i % 4) == 0;
            boolean useNull_2 = (i % 4) == 1;
            boolean useNull_3 = (i % 4) == 2;
            MyValue5_3 val5_3 = useNull_3 ? null : new MyValue5_3((byte)i);
            MyValue5_2 val5_2 = useNull_2 ? null : new MyValue5_2((byte)i, val5_3);
            MyValue5_1 val5_1 = useNull_1 ? null : new MyValue5_1((byte)i, val5_2);
            MyValue5 val5 = new MyValue5((byte)i, val5_1);
            t.field3 = val5;
            Asserts.assertEQ(t.testGet3().x, val5.x);
            if (useNull_1) {
                Asserts.assertEQ(t.testGet3().val, null);
            } else {
                Asserts.assertEQ(t.testGet3().val.x, val5_1.x);
                if (useNull_2) {
                    Asserts.assertEQ(t.testGet3().val.val, null);
                } else {
                    Asserts.assertEQ(t.testGet3().val.val.x, val5_2.x);
                    if (useNull_3) {
                        Asserts.assertEQ(t.testGet3().val.val.val, null);
                    } else {
                        Asserts.assertEQ(t.testGet3().val.val.val.x, val5_3.x);
                    }
                }
            }

            t.testSet3(null);
            Asserts.assertEQ(t.field3, null);

            t.testSet3(val5);
            Asserts.assertEQ(t.testGet3().x, val5.x);
            if (useNull_1) {
                Asserts.assertEQ(t.testGet3().val, null);
            } else {
                Asserts.assertEQ(t.testGet3().val.x, val5_1.x);
                if (useNull_2) {
                    Asserts.assertEQ(t.testGet3().val.val, null);
                } else {
                    Asserts.assertEQ(t.testGet3().val.val.x, val5_2.x);
                    if (useNull_3) {
                        Asserts.assertEQ(t.testGet3().val.val.val, null);
                    } else {
                        Asserts.assertEQ(t.testGet3().val.val.val.x, val5_3.x);
                    }
                }
            }
            t.testDeopt3((byte)i, null, null, null, null, false);

            t.field4 = null;
            Asserts.assertEQ(t.testGet4(), null);

            MyValueEmpty empty = useNull ? null : new MyValueEmpty();
            MyValue6 val6 = new MyValue6(empty);
            t.field4 = val6;
            Asserts.assertEQ(t.testGet4().val, empty);

            t.testSet4(null);
            Asserts.assertEQ(t.testGet4(), null);

            t.testSet4(val6);
            Asserts.assertEQ(t.testGet4().val, empty);

            t.testDeopt4(null, null, null, false);

            t.field5 = null;
            Asserts.assertEQ(t.testGet5(), null);

            empty = ((i % 3) == 0) ? null : new MyValueEmpty();
            val6 = ((i % 3) == 1) ? null : new MyValue6(empty);
            MyValue7 val7 = new MyValue7(val6);
            t.field5 = val7;
            Asserts.assertEQ(t.testGet5().val, val6);

            t.testSet5(null);
            Asserts.assertEQ(t.testGet5(), null);

            t.testSet5(val7);
            Asserts.assertEQ(t.testGet5().val, val6);

            t.testDeopt5(null, null, null, null, false);

            // Check accesses with constant value
            t.field1 = VAL1;
            Asserts.assertEQ(t.field1.x, VAL1.x);
            Asserts.assertEQ(t.field1.val1, VAL1.val1);
            Asserts.assertEQ(t.field1.val2, VAL1.val2);

            t.field2 = VAL4;
            Asserts.assertEQ(t.field2.val1, VAL4.val1);
            Asserts.assertEQ(t.field2.val2, VAL4.val2);

            t.field3 = VAL5;
            Asserts.assertEQ(t.field3.x, VAL5.x);
            Asserts.assertEQ(t.field3.val.x, VAL5.val.x);
            Asserts.assertEQ(t.field3.val.val.x, VAL5.val.val.x);
            Asserts.assertEQ(t.field3.val.val.val.x, VAL5.val.val.val.x);

            t.field4 = VAL6;
            Asserts.assertEQ(t.field4.val, VAL6.val);

            t.field5 = VAL7;
            Asserts.assertEQ(t.field5.val, VAL7.val);
        }

        // Trigger deoptimization to check that re-materialization takes the null marker into account
        byte x = (byte)42;
        t.testDeopt1(x, new MyValue1(x, new MyValue2(x), new MyValue2(x)), new MyValue1(x, null, null), true);
        t.testDeopt2(x, new MyValue4(new MyValue3(x), new MyValue3(x)), new MyValue4(null, null), true);

        MyValue5 val1 = new MyValue5(x, new MyValue5_1(x, new MyValue5_2(x, new MyValue5_3(x))));
        MyValue5 val2 = new MyValue5(x, new MyValue5_1(x, new MyValue5_2(x, null)));
        MyValue5 val3 = new MyValue5(x, new MyValue5_1(x, null));
        MyValue5 val4 = new MyValue5(x, null);
        t.testDeopt3(x, val1, val2, val3, val4, true);

        MyValue6 val5 = new MyValue6(new MyValueEmpty());
        MyValue6 val6 = new MyValue6(null);
        MyValue6 val7 = null;
        t.testDeopt4(val5, val6, val7, true);

        MyValue7 val8 = new MyValue7(new MyValue6(new MyValueEmpty()));
        MyValue7 val9 = new MyValue7(new MyValue6(null));
        MyValue7 val10 = new MyValue7(null);
        MyValue7 val11 = null;
        t.testDeopt5(val8, val9, val10, val11, false);
    }
}

