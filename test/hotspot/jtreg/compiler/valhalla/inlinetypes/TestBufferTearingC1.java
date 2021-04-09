/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Arm Limited. All rights reserved.
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

package compiler.valhalla.inlinetypes;

/**
 * @test TestBufferTearingC1
 * @key randomness
 * @summary Additional tests for C1 missing barriers when buffering inline types.
 * @run main/othervm -XX:InlineFieldMaxFlatSize=-1 -XX:FlatArrayElementMaxSize=0
 *                   -XX:TieredStopAtLevel=1
 *                   compiler.valhalla.inlinetypes.TestBufferTearingC1
 * @run main/othervm -XX:InlineFieldMaxFlatSize=0 -XX:FlatArrayElementMaxSize=-1
 *                   -XX:TieredStopAtLevel=1
 *                   compiler.valhalla.inlinetypes.TestBufferTearingC1
 */

primitive class Point {
    public final int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

primitive class Rect {
    public final Point a, b;

    public Rect(Point a, Point b) {
        this.a = a;
        this.b = b;
    }
}

public class TestBufferTearingC1 {

    public static Point[] points = new Point[] { new Point(1, 1) };
    public static Point point = new Point(1, 1);

    static volatile boolean running = true;

    public static void writePoint(int iter) {
        Rect r = new Rect(new Point(iter, iter), new Point(iter + 1, iter + 1));
        point = points[0];  // Indexed load of flattened array (when FlatArrayElementMaxSize != 0)
        points[0] = r.a;    // Load from flattened field (when InlineFieldMaxFlatSize != 0)
    }

    private static void checkMissingBarrier() {
        while (running) {
            // When FlatArrayElementMaxSize == 0 the "buffered" reference
            // created by the load from the flattened field `r.a' will be
            // stored directly in the array at `points[0]'.  It should not be
            // possible to read through this reference and see the
            // intermediate zero-initialised state of the object (i.e. there
            // should be a store-store barrier after copying the flattened
            // field contents before the store that publishes it).
            if (points[0].x == 0 || points[0].y == 0) {
                throw new IllegalStateException();
            }

            // Similarly, when InlineFieldMaxFlatSize == 0 the buffered
            // reference created by the indexed load from the flattened array
            // `points[0]' will be stored directly in the field `points'.  It
            // should not be possible to read through this reference and see
            // the intermediate zero-initialised state of the object.
            if (point.x == 0 || point.y == 0) {
                throw new IllegalStateException();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(TestBufferTearingC1::checkMissingBarrier);
            threads[i].start();
        }

        for (int i = 1; i < 1_000_000; i++) {
            writePoint(i);
        }

        running = false;

        for (int i = 0; i < 10; i++) {
            threads[i].join();
        }
    }
}
