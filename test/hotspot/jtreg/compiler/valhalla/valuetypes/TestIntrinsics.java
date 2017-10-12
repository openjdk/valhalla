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

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test intrinsic support for value types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          java.base/jdk.internal.misc:+open
 *          jdk.incubator.mvt
 * @compile -XDenableValueTypes TestIntrinsics.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run main ClassFileInstaller jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+EnableValhalla -Djdk.lang.reflect.DVT=true
 *                   compiler.valhalla.valuetypes.TestIntrinsics
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
        Asserts.assertTrue(test1(__Value.class, MyValue1.class), "test1_1 failed");
        Asserts.assertTrue(test1(MyValue1.class, MyValue1.class), "test1_2 failed");
        Asserts.assertTrue(test1(Object.class, java.util.ArrayList.class), "test1_3 failed");
        Asserts.assertTrue(test1(java.util.ArrayList.class, java.util.ArrayList.class), "test1_4 failed");
        Asserts.assertTrue(!test1(Object.class, MyValue1.class), "test1_5 failed");
        Asserts.assertTrue(!test1(__Value.class, java.util.ArrayList.class), "test1_6 failed");
    }

    // Verify that Class::isAssignableFrom checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test2() {
        boolean check1 = java.util.AbstractList.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check2 = MyValue1.class.isAssignableFrom(MyValue1.class);
        boolean check3 = Object.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check4 = java.lang.__Value.class.isAssignableFrom(MyValue1.class);
        boolean check5 = !Object.class.isAssignableFrom(MyValue1.class);
        boolean check6 = !MyValue1.class.isAssignableFrom(Object.class);
        return check1 && check2 && check3 && check4 && check5 && check6;
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
        Asserts.assertTrue(test3(__Value.class) == null, "test3_1 failed");
        Asserts.assertTrue(test3(Object.class) == null, "test3_2 failed");
        Asserts.assertTrue(test3(MyValue1.class) == __Value.class, "test3_3 failed");
        Asserts.assertTrue(test3(Class.class) == Object.class, "test3_4 failed");
    }

    // Verify that Class::getSuperclass checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test4() {
        boolean check1 = __Value.class.getSuperclass() == null;
        boolean check2 = Object.class.getSuperclass() == null;
        boolean check3 = MyValue1.class.getSuperclass() == __Value.class;
        boolean check4 = Class.class.getSuperclass() == Object.class;
        return check1 && check2 && check3 && check4;
    }

    public void test4_verifier(boolean warmup) {
        Asserts.assertTrue(test4(), "test4 failed");
    }
}
