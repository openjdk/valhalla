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
 * @bug 8280164 8334313
 * @summary Check emission of LoadableDescriptors attribute
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @enablePreview
 * @run main LoadableDescriptorsAttributeTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;

public class LoadableDescriptorsAttributeTest {

    value class V1 {}
    value class V2 {}
    value class V3 {}
    value class V4 {}
    value class V5 {}
    value class V6 {}
    value class V7 {}
    value class V8 {}
    value class V9 {}
    abstract value class V10 {}

    static final value class X {
        final V1 [] v1 = null; // field descriptor, encoding array type - no LoadableDescriptors.
        V2 foo() {  // method descriptor encoding value type, to be preloaded
            return null;
        }
        void foo(V3 v3) { // method descriptor encoding value type, to be preloaded
        }
        void foo(int x) {
            V4 [] v4 = null; // local variable encoding array type - no preload.
        }
        void goo(V6[] v6) { // parameter uses value type but as array component - no preload.
            V5 v5 = null;  // preload value type used for local type.
            if (v5 == null) {
                // ...
            } else {
               V5 [] v52 = null;
            }
        }
        final V7 v7 = null; // field descriptor uses value type - to be preloaded.
        V8 [] goo(V9 [] v9) { // neither V8 nor V9 call for preload being array component types
            return null;
        }
        V10 v10 = null; // abstract shouldn't be in the loadable descriptors attr
    }
    // So we expect ONLY V2, V3 V5, V7 to be in LoadableDescriptors list

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(LoadableDescriptorsAttributeTest.class.getResourceAsStream("LoadableDescriptorsAttributeTest$X.class"));

        if (cls == null) {
            throw new AssertionError("Could not locate the class files");
        }

        /* Check emission of LoadableDescriptors attribute */
        LoadableDescriptors_attribute descriptors = (LoadableDescriptors_attribute) cls.attributes.get(Attribute.LoadableDescriptors);
        if (descriptors == null) {
            throw new AssertionError("Missing LoadableDescriptors attribute!");
        }
        if (descriptors.number_of_descriptors != 4) {
            throw new AssertionError("Incorrect number of loadable descriptors");
        }

        int mask = 0x56;
        for (int i = 0; i < descriptors.number_of_descriptors; i++) {
            CONSTANT_Utf8_info clsInfo = cls.constant_pool.getUTF8Info(
                                  descriptors.descriptor_info_index[i]);
            switch (clsInfo.value) {
                case "LLoadableDescriptorsAttributeTest$V2;":
                    mask &= ~2; break;
                case "LLoadableDescriptorsAttributeTest$V3;":
                    mask &= ~4; break;
                case "LLoadableDescriptorsAttributeTest$V5;":
                    mask &= ~16; break;
                case "LLoadableDescriptorsAttributeTest$V7;" :
                    mask &= ~64; break;
                default:
                    throw new AssertionError("Unexpected LoadableDescriptors entry!");
            }
        }
        if (mask != 0) {
          throw new AssertionError("Some LoadableDescriptors entries are missing!");
        }
    }
}
