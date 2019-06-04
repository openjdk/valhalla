/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222634
 * @summary Check field descriptors in class file
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main/othervm -XX:+EnableValhalla CheckFieldDescriptors
 */

import com.sun.tools.classfile.*;

public inline class CheckFieldDescriptors {

    int x = 10;


    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(CheckFieldDescriptors.class.getResourceAsStream("CheckFieldDescriptorsAuxilliary.class"));

        Field [] flds = cls.fields;
        int fCount = 0;
        for (Field fld : flds) {
            if (fld.getName(cls.constant_pool).equals("f1")) {
                fCount++;
                if (!fld.descriptor.getValue(cls.constant_pool).equals("QCheckFieldDescriptors;"))
                    throw new Exception("Bad descriptor for field1");
            } else if (fld.getName(cls.constant_pool).equals("f2")) {
                fCount++;
                if (!fld.descriptor.getValue(cls.constant_pool).equals("LCheckFieldDescriptors;"))
                    throw new Exception("Bad descriptor for field2");
            } else if (fld.getName(cls.constant_pool).equals("f3")) {
                fCount++;
                if (!fld.descriptor.getValue(cls.constant_pool).equals("LCheckFieldDescriptors;"))
                    throw new Exception("Bad descriptor for field3");
            } else if (fld.getName(cls.constant_pool).equals("a1")) {
                fCount++;
                if (!fld.descriptor.getValue(cls.constant_pool).equals("[LCheckFieldDescriptors;"))
                    throw new Exception("Bad descriptor for field4");
            } else if (fld.getName(cls.constant_pool).equals("a2")) {
                fCount++;
                if (!fld.descriptor.getValue(cls.constant_pool).equals("[LCheckFieldDescriptors;"))
                    throw new Exception("Bad descriptor for field5");
            } else if (fld.getName(cls.constant_pool).equals("a3")) {
                fCount++;
                if (!fld.descriptor.getValue(cls.constant_pool).equals("[QCheckFieldDescriptors;"))
                    throw new Exception("Bad descriptor for field6");
            } else if (fld.getName(cls.constant_pool).equals("a4")) {
                fCount++;
                if (!fld.descriptor.getValue(cls.constant_pool).equals("[QCheckFieldDescriptors;"))
                    throw new Exception("Bad descriptor for field7");
            }
        }
        if (fCount != 7) {
            throw new Exception("Bad descriptor for field3");
        }
    }
}

class CheckFieldDescriptorsAuxilliary {

    CheckFieldDescriptors f1;
    CheckFieldDescriptors? f2;
    CheckFieldDescriptors? f3;

    CheckFieldDescriptors?[] a1 = new CheckFieldDescriptors?[42];
    CheckFieldDescriptors?[] a2 = new CheckFieldDescriptors?[42];
    CheckFieldDescriptors[] a3 = new CheckFieldDescriptors[42];
    CheckFieldDescriptors[] a4 = new CheckFieldDescriptors[42];
}
