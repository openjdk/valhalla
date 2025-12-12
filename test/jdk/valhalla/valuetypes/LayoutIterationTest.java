/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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


import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.internal.value.LayoutIteration;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * @test
 * @summary test LayoutIteration
 * @enablePreview
 * @requires vm.flagless
 * @modules java.base/jdk.internal.vm.annotation
 *          java.base/jdk.internal.value
 * @run junit/othervm LayoutIterationTest
 */
class LayoutIterationTest {

    @LooselyConsistentValue
    static value class One {
        int a;
        short b;

        One(int a, short b) {
            this.a = a;
            this.b = b;
            super();
        }
    }

    @LooselyConsistentValue
    static value class Two {
        @NullRestricted
        One one = new One(5, (short) 3);
        One anotherOne = new One(4, (short) 2);
        long l = 5L;
    }

    @Test
    void testExample() {
        Two t = new Two();
        Set<Class<?>> classes = LayoutIteration.computeElementGetters(One.class).stream()
                .map(mh -> mh.type().returnType()).collect(Collectors.toSet());
        assertEquals(Set.of(int.class, short.class), classes);
        Map<Class<?>, Object> values = LayoutIteration.computeElementGetters(Two.class).stream()
                .collect(Collectors.toMap(mh -> mh.type().returnType(), mh -> {
                    try {
                        return (Object) mh.invoke(t);
                    } catch (Throwable ex) {
                        return Assertions.fail(ex);
                    }
                }));
        assertEquals(Map.of(
                int.class, t.one.a,
                short.class, t.one.b,
                One.class, t.anotherOne,
                long.class, t.l
        ), values);
    }

    static value class IntValue {
        int value;

        static final int[] EDGE_CASES = {
                0, -1, 1,
                Integer.MIN_VALUE, Integer.MAX_VALUE
        };

        public IntValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "IntValue(" + value +
                    ", bits=0x" + Integer.toHexString(value) + ")";
        }

        static boolean cmp(int i, int j) {
            return EDGE_CASES[i] == EDGE_CASES[j];
        }
    }

    static value class NestedValue {
        SubstitutabilityTest.IntValue value;

        static final SubstitutabilityTest.IntValue[] EDGE_CASES = {
                null, new SubstitutabilityTest.IntValue(0), new SubstitutabilityTest.IntValue(1), new SubstitutabilityTest.IntValue(2),
                new SubstitutabilityTest.IntValue(3), new SubstitutabilityTest.IntValue(0)
        };

        public NestedValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "NestedValue(" + value + ")";
        }

        static boolean cmp(int i, int j) {
            return EDGE_CASES[i] == EDGE_CASES[j];
        }
    }

    @Test
    void testNested() {
        NestedValue v = new NestedValue(0);
        Map<Class<?>, Object> values = LayoutIteration.computeElementGetters(NestedValue.class).stream()
                .collect(Collectors.toMap(mh -> mh.type().returnType(), mh -> {
                    try {
                        return (Object) mh.invoke(v);
                    } catch (Throwable ex) {
                        return Assertions.fail(ex);
                    }
                }));
        assertEquals(Map.of(
                int.class, 0,
                byte.class, (byte) 0 // null marker
        ), values);

        NestedValue v1 = new NestedValue(2);
        Map<Class<?>, Object> values1 = LayoutIteration.computeElementGetters(NestedValue.class).stream()
                .collect(Collectors.toMap(mh -> mh.type().returnType(), mh -> {
                    try {
                        return (Object) mh.invoke(v1);
                    } catch (Throwable ex) {
                        return Assertions.fail(ex);
                    }
                }));
        assertEquals(Map.of(
                int.class, NestedValue.EDGE_CASES[2].value,
                byte.class, (byte) 1 // null marker
        ), values1);
    }
}
