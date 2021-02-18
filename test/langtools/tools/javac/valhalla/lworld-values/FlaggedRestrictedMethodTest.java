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
 * @bug 8260870
 * @summary Generate RestrictedMethod attributes triggered via compile time flag
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -XDflattenWithTypeRestrictions FlaggedRestrictedMethodTest.java
 * @run main FlaggedRestrictedMethodTest
 */

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

import java.lang.invoke.RestrictedType;

public class FlaggedRestrictedMethodTest {

    primitive class Point {}
    primitive class Line {}

    static Point foo(int x, Point p, int y, Line l) {
        return p;
    }

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(FlaggedRestrictedMethodTest.class.getResourceAsStream("FlaggedRestrictedMethodTest.class"));

        for (Method meth: cls.methods) {
            if (meth.getName(cls.constant_pool).equals("foo")) {
                String desc = meth.descriptor.getValue(cls.constant_pool);
                if (!desc.equals("(ILFlaggedRestrictedMethodTest$Point$ref;ILFlaggedRestrictedMethodTest$Line$ref;)LFlaggedRestrictedMethodTest$Point$ref;"))
                    throw new AssertionError("Unexpected descriptor for method" + desc);
                RestrictedMethod_attribute rma =
                    (RestrictedMethod_attribute) meth.attributes.get(Attribute.RestrictedMethod);
                if (rma == null) {
                    throw new AssertionError("Missing restricted method attribute");
                }

                if (rma.getParameterCount() != 4) {
                    throw new AssertionError("Wrong parameter count");
                }
                int typeindex;
                String type;
                for (int i = 0; i < 4; i++) {
                    typeindex = rma.getRestrictedParameterType(i);
                    switch(i) {
                        case 0:
                        case 2:
                            if (typeindex != 0) {
                                throw new AssertionError("Unexpected type restriction");
                            }
                            break;
                        case 1:
                            if (!(type = cls.constant_pool.getUTF8Value(typeindex)).equals("QFlaggedRestrictedMethodTest$Point;")) {
                                throw new AssertionError("Unexpected type restriction " + type);
                            }
                            break;
                        case 3:
                            if (!(type = cls.constant_pool.getUTF8Value(typeindex)).equals("QFlaggedRestrictedMethodTest$Line;")) {
                                throw new AssertionError("Unexpected type restriction " + type);
                            }
                            break;
                    }
                }
                typeindex = rma.getRestrictedReturnType();
                if (typeindex == 0) {
                    throw new AssertionError("Missing type restriction");
                }
                if (!(type = cls.constant_pool.getUTF8Value(typeindex)).equals("QFlaggedRestrictedMethodTest$Point;")) {
                    throw new AssertionError("Unexpected type restriction " + type);
                }
            }
        }
    }
}
