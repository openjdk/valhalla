/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8267542
 * @summary Verify that necessary checkcasts are generated while acessing an instance
 *          field/method through a reference projection.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile CheckNeededCastInMemberAccess.java
 * @run main CheckNeededCastInMemberAccess
 */

import java.io.File;
import java.io.IOException;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Code_attribute.InvalidIndex;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CheckNeededCastInMemberAccess {

    static primitive class Point {
        int x = 0, y = 0;
        void foo() {
            Point p0 = new Point();
            int x0 = p0.x;
            p0.foo();
            Point.ref p1 = null;
            int x1 = p1.x;
            p1.foo();
        }
    }

    public static void main(String[] args)
            throws IOException, ConstantPoolException, InvalidDescriptor, InvalidIndex {
        new CheckNeededCastInMemberAccess()
                .checkClassFile(new File(System.getProperty("test.classes", "."),
                    CheckNeededCastInMemberAccess.Point.class.getName() + ".class"));
    }

    void checkClassFile(File file)
            throws IOException, ConstantPoolException, InvalidDescriptor, InvalidIndex {
        ClassFile classFile = ClassFile.read(file);
        ConstantPool constantPool = classFile.constant_pool;

        Method method = Arrays.stream(classFile.methods)
                              .filter(m -> getName(m, constantPool)
                                               .equals("foo"))
                              .findAny()
                              .get();
        String expectedInstructions = """
                                    invokestatic
                                    astore_1
                                    aload_1
                                    getfield
                                    istore_2
                                    aload_1
                                    invokevirtual
                                    aconst_null
                                    astore_3
                                    aload_3
                                    checkcast
                                    getfield
                                    istore
                                    aload_3
                                    checkcast
                                    invokevirtual
                                    return
                                      """;
        Code_attribute code = (Code_attribute) method.attributes
                .get(Attribute.Code);
        String actualInstructions = printCode(code);
        if (!expectedInstructions.equals(actualInstructions)) {
            throw new AssertionError("Unexpected instructions found:\n" +
                                     actualInstructions);
        }
    }

    String printCode(Code_attribute code) {
        return StreamSupport.stream(code.getInstructions().spliterator(), false)
                            .map(Instruction::getMnemonic)
                            .collect(Collectors.joining("\n", "", "\n"));
    }

    String getName(Method m, ConstantPool constantPool) {
        try {
            return m.getName(constantPool);
        } catch (ConstantPoolException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
