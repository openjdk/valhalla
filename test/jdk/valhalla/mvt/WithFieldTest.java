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

import jdk.experimental.value.MethodHandleBuilder;
import jdk.incubator.mvt.ValueType;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.testng.Assert.*;

/*
 * @test
 * @modules java.base/jdk.experimental.value
 *          jdk.incubator.mvt
 * @run testng/othervm -XX:+EnableMVT -Dvalhalla.enablePoolPatches=true WithFieldTest
 */

@Test
public class WithFieldTest {

    static final ValueType<?> VT_Point = ValueType.forClass(Point.class);

    static final ValueType<?> VT_PrivatePoint = ValueType.forClass(PrivatePoint.class);

    static final MethodHandle MH_Point_get_x;

    static final MethodHandle MH_PrivatePoint_get_x;

    static {
        try {
            MH_Point_get_x = MethodHandles.lookup().findGetter(
                    Point.class, "x", int.class);
            MH_PrivatePoint_get_x = MethodHandles.lookup().findVirtual(
                    PrivatePoint.class, "getX", MethodType.methodType(int.class));
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }


    public void testPrivateLookupPrivateFieldUsingVCC() throws Throwable {
        testPrivateAccess(VT_PrivatePoint, VT_PrivatePoint.boxClass(), MethodHandles.lookup(), MH_PrivatePoint_get_x);
    }

    public void testPrivateLookupPublicFieldUsingVCC() throws Throwable {
        testPrivateAccess(VT_Point, VT_Point.boxClass(), MethodHandles.lookup(), MH_Point_get_x);
    }

    public void testPrivateLookupPrivateFieldUsingDVT() throws Throwable {
        testPrivateAccess(VT_PrivatePoint, VT_PrivatePoint.valueClass(), MethodHandles.lookup(), MH_PrivatePoint_get_x);
    }

    public void testPrivateLookupPublicFieldUsingDVT() throws Throwable {
        testPrivateAccess(VT_Point, VT_Point.valueClass(), MethodHandles.lookup(), MH_Point_get_x);
    }


    @Test(expectedExceptions = IllegalAccessError.class)
    public void testLookupPrivateFieldUsingVCC() throws Throwable {
        testAccess(VT_PrivatePoint, MethodHandles.lookup(), MH_PrivatePoint_get_x);
    }

    @Test(expectedExceptions = IllegalAccessError.class)
    public void testLookupPublicFieldUsingVCC() throws Throwable {
        testAccess(VT_Point, MethodHandles.lookup(), MH_Point_get_x);
    }

    @Test(expectedExceptions = IllegalAccessError.class)
    public void testLookupPrivateFieldUsingDVT() throws Throwable {
        testAccess(VT_PrivatePoint, MethodHandles.lookup(), MH_PrivatePoint_get_x);
    }

    @Test(expectedExceptions = IllegalAccessError.class)
    public void testLookupPublicFieldUsingDVT() throws Throwable {
        testAccess(VT_Point, MethodHandles.lookup(), MH_Point_get_x);
    }


    static void testPrivateAccess(ValueType<?> vt, Class<?> lc, MethodHandles.Lookup l, MethodHandle getter) throws Throwable {
        testAccess(vt, MethodHandles.privateLookupIn(lc, l), getter);
    }

    static void testAccess(ValueType<?> vt, MethodHandles.Lookup l, MethodHandle getter) throws Throwable {
        MethodHandle mh = MethodHandles.collectArguments(vwithfield(l, vt, "x", int.class), 0, vt.defaultValueConstant());

        mh = MethodHandles.filterReturnValue(mh, vt.box());

        mh = MethodHandles.filterReturnValue(mh, getter);

        int actual = (int) mh.invoke(42);
        assertEquals(actual, 42);
    }

    static MethodHandle vwithfield(MethodHandles.Lookup lookup, ValueType<?> vt, String name, Class<?> type) {
        Class<?> dvt = vt.valueClass();
        return MethodHandleBuilder.loadCode(
                lookup,
                lookup.lookupClass().toString() + "_wither$" + name,
                MethodType.methodType(dvt, dvt, type),
                C -> C.vload(0).load(1).vwithfield(dvt, name, fieldDescriptor(type)).vreturn());
    }

    static String fieldDescriptor(Class<?> type) {
        String s = MethodType.methodType(type).toMethodDescriptorString();
        return s.substring(2);
    }

}
