/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import compiler.lib.ir_framework.*;

import static compiler.valhalla.inlinetypes.InlineTypes.rI;
import static compiler.valhalla.inlinetypes.InlineTypes.rL;

/*
 * @test
 * @key randomness
 * @summary Test calling native methods with inline type arguments from compiled code.
 * @library /test/lib /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestJNICalls
 */

@ForceCompileClassInitializer
public class TestJNICalls {

    public static void main(String[] args) {
        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;

        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .addHelperClasses(MyValue1.class)
                   .start();
    }

    static {
        System.loadLibrary("TestJNICalls");
    }

    public native Object testMethod1(MyValue1 o);
    public native long testMethod2(MyValue1 o);

    // Pass an inline type to a native method that calls back into Java code and returns an inline type
    @Test
    public MyValue1 test1(MyValue1 vt, boolean callback) {
        if (!callback) {
          return (MyValue1)testMethod1(vt);
        } else {
          return vt;
        }
    }

    @Run(test = "test1")
    @Warmup(10000) // Make sure native method is compiled
    public void test1_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 result = test1(vt, false);
        Asserts.assertEQ(result.hash(), vt.hash());
        result = test1(vt, true);
        Asserts.assertEQ(result.hash(), vt.hash());
    }

    // Pass an inline type to a native method that calls the hash method and returns the result
    @Test
    public long test2(MyValue1 vt) {
        return testMethod2(vt);
    }

    @Run(test = "test2")
    @Warmup(10000) // Make sure native method is compiled
    public void test2_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        long result = test2(vt);
        Asserts.assertEQ(result, vt.hash());
    }

    static primitive class MyValueWithNative {
        public final int x;

        private MyValueWithNative(int x) {
            this.x = x;
        }

        public native int testMethod3();
    }

    // Call a native method with an inline type receiver
    @Test
    public int test3(MyValueWithNative vt) {
        return vt.testMethod3();
    }

    @Run(test = "test3")
    @Warmup(10000) // Make sure native method is compiled
    public void test3_verifier() {
        MyValueWithNative vt = new MyValueWithNative(rI);
        int result = test3(vt);
        Asserts.assertEQ(result, rI);
    }
}
