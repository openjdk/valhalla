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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.lang.runtime.WitnessSupport;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @test
 * @summary Test for witness lookup which violates type-variable bounds
 * @run junit/othervm BadWitnessLookupBoundsRuntime
 */
class BadWitnessLookupBoundsRuntime {
    interface Comparator<X> {
        int compare(X a, X b);

        __witness <Z extends Comparable<Z>> Comparator<Z> INT() {
            return (a, b) -> a.compareTo(b);
        }
    }

    interface Convertible<X, Y> {
        Y convertTo(X a);

        __witness <Z> Convertible<Z, Z> IDENTITY() {
            return a -> a;
        }
    }

    @Test
    public void testComparator() {
        assertThrows(IllegalArgumentException.class, lookup(type(Comparator.class, Runnable.class)));
        assertDoesNotThrow(lookup(type(Comparator.class, String.class)));
        assertDoesNotThrow(lookup(type(Comparator.class, Integer.class)));
    }

    @Test
    public void testConvertible() {
        assertThrows(IllegalArgumentException.class, lookup(type(Convertible.class, Integer.class, String.class)));
        assertDoesNotThrow(lookup(type(Convertible.class, String.class, String.class)));
        assertDoesNotThrow(lookup(type(Convertible.class, Integer.class, Integer.class)));
        assertThrows(IllegalArgumentException.class, lookup(type(Convertible.class, String.class, Integer.class)));
    }

    Executable lookup(Type type) {
        return () -> WitnessSupport.lookupWitness(MethodHandles.lookup(), type);
    }

    Type type(Class<?> baseType, Class<?>... typeArgs) {
        StringBuilder buf = new StringBuilder();
        String baseSig = baseType.describeConstable().get().descriptorString();
        buf.append(baseSig, 0, baseSig.length() - 1);
        if (typeArgs.length > 0) {
            buf.append("<");
            for (Class<?> targ : typeArgs) {
                buf.append(targ.describeConstable().get().descriptorString());
            }
            buf.append(">");
        }
        buf.append(";");
        return WitnessSupport.type(MethodHandles.lookup(), buf.toString());
    }
}
