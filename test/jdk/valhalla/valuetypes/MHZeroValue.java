/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @compile --enable-preview --source ${jdk.version} MHZeroValue.java
 * @run testng/othervm --enable-preview -XX:InlineFieldMaxFlatSize=128 MHZeroValue
 * @run testng/othervm --enable-preview -XX:InlineFieldMaxFlatSize=0 MHZeroValue
 * @summary Test MethodHandles::zero, MethodHandles::empty and MethodHandles::constant
 *          on value classes.
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import static java.lang.invoke.MethodType.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class MHZeroValue {
    static value class V {
        public boolean isEmpty() {
            return true;
        }
    }

    static primitive class P {
        V empty;
        P() {
            this.empty = new V();
        }
    }

    @DataProvider
    public static Object[][] defaultValue() {
        return new Object[][] {
                // for any type T, default value is always the same as (new T[1])[0]
                new Object[] { int.class,               (new int[1])[0] },
                new Object[] { Integer.class,           (new Integer[1])[0] },
                new Object[] { P.class.asValueType(),   (new P[1])[0] },
                new Object[] { P.class.asPrimaryType(), (new P.ref[1])[0] },
                new Object[] { V.class,                 (new V[1])[0] },
        };
    }
    @Test(dataProvider = "defaultValue")
    public void zero(Class<?> type, Object value) throws Throwable {
        MethodHandle mh = MethodHandles.zero(type);
        assertEquals(mh.invoke(), value);
    }

    @DataProvider
    public static Object[][] primitives() {
        return new Object[][] {
                // int : Integer
                new Object[] { int.class,             Integer.class },
                // Point : Point.ref
                new Object[] { P.class.asValueType(), P.class.asPrimaryType() },
                new Object[] { null,                  V.class },
        };
    }
    @Test(dataProvider = "primitives")
    public void constant(Class<?> primitiveType, Class<?> refType) throws Throwable {
        if (primitiveType != null) {
            try {
                MethodHandles.constant(primitiveType, null);
                fail("Expected NPE thrown for " + primitiveType.getName());
            } catch (NullPointerException e) {
            }
        }

        try {
            MethodHandles.constant(refType, "invalid value");
            fail("Expected CCE thrown for " + refType.getName());
        } catch (ClassCastException e) {}
    }

    @DataProvider
    public static Object[][] emptyTypes() {
        Class<?> pref = P.class.asPrimaryType();
        Class<?> pval = P.class.asValueType();
        return new Object[][] {
                new Object[] { methodType(int.class, int.class, Object.class),     new V(), 0 },
                new Object[] { methodType(Integer.class, int.class, Object.class), new P(), null },
                new Object[] { methodType(pval, int.class, pref),                  null,    P.default },
                new Object[] { methodType(pref, int.class, pval),                  new P(), null },
                new Object[] { methodType(V.class, int.class, pval),               new P(), null },
                new Object[] { methodType(V.class, int.class, V.class),            new V(), null },
        };
    }

    @Test(dataProvider = "emptyTypes")
    public void empty(MethodType mtype, Object param, Object value) throws Throwable {
        MethodHandle mh = MethodHandles.empty(mtype);
        assertEquals(mh.invoke(1, param), value);
    }
}
