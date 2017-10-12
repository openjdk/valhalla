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
 * @summary Test on stack replacement (OSR) with value types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          java.base/jdk.internal.misc:+open
 *          jdk.incubator.mvt
 * @compile -XDenableValueTypes TestOnStackReplacement.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run main ClassFileInstaller jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.TestOnStackReplacement
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.TestOnStackReplacement
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestOnStackReplacement
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestOnStackReplacement
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestOnStackReplacement
 */
public class TestOnStackReplacement extends ValueTypeTest {

    public static void main(String[] args) throws Throwable {
        TestOnStackReplacement test = new TestOnStackReplacement();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class);
    }

    // Helper methods

    protected long hash() {
        return hash(rI, rL);
    }

    protected long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    // Test OSR compilation
    @Test()
    public long test1() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1[] va = new MyValue1[Math.abs(rI) % 3];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        long result = 0;
        // Long loop to trigger OSR compilation
        for (int i = 0 ; i < 50_000; ++i) {
            // Reference local value type in interpreter state
            result = v.hash();
            for (int j = 0; j < va.length; ++j) {
                result += va[j].hash();
            }
        }
        return result;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        long result = test1();
        Asserts.assertEQ(result, ((Math.abs(rI) % 3) + 1) * hash());
    }

    // Test loop peeling
    @Test(failOn = ALLOC + LOAD + STORE)
    public void test2() {
        MyValue1 v = MyValue1.createWithFieldsInline(0, 1);
        // Trigger OSR compilation and loop peeling
        for (int i = 0; i < 50_000; ++i) {
            if (v.x != i || v.y != i + 1) {
                // Uncommon trap
                throw new RuntimeException("test2 failed");
            }
            v = MyValue1.createWithFieldsInline(i + 1, i + 2);
        }
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        test2();
    }

    // Test loop peeling and unrolling
    @Test()
    public void test3() {
        MyValue1 v1 = MyValue1.createWithFieldsInline(0, 0);
        MyValue1 v2 = MyValue1.createWithFieldsInline(1, 1);
        // Trigger OSR compilation and loop peeling
        for (int i = 0; i < 50_000; ++i) {
            if (v1.x != 2*i || v2.x != i+1 || v2.y != i+1) {
                // Uncommon trap
                throw new RuntimeException("test3 failed");
            }
            v1 = MyValue1.createWithFieldsInline(2*(i+1), 0);
            v2 = MyValue1.createWithFieldsInline(i+2, i+2);
        }
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        test3();
    }

    // OSR compilation with __Value local
    @DontCompile
    public __Value test4_init() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @DontCompile
    public __Value test4_body() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @Test()
    public __Value test4() throws Throwable {
        __Value vt = test4_init();
        for (int i = 0; i < 50_000; i++) {
            if (i % 2 == 1) {
                vt = test4_body();
            }
        }
        return vt;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) throws Throwable {
        test4();
    }
}
