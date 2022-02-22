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
 * @bug 8280942
 * @summary Preload attribute should mention primitive classes when reference projection is used in descriptors
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main PreloadAttributeTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

public class PreloadAttributeTest {

    public primitive class P1 {}
    public primitive class P2 {}
    public primitive class P3 {}
    public primitive class P4 {}
    public primitive class P5 {}
    public primitive class P6 {}
    public primitive class P7 {}
    public primitive class P8 {}

    // We expect NO Preload Entries for ANY of P1 .. P4
    P1 p1;
    P2 foo(P3 p3) {
        P4 p4;
        return new P2();
    }

    // We expect Preload Entries for ALL of P5 .. P8
    P5.ref p5;
    P6.ref foo(P7.ref p7) {
        P8.ref p8;
        return null;
    }

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(PreloadAttributeTest.class.getResourceAsStream("PreloadAttributeTest.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of Preload attribute */
        Preload_attribute preloads = (Preload_attribute) cls.attributes.get(Attribute.Preload);
        if (preloads == null) {
            throw new AssertionError("Missing Preload attribute!");
        }
        if (preloads.number_of_classes != 4) {
            throw new AssertionError("Incorrect number of Preload classes");
        }

        int mask = 0xF0;
        for (int i = 0; i < preloads.number_of_classes; i++) {
            CONSTANT_Class_info clsInfo = cls.constant_pool.getClassInfo(
                                  preloads.value_class_info_index[i]);
            switch (clsInfo.getName()) {
                case "PreloadAttributeTest$P5":
                    mask &= ~16; break;
                case "PreloadAttributeTest$P6":
                    mask &= ~32; break;
                case "PreloadAttributeTest$P7":
                    mask &= ~64; break;
                case "PreloadAttributeTest$P8" :
                    mask &= ~128; break;
                default:
                    throw new AssertionError("Unexpected Preload class entry: " + clsInfo.getName());
            }
        }
        if (mask != 0) {
          throw new AssertionError("Some Preload class entries are missing!");
        }
    }
}
