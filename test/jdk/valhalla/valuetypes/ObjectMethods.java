/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @ignore 8205549
 * @summary test hashCode/equals/toString on value types
 * @run testng ObjectMethods
 */


import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ObjectMethods {
    @DataProvider(name="dataForHashes")
    Object[][] dataForHashes() {
        return new Object[][] {
            { Point.makePoint(100, 200), hash(Point.class, 100, 200) },
            { Line.makeLine(1, 2, 3, 4), hash(Line.class, 1, 2, 3, 4)}
        };
    }

    @DataProvider(name="dataForEquals")
    Object[][] dataForEquals() {
        return new Object[][] {
            { Point.makePoint(100, 200), Point.makePoint(200, 200) },
            { Line.makeLine(1, 2, 3, 4), Line.makeLine(2, 2, 4, 4)}
        };
    }

    @DataProvider(name="dataFortoString")
    Object[][] dataFortoString() {
        return new Object[][] {
            { Point.makePoint(100, 200), "[value class Point, 100, 200]" },
            { Line.makeLine(1, 2, 3, 4), "[value class Line, 1, 2, 3, 4]"}
        };
    }

    @Test(dataProvider="dataForHashes")
    public void testHashCode(Object o, int hash) {
        assertTrue(o.hashCode() == hash);
    }

    @Test(dataProvider="dataForEquals")
    public void testEquals(Object o1, Object o2) {
        assertTrue(o1.equals(o1));
        assertFalse(o1.equals(o2));
    }

    @Test(dataProvider="dataFortoString")
    public void testToString(Object o, String s) {
        assertTrue(o.toString().equals(s));
    }

    static int hash(Object... values) {
        int hc = 1;
        for (Object o : values) {
            hc = 31 * hc + o.hashCode();
        }
        return hc;
    }

}
