/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.NullRestricted;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;



/*
 * @test
 * @summary Test of ImplicitlyConstructible, NullRestricted and LooselyConsistentValue annotations
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.vm.annotation
 * @library /test/lib
 * @enablePreview
 * @compile AnnotationsTests.java
 * @run main/othervm AnnotationsTests
 */


 public class AnnotationsTests {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    public static void main(String[] args) {
        AnnotationsTests tests = new AnnotationsTests();
        Class c = tests.getClass();
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().startsWith("test_")) {
                try {
                    System.out.println("Running " + m.getName());
                  m.invoke(tests);
                } catch (Throwable t) {
                  t.printStackTrace();
                  throw new RuntimeException(t);
                }
            }
        }
    }

    static class BadClass0 {
        @NullRestricted
        String s;
    }

    // Test detection of illegal usage of NullRestricted on an identity field
    void test_0() {
        Throwable exception = null;
        try {
            BadClass0 bc = new BadClass0();
        } catch (IncompatibleClassChangeError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Failed to detect illegal use of @NullRestricted");
    }

    // Test detection of mismatch between a @NullRestricted field and its class that is not @ImplicitlyConstructible
    static value class ValueClass1 {
        int i = 0;
        int j = 0;
    }

    static class BadClass1 {
        @NullRestricted
        ValueClass1 vc;
    }

    void test_1() {
        Throwable exception = null;
        try {
            BadClass1 tc = new BadClass1();
        } catch (IncompatibleClassChangeError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Failed to detect illegal use of @NullRestricted");
    }

    // Test a valid @NullRestricted field with a class that is @ImplicitlyConstructible
    @ImplicitlyConstructible
    static value class ValueClass2 {
        int i = 0;
        int j = 0;
    }

    static class GoodClass2 {
        @NullRestricted
        ValueClass2 vc;
    }

    void test_2() {
        Throwable exception = null;
        try {
            GoodClass2 tc = new GoodClass2();
        } catch (IncompatibleClassChangeError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNull(exception, "Unexpected exception: " + exception);
    }

    // Invalid usage of @ImplicitlyConstructible on an identity class
    @ImplicitlyConstructible
    static class BadClass3 {

    }

    void test_3() {
        Throwable exception = null;
        try {
            BadClass3 tc = new BadClass3();
        } catch (ClassFormatError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Failed to detect illegal use of @ImplicitlyConstructible");
    }

    // Test invalid usage of @LooselyConsistentValue on an identity class
    @LooselyConsistentValue
    static class BadClass4 {

    }

    void test_4() {
        Throwable exception = null;
        try {
            BadClass4 tc = new BadClass4();
        } catch (ClassFormatError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Failed to detect illegal use of @LooselyConsistentValue");
    }

    // Test field flattening of @NullRestricted annotated fields

    @ImplicitlyConstructible
    static value class ValueClass5 {
      int i = 0;
    }

    static class GoodClass5 {
      ValueClass5 f0 = new ValueClass5();

      @NullRestricted
      ValueClass5 f1 = new ValueClass5();
    }

    void test_5() {
        Throwable exception = null;
        try {
            GoodClass5 vc = new GoodClass5();
            Field f0 = vc.getClass().getDeclaredField("f0");
            Asserts.assertFalse(UNSAFE.isFlattened(f0), "Unexpected flat field");
            Field f1 = vc.getClass().getDeclaredField("f1");
            Asserts.assertTrue(UNSAFE.isFlattened(f1), "Flat field expected, but field is not flat");
        } catch (IncompatibleClassChangeError e) {
            exception = e;
            System.out.println("Received " + e);
        } catch(NoSuchFieldException e) {
            Asserts.fail("Test error");
        }
        Asserts.assertNull(exception, "Unexpected exception: " + exception);
    }


    // Test detection/handling of circularity

    @ImplicitlyConstructible
    static value class ValueClass6a {
        @NullRestricted
        ValueClass6b val = new ValueClass6b();
    }

    @ImplicitlyConstructible
    static value class ValueClass6b {
        @NullRestricted
        ValueClass6a val = new ValueClass6a();
    }

    static class BadClass6 {
        @NullRestricted
        ValueClass6a val = new ValueClass6a();
    }

    void test_6() {
        Throwable exception = null;
        try {
            BadClass6 bc = new BadClass6();
        } catch (ClassCircularityError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Failed to detect circularity");
    }

    // Test null restricted static field
    @ImplicitlyConstructible
    static value class ValueClass7 {
        int i = 0;
    }

    static class GoodClass7 {
        @NullRestricted
        static ValueClass7 sval;
    }

    void test_7() {
        Throwable exception = null;
        try {
            ValueClass7 val = GoodClass7.sval;
            Asserts.assertNotNull(val, "Unexpected null value");
        } catch (Throwable e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNull(exception, "Unexpected exception: " + exception);
    }

    // Test circularity on static fields
    @ImplicitlyConstructible
    static value class ValueClass8 {
        @NullRestricted
        static ValueClass8 sval;
    }

    void test_8() {
        Throwable exception = null;
        try {
            ValueClass8 val = ValueClass8.sval;
            Asserts.assertNotNull(val, "Unexpected null value");
        } catch (Throwable e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNull(exception, "Unexpected exception: " + exception);
    }

    // Test that writing null to a @NullRestricted non-static field throws an exception
    @ImplicitlyConstructible
    static value class ValueClass9 {
        int i = 0;
    }

    static class GoodClass9 {
        @NullRestricted
        ValueClass9 val = new ValueClass9();
    }

    void test_9() {
        Throwable exception = null;
        try {
            GoodClass9 gc = new GoodClass9();
            gc.val = null;
        } catch(NullPointerException e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Expected NullPointerException not received");
    }

    // Test that writing null to a @NullRestricted static field throws an exception
    @ImplicitlyConstructible
    static value class ValueClass10 {
        @NullRestricted
        static ValueClass10 sval;
    }

    void test_10() {
        Throwable exception = null;
        try {
            ValueClass10.sval = null;
        } catch(NullPointerException e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Expected NullPointerException not received");
    }

    // Test uninitialized static null restricted field with a class not implicitly constructible
    static value class ValueClass11 {
        int i = 0;
        int j = 0;
    }

    static class BadClass11 {
        @NullRestricted
        static ValueClass11 val;
    }

    void test_11() {
        Throwable exception = null;
        try {
            ValueClass11 val = BadClass11.val;
            System.out.println(val);
        } catch(IncompatibleClassChangeError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Expected IncompatibleClassChangerError not received");
    }

    // Test illegal use of @NullRestricted on a primitive field
    static class BadClass12 {
        @NullRestricted
        int i;
    }
    void test_12() {
        Throwable exception = null;
        try {
            BadClass12 val = new BadClass12();
            System.out.println(val);
        } catch(ClassFormatError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Expected ClassFormatError not received");
    }

    // Test illegal use of @NullRestricted on an array field
    static class BadClass13 {
        @NullRestricted
        int Integer[];
    }
    void test_13() {
        Throwable exception = null;
        try {
            BadClass13 val = new BadClass13();
            System.out.println(val);
        } catch(ClassFormatError e) {
            exception = e;
            System.out.println("Received " + e);
        }
        Asserts.assertNotNull(exception, "Expected ClassFormatError not received");
    }

 }

