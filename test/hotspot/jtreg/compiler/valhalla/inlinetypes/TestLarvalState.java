/*
 * Copyright (c) 2023, Arm Limited. All rights reserved.
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8303416
 * @summary Fix JVM crash at Unsafe_FinishPrivateBuffer
 * @library /test/lib
 * @compile -XDenablePrimitiveClasses
 *          --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *          TestLarvalState.java
 * @run main/othervm -XX:+EnableValhalla -XX:+EnablePrimitiveClasses
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   compiler.valhalla.inlinetypes.TestLarvalState
 */

package compiler.valhalla.inlinetypes;

import java.lang.reflect.*;
import java.util.Random;

import jdk.internal.misc.Unsafe;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

public class TestLarvalState {
    private static int LENGTH = 10000;

    private static final Random RD = Utils.getRandomInstance();

    static byte[] arr = new byte[LENGTH];

    static {
        for (int i = 0; i < LENGTH; i++) {
            arr[i] = (byte) RD.nextInt(127);
        }
    }

    public static byte test(byte b) {
        Value obj = new Value();
        obj = Unsafe.getUnsafe().makePrivateBuffer(obj);
        Unsafe.getUnsafe().putByte(obj, obj.offset, b);
        obj = Unsafe.getUnsafe().finishPrivateBuffer(obj);
        return Unsafe.getUnsafe().getByte(obj, obj.offset);
    }

    public static void main(String[] args) {
        byte actual = 0;
        for (int i = 0; i < LENGTH; i++) {
            actual += test(arr[i]);
        }

        byte expected = 0;
        for (int i = 0; i < LENGTH; i++) {
            expected += arr[i];
        }
        Asserts.assertEquals(expected, actual);
    }

    primitive static class Value {
        byte field = 0;

        static long offset = fieldOffset();

        private static long fieldOffset() {
            try {
                var f = Value.class.getDeclaredField("field");
                return Unsafe.getUnsafe().objectFieldOffset(f);
            } catch (Exception e) {
                System.out.println(e);
            }
            return -1L;
        }
    }
}

