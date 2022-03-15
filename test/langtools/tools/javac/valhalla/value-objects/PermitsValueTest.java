/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * @test
 * @bug 8280194
 * @summary Abstract classes that allow value subclasses should be marked ACC_PERMITS_VALUE (0x0040)
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main PermitsValueTest
 */

import com.sun.tools.classfile.*;

public class PermitsValueTest {

    static abstract class A0 extends PermitsValueTest {
       // Not ACC_PERMITS_VALUE as it extends an identity class.
    }

    static abstract class A1 implements IdentityObject {
       // Not ACC_PERMITS_VALUE as it implements IdentityObject
    }

    static abstract class A2 {
        int f; // Not ACC_PERMITS_VALUE as it declares an instance field.
    }

    static abstract class A3 extends A2 {
        // Not ACC_PERMITS_VALUE as it extends an implicit identity class.
    }

    static abstract class A4 {
        // Not ACC_PERMITS_VALUE as it declares a non-empty initializer block.
        {
            System.out.println("initializer block");
        }
    }

    static abstract class A5 extends A4 {
        // Not ACC_PERMITS_VALUE as it extends an implicit identity class.
    }

    static abstract class A6 {
        // Not ACC_PERMITS_VALUE as declares a synchronized method.
        synchronized void foo() {
        }
    }

    static abstract class A7 extends A6 {
        // Not ACC_PERMITS_VALUE as it extends an implicit identity class.
    }

    abstract class A8 {
        // Not ACC_PERMITS_VALUE as it is an inner class
    }

    static abstract class A9 {
        // Not ACC_PERMITS_VALUE as it defines a arg'ed constructor.
        A9(int x) {}
    }

    static abstract class A10 {
        // Not ACC_PERMITS_VALUE as it defines a non empty constructor.
        A10() {
            super();
        }
    }

    static abstract class A11 { // Permits value.
        static int f; // static field is OK.
        static {
            System.out.println("Static initializer block is OK");
        }
        A11() {
            // empty constructor is OK.
        }
        static synchronized void foo() {
            // static method may be synchronized.
        }
        {
            // empty init block is OK.
        }
    }

    static abstract class A12 extends A11 implements ValueObject {
        // Permits value
    }

    static abstract class A13 extends A12 {
        // Permits value
    }


    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A0.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A1.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A2.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A3.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A4.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A5.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A6.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A7.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A8.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A9.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A10.class"));
        if (cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should not be set!");


        // The following are all proper ACC_PERMITS_VALUE classes
        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A11.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A12.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should be set!");

        cls = ClassFile.read(PermitsValueTest.class.getResourceAsStream("PermitsValueTest$A13.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_PERMITS_VALUE))
            throw new Exception("ACC_PERMITS_VALUE flag should be set!");
    }
}
