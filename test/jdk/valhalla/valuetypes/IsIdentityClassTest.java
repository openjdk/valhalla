/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that IsIdentityClass and modifiers return true for flattened arrays.
 * @library /test/lib
 * @enablePreview
 * @run main/othervm -XX:-UseArrayFlattening -XX:-UseNullableValueFlattening IsIdentityClassTest
 * @run main/othervm -XX:+UseArrayFlattening -XX:+UseNullableValueFlattening IsIdentityClassTest
 */

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import jdk.internal.value.ValueClass;

import static jdk.test.lib.Asserts.*;

public class IsIdentityClassTest {

    static boolean UseArrayFlattening = false;
    static {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        UseArrayFlattening = !arguments.contains("-XX:-UseArrayFlattening");
        System.out.println("UseArrayFlattening: " + UseArrayFlattening);
    }

    static void testIsIdentityClass() {
        Integer[] array0 = new Integer[200];
        if (UseArrayFlattening) {
            // NYI assertTrue(ValueClass.isFlatArray(array0));
        } else {
            assertFalse(ValueClass.isFlatArray(array0));
        }
        assertFalse(Integer.class.isIdentity(), "Integer is not an IDENTITY type");
        assertTrue(Integer[].class.isIdentity(), "Arrays of inline types are IDENTITY types");
    }

    static void testModifiers() {
        int imod = Integer.class.getModifiers();
        assertFalse(Modifier.isIdentity(imod), "Modifier of Integer should not have IDENTITY set");
        int amod = Integer[].class.getModifiers();
        assertTrue(Modifier.isIdentity(amod), "Modifier of array of inline types should have IDENTITY set");
    }

    static void testAccessFlags() {
        Set<AccessFlag> iacc = Integer.class.accessFlags();
        assertFalse(iacc.contains(Modifier.IDENTITY), "Access flags should not contain IDENTITY");
        Set<AccessFlag> aacc = Integer[].class.accessFlags();
        assertFalse(aacc.contains(Modifier.IDENTITY), "Access flags of array of inline types should contain IDENTITY");
    }

    public static void main(String[] args) throws Exception {
        testIsIdentityClass();
        testModifiers();
        testAccessFlags();
    }
}
