/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import static jdk.test.lib.Asserts.*;

/*
 * @test VarArgsArray
 * @summary Test if JVM API using varargs work with inline type arrays
 * @library /test/lib
 * @compile VarArgsArray.java NewInstanceFromConstructor.java IntValue.java
 * @run main/othervm -Xint runtime.valhalla.valuetypes.VarArgsArray
 * @run main/othervm -Xcomp runtime.valhalla.valuetypes.VarArgsArray
 */
public class VarArgsArray {

    static final int TOKEN_VALUE = 4711;

    int methodACnt = 0;
    int methodBCnt = 0;
    int methodCCnt = 0;

    public VarArgsArray() {
    }

    public void test() throws Throwable {
        // test publicly accessable API in the VM...given an inline type array
        testJvmInvokeMethod();
        testJvmNewInstanceFromConstructor();
    }

    public void testJvmInvokeMethod() throws Throwable {
        MyInt[] array0 = new MyInt[0];
        MyInt[] array1 = new MyInt[] { new MyInt(TOKEN_VALUE) };
        MyInt[] array2 = new MyInt[] { new MyInt(TOKEN_VALUE), new MyInt(TOKEN_VALUE) };

        Method methodARef = getClass().getDeclaredMethod("methodA", MyInt.class);
        Method methodBRef = getClass().getDeclaredMethod("methodB", MyInt.class, MyInt.class);
        Method methodCRef = getClass().getDeclaredMethod("methodC", MyInt.class, String.class);

        // Positive tests...
        methodARef.invoke(this, (Object[])array1);
        assertWithMsg(methodACnt == 1, "methodA did not invoke");

        methodARef.invoke(this, array1[0]);
        assertWithMsg(methodACnt == 2, "methodA did not invoke");

        methodBRef.invoke(this, (Object[]) array2);
        assertWithMsg(methodBCnt == 1, "methodB did not invoke");

        methodBRef.invoke(this, array2[0], array2[1]);
        assertWithMsg(methodBCnt == 2, "methodB did not invoke");

        // Negative tests...
        int argExCnt = 0;
        try {
            methodARef.invoke(this, (Object[]) array0);
            throw new RuntimeException("Expected fail");
        } catch (IllegalArgumentException argEx) { argExCnt++; }
        try {
            methodARef.invoke(this, (Object[]) array2);
            throw new RuntimeException("Expected fail");
        } catch (IllegalArgumentException argEx) { argExCnt++; }
        try {
            methodCRef.invoke(this, (Object[]) array2);
            throw new RuntimeException("Expected fail");
        } catch (IllegalArgumentException argEx) { argExCnt++; }
        assertWithMsg(argExCnt == 3, "Did not see the correct number of exceptions");
        assertWithMsg(methodACnt == 2, "methodA bad invoke count");
        assertWithMsg(methodBCnt == 2, "methodB bad invoke count");
        assertWithMsg(methodCCnt == 0, "methodC bad invoke count");
    }

    public void testJvmNewInstanceFromConstructor() throws Throwable {
        // Inner classes use outer in param list, so these won't exercise inline type array
        Class tc = NewInstanceFromConstructor.class;
        Class pt = IntValue.class;
        Constructor consARef = tc.getConstructor(pt);
        Constructor consBRef = tc.getConstructor(pt, pt);
        Constructor consCRef = tc.getConstructor(pt, String.class);
        IntValue[] array0 = new IntValue[0];
        IntValue[] array1 = new IntValue[] { new IntValue(TOKEN_VALUE) };
        IntValue[] array2 = new IntValue[] { new IntValue(TOKEN_VALUE),
                                             new IntValue(TOKEN_VALUE) };

        // Positive tests...
        consARef.newInstance((Object[])array1);
        consARef.newInstance(array1[0]);
        NewInstanceFromConstructor test = (NewInstanceFromConstructor)
            consBRef.newInstance((Object[])array2);
        assertWithMsg(test.getValue() == (2 * TOKEN_VALUE), "Param corrrupt");
        consBRef.newInstance(array2[0], array2[1]);
        assertWithMsg(NewInstanceFromConstructor.getConsCalls() == 4, "Constructor did not invoke");

        // Negative tests...
        int argExCnt = 0;
        try {
            consARef.newInstance((Object[])array0);
            throw new RuntimeException("Expected fail");
        } catch (IllegalArgumentException argEx) { argExCnt++; }
        try {
            consARef.newInstance((Object[])array2);
            throw new RuntimeException("Expected fail");
        } catch (IllegalArgumentException argEx) { argExCnt++; }
        try {
            consCRef.newInstance((Object[])array2);
            throw new RuntimeException("Expected fail");
        } catch (IllegalArgumentException argEx) { argExCnt++; }
        assertWithMsg(argExCnt == 3, "Did not see the correct number of exceptions");
        assertWithMsg(NewInstanceFromConstructor.getConsCalls() == 4, "Constructor should have been invoked");
    }

    public void methodA(MyInt a) {
        assertWithMsg(a.value == TOKEN_VALUE, "Bad arg");
        methodACnt++;
    }

    public void methodB(MyInt a, MyInt b) {
        assertWithMsg(a.value == TOKEN_VALUE, "Bad arg");
        assertWithMsg(b.value == TOKEN_VALUE, "Bad arg");
        methodBCnt++;
    }

    public void methodC(MyInt a, String b) {
        assertWithMsg(a.value == TOKEN_VALUE, "Bad arg");
        methodCCnt++;
    }

    static void assertWithMsg(boolean expr, String msg) throws RuntimeException {
        assertTrue(expr, msg);
    }

    public static void main(String[] args) throws Throwable {
        new VarArgsArray().test();
    }

    inline class MyInt {
        int value;
        public MyInt() { this(0); }
        public MyInt(int v) { this.value = v; }
    }


}
