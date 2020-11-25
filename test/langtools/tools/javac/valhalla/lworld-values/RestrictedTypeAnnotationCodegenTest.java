/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255856 8257028
 * @summary Generate RestrictedField attributes from annotations
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -XDallowWithFieldOperator Point.java
 * @run main RestrictedTypeAnnotationCodegenTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

import java.lang.invoke.RestrictedType;

public class RestrictedTypeAnnotationCodegenTest {

    inline class Point {}
    inline class Line {}

    @RestrictedType("QRestrictedTypeAnnotationCodegenTest$Line;")
    public Object jloFld = null;

    @RestrictedType("QRestrictedTypeAnnotationCodegenTest$Point;")
    public Point.ref refProjFld = null;

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(InlineNestingAttributesTest.class.getResourceAsStream("RestrictedTypeAnnotationCodegenTest.class"));

        int goodFlds = 0;
        for (Field fld: cls.fields) {
            if (fld.getName(cls.constant_pool).equals("jloFld")) {
               String desc = fld.descriptor.getValue(cls.constant_pool);
               RestrictedField_attribute rfa =
                    (RestrictedField_attribute) fld.attributes.get(Attribute.RestrictedField);
               if (rfa.getRestrictedType(cls.constant_pool).equals("QRestrictedTypeAnnotationCodegenTest$Line;") && desc.equals("Ljava/lang/Object;"))
                    goodFlds++;
            } else if (fld.getName(cls.constant_pool).equals("refProjFld")) {
               String desc = fld.descriptor.getValue(cls.constant_pool);
               RestrictedField_attribute rfa =
                    (RestrictedField_attribute) fld.attributes.get(Attribute.RestrictedField);
               if (rfa.getRestrictedType(cls.constant_pool).equals("QRestrictedTypeAnnotationCodegenTest$Point;") && desc.equals("LRestrictedTypeAnnotationCodegenTest$Point$ref;"))
                    goodFlds++;
            }
        }
        if (goodFlds != 2) {
            throw new AssertionError("Lookup for 2 fields failed: Found only " + goodFlds);
        }
    }
}
