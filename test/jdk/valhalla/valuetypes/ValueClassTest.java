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
import org.junit.jupiter.api.Test;

import static jdk.internal.value.ValueClass.isValueObjectCompatible;
import static jdk.internal.value.ValueClass.isValueObjectInstance;
import static org.junit.jupiter.api.Assertions.*;

class ValueClassTest {
    @Test
    void testIsValueObjectCompatible() {
        assertFalse(isValueObjectCompatible(int.class), "primitive");
        assertEquals(PreviewFeatures.isEnabled(), isValueObjectCompatible(Object.class), "Object");
        assertEquals(PreviewFeatures.isEnabled(), isValueObjectCompatible(Number.class), "abstract value class");
        assertEquals(PreviewFeatures.isEnabled(), isValueObjectCompatible(Integer.class), "final value class");
        assertFalse(isValueObjectCompatible(ClassValue.class), "abstract identity class");
        assertFalse(isValueObjectCompatible(ArrayList.class), "identity class");
        assertFalse(isValueObjectCompatible(String.class), "final identity class");
        assertEquals(PreviewFeatures.isEnabled(), isValueObjectCompatible(Comparable.class), "interface");
        assertFalse(isValueObjectCompatible(int[].class), "array class");
        assertFalse(isValueObjectCompatible(Object[].class), "array class");
        assertFalse(isValueObjectCompatible(Number[].class), "array class");
        assertFalse(isValueObjectCompatible(Integer[].class), "array class");
        assertFalse(isValueObjectCompatible(ClassValue[].class), "array class");
        assertFalse(isValueObjectCompatible(ArrayList[].class), "array class");
        assertFalse(isValueObjectCompatible(String[].class), "array class");
        assertFalse(isValueObjectCompatible(Comparable[].class), "array class");
    }

    @Test
    void testIsValueObjectInstance() {
        assertFalse(isValueObjectInstance(int.class), "primitive");
        assertFalse(isValueObjectInstance(Object.class), "Object");
        assertFalse(isValueObjectInstance(Number.class), "abstract value class");
        assertEquals(PreviewFeatures.isEnabled(), isValueObjectInstance(Integer.class), "final value class");
        assertFalse(isValueObjectInstance(ClassValue.class), "abstract identity class");
        assertFalse(isValueObjectInstance(ArrayList.class), "identity class");
        assertFalse(isValueObjectInstance(String.class), "final identity class");
        assertFalse(isValueObjectInstance(Comparable.class), "interface");
        assertFalse(isValueObjectInstance(int[].class), "array class");
        assertFalse(isValueObjectInstance(Object[].class), "array class");
        assertFalse(isValueObjectInstance(Number[].class), "array class");
        assertFalse(isValueObjectInstance(Integer[].class), "array class");
        assertFalse(isValueObjectInstance(ClassValue[].class), "array class");
        assertFalse(isValueObjectInstance(ArrayList[].class), "array class");
        assertFalse(isValueObjectInstance(String[].class), "array class");
        assertFalse(isValueObjectInstance(Comparable[].class), "array class");
    }
}
