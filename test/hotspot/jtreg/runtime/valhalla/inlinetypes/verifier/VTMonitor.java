/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8227373
 * @summary Test that verifier allows monitor operations on inline types.
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowWithFieldOperator VTMonitor.java
 * @run main/othervm -Xverify:remote VTMonitor
 */

public inline final class VTMonitor {
    final int x;
    final int y;

    private VTMonitor() {
        x = 0;
        y = 0;
    }

    public static VTMonitor createVTMonitor(int x, int y) {
        VTMonitor p = VTMonitor.default;
        p = __WithField(p.x, x);
        p = __WithField(p.y, y);
        return p;
    }

    public static void main(String[] args) {
        Object a = createVTMonitor(3, 4);
        try {
            synchronized(a) {
                throw new RuntimeException("Synchronization on inline type should fail");
            }
        } catch (java.lang.IllegalMonitorStateException e) {
            // Expected
        }
    }
}
