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

import jdk.incubator.mvt.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;
import org.testng.annotations.*;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @run testng/othervm -XX:+EnableMVT VectorTest
 */

@Test
public class VectorTest {
    static final ValueType<?> VT = ValueType.forClass(Long2.class);

    static /* QLong2[] */ Object initL2Array(int length) {
        try {
            Object arr = MethodHandles.arrayConstructor(VT.arrayValueClass()).invoke(length);
            for (int i = 0; i < length; i++) {
                Long2 v = new Long2(2 * i, 2 * i + 1);
                MethodHandles.arrayElementSetter(VT.arrayValueClass()).invoke(arr, i, v);
            }
            return arr;
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static final MethodHandle SUM_ARRAY_L2 =
            VectorUtils.reducerLoop(VT, VectorLibrary.L2.ADD_L, VectorLibrary.L2.HADD_L, VT.defaultValueConstant()).
                    asType(methodType(long.class, Object.class));

    /**
     * long sum(QLong2[] a) {
     *   QLong2 v = QLong2.default; // (0,0)
     *   for (int i = 0; i < a.length; i++) {
     *     v = QLong2(v.lo + a[i].lo, v.hi + a[i].hi);
     *   }
     *   return v.lo + v.hi;
     * }
     */
    // @DontInline
    static long sumArrayL2(Object arr) {
        try {
            return (long) SUM_ARRAY_L2.invokeExact(arr);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    @Test(dataProvider = "arraySizes")
    public void testSumArray(Integer size) {
        Object arr = initL2Array(size); // QLong2[size]
        long expected = size * (2*size - 1);
        for (int i = 0; i < 20_000; i++) {
            long sum = sumArrayL2(arr);
            assertEquals(expected, sum);
        }
    }

    /*========================================================*/

    static MethodHandle createConditional() {
        // T target(A...,B...);
        // T fallback(A...,B...);
        // T adapter(A... a,B... b) {
        //   if (test(a...))
        //     return target(a..., b...);
        //   else
        //     return fallback(a..., b...);
        // }
        MethodHandle test = MethodHandles.identity(boolean.class);

        MethodHandle add = VectorLibrary.L2.ADD_L;
        MethodHandle addVL = MethodHandles.filterArguments(add, 1, VT.unbox());
        MethodHandle inc = MethodHandles.insertArguments(addVL, 1, new Long2(1L, 1L));
        MethodHandle incZ = MethodHandles.dropArguments(inc, 0, boolean.class);

        MethodHandle idZ = MethodHandles.dropArguments(VT.identity(), 0, boolean.class);

        // (boolean, QLong2)QLong2
        MethodHandle gwt = MethodHandles.guardWithTest(test, incZ, idZ);
        return gwt;
    }

    static final MethodHandle conditionalMH = createConditional().
            asType(methodType(Long2.class, boolean.class, Long2.class));

    // @DontInline
    static Long2 conditional(boolean b, Long2 v) {
        try {
            return (Long2) conditionalMH.invokeExact(b, v);
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    public void testConditional() {
        Long2 v = new Long2(1L, 2L);
        for (int i = 0; i < 20_000; i++) {
            conditional(true,  v);
            conditional(false, v);
        }
    }

    @DataProvider(name = "arraySizes")
    public Object[][] arraySizes() {
        return new Object[][] {
            new Object[] { 1 },
            new Object[] { 5 },
            new Object[] { 10 },
            new Object[] { 0 }
        };
    }
}

