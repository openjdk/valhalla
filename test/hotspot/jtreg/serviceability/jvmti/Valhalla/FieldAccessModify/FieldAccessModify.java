/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests that all FieldAccess and FieldModification notifications
            are generated for primitive classes.
 * @requires vm.jvmti
 * @compile FieldAccessModify.java
 * @run main/othervm/native -agentlib:FieldAccessModify FieldAccessModify
 */

import java.lang.reflect.Field;
import java.util.Arrays;


public class FieldAccessModify {

    private static final String agentLib = "FieldAccessModify";

    private static primitive class MyPrimitive {
        public int MyPrimitive_fld1;
        public int MyPrimitive_fld2;

        public MyPrimitive(int v1, int v2) { MyPrimitive_fld1 = v1; MyPrimitive_fld2 = v2; }

        public static MyPrimitive create(int v1, int v2) {
            return new MyPrimitive(v1, v2);
        }

        public String toString() {
            return "MyPrimitive { fld1=" + MyPrimitive_fld1 + ", fld2=" + MyPrimitive_fld2 + "}";
        }

    }

    private static class InstanceHolder {
        public final MyPrimitive InstanceHolder_fld1;

        public InstanceHolder(int v) {
            InstanceHolder_fld1 = MyPrimitive.create(v, v + 100);
        }

        public String toString() {
            return "InstanceHolder { fld1 is " + InstanceHolder_fld1 + "}";
        }
    }

    private static primitive class PrimitiveHolder {
        public MyPrimitive PrimitiveHolder_fld1;

        public PrimitiveHolder(int v) {
            PrimitiveHolder_fld1 = MyPrimitive.create(v, v + 200);
        }

        public String toString() {
            return "PrimitiveHolder { fld1 is " + PrimitiveHolder_fld1 + "}";
        }
    }

    private static class TestHolder {
        public MyPrimitive primitiveObj = MyPrimitive.create(1, 1);
        public InstanceHolder instanceHolderObj = new InstanceHolder(1);
        public PrimitiveHolder primitiveHolderObj = new PrimitiveHolder(1);
    }

    public static void main(String[] args) throws Exception {
        try {
            System.loadLibrary(agentLib);
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Failed to load " + agentLib + " lib");
            System.err.println("java.library.path: " + System.getProperty("java.library.path"));
            throw ex;
        }

        // create objects for access testing before setting watchers
        TestHolder testHolder = new TestHolder();

        if (!initWatchers(MyPrimitive.class, MyPrimitive.class.getDeclaredField("MyPrimitive_fld1"))) {
            throw new RuntimeException("Watchers initializations error (MyPrimitive_fld1)");
        }
        if (!initWatchers(MyPrimitive.class, MyPrimitive.class.getDeclaredField("MyPrimitive_fld2"))) {
            throw new RuntimeException("Watchers initializations error (MyPrimitive_fld2)");
        }
        if (!initWatchers(InstanceHolder.class, InstanceHolder.class.getDeclaredField("InstanceHolder_fld1"))) {
            throw new RuntimeException("Watchers initializations error (InstanceHolder_fld1)");
        }
        if (!initWatchers(PrimitiveHolder.class, PrimitiveHolder.class.getDeclaredField("PrimitiveHolder_fld1"))) {
            throw new RuntimeException("Watchers initializations error (PrimitiveHolder_fld1)");
        }

        test("MyPrimitive (access)", () -> {
                testHolder.primitiveObj.toString();     // should access both MyPrimitive_fld1 and MyPrimitive_fld2
            }, new TestResult() {
                public boolean MyPrimitive_fld1_access;
                public boolean MyPrimitive_fld2_access;
            });

        test("InstanceHolder (access)", () ->  {
                testHolder.instanceHolderObj.toString();
            }, new TestResult() {
                public boolean InstanceHolder_fld1_access;
                // MyPrimitive fields should be accessed too
                public boolean MyPrimitive_fld1_access;
                public boolean MyPrimitive_fld2_access;
            });

        test("PrimitiveHolder (access)", () ->  {
                testHolder.primitiveHolderObj.toString();
            }, new TestResult() {
                public boolean PrimitiveHolder_fld1_access;
                // MyPrimitive fields should be accessed too
                public boolean MyPrimitive_fld1_access;
                public boolean MyPrimitive_fld2_access;
            });

        test("MyPrimitive (modify)", () ->  {
                MyPrimitive obj = MyPrimitive.create(1, 1);
            }, new TestResult() {
                // KNOWN_ISSUE public boolean MyPrimitive_fld1_modify;
                // KNOWN_ISSUE public boolean MyPrimitive_fld2_modify;
            });

        test("InstanceHolder (modify)", () ->  {
                InstanceHolder obj = new InstanceHolder(10);
            }, new TestResult() {
                public boolean InstanceHolder_fld1_modify;
                // MyPrimitive fields should be modified too
                // KNOWN_ISSUE public boolean MyPrimitive_fld1_modify;
                // KNOWN_ISSUE public boolean MyPrimitive_fld2_modify;
            });

        test("PrimitiveHolder (modify)", () ->  {
                PrimitiveHolder obj = new PrimitiveHolder(11);
            }, new TestResult() {
                // KNOWN_ISSUE public boolean PrimitiveHolder_fld1_modify;
                // MyPrimitive fields should be modified too
                // KNOWN_ISSUE public boolean MyPrimitive_fld1_modify;
                // KNOWN_ISSUE public boolean MyPrimitive_fld2_modify;
            });

    }

    private static void log(String msg) {
        System.out.println(msg);
        System.out.flush();
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new RuntimeException("assert");
        }
    }

    // For every access/modify notification native part tries to locate
    // boolean "<field_name>_access"/"<field_name>_modify" field and sets it to true
    private static class TestResult {

        // verify that all fields are set to true
        public void verify() {
            Arrays.stream(this.getClass().getDeclaredFields()).forEach(f -> verify(f));
        }

        private void verify(Field f) {
            try {
                if (!f.getBoolean(this)) {
                    throw new RuntimeException(f.getName() + " notification is missed");
                }
                log("  - " + f.getName() + ": OK");
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @FunctionalInterface
    private interface TestAction {
        void apply();
    }

    private static void test(String descr, TestAction action, TestResult result) throws Exception {
        log(descr + ": starting");
        if (!startTest(result)) {
            throw new RuntimeException("startTest failed");
        }
        action.apply();
        // wait some time to ensure all posted events are handled
        Thread.sleep(500);

        stopTest();

        // check the results
        result.verify();

        log(descr + ": OK");
        log("");
    }

    private static native boolean initWatchers(Class cls, Field field);
    private static native boolean startTest(TestResult results);
    private static native void stopTest();

}
