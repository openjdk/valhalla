/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8264977
 * @summary A primitive class field by name val confuses javac
 * @run main ValRefTokensTest
 */

public primitive class ValRefTokensTest  {

    ValRefTokensTest.ref aa = null;
    static ValRefTokensTest.val bb = ValRefTokensTest.default;

    EmptyValue empty = EmptyValue.default;

    static primitive class ValRefTokensTestWrapper {
       ValRefTokensTest val = ValRefTokensTest.default;
       ValRefTokensTest ref = ValRefTokensTest.default;
    }

    public EmptyValue test139(int x) {
        ValRefTokensTestWrapper w = new ValRefTokensTestWrapper();
        return x == 0 ? w.val.empty : w.ref.empty;
    }

    int valx() {
        return EmptyValue.val.x;
    }

    int refx() {
        return EmptyValue.ref.x;
    }

    static primitive class EmptyValue {
        static int x = 42;
    }

    public static void main(String [] args) {
        if (new ValRefTokensTest().valx() != new ValRefTokensTest().refx())
            throw new AssertionError("Broken");
        if (new ValRefTokensTest().test139(0).x != new ValRefTokensTest().test139(1).x)
            throw new AssertionError("Broken");
    }
}
