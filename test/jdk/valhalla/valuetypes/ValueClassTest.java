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

/*
 * @test
 * @summary Test jdk.internal.value.ValueClass
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.value
 * @run junit ValueClassTest
 * @run junit/othervm --enable-preview ValueClassTest
 */

import java.util.ArrayList;

import jdk.internal.misc.PreviewFeatures;
import jdk.internal.value.ValueClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValueClassTest {
    @Test
    void testIsValueObjectCompatible() {
        isValueObjectCompatibleCase(false, int.class, "primitive");
        isValueObjectCompatibleCase(true, Object.class, "Object");
        isValueObjectCompatibleCase(true, Number.class, "abstract value class");
        isValueObjectCompatibleCase(true, Integer.class, "final value class");
        isValueObjectCompatibleCase(false, ClassValue.class, "abstract identity class");
        isValueObjectCompatibleCase(false, ArrayList.class, "identity class");
        isValueObjectCompatibleCase(false, String.class, "final identity class");
        isValueObjectCompatibleCase(true, Comparable.class, "interface");
        isValueObjectCompatibleCase(false, int[].class, "array class");
        isValueObjectCompatibleCase(false, Object[].class, "array class");
        isValueObjectCompatibleCase(false, Number[].class, "array class");
        isValueObjectCompatibleCase(false, Integer[].class, "array class");
        isValueObjectCompatibleCase(false, ClassValue[].class, "array class");
        isValueObjectCompatibleCase(false, ArrayList[].class, "array class");
        isValueObjectCompatibleCase(false, String[].class, "array class");
        isValueObjectCompatibleCase(false, Comparable[].class, "array class");
    }

    private static void isValueObjectCompatibleCase(boolean expected, Class<?> arg, String classification) {
        assertEquals(PreviewFeatures.isEnabled() && expected,
                     ValueClass.isValueObjectCompatible(arg),
                     () -> classification + ": " + arg.getTypeName());
    }

    @Test
    void testIsConcreteValueClass() {
        isConcreteValueClassCase(false, int.class, "primitive");
        isConcreteValueClassCase(false, Object.class, "Object");
        isConcreteValueClassCase(false, Number.class, "abstract value class");
        isConcreteValueClassCase(true, Integer.class, "final value class");
        isConcreteValueClassCase(false, ClassValue.class, "abstract identity class");
        isConcreteValueClassCase(false, ArrayList.class, "identity class");
        isConcreteValueClassCase(false, String.class, "final identity class");
        isConcreteValueClassCase(false, Comparable.class, "interface");
        isConcreteValueClassCase(false, int[].class, "array class");
        isConcreteValueClassCase(false, Object[].class, "array class");
        isConcreteValueClassCase(false, Number[].class, "array class");
        isConcreteValueClassCase(false, Integer[].class, "array class");
        isConcreteValueClassCase(false, ClassValue[].class, "array class");
        isConcreteValueClassCase(false, ArrayList[].class, "array class");
        isConcreteValueClassCase(false, String[].class, "array class");
        isConcreteValueClassCase(false, Comparable[].class, "array class");
    }

    private static void isConcreteValueClassCase(boolean expected, Class<?> arg, String classification) {
        assertEquals(PreviewFeatures.isEnabled() && expected,
                     ValueClass.isConcreteValueClass(arg),
                     () -> classification + ": " + arg.getTypeName());
    }
}
