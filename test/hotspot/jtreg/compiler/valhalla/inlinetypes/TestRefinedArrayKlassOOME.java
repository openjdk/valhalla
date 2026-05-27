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
 * @summary Test array klass creation during compilation running out of metaspace.
 * @bug 8385473
 * @requires vm.compiler1.enabled
 * @library /test/lib /
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:MaxMetaspaceSize=14m -XX:TieredStopAtLevel=3 -Xbatch
 *                   -XX:CompileCommand=compileonly,${test.main.class}::test*
 *                   ${test.main.class}
 */

package compiler.valhalla.inlinetypes;

import java.lang.reflect.Method;

import jdk.test.whitebox.WhiteBox;

public class TestRefinedArrayKlassOOME {
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    static final int COMP_LEVEL_FULL_PROFILE = 3;

    interface I0 {
    }

    interface I1 {
    }

    interface I2 {
    }

    interface I3 {
    }

    interface I4 {
    }

    interface I5 {
    }

    interface I6 {
    }

    interface I7 {
    }

    interface I8 {
    }

    interface I9 {
    }

    interface I10 {
    }

    static I0[] test0(Object array) {
        return (I0[]) array;
    }

    static I1[] test1(Object array) {
        return (I1[]) array;
    }

    static I2[] test2(Object array) {
        return (I2[]) array;
    }

    static I3[] test3(Object array) {
        return (I3[]) array;
    }

    static I4[] test4(Object array) {
        return (I4[]) array;
    }

    static I5[] test5(Object array) {
        return (I5[]) array;
    }

    static I6[] test6(Object array) {
        return (I6[]) array;
    }

    static I7[] test7(Object array) {
        return (I7[]) array;
    }

    static I8[] test8(Object array) {
        return (I8[]) array;
    }

    static I9[] test9(Object array) {
        return (I9[]) array;
    }

    static I10[] test10(Object array) {
        return (I10[]) array;
    }

    static void fillClassMetaspace() throws Exception {
        Class<?>[] elementTypes = new Class<?>[] {I0.class, I1.class, I2.class, I3.class,
                                                  I4.class, I5.class, I6.class, I7.class,
                                                  I8.class, I9.class, I10.class};
        try {
            for (Class<?> elementType : elementTypes) {
                String arrayName = "L" + elementType.getName() + ";";
                ClassLoader loader = elementType.getClassLoader();
                for (int dimension = 1; dimension <= 255; dimension++) {
                    arrayName = "[" + arrayName;
                    Class.forName(arrayName, false, loader);
                }
            }
            throw new RuntimeException("No OutOfMemoryError triggered!");
        } catch (OutOfMemoryError oome) {
            // Expected
        }
    }

    public static void main(String[] args) throws Exception {
        // First fill up the class metaspace until OutOfMemoryError is triggered
        fillClassMetaspace();

        // Then trigger C1 compilation in the hope that array klass creation will fail due to OOME
        for (int i = 0; i <= 10; i++) {
            Method method = TestRefinedArrayKlassOOME.class.getDeclaredMethod("test" + i, Object.class);
            WHITE_BOX.enqueueMethodForCompilation(method, COMP_LEVEL_FULL_PROFILE);
        }
    }
}

