/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.Optional;

import jdk.internal.misc.Unsafe;
import sun.hotspot.WhiteBox;
import static jdk.test.lib.Asserts.*;

/*
 * @test ValueAtomicAccess
 * @summary Test atomicity of inline fields and array elements
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile ValueAtomicAccess.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xint  -XX:+UnlockDiagnosticVMOptions -XX:ForceAtomicAccess=
 *                   -DSTEP_COUNT=10000 -XX:InlineFieldMaxFlatSize=128 -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:+WhiteBoxAPI
 *                                   runtime.valhalla.inlinetypes.ValueAtomicAccess
 * @run main/othervm -Xint  -XX:+UnlockDiagnosticVMOptions -XX:ForceAtomicAccess=*
 *                   -DSTEP_COUNT=10000 -XX:InlineFieldMaxFlatSize=128 -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:+WhiteBoxAPI
 *                                   runtime.valhalla.inlinetypes.ValueAtomicAccess
 * @run main/othervm -Xbatch -DSTEP_COUNT=10000000 -XX:InlineFieldMaxFlatSize=128 -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                                   runtime.valhalla.inlinetypes.ValueAtomicAccess
 * @run main/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:ForceAtomicAccess=
 *                   -DTEAR_MODE=fieldonly -XX:InlineFieldMaxFlatSize=128 -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:+WhiteBoxAPI
 *                                   runtime.valhalla.inlinetypes.ValueAtomicAccess
 * @run main/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:ForceAtomicAccess=
 *                   -DTEAR_MODE=arrayonly -XX:InlineFieldMaxFlatSize=128 -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:+WhiteBoxAPI
 *                                   runtime.valhalla.inlinetypes.ValueAtomicAccess
 * @run main/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:ForceAtomicAccess=*
 *                   -DTEAR_MODE=both -XX:InlineFieldMaxFlatSize=128 -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:+WhiteBoxAPI
 *                                   runtime.valhalla.inlinetypes.ValueAtomicAccess
 */
public class ValueAtomicAccess {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    private static final boolean ALWAYS_ATOMIC = WHITE_BOX.getStringVMFlag("ForceAtomicAccess").contains("*");
    private static final String TEAR_MODE = System.getProperty("TEAR_MODE", "both");
    private static final boolean TEAR_FIELD = !TEAR_MODE.equals("arrayonly");
    private static final boolean TEAR_ARRAY = !TEAR_MODE.equals("fieldonly");
    private static final int STEP_COUNT = Integer.getInteger("STEP_COUNT", 100_000);
    private static final boolean TFIELD_FLAT, TARRAY_FLAT;
    private static final boolean AAFIELD_FLAT, AAARRAY_FLAT;
    static {
        try {
            Field TPB_field = TPointBox.class.getDeclaredField("field");
            Field TPB_array = TPointBox.class.getDeclaredField("array");
            Field AAPB_field = AAPointBox.class.getDeclaredField("field");
            Field AAPB_array = AAPointBox.class.getDeclaredField("array");
            TFIELD_FLAT = UNSAFE.isFlattened(TPB_field);
            TARRAY_FLAT = UNSAFE.isFlattenedArray(TPB_array.getType());
            AAFIELD_FLAT = UNSAFE.isFlattened(AAPB_field);
            AAARRAY_FLAT = UNSAFE.isFlattenedArray(AAPB_array.getType());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
    private static final String SETTINGS =
        String.format("USE_COMPILER=%s ALWAYS_ATOMIC=%s TEAR_MODE=%s STEP_COUNT=%s FLAT TF/TA=%s/%s AAF/AAA=%s/%s",
                      USE_COMPILER, ALWAYS_ATOMIC, TEAR_MODE, STEP_COUNT,
                      TFIELD_FLAT, TARRAY_FLAT, AAFIELD_FLAT, AAARRAY_FLAT);
    private static final String NOTE_TORN_POINT = "Note: torn point";

    public static void main(String[] args) throws Exception {
        System.out.println(SETTINGS);
        ValueAtomicAccess valueTearing = new ValueAtomicAccess();
        valueTearing.run();
        // Extra representation check:
        assert(!AAFIELD_FLAT) : "AA field must be indirect not flat";
        assert(!AAARRAY_FLAT) : "AA array must be indirect not flat";
        if (ALWAYS_ATOMIC) {
            assert(!TFIELD_FLAT) : "field must be indirect not flat";
            assert(!TARRAY_FLAT) : "array must be indirect not flat";
        }
    }

    // A normally non-atomic inline value.
    static primitive class TPoint {
        TPoint(long x, long y) { this.x = x; this.y = y; }
        final long x, y;
        public String toString() { return String.format("(%d,%d)", x, y); }
    }

    static class TooTearable extends AssertionError {
        final Object badPoint;
        TooTearable(String msg, Object badPoint) {
            super(msg);
            this.badPoint = badPoint;
        }
    }

    interface PointBox {
        void step();    // mutate inline value state
        void check();   // check sanity of inline value state
    }

    class TPointBox implements PointBox {
        TPoint field;
        TPoint[] array = new TPoint[1];
        // Step the points forward by incrementing their components
        // "simultaneously".  A racing thread will catch flaws in the
        // simultaneity.
        TPoint step(TPoint p) {
            return new TPoint(p.x + 1, p.y + 1);
        }
        public @Override
        void step() {
            if (TEAR_FIELD) {
                field = step(field);
            }
            if (TEAR_ARRAY) {
                array[0] = step(array[0]);
            }
            check();
        }
        // Invariant:  The components of each point are "always" equal.
        // As long as simultaneity is preserved, this is true.
        public @Override
        void check() {
            if (TEAR_FIELD) {
                check(field, "field");
            }
            if (TEAR_ARRAY) {
                check(array[0], "array element");
            }
        }
        void check(TPoint p, String where) {
            if (p.x == p.y)  return;
            String msg = String.format("%s %s in %s; settings = %s",
                                       NOTE_TORN_POINT,
                                       p, where, SETTINGS);
            throw new TooTearable(msg, p);
        }
        public String toString() {
            return String.format("TPB[%s, {%s}]", field, array[0]);
        }
    }

    // Add an indirection, as an extra test.
    interface AA extends AtomicAccess { }

    // A hardened, always atomic version of TPoint.
    static primitive class AAPoint implements AA {
        AAPoint(long x, long y) { this.x = x; this.y = y; }
        final long x, y;
        public String toString() { return String.format("(%d,%d)", x, y); }
    }

    class AAPointBox implements PointBox {
        AAPoint field;
        AAPoint[] array = new AAPoint[1];
        // Step the points forward by incrementing their components
        // "simultaneously".  A racing thread will catch flaws in the
        // simultaneity.
        AAPoint step(AAPoint p) {
            return new AAPoint(p.x + 1, p.y + 1);
        }
        public @Override
        void step() {
            field = step(field);
            array[0] = step(array[0]);
            check();
        }
        // Invariant:  The components of each point are "always" equal.
        public @Override
        void check() {
            check(field, "field");
            check(array[0], "array element");
        }
        void check(AAPoint p, String where) {
            if (p.x == p.y)  return;
            String msg = String.format("%s *AlwaysAtomic* %s in %s; settings = %s",
                                       NOTE_TORN_POINT,
                                       p, where, SETTINGS);
            throw new TooTearable(msg, p);
        }
        public String toString() {
            return String.format("AAPB[%s, {%s}]", field, array[0]);
        }
    }

    class AsyncObserver extends Thread {
        volatile boolean done;
        long observationCount;
        final PointBox pointBox;
        volatile Object badPointObserved;
        AsyncObserver(PointBox pointBox) {
            this.pointBox = pointBox;
        }
        public void run() {
            try {
                while (!done) {
                    observationCount++;
                    pointBox.check();
                }
            } catch (TooTearable ex) {
                done = true;
                badPointObserved = ex.badPoint;
                System.out.println(ex);
                if (ALWAYS_ATOMIC || ex.badPoint instanceof AtomicAccess) {
                    throw ex;
                }
            }
        }
    }

    public void run() throws Exception {
        System.out.println("Test for access atomicity of AAPoint, which must not be broken...");
        run(new AAPointBox(), false);
        System.out.println("Test for access atomicity of TPoint, which "+
                           (ALWAYS_ATOMIC ? "must not" : "is allowed to")+
                           " be broken...");
        run(new TPointBox(), ALWAYS_ATOMIC ? false : true);
    }
    public void run(PointBox pointBox, boolean nonAtomic) throws Exception {
        var observer = new AsyncObserver(pointBox);
        observer.start();
        for (int i = 0; i < STEP_COUNT; i++) {
            pointBox.step();
            if (observer.done)  break;
        }
        observer.done = true;
        observer.join();
        var obCount = observer.observationCount;
        var badPoint = observer.badPointObserved;
        System.out.println(String.format("finished after %d observations at %s; %s",
                                         obCount, pointBox,
                                         (badPoint == null
                                          ? "no access atomicity problems observed"
                                          : "bad point = " + badPoint)));
        if (nonAtomic && badPoint == null) {
            var complain = String.format("%s NOT observed after %d observations",
                                         NOTE_TORN_POINT, obCount);
            System.out.println("?????? "+complain);
            if (STEP_COUNT >= 3_000_000) {
                // If it's a small count, OK, but if it's big the test is broken.
                throw new AssertionError(complain + ", but it should have been");
            }
        }
        if (!nonAtomic && badPoint != null) {
            throw new AssertionError("should not reach here; other thread must throw");
        }
    }
}
