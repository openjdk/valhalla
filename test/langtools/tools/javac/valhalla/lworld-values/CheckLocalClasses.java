/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211910
 * @summary [lworld] Reinstate support for local value classes.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main/othervm -XX:+EnableValhalla CheckLocalClasses
 */

import com.sun.tools.classfile.*;

public class CheckLocalClasses {
    public class RefOuter {
        void foo() {
            RefOuter o = new RefOuter();
            inline  class Inner {
                private final int value2;
                public Inner(int value2) {
                    System.out.println(o);
                    this.value2 = value2;
                }
            }
        }
    }
    public inline class ValueOuter {
        int x = 10;
        void foo() {
            ValueOuter o = new ValueOuter();
            inline class Inner {
                private final int value2;
                public Inner(int value2) {
                    System.out.println(o);
                    this.value2 = value2;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(CheckLocalClasses.class.getResourceAsStream("CheckLocalClasses$ValueOuter$1Inner.class"));

        if (!cls.access_flags.is(AccessFlags.ACC_VALUE))
            throw new Exception("Value flag not set");

        if (!cls.access_flags.is(AccessFlags.ACC_FINAL))
            throw new Exception("Final flag not set");

        cls = ClassFile.read(CheckLocalClasses.class.getResourceAsStream("CheckLocalClasses$RefOuter$1Inner.class"));

        if (!cls.access_flags.is(AccessFlags.ACC_VALUE))
            throw new Exception("Value flag not set");

        if (!cls.access_flags.is(AccessFlags.ACC_FINAL))
            throw new Exception("Final flag not set");

    }
}
