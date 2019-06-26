/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessPoint
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessPoint
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessPoint
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessPoint
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

public class VarHandleTestAccessPoint extends VarHandleBaseTest {
    static final Point static_final_v = Point.getInstance(1,1);

    static Point static_v;

    final Point final_v = Point.getInstance(1,1);

    Point v;

    static final Point static_final_v2 = Point.getInstance(1,1);

    static Point static_v2;

    final Point final_v2 = Point.getInstance(1,1);

    Point v2;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    VarHandle vhArrayObject;
    VarHandle vhValueTypeField;

    VarHandle[] allocate(boolean same) {
        List<VarHandle> vhs = new ArrayList<>();

        String postfix = same ? "" : "2";
        VarHandle vh;
        try {
            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessPoint.class, "final_v" + postfix, Point.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessPoint.class, "v" + postfix, Point.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessPoint.class, "static_final_v" + postfix, Point.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessPoint.class, "static_v" + postfix, Point.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(Point[].class);
            }
            else {
                vh = MethodHandles.arrayElementVarHandle(String[].class);
            }
            vhs.add(vh);
        } catch (Exception e) {
            throw new InternalError(e);
        }
        return vhs.toArray(new VarHandle[0]);
    }

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessPoint.class, "final_v", Point.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessPoint.class, "v", Point.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessPoint.class, "static_final_v", Point.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessPoint.class, "static_v", Point.class);

        vhArray = MethodHandles.arrayElementVarHandle(Point[].class);
        vhArrayObject = MethodHandles.arrayElementVarHandle(Object[].class);

        vhValueTypeField = MethodHandles.lookup().findVarHandle(
                    Value.class, "point_v", Point.class);
    }


    @DataProvider
    public Object[][] varHandlesProvider() throws Exception {
        List<VarHandle> vhs = new ArrayList<>();
        vhs.add(vhField);
        vhs.add(vhStaticField);
        vhs.add(vhArray);

        return vhs.stream().map(tc -> new Object[]{tc}).toArray(Object[][]::new);
    }

    @Test
    public void testEquals() {
        VarHandle[] vhs1 = allocate(true);
        VarHandle[] vhs2 = allocate(true);

        for (int i = 0; i < vhs1.length; i++) {
            for (int j = 0; j < vhs1.length; j++) {
                if (i != j) {
                    assertNotEquals(vhs1[i], vhs1[j]);
                    assertNotEquals(vhs1[i], vhs2[j]);
                }
            }
        }

        VarHandle[] vhs3 = allocate(false);
        for (int i = 0; i < vhs1.length; i++) {
            assertNotEquals(vhs1[i], vhs3[i]);
        }
    }

    @Test(dataProvider = "varHandlesProvider")
    public void testIsAccessModeSupported(VarHandle vh) {
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_VOLATILE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_VOLATILE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_OPAQUE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_OPAQUE));

        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_PLAIN));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_SET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_SET_RELEASE));

        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD_RELEASE));

        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR_RELEASE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND_RELEASE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR_RELEASE));
    }


    @DataProvider
    public Object[][] typesProvider() throws Exception {
        List<Object[]> types = new ArrayList<>();
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessPoint.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(Point[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), Point.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessPoint.class, "final_v", Point.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessPoint.class, "v", Point.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessPoint.class, "static_final_v", Point.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessPoint.class, "static_v", Point.class);
        });
    }


    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance final field",
                                              vhFinalField, vh -> testInstanceFinalField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance final field unsupported",
                                              vhFinalField, vh -> testInstanceFinalFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static final field",
                                              vhStaticFinalField, VarHandleTestAccessPoint::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessPoint::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessPoint::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessPoint::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessPoint::testArray));
        cases.add(new VarHandleAccessTestCase("Array Object[]",
                                              vhArrayObject, VarHandleTestAccessPoint::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessPoint::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessPoint::testArrayIndexOutOfBounds,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array store exception",
                                              vhArrayObject, VarHandleTestAccessPoint::testArrayStoreException,
                                              false));
        cases.add(new VarHandleAccessTestCase("Value type field",
                                              vhValueTypeField, vh -> testValueTypeField(Value.getInstance(), vh)));
        cases.add(new VarHandleAccessTestCase("Value type field unsupported",
                                              vhValueTypeField, vh -> testValueTypeFieldUnsupported(Value.getInstance(), vh),
                                              false));
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




    static void testInstanceFinalField(VarHandleTestAccessPoint recv, VarHandle vh) {
        // Plain
        {
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "get Point value");
        }


        // Volatile
        {
            Point x = (Point) vh.getVolatile(recv);
            assertEquals(x, Point.getInstance(1,1), "getVolatile Point value");
        }

        // Lazy
        {
            Point x = (Point) vh.getAcquire(recv);
            assertEquals(x, Point.getInstance(1,1), "getRelease Point value");
        }

        // Opaque
        {
            Point x = (Point) vh.getOpaque(recv);
            assertEquals(x, Point.getInstance(1,1), "getOpaque Point value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessPoint recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, Point.getInstance(2,2));
        });

        checkUOE(() -> {
            vh.setVolatile(recv, Point.getInstance(2,2));
        });

        checkUOE(() -> {
            vh.setRelease(recv, Point.getInstance(2,2));
        });

        checkUOE(() -> {
            vh.setOpaque(recv, Point.getInstance(2,2));
        });


        checkUOE(() -> {
            Point o = (Point) vh.getAndAdd(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddRelease(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOr(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrRelease(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAnd(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndRelease(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXor(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorRelease(recv, Point.getInstance(1,1));
        });
    }

    static void testValueTypeField(Value recv, VarHandle vh) {
        // Plain
        {
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "get Point value");
        }
    }

    static void testValueTypeFieldUnsupported(Value recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, Point.getInstance(2,2));
        });
    }

    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "get Point value");
        }


        // Volatile
        {
            Point x = (Point) vh.getVolatile();
            assertEquals(x, Point.getInstance(1,1), "getVolatile Point value");
        }

        // Lazy
        {
            Point x = (Point) vh.getAcquire();
            assertEquals(x, Point.getInstance(1,1), "getRelease Point value");
        }

        // Opaque
        {
            Point x = (Point) vh.getOpaque();
            assertEquals(x, Point.getInstance(1,1), "getOpaque Point value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set(Point.getInstance(2,2));
        });

        checkUOE(() -> {
            vh.setVolatile(Point.getInstance(2,2));
        });

        checkUOE(() -> {
            vh.setRelease(Point.getInstance(2,2));
        });

        checkUOE(() -> {
            vh.setOpaque(Point.getInstance(2,2));
        });


        checkUOE(() -> {
            Point o = (Point) vh.getAndAdd(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddRelease(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOr(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrRelease(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAnd(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndRelease(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXor(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorRelease(Point.getInstance(1,1));
        });
    }


    static void testInstanceField(VarHandleTestAccessPoint recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, Point.getInstance(1,1));
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "set Point value");
        }


        // Volatile
        {
            vh.setVolatile(recv, Point.getInstance(2,2));
            Point x = (Point) vh.getVolatile(recv);
            assertEquals(x, Point.getInstance(2,2), "setVolatile Point value");
        }

        // Lazy
        {
            vh.setRelease(recv, Point.getInstance(1,1));
            Point x = (Point) vh.getAcquire(recv);
            assertEquals(x, Point.getInstance(1,1), "setRelease Point value");
        }

        // Opaque
        {
            vh.setOpaque(recv, Point.getInstance(2,2));
            Point x = (Point) vh.getOpaque(recv);
            assertEquals(x, Point.getInstance(2,2), "setOpaque Point value");
        }

        vh.set(recv, Point.getInstance(1,1));

        // Compare
        {
            boolean r = vh.compareAndSet(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, true, "success compareAndSet Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "success compareAndSet Point value");
        }

        {
            boolean r = vh.compareAndSet(recv, Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, false, "failing compareAndSet Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "failing compareAndSet Point value");
        }

        {
            Point r = (Point) vh.compareAndExchange(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchange Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchange Point value");
        }

        {
            Point r = (Point) vh.compareAndExchange(recv, Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchange Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchange Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeAcquire(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, Point.getInstance(1,1), "success compareAndExchangeAcquire Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "success compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeAcquire(recv, Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeRelease(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchangeRelease Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchangeRelease Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeRelease(recv, Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchangeRelease Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchangeRelease Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetPlain Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetPlain Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSetAcquire Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSetAcquire Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetRelease Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetRelease Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSet Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSet Point value");
        }

        // Compare set and get
        {
            vh.set(recv, Point.getInstance(1,1));

            Point o = (Point) vh.getAndSet(recv, Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSet Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "getAndSet Point value");
        }

        {
            vh.set(recv, Point.getInstance(1,1));

            Point o = (Point) vh.getAndSetAcquire(recv, Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSetAcquire Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "getAndSetAcquire Point value");
        }

        {
            vh.set(recv, Point.getInstance(1,1));

            Point o = (Point) vh.getAndSetRelease(recv, Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSetRelease Point");
            Point x = (Point) vh.get(recv);
            assertEquals(x, Point.getInstance(2,2), "getAndSetRelease Point value");
        }


    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessPoint recv, VarHandle vh) {

        checkUOE(() -> {
            Point o = (Point) vh.getAndAdd(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddRelease(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOr(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrRelease(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAnd(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndRelease(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXor(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorAcquire(recv, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorRelease(recv, Point.getInstance(1,1));
        });
    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set(Point.getInstance(1,1));
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "set Point value");
        }


        // Volatile
        {
            vh.setVolatile(Point.getInstance(2,2));
            Point x = (Point) vh.getVolatile();
            assertEquals(x, Point.getInstance(2,2), "setVolatile Point value");
        }

        // Lazy
        {
            vh.setRelease(Point.getInstance(1,1));
            Point x = (Point) vh.getAcquire();
            assertEquals(x, Point.getInstance(1,1), "setRelease Point value");
        }

        // Opaque
        {
            vh.setOpaque(Point.getInstance(2,2));
            Point x = (Point) vh.getOpaque();
            assertEquals(x, Point.getInstance(2,2), "setOpaque Point value");
        }

        vh.set(Point.getInstance(1,1));

        // Compare
        {
            boolean r = vh.compareAndSet(Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, true, "success compareAndSet Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "success compareAndSet Point value");
        }

        {
            boolean r = vh.compareAndSet(Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, false, "failing compareAndSet Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "failing compareAndSet Point value");
        }

        {
            Point r = (Point) vh.compareAndExchange(Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchange Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchange Point value");
        }

        {
            Point r = (Point) vh.compareAndExchange(Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchange Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchange Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeAcquire(Point.getInstance(1,1), Point.getInstance(2,2));
            assertEquals(r, Point.getInstance(1,1), "success compareAndExchangeAcquire Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "success compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeAcquire(Point.getInstance(1,1), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeRelease(Point.getInstance(2,2), Point.getInstance(1,1));
            assertEquals(r, Point.getInstance(2,2), "success compareAndExchangeRelease Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "success compareAndExchangeRelease Point value");
        }

        {
            Point r = (Point) vh.compareAndExchangeRelease(Point.getInstance(2,2), Point.getInstance(3,3));
            assertEquals(r, Point.getInstance(1,1), "failing compareAndExchangeRelease Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "failing compareAndExchangeRelease Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetPlain Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetPlain Point value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSetAcquire Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSetAcquire Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(Point.getInstance(1,1), Point.getInstance(2,2));
            }
            assertEquals(success, true, "weakCompareAndSetRelease Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetRelease Point");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(Point.getInstance(2,2), Point.getInstance(1,1));
            }
            assertEquals(success, true, "weakCompareAndSet Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(1,1), "weakCompareAndSet Point");
        }

        // Compare set and get
        {
            vh.set(Point.getInstance(1,1));

            Point o = (Point) vh.getAndSet(Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSet Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "getAndSet Point value");
        }

        {
            vh.set(Point.getInstance(1,1));

            Point o = (Point) vh.getAndSetAcquire(Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSetAcquire Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "getAndSetAcquire Point value");
        }

        {
            vh.set(Point.getInstance(1,1));

            Point o = (Point) vh.getAndSetRelease(Point.getInstance(2,2));
            assertEquals(o, Point.getInstance(1,1), "getAndSetRelease Point");
            Point x = (Point) vh.get();
            assertEquals(x, Point.getInstance(2,2), "getAndSetRelease Point value");
        }


    }

    static void testStaticFieldUnsupported(VarHandle vh) {

        checkUOE(() -> {
            Point o = (Point) vh.getAndAdd(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddRelease(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOr(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrRelease(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAnd(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndRelease(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXor(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorAcquire(Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorRelease(Point.getInstance(1,1));
        });
    }


    static void testArray(VarHandle vh) {
        Point[] array = new Point[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, Point.getInstance(1,1));
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(1,1), "get Point value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, Point.getInstance(2,2));
                Point x = (Point) vh.getVolatile(array, i);
                assertEquals(x, Point.getInstance(2,2), "setVolatile Point value");
            }

            // Lazy
            {
                vh.setRelease(array, i, Point.getInstance(1,1));
                Point x = (Point) vh.getAcquire(array, i);
                assertEquals(x, Point.getInstance(1,1), "setRelease Point value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, Point.getInstance(2,2));
                Point x = (Point) vh.getOpaque(array, i);
                assertEquals(x, Point.getInstance(2,2), "setOpaque Point value");
            }

            vh.set(array, i, Point.getInstance(1,1));

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                assertEquals(r, true, "success compareAndSet Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "success compareAndSet Point value");
            }

            {
                boolean r = vh.compareAndSet(array, i, Point.getInstance(1,1), Point.getInstance(3,3));
                assertEquals(r, false, "failing compareAndSet Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "failing compareAndSet Point value");
            }

            {
                Point r = (Point) vh.compareAndExchange(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                assertEquals(r, Point.getInstance(2,2), "success compareAndExchange Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(1,1), "success compareAndExchange Point value");
            }

            {
                Point r = (Point) vh.compareAndExchange(array, i, Point.getInstance(2,2), Point.getInstance(3,3));
                assertEquals(r, Point.getInstance(1,1), "failing compareAndExchange Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(1,1), "failing compareAndExchange Point value");
            }

            {
                Point r = (Point) vh.compareAndExchangeAcquire(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                assertEquals(r, Point.getInstance(1,1), "success compareAndExchangeAcquire Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "success compareAndExchangeAcquire Point value");
            }

            {
                Point r = (Point) vh.compareAndExchangeAcquire(array, i, Point.getInstance(1,1), Point.getInstance(3,3));
                assertEquals(r, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "failing compareAndExchangeAcquire Point value");
            }

            {
                Point r = (Point) vh.compareAndExchangeRelease(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                assertEquals(r, Point.getInstance(2,2), "success compareAndExchangeRelease Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(1,1), "success compareAndExchangeRelease Point value");
            }

            {
                Point r = (Point) vh.compareAndExchangeRelease(array, i, Point.getInstance(2,2), Point.getInstance(3,3));
                assertEquals(r, Point.getInstance(1,1), "failing compareAndExchangeRelease Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(1,1), "failing compareAndExchangeRelease Point value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                }
                assertEquals(success, true, "weakCompareAndSetPlain Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetPlain Point value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                }
                assertEquals(success, true, "weakCompareAndSetAcquire Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(1,1), "weakCompareAndSetAcquire Point");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, Point.getInstance(1,1), Point.getInstance(2,2));
                }
                assertEquals(success, true, "weakCompareAndSetRelease Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "weakCompareAndSetRelease Point");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, Point.getInstance(2,2), Point.getInstance(1,1));
                }
                assertEquals(success, true, "weakCompareAndSet Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(1,1), "weakCompareAndSet Point");
            }

            // Compare set and get
            {
                vh.set(array, i, Point.getInstance(1,1));

                Point o = (Point) vh.getAndSet(array, i, Point.getInstance(2,2));
                assertEquals(o, Point.getInstance(1,1), "getAndSet Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "getAndSet Point value");
            }

            {
                vh.set(array, i, Point.getInstance(1,1));

                Point o = (Point) vh.getAndSetAcquire(array, i, Point.getInstance(2,2));
                assertEquals(o, Point.getInstance(1,1), "getAndSetAcquire Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "getAndSetAcquire Point value");
            }

            {
                vh.set(array, i, Point.getInstance(1,1));

                Point o = (Point) vh.getAndSetRelease(array, i, Point.getInstance(2,2));
                assertEquals(o, Point.getInstance(1,1), "getAndSetRelease Point");
                Point x = (Point) vh.get(array, i);
                assertEquals(x, Point.getInstance(2,2), "getAndSetRelease Point value");
            }


        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        Point[] array = new Point[10];

        int i = 0;

        checkUOE(() -> {
            Point o = (Point) vh.getAndAdd(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddAcquire(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndAddRelease(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOr(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrAcquire(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseOrRelease(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAnd(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndAcquire(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseAndRelease(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXor(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorAcquire(array, i, Point.getInstance(1,1));
        });

        checkUOE(() -> {
            Point o = (Point) vh.getAndBitwiseXorRelease(array, i, Point.getInstance(1,1));
        });
    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        Point[] array = new Point[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkIOOBE(() -> {
                Point x = (Point) vh.get(array, ci);
            });

            checkIOOBE(() -> {
                vh.set(array, ci, Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                Point x = (Point) vh.getVolatile(array, ci);
            });

            checkIOOBE(() -> {
                vh.setVolatile(array, ci, Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                Point x = (Point) vh.getAcquire(array, ci);
            });

            checkIOOBE(() -> {
                vh.setRelease(array, ci, Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                Point x = (Point) vh.getOpaque(array, ci);
            });

            checkIOOBE(() -> {
                vh.setOpaque(array, ci, Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, Point.getInstance(1,1), Point.getInstance(2,2));
            });

            checkIOOBE(() -> {
                Point r = (Point) vh.compareAndExchange(array, ci, Point.getInstance(2,2), Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                Point r = (Point) vh.compareAndExchangeAcquire(array, ci, Point.getInstance(2,2), Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                Point r = (Point) vh.compareAndExchangeRelease(array, ci, Point.getInstance(2,2), Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, Point.getInstance(1,1), Point.getInstance(2,2));
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, Point.getInstance(1,1), Point.getInstance(2,2));
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, Point.getInstance(1,1), Point.getInstance(2,2));
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, Point.getInstance(1,1), Point.getInstance(2,2));
            });

            checkIOOBE(() -> {
                Point o = (Point) vh.getAndSet(array, ci, Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                Point o = (Point) vh.getAndSetAcquire(array, ci, Point.getInstance(1,1));
            });

            checkIOOBE(() -> {
                Point o = (Point) vh.getAndSetRelease(array, ci, Point.getInstance(1,1));
            });


        }
    }

    static void testArrayStoreException(VarHandle vh) throws Throwable {
        Object[] array = new Point[10];
        Arrays.fill(array, Point.getInstance(1,1));
        Object value = new Object();

        // Set
        checkASE(() -> {
            vh.set(array, 0, value);
        });

        // SetVolatile
        checkASE(() -> {
            vh.setVolatile(array, 0, value);
        });

        // SetOpaque
        checkASE(() -> {
            vh.setOpaque(array, 0, value);
        });

        // SetRelease
        checkASE(() -> {
            vh.setRelease(array, 0, value);
        });

        // CompareAndSet
        checkASE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(array, 0, Point.getInstance(1,1), value);
        });

        // WeakCompareAndSet
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Point.getInstance(1,1), value);
        });

        // WeakCompareAndSetVolatile
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(array, 0, Point.getInstance(1,1), value);
        });

        // WeakCompareAndSetAcquire
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Point.getInstance(1,1), value);
        });

        // WeakCompareAndSetRelease
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Point.getInstance(1,1), value);
        });

        // CompareAndExchange
        checkASE(() -> { // receiver reference class
            Point x = (Point) vh.compareAndExchange(array, 0, Point.getInstance(1,1), value);
        });

        // CompareAndExchangeAcquire
        checkASE(() -> { // receiver reference class
            Point x = (Point) vh.compareAndExchangeAcquire(array, 0, Point.getInstance(1,1), value);
        });

        // CompareAndExchangeRelease
        checkASE(() -> { // receiver reference class
            Point x = (Point) vh.compareAndExchangeRelease(array, 0, Point.getInstance(1,1), value);
        });

        // GetAndSet
        checkASE(() -> { // receiver reference class
            Point x = (Point) vh.getAndSet(array, 0, value);
        });

        // GetAndSetAcquire
        checkASE(() -> { // receiver reference class
            Point x = (Point) vh.getAndSetAcquire(array, 0, value);
        });

        // GetAndSetRelease
        checkASE(() -> { // receiver reference class
            Point x = (Point) vh.getAndSetRelease(array, 0, value);
        });
    }
}

