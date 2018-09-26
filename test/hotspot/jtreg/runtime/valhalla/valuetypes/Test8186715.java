/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.valuetypes;

/*
 * @test Test8186715
 * @summary test return of buffered value passed in argument by caller
 * @library /test/lib
 * @compile -XDallowWithFieldOperator -XDenableValueTypes Test8186715.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.Test8186715
 * @run main/othervm -XX:+EnableValhalla runtime.valhalla.valuetypes.Test8186715
 */

public class Test8186715 {

    public static void main(String[] args) {
        MyValueType v = MyValueType.testDefault();

        for (int i = 0; i < 1000000; i++) {
            MyValueType.testBranchArg1(false, v);
        }
    }
}

value final class MyValueType {
    final int i;
    final int j;

    private MyValueType() {
        i = 0;
        j = 0;
    }

    static MyValueType testDefault() {
        return MyValueType.default;
    }

    static MyValueType testBranchArg1(boolean flag, MyValueType v1) {
        if (flag) {
            v1 = __WithField(v1.i, 3);
            v1 = __WithField(v1.j, 4);
        }
        return v1;
    }
}
