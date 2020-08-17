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
 * @run main/othervm -Dvalue.bsm.salt=1 ValueBootstrapMethodsTest
 */

import java.util.List;
import java.util.Objects;

public class ValueBootstrapMethodsTest {

    public static final inline class Value {
        private final int i;
        private final double d;
        private final String s;
        private final List<String> l;
        Value(int i, double d, String s, String... items) {
            this.i = i;
            this.d = d;
            this.s = s;
            this.l = List.of(items);
        }

        private List<Object> values() {
            return List.of(Value.class, i, d, s, l);
        }

        public int localHashCode() {
            return values().hashCode();
        }

        public String localToString() {
            System.out.println(l);
            return String.format("[%s i=%s d=%s s=%s l=%s]", Value.class.getName(),
                                 i, String.valueOf(d), s, l.toString());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Value) {
                Value v = (Value)obj;
                return this.i == v.i && this.d == v.d &&
                        Objects.equals(this.s, v.s) &&
                        Objects.equals(this.l, this.l);
            }
            return false;
        }
    }

    private static void assertEquals(Object o1, Object expected) {
        if (!Objects.equals(o1, expected)) {
            throw new RuntimeException(o1 + " expected: " + expected);
        }
    }

    public static void main(String... args) throws Throwable {

        Value value = new Value(10, 5.03, "foo", "bar", "goo");

        assertEquals(value.localHashCode(), value.hashCode());
        assertEquals(value.localToString(), value.toString());

        // verify ifacmp and the overridden equals method

        // same instance
        if (value != value || !value.equals(value)) {
            throw new RuntimeException("expected == and equals");
        }

        // value and v2 are of different values
        Value v2 = new Value(20, 5.03, "foo", "bar", "goo");
        if (value == v2 || value.equals(v2)) {
            throw new RuntimeException("expected != and unequals");
        }

        // v2 and v3 are of different values but Value::equals
        // returns true because v2::l and v3::l field contain the same elements
        Value v3 = new Value(20, 5.03, "foo", "bar", "goo");
        if (v2 == v3 || !v2.equals(v3)) {
            throw new RuntimeException("expected != and equals");
        }
    }
}
