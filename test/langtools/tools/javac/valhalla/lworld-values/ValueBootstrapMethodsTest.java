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
 * @summary test value bootstrap methods
 * @run main/othervm -XX:+EnableValhalla ValueBootstrapMethodsTest
 */

import java.util.List;
import java.util.Objects;

public class ValueBootstrapMethodsTest {

    public static final __ByValue class Value {
        private final int i;
        private final double d;
        private final String s;
        private final List<String> l;
        Value() {
            this.i = 0;
            this.d = 0;
            this.s = "default";
            this.l = List.of();
        }
        public static Value make(int i, double d, String s, String... items) {
            Value v = Value.default;
            v = __WithField(v.i, i);
            v = __WithField(v.d, d);
            v = __WithField(v.s, s);
            v = __WithField(v.l, List.of(items));
            return v;
        }

        private List<Object> values() {
            return List.of(Value.class, i, d, s, l);
        }

        public int localHashCode() {
            return values().hashCode();
        }

        public long localLongHashCode() {
            long hash = 1;
            for (Object o : values()) {
                hash = 31 * hash + o.hashCode();
            }
            return hash;
        }

        public String localToString() {
            System.out.println(l);
            return String.format("[value %s, %s, %s, %s, %s]", Value.class,
                                 i, String.valueOf(d), s, l.toString());
        }
    }

    private static void assertEquals(Object o1, Object expected) {
        if (!Objects.equals(o1, expected)) {
            throw new RuntimeException(o1 + " expected: " + expected);
        }
    }

    public static void main(String... args) throws Throwable {

        Value value = Value.make(10, 5.03, "foo", "bar", "goo");

        assertEquals(value.localHashCode(), value.hashCode());
        assertEquals(value.localLongHashCode(), value.longHashCode());
        assertEquals(value.localToString(), value.toString());

        if (!value.equals(value)) {
            throw new RuntimeException("expected equals");
        }

        Value v2 = Value.make(20, 5.03, "foo", "bar", "goo");
        if (value.equals(v2)) {
            throw new RuntimeException("expected unequals");
        }

        Value v3 = Value.make(20, 5.03, "foo", "bar", "goo");
        if (!v2.equals(v3)) {
            throw new RuntimeException("expected equals");
        }
    }
}
