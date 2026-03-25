/*
 * Copyright (c) 2003, 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4822887
 * @summary Basic test for Collections.addAll
 * @author  Josh Bloch
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.valueclass.ValueClass
 *
 * @compile AddAll.java
 * @run main AddAll
 *
 * @compile -XDaccessInternalAPI --enable-preview --source ${java.specification.version} -Xplugin:ValueClassPlugin AddAll.java
 * @run main/othervm --enable-preview AddAll
 */

import jdk.test.lib.valueclass.ValueClass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class AddAll {
    static final int N = 100;
    public static void main(String[] args) {
        test(new ArrayList<Integer>());
        test(new LinkedList<Integer>());
        test(new HashSet<Integer>());
        test(new LinkedHashSet<Integer>());
        checkValueClass();
        testPoint(new ArrayList<Point>());
    }

    private static Random rnd = new Random();

    @ValueClass
    static class Point {
        int x;
        int y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    static void test(Collection<Integer> c) {
        int x = 0;
        for (int i = 0; i < N; i++) {
            int rangeLen = rnd.nextInt(10);
            if (Collections.addAll(c, range(x, x + rangeLen)) !=
                    (rangeLen != 0))
                throw new RuntimeException("" + rangeLen);
            x += rangeLen;
        }
        if (c instanceof List) {
            if (!c.equals(Arrays.asList(range(0, x))))
                throw new RuntimeException(x +": "+c);
        } else {
            if (!c.equals(new HashSet<Integer>(Arrays.asList(range(0, x)))))
                throw new RuntimeException(x +": "+c);
        }
    }

    private static Integer[] range(int from, int to) {
        Integer[] result = new Integer[to - from];
        for (int i = from, j=0; i < to; i++, j++)
            result[j] = new Integer(i);
        return result;
    }

    static void checkValueClass() {
        boolean transformed = !Point.class.isIdentity();

        try (var is = Point.class.getResourceAsStream("AddAll$Point.class")) {
            if (is == null) {
                throw new RuntimeException("Cannot read Point.class");
            }

            byte[] header = is.readNBytes(8);
            boolean pointClassUsesPreview =
                    (header[4] & 0xFF) == 0xFF &&
                    (header[5] & 0xFF) == 0xFF;

            if (pointClassUsesPreview && !transformed) {
                throw new RuntimeException("ValueClassPlugin did not transform Point");
            }
            if (!pointClassUsesPreview && transformed) {
                throw new RuntimeException("Point is a value class but Point.class is not preview-marked");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Point.class", e);
        }
    }

    static void testPoint(Collection<Point> c) {
        int x = 0;
        for (int i = 0; i < N; i++) {
            int rangeLen = rnd.nextInt(10);
            if (Collections.addAll(c, rangePoint(x, x + rangeLen)) !=
                    (rangeLen != 0))
                throw new RuntimeException("" + rangeLen);
            x += rangeLen;
        }
    }

    private static Point[] rangePoint(int from, int to) {
        Point[] result = new Point[to - from];
        for (int i = from, j = 0; i < to; i++, j++)
            result[j] = new Point(i, i);
        return result;
    }
}
