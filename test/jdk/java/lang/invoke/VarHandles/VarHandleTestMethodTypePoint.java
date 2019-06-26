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
 * @bug 8156486
 * @run testng/othervm VarHandleTestMethodTypePoint
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false VarHandleTestMethodTypePoint
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

import static java.lang.invoke.MethodType.*;

public class VarHandleTestMethodTypePoint extends VarHandleBaseTest {
    static final Point static_final_v = Point.getInstance(1,1);

    static Point static_v = Point.getInstance(1,1);

    final Point final_v = Point.getInstance(1,1);

    Point v = Point.getInstance(1,1);

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypePoint.class, "final_v", Point.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypePoint.class, "v", Point.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypePoint.class, "static_final_v", Point.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypePoint.class, "static_v", Point.class);

        vhArray = MethodHandles.arrayElementVarHandle(Point[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypePoint::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypePoint::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypePoint::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypePoint::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypePoint recv, VarHandle vh) throws Throwable {
        // Get
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.get(null);
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.get(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            Point x = (Point) vh.get(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.get(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.get();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.get(recv, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.set(null, Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            vh.set(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(recv, Point.getInstance(1,1), Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.getVolatile(null);
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            Point x = (Point) vh.getVolatile(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getVolatile(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getVolatile(recv, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setVolatile(null, Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            vh.setVolatile(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(recv, Point.getInstance(1,1), Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.getOpaque(null);
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            Point x = (Point) vh.getOpaque(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getOpaque(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getOpaque(recv, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setOpaque(null, Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            vh.setOpaque(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(recv, Point.getInstance(1,1), Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.getAcquire(null);
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            Point x = (Point) vh.getAcquire(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getAcquire(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAcquire(recv, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setRelease(null, Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            vh.setRelease(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(recv, Point.getInstance(1,1), Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.compareAndSet(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.compareAndSet(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.compareAndExchange(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.compareAndExchange(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchange(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchange(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            Point x = (Point) vh.compareAndExchange(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(recv, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(recv, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchange(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.compareAndExchangeAcquire(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.compareAndExchangeAcquire(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchangeAcquire(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchangeAcquire(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            Point x = (Point) vh.compareAndExchangeAcquire(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(recv, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchangeAcquire(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.compareAndExchangeRelease(null, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.compareAndExchangeRelease(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchangeRelease(recv, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchangeRelease(recv, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            Point x = (Point) vh.compareAndExchangeRelease(0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(recv, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchangeRelease(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.getAndSet(null, Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.getAndSet(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            Point x = (Point) vh.getAndSet(0, Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSet(recv, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(recv, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSet(recv, Point.getInstance(1,1), Void.class);
        });

        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.getAndSetAcquire(null, Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.getAndSetAcquire(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            Point x = (Point) vh.getAndSetAcquire(0, Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(recv, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(recv, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSetAcquire(recv, Point.getInstance(1,1), Void.class);
        });

        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.getAndSetRelease(null, Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            Point x = (Point) vh.getAndSetRelease(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            Point x = (Point) vh.getAndSetRelease(0, Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(recv, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(recv, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSetRelease(recv, Point.getInstance(1,1), Void.class);
        });


    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypePoint recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class)).
                    invokeExact((VarHandleTestMethodTypePoint) null);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                Point x = (Point) hs.get(am, methodType(Point.class, int.class)).
                    invokeExact(0);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypePoint.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypePoint.class)).
                    invokeExact(recv);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                hs.get(am, methodType(void.class, VarHandleTestMethodTypePoint.class, Point.class)).
                    invokeExact((VarHandleTestMethodTypePoint) null, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                hs.get(am, methodType(void.class, Class.class, Point.class)).
                    invokeExact(Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                hs.get(am, methodType(void.class, VarHandleTestMethodTypePoint.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, Point.class)).
                    invokeExact(0, Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, VarHandleTestMethodTypePoint.class, Point.class, Class.class)).
                    invokeExact(recv, Point.getInstance(1,1), Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypePoint.class, Point.class, Point.class)).
                    invokeExact((VarHandleTestMethodTypePoint) null, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, Point.class, Point.class)).
                    invokeExact(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypePoint.class, Class.class, Point.class)).
                    invokeExact(recv, Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypePoint.class, Point.class, Class.class)).
                    invokeExact(recv, Point.getInstance(1,1), Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , Point.class, Point.class)).
                    invokeExact(0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypePoint.class, Point.class, Point.class, Class.class)).
                    invokeExact(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { // null receiver
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Point.class, Point.class)).
                    invokeExact((VarHandleTestMethodTypePoint) null, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class, Point.class, Point.class)).
                    invokeExact(Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Class.class, Point.class)).
                    invokeExact(recv, Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Point.class, Class.class)).
                    invokeExact(recv, Point.getInstance(1,1), Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                Point x = (Point) hs.get(am, methodType(Point.class, int.class , Point.class, Point.class)).
                    invokeExact(0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypePoint.class , Point.class, Point.class)).
                    invokeExact(recv, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypePoint.class , Point.class, Point.class)).
                    invokeExact(recv, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Point.class, Point.class, Class.class)).
                    invokeExact(recv, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { // null receiver
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Point.class)).
                    invokeExact((VarHandleTestMethodTypePoint) null, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class, Point.class)).
                    invokeExact(Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                Point x = (Point) hs.get(am, methodType(Point.class, int.class, Point.class)).
                    invokeExact(0, Point.getInstance(1,1));
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypePoint.class, Point.class)).
                    invokeExact(recv, Point.getInstance(1,1));
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypePoint.class, Point.class)).
                    invokeExact(recv, Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, VarHandleTestMethodTypePoint.class, Point.class)).
                    invokeExact(recv, Point.getInstance(1,1), Void.class);
            });
        }


    }


    static void testStaticFieldWrongMethodType(VarHandle vh) throws Throwable {
        // Get
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.get();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get();
        });
        // Incorrect arity
        checkWMTE(() -> { // >
            Point x = (Point) vh.get(Void.class);
        });


        // Set
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.set(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(Point.getInstance(1,1), Void.class);
        });


        // GetVolatile
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getVolatile();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getVolatile(Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.setVolatile(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(Point.getInstance(1,1), Void.class);
        });


        // GetOpaque
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getOpaque();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getOpaque(Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.setOpaque(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(Point.getInstance(1,1), Void.class);
        });


        // GetAcquire
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getAcquire();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAcquire(Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.setRelease(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(Point.getInstance(1,1), Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.compareAndSet(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.compareAndSet(Point.getInstance(1,1), Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(Point.getInstance(1,1), Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(Point.getInstance(1,1), Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(Point.getInstance(1,1), Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(Point.getInstance(1,1), Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchange(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchange(Point.getInstance(1,1), Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchange(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchangeAcquire(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchangeAcquire(Point.getInstance(1,1), Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchangeAcquire(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchangeRelease(Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchangeRelease(Point.getInstance(1,1), Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchangeRelease(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSet(Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSet(Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSet(Point.getInstance(1,1), Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSetAcquire(Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSetAcquire(Point.getInstance(1,1), Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSetRelease(Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSetRelease(Point.getInstance(1,1), Void.class);
        });


    }

    static void testStaticFieldWrongMethodType(Handles hs) throws Throwable {
        int i = 0;

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            // Incorrect arity
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Class.class)).
                    invokeExact(Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            hs.checkWMTEOrCCE(() -> { // value reference class
                hs.get(am, methodType(void.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, Point.class, Class.class)).
                    invokeExact(Point.getInstance(1,1), Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            hs.checkWMTEOrCCE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, Point.class)).
                    invokeExact(Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Point.class, Class.class)).
                    invokeExact(Point.getInstance(1,1), Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Point.class, Point.class, Class.class)).
                    invokeExact(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            hs.checkWMTEOrCCE(() -> { // expected reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class, Point.class)).
                    invokeExact(Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Point.class, Class.class)).
                    invokeExact(Point.getInstance(1,1), Void.class);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, Point.class, Point.class)).
                    invokeExact(Point.getInstance(1,1), Point.getInstance(1,1));
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Point.class, Point.class)).
                    invokeExact(Point.getInstance(1,1), Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, Point.class, Point.class, Class.class)).
                    invokeExact(Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            hs.checkWMTEOrCCE(() -> { // value reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, Point.class)).
                    invokeExact(Point.getInstance(1,1));
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Point.class)).
                    invokeExact(Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, Point.class, Class.class)).
                    invokeExact(Point.getInstance(1,1), Void.class);
            });
        }


    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        Point[] array = new Point[10];
        Arrays.fill(array, Point.getInstance(1,1));

        // Get
        // Incorrect argument types
        checkNPE(() -> { // null array
            Point x = (Point) vh.get(null, 0);
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            Point x = (Point) vh.get(0, 0);
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.get(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.get(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.get();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.get(array, 0, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.set(null, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            vh.set(Void.class, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            vh.set(array, Void.class, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(array, 0, Point.getInstance(1,1), Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            Point x = (Point) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            Point x = (Point) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.getVolatile(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getVolatile(array, 0, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setVolatile(null, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            vh.setVolatile(Void.class, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            vh.setVolatile(array, Void.class, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(array, 0, Point.getInstance(1,1), Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            Point x = (Point) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            Point x = (Point) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.getOpaque(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getOpaque(array, 0, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setOpaque(null, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            vh.setOpaque(Void.class, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            vh.setOpaque(array, Void.class, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(array, 0, Point.getInstance(1,1), Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            Point x = (Point) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            Point x = (Point) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.getAcquire(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAcquire(array, 0, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setRelease(null, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            vh.setRelease(Void.class, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            vh.setRelease(array, Void.class, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(array, 0, Point.getInstance(1,1), Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.compareAndSet(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.compareAndSet(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.compareAndSet(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSet(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.compareAndExchange(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.compareAndExchange(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchange(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchange(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // array primitive class
            Point x = (Point) vh.compareAndExchange(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.compareAndExchange(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchange(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.compareAndExchangeAcquire(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.compareAndExchangeAcquire(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchangeAcquire(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchangeAcquire(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // array primitive class
            Point x = (Point) vh.compareAndExchangeAcquire(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.compareAndExchangeAcquire(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchangeAcquire(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            Point x = (Point) vh.compareAndExchangeRelease(null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.compareAndExchangeRelease(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkCCE(() -> { // expected reference class
            Point x = (Point) vh.compareAndExchangeRelease(array, 0, Void.class, Point.getInstance(1,1));
        });
        checkCCE(() -> { // actual reference class
            Point x = (Point) vh.compareAndExchangeRelease(array, 0, Point.getInstance(1,1), Void.class);
        });
        checkWMTE(() -> { // array primitive class
            Point x = (Point) vh.compareAndExchangeRelease(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.compareAndExchangeRelease(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.compareAndExchangeRelease(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null array
            Point x = (Point) vh.getAndSet(null, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.getAndSet(Void.class, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            Point x = (Point) vh.getAndSet(0, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.getAndSet(array, Void.class, Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSet(array, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(array, 0, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSet(array, 0, Point.getInstance(1,1), Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            Point x = (Point) vh.getAndSetAcquire(null, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.getAndSetAcquire(Void.class, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            Point x = (Point) vh.getAndSetAcquire(0, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.getAndSetAcquire(array, Void.class, Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(array, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSetAcquire(array, 0, Point.getInstance(1,1), Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            Point x = (Point) vh.getAndSetRelease(null, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // array reference class
            Point x = (Point) vh.getAndSetRelease(Void.class, 0, Point.getInstance(1,1));
        });
        checkCCE(() -> { // value reference class
            Point x = (Point) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            Point x = (Point) vh.getAndSetRelease(0, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // index reference class
            Point x = (Point) vh.getAndSetRelease(array, Void.class, Point.getInstance(1,1));
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(array, 0, Point.getInstance(1,1));
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(array, 0, Point.getInstance(1,1));
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            Point x = (Point) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            Point x = (Point) vh.getAndSetRelease(array, 0, Point.getInstance(1,1), Void.class);
        });


    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        Point[] array = new Point[10];
        Arrays.fill(array, Point.getInstance(1,1));

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class)).
                    invokeExact((Point[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { // array primitive class
                Point x = (Point) hs.get(am, methodType(Point.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { // index reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, Point[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Point[].class, int.class)).
                    invokeExact(array, 0);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                hs.get(am, methodType(void.class, Point[].class, int.class, Point.class)).
                    invokeExact((Point[]) null, 0, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                hs.get(am, methodType(void.class, Class.class, int.class, Point.class)).
                    invokeExact(Void.class, 0, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                hs.get(am, methodType(void.class, Point[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, int.class, Point.class)).
                    invokeExact(0, 0, Point.getInstance(1,1));
            });
            checkWMTE(() -> { // index reference class
                hs.get(am, methodType(void.class, Point[].class, Class.class, Point.class)).
                    invokeExact(array, Void.class, Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, Point[].class, int.class, Class.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Point[].class, int.class, Point.class, Point.class)).
                    invokeExact((Point[]) null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, Point.class, Point.class)).
                    invokeExact(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Point[].class, int.class, Class.class, Point.class)).
                    invokeExact(array, 0, Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Point[].class, int.class, Point.class, Class.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, Point.class, Point.class)).
                    invokeExact(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            checkWMTE(() -> { // index reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Point[].class, Class.class, Point.class, Point.class)).
                    invokeExact(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Point[].class, int.class, Point.class, Point.class, Class.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Point.class, Point.class)).
                    invokeExact((Point[]) null, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class, int.class, Point.class, Point.class)).
                    invokeExact(Void.class, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Class.class, Point.class)).
                    invokeExact(array, 0, Void.class, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Point.class, Class.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Void.class);
            });
            checkWMTE(() -> { // array primitive class
                Point x = (Point) hs.get(am, methodType(Point.class, int.class, int.class, Point.class, Point.class)).
                    invokeExact(0, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            checkWMTE(() -> { // index reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, Class.class, Point.class, Point.class)).
                    invokeExact(array, Void.class, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, Point[].class, int.class, Point.class, Point.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Point[].class, int.class, Point.class, Point.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Point.class, Point.class, Class.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Point.getInstance(1,1), Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Point.class)).
                    invokeExact((Point[]) null, 0, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Class.class, int.class, Point.class)).
                    invokeExact(Void.class, 0, Point.getInstance(1,1));
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                Point x = (Point) hs.get(am, methodType(Point.class, int.class, int.class, Point.class)).
                    invokeExact(0, 0, Point.getInstance(1,1));
            });
            checkWMTE(() -> { // index reference class
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, Class.class, Point.class)).
                    invokeExact(array, Void.class, Point.getInstance(1,1));
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, Point[].class, int.class, Point.class)).
                    invokeExact(array, 0, Point.getInstance(1,1));
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Point[].class, int.class, Point.class)).
                    invokeExact(array, 0, Point.getInstance(1,1));
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                Point x = (Point) hs.get(am, methodType(Point.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                Point x = (Point) hs.get(am, methodType(Point.class, Point[].class, int.class, Point.class, Class.class)).
                    invokeExact(array, 0, Point.getInstance(1,1), Void.class);
            });
        }


    }
}
