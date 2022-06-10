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
 * @bug 8287713
 * @summary [lw4] Javac incorrectly flags subclasses as being ACC_IDENTITY classes.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main NoAutoInheritanceOfIdentityFlagBit
 */

import com.sun.tools.classfile.*;

public class NoAutoInheritanceOfIdentityFlagBit { // ACC_IDENTITY - concrete class

    abstract class A {}  // ACC_IDENTITY: Inner class

    static abstract class B {}  // NO ACC_IDENTITY: No an inner class with an enclosing instance

    static abstract class C {
        int f; // ACC_IDENTITY since an instance field is declared.
    }

    static abstract value class D {} // No ACC_IDENTITY since an express abstract value class.

    static value class E {} // No ACC_IDENTITY since an express concrete value class.

    static abstract class F extends C {} // No ACC_IDENTITY - since no auto propagation.

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(NoAutoInheritanceOfIdentityFlagBit.class.getResourceAsStream("NoAutoInheritanceOfIdentityFlagBit.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(NoAutoInheritanceOfIdentityFlagBit.class.getResourceAsStream("NoAutoInheritanceOfIdentityFlagBit$A.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(NoAutoInheritanceOfIdentityFlagBit.class.getResourceAsStream("NoAutoInheritanceOfIdentityFlagBit$B.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should NOT be set!");

        cls = ClassFile.read(NoAutoInheritanceOfIdentityFlagBit.class.getResourceAsStream("NoAutoInheritanceOfIdentityFlagBit$C.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(NoAutoInheritanceOfIdentityFlagBit.class.getResourceAsStream("NoAutoInheritanceOfIdentityFlagBit$D.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should NOT be set!");

        cls = ClassFile.read(NoAutoInheritanceOfIdentityFlagBit.class.getResourceAsStream("NoAutoInheritanceOfIdentityFlagBit$E.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should NOT be set!");

        cls = ClassFile.read(NoAutoInheritanceOfIdentityFlagBit.class.getResourceAsStream("NoAutoInheritanceOfIdentityFlagBit$F.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should NOT be set!");
    }
}
