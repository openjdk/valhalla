/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @enablePreview
 * @library /test/lib
 * @run junit/othervm StrictClassFileGenerationTest
 */

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.constant.ClassDesc;
import java.util.List;

import jdk.test.lib.ByteCodeLoader;
import org.junit.jupiter.api.Test;

import static java.lang.classfile.ClassFile.ACC_FINAL;
import static java.lang.classfile.ClassFile.ACC_STRICT;
import static java.lang.constant.ConstantDescs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StrictClassFileGenerationTest {
    @Test
    void basicBranchTest() throws Throwable {
        var className = ClassDesc.of("Test");
        var classBytes = ClassFile.of().build(className, clb -> clb
                .withField("fs", CD_int, ACC_STRICT)
                .withField("fsf", CD_int, ACC_STRICT | ACC_FINAL)
                .withMethodBody(INIT_NAME, MTD_void, 0, cob -> cob
                        .aload(0)
                        .iconst_5()
                        .putfield(className, "fs", CD_int)
                        .aload(0)
                        .iconst_0()
                        .ifThenElse(thb -> thb
                                .iconst_3()
                                .putfield(className, "fsf", CD_int), elb -> elb
                                .iconst_2()
                                .putfield(className, "fsf", CD_int))
                        .aload(0)
                        .invokespecial(CD_Object, INIT_NAME, MTD_void)
                        .return_()));
        var clazz = ByteCodeLoader.load("Test", classBytes); // sanity check to pass verification
        var classModel = ClassFile.of().parse(classBytes);
        var ctorModel = classModel.methods().getFirst();
        var stackMaps = ctorModel.code().orElseThrow().findAttribute(Attributes.stackMapTable()).orElseThrow();
        assertEquals(2 * 2, stackMaps.entries().size()); // if -> else, then -> end of if + asserts
        var elseAssertFrame = stackMaps.entries().get(0); // else jump from if
        assertEquals(246, elseAssertFrame.frameType());
        assertEquals(List.of(ConstantPoolBuilder.of().nameAndTypeEntry("fsf", CD_int)), elseAssertFrame.unsetFields());
        var mergedAssertFrame = stackMaps.entries().get(2); // then jump to join else
        assertEquals(246, mergedAssertFrame.frameType());
        assertEquals(List.of(), mergedAssertFrame.unsetFields());
    }

    // Need testing for:
    // - Skip assert unset if there is no change
    // - Frames after this() delegating constructor call clears unset
    // - Correctly fails if flow merge has conflicting unset fields
}
