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

package runtime.valhalla.typerestrictions;

import java.lang.invoke.RestrictedType;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Testing type restrictions on method arguments and return value
 * @library /test/lib
 * @compile RestrictedMethodTest.java
 * @run main/othervm -Xint runtime.valhalla.typerestrictions.RestrictedMethodTest
 */

interface RMInterface {
  default public void imethod(@RestrictedType("Qruntime/valhalla/typerestrictions/RestrictedMethodTest$Point;") Object o) {
    System.out.println("imethod says "+o);
  }
}

public class RestrictedMethodTest implements RMInterface {
  static inline class Point {
    int x = 0;
    int y = 0;
  }

  void oneRestrictedArgument(@RestrictedType("Qruntime/valhalla/typerestrictions/RestrictedMethodTest$Point;") Object o) {
    System.out.println("oneRestrictedArgument says " + o);
  }

  void twoRestrictedArguments(@RestrictedType("Qruntime/valhalla/typerestrictions/RestrictedMethodTest$Point;") Object o0,
                             @RestrictedType("Qruntime/valhalla/typerestrictions/RestrictedMethodTest$Point;") Object o1) {
    System.out.println("twoRestrictedArguments says " + o0 + " " + o1);
  }

  void secondArgumentRestricted(long l, @RestrictedType("Qruntime/valhalla/typerestrictions/RestrictedMethodTest$Point;") Object o) {
    System.out.println("secondArgumentRestricted says " + l + " " + o);
  }

  static void staticWithOneRestrictedArgument(@RestrictedType("Qruntime/valhalla/typerestrictions/RestrictedMethodTest$Point;") Object o) {
    System.out.println("staticWithOneRestrictedArgument says " + o);
  }

  static @RestrictedType("Qruntime/valhalla/typerestrictions/RestrictedMethodTest$Point;") Object restrictedReturnValue(Object o) {
    System.out.println("Returning "+o);
    return o;
  }

  static void testRMInterface(RMInterface i) {
    Point p = new Point();
    Throwable result = null;
    try {
      i.imethod(p);
    } catch(Throwable t) {
      result = t;
    }
    expectNoError(result);
    try {
      i.imethod(new Object());
    } catch(Throwable t) {
      result = t;
    }
    expectICC(result);
    result = null;
  }
  static void expectICC(Throwable t) {
    Asserts.assertNotNull(t, "An error should have been thrown");
    Asserts.assertTrue(t instanceof java.lang.IncompatibleClassChangeError, "Wrong error type");
  }

  static void expectNoError(Throwable t) {
    Asserts.assertNull(t, "No Error should have been thrown");
  }
  public static void main(String[] args) {
    Point p = new Point();
    RestrictedMethodTest test = new RestrictedMethodTest();
    Throwable result = null;
    try {
      test.oneRestrictedArgument(p);
    }  catch(Throwable t) {
      result = t;
    }
    expectNoError(result);
    try {
        test.oneRestrictedArgument(new Object());
    } catch(Throwable t) {
        result = t;
    }
    expectICC(result);
    result = null;
    try {
      test.twoRestrictedArguments(p, new Point());
    } catch(Throwable t) {
      result = t;
    }
    expectNoError(result);
    try {
      test.twoRestrictedArguments(new Object(), new Point());
    } catch(Throwable t) {
      result = t;
    }
    expectICC(result);
    result = null;
    try {
      test.twoRestrictedArguments(p, new Object());
    } catch(Throwable t) {
      result = t;
    }
    expectICC(result);
    result = null;
    try {
      test.secondArgumentRestricted(42L, p);
    } catch(Throwable t) {
      result = t;
    }
    expectNoError(result);
    try {
      test.secondArgumentRestricted(42L, new String("Duke"));
    } catch(Throwable t) {
      result = t;
    }
    expectICC(result);
    result = null;
    // Is code robust againt null receiver?
    RestrictedMethodTest test2 = null;
    try {
        test2.oneRestrictedArgument(new Object());
    } catch(Throwable t) {
        result = t;
    }
    Asserts.assertNotNull(result, "An NPE should have been thrown");
    Asserts.assertTrue(result instanceof java.lang.NullPointerException, "Wrong exception type");
    result = null;
    try {
      staticWithOneRestrictedArgument(p);
    } catch(Throwable t) {
      result = t;
    }
    expectNoError(result);
    try {
        staticWithOneRestrictedArgument(new Object());
    } catch(Throwable t) {
        result = t;
    }
    expectICC(result);
    result = null;
    testRMInterface(test);
    try {
      restrictedReturnValue(new Point());
    } catch(Throwable t) {
      result = t;
    }
    expectNoError(result);
    try {
        restrictedReturnValue(new Object());
    } catch(Throwable t) {
        result = t;
    }
    expectICC(result);
    result = null;
  }
}
