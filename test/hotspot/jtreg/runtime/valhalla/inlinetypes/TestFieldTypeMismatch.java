/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @library /test/lib
 * @compile TestFieldTypeMismatchHelper.jasm
 * @compile TestFieldTypeMismatch.java
 * @run main/othervm compiler.valhalla.inlinetypes.TestFieldTypeMismatch
 */

final class MyValue {
    int foo = 42;
}

public class TestFieldTypeMismatch {

    public static void main(String[] args) {
        boolean exception = false;
        try {
          TestFieldTypeMismatchHelper t = new TestFieldTypeMismatchHelper();
        } catch(IncompatibleClassChangeError err) {
            exception = true;
            Asserts.assertEquals(err.getMessage(),
                "Class compiler/valhalla/inlinetypes/TestFieldTypeMismatchHelper expects class compiler.valhalla.inlinetypes.MyValue to be an inline type, but it is not");
        }
        Asserts.assertTrue(exception);
    }
}

