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

/*
 * @test
 * @summary Basic test for Array::get, Array::set, Arrays::setAll on inline class array
 * @compile -XDallowGenericsOverValues StreamTest.java
 * @run testng StreamTest
 */

import java.util.Arrays;
import java.util.stream.*;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class StreamTest {
    final Value[] values = init();
    private Value[] init() {
        Value[] values = new Value[10];
        for (int i = 0; i < 10; i++) {
            values[i] = new Value(i, new Point(i,i*2), (i%2) == 0 ? null : new Point(i*10, i*20));
        }
        return values;
    }

    @Test
    public void testPrimitive() {
        Arrays.stream(values)
              .filter(v -> (v.i % 2) == 0)
              .forEach(System.out::println);
    }

    @Test
    public void testInlineType() {
        Arrays.stream(values)
                .map(Value::point)
                .filter(p -> p.x >= 5)
                .forEach(System.out::println);

        Arrays.stream(values)
                .map(Value::nullablePoint)
                .filter(p -> p != null)
                .forEach(System.out::println);
    }

    static inline class Value {
        int i;
        Point p;
        Point? nullable;
        Value(int i, Point p, Point? np) {
            this.i = i;
            this.p = p;
            this.nullable = np;
        }

        Point point() {
            return p;
        }

        Point? nullablePoint() {
            return nullable;
        }
    }
}
