/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @bug 8267910
 * @summary Javac fails to implicitly type abstract classes as having identity
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main ImplicitIdentityTypeTest
 */

/* An abstract implicitly implements IdentityObject
        - if it declares a field,
        - an instance initializer,
        - a non-empty constructor,
        - a synchronized method,
        - has a concrete super,
        - is an inner class.
*/

import com.sun.tools.classfile.*;

public class ImplicitIdentityTypeTest {

    static abstract class A {}  // Not an Identity class.
    static abstract class B { static { System.out.println(); } }  // Not an Identity class.


    // All abstract classes below are identity classes by implicit typing.

    abstract class C {}  // inner class implicitly implements IdentityObject
    static abstract class D { int f; }  // instance field lends it identity.
    static abstract class E { { System.out.println(); } }  // initializer lends it identity.
    static abstract class F { F(){ System.out.println(); }}  // non-empty ctor.
    static abstract class G { synchronized void f() {} }  // synchronized method.

    static abstract class H extends ImplicitIdentityTypeTest {}  // not identity - no inheritance of flag bits from concrete super.

    public static void main(String [] args) throws Exception {

        ClassFile cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$A.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should not be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$B.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should not be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$C.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$D.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$E.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$F.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$G.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$H.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should NOT be set!");

    }
}
