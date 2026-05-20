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
 */

import jdk.internal.misc.PreviewFeatures;
import jdk.internal.value.ValueClass;

// Test app for flat arrays in the AOT map
public class AOTMapTestApp {

    public static value class Wrapper implements Comparable<Wrapper> {
        Integer i;

        public String toString() {
            return i.toString();
        }

        public int compareTo(Wrapper o) {
            return i - o.i;
        }

        Wrapper(int i) {
            this.i = new Integer(i);
        }
    }

    // Check that arrays of both migrated value
    // classes and custom value classes are archived
    public static class ArchivedData {
        Wrapper[] wrapperArray;
    }

    static ArchivedData archivedObjects;
    static {
        if (archivedObjects == null) {
            archivedObjects = new ArchivedData();
            archivedObjects.wrapperArray = new Wrapper[3];
            archivedObjects.wrapperArray[0] = new Wrapper(0);
            archivedObjects.wrapperArray[1] = new Wrapper(1);
            archivedObjects.wrapperArray[2] = new Wrapper(2);
        } else {
            System.out.println("Initialized from CDS");
            System.out.println("wrapperArray " + archivedObjects.wrapperArray);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Hello FlatAOTMapTestApp");
        Class.forName("Hello");
        if (PreviewFeatures.isEnabled() && !ValueClass.isFlatArray(archivedObjects.wrapperArray)) {
            throw new RuntimeException("Wrapper array should be flat");
        }
    }
}
