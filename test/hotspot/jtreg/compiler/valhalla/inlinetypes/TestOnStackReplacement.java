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

package compiler.valhalla.inlinetypes;

import jdk.test.lib.Asserts;
import java.lang.reflect.Method;

/*
 * @test
 * @key randomness
 * @summary Test on stack replacement (OSR) with inline types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @compile TestOnStackReplacement.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestOnStackReplacement
 */
public class TestOnStackReplacement extends InlineTypeTest {
    // Extra VM parameters for some test scenarios. See InlineTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 3: return new String[] {"-XX:FlatArrayElementMaxSize=0"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestOnStackReplacement test = new TestOnStackReplacement();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class, MyValue3Inline.class);
    }

    // Helper methods

    protected long hash() {
        return hash(rI, rL);
    }

    protected long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    // Test OSR compilation
    @Test() @Warmup(0) @OSRCompileOnly
    public long test1() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1[] va = new MyValue1[Math.abs(rI) % 3];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        long result = 0;
        // Long loop to trigger OSR compilation
        for (int i = 0 ; i < 50_000; ++i) {
            // Reference local inline type in interpreter state
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
    @Test(failOn = ALLOC + LOAD + STORE) @Warmup(0) @OSRCompileOnly
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
    @Test() @Warmup(0) @OSRCompileOnly
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

    // OSR compilation with Object local
    @DontCompile
    public Object test4_init() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @DontCompile
    public Object test4_body() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @Test() @Warmup(0) @OSRCompileOnly
    public Object test4() {
        Object vt = test4_init();
        for (int i = 0; i < 50_000; i++) {
            if (i % 2 == 1) {
                vt = test4_body();
            }
        }
        return vt;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        test4();
    }

    // OSR compilation with null inline type local

    MyValue1.ref nullField;

    @Test() @Warmup(0) @OSRCompileOnly
    public void test5() {
        MyValue1.ref vt = nullField;
        for (int i = 0; i < 50_000; i++) {
            if (vt != null) {
                throw new RuntimeException("test5 failed: vt should be null");
            }
        }
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        test5();
    }

    // Test OSR in method with inline type receiver
    primitive class Test6Value {
        public int f = 0;

        public int test() {
            int res = 0;
            for (int i = 1; i < 20_000; ++i) {
                res -= i;
            }
            return res;
        }
    }

    @Test() @Warmup(0) @OSRCompileOnly
    public void test6() {
        Test6Value tmp = new Test6Value();
        for (int i = 0; i < 100; ++i) {
            tmp.test();
        }
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        test6();
    }

    // Similar to test6 but with more fields and reserved stack entry
    static primitive class Test7Value1 {
        public int i1 = rI;
        public int i2 = rI;
        public int i3 = rI;
        public int i4 = rI;
        public int i5 = rI;
        public int i6 = rI;
    }

    static primitive class Test7Value2 {
        public int i1 = rI;
        public int i2 = rI;
        public int i3 = rI;
        public int i4 = rI;
        public int i5 = rI;
        public int i6 = rI;
        public int i7 = rI;
        public int i8 = rI;
        public int i9 = rI;
        public int i10 = rI;
        public int i11 = rI;
        public int i12 = rI;
        public int i13 = rI;
        public int i14 = rI;
        public int i15 = rI;
        public int i16 = rI;
        public int i17 = rI;
        public int i18 = rI;
        public int i19 = rI;
        public int i20 = rI;
        public int i21 = rI;

        public Test7Value1 vt = new Test7Value1();

        public int test(String[] args) {
            int res = 0;
            for (int i = 1; i < 20_000; ++i) {
                res -= i;
            }
            return res;
        }
    }

    @Test() @Warmup(0) @OSRCompileOnly
    public void test7() {
        Test7Value2 tmp = new Test7Value2();
        for (int i = 0; i < 10; ++i) {
            tmp.test(null);
        }
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        test7();
    }

    // Test OSR with scalarized inline type return
    MyValue3 test8_vt;

    @DontInline
    public MyValue3 test8_callee(int len) {
        test8_vt = MyValue3.create();
        int val = 0;
        for (int i = 0; i < len; ++i) {
            val = i;
        }
        test8_vt = test8_vt.setI(test8_vt, val);
        return test8_vt;
    }

    @Test() @Warmup(2)
    public int test8(int start) {
        MyValue3 vt = test8_callee(start);
        test8_vt.verify(vt);
        int result = 0;
        for (int i = 0; i < 50_000; ++i) {
            result += i;
        }
        return result;
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        test8(1);
        test8(50_000);
    }
}
