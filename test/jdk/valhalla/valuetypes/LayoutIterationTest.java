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
    void test() {
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
}
