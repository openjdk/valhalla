/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

// -- This file was mechanically generated: Do not edit! -- //

/*
 * @test
 * @run testng/othervm -Diters=2000 VarHandleTestMethodHandleAccessPoint
 */

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class VarHandleTestMethodHandleAccessPoint extends VarHandleBaseTest {
    static final Point static_final_v = Point.getInstance(1,1);

    static Point static_v;

    final Point final_v = Point.getInstance(1,1);

    Point v;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    VarHandle vhValueTypeField;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessPoint.class, "final_v", Point.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessPoint.class, "v", Point.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessPoint.class, "static_final_v", Point.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessPoint.class, "static_v", Point.class);

        vhArray = MethodHandles.arrayElementVarHandle(Point[].class);

        vhValueTypeField = MethodHandles.lookup().findVarHandle(
                    Value.class, "point_v", Point.class);
    }


    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceField(this, hs)));
            cases.add(new MethodHandleAccessTestCase("Instance field unsupported",
                                                     vhField, f, hs -> testInstanceFieldUnsupported(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessPoint::testStaticField));
            cases.add(new MethodHandleAccessTestCase("Static field unsupported",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessPoint::testStaticFieldUnsupported,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodHandleAccessPoint::testArray));
            cases.add(new MethodHandleAccessTestCase("Array unsupported",
                                                     vhArray, f, VarHandleTestMethodHandleAccessPoint::testArrayUnsupported,
                                                     false));
            cases.add(new MethodHandleAccessTestCase("Array index out of bounds",
                                                     vhArray, f, VarHandleTestMethodHandleAccessPoint::testArrayIndexOutOfBounds,
                                                     false));
        cases.add(new MethodHandleAccessTestCase("Value type field",
                                                 vhValueTypeField, f, hs -> testValueTypeField(Value.getInstance(), hs)));
        cases.add(new MethodHandleAccessTestCase("Value type field unsupported",
                                                 vhValueTypeField, f, hs -> testValueTypeFieldUnsupported(Value.getInstance(), hs),
                                                 false));
        }

        // Work around issue with jtreg summary reporting which truncates
        // the String result of Object.toString to 30 characters, hence
        // the first dummy argument
        return cases.stream().map(tc -> new Object[]{tc.toString(), tc}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "accessTestCaseProvider")
    public <T> void testAccess(String desc, AccessTestCase<T> atc) throws Throwable {
        T t = atc.get();
        int iters = atc.requiresLoop() ? ITERS : 1;
        for (int c = 0; c < iters; c++) {
            atc.testAccess(t);
        }
    }


    static void testInstanceField(VarHandleTestMethodHandleAccessPoint recv, Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, Point.getInstance(1,1));
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "set Point value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(recv, Point.getInstance(2,2));
            Point x = (Point) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "setVolatile Point value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(recv, Point.getInstance(1,1));
            Point x = (Point) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "setRelease Point value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(recv, Point.getInstance(2,2));
            Point x = (Point) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "setOpaque Point value");
        }

        hs.get(TestAccessMode.SET).invokeExact(recv, Point.getInstance(1,1));

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, true, "success compareAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "success compareAndSet Point value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, false, "failing compareAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "failing compareAndSet Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchange Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchange Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchange Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchange Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, Point.getInstance(1,1), "success compareAndExchangeAcquire Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "success compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchangeRelease Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchangeRelease Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchangeRelease Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchangeRelease Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetPlain Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetPlain Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSetAcquire Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSetAcquire Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetRelease Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetRelease Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSet Point");
        }

        // Compare set and get
        {
            Point o = (Point) hs.get(TestAccessMode.GET_AND_SET).invokeExact(recv, Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(2,2), "getAndSet Point value");
        }


    }

    static void testInstanceFieldUnsupported(VarHandleTestMethodHandleAccessPoint recv, Handles hs) throws Throwable {

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkUOE(am, () -> {
                Point r = (Point) hs.get(am).invokeExact(recv, Point.getInstance(1,1));
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                Point r = (Point) hs.get(am).invokeExact(recv, Point.getInstance(1,1));
            });
        }
    }

    static void testValueTypeField(Value recv, Handles hs) throws Throwable {
        // Plain
        {
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, Point.getInstance(1,1), "get Point value");
        }
    }

    static void testValueTypeFieldUnsupported(Value recv, Handles hs) throws Throwable {
        // Plain
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkUOE(am, () -> {
                hs.get(am).invokeExact(recv, Point.getInstance(1,1));
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkUOE(am, () -> {
                Point r = (Point) hs.get(am).invokeExact(recv, Point.getInstance(1,1));
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                Point r = (Point) hs.get(am).invokeExact(recv, Point.getInstance(1,1));
            });
        }
    }

    static void testStaticField(Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(Point.getInstance(1,1));
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "set Point value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(Point.getInstance(2,2));
            Point x = (Point) hs.get(TestAccessMode.GET_VOLATILE).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "setVolatile Point value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(Point.getInstance(1,1));
            Point x = (Point) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "setRelease Point value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(Point.getInstance(2,2));
            Point x = (Point) hs.get(TestAccessMode.GET_OPAQUE).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "setOpaque Point value");
        }

        hs.get(TestAccessMode.SET).invokeExact(Point.getInstance(1,1));

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, true, "success compareAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "success compareAndSet Point value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, false, "failing compareAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "failing compareAndSet Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchange Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchange Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchange Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchange Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, Point.getInstance(1,1), "success compareAndExchangeAcquire Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "success compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchangeRelease Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchangeRelease Point value");
        }

        {
            Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchangeRelease Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchangeRelease Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetPlain Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetPlain Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSetAcquire Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSetAcquire Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetRelease Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetRelease Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSet Point");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(Point.getInstance(1,1));

            Point o = (Point) hs.get(TestAccessMode.GET_AND_SET).invokeExact(Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSet Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "getAndSet Point value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(Point.getInstance(1,1));

            Point o = (Point) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSetAcquire Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "getAndSetAcquire Point value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(Point.getInstance(1,1));

            Point o = (Point) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSetRelease Point");
            Point x = (Point) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, Point.getInstance(2,2), "getAndSetRelease Point value");
        }


    }

    static void testStaticFieldUnsupported(Handles hs) throws Throwable {

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkUOE(am, () -> {
                Point r = (Point) hs.get(am).invokeExact(Point.getInstance(1,1));
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                Point r = (Point) hs.get(am).invokeExact(Point.getInstance(1,1));
            });
        }
    }


    static void testArray(Handles hs) throws Throwable {
        Point[] array = new Point[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, Point.getInstance(1,1));
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "get Point value");
            }


            // Volatile
            {
                hs.get(TestAccessMode.SET_VOLATILE).invokeExact(array, i, Point.getInstance(2,2));
                Point x = (Point) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "setVolatile Point value");
            }

            // Lazy
            {
                hs.get(TestAccessMode.SET_RELEASE).invokeExact(array, i, Point.getInstance(1,1));
                Point x = (Point) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "setRelease Point value");
            }

            // Opaque
            {
                hs.get(TestAccessMode.SET_OPAQUE).invokeExact(array, i, Point.getInstance(2,2));
                Point x = (Point) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "setOpaque Point value");
            }

            hs.get(TestAccessMode.SET).invokeExact(array, i, Point.getInstance(1,1));

            // Compare
            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                assertEquals(r, true, "success compareAndSet Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "success compareAndSet Point value");
            }

            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, Point.getInstance(1,1), Point.getInstance(3,3));
                assertEquals(r, false, "failing compareAndSet Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "failing compareAndSet Point value");
            }

            {
                Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                assertEquals(r, Point.getInstance(2,2), "success compareAndExchange Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "success compareAndExchange Point value");
            }

            {
                Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, Point.getInstance(2,2), Point.getInstance(3,3));
                assertEquals(r, Point.getInstance(1,1), "failing compareAndExchange Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "failing compareAndExchange Point value");
            }

            {
                Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                assertEquals(r, Point.getInstance(1,1), "success compareAndExchangeAcquire Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "success compareAndExchangeAcquire Point value");
            }

            {
                Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, Point.getInstance(1,1), Point.getInstance(3,3));
                assertEquals(r, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point value");
            }

            {
                Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                assertEquals(r, Point.getInstance(2,2), "success compareAndExchangeRelease Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "success compareAndExchangeRelease Point value");
            }

            {
                Point r = (Point) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, Point.getInstance(2,2), Point.getInstance(3,3));
                assertEquals(r, Point.getInstance(1,1), "failing compareAndExchangeRelease Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "failing compareAndExchangeRelease Point value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                }
                assertEquals(success, true, "weakCompareAndSetPlain Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetPlain Point value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                }
                assertEquals(success, true, "weakCompareAndSetAcquire Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "weakCompareAndSetAcquire Point");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                }
                assertEquals(success, true, "weakCompareAndSetRelease Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetRelease Point");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                }
                assertEquals(success, true, "weakCompareAndSet Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(1,1), "weakCompareAndSet Point");
            }

            // Compare set and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, Point.getInstance(1,1));

                Point o = (Point) hs.get(TestAccessMode.GET_AND_SET).invokeExact(array, i, Point.getInstance(2,2));
                assertEquals(o, Point.getInstance(1,1), "getAndSet Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "getAndSet Point value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, Point.getInstance(1,1));

                Point o = (Point) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(array, i, Point.getInstance(2,2));
                assertEquals(o, Point.getInstance(1,1), "getAndSetAcquire Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "getAndSetAcquire Point value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, Point.getInstance(1,1));

                Point o = (Point) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(array, i, Point.getInstance(2,2));
                assertEquals(o, Point.getInstance(1,1), "getAndSetRelease Point");
                Point x = (Point) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, Point.getInstance(2,2), "getAndSetRelease Point value");
            }


        }
    }

    static void testArrayUnsupported(Handles hs) throws Throwable {
        Point[] array = new Point[10];

        final int i = 0;

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkUOE(am, () -> {
                Point o = (Point) hs.get(am).invokeExact(array, i, Point.getInstance(1,1));
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                Point o = (Point) hs.get(am).invokeExact(array, i, Point.getInstance(1,1));
            });
        }
    }

    static void testArrayIndexOutOfBounds(Handles hs) throws Throwable {
        Point[] array = new Point[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
                checkIOOBE(am, () -> {
                    Point x = (Point) hs.get(am).invokeExact(array, ci);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
                checkIOOBE(am, () -> {
                    hs.get(am).invokeExact(array, ci, Point.getInstance(1,1));
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
                checkIOOBE(am, () -> {
                    boolean r = (boolean) hs.get(am).invokeExact(array, ci, Point.getInstance(1,1), Point.getInstance(2,2));
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
                checkIOOBE(am, () -> {
                    Point r = (Point) hs.get(am).invokeExact(array, ci, Point.getInstance(2,2), Point.getInstance(1,1));
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
                checkIOOBE(am, () -> {
                    Point o = (Point) hs.get(am).invokeExact(array, ci, Point.getInstance(1,1));
                });
            }


        }
    }
}

