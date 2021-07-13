/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng/othervm LambdaConversion
 * @summary test lambda type conversion of primitive class
 */

import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class LambdaConversion {

    static class c_int { }
    static primitive class Pointer<X> {
        final long addr;

        public Pointer(long addr) {
            this.addr = addr;
        }

        long address() { return addr; }
    }

    @Test
    public static void test() {
        Pointer<c_int> p_int = new Pointer<>(12);
        assertTrue(doAction(p_int, LambdaConversion::one) == 1);
        assertTrue(doAction(p_int, LambdaConversion::two) == 2);
    }

    static <Z> int doAction(Pointer<Z> pointer, ToIntFunction<Pointer.ref<Z>> action) {
        return action.applyAsInt(pointer);
    }

    static int one(Pointer<c_int> pointer) {
        return 1;
    }

    static int two(Pointer<c_int> p_int) {
        return 2;
    }
}

