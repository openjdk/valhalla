/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.runtime.WitnessSupport;
import java.util.function.Supplier;

/*
 * @test
 * @summary negative test for bad dynamic witness lookup
 */
public class BadWitnessDynamicLookup<Z> {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Expected {
        String value();
    }

    interface Foo<X> { }

    interface Bar { }

    @Expected("Unsupported toplevel wildcard")
    Foo<? extends Bar> f1 = null; // error, captured type-variable
    @Expected("Unsupported type variable")
    Foo<Z> f2 = null; // error, declared type-variable
    @Expected("Witness is not a generic type")
    Bar f3 = null; // error, not generic

    public static void main(String[] args) {
        for (Field f : BadWitnessDynamicLookup.class.getDeclaredFields()) {
            Expected expected = f.getAnnotation(Expected.class);
            assertThrows(() -> WitnessSupport.lookupWitness(MethodHandles.lookup(), f.getGenericType()), IllegalArgumentException.class, expected);
        }
    }

    static <T> void assertThrows(Supplier<T> supplier, Class<? extends Throwable> exceptionClass, Expected expected) {
        try {
            supplier.get();
            throw new AssertionError("Expected exception not thrown");
        } catch (Throwable ex) {
            if (!ex.getClass().equals(exceptionClass)) {
                throw new AssertionError("Unexpected exception thrown: " + ex.getClass().getName());
            } else if (!ex.getMessage().contains(expected.value())) {
                throw new AssertionError("Unexpected exception message: " + ex.getMessage());
            }
        }
    }
}
