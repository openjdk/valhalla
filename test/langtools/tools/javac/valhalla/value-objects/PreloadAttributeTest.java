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
 * @bug 8280164
 * @summary Check emission of Preload attribute
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile MultiValues.java
 * @compile -g PreloadAttributeTest.java
 * @run main PreloadAttributeTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class PreloadAttributeTest {

    static final value class X {
        final V1 [] v1 = null; // field descriptor
        V2[] foo() {  // method descriptor encoding value type
            return null;
        }
        void foo(V3 v3) { // method descriptor encoding value type
        }
        void foo(int x) {
            V4 [] v4 = null; // local variable.
        }
        void goo() {
            V5 [] v5 = null;
            if (v5 == null) {
                // ...
            } else {
               V5 [] v52 = null;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(PreloadAttributeTest.class.getResourceAsStream("PreloadAttributeTest$X.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of Preload attribute */
        Preload_attribute preloads = (Preload_attribute) cls.attributes.get(Attribute.Preload);
        if (preloads == null) {
            throw new AssertionError("Missing Preload attribute!");
        }
        if (preloads.number_of_classes != 6) {
            throw new AssertionError("Incorrect number of Preload classes");
        }

        int mask = 0x3F;
        for (int i = 0; i < preloads.number_of_classes; i++) {
            CONSTANT_Class_info clsInfo = cls.constant_pool.getClassInfo(
                                  preloads.value_class_info_index[i]);
            switch (clsInfo.getName()) {
                case "V1":
                    mask &= ~1; break;
                case "V2":
                    mask &= ~2; break;
                case "V3":
                    mask &= ~4; break;
                case "V4":
                    mask &= ~8; break;
                case "V5":
                    mask &= ~16; break;
                case "PreloadAttributeTest$X":
                    mask &= ~32; break;
            }
        }
        if (mask != 0) {
          throw new AssertionError("Some Preload class entries are missing!");
        }
    }
}
