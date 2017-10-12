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
 * @summary Test Minimal Value Types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          java.base/jdk.internal.misc:+open
 *          jdk.incubator.mvt
 * @compile -XDenableValueTypes ValueCapableClass1.java ValueCapableClass2.java TestMinimalValueTypes.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run main ClassFileInstaller jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableMVT -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.TestMinimalValueTypes
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableMVT -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.TestMinimalValueTypes
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableMVT -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestMinimalValueTypes
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableMVT -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestMinimalValueTypes
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+EnableMVT -XX:+ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestMinimalValueTypes
 */
public class TestMinimalValueTypes extends ValueTypeTest {

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // Generate a MethodHandle that obtains field t of the derived value type
            vccUnboxLoadLongMH = MethodHandleBuilder.loadCode(lookup,
                    "vccUnboxLoadLong",
                    MethodType.methodType(long.class, ValueCapableClass1.class),
                    CODE -> {
                        CODE.
                        aload_0().
                        vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                        getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                        lreturn();
                    }
                    );

            // Generate a MethodHandle that obtains field x of the derived value type
            vccUnboxLoadIntMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "vccUnboxLoadInt",
                        MethodType.methodType(int.class, ValueCapableClass1.class),
                        CODE -> {
                            CODE.
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "x", "I").
                            ireturn();
                        }
                        );


            // Generate a MethodHandle that takes a value-capable class,
            // unboxes it, then boxes it again and returns it.
            vccUnboxBoxMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "vccUnboxBox",
                        MethodType.methodType(ValueCapableClass1.class, ValueCapableClass1.class),
                        CODE -> {
                            CODE.
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            vbox(ValueCapableClass1.class).
                            areturn();
                        }
                        );

            // Generate a MethodHandle that takes a value-capable class,
            // unboxes it, boxes it, reads a field from it, and returns the field.
            vccUnboxBoxLoadIntMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "vccUnboxBoxLoadInt",
                        MethodType.methodType(int.class, ValueCapableClass1.class),
                        CODE -> {
                            CODE.
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            vbox(ValueCapableClass1.class).
                            getfield(ValueCapableClass1.class, "x", "I").
                            ireturn();
                        }
                        );

            nullvccUnboxLoadLongMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "nullvccUnboxLoadLong",
                        MethodType.methodType(long.class),
                        CODE -> {
                            CODE.
                            aconst_null().
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                            lreturn();
                        }
                        );

            objectUnboxLoadLongMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "ObjectUnboxLoadLong",
                        MethodType.methodType(long.class, Object.class),
                        CODE -> {
                            CODE.
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                            lreturn();
                        }
                        );

            objectBoxMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "ObjectBox",
                        MethodType.methodType(long.class, Object.class, boolean.class),
                        CODE -> {
                            CODE.
                            iload_1().
                            iconst_1().
                            ifcmp(TypeTag.I, CondKind.NE, "not_equal").
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            vbox(ValueCapableClass1.class).
                            getfield(ValueCapableClass1.class, "t", "J").
                            lreturn().
                            label("not_equal").
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass2.class).valueClass()).
                            vbox(ValueCapableClass1.class).
                            getfield(ValueCapableClass1.class, "t", "J").
                            lreturn();
                        }
                        );

            checkedvccUnboxLoadLongMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "checkedVCCUnboxLoadLongMH",
                        MethodType.methodType(long.class),
                        CODE -> {
                            CODE.
                            invokestatic(ValueCapableClass1.class, "createInline", "()Lcompiler/valhalla/valuetypes/ValueCapableClass1;", false).
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                            lreturn();
                        }
                        );

            vastoreMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "Vastore",
                        MethodType.methodType(void.class, ValueCapableClass1.class),
                        CODE -> {
                            CODE.
                            iconst_1().
                            anewarray(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            iconst_0().
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            vastore().
                            return_();
                        }
                        );

            invalidVastoreMH = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                        "Vastore",
                        MethodType.methodType(void.class, ValueCapableClass2.class),
                        CODE -> {
                            CODE.
                            iconst_1().
                            anewarray(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                            iconst_0().
                            aload_0().
                            vunbox(ValueType.forClass(ValueCapableClass2.class).valueClass()).
                            vastore().
                            return_();
                        }
                        );

            MethodHandle test102_count = MethodHandles.constant(int.class, 100);
            ValueType<?> test102_VT = ValueType.forClass(ValueCapableClass2.class);
            MethodHandle test102_init = test102_VT.defaultValueConstant();
            MethodHandle test102_getfield = test102_VT.findGetter(lookup, "u", long.class);
            MethodHandle test102_add = lookup.findStatic(Long.class, "sum", MethodType.methodType(long.class, long.class, long.class));
            MethodHandle test102_body = MethodHandles.collectArguments(ValueCapableClass2.FACTORY,
                                                                      0,
                                                                      MethodHandles.dropArguments(MethodHandles.collectArguments(MethodHandles.insertArguments(test102_add,
                                                                                                                                                               0,
                                                                                                                                                               1L),
                                                                                                                                 0,
                                                                                                                                 test102_getfield),
                                                                                                  1,
                                                                                                  int.class));
            test13_mh = MethodHandles.collectArguments(test102_getfield,
                                                       0,
                                                       MethodHandles.countedLoop(test102_count, test102_init, test102_body));
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException("Method handle lookup failed");
        }
    }

    public static void main(String[] args) throws Throwable {
        TestMinimalValueTypes test = new TestMinimalValueTypes();
        test.run(args);
    }

    private static final ValueCapableClass1 vcc = ValueCapableClass1.create(rL, rI, (short)rI, (short)rI);
    private static final ValueCapableClass2 vcc2 = ValueCapableClass2.create(rL + 1);

    // Test vbox and vunbox

    private static final MethodHandle vccUnboxLoadLongMH;

    @Test
    public long test1() throws Throwable {
        return (long)vccUnboxLoadLongMH.invokeExact(vcc);
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        try {
            long result = test1();
            Asserts.assertEQ(vcc.t, result, "Field t of input and result must be equal.");
        } catch (Throwable t) {
            throw new RuntimeException("Test1 failed", t);
        }
    }

    private static final MethodHandle vccUnboxLoadIntMH;

    @Test
    public int test2() throws Throwable {
        return (int)vccUnboxLoadIntMH.invokeExact(vcc);
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        try {
            int result = test2();
            Asserts.assertEQ(vcc.x, result, "Field x of input and result must be equal.");
        } catch (Throwable t) {
            throw new RuntimeException("Test2 failed", t);
        }
    }


    private static final MethodHandle vccUnboxBoxMH;

    @Test
    public ValueCapableClass1 test3() throws Throwable {
        return (ValueCapableClass1)vccUnboxBoxMH.invokeExact(vcc);
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        try {
            ValueCapableClass1 result = test3();
            Asserts.assertEQ(vcc.value(), result.value(), "Value of VCC and returned VCC must be equal");
        } catch (Throwable t) {
            throw new RuntimeException("Test3 failed", t);
        }
    }

    private static final MethodHandle vccUnboxBoxLoadIntMH;

    @Test
    public int test4() throws Throwable {
        return (int)vccUnboxBoxLoadIntMH.invokeExact(vcc);
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        try {
            int result = test4();
            Asserts.assertEQ(vcc.x, result, "Field x of VCC and result must be equal");
        } catch (Throwable t) {
            throw new RuntimeException("Test4 failed in the interpeter", t);
        }
    }


    /* The compiler is supposed to determine that the value to be
     * unboxed in nullcvvUnboxLoadLong is always null. Therefore, the
     * compiler generates only the path leading to the corresponding
     * uncommon trap. */

    private static final MethodHandle nullvccUnboxLoadLongMH;

    @Test(failOn = RETURN)
    public long test5() throws Throwable {
        return (long)nullvccUnboxLoadLongMH.invokeExact();
    }

    @DontCompile
    public void test5_verifier(boolean warmup) throws Throwable {
        try {
            long result = test5();
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }


    /* The compiler is supposed to determine that the allocated
     * ValueCapableClass1 instance is never null (and therefore not
     * generate a null check). Also, the source and target type match
     * (known at compile time), so no type check is needed either.*/

    private static final MethodHandle checkedvccUnboxLoadLongMH;

    @Test(failOn = NPE)
    public long test6() throws Throwable {
        return (long)checkedvccUnboxLoadLongMH.invokeExact();
    }

    @DontCompile
    public void test6_verifier(boolean warmup) throws Throwable {
        long result = test6();
        Asserts.assertEQ(result, 17L);
    }

    /* The compiler is supposed to emit a runtime null check because
     * it does not have enough information to determine that the value
     * to be unboxed is not null (and either that it is null). The
     * declared type of the */
    @Test(match = {NPE}, matchCount = {1})
    public long test7(ValueCapableClass1 vcc) throws Throwable {
        return (long)vccUnboxLoadLongMH.invokeExact(vcc);
    }

    @DontCompile
    public void test7_verifier(boolean warmup) throws Throwable {
        try {
            long result = test7(null);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (NullPointerException e) {
        }
    }


    /* Attempt to unbox an object that is not a subclass of the
     * value-capable class derived from the value type specified in
     * the vunbox bytecode. */

    private static final MethodHandle objectUnboxLoadLongMH;

    @Test(match = {NPE,CCE}, matchCount = {1,1})
    public long test8(Object vcc) throws Throwable {
        return (long)objectUnboxLoadLongMH.invokeExact(vcc);
    }

    @DontCompile
    public void test8_verifier(boolean warmup) throws Throwable {
        try {
            long result = test8(new Object());
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        try {
            long result = test8(vcc2);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        Asserts.assertEQ(test8(vcc), rL);
    }


    /* Generate an if-then-else construct with one path that contains
     * an invalid boxing operation (boxing of a value-type to a
     * non-matching value-capable class).*/

    private static final MethodHandle objectBoxMH;

    @Test(match = {NPE, CCE}, matchCount = {2, 2})
    public long test9(Object obj, boolean warmup) throws Throwable {
        return (long)objectBoxMH.invokeExact(obj, warmup);
    }

    @DontCompile
    public void test9_verifier(boolean warmup) throws Throwable {
        try {
            long result = test9(vcc2, true);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        Asserts.assertEQ(test9(vcc, true), rL);

        try {
            long result = test9(vcc2, false);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        try {
            long result = test9(vcc, false);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }
    }


    /* Create a new value type array and store a value type into
     * it. The test should pass without throwing an exception. */

    private static final MethodHandle vastoreMH;

    @Test
    public void test10() throws Throwable {
        vastoreMH.invokeExact(vcc);
    }

    public void test10_verifier(boolean warmup) throws Throwable {
        test10();
    }


    /* Create a new value type array with element type
     * ValueCapableClass1 and attempt to store a value type of type
     * ValueCapableClass2 into it. */

    private static final MethodHandle invalidVastoreMH;

    @Test
    public void test11() throws Throwable {
        invalidVastoreMH.invokeExact(vcc2);
    }

    public void test11_verifier(boolean warmup) throws Throwable {
        boolean exceptionThrown = false;
        try {
            test11();
        } catch (ArrayStoreException e) {
            exceptionThrown = true;
        }
        Asserts.assertTrue(exceptionThrown, "ArrayStoreException must be thrown");
    }

    // Test Class::cast intrinsic
    @Test()
    public Object test12(Class<?> cls, Object o) throws ClassCastException {
        return cls.cast(o);
    }

    public void test12_verifier(boolean warmup) {
        try {
            test12(ValueCapableClass1.class, vcc);
        } catch (ClassCastException e) {
            throw new RuntimeException("test12_1 failed");
        }
        try {
            test12(__Value.class, new Object());
            throw new RuntimeException("test12_2 failed");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Simple reduction with intermediate result merged in a Lambda
    // Form as an __Value. Shouldn't cause any allocations. The entire
    // loop should go away as the result is a constant.
    static final MethodHandle test13_mh;

    @Test(failOn = ALLOC + STORE + LOOP + STOREVALUETYPEFIELDS)
    long test13() throws Throwable {
        return (long)test13_mh.invokeExact();
    }

    @DontCompile
    public void test13_verifier(boolean warmup) throws Throwable {
        long v = test13();
        Asserts.assertEQ(v, 100L);
    }
}
