/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4394937 8051382 8281463
 * @summary tests the toString method of reflect.Modifier
 * @run testng toStringTest
 */

import java.lang.reflect.Modifier;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

public class toStringTest {

    static final int allMods = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
            Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL |
            Modifier.TRANSIENT | Modifier.VOLATILE | Modifier.SYNCHRONIZED |
            Modifier.NATIVE | Modifier.VALUE  | Modifier.PERMITS_VALUE | Modifier.PRIMITIVE | Modifier.INTERFACE;
    String allModsString = "public protected private abstract static " +
            "final transient volatile synchronized native primitive interface";

    static final int  primitiveMods = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
            Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL |
            Modifier.TRANSIENT | Modifier.VOLATILE | Modifier.SYNCHRONIZED |
            Modifier.NATIVE | Modifier.VALUE  | Modifier.PERMITS_VALUE | Modifier.PRIMITIVE | Modifier.INTERFACE;
    String primitiveModsString = "public protected private abstract static " +
            "final transient volatile synchronized native primitive interface";

    static final int  valueMods = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
            Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL |
            Modifier.TRANSIENT | Modifier.VOLATILE | Modifier.SYNCHRONIZED |
            Modifier.NATIVE | Modifier.VALUE  | Modifier.PERMITS_VALUE | Modifier.INTERFACE;
    String valueModsString = "public protected private abstract static " +
            "final transient volatile synchronized native value interface";

    @DataProvider(name="Modifiers")
    Object[][] modifiers() {
        return new Object[][] {
                {0, ""},
                {~0, allModsString},
                {allMods, allModsString},
                {primitiveMods, primitiveModsString},
                {valueMods, valueModsString},
        };
    }

    @Test(dataProvider = "Modifiers")
    void testString(int test, String expected) {
        String actual = Modifier.toString(test);
        Assert.assertEquals(actual, expected, "incorrect toString");
    }
}
