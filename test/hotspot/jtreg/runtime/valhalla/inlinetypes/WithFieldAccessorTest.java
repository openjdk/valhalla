/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

/**
 * @test
 * @bug 8210351
 * @summary test nestmate access to an inline type's public, protected and private final fields.
 * @library /test/lib
 * @build org.openjdk.asmtools.* org.openjdk.asmtools.jasm.*
 * @run driver org.openjdk.asmtools.JtregDriver jasm -strict WithFieldAccessorTestClasses.jasm
 * @run main/othervm runtime.valhalla.inlinetypes.WithFieldAccessorTest
 */

public class WithFieldAccessorTest {

    public static void main(String... args) {
        WithFieldOwner start = WithFieldOwner.default;
        WithFieldOwner x = start;
        x.checkFields((char) 0, 0, 0, 0);

        x = WithFieldOwner.withC(start, 'a');
        x = WithFieldOwner.withL(x, 1);
        x = WithFieldOwner.withD(x, 2);
        x = WithFieldOwner.withI(x, 3);
        x.checkFields('a', 1, 2, 3);

        x = WithFieldNestHost.withC(start, 'b');
        x = WithFieldNestHost.withL(x, 4);
        x = WithFieldNestHost.withD(x, 5);
        x = WithFieldNestHost.withI(x, 6);
        x.checkFields('b', 4, 5, 6);

        x = WithFieldNestmate.withC(start, 'c');
        x = WithFieldNestmate.withL(x, 7);
        x = WithFieldNestmate.withD(x, 8);
        x = WithFieldNestmate.withI(x, 9);
        x.checkFields('c', 7, 8, 9);

        catchAccessError(() -> WithFieldSamePackage.withC(start, 'd'));
        catchAccessError(() -> WithFieldSamePackage.withL(start, 10));
        catchAccessError(() -> WithFieldSamePackage.withD(start, 11));
        catchAccessError(() -> WithFieldSamePackage.withI(start, 12));
    }

    static void catchAccessError(Runnable r) {
        try {
            r.run();
            Asserts.fail("access violation not caught");
        }
        catch (IllegalAccessError e)  { /* expected */ }
    }

}
