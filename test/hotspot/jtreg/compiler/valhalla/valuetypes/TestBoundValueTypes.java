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


import java.lang.invoke.*;

import jdk.experimental.value.MethodHandleBuilder;
import jdk.incubator.mvt.ValueType;

/**
 * @test
 * @bug 8185339
 * @summary Test correct compilation of MethodHandles with bound value type arguments.
 * @modules java.base/jdk.experimental.value
 *          jdk.incubator.mvt
 * @compile -XDenableValueTypes ValueCapableClass2.java TestBoundValueTypes.java
 * @run main/othervm -XX:+EnableMVT -XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.TestBoundValueTypes::*
 *                   -XX:CompileCommand=compileonly,java.lang.invoke.*::* compiler.valhalla.valuetypes.TestBoundValueTypes
 */
public class TestBoundValueTypes {
    static final Object vcc = ValueCapableClass2.create(42L);

    static final MethodHandle mh;
    static {
        final MethodHandle getU = MethodHandleBuilder.loadCode(MethodHandles.lookup(), "getU",
                                        MethodType.methodType(long.class, ValueType.forClass(ValueCapableClass2.class).valueClass()),
                                            CODE -> {
                                                CODE.
                                                vload(0).
                                                vbox(ValueCapableClass2.class).
                                                vunbox(ValueType.forClass(ValueCapableClass2.class).valueClass()).
                                                getfield(ValueType.forClass(ValueCapableClass2.class).valueClass(), "u", "J").
                                                lreturn();
                                            });
        // Bind the value type argument to 'vcc'
        mh = MethodHandles.insertArguments(getU, 0, vcc);
    }

    long test() throws Throwable {
        return (long)mh.invoke();
    }

    public static void main(String[] args) throws Throwable {
        TestBoundValueTypes t = new TestBoundValueTypes();
        for (int i = 0; i < 100_000; ++i) {
            if (t.test() != 42) {
                throw new RuntimeException("Test failed");
            }
        }
    }
}
