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
 * @bug 8287763
 * @summary [lw4] Javac does not implement the spec for non-trivial constructors in toto
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main NontrivialCtorInducedIdentity
 */

import com.sun.tools.classfile.*;

public class NontrivialCtorInducedIdentity {

    public static abstract class A0 { // Trivial constructor - no induced identity.
        public A0() {
            super();
        }
    }

    public static abstract class A1 {
        private A1() {} // restricted constructor
    }

    public static abstract class A2 {
        public <T> A2() {} // generic constructor
    }

    public static abstract class A3 {
        public A3() throws RuntimeException {} // throws
    }

    public static abstract class A4 {
        public A4(int x) {} // not no-arg
    }

    public static abstract class A5 {
        public A5() {
            System.out.println("Bodied constructor");
        }
    }

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(NontrivialCtorInducedIdentity.class.getResourceAsStream("NontrivialCtorInducedIdentity$A0.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should NOT be set!");

        cls = ClassFile.read(NontrivialCtorInducedIdentity.class.getResourceAsStream("NontrivialCtorInducedIdentity$A1.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(NontrivialCtorInducedIdentity.class.getResourceAsStream("NontrivialCtorInducedIdentity$A2.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(NontrivialCtorInducedIdentity.class.getResourceAsStream("NontrivialCtorInducedIdentity$A3.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(NontrivialCtorInducedIdentity.class.getResourceAsStream("NontrivialCtorInducedIdentity$A4.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(NontrivialCtorInducedIdentity.class.getResourceAsStream("NontrivialCtorInducedIdentity$A5.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");
    }
}
