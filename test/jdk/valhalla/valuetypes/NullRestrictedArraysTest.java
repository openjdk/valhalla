/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @enablePreview
 * @run junit/othervm NullRestrictedArraysTest
 * @run junit/othervm -XX:FlatArrayElementMaxSize=0 NullRestrictedArraysTest
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.internal.value.CheckedType;
import jdk.internal.value.NullRestrictedCheckedType;
import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestricted;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class NullRestrictedArraysTest {
    interface I {
        int getValue();
    }
    @ImplicitlyConstructible
    static value class Value implements I {
        int v;
        Value() {
            this(0);
        }
        Value(int v) {
            this.v = v;
        }
        public int getValue() {
            return v;
        }
    }

    static class T {
        String s;
        Value obj;  // can be null
        @NullRestricted
        Value value;
    }

    static Stream<Arguments> checkedTypes() throws ReflectiveOperationException {
        return Stream.of(
                Arguments.of(T.class.getDeclaredField("s"), String.class, false),
                Arguments.of(T.class.getDeclaredField("obj"), Value.class, false),
                Arguments.of(T.class.getDeclaredField("value"), Value.class, true)
        );
    }

    /*
     * Test creating null-restricted arrays with CheckedType
     */
    @ParameterizedTest
    @MethodSource("checkedTypes")
    public void testCheckedTypeArrays(Field field, Class<?> type, boolean nullRestricted) throws ReflectiveOperationException {
        CheckedType checkedType = ValueClass.checkedType(field);
        assertTrue(field.getType() == type);
        assertTrue(checkedType.boundingClass() == type);
        Object[] array = ValueClass.newArrayInstance(checkedType, 4);
        assertTrue(ValueClass.isNullRestrictedArray(array) == nullRestricted);
        assertTrue(checkedType instanceof NullRestrictedCheckedType == nullRestricted);
        for (int i=0; i < array.length; i++) {
            array[i] = type.newInstance();
        }
        if (nullRestricted) {
            // NPE thrown if elements in a null-restricted array set to null
            assertThrows(NullPointerException.class, () -> array[0] = null);
        } else {
            array[0] = null;
        }
    }

    @Test
    public void testVarHandle() {
        int len = 4;
        Object[] array = (Object[]) Array.newInstance(Value.class, len);
        Object[] nullRestrictedArray = ValueClass.newNullRestrictedArray(Value.class, len);

        // Test var handles
        testVarHandleArray(array, Value[].class);
        testVarHandleArray(array, I[].class);
        testVarHandleNullRestrictedArray(nullRestrictedArray, Value[].class);
        testVarHandleNullRestrictedArray(nullRestrictedArray, I[].class);
    }

    private void testVarHandleArray(Object[] array, Class<?> arrayClass) {
        for (int i=0; i < array.length; i++) {
            array[i] = new Value(i);
        }

        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayClass);
        Value value = new Value(0);
        Value value1 =  new Value(1);

        assertTrue(vh.get(array, 0) == value);
        assertTrue(vh.getVolatile(array, 0) == value);
        assertTrue(vh.getOpaque(array, 0) == value);
        assertTrue(vh.getAcquire(array, 0) == value);
        vh.set(array, 0, null);
        vh.setVolatile(array, 0, null);
        vh.setOpaque(array, 0, null);
        vh.setRelease(array, 0, null);

        vh.compareAndSet(array, 1, value1, null);             vh.set(array, 1, value1);
        vh.compareAndExchange(array, 1, value1, null);        vh.set(array, 1, value1);
        vh.compareAndExchangeAcquire(array, 1, value1, null); vh.set(array, 1, value1);
        vh.compareAndExchangeRelease(array, 1, value1, null); vh.set(array, 1, value1);
        vh.weakCompareAndSet(array, 1, value1, null);         vh.set(array, 1, value1);
        vh.weakCompareAndSetAcquire(array, 1, value1, null);  vh.set(array, 1, value1);
        vh.weakCompareAndSetPlain(array, 1, value1, null);    vh.set(array, 1, value1);
        vh.weakCompareAndSetRelease(array, 1, value1, null);  vh.set(array, 1, value1);
    }

    private void testVarHandleNullRestrictedArray(Object[] array, Class<?> arrayClass) {
        for (int i=0; i < array.length; i++) {
            array[i] = new Value(i);
        }

        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayClass);
        Value value = new Value(0);
        Value value1 =  new Value(1);
        assertTrue(vh.get(array, 0) == value);
        assertTrue(vh.getVolatile(array, 0) == value);
        assertTrue(vh.getOpaque(array, 0) == value);
        assertTrue(vh.getAcquire(array, 0) == value);
        assertThrows(NullPointerException.class, () -> vh.set(array, 0, null));
        assertThrows(NullPointerException.class, () -> vh.setVolatile(array, 0, null));
        assertThrows(NullPointerException.class, () -> vh.setOpaque(array, 0, null));
        assertThrows(NullPointerException.class, () -> vh.setRelease(array, 0, null));

        assertThrows(NullPointerException.class, () -> vh.compareAndSet(array, 1, value1, null));
        assertThrows(NullPointerException.class, () -> vh.compareAndExchange(array, 1, value1, null));
        assertThrows(NullPointerException.class, () -> vh.compareAndExchangeAcquire(array, 1, value1, null));
        assertThrows(NullPointerException.class, () -> vh.compareAndExchangeRelease(array, 1, value1, null));
        assertThrows(NullPointerException.class, () -> vh.weakCompareAndSet(array, 1, value1, null));
        assertThrows(NullPointerException.class, () -> vh.weakCompareAndSetAcquire(array, 1, value1, null));
        assertThrows(NullPointerException.class, () -> vh.weakCompareAndSetPlain(array, 1, value1, null));
        assertThrows(NullPointerException.class, () -> vh.weakCompareAndSetRelease(array, 1, value1, null));
    }

}
