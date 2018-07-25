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

/**
 * @test
 * @summary Test for value types consistency checks
 *
 * @comment CASE [A] ConsistencyTest.java uses Point as a VT, but Mismatched.java uses Point as a POJO Type.
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers  ConsistencyTest.java POJOPoint.java Mismatched.java
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers  ConsistencyTest.java ValuePoint.java
 * @run main/othervm -Xint  -XX:+EnableValhalla ConsistencyTest
 * @run main/othervm -Xcomp -XX:+EnableValhalla ConsistencyTest
 *
 * @comment CASE [B] ConsistencyTest.java uses Point as a POJO Type, but Mismatched.java uses Point as a VT.
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers  ConsistencyTest.java ValuePoint.java Mismatched.java
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers  ConsistencyTest.java POJOPoint.java
 * @run main/othervm -Xint  -XX:+EnableValhalla ConsistencyTest
 * @run main/othervm -Xcomp -XX:+EnableValhalla ConsistencyTest
 */

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.*;


// !!! NOTE !!!
// ConsistentTest.java and Mismatched.java are compiled separately, such that the classes in the two files
// will have the exact opposite understanding of whether Point is a VT.

// Each test case for ConsistencyTest is written as a separate class, which must have exactly
// one of the following 3 annotations: @NegativeTest, @PositiveTest or @LegacyPositiveTest.

// The test case must always fail with an ICCE.
@Retention(RetentionPolicy.RUNTIME)
@interface NegativeTest {}

// The test case must always succeed.
@Retention(RetentionPolicy.RUNTIME)
@interface PositiveTest {}

// The test case is for a "legacy" class, which doesn't know that Point is a VT. Some
// operations by such legacy classes are allowed (such as declaring, but not using, a method
// whose signature includes LPoint;). See LocalMethod in Mismatched.java for an example
@Retention(RetentionPolicy.RUNTIME)
@interface LegacyPositiveTest {}

public class ConsistencyTest {
    public static void main(String[] args) {
        // Resolve this class now, before some of the test classes try to declare
        // a field or parameter of this type.
        Class pointClass = Point.class;
        boolean isVT = pointClass.isValue();
        System.out.println("Is class " + pointClass + " a ValueType? " + (isVT ? "YES" : "NO"));

        // Run with "jtreg -DNonVTOnly ConsistencyTest.java", etc, to select a test scenario.
        if (isVT && (System.getProperty("NonVTOnly") != null)) {
            System.out.println("-DNonVTOnly is specified. Test skipped");
            return;
        }
        if (!isVT && (System.getProperty("VTOnly") != null)) {
            System.out.println("-DVTOnly is specified. Test skipped");
            return;
        }

        test(
             "InvokeMethodHandle",
             "FlattenableField",
             "NotFlattenableField",
             "NotFlattenableStaticField",
             "ValueArrayField",
             "ResolveClass",
             "ResolveArray",
             "ResolveMultiArray",
             "ResolveMethod",
             "LocalMethod",
             "LocalMethodWithArray",
             "LocalOverridingMethod",
             "LocalOverridingDefaultMethod",
             "RemoteMethod",
             "RemoteMethodNull",
             "RemoteMethodWithArray",
             "BothWrongRemoteMethod");
    }
    
    public static void test(String...tests) {
        List<String> failed = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();
        Arrays.sort(tests);

	for (String name : tests) {
            try {
                testOne(name);
            } catch (Throwable t) {
                failed.add(name);
                exceptions.add(t);
            }
        }
        if (failed.size() > 0) {
            System.out.println("-----------------------------------------");
            for (int i=0; i<failed.size(); i++) {
                String name = failed.get(i);
                Throwable t = exceptions.get(i);
                System.out.println("*** FAILED: " + name);
                t.printStackTrace(System.out);
            }

            if (System.getProperty("IgnoreFailures") != null) {
                System.out.println("-DIgnoreFailures is specified -- I am ignoring the failures so you can test other scenarios");
            } else {
                throw new RuntimeException(failed.size() + " test case(s) have failed");
            }
        }
    }

    static void testOne(String name) {
        System.out.println("Testing " + name);
        Class c;
        Method m;
        try {
            c = Class.forName(name);
            m = c.getMethod("run");
        } catch (IncompatibleClassChangeError  e) {
            // If you are testing a class that's suppose to throw an ICCE during class
            // loading, do that in a helper class. See LocalOverridingMethod for an example.
            throw new RuntimeException("Unexpected IncompatibleClassChangeError. Please use a helper class.", e);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("Unexpected Exception", e);
        }
        
        int count = 0;
        Throwable ICCE = null;
        boolean expectICCE = false;
        if (c.getAnnotation(NegativeTest.class) != null) {
            count ++;
            expectICCE = true;
        }
        if (c.getAnnotation(PositiveTest.class) != null) {
            count ++;
            expectICCE = false;
        }
        if (c.getAnnotation(LegacyPositiveTest.class) != null) {
            count ++;
            if (!Point.class.isValue()) {
                // The classes in this file think that Point is NOT a VT.
                // We have loaded a Point class that is NOT a VT.
                // - however -
                // In this test case, are calling a class X (defined in Mismatched.java)
                // that uses Point as a VT. The operation in X must fail with an ICCE.
                expectICCE = true;
            } else {
                expectICCE = false;
            }
        }
        if (count != 1) {
            throw new RuntimeException("Class " + name + " must have exactly one annotation of" +
                                       " @NegativeTest, @PositiveTest, or @LegacyPositiveTest");
        }

        try {
            m.invoke(null);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Unexpected Exception", e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            while (t != null) {
                if (t instanceof IncompatibleClassChangeError) {
                    ICCE = t;
                }
                t = t.getCause();
            }
        }

        if (ICCE != null) {
            if (!expectICCE) {
                ICCE.printStackTrace();
                throw new RuntimeException("FAILED: " + name + " received ICCE unexpectedly", ICCE);
            }
            System.out.println("    ICCE = " + ICCE);
        }
        if  (ICCE == null) {
            if (expectICCE) {
                throw new RuntimeException("FAILED: " + name + " did not receive ICCE as expected");
            }
            System.out.println("    ICCE = <none>");
        }
    }
}

// This class has a *correct* notion of whether Point is a VT
class CorrectProvider {
    static __NotFlattened Point p; // to get a null value

    static Point getPoint() {
        return Point.createPoint();
    }
    static Point getNullPoint() {
        return p;
    }
    static Point[] getPointArray() {
        return new Point[10];
    }
}

//----------------------------------------------------------------------
// Here are the test cases. More test cases are in Mismatched.java
//----------------------------------------------------------------------

// It must not be possible to override a method with a mismatched type
@NegativeTest
class LocalOverridingMethod {
    static class Helper extends Parent {
        Point foo() {
            return Point.createPoint();
        }
    }
    public static void run() {
        new Helper();
    }
}

// It must not be possible to override a default method with a mismatched type
@NegativeTest
class LocalOverridingDefaultMethod {
    static class Helper implements InterfaceWithDefault {
        public Point foo() {
            return Point.createPoint();
        }
    }
    public static void run() {
        new Helper();
    }
}

// Cannot call a method whose signature contains a mismatched VT.
@NegativeTest
class RemoteMethod {
    public static void run() {
	Point p = Provider.getPoint();
    }
}

// Cannot call a method whose parameter is a mismatched VT, even if you are passing null.
@NegativeTest
class RemoteMethodNull {
    static __NotFlattened Point p; // to get a null value
    public static void run() {
	Provider.processPoint(p);
    }
}

// Cannot call a method whose signature contains an array of a mismatched VT, even if you are passing null.
@NegativeTest
class RemoteMethodWithArray {
    public static void run() {
	Provider.foo(null);
    }
}

// You shouldn't be able to invoke a method defined by a class who has a different idea of whether Point is VT.
@NegativeTest
class InvokeMethodHandle {
    public static void run() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(Point.class);
        MethodHandle mh = lookup.findStatic(CorrectProvider.class, "getNullPoint", mt);
        InvokeMethodHandle_mismatched.test(mh);
    }
}
