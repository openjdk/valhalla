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
 * @summary V.ref class should not inadvertently carry over attributes from V.class
 * @bug 8244713
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main AttributesTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class AttributesTest {

    void foo() {
        @Deprecated
        primitive class V<T> {}
    }

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(AttributesTest.class.getResourceAsStream("AttributesTest$1V$ref.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of inner class attribute */
        InnerClasses_attribute inners = (InnerClasses_attribute) cls.attributes.get(Attribute.InnerClasses);
        if (inners == null) {
            throw new AssertionError("Missing inner class attribute");
        }
        if (inners.number_of_classes != 2) {
            throw new AssertionError("Unexpected inner class attribute");
        }
        String name = inners.classes[0].getInnerName(cls.constant_pool);
        if (!name.equals("V")) {
            throw new AssertionError("Unexpected inner class " + name);
        }
        name = inners.classes[1].getInnerName(cls.constant_pool);
        if (!name.equals("V$ref")) {
            throw new AssertionError("Unexpected inner class " + name);
        }

        // Test emission of nest host attribute. Nest members attribute tested in InlineNesting*
        NestHost_attribute nestHost = (NestHost_attribute)cls.attributes.get(Attribute.NestHost);
        CONSTANT_Class_info host = nestHost != null ? nestHost.getNestTop(cls.constant_pool) : null;
        if (host == null || !host.getName().equals("AttributesTest")) {
            throw new RuntimeException("Wrong Nest host " + host.getName());
        }

        // Test signature attribute
        Signature_attribute signature = (Signature_attribute)cls.attributes.get(Attribute.Signature);
        String sign =  signature.getSignature(cls.constant_pool);
        if (sign == null || !sign.equals("<T:Ljava/lang/Object;>Ljava/lang/Object;")) {
            throw new RuntimeException("Wrong signature " + sign);
        }

        // Test SourceFile attribute
        SourceFile_attribute source = (SourceFile_attribute)cls.attributes.get(Attribute.SourceFile);
        String src =  source.getSourceFile(cls.constant_pool);
        if (src == null || !src.equals("AttributesTest.java")) {
            throw new RuntimeException("Wrong source " + src);
        }

        // Test Deprecated attribute
        Deprecated_attribute depr = (Deprecated_attribute) cls.attributes.get(Attribute.Deprecated);
        if (depr == null) {
            throw new RuntimeException("Missing deprecated annotation");
        }

        // Test EnclosingMethod attribute
        EnclosingMethod_attribute meth = (EnclosingMethod_attribute) cls.attributes.get(Attribute.EnclosingMethod);
        if (meth == null) {
            throw new RuntimeException("Missing enclosing method attribute");
        }
        String mName = meth.getMethodName(cls.constant_pool);
        if (mName == null || !mName.equals("foo")) {
            throw new RuntimeException("Wrong enclosing method " + mName);
        }

        // The following attributes should not be present in the projection file.
        String [] shouldBeAbsent = {
                                        "SourceDebugExtension",
                                        "BootstrapMethods",
                                        "Module",
                                        "ModulePackages",
                                        "ModuleMainClass",
                                        "Synthetic",
                                   };
        for (String attr : shouldBeAbsent) {
            if (cls.getAttribute(attr) != null) {
                throw new RuntimeException("Unexpected attribute: " + attr);
            }
        }
    }
}
