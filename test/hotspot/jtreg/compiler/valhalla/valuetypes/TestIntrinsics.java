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

import java.lang.reflect.Array;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test intrinsic support for value types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers TestIntrinsics.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   compiler.valhalla.valuetypes.TestIntrinsics
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   compiler.valhalla.valuetypes.TestIntrinsics
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestIntrinsics
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -XX:-MonomorphicArrayCheck
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestIntrinsics
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -XX:-MonomorphicArrayCheck
 *                   -DVerifyIR=false compiler.valhalla.valuetypes.TestIntrinsics
 */
public class TestIntrinsics extends ValueTypeTest {

    public static void main(String[] args) throws Throwable {
        TestIntrinsics test = new TestIntrinsics();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class);
    }

    // Test correctness of the Class::isAssignableFrom intrinsic
    @Test()
    public boolean test1(Class<?> supercls, Class<?> subcls) {
        return supercls.isAssignableFrom(subcls);
    }

    public void test1_verifier(boolean warmup) {
        Asserts.assertTrue(test1(Object.class, MyValue1.class), "test1_1 failed");
        Asserts.assertTrue(test1(MyValue1.class, MyValue1.class), "test1_2 failed");
        Asserts.assertTrue(test1(Object.class, java.util.ArrayList.class), "test1_3 failed");
        Asserts.assertTrue(test1(java.util.ArrayList.class, java.util.ArrayList.class), "test1_4 failed");
    }

    // Verify that Class::isAssignableFrom checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test2() {
        boolean check1 = java.util.AbstractList.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check2 = MyValue1.class.isAssignableFrom(MyValue1.class);
        boolean check3 = Object.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check4 = Object.class.isAssignableFrom(MyValue1.class);
        boolean check5 = !MyValue1.class.isAssignableFrom(Object.class);
        return check1 && check2 && check3 && check4 && check5;
    }

    public void test2_verifier(boolean warmup) {
        Asserts.assertTrue(test2(), "test2 failed");
    }

    // Test correctness of the Class::getSuperclass intrinsic
    @Test()
    public Class<?> test3(Class<?> cls) {
        return cls.getSuperclass();
    }

    public void test3_verifier(boolean warmup) {
        Asserts.assertTrue(test3(Object.class) == null, "test3_1 failed");
        Asserts.assertTrue(test3(MyValue1.class) == Object.class, "test3_2 failed");
        Asserts.assertTrue(test3(Class.class) == Object.class, "test3_3 failed");
    }

    // Verify that Class::getSuperclass checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test4() {
        boolean check1 = Object.class.getSuperclass() == null;
        boolean check2 = MyValue1.class.getSuperclass() == Object.class;
        boolean check3 = Class.class.getSuperclass() == Object.class;
        return check1 && check2 && check3;
    }

    public void test4_verifier(boolean warmup) {
        Asserts.assertTrue(test4(), "test4 failed");
    }

    // TODO re-enable once Object method support is implemented
/*
    // Test toString() method
    @Test(failOn = ALLOC + STORE + LOAD)
    public String test5(MyValue1 v) {
        return v.toString();
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createDefaultInline();
        test5(v);
    }

    // Test hashCode() method
    @Test()
    public int test6(MyValue1 v) {
        return v.hashCode();
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createDefaultInline();
        test6(v);
    }
*/

    // Test default value type array creation via reflection
    @Test()
    public void test7(Class<?> componentType, int len, long hash) {
        Object[] va = (Object[])Array.newInstance(componentType, len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)va[i]).hashPrimitive(), hash);
        }
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        int len = Math.abs(rI) % 42;
        long hash = MyValue1.createDefaultDontInline().hashPrimitive();
        test7(MyValue1.class, len, hash);
    }

    // Class.isInstance
    @Test()
    public boolean test8(Class c, MyValue1 vt) {
        return c.isInstance(vt);
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test8(MyValue1.class, vt);
        Asserts.assertTrue(result);
    }

    @Test()
    public boolean test9(Class c, MyValue1 vt) {
        return c.isInstance(vt);
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test9(MyValue2.class, vt);
        Asserts.assertFalse(result);
    }

    // TODO add tests for _identityHashCode,_clone,_copyOf,_copyOfRange,_arraycopy,_allocateUninitializedArray, ...

}
