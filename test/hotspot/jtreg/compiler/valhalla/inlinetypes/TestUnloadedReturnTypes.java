/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary Test scalarization in returns with unloaded return types.
 * @library /test/lib /compiler/whitebox /
 * @build jdk.test.whitebox.WhiteBox
 * @compile -XDenablePrimitiveClasses TestUnloadedReturnTypes.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -Xbatch -XX:CompileCommand=dontinline,*::test*
 *                   TestUnloadedReturnTypes
 */

import java.lang.reflect.Method;

import jdk.test.whitebox.WhiteBox;

value class MyValue1 {
    int x;

    public MyValue1(int x) {
        this.x = x;
    }
}

class MyClass {

    static MyValue1 test(boolean b) {
        return b ? new MyValue1(42) : null;
    }
}

public class TestUnloadedReturnTypes {
    public static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    static Object res = null;

    public static void test(boolean b) {
        res = MyClass.test(b);
    }

    public static void main(String[] args) throws Exception {
        // C1 compile caller method
        Method m = TestUnloadedReturnTypes.class.getMethod("test", boolean.class);
        WHITE_BOX.enqueueMethodForCompilation(m, 3);

        // Make sure the callee method is C2 compiled
        for (int i = 0; i < 100_000; ++i) {
            MyClass.test((i % 2) == 0);
        }

        test(true);
        if (((MyValue1)res).x != 42) {
            throw new RuntimeException("Test failed");
        }

        test(false);
        if (res != null) {
            throw new RuntimeException("Test failed");
        }
    }
}
