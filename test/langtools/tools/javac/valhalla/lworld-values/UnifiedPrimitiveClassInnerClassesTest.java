/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8265423
 * @summary Experimental support for generating a single class file per primitive class
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -XDunifiedValRefClass UnifiedPrimitiveClassInnerClassesTest.java
 * @run main UnifiedPrimitiveClassInnerClassesTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class UnifiedPrimitiveClassInnerClassesTest {

    primitive class V<T> implements java.io.Serializable {}

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(UnifiedPrimitiveClassInnerClassesTest.class.getResourceAsStream("UnifiedPrimitiveClassInnerClassesTest.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of inner class attribute */
        InnerClasses_attribute inners = (InnerClasses_attribute) cls.attributes.get(Attribute.InnerClasses);
        if (inners == null) {
            throw new AssertionError("Missing inner class attribute");
        }
        boolean foundV = false, foundVref = false;
        for (int i = 0; i < inners.number_of_classes; i++) {
            String name = inners.classes[i].getInnerName(cls.constant_pool);
            if (name.equals("V"))
                foundV = true;
            else if (name.equals("V$ref"))
                foundVref = true;
        }
        if (!foundV || foundVref) {
            throw new AssertionError("Incorrect inner class attribute");
        }

        // Test signature attribute
        cls = ClassFile.read(UnifiedPrimitiveClassInnerClassesTest.class.getResourceAsStream("UnifiedPrimitiveClassInnerClassesTest$V.class"));
        Signature_attribute signature = (Signature_attribute)cls.attributes.get(Attribute.Signature);
        String sign =  signature.getSignature(cls.constant_pool);
        if (sign == null || !sign.equals("<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/io/Serializable;")) {
            throw new RuntimeException("Wrong signature " + sign);
        }
    }
}
