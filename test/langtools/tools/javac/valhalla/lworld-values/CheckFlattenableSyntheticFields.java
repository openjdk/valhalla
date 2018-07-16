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
 * @bug 8207330
 * @summary Check that flattenable flag is set for synthetic fields as needed.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main/othervm -XX:+EnableValhalla CheckFlattenableSyntheticFields
 */

import com.sun.tools.classfile.*;

public class CheckFlattenableSyntheticFields {
    public class RefOuter {
        void foo() {
            RefOuter o = new RefOuter();
            __ByValue  class Inner {
                private final int value2;
                public Inner(int value2) {
                    System.out.println(o);
                    this.value2 = value2;
                }
            }
        }
    }
    public __ByValue class ValueOuter {
        int x = 10;
        void foo() {
            ValueOuter o = new ValueOuter();
            __ByValue  class Inner {
                private final int value2;
                public Inner(int value2) {
                    System.out.println(o);
                    this.value2 = value2;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(CheckFlattenableSyntheticFields.class.getResourceAsStream("CheckFlattenableSyntheticFields$ValueOuter$1Inner.class"));

        if (!cls.access_flags.is(AccessFlags.ACC_VALUE))
            throw new Exception("Value flag not set");

        if (!cls.access_flags.is(AccessFlags.ACC_FINAL))
            throw new Exception("Final flag not set");

        Field [] flds = cls.fields;

        for (Field fld : flds) {
            if (fld.getName(cls.constant_pool).equals("this$1")) {
                if (!fld.access_flags.is(AccessFlags.ACC_FLATTENABLE))
                    throw new Exception("Flattenable flag not set");
            } else if (fld.getName(cls.constant_pool).equals("val$o")) {
                if (!fld.access_flags.is(AccessFlags.ACC_FLATTENABLE))
                    throw new Exception("Flattenable flag not set");
            } else if (fld.getName(cls.constant_pool).equals("value2")) {
                if (fld.access_flags.is(AccessFlags.ACC_FLATTENABLE))
                    throw new Exception("Flattenable flag set");
            }
        }

        cls = ClassFile.read(CheckFlattenableSyntheticFields.class.getResourceAsStream("CheckFlattenableSyntheticFields$RefOuter$1Inner.class"));

        if (!cls.access_flags.is(AccessFlags.ACC_VALUE))
            throw new Exception("Value flag not set");

        if (!cls.access_flags.is(AccessFlags.ACC_FINAL))
            throw new Exception("Final flag not set");

        flds = cls.fields;

        for (Field fld : flds) {
            if (fld.getName(cls.constant_pool).equals("this$1")) {
                if (fld.access_flags.is(AccessFlags.ACC_FLATTENABLE))
                    throw new Exception("Flattenable flag is set");
            } else if (fld.getName(cls.constant_pool).equals("val$o")) {
                if (fld.access_flags.is(AccessFlags.ACC_FLATTENABLE))
                    throw new Exception("Flattenable flag is set");
            } else if (fld.getName(cls.constant_pool).equals("value2")) {
                if (fld.access_flags.is(AccessFlags.ACC_FLATTENABLE))
                    throw new Exception("Flattenable flag set");
            }
        }
    }
}