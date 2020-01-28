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

/**
 * @test
 * @bug 8237955
 * @summary Identity types that have no declaration sites fail to be IdentityObjects
 * @run main/othervm InstanceOfTopTypeTest
 */

public class InstanceOfTopTypeTest {
    static inline class V {
        int x = 42;
    }
    public static void main(String [] args) {
        int points = 0;
        Object o = new InstanceOfTopTypeTest();
        if (o instanceof IdentityObject)
            points++;     // 1
        if (o instanceof InlineObject)
            throw new AssertionError("Broken");
        o = new V();
        if (o instanceof IdentityObject)
            throw new AssertionError("Broken");
        if (o instanceof InlineObject)
            points++; // 2
        Object [] oa = new InstanceOfTopTypeTest[] { new InstanceOfTopTypeTest() };
        if (oa instanceof IdentityObject)
            points++; // 3
        if (oa[0] instanceof IdentityObject)
            points++; // 4
        if (oa[0] instanceof InlineObject)
            throw new AssertionError("Broken");
        oa = new V[] { new V() };
        if (oa instanceof IdentityObject)
            points++; // 5
        if (oa[0] instanceof IdentityObject)
            throw new AssertionError("Broken");
        if (oa[0] instanceof InlineObject)
            points++;
        if (points != 4) // Change to != 6 after JDK-8237958 is fixed
            throw new AssertionError("Broken top type set up" + points);
    }
}
