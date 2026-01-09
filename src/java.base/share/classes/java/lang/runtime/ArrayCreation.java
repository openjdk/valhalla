/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.runtime;

import jdk.internal.javac.PreviewFeature;
import jdk.internal.javac.PreviewFeature.Feature;

import java.lang.reflect.Array;

/**
 * Bootstrap methods for strictly-initialized array creation.
 */
@PreviewFeature(reflective = true, feature = Feature.VALUE_OBJECTS)
public class ArrayCreation {

    private ArrayCreation() {}

    /**
     * {@return an empty array with provided type and flags}
     *
     * @param componentType The array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object empty(Class<?> componentType, int flags) {
        return copied(componentType, flags, 0, new Object[0]);
    }

    /**
     * {@return an array filled with the provided values}
     *
     * @param componentType The array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param v1            First value
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int flags, Object v1) {
        return copied(componentType, flags, 1, new Object[] { v1 });
    }

    /**
     * {@return an array filled with the provided values}
     *
     * @param componentType The array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param v1            First value
     * @param v2            Second value
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int flags, Object v1, Object v2) {
        return copied(componentType, flags, 2, new Object[] { v1, v2 });
    }

    /**
     * {@return an array filled with the provided values}
     *
     * @param componentType The array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param v1            First value
     * @param v2            Second value
     * @param v3            Third value
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int flags, Object v1, Object v2, Object v3) {
        return copied(componentType, flags, 3, new Object[] { v1, v2, v3 });
    }

    /**
     * {@return an array filled with the provided values}
     *
     * @param componentType The array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param v1            First value
     * @param v2            Second value
     * @param v3            Third value
     * @param v4            Fourth value
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int flags, Object v1, Object v2, Object v3, Object v4) {
        return copied(componentType, flags, 4, new Object[] { v1, v2, v3, v4 });
    }

    /**
     * {@return an array filled with the provided values}
     *
     * @param componentType The array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param v1            First value
     * @param v2            Second value
     * @param v3            Third value
     * @param v4            Fourth value
     * @param v5            Fifth value
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int flags, Object v1, Object v2, Object v3, Object v4, Object v5) {
        return copied(componentType, flags, 5, new Object[] { v1, v2, v3, v4, v5 });
    }

    /**
     * {@return an array filled with values copied from another
     * array}
     *
     * @param componentType The array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param len           Length of the array
     * @param arr           The array from which values are copied
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     * @throws NegativeArraySizeException if {@code length < 0}
     */
    public static Object copied(Class<?> componentType, int flags, int len, Object arr) {
        return Array.newInstance(componentType, flags, len, arr, 0);
    }
}
