/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;
import java.lang.reflect.Method;

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test method handle support for value types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile TestMethodHandles.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestMethodHandles
 */
public class TestMethodHandles extends ValueTypeTest {
    // Extra VM parameters for some test scenarios. See ValueTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        // Prevent inlining through MethodHandle linkTo adapters to stress the calling convention
        case 2: return new String[] {"-DVerifyIR=false", "-XX:CompileCommand=dontinline,java.lang.invoke.DirectMethodHandle::internalMemberName"};
        case 4: return new String[] {"-XX:CompileCommand=dontinline,java.lang.invoke.DirectMethodHandle::internalMemberName"};
        }
        return null;
    }

    static {
        try {
            Class<?> clazz = TestMethodHandles.class;
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType mt = MethodType.methodType(MyValue3.class);
            test1_mh = lookup.findVirtual(clazz, "test1_target", mt);
            test2_mh = lookup.findVirtual(clazz, "test2_target", mt);
            test3_mh = lookup.findVirtual(clazz, "test3_target", mt);

            MethodType test4_mt1 = MethodType.methodType(int.class, MyValue1.class);
            MethodType test4_mt2 = MethodType.methodType(MyValue1.class);
            MethodHandle test4_mh1 = lookup.findStatic(clazz, "test4_helper1", test4_mt1);
            MethodHandle test4_mh2 = lookup.findStatic(clazz, "test4_helper2", test4_mt2);
            test4_mh = MethodHandles.filterReturnValue(test4_mh2, test4_mh1);

            MethodType test5_mt = MethodType.methodType(int.class, MyValue1.class);
            test5_mh = lookup.findVirtual(clazz, "test5_target", test5_mt);

            MethodType test6_mt = MethodType.methodType(MyValue3.class);
            MethodHandle test6_mh1 = lookup.findVirtual(clazz, "test6_target1", test6_mt);
            MethodHandle test6_mh2 = lookup.findVirtual(clazz, "test6_target2", test6_mt);
            MethodType boolean_mt = MethodType.methodType(boolean.class);
            MethodHandle test6_mh_test = lookup.findVirtual(clazz, "test6_test", boolean_mt);
            test6_mh = MethodHandles.guardWithTest(test6_mh_test, test6_mh1, test6_mh2);

            MethodType myvalue2_mt = MethodType.methodType(MyValue2.class);
            test7_mh1 = lookup.findStatic(clazz, "test7_target1", myvalue2_mt);
            MethodHandle test7_mh2 = lookup.findStatic(clazz, "test7_target2", myvalue2_mt);
            MethodHandle test7_mh_test = lookup.findStatic(clazz, "test7_test", boolean_mt);
            test7_mh = MethodHandles.guardWithTest(test7_mh_test,
                                                    MethodHandles.invoker(myvalue2_mt),
                                                    MethodHandles.dropArguments(test7_mh2, 0, MethodHandle.class));

            MethodHandle test8_mh1 = lookup.findStatic(clazz, "test8_target1", myvalue2_mt);
            test8_mh2 = lookup.findStatic(clazz, "test8_target2", myvalue2_mt);
            MethodHandle test8_mh_test = lookup.findStatic(clazz, "test8_test", boolean_mt);
            test8_mh = MethodHandles.guardWithTest(test8_mh_test,
                                                    MethodHandles.dropArguments(test8_mh1, 0, MethodHandle.class),
                                                    MethodHandles.invoker(myvalue2_mt));

            MethodType test9_mt = MethodType.methodType(MyValue3.class);
            MethodHandle test9_mh1 = lookup.findVirtual(clazz, "test9_target1", test9_mt);
            MethodHandle test9_mh2 = lookup.findVirtual(clazz, "test9_target2", test9_mt);
            MethodHandle test9_mh3 = lookup.findVirtual(clazz, "test9_target3", test9_mt);
            MethodType test9_mt2 = MethodType.methodType(boolean.class);
            MethodHandle test9_mh_test1 = lookup.findVirtual(clazz, "test9_test1", test9_mt2);
            MethodHandle test9_mh_test2 = lookup.findVirtual(clazz, "test9_test2", test9_mt2);
            test9_mh = MethodHandles.guardWithTest(test9_mh_test1,
                                                    test9_mh1,
                                                    MethodHandles.guardWithTest(test9_mh_test2, test9_mh2, test9_mh3));

            MethodType test10_mt = MethodType.methodType(MyValue2.class);
            MethodHandle test10_mh1 = lookup.findStatic(clazz, "test10_target1", test10_mt);
            test10_mh2 = lookup.findStatic(clazz, "test10_target2", test10_mt);
            test10_mh3 = lookup.findStatic(clazz, "test10_target3", test10_mt);
            MethodType test10_mt2 = MethodType.methodType(boolean.class);
            MethodType test10_mt3 = MethodType.methodType(MyValue2.class);
            MethodHandle test10_mh_test1 = lookup.findStatic(clazz, "test10_test1", test10_mt2);
            MethodHandle test10_mh_test2 = lookup.findStatic(clazz, "test10_test2", test10_mt2);
            test10_mh = MethodHandles.guardWithTest(test10_mh_test1,
                                                    MethodHandles.dropArguments(test10_mh1, 0, MethodHandle.class, MethodHandle.class),
                                                    MethodHandles.guardWithTest(test10_mh_test2,
                                                                                MethodHandles.dropArguments(MethodHandles.invoker(test10_mt3), 1, MethodHandle.class),
                                                                                MethodHandles.dropArguments(MethodHandles.invoker(test10_mt3), 0, MethodHandle.class))
                                                    );

            MethodHandle test11_mh1 = lookup.findStatic(clazz, "test11_target1", myvalue2_mt);
            test11_mh2 = lookup.findStatic(clazz, "test11_target2", myvalue2_mt);
            MethodHandle test11_mh_test = lookup.findStatic(clazz, "test11_test", boolean_mt);
            test11_mh = MethodHandles.guardWithTest(test11_mh_test,
                                                    MethodHandles.dropArguments(test11_mh1, 0, MethodHandle.class),
                                                    MethodHandles.invoker(myvalue2_mt));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Method handle lookup failed");
        }
    }

    public static void main(String[] args) throws Throwable {
        TestMethodHandles test = new TestMethodHandles();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class, MyValue3Inline.class);
    }

    // Everything inlined
    final MyValue3 test1_vt = MyValue3.create();

    @ForceInline
    MyValue3 test1_target() {
        return test1_vt;
    }

    static final MethodHandle test1_mh;

    @Test(valid = InlineTypeReturnedAsFieldsOn, failOn = ALLOC + STORE + CALL)
    @Test(valid = InlineTypeReturnedAsFieldsOff, match = { ALLOC, STORE }, matchCount = { 1, 14 })
    public MyValue3 test1() throws Throwable {
        return (MyValue3)test1_mh.invokeExact(this);
    }

    @DontCompile
    public void test1_verifier(boolean warmup) throws Throwable {
        MyValue3 vt = test1();
        test1_vt.verify(vt);
    }

    // Leaf method not inlined but returned type is known
    final MyValue3 test2_vt = MyValue3.create();
    @DontInline
    MyValue3 test2_target() {
        return test2_vt;
    }

    static final MethodHandle test2_mh;

    @Test
    public MyValue3 test2() throws Throwable {
        return (MyValue3)test2_mh.invokeExact(this);
    }

    @DontCompile
    public void test2_verifier(boolean warmup) throws Throwable {
        Method helper_m = getClass().getDeclaredMethod("test2_target");
        if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
            enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test2_target not compiled");
        }
        MyValue3 vt = test2();
        test2_vt.verify(vt);
    }

    // Leaf method not inlined and returned type not known
    final MyValue3 test3_vt = MyValue3.create();
    @DontInline
    MyValue3 test3_target() {
        return test3_vt;
    }

    static final MethodHandle test3_mh;

    @Test
    public MyValue3 test3() throws Throwable {
        return (MyValue3)test3_mh.invokeExact(this);
    }

    @DontCompile
    public void test3_verifier(boolean warmup) throws Throwable {
        // hack so C2 doesn't know the target of the invoke call
        Class c = Class.forName("java.lang.invoke.DirectMethodHandle");
        Method m = c.getDeclaredMethod("internalMemberName", Object.class);
        WHITE_BOX.testSetDontInlineMethod(m, warmup);
        MyValue3 vt = test3();
        test3_vt.verify(vt);
    }

    // When test75_helper1 is inlined in test75, the method handle
    // linker that called it is passed a pointer to a copy of vt
    // stored in memory. The method handle linker needs to load the
    // fields from memory before it inlines test75_helper1.
    static public int test4_helper1(MyValue1 vt) {
        return vt.x;
    }

    static MyValue1 test4_vt = MyValue1.createWithFieldsInline(rI, rL);
    static public MyValue1 test4_helper2() {
        return test4_vt;
    }

    static final MethodHandle test4_mh;

    @Test
    public int test4() throws Throwable {
        return (int)test4_mh.invokeExact();
    }

    @DontCompile
    public void test4_verifier(boolean warmup) throws Throwable {
        int i = test4();
        Asserts.assertEQ(i, test4_vt.x);
    }

    // Test method handle call with value type argument
    public int test5_target(MyValue1 vt) {
        return vt.x;
    }

    static final MethodHandle test5_mh;
    MyValue1 test5_vt = MyValue1.createWithFieldsInline(rI, rL);

    @Test
    public int test5() throws Throwable {
        return (int)test5_mh.invokeExact(this, test5_vt);
    }

    @DontCompile
    public void test5_verifier(boolean warmup) throws Throwable {
        int i = test5();
        Asserts.assertEQ(i, test5_vt.x);
    }

    // Return of target1 and target2 merged in a Lambda Form as an
    // Object. Shouldn't cause any allocation
    final MyValue3 test6_vt1 = MyValue3.create();
    @ForceInline
    MyValue3 test6_target1() {
        return test6_vt1;
    }

    final MyValue3 test6_vt2 = MyValue3.create();
    @ForceInline
    MyValue3 test6_target2() {
        return test6_vt2;
    }

    boolean test6_bool = true;
    @ForceInline
    boolean test6_test() {
        return test6_bool;
    }

    static final MethodHandle test6_mh;

    @Test(valid = InlineTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = InlineTypeReturnedAsFieldsOff)
    public MyValue3 test6() throws Throwable {
        return (MyValue3)test6_mh.invokeExact(this);
    }

    @DontCompile
    public void test6_verifier(boolean warmup) throws Throwable {
        test6_bool = !test6_bool;
        MyValue3 vt = test6();
        vt.verify(test6_bool ? test6_vt1 : test6_vt2);
    }

    // Similar as above but with the method handle for target1 not
    // constant. Shouldn't cause any allocation.
    @ForceInline
    static MyValue2 test7_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test7_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    static boolean test7_bool = true;
    @ForceInline
    static boolean test7_test() {
        return test7_bool;
    }

    static final MethodHandle test7_mh;
    static MethodHandle test7_mh1;

    @Test
    public long test7() throws Throwable {
        return ((MyValue2)test7_mh.invokeExact(test7_mh1)).hash();
    }

    @DontCompile
    public void test7_verifier(boolean warmup) throws Throwable {
        test7_bool = !test7_bool;
        long hash = test7();
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+(test7_bool ? 0 : 1), test7_bool).hash());
    }

    // Same as above but with the method handle for target2 not
    // constant. Shouldn't cause any allocation.
    @ForceInline
    static MyValue2 test8_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test8_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    static boolean test8_bool = true;
    @ForceInline
    static boolean test8_test() {
        return test8_bool;
    }

    static final MethodHandle test8_mh;
    static MethodHandle test8_mh2;

    @Test
    public long test8() throws Throwable {
        return ((MyValue2)test8_mh.invokeExact(test8_mh2)).hash();
    }

    @DontCompile
    public void test8_verifier(boolean warmup) throws Throwable {
        test8_bool = !test8_bool;
        long hash = test8();
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+(test8_bool ? 0 : 1), test8_bool).hash());
    }

    // Return of target1, target2 and target3 merged in Lambda Forms
    // as an Object. Shouldn't cause any allocation
    final MyValue3 test9_vt1 = MyValue3.create();
    @ForceInline
    MyValue3 test9_target1() {
        return test9_vt1;
    }

    final MyValue3 test9_vt2 = MyValue3.create();
    @ForceInline
    MyValue3 test9_target2() {
        return test9_vt2;
    }

    final MyValue3 test9_vt3 = MyValue3.create();
    @ForceInline
    MyValue3 test9_target3() {
        return test9_vt3;
    }

    boolean test9_bool1 = true;
    @ForceInline
    boolean test9_test1() {
        return test9_bool1;
    }

    boolean test9_bool2 = true;
    @ForceInline
    boolean test9_test2() {
        return test9_bool2;
    }

    static final MethodHandle test9_mh;

    @Test(valid = InlineTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = InlineTypeReturnedAsFieldsOff)
    public MyValue3 test9() throws Throwable {
        return (MyValue3)test9_mh.invokeExact(this);
    }

    static int test9_i = 0;
    @DontCompile
    public void test9_verifier(boolean warmup) throws Throwable {
        test9_i++;
        test9_bool1 = (test9_i % 2) == 0;
        test9_bool2 = (test9_i % 3) == 0;
        MyValue3 vt = test9();
        vt.verify(test9_bool1 ? test9_vt1 : (test9_bool2 ? test9_vt2 : test9_vt3));
    }

    // Same as above but with non constant target2 and target3
    @ForceInline
    static MyValue2 test10_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test10_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    @ForceInline
    static MyValue2 test10_target3() {
        return MyValue2.createWithFieldsInline(rI+2, true);
    }

    static boolean test10_bool1 = true;
    @ForceInline
    static boolean test10_test1() {
        return test10_bool1;
    }

    static boolean test10_bool2 = true;
    @ForceInline
    static boolean test10_test2() {
        return test10_bool2;
    }

    static final MethodHandle test10_mh;
    static MethodHandle test10_mh2;
    static MethodHandle test10_mh3;

    @Test
    public long test10() throws Throwable {
        return ((MyValue2)test10_mh.invokeExact(test10_mh2, test10_mh3)).hash();
    }

    static int test10_i = 0;

    @DontCompile
    public void test10_verifier(boolean warmup) throws Throwable {
        test10_i++;
        test10_bool1 = (test10_i % 2) == 0;
        test10_bool2 = (test10_i % 3) == 0;
        long hash = test10();
        int i = rI+(test10_bool1 ? 0 : (test10_bool2 ? 1 : 2));
        boolean b = test10_bool1 ? true : (test10_bool2 ? false : true);
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(i, b).hash());
    }

    static int test11_i = 0;

    @ForceInline
    static MyValue2 test11_target1() {
        return MyValue2.createWithFieldsInline(rI+test11_i, true);
    }

    @ForceInline
    static MyValue2 test11_target2() {
        return MyValue2.createWithFieldsInline(rI-test11_i, false);
    }

    @ForceInline
    static boolean test11_test() {
        return (test11_i % 100) == 0;
    }

    static final MethodHandle test11_mh;
    static MethodHandle test11_mh2;

    // Check that a buffered value returned by a compiled lambda form
    // is properly handled by the caller.
    @Test
    @Warmup(11000)
    public long test11() throws Throwable {
        return ((MyValue2)test11_mh.invokeExact(test11_mh2)).hash();
    }

    @DontCompile
    public void test11_verifier(boolean warmup) throws Throwable {
        test11_i++;
        long hash = test11();
        boolean b = (test11_i % 100) == 0;
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+test11_i * (b ? 1 : -1), b).hash());
    }
}
