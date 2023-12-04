/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes;

import jdk.internal.misc.VM;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;

/*
 * @test Test8186715
 * @summary test return of buffered inline type passed in argument by caller
 * @library /test/lib
 * @compile --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED Test8186715.java
 * @run main/othervm -XX:+EnableValhalla --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED runtime.valhalla.inlinetypes.Test8186715
 */

public class Test8186715 {

    public static void main(String[] args) {
        MyValueType v = MyValueType.testDefault();

        for (int i = 0; i < 1000000; i++) {
            MyValueType.testBranchArg1(false, v);
        }
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValueType {
    final int i;
    final int j;

    private MyValueType(int i, int j) {
        this.i = i;
        this.j = j;
    }

    static MyValueType testDefault() {
        MyValueType[] array = (MyValueType[])VM.newNullRestrictedArray(MyValueType.class, 1);
        return array[0];
    }

    static MyValueType testBranchArg1(boolean flag, MyValueType v1) {
        if (flag) {
            v1 = new MyValueType(3, 4);
        }
        return v1;
    }
}
