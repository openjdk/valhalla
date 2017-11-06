/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.experimental.value.MethodHandleBuilder;
import jdk.incubator.mvt.ValueType;
import jdk.test.lib.Asserts;

import java.lang.invoke.*;
import java.lang.reflect.Method;

/*
 * @test
 * @summary Test method handle support for value types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          java.base/jdk.internal.misc:+open
 *          jdk.incubator.mvt
 * @compile -XDenableValueTypes TestMethodHandles.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run main ClassFileInstaller jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   compiler.valhalla.valuetypes.TestMethodHandles
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   compiler.valhalla.valuetypes.TestMethodHandles
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestMethodHandles
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestMethodHandles
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestMethodHandles
 */
public class TestMethodHandles extends ValueTypeTest {

    static {
        try {
            Class<?> clazz = TestMethodHandles.class;
            ClassLoader loader = clazz.getClassLoader();
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue3;", loader);
            test3_mh = lookup.findVirtual(clazz, "test3_target", mt);
            test4_mh = lookup.findVirtual(clazz, "test4_target", mt);
            test5_mh = lookup.findVirtual(clazz, "test5_target", mt);

            MethodType test6_mt1 = MethodType.fromMethodDescriptorString("(Qcompiler/valhalla/valuetypes/MyValue1;)I", loader);
            MethodType test6_mt2 = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue1;", loader);
            MethodHandle test6_mh1 = lookup.findStatic(clazz, "test6_helper1", test6_mt1);
            MethodHandle test6_mh2 = lookup.findStatic(clazz, "test6_helper2", test6_mt2);
            test6_mh = MethodHandles.filterReturnValue(test6_mh2, test6_mh1);

            MethodType test7_mt = MethodType.fromMethodDescriptorString("(Qcompiler/valhalla/valuetypes/MyValue1;)I", loader);
            test7_mh = lookup.findVirtual(clazz, "test7_target", test7_mt);

            MethodType test8_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue3;", loader);
            MethodHandle test8_mh1 = lookup.findVirtual(clazz, "test8_target1", test8_mt);
            MethodHandle test8_mh2 = lookup.findVirtual(clazz, "test8_target2", test8_mt);
            MethodType boolean_mt = MethodType.methodType(boolean.class);
            MethodHandle test8_mh_test = lookup.findVirtual(clazz, "test8_test", boolean_mt);
            test8_mh = MethodHandles.guardWithTest(test8_mh_test, test8_mh1, test8_mh2);

            MethodType myvalue2_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue2;", loader);
            test9_mh1 = lookup.findStatic(clazz, "test9_target1", myvalue2_mt);
            MethodHandle test9_mh2 = lookup.findStatic(clazz, "test9_target2", myvalue2_mt);
            MethodHandle test9_mh_test = lookup.findStatic(clazz, "test9_test", boolean_mt);
            test9_mh = MethodHandles.guardWithTest(test9_mh_test,
                                                    MethodHandles.invoker(myvalue2_mt),
                                                    MethodHandles.dropArguments(test9_mh2, 0, MethodHandle.class));

            MethodHandle test10_mh1 = lookup.findStatic(clazz, "test10_target1", myvalue2_mt);
            test10_mh2 = lookup.findStatic(clazz, "test10_target2", myvalue2_mt);
            MethodHandle test10_mh_test = lookup.findStatic(clazz, "test10_test", boolean_mt);
            test10_mh = MethodHandles.guardWithTest(test10_mh_test,
                                                    MethodHandles.dropArguments(test10_mh1, 0, MethodHandle.class),
                                                    MethodHandles.invoker(myvalue2_mt));

            MethodType test11_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue3;", loader);
            MethodHandle test11_mh1 = lookup.findVirtual(clazz, "test11_target1", test11_mt);
            MethodHandle test11_mh2 = lookup.findVirtual(clazz, "test11_target2", test11_mt);
            MethodHandle test11_mh3 = lookup.findVirtual(clazz, "test11_target3", test11_mt);
            MethodType test11_mt2 = MethodType.methodType(boolean.class);
            MethodHandle test11_mh_test1 = lookup.findVirtual(clazz, "test11_test1", test11_mt2);
            MethodHandle test11_mh_test2 = lookup.findVirtual(clazz, "test11_test2", test11_mt2);
            test11_mh = MethodHandles.guardWithTest(test11_mh_test1,
                                                    test11_mh1,
                                                    MethodHandles.guardWithTest(test11_mh_test2, test11_mh2, test11_mh3));

            MethodType test12_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue2;", loader);
            MethodHandle test12_mh1 = lookup.findStatic(clazz, "test12_target1", test12_mt);
            test12_mh2 = lookup.findStatic(clazz, "test12_target2", test12_mt);
            test12_mh3 = lookup.findStatic(clazz, "test12_target3", test12_mt);
            MethodType test12_mt2 = MethodType.methodType(boolean.class);
            MethodType test12_mt3 = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue2;", loader);
            MethodHandle test12_mh_test1 = lookup.findStatic(clazz, "test12_test1", test12_mt2);
            MethodHandle test12_mh_test2 = lookup.findStatic(clazz, "test12_test2", test12_mt2);
            test12_mh = MethodHandles.guardWithTest(test12_mh_test1,
                                                    MethodHandles.dropArguments(test12_mh1, 0, MethodHandle.class, MethodHandle.class),
                                                    MethodHandles.guardWithTest(test12_mh_test2,
                                                                                MethodHandles.dropArguments(MethodHandles.invoker(test12_mt3), 1, MethodHandle.class),
                                                                                MethodHandles.dropArguments(MethodHandles.invoker(test12_mt3), 0, MethodHandle.class))
                                                    );

            MethodHandle test13_mh1 = lookup.findStatic(clazz, "test13_target1", myvalue2_mt);
            test13_mh2 = lookup.findStatic(clazz, "test13_target2", myvalue2_mt);
            MethodHandle test13_mh_test = lookup.findStatic(clazz, "test13_test", boolean_mt);
            test13_mh = MethodHandles.guardWithTest(test13_mh_test,
                                                    MethodHandles.dropArguments(test13_mh1, 0, MethodHandle.class),
                                                    MethodHandles.invoker(myvalue2_mt));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Method handle lookup failed");
        }
    }

    public static void main(String[] args) throws Throwable {
        TestMethodHandles test = new TestMethodHandles();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class);
    }

    MyValue1 val1 = MyValue1.createDefaultInline();

    // When calling a method on __Value, passing fields as arguments is impossible
    @Test(failOn = ALLOC + STORE + LOAD)
    public String test1(MyValue1 v) {
        return v.toString();
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        boolean failed = false;
        try {
            test1(val1);
            failed = true;
        } catch (UnsupportedOperationException uoe) {
        }
        Asserts.assertFalse(failed);
    }

    // Same as above, but the method on __Value is inlined
    // hashCode allocates an exception so can't really check the graph shape
    @Test()
    public int test2(MyValue1 v) {
        return v.hashCode();
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        boolean failed = false;
        try {
            test2(val1);
            failed = true;
        } catch (UnsupportedOperationException uoe) {
        }
        Asserts.assertFalse(failed);
    }

    // Return values and method handles tests

    // Everything inlined
    final MyValue3 test3_vt = MyValue3.create();

    @ForceInline
    MyValue3 test3_target() {
        return test3_vt;
    }

    static final MethodHandle test3_mh;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + STORE + CALL)
    @Test(valid = ValueTypeReturnedAsFieldsOff, match = { ALLOC, STORE }, matchCount = { 1, 11 })
    MyValue3 test3() throws Throwable {
        return (MyValue3)test3_mh.invokeExact(this);
    }

    @DontCompile
    public void test3_verifier(boolean warmup) throws Throwable {
        MyValue3 vt = test3();
        test3_vt.verify(vt);
    }

    // Leaf method not inlined but returned type is known
    final MyValue3 test4_vt = MyValue3.create();
    @DontInline
    MyValue3 test4_target() {
        return test4_vt;
    }

    static final MethodHandle test4_mh;

    @Test
    MyValue3 test4() throws Throwable {
        return (MyValue3)test4_mh.invokeExact(this);
    }

    @DontCompile
    public void test4_verifier(boolean warmup) throws Throwable {
        Method helper_m = getClass().getDeclaredMethod("test64_target");
        if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
            WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test64_target not compiled");
        }
        MyValue3 vt = test4();
        test4_vt.verify(vt);
    }

    // Leaf method not inlined and returned type not known
    final MyValue3 test5_vt = MyValue3.create();
    @DontInline
    MyValue3 test5_target() {
        return test5_vt;
    }

    static final MethodHandle test5_mh;

    @Test
    MyValue3 test5() throws Throwable {
        return (MyValue3)test5_mh.invokeExact(this);
    }

    @DontCompile
    public void test5_verifier(boolean warmup) throws Throwable {
        // hack so C2 doesn't know the target of the invoke call
        Class c = Class.forName("java.lang.invoke.DirectMethodHandle");
        Method m = c.getDeclaredMethod("internalMemberName", Object.class);
        WHITE_BOX.testSetDontInlineMethod(m, warmup);
        MyValue3 vt = test5();
        test5_vt.verify(vt);
    }

    // When test75_helper1 is inlined in test75, the method handle
    // linker that called it is passed a pointer to a copy of vt
    // stored in memory. The method handle linker needs to load the
    // fields from memory before it inlines test75_helper1.
    static public int test6_helper1(MyValue1 vt) {
        return vt.x;
    }

    static MyValue1 test6_vt = MyValue1.createWithFieldsInline(rI, rL);
    static public MyValue1 test6_helper2() {
        return test6_vt;
    }

    static final MethodHandle test6_mh;

    @Test
    public int test6() throws Throwable {
        return (int)test6_mh.invokeExact();
    }

    @DontCompile
    public void test6_verifier(boolean warmup) throws Throwable {
        int i = test6();
        Asserts.assertEQ(i, test6_vt.x);
    }

    // Test method handle call with value type argument
    public int test7_target(MyValue1 vt) {
        return vt.x;
    }

    static final MethodHandle test7_mh;
    MyValue1 test7_vt = MyValue1.createWithFieldsInline(rI, rL);

    @Test
    public int test7() throws Throwable {
        return (int)test7_mh.invokeExact(this, test7_vt);
    }

    @DontCompile
    public void test7_verifier(boolean warmup) throws Throwable {
        int i = test7();
        Asserts.assertEQ(i, test7_vt.x);
    }

    // Return of target1 and target2 merged in a Lambda Form as an
    // __Value. Shouldn't cause any allocation
    final MyValue3 test8_vt1 = MyValue3.create();
    @ForceInline
    MyValue3 test8_target1() {
        return test8_vt1;
    }

    final MyValue3 test8_vt2 = MyValue3.create();
    @ForceInline
    MyValue3 test8_target2() {
        return test8_vt2;
    }

    boolean test8_bool = true;
    @ForceInline
    boolean test8_test() {
        return test8_bool;
    }

    static final MethodHandle test8_mh;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    MyValue3 test8() throws Throwable {
        return (MyValue3)test8_mh.invokeExact(this);
    }

    @DontCompile
    public void test8_verifier(boolean warmup) throws Throwable {
        test8_bool = !test8_bool;
        MyValue3 vt = test8();
        vt.verify(test8_bool ? test8_vt1 : test8_vt2);
    }

    // Similar as above but with the method handle for target1 not
    // constant. Shouldn't cause any allocation.
    @ForceInline
    static MyValue2 test9_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test9_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    static boolean test9_bool = true;
    @ForceInline
    static boolean test9_test() {
        return test9_bool;
    }

    static final MethodHandle test9_mh;
    static MethodHandle test9_mh1;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    long test9() throws Throwable {
        return ((MyValue2)test9_mh.invokeExact(test9_mh1)).hash();
    }

    @DontCompile
    public void test9_verifier(boolean warmup) throws Throwable {
        test9_bool = !test9_bool;
        long hash = test9();
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+(test9_bool ? 0 : 1), test9_bool).hash());
    }

    // Same as above but with the method handle for target2 not
    // constant. Shouldn't cause any allocation.
    @ForceInline
    static MyValue2 test10_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test10_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    static boolean test10_bool = true;
    @ForceInline
    static boolean test10_test() {
        return test10_bool;
    }

    static final MethodHandle test10_mh;
    static MethodHandle test10_mh2;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    long test10() throws Throwable {
        return ((MyValue2)test10_mh.invokeExact(test10_mh2)).hash();
    }

    @DontCompile
    public void test10_verifier(boolean warmup) throws Throwable {
        test10_bool = !test10_bool;
        long hash = test10();
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+(test10_bool ? 0 : 1), test10_bool).hash());
    }

    // Return of target1, target2 and target3 merged in Lambda Forms
    // as an __Value. Shouldn't cause any allocation
    final MyValue3 test11_vt1 = MyValue3.create();
    @ForceInline
    MyValue3 test11_target1() {
        return test11_vt1;
    }

    final MyValue3 test11_vt2 = MyValue3.create();
    @ForceInline
    MyValue3 test11_target2() {
        return test11_vt2;
    }

    final MyValue3 test11_vt3 = MyValue3.create();
    @ForceInline
    MyValue3 test11_target3() {
        return test11_vt3;
    }

    boolean test11_bool1 = true;
    @ForceInline
    boolean test11_test1() {
        return test11_bool1;
    }

    boolean test11_bool2 = true;
    @ForceInline
    boolean test11_test2() {
        return test11_bool2;
    }

    static final MethodHandle test11_mh;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    MyValue3 test11() throws Throwable {
        return (MyValue3)test11_mh.invokeExact(this);
    }

    static int test11_i = 0;
    @DontCompile
    public void test11_verifier(boolean warmup) throws Throwable {
        test11_i++;
        test11_bool1 = (test11_i % 2) == 0;
        test11_bool2 = (test11_i % 3) == 0;
        MyValue3 vt = test11();
        vt.verify(test11_bool1 ? test11_vt1 : (test11_bool2 ? test11_vt2 : test11_vt3));
    }

    // Same as above but with non constant target2 and target3
    @ForceInline
    static MyValue2 test12_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test12_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    @ForceInline
    static MyValue2 test12_target3() {
        return MyValue2.createWithFieldsInline(rI+2, true);
    }

    static boolean test12_bool1 = true;
    @ForceInline
    static boolean test12_test1() {
        return test12_bool1;
    }

    static boolean test12_bool2 = true;
    @ForceInline
    static boolean test12_test2() {
        return test12_bool2;
    }

    static final MethodHandle test12_mh;
    static MethodHandle test12_mh2;
    static MethodHandle test12_mh3;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    long test12() throws Throwable {
        return ((MyValue2)test12_mh.invokeExact(test12_mh2, test12_mh3)).hash();
    }

    static int test12_i = 0;

    @DontCompile
    public void test12_verifier(boolean warmup) throws Throwable {
        test12_i++;
        test12_bool1 = (test12_i % 2) == 0;
        test12_bool2 = (test12_i % 3) == 0;
        long hash = test12();
        int i = rI+(test12_bool1 ? 0 : (test12_bool2 ? 1 : 2));
        boolean b = test12_bool1 ? true : (test12_bool2 ? false : true);
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(i, b).hash());
    }

    static int test13_i = 0;

    @ForceInline
    static MyValue2 test13_target1() {
        return MyValue2.createWithFieldsInline(rI+test13_i, true);
    }

    @ForceInline
    static MyValue2 test13_target2() {
        return MyValue2.createWithFieldsInline(rI-test13_i, false);
    }

    @ForceInline
    static boolean test13_test() {
        return (test13_i % 100) == 0;
    }

    static final MethodHandle test13_mh;
    static MethodHandle test13_mh2;

    // Check that a buffered value returned by a compiled lambda form
    // is properly handled by the caller.
    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    @Warmup(11000)
    long test13() throws Throwable {
        return ((MyValue2)test13_mh.invokeExact(test13_mh2)).hash();
    }

    @DontCompile
    public void test13_verifier(boolean warmup) throws Throwable {
        test13_i++;
        long hash = test13();
        boolean b = (test13_i % 100) == 0;
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+test13_i * (b ? 1 : -1), b).hash());
    }
}
