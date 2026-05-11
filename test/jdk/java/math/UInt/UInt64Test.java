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
 * @bug 8383809
 * @run junit UInt64Test
 */

import org.junit.jupiter.api.Test;

import java.math.UInt64;

import static org.junit.jupiter.api.Assertions.*;

public class UInt64Test {

    @Test
    void testLen10() {
        for (int e = 0; e <= UInt64.DEC_SIZE; ++e) {
            long pow10 = UInt64.pow10(e);
            assertEquals(e, UInt64.len10(pow10 - 1));
            assertEquals(e + 1, UInt64.len10(pow10));
            assertEquals(e + 1, UInt64.len10(pow10 + 1));
        }
    }

}
