import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

public class Test {

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

    MyValue1 field1;
    MyValue4 field2;
    MyValue5 field3;
    MyValue6 field4;
    MyValue7 field5;

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
            if (val1.x != x) throw new RuntimeException("FAIL");
            if (val1.val1 != val2) throw new RuntimeException("FAIL");
            if (val1.val2 != val2) throw new RuntimeException("FAIL");
            if (neverNull.x != x) throw new RuntimeException("FAIL");
            if (neverNull.val1 != val2) throw new RuntimeException("FAIL");
            if (neverNull.val2 != val2) throw new RuntimeException("FAIL");
            if (alwaysNull.x != x) throw new RuntimeException("FAIL");
            if (alwaysNull.val1 != null) throw new RuntimeException("FAIL");
            if (alwaysNull.val2 != null) throw new RuntimeException("FAIL");
        }
    }

    public void testOSR() {
        // Trigger OSR
        for (int i = 0; i < 100_000; ++i) {
            field1 = null;
            if (field1 != null) throw new RuntimeException("FAIL1");
            MyValue2 val2 = new MyValue2((byte)i);
            MyValue1 val = new MyValue1((byte)i, val2, null);
            field1 = val;
            if (field1.x != (byte)i) throw new RuntimeException("FAIL");
            if (field1.val1 != val2) throw new RuntimeException("FAIL");
            if (field1.val2 != null) throw new RuntimeException("FAIL");
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
            if (val4.val1 != val3) throw new RuntimeException("FAIL" + val4.val1);
            if (val4.val2 != null) throw new RuntimeException("FAIL");
            if (neverNull.val1 != val3) throw new RuntimeException("FAIL");
            if (neverNull.val2 != val3) throw new RuntimeException("FAIL");
            if (alwaysNull.val1 != null) throw new RuntimeException("FAIL");
            if (alwaysNull.val2 != null) throw new RuntimeException("FAIL");
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
            if (val1.x != x) throw new RuntimeException("FAIL");
            if (val1.val.x != x) throw new RuntimeException("FAIL");
            if (val1.val.val.x != x) throw new RuntimeException("FAIL");
            if (val1.val.val.val.x != x) throw new RuntimeException("FAIL");
            if (val2.x != x) throw new RuntimeException("FAIL");
            if (val2.val.x != x) throw new RuntimeException("FAIL");
            if (val2.val.val.x != x) throw new RuntimeException("FAIL");
            if (val2.val.val.val != null) throw new RuntimeException("FAIL");
            if (val3.x != x) throw new RuntimeException("FAIL");
            if (val3.val.x != x) throw new RuntimeException("FAIL");
            if (val3.val.val != null) throw new RuntimeException("FAIL");
            if (val4.x != x) throw new RuntimeException("FAIL");
            if (val4.val != null) throw new RuntimeException("FAIL");
            if (val5 != null) throw new RuntimeException("FAIL");

            if (val6.x != x) throw new RuntimeException("FAIL");
            if (val6.val.x != x) throw new RuntimeException("FAIL");
            if (val6.val.val.x != x) throw new RuntimeException("FAIL");
            if (val6.val.val.val.x != x) throw new RuntimeException("FAIL");
            if (val7.x != x) throw new RuntimeException("FAIL");
            if (val7.val.x != x) throw new RuntimeException("FAIL");
            if (val7.val.val.x != x) throw new RuntimeException("FAIL");
            if (val7.val.val.val != null) throw new RuntimeException("FAIL");
            if (val8.x != x) throw new RuntimeException("FAIL");
            if (val8.val.x != x) throw new RuntimeException("FAIL");
            if (val8.val.val != null) throw new RuntimeException("FAIL");
            if (val9.x != x) throw new RuntimeException("FAIL");
            if (val9.val != null) throw new RuntimeException("FAIL");
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
            if (val1.val != new MyValueEmpty()) throw new RuntimeException("FAIL");
            if (val2.val != null) throw new RuntimeException("FAIL");
            if (val3 != null) throw new RuntimeException("FAIL");

            if (val4.val != new MyValueEmpty()) throw new RuntimeException("FAIL");
            if (val5.val != null) throw new RuntimeException("FAIL");
            if (val6 != null) throw new RuntimeException("FAIL");
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
            if (val1.val != new MyValue6(new MyValueEmpty())) throw new RuntimeException("FAIL");
            if (val2.val != new MyValue6(null)) throw new RuntimeException("FAIL");
            if (val3.val != null) throw new RuntimeException("FAIL");
            if (val4 != null) throw new RuntimeException("FAIL");

            if (val5.val != new MyValue6(new MyValueEmpty())) throw new RuntimeException("FAIL");
            if (val6.val != new MyValue6(null)) throw new RuntimeException("FAIL");
            if (val7.val != null) throw new RuntimeException("FAIL");
            if (val8 != null) throw new RuntimeException("FAIL");
        }
    }

    public static void main(String[] args) {
        Test t = new Test();
        t.testOSR();
        for (int i = 0; i < 100_000; ++i) {
            t.field1 = null;
            if (t.testGet1() != null) throw new RuntimeException("FAIL1");

            boolean useNull = (i % 2) == 0;
            MyValue2 val2 = useNull ? null : new MyValue2((byte)i);
            MyValue1 val = new MyValue1((byte)i, val2, val2);
            t.field1 = val;
            if (t.testGet1().x != val.x) throw new RuntimeException("FAIL2");
            if (t.testGet1().val1 != val2) throw new RuntimeException("FAIL2.2");
            if (t.testGet1().val2 != val2) throw new RuntimeException("FAIL2.3");
            // TODO The substitutability test uses Unsafe
            // if (t.testGet1() != val) throw new RuntimeException("FAIL2");

            if (!t.testACmp(val2)) throw new RuntimeException("FAIL2.2");

            t.testSet1(null);
            if (t.field1 != null) throw new RuntimeException("FAIL3");

            t.testSet1(val);
            if (t.field1.x != val.x) throw new RuntimeException("FAIL4");
            if (t.field1.val1 != val2) throw new RuntimeException("FAIL4.2");
            if (t.field1.val2 != val2) throw new RuntimeException("FAIL4.3");
            // TODO The substitutability test uses Unsafe
            // if (t.field1 != val) throw new RuntimeException("FAIL4");

            t.testDeopt1((byte)i, null, null, false);

            t.field2 = null;
            if (t.testGet2() != null) throw new RuntimeException("FAIL1");

            MyValue3 val3 = useNull ? null : new MyValue3((byte)i);
            MyValue4 val4 = new MyValue4(val3, val3);
            t.field2 = val4;
            if (t.testGet2().val1 != val3) throw new RuntimeException("FAIL3");
            if (t.testGet2().val2 != val3) throw new RuntimeException("FAIL3");

            t.testSet2(null);
            if (t.testGet2() != null) throw new RuntimeException("FAIL3");

            t.testSet2(val4);
            if (t.testGet2().val1 != val3) throw new RuntimeException("FAIL3");
            if (t.testGet2().val2 != val3) throw new RuntimeException("FAIL3");

            t.testDeopt2((byte)i, null, null, false);

            t.field3 = null;
            if (t.testGet3() != null) throw new RuntimeException("FAIL1");

            boolean useNull_1 = (i % 4) == 0;
            boolean useNull_2 = (i % 4) == 1;
            boolean useNull_3 = (i % 4) == 2;
            MyValue5_3 val5_3 = useNull_3 ? null : new MyValue5_3((byte)i);
            MyValue5_2 val5_2 = useNull_2 ? null : new MyValue5_2((byte)i, val5_3);
            MyValue5_1 val5_1 = useNull_1 ? null : new MyValue5_1((byte)i, val5_2);
            MyValue5 val5 = new MyValue5((byte)i, val5_1);
            t.field3 = val5;
            if (t.testGet3().x != val5.x) throw new RuntimeException("FAIL3");
            if (useNull_1) {
                if (t.testGet3().val != null) throw new RuntimeException("FAIL3");
            } else {
                if (t.testGet3().val.x != val5_1.x) throw new RuntimeException("FAIL3");
                if (useNull_2) {
                    if (t.testGet3().val.val != null) throw new RuntimeException("FAIL3");
                } else {
                    if (t.testGet3().val.val.x != val5_2.x) throw new RuntimeException("FAIL3");
                    if (useNull_3) {
                        if (t.testGet3().val.val.val != null) throw new RuntimeException("FAIL3");
                    } else {
                        if (t.testGet3().val.val.val.x != val5_3.x) throw new RuntimeException("FAIL3");
                    }
                }
            }

            t.testSet3(null);
            if (t.field3 != null) throw new RuntimeException("FAIL3");

            t.testSet3(val5);
            if (t.testGet3().x != val5.x) throw new RuntimeException("FAIL3");
            if (useNull_1) {
                if (t.testGet3().val != null) throw new RuntimeException("FAIL3");
            } else {
                if (t.testGet3().val.x != val5_1.x) throw new RuntimeException("FAIL3");
                if (useNull_2) {
                    if (t.testGet3().val.val != null) throw new RuntimeException("FAIL3");
                } else {
                    if (t.testGet3().val.val.x != val5_2.x) throw new RuntimeException("FAIL3");
                    if (useNull_3) {
                        if (t.testGet3().val.val.val != null) throw new RuntimeException("FAIL3");
                    } else {
                        if (t.testGet3().val.val.val.x != val5_3.x) throw new RuntimeException("FAIL3");
                    }
                }
            }
            t.testDeopt3((byte)i, null, null, null, null, false);

            t.field4 = null;
            if (t.testGet4() != null) throw new RuntimeException("FAIL1");

            MyValueEmpty empty = useNull ? null : new MyValueEmpty();
            MyValue6 val6 = new MyValue6(empty);
            t.field4 = val6;
            if (t.testGet4().val != empty) throw new RuntimeException("FAIL3");

            t.testSet4(null);
            if (t.testGet4() != null) throw new RuntimeException("FAIL3");

            t.testSet4(val6);
            if (t.testGet4().val != empty) throw new RuntimeException("FAIL3");

            t.testDeopt4(null, null, null, false);


            t.field5 = null;
            if (t.testGet5() != null) throw new RuntimeException("FAIL1");

            empty = ((i % 3) == 0) ? null : new MyValueEmpty();
            val6 = ((i % 3) == 1) ? null : new MyValue6(empty);
            MyValue7 val7 = new MyValue7(val6);
            t.field5 = val7;
            if (t.testGet5().val != val6) throw new RuntimeException("FAIL3");

            t.testSet5(null);
            if (t.testGet5() != null) throw new RuntimeException("FAIL3");

            t.testSet5(val7);
            if (t.testGet5().val != val6) throw new RuntimeException("FAIL3");

            t.testDeopt5(null, null, null, null, false);
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
