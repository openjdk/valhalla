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

import java.lang.invoke.MethodHandles;
import java.lang.runtime.WitnessSupport;
import java.util.List;

/*
 * @test
 * @summary Smoke test for subtyping checks in witness lookups
 */
public class TypeClassesSubTypingRuntimeTest {
    interface A<X> {
        String a();
    }
    interface B<X> extends A<X> { }

    interface C<W> { String a(); }
    interface D<X> extends C<X> { }

    static class E {
        __witness B<E> BE = () -> "BE";
        __witness D<E> DE(A<E> az) {
            return () -> "DE";
        }
    }

    public static void main(String[] args) {
        for (String name : List.of("A", "B", "C", "D")) {
            var typeName = String.format("LTypeClassesSubTypingRuntimeTest$%s<LTypeClassesSubTypingRuntimeTest$E;>;", name);
            var lookup = MethodHandles.lookup();
            var type = WitnessSupport.type(lookup, typeName);
            try {
                WitnessSupport.lookupWitness(lookup, type);
                System.out.println("Success: " + name);
                if (!name.equals("B")) {
                    throw new AssertionError("Expected failure: " + name);
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Fail: " + name);
                if (name.equals("B")) {
                    throw new AssertionError("Expected success: " + name);
                }
            }
        }
    }
}
