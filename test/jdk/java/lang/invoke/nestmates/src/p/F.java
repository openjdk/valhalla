/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import static java.lang.invoke.MethodType.methodType;

public class F extends ThreadLocal {
    private String name() {
        return "F";
    }

    public static class Inner {
        public static void test() throws Throwable {
            // Inner is a nestmate of F
            assertTrue(Inner.class.isNestmateOf(F.class));

            F f = new F();
            // Inner is a nestmate of F and this lookup can find private member of F
            Lookup lookup = MethodHandles.lookup();
            MethodHandle mh1 = lookup.findVirtual(F.class, "name", methodType(String.class));
            String n = (String)mh1.invokeExact(f);
            assertTrue(n.equals("F"));

            // Lookup::in teleporting to a class member and give full-power lookup
            // calling findSpecial: lookupClass must be identical to specialCaller i.e. F
            lookup = lookup.in(F.class);
            assertTrue(lookup.hasPrivateAccess());
            MethodHandle mh2 = lookup.findSpecial(ThreadLocal.class, "initialValue",
                                                  methodType(Object.class),
                                                  F.class);
            System.out.println("invoking protected ThreadLocal::initialValue");
            Object o = mh2.invokeExact(f);
        }
    }

    public static void test() throws Throwable {
        Inner.test();
    }

    static void assertTrue(boolean v) {
        if (!v) {
            throw new AssertionError("expected true but got " + v);
        }
    }

    public static void main(String... args) throws Throwable {
        test();
    }
}
