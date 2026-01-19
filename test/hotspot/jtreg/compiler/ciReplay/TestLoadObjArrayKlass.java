/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8375548
 * @enablePreview
 * @library / /test/lib
 * @summary Testing that compiler replay correctly loads sub classes of ObjArrayKlass.
 * @requires vm.flightRecorder != true & vm.compMode != "Xint" & vm.compMode != "Xcomp" & vm.debug == true &
 *           vm.compiler2.enabled
 * @modules java.base/jdk.internal.misc
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   compiler.ciReplay.TestLoadObjArrayKlass
 */

package compiler.ciReplay;

public class TestLoadObjArrayKlass extends DumpReplayBase {

    public static void main(String[] args) {
        new TestLoadObjArrayKlass().runTest("-XX:CompileCommand=dontinline,*::test", "--enable-preview", TIERED_DISABLED_VM_OPTION);
    }

    @Override
    public void testAction() {
        positiveTest(TIERED_DISABLED_VM_OPTION, "-XX:+ReplayIgnoreInitErrors", "--enable-preview");
    }

    @Override
    public String getTestClass() {
        return TestLoadObjArrayKlassTest.class.getName();
    }
}

class TestLoadObjArrayKlassTest {
    static Object[] oArr = new A[100];

    public static void main(String[] args) {
        oArr[0] = new A();
        for (int i = 0; i < 10000; i++) {
            test();
        }
    }

    static void test() {
        Object[] oA = oArr;
        Object o = oA[0];
        // Use the object with its speculated type. This triggers the code path to call
        //
        //     Parse::create_speculative_inline_type_array_checks()
        //     ...
        //     ciArrayKlass::is_elem_null_free()
        //
        // which tries to directly query a ObjectArrayKlass instance in compiler replay instead of one of its subclasses
        // (i.e. RefArrayKlass or FlatArrayKlass). This is unexpected and results in an assertion failure.
        o.toString();
    }

    static value class A {
    }
}
