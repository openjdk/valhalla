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

package compiler.valhalla.valuetypes;

import jdk.test.lib.Asserts;

import java.lang.reflect.Method;

/*
 * @test
 * @summary Test value type calling convention optimizations
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDenableValueTypes -XDallowWithFieldOperator -XDallowFlattenabilityModifiers TestCallingConvention.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+EnableValhalla
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestCallingConvention
 */
public class TestCallingConvention extends ValueTypeTest {
    // Extra VM parameters for some test scenarios. See ValueTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 3: return new String[] {"-XX:-ValueArrayFlatten"};
        }
        return null;
    }

   public static void main(String[] args) throws Throwable {
       TestCallingConvention test = new TestCallingConvention();
       test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue4.class);
   }

   // Test interpreter to compiled code with various signatures
   @Test(failOn = ALLOC + STORE + TRAP)
   public long test1(MyValue2 v) {
       return v.hash();
   }

   @DontCompile
   public void test1_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test1(v);
       Asserts.assertEQ(result, v.hashInterpreted());
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test2(int i1, MyValue2 v, int i2) {
       return v.hash() + i1 - i2;
   }

   @DontCompile
   public void test2_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test2(rI, v, 2*rI);
       Asserts.assertEQ(result, v.hashInterpreted() - rI);
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test3(long l1, MyValue2 v, long l2) {
       return v.hash() + l1 - l2;
   }

   @DontCompile
   public void test3_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test3(rL, v, 2*rL);
       Asserts.assertEQ(result, v.hashInterpreted() - rL);
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test4(int i, MyValue2 v, long l) {
       return v.hash() + i + l;
   }

   @DontCompile
   public void test4_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test4(rI, v, rL);
       Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test5(long l, MyValue2 v, int i) {
       return v.hash() + i + l;
   }

   @DontCompile
   public void test5_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test5(rL, v, rI);
       Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test6(long l, MyValue1 v1, int i, MyValue2 v2) {
       return v1.hash() + i + l + v2.hash();
   }

   @DontCompile
   public void test6_verifier(boolean warmup) {
       MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
       MyValue2 v2 = MyValue2.createWithFieldsInline(rI, true);
       long result = test6(rL, v1, rI, v2);
       Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
   }

   // Test compiled code to interpreter with various signatures
   @DontCompile
   public long test7_interp(MyValue2 v) {
       return v.hash();
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test7(MyValue2 v) {
       return test7_interp(v);
   }

   @DontCompile
   public void test7_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test7(v);
       Asserts.assertEQ(result, v.hashInterpreted());
   }

   @DontCompile
   public long test8_interp(int i1, MyValue2 v, int i2) {
       return v.hash() + i1 - i2;
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test8(int i1, MyValue2 v, int i2) {
       return test8_interp(i1, v, i2);
   }

   @DontCompile
   public void test8_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test8(rI, v, 2*rI);
       Asserts.assertEQ(result, v.hashInterpreted() - rI);
   }

   @DontCompile
   public long test9_interp(long l1, MyValue2 v, long l2) {
       return v.hash() + l1 - l2;
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test9(long l1, MyValue2 v, long l2) {
       return test9_interp(l1, v, l2);
   }

   @DontCompile
   public void test9_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test9(rL, v, 2*rL);
       Asserts.assertEQ(result, v.hashInterpreted() - rL);
   }

   @DontCompile
   public long test10_interp(int i, MyValue2 v, long l) {
       return v.hash() + i + l;
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test10(int i, MyValue2 v, long l) {
       return test10_interp(i, v, l);
   }

   @DontCompile
   public void test10_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test10(rI, v, rL);
       Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
   }

   @DontCompile
   public long test11_interp(long l, MyValue2 v, int i) {
       return v.hash() + i + l;
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test11(long l, MyValue2 v, int i) {
       return test11_interp(l, v, i);
   }

   @DontCompile
   public void test11_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       long result = test11(rL, v, rI);
       Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
   }

   @DontCompile
   public long test12_interp(long l, MyValue1 v1, int i, MyValue2 v2) {
       return v1.hash() + i + l + v2.hash();
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test12(long l, MyValue1 v1, int i, MyValue2 v2) {
       return test12_interp(l, v1, i, v2);
   }

   @DontCompile
   public void test12_verifier(boolean warmup) {
       MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
       MyValue2 v2 = MyValue2.createWithFieldsInline(rI, true);
       long result = test12(rL, v1, rI, v2);
       Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
   }

   // Test that debug info at a call is correct
   @DontCompile
   public long test13_interp(MyValue2 v, MyValue1[] va, boolean deopt) {
       if (deopt) {
           // uncommon trap
           WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test13"));
       }
       return v.hash() + va[0].hash() + va[1].hash();
   }

   @Test(failOn = ALLOC + STORE + TRAP)
   public long test13(MyValue2 v, MyValue1[] va, boolean flag, long l) {
       return test13_interp(v, va, flag) + l;
   }

   @DontCompile
   public void test13_verifier(boolean warmup) {
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       MyValue1[] va = new MyValue1[2];
       va[0] = MyValue1.createWithFieldsDontInline(rI, rL);
       va[1] = MyValue1.createWithFieldsDontInline(rI, rL);
       long result = test13(v, va, !warmup, rL);
       Asserts.assertEQ(result, v.hashInterpreted() + va[0].hash() + va[1].hash() + rL);
   }

   // Test deoptimization at call return with return value in registers
   @DontCompile
   public MyValue2 test14_interp(boolean deopt) {
       if (deopt) {
           // uncommon trap
           WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test14"));
       }
       return MyValue2.createWithFieldsInline(rI, true);
   }

   @Test()
   public MyValue2 test14(boolean flag) {
       return test14_interp(flag);
   }

   @DontCompile
   public void test14_verifier(boolean warmup) {
       MyValue2 result = test14(!warmup);
       MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
       Asserts.assertEQ(result.hash(), v.hash());
   }

   // Return value types in registers from interpreter -> compiled
   final MyValue3 test15_vt = MyValue3.create();
   @DontCompile
   public MyValue3 test15_interp() {
       return test15_vt;
   }

   MyValue3 test15_vt2;
   @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
   @Test(valid = ValueTypeReturnedAsFieldsOff)
   public void test15() {
       test15_vt2 = test15_interp();
   }

   @DontCompile
   public void test15_verifier(boolean warmup) {
       test15();
       test15_vt.verify(test15_vt2);
   }

   // Return value types in registers from compiled -> interpreter
   final MyValue3 test16_vt = MyValue3.create();
   @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + STORE + TRAP)
   @Test(valid = ValueTypeReturnedAsFieldsOff)
   public MyValue3 test16() {
       return test16_vt;
   }

   @DontCompile
   public void test16_verifier(boolean warmup) {
       MyValue3 vt = test16();
       test16_vt.verify(vt);
   }

   // Return value types in registers from compiled -> compiled
   final MyValue3 test17_vt = MyValue3.create();
   @DontInline
   public MyValue3 test17_comp() {
       return test17_vt;
   }

   MyValue3 test17_vt2;
   @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
   @Test(valid = ValueTypeReturnedAsFieldsOff)
   public void test17() {
       test17_vt2 = test17_comp();
   }

   @DontCompile
   public void test17_verifier(boolean warmup) throws Exception {
       Method helper_m = getClass().getDeclaredMethod("test17_comp");
       if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
           WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
           Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test17_comp not compiled");
       }
       test17();
       test17_vt.verify(test17_vt2);
   }

   // Same tests as above but with a value type that cannot be returned in registers

   // Return value types in registers from interpreter -> compiled
   final MyValue4 test18_vt = MyValue4.create();
   @DontCompile
   public MyValue4 test18_interp() {
       return test18_vt;
   }

   MyValue4 test18_vt2;
   @Test
   public void test18() {
       test18_vt2 = test18_interp();
   }

   @DontCompile
   public void test18_verifier(boolean warmup) {
       test18();
       test18_vt.verify(test18_vt2);
   }

   // Return value types in registers from compiled -> interpreter
   final MyValue4 test19_vt = MyValue4.create();
   @Test
   public MyValue4 test19() {
       return test19_vt;
   }

   @DontCompile
   public void test19_verifier(boolean warmup) {
       MyValue4 vt = test19();
       test19_vt.verify(vt);
   }

   // Return value types in registers from compiled -> compiled
   final MyValue4 test20_vt = MyValue4.create();
   @DontInline
   public MyValue4 test20_comp() {
       return test20_vt;
   }

   MyValue4 test20_vt2;
   @Test
   public void test20() {
       test20_vt2 = test20_comp();
   }

   @DontCompile
   public void test20_verifier(boolean warmup) throws Exception {
       Method helper_m = getClass().getDeclaredMethod("test20_comp");
       if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
           WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
           Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test20_comp not compiled");
       }
       test20();
       test20_vt.verify(test20_vt2);
   }

   // Test no result from inlined method for incremental inlining
   final MyValue3 test21_vt = MyValue3.create();
   public MyValue3 test21_inlined() {
       throw new RuntimeException();
   }

   @Test
   public MyValue3 test21() {
       try {
           return test21_inlined();
       } catch (RuntimeException ex) {
           return test21_vt;
       }
   }

   @DontCompile
   public void test21_verifier(boolean warmup) {
       MyValue3 vt = test21();
       test21_vt.verify(vt);
   }
}
