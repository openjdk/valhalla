/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import jdk.internal.value.ValueClass;

public class ArchivedFlatArrayApp {
    static Object[] archivedObjects;
    static {
    if (archivedObjects == null) {
        System.out.println("Not archived");
        archivedObjects = new Integer[3];
        archivedObjects[0] = new Integer(0);
        archivedObjects[1] = new Integer(1);
        archivedObjects[2] = new Integer(2);
    } else {
        System.out.println("Initialized from CDS");
    }

      for (Object o : archivedObjects) {
        System.out.println(o);
      }
    }

    public static void main(String[] args) {
        if (!ValueClass.isFlatArray(archivedObjects)) {
            throw new RuntimeException("Should be flat");
        }

        System.out.println("PASSED");
    }
}

class ArchivedFlatArrayApp2 {
    public static CharPair[] archivedObjects;
    static {
        if (archivedObjects == null) {
            System.out.println("Not archived");
            archivedObjects = new CharPair[3];
            archivedObjects[0] = new CharPair('a', 'b');
            archivedObjects[1] = new CharPair('c', 'd');
            archivedObjects[2] = new CharPair('e', 'f');
        } else {
            System.out.println("Initialized from CDS");
        }

        for (Object o : archivedObjects) {
            System.out.println(o);
        }
    }

    public static value class CharPair {
        char c0, c1;

        public String toString() {
            return "(" + c0 + ", " + c1 + ")";
        }

        public CharPair(char c0, char c1) {
            this.c0 = c0;
            this.c1 = c1;
        }
    }

    public static void main(String[] args) {
        if (!ValueClass.isFlatArray(archivedObjects)) {
            throw new RuntimeException("Should be flat");
        }

        System.out.println("PASSED");
    }
}
