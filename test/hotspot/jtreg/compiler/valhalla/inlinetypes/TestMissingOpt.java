/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary TODO
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main/othervm -Xbatch -Xcomp -XX:-TieredCompilation -XX:CompileCommand=compileonly,*TestMissingOpt::test
 *      -XX:VerifyIterativeGVN=1110
 *      compiler.valhalla.inlinetypes.TestMissingOpt
 */

public class TestMissingOpt {
    static class MyClass {}

    static value class MyValue {
        MyClass obj;

        public MyValue(MyClass obj) {
            this.obj = obj;
        }
    }

    static MyValue field;

    public static void test() {
        field = new MyValue(null);
    }

    public static void main(String[] args) {
        // new MyClass();
        new MyValue(null);
        test();
    }
}

