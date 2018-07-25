/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;

class Parent {
    Point foo() {
	return Point.createPoint();
    }
}

interface InterfaceWithDefault {
    default Point foo() {
        return Point.createPoint();
    }
}

class Provider {
    static Point getPoint() {
	return Point.createPoint();
    }
    static void foo(Point[] array) { }

    static void processPoint(Point p) { }
}

// It's OK to declare a Flattenable field of a mismatched type, but only from a legacy class that doesn't know Point is a VT.
@LegacyPositiveTest
class FlattenableField {
    static class Helper {
        // test assumes __Flattenable is the default for non static fields
        Point p;
    }
    public static void run() {
        // Helper will fail to load if it's not a legacy class (i.e., it declares that Point is a VT, but
        // point ends up not being a VT.
        new Helper();
    }
}

// It's OK to declare a NonFlattenable field of a mismatched type
@PositiveTest
class NotFlattenableField {
    __NotFlattened Point p;
    public static void run() {};
}

// It's OK to declare a NonFlattenable static field of a mismatched type
@PositiveTest
class NotFlattenableStaticField {
    __NotFlattened static Point p;
    public static void run() {}
}

// It's OK to declare a NonFlattenable static field of a mismatched type
@PositiveTest
class ValueArrayField {
    Point[]  p;
    public static void run() {};
}

// Cannot resolve the Point class if your idea of whether it's a ValueType is wrong.
@NegativeTest
class ResolveClass {
    public static void run() {
        Class c = Point.class;
	//TODO Point p = Point.createPoint();
    }
}

// It's NOT OK to resolve the array class of Point even if your idea of whether it's a ValueType is wrong.
@NegativeTest
class ResolveArray {
    public static void run() {
	Point[] array = new Point[5];
    }
}

// It's NOT OK to resolve a higher-dimensional array class of Point even if your idea of whether it's a ValueType is wrong.
@NegativeTest
class ResolveMultiArray {
    public static void run() {
	Point[][] array = new Point[5][4];
    }
}

// Cannot resolve a method in the Point class if your idea of whether it's a ValueType is wrong.
@NegativeTest
class ResolveMethod {
    public static void run() {
	Point.createPoint();
    }
}

// It's OK for a legacy class to declare (but not use) a method whose signature contains a mismatched VT.
@LegacyPositiveTest
class LocalMethod {
    static class Helper {
        Point foo() {
            return Point.createPoint();
        }
    }
    public static void run() {
        new Helper();
    }
}

// It's OK for a legacy class to declare (but not use) a method whose signature contains a array of a mismatched VT.
@LegacyPositiveTest
class LocalMethodWithArray {
    static class Helper {
        void foo(Point[] array) { }
    }
    public static void run() {
        new Helper();
    }
}


// Two legacy classes can call each other's methods, as long as both of them
// have the wrong understanding of whether Point is a VT.
@LegacyPositiveTest
class BothWrongRemoteMethod {
    static __NotFlattened Point p; // to get a null value
    public static void run() {
	Provider.processPoint(p);
    }
}

// helper class used by InvokeMethodHandle (defined in ConsistencyTest.java).
class InvokeMethodHandle_mismatched {
    static void test(MethodHandle mh) throws Throwable {
        Point p = (Point)mh.invoke();
        System.out.println("(should not get here) Point = " + p);
    }
}
