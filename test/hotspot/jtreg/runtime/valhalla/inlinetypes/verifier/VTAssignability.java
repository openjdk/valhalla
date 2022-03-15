/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test basic verifier assignability of inline types.
 * @run main/othervm -Xverify:remote VTAssignability
 */

// Test that an inline type is assignable to itself, to java.lang.Object,
// and to an interface,
//
interface II { }

public primitive final class VTAssignability implements II {
    final int x;
    final int y;

    public VTAssignability(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public boolean isSameVTAssignability(VTAssignability that) {
        return this.getX() == that.getX() && this.getY() == that.getY();
    }

    public boolean equals(Object o) {
        if(o instanceof VTAssignability) {
            return ((VTAssignability)o).x == x &&  ((VTAssignability)o).y == y;
        } else {
            return false;
        }
    }

    public void takesInterface(II i) {
        System.out.println("Test passes!!");
    }

    public static void main(String[] args) {
        VTAssignability a = new VTAssignability(3, 4);
        VTAssignability b = new VTAssignability(2, 4);

        // Test assignability of an inline type to itself.
        boolean res = a.isSameVTAssignability(b);

        // Test assignability of an inline type to java.lang.Object.
        res = b.equals(a);

        // Test assignability of an inline type to an interface.
        a.takesInterface(b);
    }
}
