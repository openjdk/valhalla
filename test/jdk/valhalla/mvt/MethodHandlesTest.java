/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import jdk.incubator.mvt.ValueType;
import jdk.incubator.mvt.ValueCapableClass;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;
import static org.testng.Assert.*;

/*
 * @test
 * @run testng/othervm -XX:+EnableMVT -Dvalhalla.enablePoolPatches=true MethodHandlesTest
 */

@Test
public class MethodHandlesTest {
    @ValueCapableClass
    final class ValueCapable {}

    static final ValueType<?> VT = ValueType.forClass(Point.class);
    static final Class<?> VCC = Point.class;
    static final Class<?> DVT = VT.valueClass();

    static final MethodHandle ID_DVT_MH = MethodHandles.identity(DVT);            // (DVT)DVT
    static final MethodHandle ID_VCC_MH = ID_DVT_MH.asType(methodType(VCC, VCC)); // (VCC)VCC

    static final Object ARG = new Point(0, (short)1, (short)2);

    @Test
    void testInsertArgumentDVT() throws Throwable {
        {
            MethodHandle mh = MethodHandles.insertArguments(ID_DVT_MH, 0, ARG);
            assertEquals(mh.invokeWithArguments(), ARG);
        }

        assertThrows(ClassCastException.class,
                () -> MethodHandles.insertArguments(ID_DVT_MH, 0, new ValueCapable()));

        assertThrows(NullPointerException.class,
                () -> MethodHandles.insertArguments(ID_DVT_MH, 0, new Object[] { null }));
    }

    @Test
    void testInsertArgumentVCC() throws Throwable {
        assertEquals(MethodHandles.insertArguments(ID_VCC_MH, 0, ARG).invokeWithArguments(), ARG);

        assertThrows(ClassCastException.class,
                () -> MethodHandles.insertArguments(ID_VCC_MH, 0, new ValueCapable()));

        {
            MethodHandle mh = MethodHandles.insertArguments(ID_VCC_MH, 0, new Object[]{null});
            assertThrows(NullPointerException.class, () -> mh.invokeWithArguments());
        }
    }

    @Test
    void testConstantDVT() throws Throwable {
        assertEquals(MethodHandles.constant(DVT, ARG).type(), methodType(DVT));

        assertEquals(MethodHandles.constant(DVT, ARG).invokeWithArguments(), ARG);

        assertThrows(ClassCastException.class,
                () -> MethodHandles.constant(DVT, new Object()));

        assertThrows(NullPointerException.class,
                () -> MethodHandles.constant(DVT, null));
    }

    @Test
    void testBindToDVT() {
        assertThrows(IllegalArgumentException.class,
                () -> ID_DVT_MH.bindTo(ARG));
    }
}
