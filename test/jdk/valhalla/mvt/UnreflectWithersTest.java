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

/*
 * @test
 * @run testng/othervm -XX:+EnableMVT -Dvalhalla.enablePoolPatches=true UnreflectWithersTest
 */

import jdk.incubator.mvt.ValueType;
import jdk.incubator.mvt.ValueCapableClass;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import static org.testng.Assert.assertEquals;

@Test
public class UnreflectWithersTest {

    @ValueCapableClass
    static final class H {
        final boolean f1;
        final byte f2;
        final short f3;
        final char f4;
        final int f5;
        final long f6;
        final float f7;
        final double f8;
        final Object f9;

        public H(boolean f1, byte f2, short f3, char f4, int f5, long f6, float f7, double f8, Object f9) {
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
            this.f5 = f5;
            this.f6 = f6;
            this.f7 = f7;
            this.f8 = f8;
            this.f9 = f9;
        }
    }

    static final ValueType<?> VT_H = ValueType.forClass(H.class);

    static final MethodHandle VT_H_fromDefault;

    static final MethodHandle VT_H_withArg;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.privateLookupIn(VT_H.boxClass(), MethodHandles.lookup());

            Field[] fs = VT_H.valueFields();
            Arrays.sort(fs, Comparator.comparing(Field::getName));

            MethodHandle mh = VT_H.unreflectWithers(l, true, VT_H.valueFields());
            VT_H_fromDefault = MethodHandles.filterReturnValue(mh, VT_H.box());

            mh = VT_H.unreflectWithers(l, false, VT_H.valueFields());
            VT_H_withArg = MethodHandles.filterReturnValue(mh, VT_H.box());
        }
        catch (Exception e) {
            throw new InternalError(e);
        }
    }

    public void testHFromDefault() throws Throwable {
        H h = (H) VT_H_fromDefault.invoke(true, (byte)1, (short)2, (char)3, 4, 5L, 6.0f, 7.0d, "8");
        assertEquals(h.f1, true);
        assertEquals(h.f2, (byte)1);
        assertEquals(h.f3, (short)2);
        assertEquals(h.f4, (char)3);
        assertEquals(h.f5, 4);
        assertEquals(h.f6, 5L);
        assertEquals(h.f7, 6.0f);
        assertEquals(h.f8, 7.0d);
        assertEquals(h.f9, "8");
    }

    public void testHWithArgumentDefault() throws Throwable {
        MethodHandle mh = MethodHandles.collectArguments(VT_H_withArg, 0, VT_H.defaultValueConstant());

        H h = (H) mh.invoke(true, (byte)1, (short)2, (char)3, 4, 5L, 6.0f, 7.0d, "8");
        assertEquals(h.f1, true);
        assertEquals(h.f2, (byte)1);
        assertEquals(h.f3, (short)2);
        assertEquals(h.f4, (char)3);
        assertEquals(h.f5, 4);
        assertEquals(h.f6, 5L);
        assertEquals(h.f7, 6.0f);
        assertEquals(h.f8, 7.0d);
        assertEquals(h.f9, "8");
    }
}


