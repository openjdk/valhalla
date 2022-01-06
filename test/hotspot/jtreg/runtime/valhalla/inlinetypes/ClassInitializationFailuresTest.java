/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

/*
* @test
* @summary Test several scenarios of class initialization failures
* @library /test/lib
* @run main runtime.valhalla.inlinetypes.ClassInitializationFailuresTest

*/
public class ClassInitializationFailuresTest {
    static boolean failingInitialization = true;
    static Object bo = null;

    static primitive class BadOne {
        int i = 0;
        static {
            if (ClassInitializationFailuresTest.failingInitialization) {
                throw new RuntimeException("Failing initialization");
            }
        }
    }

    static primitive class TestClass1 {
        BadOne badField = new BadOne();
    }

    // Test handling of errors during the initialization of a primitive class
    // Initialization of TestClass1 triggers the initialization of classes
    // of all its primitive class typed fields, in this case BadOne
    // Static initializer of BadOne throws an exception, so BadOne's initialization
    // fails, which must caused the initialization of TestClass1 to fail too
    // First attempt to instantiate TestClass1 must fail with an ExceptionInInitializerError
    // because an exception has been thrown during the initialization process
    // Second attempt to instantiate TestClass1 must fail with a NoClassDefFoundError
    // because TestClass1 must already be in a failed initialization state (so no new
    // attempt to initialize the class)
    static void testClassInitialization() {
        Throwable e = null;
        try {
            TestClass1 t1 = new TestClass1();
        } catch(Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Exception should have been thrown");
        Asserts.assertTrue(e.getClass() == ExceptionInInitializerError.class, "Must be an ExceptionInInitializerError");
        Asserts.assertTrue(e.getCause().getClass() == RuntimeException.class, "Must be the exception thown in the static initializer of BadOne");
        // Second attempt because it doesn't fail the same way
        e = null;
        try {
            TestClass1 t1 = new TestClass1();
        } catch(Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error should have been thrown");
        Asserts.assertTrue(e.getClass() == NoClassDefFoundError.class, "Must be a NoClassDefFoundError");
        Asserts.assertTrue(e.getCause().getClass() == ExceptionInInitializerError.class, "Must be an ExceptionInInitializerError");
    }

    static primitive class BadTwo {
        int i = 0;
        static {
            if (ClassInitializationFailuresTest.failingInitialization) {
                throw new RuntimeException("Failing initialization");
            }
        }
    }

    static primitive class BadThree {
        int i = 0;
        static {
            if (ClassInitializationFailuresTest.failingInitialization) {
                throw new RuntimeException("Failing initialization");
            }
        }
    }

    // Same test as above, but for arrays of primitive objects
    static void testArrayInitialization() {
        // Testing anewarray when the primitive element class fails to initialize properly
        Throwable e = null;
        try {
            BadTwo[] array = new BadTwo[10];
        } catch(Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error should have been thrown");
        Asserts.assertTrue(e.getClass() == ExceptionInInitializerError.class, " Must be an ExceptionInInitializerError");
        // Second attempt because it doesn't fail the same way
        try {
            BadTwo[] array = new BadTwo[10];
        } catch(Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error should have been thrown");
        Asserts.assertTrue(e.getClass() == NoClassDefFoundError.class, "Must be a NoClassDefFoundError");
        Asserts.assertTrue(e.getCause().getClass() == ExceptionInInitializerError.class, "Must be an ExceptionInInitializerError");
        // Testing multianewarray when the primitive element class fails to initialize properly
        try {
            BadThree[][] array = new BadThree[10][20];
        } catch(Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error should have been thrown");
        Asserts.assertTrue(e.getClass() == ExceptionInInitializerError.class, " Must be an ExceptionInInitializerError");
        // Second attempt because it doesn't fail the same way
        try {
            BadThree[][][] array = new BadThree[10][30][10];
        } catch(Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error should have been thrown");
        Asserts.assertTrue(e.getClass() == NoClassDefFoundError.class, "Must be a NoClassDefFoundError");
        Asserts.assertTrue(e.getCause().getClass() == ExceptionInInitializerError.class, "Must be an ExceptionInInitializerError");
    }

    static primitive class BadFour {
        int i = 0;
        static BadFour[] array;
        static {
            array = new BadFour[10];
            if (ClassInitializationFailuresTest.failingInitialization) {
                throw new RuntimeException("Failing initialization");
            }
        }
    }

    // Even if a primitive class fails to initialize properly, some instances
    // of this class can escape and be accessible. The JVM must be able to
    // deal with those instances without crashes. The test below checks that
    // escaped values stored in an array are handled correctly
    static void testEscapedValueInArray() {
        Throwable e = null;
        try {
            BadFour bt = new BadFour();
        } catch (Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error must have been thrown");
        Asserts.assertTrue(e.getClass() == ExceptionInInitializerError.class, " Must be an ExceptionInInitializerError");
        e = null;
        try {
            BadFour t = BadFour.array[0];
        } catch(Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error should have been thrown");
        Asserts.assertTrue(e.getClass() == NoClassDefFoundError.class, "Must be a NoClassDefFoundError");
        Asserts.assertTrue(e.getCause().getClass() == ExceptionInInitializerError.class, "Must be an ExceptionInInitializerError");
    }

    static primitive class BadFive {
        int i = 0;
        static {
            ClassInitializationFailuresTest.bo = new BadSix();
            if (ClassInitializationFailuresTest.failingInitialization) {
                throw new RuntimeException("Failing initialization");
            }
        }
    }

    static class BadSix {
        BadFive bf = new BadFive();
    }

    // Same test as above, but escaped values are stored in an object
    static void testEscapedValueInObject() {
        Throwable e = null;
        try {
            BadSix bt = new BadSix();
        } catch (Throwable t) {
            e = t;
        }
        Asserts.assertNotNull(e, "Error must have been thrown");
        Asserts.assertNotNull(ClassInitializationFailuresTest.bo, "bo object should have been set");
        BadFive bf = ((BadSix)ClassInitializationFailuresTest.bo).bf;
    }

    public static void main(String[] args) {
        testClassInitialization();
        testArrayInitialization();
        testEscapedValueInArray();
        testEscapedValueInObject();
    }
}
