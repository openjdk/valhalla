/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @compile -XDenablePrimitiveClasses MHZeroValue.java
 * @run junit/othervm -XX:+EnableValhalla -XX:InlineFieldMaxFlatSize=128 MHZeroValue
 * @run junit/othervm -XX:+EnableValhalla -XX:InlineFieldMaxFlatSize=0 MHZeroValue
 * @summary Test MethodHandles::zero, MethodHandles::empty and MethodHandles::constant
 *          on value classes.
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.*;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestricted;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

public class MHZeroValue {
    @ImplicitlyConstructible
    static value class V {}

    @ImplicitlyConstructible
    static value class P {
        @NullRestricted
        V empty;
        P() {
            this.empty = new V();
        }
    }

    static Stream<Arguments> defaultValue() {
        return Stream.of(
                // for any type T, default value is always the same as (new T[1])[0]
                Arguments.of(int.class,         (new int[1])[0],      0 /* default value */),
                Arguments.of(Integer.class,     (new Integer[1])[0],  null),
                Arguments.of(P.class,           (new P[1])[0],        null),
                Arguments.of(V.class,           (new V[1])[0],        null)
        );
    }

    @ParameterizedTest
    @MethodSource("defaultValue")
    public void zero(Class<?> type, Object value, Object expected) throws Throwable {
        var mh = MethodHandles.zero(type);
        assertEquals(mh.invoke(), expected);
        assertEquals(value, expected);
    }

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(methodType(int.class, int.class, Object.class),     new V(), 0),
                Arguments.of(methodType(Integer.class, int.class, Object.class), new P(), null),
                Arguments.of(methodType(P.class, int.class, P.class),            new P(), null),
                Arguments.of(methodType(V.class, int.class, P.class),            new P(), null),
                Arguments.of(methodType(V.class, int.class, V.class),            new V(), null)
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void empty(MethodType mtype, Object param, Object value) throws Throwable {
        var mh = MethodHandles.empty(mtype);
        assertEquals(mh.invoke(1, param), value);
    }
}
