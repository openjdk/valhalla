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
 * @run testng/othervm -XX:+EnableMVT -XX:+ValueArrayFlatten MVTAccessCheck
 * @run testng/othervm -XX:+EnableMVT -XX:-ValueArrayFlatten MVTAccessCheck
 * @run testng/othervm -XX:+EnableMVT -Dvalhalla.enableValueLambdaForms=true MVTAccessCheck
 * @run testng/othervm -XX:+EnableMVT -Dvalhalla.enableValueLambdaForms=true -Dvalhalla.enablePoolPatches=true MVTAccessCheck
 */

import jdk.incubator.mvt.ValueType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class MVTAccessCheck {

    static final ValueType<?> VT_Point = ValueType.forClass(Point.class);
    static final ValueType<?> VT_PrivatePoint = ValueType.forClass(PrivatePoint.class);

    static final String[] FIELD_NAMES = {"x", "y", "z"};
    static final  Class<?>[] FIELD_TYPES = {int.class, short.class, short.class};

    public void testGetter() throws Throwable {
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            final int offset = i;
            assertThrow(() -> VT_PrivatePoint.findGetter(
                    MethodHandles.lookup(), FIELD_NAMES[offset], FIELD_TYPES[offset]), IllegalAccessException.class);
        }
    }

    public void testWither() throws Throwable {
        testWither(VT_Point);
        testWither(VT_PrivatePoint);
    }

    void testWither(ValueType<?> vt) throws Throwable {
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            final int offset = i;
            assertThrow(() -> vt.findWither(
                    MethodHandles.lookup(), FIELD_NAMES[offset], FIELD_TYPES[offset]), IllegalAccessException.class);
        }
    }

    public void testSubstitutability() throws Throwable {
        PrivatePoint[] pts = {new PrivatePoint(1, (short) 6, (short) 3), new PrivatePoint(1, (short) 2, (short) 3)};

        MethodHandle substTest = VT_PrivatePoint.substitutabilityTest();
        for (PrivatePoint p1 : pts) {
            for (PrivatePoint p2 : pts) {
                boolean b1 = (boolean) substTest.invoke(p1, p2);
                boolean b2 = p1.equals(p2);
                assertEquals(b1, b2);
            }
        }

        MethodHandle hash = VT_PrivatePoint.substitutabilityHashCode();
        for (PrivatePoint p1 : pts) {
            for (PrivatePoint p2 : pts) {
                boolean vHashEq = (int) hash.invoke(p1) == (int) hash.invoke(p2);
                boolean rHashEq = p1.hashCode() == p2.hashCode();
                assertEquals(vHashEq, rHashEq);
            }
        }
    }

    interface MHAction {
        void run() throws Throwable;
    }

    static void assertThrow(MHAction action, Class<? extends Throwable> ex) {
        try {
            action.run();
            Assert.fail();
        } catch (Throwable t) {
            assertTrue(ex.isAssignableFrom(t.getClass()));
        }
    }
}
