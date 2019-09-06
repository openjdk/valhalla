/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.valuetypes;

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test the basic value type implementation in C2
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile TestBasicFunctionality.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestBasicFunctionality
 */
public class TestBasicFunctionality extends ValueTypeTest {
    // Extra VM parameters for some test scenarios. See ValueTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 2: return new String[] {"-DVerifyIR=false"};
        case 3: return new String[] {"-XX:ValueArrayElemMaxFlatSize=0"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestBasicFunctionality test = new TestBasicFunctionality();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class, MyValue3Inline.class);
    }

    // Helper methods

    protected long hash() {
        return hash(rI, rL);
    }

    protected long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    // Receive value type through call to interpreter
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test1() {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        long result = test1();
        Asserts.assertEQ(result, hash());
    }

    // Receive value type from interpreter via parameter
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test2(MyValue1 v) {
        return v.hash();
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        long result = test2(v);
        Asserts.assertEQ(result, hash());
    }

    // Return incoming value type without accessing fields
    @Test(valid = ValueTypePassFieldsAsArgsOn, match = {ALLOC, STORE}, matchCount = {1, 14}, failOn = LOAD + TRAP)
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = ALLOC + LOAD + STORE + TRAP)
    public MyValue1 test3(MyValue1 v) {
        return v;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1 v2 = test3(v1);
        Asserts.assertEQ(v1.x, v2.x);
        Asserts.assertEQ(v1.y, v2.y);
    }

    // Create a value type in compiled code and only use fields.
    // Allocation should go away because value type does not escape.
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public long test4() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        long result = test4();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to
    // an inlined compiled method via a call.
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public long test5() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return test5Inline(v);
    }

    @ForceInline
    public long test5Inline(MyValue1 v) {
        return v.hash();
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        long result = test5();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to
    // the interpreter via a call.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = LOAD + TRAP + ALLOC)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public long test6() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        // Pass to interpreter
        return v.hashInterpreted();
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        long result = test6();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to
    // the interpreter by returning.
    @Test(match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public MyValue1 test7(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y);
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        MyValue1 v = test7(rI, rL);
        Asserts.assertEQ(v.hash(), hash());
    }

    // Merge value types created from two branches
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test8(boolean b) {
        MyValue1 v;
        if (b) {
            v = MyValue1.createWithFieldsInline(rI, rL);
        } else {
            v = MyValue1.createWithFieldsDontInline(rI + 1, rL + 1);
        }
        return v.hash();
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        Asserts.assertEQ(test8(true), hash());
        Asserts.assertEQ(test8(false), hash(rI + 1, rL + 1));
    }

    // Merge value types created from two branches
    @Test(valid = ValueTypePassFieldsAsArgsOn, match = {LOAD}, matchCount = {12}, failOn = TRAP + ALLOC + STORE)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC, STORE}, matchCount = {1, 12}, failOn = LOAD + TRAP)
    public MyValue1 test9(boolean b, int localrI, long localrL) {
        MyValue1 v;
        if (b) {
            // Value type is not allocated
            // Do not use rI/rL directly here as null values may cause
            // some redundant null initializations to be optimized out
            // and matching to fail.
            v = MyValue1.createWithFieldsInline(localrI, localrL);
        } else {
            // Value type is allocated by the callee
            v = MyValue1.createWithFieldsDontInline(rI + 1, rL + 1);
        }
        // Need to allocate value type if 'b' is true
        long sum = v.hashInterpreted();
        if (b) {
            v = MyValue1.createWithFieldsDontInline(rI, sum);
        } else {
            v = MyValue1.createWithFieldsDontInline(rI, sum + 1);
        }
        // Don't need to allocate value type because both branches allocate
        return v;
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        MyValue1 v = test9(true, rI, rL);
        Asserts.assertEQ(v.x, rI);
        Asserts.assertEQ(v.y, hash());
        v = test9(false, rI, rL);
        Asserts.assertEQ(v.x, rI);
        Asserts.assertEQ(v.y, hash(rI + 1, rL + 1) + 1);
    }

    // Merge value types created in a loop (not inlined)
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test10(int x, long y) {
        MyValue1 v = MyValue1.createWithFieldsDontInline(x, y);
        for (int i = 0; i < 10; ++i) {
            v = MyValue1.createWithFieldsDontInline(v.x + 1, v.y + 1);
        }
        return v.hash();
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        long result = test10(rI, rL);
        Asserts.assertEQ(result, hash(rI + 10, rL + 10));
    }

    // Merge value types created in a loop (inlined)
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public long test11(int x, long y) {
        MyValue1 v = MyValue1.createWithFieldsInline(x, y);
        for (int i = 0; i < 10; ++i) {
            v = MyValue1.createWithFieldsInline(v.x + 1, v.y + 1);
        }
        return v.hash();
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        long result = test11(rI, rL);
        Asserts.assertEQ(result, hash(rI + 10, rL + 10));
    }

    // Test loop with uncommon trap referencing a value type
    @Test(match = {SCOBJ}, matchCount = {-1 /* at least 1 */}, failOn = LOAD)
    public long test12(boolean b) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1[] va = new MyValue1[Math.abs(rI) % 10];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        long result = rL;
        for (int i = 0; i < 1000; ++i) {
            if (b) {
                result += v.x;
            } else {
                // Uncommon trap referencing v. We delegate allocation to the
                // interpreter by adding a SafePointScalarObjectNode.
                result = v.hashInterpreted();
                for (int j = 0; j < va.length; ++j) {
                    result += va[j].hash();
                }
            }
        }
        return result;
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        long result = test12(warmup);
        Asserts.assertEQ(result, warmup ? rL + (1000 * rI) : ((Math.abs(rI) % 10) + 1) * hash());
    }

    // Test loop with uncommon trap referencing a value type
    @Test
    public long test13(boolean b) {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1[] va = new MyValue1[Math.abs(rI) % 10];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }
        long result = rL;
        for (int i = 0; i < 1000; ++i) {
            if (b) {
                result += v.x;
            } else {
                // Uncommon trap referencing v. Should not allocate
                // but just pass the existing oop to the uncommon trap.
                result = v.hashInterpreted();
                for (int j = 0; j < va.length; ++j) {
                    result += va[j].hashInterpreted();
                }
            }
        }
        return result;
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        long result = test13(warmup);
        Asserts.assertEQ(result, warmup ? rL + (1000 * rI) : ((Math.abs(rI) % 10) + 1) * hash());
    }

    // Create a value type in a non-inlined method and then call a
    // non-inlined method on that value type.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = (ALLOC + STORE + TRAP), match = {LOAD}, matchCount = {12})
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = (ALLOC + LOAD + STORE + TRAP))
    public long test14() {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        return v.hashInterpreted();
    }

    @DontCompile
    public void test14_verifier(boolean b) {
        long result = test14();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in an inlined method and then call a
    // non-inlined method on that value type.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = (LOAD + TRAP + ALLOC))
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = (LOAD + TRAP), match = {ALLOC}, matchCount = {1})
    public long test15() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return v.hashInterpreted();
    }

    @DontCompile
    public void test15_verifier(boolean b) {
        long result = test15();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in a non-inlined method and then call an
    // inlined method on that value type.
    @Test(failOn = (ALLOC + STORE + TRAP))
    public long test16() {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test16_verifier(boolean b) {
        long result = test16();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in an inlined method and then call an
    // inlined method on that value type.
    @Test(failOn = (ALLOC + LOAD + STORE + TRAP))
    public long test17() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test17_verifier(boolean b) {
        long result = test17();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to the
    // interpreter via a call. The value is live at the first call so
    // debug info should include a reference to all its fields.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public long test18() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        v.hashInterpreted();
        return v.hashInterpreted();
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        long result = test18();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to the
    // interpreter via a call. The value type is passed twice but
    // should only be allocated once.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public long test19() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return sumValue(v, v);
    }

    @DontCompile
    public long sumValue(MyValue1 v, MyValue1 dummy) {
        return v.hash();
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        long result = test19();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type (array) in compiled code and pass it to the
    // interpreter via a call. The value type is live at the uncommon
    // trap: verify that deoptimization causes the value type to be
    // correctly allocated.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = LOAD + ALLOC + STORE)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {1}, failOn = LOAD)
    public long test20(boolean deopt) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue2[] va = new MyValue2[3];
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test20"));
        }
        return v.hashInterpreted() + va[0].hashInterpreted() +
                va[1].hashInterpreted() + va[2].hashInterpreted();
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
        MyValue2[] va = new MyValue2[42];
        long result = test20(!warmup);
        Asserts.assertEQ(result, hash() + va[0].hash() + va[1].hash() + va[2].hash());
    }

    // Value type fields in regular object
    MyValue1 val1;
    MyValue2 val2;
    final MyValue1 val3 = MyValue1.createWithFieldsInline(rI, rL);
    static MyValue1 val4;
    static final MyValue1 val5 = MyValue1.createWithFieldsInline(rI, rL);

    // Test value type fields in objects
    @Test(match = {ALLOC}, matchCount = {1}, failOn = (TRAP))
    public long test21(int x, long y) {
        // Compute hash of value type fields
        long result = val1.hash() + val2.hash() + val3.hash() + val4.hash() + val5.hash();
        // Update fields
        val1 = MyValue1.createWithFieldsInline(x, y);
        val2 = MyValue2.createWithFieldsInline(x, true);
        val4 = MyValue1.createWithFieldsInline(x, y);
        return result;
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        // Check if hash computed by test18 is correct
        val1 = MyValue1.createWithFieldsInline(rI, rL);
        val2 = val1.v2;
        // val3 is initialized in the constructor
        val4 = val1;
        // val5 is initialized in the static initializer
        long hash = val1.hash() + val2.hash() + val3.hash() + val4.hash() + val5.hash();
        long result = test21(rI + 1, rL + 1);
        Asserts.assertEQ(result, hash);
        // Check if value type fields were updated
        Asserts.assertEQ(val1.hash(), hash(rI + 1, rL + 1));
        Asserts.assertEQ(val2.hash(), MyValue2.createWithFieldsInline(rI + 1, true).hash());
        Asserts.assertEQ(val4.hash(), hash(rI + 1, rL + 1));
    }

    // Test folding of constant value type fields
    @Test(failOn = ALLOC + LOAD + STORE + LOOP + TRAP)
    public long test22() {
        // This should be constant folded
        return val5.hash() + val5.v3.hash();
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        long result = test22();
        Asserts.assertEQ(result, val5.hash() + val5.v3.hash());
    }

    // Test defaultvalue
    @Test(failOn = ALLOC + LOAD + STORE + LOOP + TRAP)
    public long test23() {
        MyValue2 v = MyValue2.createDefaultInline();
        return v.hash();
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        long result = test23();
        Asserts.assertEQ(result, MyValue2.createDefaultInline().hash());
    }

    // Test defaultvalue
    @Test(failOn = ALLOC + STORE + LOOP + TRAP)
    public long test24() {
        MyValue1 v1 = MyValue1.createDefaultInline();
        MyValue1 v2 = MyValue1.createDefaultDontInline();
        return v1.hashPrimitive() + v2.hashPrimitive();
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
        long result = test24();
        Asserts.assertEQ(result, 2 * MyValue1.createDefaultInline().hashPrimitive());
    }

    // Test withfield
    @Test(failOn = ALLOC + LOAD + STORE + LOOP + TRAP)
    public long test25() {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        return v.hash();
    }

    @DontCompile
    public void test25_verifier(boolean warmup) {
        long result = test25();
        Asserts.assertEQ(result, MyValue2.createWithFieldsInline(rI, true).hash());
    }

    // Test withfield
    @Test(failOn = ALLOC + STORE + LOOP + TRAP)
    public long test26() {
        MyValue1 v1 = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 v2 = MyValue1.createWithFieldsDontInline(rI, rL);
        return v1.hash() + v2.hash();
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        long result = test26();
        Asserts.assertEQ(result, 2 * hash());
    }

    class TestClass27 {
        public MyValue1 v;
    }

    // Test allocation elimination of unused object with initialized value type field
    @Test(failOn = ALLOC + LOAD + STORE + LOOP)
    public void test27(boolean deopt) {
        TestClass27 unused = new TestClass27();
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        unused.v = v;
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test27"));
        }
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        test27(!warmup);
    }

    static MyValue3 staticVal3;
    static MyValue3 staticVal3_copy;

    // Check elimination of redundant value type allocations
    @Test(match = {ALLOC}, matchCount = {1})
    public MyValue3 test28(MyValue3[] va) {
        // Create value type and force allocation
        MyValue3 vt = MyValue3.create();
        va[0] = vt;
        staticVal3 = vt;
        vt.verify(staticVal3);

        // Value type is now allocated, make a copy and force allocation.
        // Because copy is equal to vt, C2 should remove this redundant allocation.
        MyValue3 copy = MyValue3.setC(vt, vt.c);
        va[0] = copy;
        staticVal3_copy = copy;
        copy.verify(staticVal3_copy);
        return copy;
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test28(va);
        staticVal3.verify(vt);
        staticVal3.verify(va[0]);
        staticVal3_copy.verify(vt);
        staticVal3_copy.verify(va[0]);
    }

    // Verify that only dominating allocations are re-used
    @Test()
    public MyValue3 test29(boolean warmup) {
        MyValue3 vt = MyValue3.create();
        if (warmup) {
            staticVal3 = vt; // Force allocation
        }
        // Force allocation to verify that above
        // non-dominating allocation is not re-used
        MyValue3 copy = MyValue3.setC(vt, vt.c);
        staticVal3_copy = copy;
        copy.verify(vt);
        return copy;
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
        MyValue3 vt = test29(warmup);
        if (warmup) {
            staticVal3.verify(vt);
        }
    }

    // Verify that C2 recognizes value type loads and re-uses the oop to avoid allocations
    @Test(failOn = ALLOC + ALLOCA + STORE)
    public MyValue3 test30(MyValue3[] va) {
        // C2 can re-use the oop of staticVal3 because staticVal3 is equal to copy
        MyValue3 copy = MyValue3.copy(staticVal3);
        va[0] = copy;
        staticVal3 = copy;
        copy.verify(staticVal3);
        return copy;
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
        staticVal3 = MyValue3.create();
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test30(va);
        staticVal3.verify(vt);
        staticVal3.verify(va[0]);
    }

    // Verify that C2 recognizes value type loads and re-uses the oop to avoid allocations
    @Test(valid = ValueTypeReturnedAsFieldsOn)
    @Test(valid = ValueTypeReturnedAsFieldsOff, failOn = ALLOC + ALLOCA + STORE)
    public MyValue3 test31(MyValue3[] va) {
        // C2 can re-use the oop returned by createDontInline()
        // because the corresponding value type is equal to 'copy'.
        MyValue3 copy = MyValue3.copy(MyValue3.createDontInline());
        va[0] = copy;
        staticVal3 = copy;
        copy.verify(staticVal3);
        return copy;
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test31(va);
        staticVal3.verify(vt);
        staticVal3.verify(va[0]);
    }

    // Verify that C2 recognizes value type loads and re-uses the oop to avoid allocations
    @Test(valid = ValueTypePassFieldsAsArgsOn)
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = ALLOC + ALLOCA + STORE)
    public MyValue3 test32(MyValue3 vt, MyValue3[] va) {
        // C2 can re-use the oop of vt because vt is equal to 'copy'.
        MyValue3 copy = MyValue3.copy(vt);
        va[0] = copy;
        staticVal3 = copy;
        copy.verify(staticVal3);
        return copy;
    }

    @DontCompile
    public void test32_verifier(boolean warmup) {
        MyValue3 vt = MyValue3.create();
        MyValue3[] va = new MyValue3[1];
        MyValue3 result = test32(vt, va);
        staticVal3.verify(vt);
        va[0].verify(vt);
        result.verify(vt);
    }

    // Test correct identification of value type copies
    @Test()
    public MyValue3 test33(MyValue3[] va) {
        MyValue3 vt = MyValue3.copy(staticVal3);
        vt = MyValue3.setI(vt, vt.c);
        // vt is not equal to staticVal3, so C2 should not re-use the oop
        va[0] = vt;
        staticVal3 = vt;
        vt.verify(staticVal3);
        return vt;
    }

    @DontCompile
    public void test33_verifier(boolean warmup) {
        staticVal3 = MyValue3.create();
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test33(va);
        Asserts.assertEQ(staticVal3.i, (int)staticVal3.c);
        Asserts.assertEQ(va[0].i, (int)staticVal3.c);
        Asserts.assertEQ(vt.i, (int)staticVal3.c);
    }

    // Verify that the default value type is never allocated.
    // C2 code should load and use the default oop from the java mirror.
    @Test(failOn = ALLOC + ALLOCA + LOAD + STORE + LOOP + TRAP)
    public MyValue3 test34(MyValue3[] va) {
        // Explicitly create default value
        MyValue3 vt = MyValue3.createDefault();
        va[0] = vt;
        staticVal3 = vt;
        vt.verify(vt);

        // Load default value from uninitialized value array
        MyValue3[] dva = new MyValue3[1];
        staticVal3_copy = dva[0];
        va[1] = dva[0];
        dva[0].verify(dva[0]);
        return vt;
    }

    @DontCompile
    public void test34_verifier(boolean warmup) {
        MyValue3 vt = MyValue3.createDefault();
        MyValue3[] va = new MyValue3[2];
        va[0] = MyValue3.create();
        va[1] = MyValue3.create();
        MyValue3 res = test34(va);
        res.verify(vt);
        staticVal3.verify(vt);
        staticVal3_copy.verify(vt);
        va[0].verify(vt);
        va[1].verify(vt);
    }

    // Same as above but manually initialize value type fields to default.
    @Test(failOn = ALLOC + ALLOCA + LOAD + STORE + LOOP + TRAP)
    public MyValue3 test35(MyValue3 vt, MyValue3[] va) {
        vt = MyValue3.setC(vt, (char)0);
        vt = MyValue3.setBB(vt, (byte)0);
        vt = MyValue3.setS(vt, (short)0);
        vt = MyValue3.setI(vt, 0);
        vt = MyValue3.setL(vt, 0);
        vt = MyValue3.setO(vt, null);
        vt = MyValue3.setF1(vt, 0);
        vt = MyValue3.setF2(vt, 0);
        vt = MyValue3.setF3(vt, 0);
        vt = MyValue3.setF4(vt, 0);
        vt = MyValue3.setF5(vt, 0);
        vt = MyValue3.setF6(vt, 0);
        vt = MyValue3.setV1(vt, MyValue3Inline.createDefault());
        va[0] = vt;
        staticVal3 = vt;
        vt.verify(vt);
        return vt;
    }

    @DontCompile
    public void test35_verifier(boolean warmup) {
        MyValue3 vt = MyValue3.createDefault();
        MyValue3[] va = new MyValue3[1];
        va[0] = MyValue3.create();
        MyValue3 res = test35(va[0], va);
        res.verify(vt);
        staticVal3.verify(vt);
        va[0].verify(vt);
    }

    // Merge value types created from two branches

    private Object test36_helper(Object v) {
        return v;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test36(boolean b) {
        Object o;
        if (b) {
            o = test36_helper(MyValue1.createWithFieldsInline(rI, rL));
        } else {
            o = test36_helper(MyValue1.createWithFieldsDontInline(rI + 1, rL + 1));
        }
        MyValue1 v = (MyValue1)o;
        return v.hash();
    }

    @DontCompile
    public void test36_verifier(boolean warmup) {
        Asserts.assertEQ(test36(true), hash());
        Asserts.assertEQ(test36(false), hash(rI + 1, rL + 1));
    }

    // Test correct loading of flattened fields
    inline class Test37Value2 {
        final int x = 0;
        final int y = 0;
    }

    inline class Test37Value1 {
        final double d = 0;
        final float f = 0;
        final Test37Value2 v = new Test37Value2();
    }

    @Test
    public Test37Value1 test37(Test37Value1 vt) {
        return vt;
    }

    @DontCompile
    public void test37_verifier(boolean warmup) {
        Test37Value1 vt = new Test37Value1();
        Asserts.assertEQ(test37(vt), vt);
    }

    // Test elimination of inline type allocations without a unique CheckCastPP
    inline class Test38Value {
        public int i;
        public Test38Value(int i) { this.i = i; }
    }

    static Test38Value test38Field;

    @Test
    public void test38() {
        for (int i = 3; i < 100; ++i) {
            int j = 1;
            while (++j < 11) {
                try {
                    test38Field = new Test38Value(i);
                } catch (ArithmeticException ae) { }
            }
        }
    }

    @DontCompile
    public void test38_verifier(boolean warmup) {
        test38Field = Test38Value.default;
        test38();
        Asserts.assertEQ(test38Field, new Test38Value(99));
    }
}
