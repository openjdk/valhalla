/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.valuetypes;

import java.lang.invoke.*;

import jdk.experimental.value.MethodHandleBuilder;

/*
 * @test ObjectMethods
 * @summary Check object method implemented by the VM behave with value types
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /test/lib
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowWithFieldOperator ObjectMethods.java
 * @run main/othervm -Xint -XX:+UseBiasedLocking -XX:+UseCompressedClassPointers runtime.valhalla.valuetypes.ObjectMethods
 * @run main/othervm -Xint -XX:-UseBiasedLocking -XX:-UseCompressedClassPointers runtime.valhalla.valuetypes.ObjectMethods
 * @run main/othervm -Xint -noverify runtime.valhalla.valuetypes.ObjectMethods noverify
 * @run main/othervm -Xcomp -XX:+UseBiasedLocking -XX:+UseCompressedClassPointers runtime.valhalla.valuetypes.ObjectMethods
 * @run main/othervm -Xcomp -XX:-UseBiasedLocking -XX:-UseCompressedClassPointers runtime.valhalla.valuetypes.ObjectMethods
 * @run main/othervm -Xcomp -noverify runtime.valhalla.valuetypes.ObjectMethods noverify
 */

public class ObjectMethods {

    public static void main(String[] args) {
        testObjectMethods((args.length > 0 && args[0].equals("noverify")));
    }

    public static void testObjectMethods(boolean verifierDisabled) {
        MyInt val = MyInt.create(7);
        MyInt sameVal = MyInt.create(7);

        // Exercise all the Object native/VM methods...

        if (verifierDisabled) { // Just noverifier...
            checkMonitorExit(val);
            return;
        }

        // getClass()
        checkGetClass(val, MyInt.class);

        //hashCode()/identityHashCode()
        checkHashCodes(val, sameVal.hashCode());

        // clone()
        checkNotCloneable(val);

        // synchronized
        checkSynchronized(val);

        // wait/notify()
        checkWait(val);
        checkNotify(val);

        System.gc();
    }


    static void checkGetClass(Object val, Class<?> expectedClass) {
        Class<?> clazz = val.getClass();
        if (clazz == null) {
            throw new RuntimeException("getClass return null");
        } else if (clazz != expectedClass) {
            throw new RuntimeException("getClass (" + clazz + ") doesn't match " + expectedClass);
        }
    }

    // Just check we don't crash the VM
    static void checkHashCodes(Object val, int expectedHashCode) {
        if (val.hashCode() != expectedHashCode) {
            throw new RuntimeException("Hash code mismatch value: " + val.hashCode() +
                                       " expected: " + expectedHashCode);
        }
    }

    static void checkNotCloneable(MyInt val) {
        boolean sawCnse = false;
        try {
            val.attemptClone();
        } catch (CloneNotSupportedException cnse) {
            sawCnse = true;
        }
        if (!sawCnse) {
            throw new RuntimeException("clone() did not fail");
        }
        // Cloneable inline type checked by "BadValueTypes" CFP tests
    }

    static void checkSynchronized(Object val) {
        boolean sawImse = false;
        try {
            synchronized (val) {
                throw new IllegalStateException("Unreachable code, reached");
            }
        } catch (IllegalMonitorStateException imse) {
            sawImse = true;
        }
        if (!sawImse) {
            throw new RuntimeException("monitorenter did not fail");
        }
        // synchronized method modifiers tested by "BadValueTypes" CFP tests
        // jni monitor ops tested by "ValueWithJni"
    }

    // Check we haven't broken the mismatched monitor block check...
    static void checkMonitorExit(Object val) {
        boolean sawImse = false;
        try {
            MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                                         "mismatchedMonitorExit",
                                         MethodType.methodType(Void.TYPE, Object.class),
                                         CODE->{
                                             CODE
                                                 .aload(0)
                                                 .monitorexit()
                                                 .return_();
                                         }).invokeExact(val);
            throw new IllegalStateException("Unreachable code, reached");
        } catch (Throwable t) {
            if (t instanceof IllegalMonitorStateException) {
                sawImse = true;
            } else {
                throw new RuntimeException(t);
            }
        }
        if (!sawImse) {
            throw new RuntimeException("monitorexit did not fail");
        }
    }

    static void checkWait(Object val) {
        boolean sawImse = false;
        try {
            val.wait();
        } catch (IllegalMonitorStateException imse) {
            sawImse = true;
        } catch (InterruptedException intExc) {
            throw new RuntimeException(intExc);
        }
        if (!sawImse) {
            throw new RuntimeException("wait() did not fail");
        }

        sawImse = false;
        try {
            val.wait(1l);
        } catch (IllegalMonitorStateException imse) {
            sawImse = true;
        } catch (InterruptedException intExc) {
            throw new RuntimeException(intExc);
        }
        if (!sawImse) {
            throw new RuntimeException("wait() did not fail");
        }

        sawImse = false;
        try {
            val.wait(0l, 100);
        } catch (IllegalMonitorStateException imse) {
            sawImse = true;
        } catch (InterruptedException intExc) {
            throw new RuntimeException(intExc);
        }
        if (!sawImse) {
            throw new RuntimeException("wait() did not fail");
        }
    }

    static void checkNotify(Object val) {
        boolean sawImse = false;
        try {
            val.notify();
        } catch (IllegalMonitorStateException imse) {
            sawImse = true;
        }
        if (!sawImse) {
            throw new RuntimeException("notify() did not fail");
        }

        sawImse = false;
        try {
            val.notifyAll();
        } catch (IllegalMonitorStateException imse) {
            sawImse = true;
        }
        if (!sawImse) {
            throw new RuntimeException("notifyAll() did not fail");
        }
    }

    static final inline class MyInt {
        final int value;
        private MyInt() { value = 0; }
        public static MyInt create(int v) {
            MyInt mi = MyInt.default;
            mi = __WithField(mi.value, v);
            return mi;
        }
        public Object attemptClone() throws CloneNotSupportedException {
            try { // Check it is not possible to clone...
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle mh = lookup.findVirtual(getClass(),
                                                     "clone",
                                                     MethodType.methodType(Object.class));
                return mh.invokeExact(this);
            } catch (Throwable t) {
                if (t instanceof CloneNotSupportedException) {
                    throw (CloneNotSupportedException) t;
                }
                throw new RuntimeException(t);
            }
        }
    }

}
