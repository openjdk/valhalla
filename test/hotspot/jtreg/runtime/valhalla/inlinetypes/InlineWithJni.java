/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary test JNI functions with inline types
 * @run main/native runtime.valhalla.inlinetypes.InlineWithJni
 */
public primitive final class InlineWithJni {

    static {
        System.loadLibrary("InlineWithJni");
    }

    public static void main(String[] args) {
        testJniMonitorOps();
    }

    final int x;

    public InlineWithJni(int x) {
        this.x = x;
    }

    public native void doJniMonitorEnter();
    public native void doJniMonitorExit();

    public static void testJniMonitorOps() {
        boolean sawImse = false;
        try {
            new InlineWithJni(0).doJniMonitorEnter();
        } catch (Throwable t) {
            sawImse = checkImse(t);
        }
        if (!sawImse) {
            throw new RuntimeException("JNI MonitorEnter did not fail");
        }
        sawImse = false;
        try {
            new InlineWithJni(0).doJniMonitorExit();
        } catch (Throwable t) {
            sawImse = checkImse(t);
        }
        if (!sawImse) {
            throw new RuntimeException("JNI MonitorExit did not fail");
        }
    }

    static boolean checkImse(Throwable t) {
        if (t instanceof IllegalMonitorStateException) {
            return true;
        }
        throw new RuntimeException(t);
    }
}
