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
 * @summary Check emission of Preload attribute to make sure javac does not emit unneeded entries.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main NoUnnecessaryPreloadsTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class NoUnnecessaryPreloadsTest {

    public value class PreLoadTest1 {
        byte b;
        public PreLoadTest1(byte b) {
            this.b = b;
        }
    }

    public class PreLoadTest2 {
        static class Inner1 {
            static value class Inner2 {}
            Inner2 inner;
        }
    }

    public static void main(String[] args) throws Exception {

        // There should be no Preload attribute in NoUnnecessaryPreloadsTest.class
        ClassFile cls = ClassFile.read(NoUnnecessaryPreloadsTest.class.getResourceAsStream("NoUnnecessaryPreloadsTest.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of Preload attribute */
        Preload_attribute preloads = (Preload_attribute) cls.attributes.get(Attribute.Preload);
        if (preloads != null) {
            throw new AssertionError("Unexpected Preload attribute!");
        }

        // There should be no Preload attribute in NoUnnecessaryPreloadsTest$PreloadTest1.class
        cls = ClassFile.read(NoUnnecessaryPreloadsTest.class.getResourceAsStream("NoUnnecessaryPreloadsTest$PreLoadTest1.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of Preload attribute */
        preloads = (Preload_attribute) cls.attributes.get(Attribute.Preload);
        if (preloads != null) {
            throw new AssertionError("Unexpected Preload attribute!");
        }

        // There should be no Preload attribute in NoUnnecessaryPreloadsTest$PreloadTest2.class
        cls = ClassFile.read(NoUnnecessaryPreloadsTest.class.getResourceAsStream("NoUnnecessaryPreloadsTest$PreLoadTest2.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of Preload attribute */
        preloads = (Preload_attribute) cls.attributes.get(Attribute.Preload);
        if (preloads != null) {
            throw new AssertionError("Unexpected Preload attribute!");
        }

        // There should be no Preload attribute in NoUnnecessaryPreloadsTest$PreloadTest2$Inner2.class
        cls = ClassFile.read(NoUnnecessaryPreloadsTest.class.getResourceAsStream("NoUnnecessaryPreloadsTest$PreLoadTest2$Inner1$Inner2.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of Preload attribute */
        preloads = (Preload_attribute) cls.attributes.get(Attribute.Preload);
        if (preloads != null) {
            throw new AssertionError("Unexpected Preload attribute!");
        }

        // There should be ONE Preload attribute entry in NoUnnecessaryPreloadsTest$PreloadTest2$Inner1.class
        cls = ClassFile.read(NoUnnecessaryPreloadsTest.class.getResourceAsStream("NoUnnecessaryPreloadsTest$PreLoadTest2$Inner1.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of Preload attribute */
        preloads = (Preload_attribute) cls.attributes.get(Attribute.Preload);
        if (preloads == null) {
            throw new AssertionError("Missing Preload attribute!");
        }

        if (preloads.number_of_classes != 1) {
            throw new AssertionError("Incorrect number of Preload classes");
        }

        CONSTANT_Class_info clsInfo = cls.constant_pool.getClassInfo(preloads.value_class_info_index[0]);
        if (!clsInfo.getName().equals("NoUnnecessaryPreloadsTest$PreLoadTest2$Inner1$Inner2")) {
            throw new AssertionError("Expected Preload class entry is missing, but found " + clsInfo.getName());
        }
    }
}
