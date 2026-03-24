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

package compiler.valhalla.inlinetypes;

import jdk.test.lib.Asserts;

/*
 * @test
 * @bug 8380434
 * @summary TODO
 * @library /test/lib /
 * @enablePreview
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *                   -Xbatch -XX:-TieredCompilation
 *                   -XX:PerMethodSpecTrapLimit=0 -XX:PerMethodTrapLimit=0
 *                   -XX:CompileCommand=compileonly,${test.main.class}::test
 *                   ${test.main.class}
 * @run main ${test.main.class}
 */

value class MyValueRetType {
    int i1 = 42;
    int i2 = 43;
    int i3 = 44;
    int i4 = 45;
    int i5 = 46;
    int i6 = 47;
    int i7 = 48;
}

class A {
    public MyValueRetType virtual(boolean cond) {
        return new MyValueRetType();
    }
}

class B extends A {
    @Override
    public MyValueRetType virtual(boolean cond) {
        return new MyValueRetType();
    }
}

public class TestMakeFromOopReturnValue {
    public static void test() {
        new B();
        A a = new A();

        // We want an OSR compilation here
        for (int i = 0; i < 100_000; i++) {
            Asserts.assertEquals(a.virtual(true), new MyValueRetType());
        }
    }

    public static void main(String[] args) {
        test();
    }
}