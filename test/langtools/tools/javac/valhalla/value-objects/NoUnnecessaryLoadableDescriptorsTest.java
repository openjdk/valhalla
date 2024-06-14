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
 * @test
 * @bug 8281323
 * @summary Check emission of LoadableDescriptors attribute to make sure javac does not emit unneeded entries.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @enablePreview
 * @run main NoUnnecessaryLoadableDescriptorsTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;

public class NoUnnecessaryLoadableDescriptorsTest {

    public value class LoadableDescriptorsTest1 {
        byte b;
        public LoadableDescriptorsTest1(byte b) {
            this.b = b;
        }
    }

    public class LoadableDescriptorsTest2 {
        static class Inner1 {
            static value class Inner2 {}
            Inner2 inner;
        }
    }

    public static void main(String[] args) throws Exception {

        // There should be no LoadableDescriptors attribute in NoUnnecessaryLoadableDescriptorsTest.class
        ClassFile cls = ClassFile.read(NoUnnecessaryLoadableDescriptorsTest.class.getResourceAsStream("NoUnnecessaryLoadableDescriptorsTest.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of LoadableDescriptors attribute */
        LoadableDescriptors_attribute LoadableDescriptors = (LoadableDescriptors_attribute) cls.attributes.get(Attribute.LoadableDescriptors);
        if (LoadableDescriptors != null) {
            throw new AssertionError("Unexpected LoadableDescriptors attribute!");
        }

        // There should be no LoadableDescriptors attribute in NoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest1.class
        cls = ClassFile.read(NoUnnecessaryLoadableDescriptorsTest.class.getResourceAsStream("NoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest1.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of LoadableDescriptors attribute */
        LoadableDescriptors = (LoadableDescriptors_attribute) cls.attributes.get(Attribute.LoadableDescriptors);
        if (LoadableDescriptors != null) {
            throw new AssertionError("Unexpected LoadableDescriptors attribute!");
        }

        // There should be no LoadableDescriptors attribute in NoUnnecessaryLoadableDescriptorsTest$PreloadTest2.class
        cls = ClassFile.read(NoUnnecessaryLoadableDescriptorsTest.class.getResourceAsStream("NoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest2.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of LoadableDescriptors attribute */
        LoadableDescriptors = (LoadableDescriptors_attribute) cls.attributes.get(Attribute.LoadableDescriptors);
        if (LoadableDescriptors != null) {
            throw new AssertionError("Unexpected LoadableDescriptors attribute!");
        }

        // There should be no LoadableDescriptors attribute in NoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest2$Inner2.class
        cls = ClassFile.read(NoUnnecessaryLoadableDescriptorsTest.class.getResourceAsStream("NoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest2$Inner1$Inner2.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of LoadableDescriptors attribute */
        LoadableDescriptors = (LoadableDescriptors_attribute) cls.attributes.get(Attribute.LoadableDescriptors);
        if (LoadableDescriptors != null) {
            throw new AssertionError("Unexpected LoadableDescriptors attribute!");
        }

        // There should be ONE LoadableDescriptors attribute entry in NoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest2$Inner1.class
        cls = ClassFile.read(NoUnnecessaryLoadableDescriptorsTest.class.getResourceAsStream("NoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest2$Inner1.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of LoadableDescriptors attribute */
        LoadableDescriptors = (LoadableDescriptors_attribute) cls.attributes.get(Attribute.LoadableDescriptors);
        if (LoadableDescriptors == null) {
            throw new AssertionError("Missing LoadableDescriptors attribute!");
        }

        if (LoadableDescriptors.number_of_descriptors != 1) {
            throw new AssertionError("Incorrect number of LoadableDescriptors classes");
        }

        CONSTANT_Utf8_info utf8Info = cls.constant_pool.getUTF8Info(LoadableDescriptors.descriptor_info_index[0]);
        System.err.println("utf8 " + utf8Info.value);
        if (!utf8Info.value.equals("LNoUnnecessaryLoadableDescriptorsTest$LoadableDescriptorsTest2$Inner1$Inner2;")) {
            throw new AssertionError("Expected LoadableDescriptors class entry is missing, but found " + utf8Info.value);
        }
    }
}
