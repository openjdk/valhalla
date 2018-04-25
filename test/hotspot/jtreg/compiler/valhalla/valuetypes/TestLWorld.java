/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.experimental.value.MethodHandleBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test value types in LWorld.
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDenableValueTypes TestLWorld.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   compiler.valhalla.valuetypes.TestLWorld
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   compiler.valhalla.valuetypes.TestLWorld
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestLWorld
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestLWorld
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestLWorld
 */
public class TestLWorld extends ValueTypeTest {

    public static void main(String[] args) throws Throwable {
        TestLWorld test = new TestLWorld();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class, MyValue3Inline.class);
    }

    // Helper methods

    protected long hash() {
        return hash(rI, rL);
    }

    protected long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    // Test passing a value type as an Object
    @DontInline
    public Object test1_dontinline1(Object o) {
        return o;
    }

    @DontInline
    public MyValue1 test1_dontinline2(Object o) {
        return (MyValue1)o;
    }

    @ForceInline
    public Object test1_inline1(Object o) {
        return o;
    }

    @ForceInline
    public MyValue1 test1_inline2(Object o) {
        return (MyValue1)o;
    }

    @Test()
    public MyValue1 test1() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        vt = (MyValue1)test1_dontinline1(vt);
        vt =           test1_dontinline2(vt);
        vt = (MyValue1)test1_inline1(vt);
        vt =           test1_inline2(vt);
        return vt;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        Asserts.assertEQ(test1().hash(), hash());
    }

    // Test storing/loading value types to/from Object and value type fields
    Object objectField1 = null;
    Object objectField2 = null;
    Object objectField3 = null;
    Object objectField4 = null;
    Object objectField5 = null;
    Object objectField6 = null;

    __Flattenable  MyValue1 valueField1 = MyValue1.createWithFieldsInline(rI, rL);
    __Flattenable  MyValue1 valueField2 = MyValue1.createWithFieldsInline(rI, rL);
    __NotFlattened MyValue1 valueField3 = MyValue1.createWithFieldsInline(rI, rL);
    __Flattenable  MyValue1 valueField4;
    __NotFlattened MyValue1 valueField5;

    static __NotFlattened MyValue1 staticValueField1 = MyValue1.createWithFieldsInline(rI, rL);
    static __Flattenable  MyValue1 staticValueField2 = MyValue1.createWithFieldsInline(rI, rL);
    static __Flattenable  MyValue1 staticValueField3;
    static __NotFlattened MyValue1 staticValueField4;

    @DontInline
    public Object readValueField5() {
        return (Object)valueField5;
    }

    @DontInline
    public Object readStaticValueField4() {
        return (Object)staticValueField4;
    }

    @Test()
    public long test2(MyValue1 vt1, Object vt2) {
        objectField1 = vt1;
        objectField2 = (MyValue1)vt2;
        objectField3 = MyValue1.createWithFieldsInline(rI, rL);
        objectField4 = MyValue1.createWithFieldsDontInline(rI, rL);
        objectField5 = valueField1;
        objectField6 = valueField3;
        valueField1 = (MyValue1)objectField1;
        valueField2 = (MyValue1)vt2;
        valueField3 = (MyValue1)vt2;
        staticValueField1 = (MyValue1)objectField1;
        staticValueField2 = (MyValue1)vt1;
        // Don't inline these methods because reading NULL will trigger a deoptimization
        if (readValueField5() != null || readStaticValueField4() != null) {
            throw new RuntimeException("Should be null");
        }
        return ((MyValue1)objectField1).hash() + ((MyValue1)objectField2).hash() +
               ((MyValue1)objectField3).hash() + ((MyValue1)objectField4).hash() +
               ((MyValue1)objectField5).hash() + ((MyValue1)objectField6).hash() +
                valueField1.hash() + valueField2.hash() + valueField3.hash() + valueField4.hashPrimitive() +
                staticValueField1.hash() + staticValueField2.hash() + staticValueField3.hashPrimitive();
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 def = MyValue1.createDefaultDontInline();
        long result = test2(vt, vt);
        Asserts.assertEQ(result, 11*vt.hash() + 2*def.hashPrimitive());
    }

    // Test merging value types and objects
    @Test()
    public Object test3(int state) {
        Object res = null;
        if (state == 0) {
            res = new Integer(rI);
        } else if (state == 1) {
            res = MyValue1.createWithFieldsInline(rI, rL);
        } else if (state == 2) {
            res = MyValue1.createWithFieldsDontInline(rI, rL);
        } else if (state == 3) {
            res = (MyValue1)objectField1;
        } else if (state == 4) {
            res = valueField1;
        } else if (state == 5) {
            res = null;
        }
        return res;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        objectField1 = valueField1;
        Object result = null;
        result = test3(0);
        Asserts.assertEQ((Integer)result, rI);
        result = test3(1);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(2);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(3);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(4);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(5);
        Asserts.assertEQ(result, null);
    }

    // Test merging value types and objects in loops
    @Test()
    public Object test4(int iters) {
        Object res = new Integer(rI);
        for (int i = 0; i < iters; ++i) {
            if (res instanceof Integer) {
                res = MyValue1.createWithFieldsInline(rI, rL);
            } else {
                res = MyValue1.createWithFieldsInline(((MyValue1)res).x + 1, rL);
            }
        }
        return res;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        Integer result1 = (Integer)test4(0);
        Asserts.assertEQ(result1, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test4(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test value types in object variables that are live at safepoint
    @Test(failOn = ALLOC + STORE + LOOP)
    public long test5(MyValue1 arg, boolean deopt) {
        Object vt1 = MyValue1.createWithFieldsInline(rI, rL);
        Object vt2 = MyValue1.createWithFieldsDontInline(rI, rL);
        Object vt3 = arg;
        Object vt4 = valueField1;
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test5"));
        }
        return ((MyValue1)vt1).hash() + ((MyValue1)vt2).hash() +
               ((MyValue1)vt3).hash() + ((MyValue1)vt4).hash();
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        long result = test5(valueField1, !warmup);
        Asserts.assertEQ(result, 4*hash());
    }

    // Test comparing value types with objects
    @Test(failOn = ALLOC + LOAD + STORE + LOOP)
    public boolean test6(Object arg) {
        Object vt = MyValue1.createWithFieldsInline(rI, rL);
        if (vt == arg || vt == (Object)valueField1 || vt == objectField1 || vt == null ||
            arg == vt || (Object)valueField1 == vt || objectField1 == vt || null == vt) {
            return true;
        }
        return false;
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        boolean result = test6(null);
        Asserts.assertFalse(result);
    }

    // Test correct handling of null-ness of value types

    __NotFlattened MyValue1 nullField;

    @Test
    public long test7(MyValue1 vt) {
        long result = 0;
        try {
            result = vt.hash();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    private static final MethodHandle callTest7WithNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "callTest7WithNull",
        MethodType.methodType(void.class, TestLWorld.class),
        CODE -> {
            CODE.
            aload_0().
            aconst_null().
            invokevirtual(TestLWorld.class, "test7", "(Lcompiler/valhalla/valuetypes/MyValue1;)J", false).
            return_();
        }
        );

    @DontCompile
    public void test7_verifier(boolean warmup) throws Throwable {
        long result = (long)callTest7WithNull.invoke(this);
        Asserts.assertEquals(result, 0L);
    }

    @Test
    public long test8(MyValue1 vt) {
        long result = 0;
        try {
            result = vt.hashInterpreted();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @DontCompile
    public void test8_verifier(boolean warmup) throws Throwable {
        long result = test8(nullField);
        Asserts.assertEquals(result, 0L);
    }

    @Test
    public long test9() {
        long result = 0;
        try {
            if ((Object)nullField != null) {
                throw new RuntimeException("nullField should be null");
            }
            result = nullField.hash();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @DontCompile
    public void test9_verifier(boolean warmup) throws Throwable {
        long result = test9();
        Asserts.assertEquals(result, 0L);
    }

    @Test
    public void test10() {
        try {
            valueField1 = nullField;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @DontCompile
    public void test10_verifier(boolean warmup) throws Throwable {
        test10();
    }

    @Test
    public MyValue1 test11(MyValue1 vt) {
        try {
            Object o = vt;
            vt = (MyValue1)o;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }

        // Should not throw
        vt = test11_dontinline(vt);
        vt = test11_inline(vt);
        return vt;
    }

    @DontCompile
    public void test11_verifier(boolean warmup) throws Throwable {
        MyValue1 vt = test11(nullField);
        Asserts.assertEquals((Object)vt, null);
    }

    @DontInline
    public MyValue1 test11_dontinline(MyValue1 vt) {
        return vt;
    }

    @ForceInline
    public MyValue1 test11_inline(MyValue1 vt) {
        return vt;
    }

    @Test
    public MyValue1 test12(Object obj) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            vt = (MyValue1)obj;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return vt;
    }

    @DontCompile
    public void test12_verifier(boolean warmup) throws Throwable {
        MyValue1 vt = test12(null);
        Asserts.assertEquals(vt.hash(), hash());
    }

    private static final MethodHandle getNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "getNull",
        MethodType.methodType(MyValue1.class),
        CODE -> {
            CODE.
            aconst_null().
            areturn();
        }
        );

    @Test
    public void test13() throws Throwable {
        try {
            valueField1 = (MyValue1)getNull.invoke();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
    }

    @DontCompile
    public void test13_verifier(boolean warmup) throws Throwable {
        test13();
    }

    // null constant
    private static final MethodHandle setNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "setNull",
        MethodType.methodType(void.class, TestLWorld.class),
        CODE -> {
            CODE.
            aload_0().
            aconst_null().
            putfield(TestLWorld.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            return_();
        }
        );

    @Test
    public void test14() throws Throwable {
        try {
            setNull.invoke(this);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
    }

    @DontCompile
    public void test14_verifier(boolean warmup) throws Throwable {
        test14();
    }

    // merge of 2 values, one being null
    @Test
    public void test15(boolean flag1) throws Throwable {
        MyValue1 v;
        if (flag1) {
            v = valueField1;
        } else {
            v = valueField5;
        }
        valueField1 = v;
    }

    @DontCompile
    public void test15_verifier(boolean warmup) throws Throwable {
        test15(true);
        try {
            test15(false);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
    }

    // null constant
    private static final MethodHandle mergeNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "mergeNull",
        MethodType.methodType(void.class, TestLWorld.class, boolean.class),
        CODE -> {
            CODE.
            iload_1().
            iconst_0().
            ifcmp(TypeTag.I, CondKind.EQ, "null").
            aload_0().
            getfield(TestLWorld.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            goto_("continue").
            label("null").
            aconst_null().
            label("continue").
            aload_0().
            swap().
            putfield(TestLWorld.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            return_();
        }
        );

    @Test
    public void test16(boolean flag) throws Throwable {
        mergeNull.invoke(this, flag);
    }

    @DontCompile
    public void test16_verifier(boolean warmup) throws Throwable {
        test16(true);
        try {
            test16(false);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
    }

    // merge of value and non value
    @Test
    public Object test17(boolean flag) throws Throwable {
        Object res = null;
        if (flag) {
            res = valueField1;
        } else {
            res = objectField1;
        }
        return res;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) throws Throwable {
        test17(true);
        test17(false);
    }

    @Test
    public Object test18(boolean flag) throws Throwable {
        Object res = null;
        if (flag) {
            res = objectField1;
        } else {
            res = valueField1;
        }
        return res;
    }

    @DontCompile
    public void test18_verifier(boolean warmup) throws Throwable {
        test17(true);
        test17(false);
    }

    // Disable for now: bug in MethodHandleBuilder
    // null constant
    // private static final MethodHandle mergeNull2 = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
    //     "mergeNull2",
    //     MethodType.methodType(void.class, TestLWorld.class, boolean.class),
    //     CODE -> {
    //         CODE.
    //         iload_1().
    //         iconst_0().
    //         ifcmp(TypeTag.I, CondKind.EQ, "not_null").
    //         aconst_null().
    //         goto_("continue").
    //         label("not_null").
    //         aload_0().
    //         getfield(TestLWorld.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
    //         label("continue").
    //         aload_0().
    //         swap().
    //         putfield(TestLWorld.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
    //         return_();
    //     }
    //     );

    // @Test
    // public void test19(boolean flag) throws Throwable {
    //     mergeNull2.invoke(this, flag);
    // }

    // @DontCompile
    // public void test19_verifier(boolean warmup) throws Throwable {
    //     test19(false);
    //     try {
    //         test19(true);
    //         throw new RuntimeException("NullPointerException expected");
    //     } catch (NullPointerException e) {
    //     }
    // }

    // merge of values in a loop, stored in an object local
    @Test
    public Object test20() {
        Object o = valueField1;
        for (int i = 1; i < 100; i *= 2) {
            MyValue1 v = (MyValue1)o;
            o = MyValue1.setX(v, v.x + 1);
        }
        return o;
    }

    @DontCompile
    public void test20_verifier(boolean warmup) throws Throwable {
        test20();
    }

    // merge of values in an object local
    public Object test21_helper() {
        return valueField1;
    }

    @Test(failOn = ALLOC + LOAD + STORE)
    public void test21(boolean flag) {
        Object o = null;
        if (flag) {
            o = valueField1;
        } else {
            o = test21_helper();
        }
        valueField1 = (MyValue1)o;
    }

    @DontCompile
    public void test21_verifier(boolean warmup) throws Throwable {
        test21(true);
        test21(false);
    }

    // null return
    int test_22_cnt;

    @DontInline
    public MyValue1 test22_helper() {
        test_22_cnt++;
        return valueField5;
    }

    @Test
    public void test22() {
        valueField1 = test22_helper();
    }

    @DontCompile
    public void test22_verifier(boolean warmup) throws Throwable {
        try {
            test_22_cnt = 0;
            test22();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
        if (test_22_cnt != 1) {
            throw new RuntimeException("call executed twice");
        }
    }

    // null return at virtual call
    class A {
        public MyValue1 test23_helper() {
            return valueField5;
        }
    }

    class B extends A {
        public MyValue1 test23_helper() {
            return valueField5;
        }
    }

    class C extends A {
        public MyValue1 test23_helper() {
            return valueField5;
        }
    }

    class D extends A {
        public MyValue1 test23_helper() {
            return valueField5;
        }
    }

    @Test
    public void test23(A a) {
        valueField1 = a.test23_helper();
    }

    @DontCompile
    public void test23_verifier(boolean warmup) throws Throwable {
        A b = new B();
        A c = new C();
        A d = new D();
        try {
            test23(b);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
        try {
            test23(c);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
        try {
            test23(d);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
        }
    }

// TODO enable and add more tests
/*
    // Test subtype check when casting to value type
    @Test
    public MyValue1 test13(MyValue1 vt, Object obj) {
        try {
            vt = (MyValue1)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        return vt;
    }

    @DontCompile
    public void test13_verifier(boolean warmup) throws Throwable {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 result = test13(vt, new Integer(rI));
        Asserts.assertEquals(result.hash(), vt.hash());
    }
*/

// TODO Add tests for value type with non-flattened/non-flattenable value type field
// TODO Add test for writing null to a non-flattenable value type field
// TODO Add tests for null returns
// TODO Test above code with value types implementing interfaces
// TODO Add array tests

}
