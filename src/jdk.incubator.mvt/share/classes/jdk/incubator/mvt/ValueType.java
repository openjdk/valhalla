/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.incubator.mvt;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

import valhalla.shady.MinimalValueTypes_1_0;
import valhalla.shady.ValueTypeHolder;

/**
 * Value type reflection support.
 */
public class ValueType<T> {
    /**
     * Returns a {@code ValueType} object representing the specified
     * value-capable class.
     *
     * @param vcc Value-capable class
     * @param <T> Value type
     * @return a {@code ValueType} object representing the specified
     *         value-capable class.
     *
     * @throws IllegalArgumentException if the specified {@code vcc}
     *         is not a value-capable class.
     */
    public static <T> ValueType<T> forClass(Class<T> vcc) {
        return new ValueType<>(MinimalValueTypes_1_0.getValueFor(vcc));
    }

    /**
     * Returns {@code true} if the specified class is value-capable class.
     *
     * @param c a Class
     * @return true if the specified class is a value-capable class.
     */
    public static boolean classHasValueType(Class<?> c) {
        if (!MinimalValueTypes_1_0.isValueCapable(c)) {
            return false;
        }
        return MinimalValueTypes_1_0.getValueTypeClass(c) != null;
    }

    private final ValueTypeHolder<T> valueTypeHolder;
    private ValueType(ValueTypeHolder<T> vt) {
        this.valueTypeHolder = vt;
    }

    /**
     * Returns the value-capable class of this value type.
     *
     * @return the value-capable class of this value type.
     */
    public Class<T> boxClass() {
        return valueTypeHolder.boxClass();
    }

    /**
     * Returns the derived value type of this value type.
     *
     * @return the derived value type of this value type.
     */
    public Class<?> valueClass() {
        return valueTypeHolder.valueClass();
    }

    /**
     * Returns an array class of this value type.
     *
     * @return an array class of this value type.
     */
    public Class<?> arrayValueClass() {
        return arrayValueClass(1);
    }

    /**
     * Returns an array class of the specified dimension for this value type.
     *
     * @return an array class of the specified dimension for this value type.
     */
    public Class<?> arrayValueClass(int dims) {
        String dimsStr = "[[[[[[[[[[[[[[[[";
        if (dims < 1 || dims > 16) {
            throw new IllegalArgumentException("cannot create array class for dimension > 16");
        }
        String cn = dimsStr.substring(0, dims) + "Q" + valueClass().getName() + ";";
        return MinimalValueTypes_1_0.loadValueTypeClass(boxClass(), cn);
    }

    /**
     * Returns a string representation of this value type.
     *
     * @return a string representation of this value type.
     */
    public String toString() {
        return valueTypeHolder.toString();
    }

    public static <T> ValueType<T> make(Lookup lookup,
                                        String name,
                                        String[] fieldNames,
                                        Class<?>... fieldTypes)
        throws ReflectiveOperationException
    {
        if (fieldNames.length != fieldTypes.length) {
            throw new IllegalArgumentException("Field names length and field types length must match");
        }
        if (!(fieldNames.length > 0)) {
            throw new IllegalArgumentException("Field length must be greater than zero");
        }
        Class<T> vtClass = ValueTypeHolder.makeValueTypeClass(lookup, name, fieldNames, fieldTypes);
        return forClass(vtClass);
    }

    // ()Q
    public MethodHandle findConstructor(Lookup lookup, MethodType type)
        throws NoSuchMethodException, IllegalAccessException {
        return valueTypeHolder.findConstructor(lookup, type);
    }

    // (F1, ..., Fn)Q, fromDefault == true
    // (Q, F1, ..., Fn)Q, fromDefault == false
    public MethodHandle unreflectWithers(Lookup lookup,
                                         boolean fromDefault,
                                         Field... fields)
        throws NoSuchFieldException, IllegalAccessException {
        return valueTypeHolder.unreflectWithers(lookup, fromDefault, fields);
    }

    // (Q, T)Q
    public MethodHandle findWither(Lookup lookup, String name, Class<?> type)
        throws NoSuchFieldException, IllegalAccessException {
        return valueTypeHolder.findWither(lookup, name, type);
    }

    public MethodHandle unbox() {
        return valueTypeHolder.unbox();
    }

    public MethodHandle box() {
        return valueTypeHolder.box();
    }

    public MethodHandle newArray() {
        return valueTypeHolder.newArray();
    }

    public MethodHandle arrayGetter() {
        return valueTypeHolder.arrayGetter();
    }

    public MethodHandle arraySetter() {
        return valueTypeHolder.arraySetter();
    }

    public MethodHandle newMultiArray(int dims) {
        return valueTypeHolder.newMultiArray(dims);
    }

    public MethodHandle arrayLength() {
        return valueTypeHolder.arrayLength();
    }

    public MethodHandle identity() {
        return valueTypeHolder.identity();
    }

    public MethodHandle findGetter(Lookup lookup, String name, Class<?> type)
        throws NoSuchFieldException, IllegalAccessException {
        return valueTypeHolder.findGetter(lookup, name, type);
    }

    public MethodHandle substitutabilityTest() {
        return valueTypeHolder.substitutabilityTest();
    }

    public MethodHandle substitutabilityHashCode() {
        return valueTypeHolder.substitutabilityHashCode();
    }

    public MethodHandle defaultValueConstant() {
        return valueTypeHolder.defaultValueConstant();
    }

    public Field[] valueFields() {
        return valueTypeHolder.valueFields();
    }
}
