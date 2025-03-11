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
 * @build jdk.test.lib.ByteCodeLoader
 * @run junit/othervm -Xverify StrictStackMapsTest
 */

import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import jdk.test.lib.ByteCodeLoader;
import org.junit.jupiter.api.Test;

import static java.lang.classfile.ClassFile.*;
import static java.lang.constant.ConstantDescs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrictStackMapsTest {
    @Test
    void basicBranchTest() throws Throwable {
        var className = "Test";
        var classDesc = ClassDesc.of(className);
        var classBytes = ClassFile.of().build(classDesc, clb -> clb
                .withField("fs", CD_int, ACC_STRICT)
                .withField("fsf", CD_int, ACC_STRICT | ACC_FINAL)
                .withMethodBody(INIT_NAME, MTD_void, 0, cob -> cob
                        .aload(0)
                        .iconst_5()
                        .putfield(classDesc, "fs", CD_int)
                        .aload(0)
                        .iconst_0()
                        .ifThenElse(thb -> thb
                                .iconst_3()
                                .putfield(classDesc, "fsf", CD_int), elb -> elb
                                .iconst_2()
                                .putfield(classDesc, "fsf", CD_int))
                        .aload(0)
                        .invokespecial(CD_Object, INIT_NAME, MTD_void)
                        .return_()));
        var clazz = ByteCodeLoader.load(className, classBytes); // sanity check to pass verification
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

    @Test
    void skipUnnecessaryUnsetFramesTest() throws Throwable {
        var className = "Test";
        var classDesc = ClassDesc.of(className);
        var classBytes = ClassFile.of().build(classDesc, clb -> clb
                .withField("fPlain", CD_char, ACC_PRIVATE)
                .withField("fs", CD_int, ACC_STRICT)
                .withField("fsf", CD_int, ACC_STRICT | ACC_FINAL)
                .withMethodBody(INIT_NAME, MTD_void, 0, cob -> cob
                        .aload(0)
                        .iconst_5()
                        .putfield(classDesc, "fs", CD_int)
                        .aload(0)
                        .iconst_0()
                        .ifThenElse(thb -> thb
                                .iconst_3()
                                .putfield(classDesc, "fPlain", CD_int), elb -> elb
                                .iconst_2()
                                .putfield(classDesc, "fPlain", CD_int))
                        .aload(0)
                        .iconst_5()
                        .putfield(classDesc, "fsf", CD_int)
                        .aload(0)
                        .invokespecial(CD_Object, INIT_NAME, MTD_void)
                        .return_()));
        var clazz = ByteCodeLoader.load(className, classBytes); // sanity check to pass verification
        var classModel = ClassFile.of().parse(classBytes);
        var ctorModel = classModel.methods().getFirst();
        var stackMaps = ctorModel.code().orElseThrow().findAttribute(Attributes.stackMapTable()).orElseThrow();
        assertEquals(2 + 1, stackMaps.entries().size()); // if -> else, then -> end of if, just one assert
        var assertFrame = stackMaps.entries().get(0); // else jump from if
        assertEquals(246, assertFrame.frameType());
        assertEquals(List.of(ConstantPoolBuilder.of().nameAndTypeEntry("fsf", CD_int)), assertFrame.unsetFields());
    }

    @Test
    void clearUnsetAfterThisConstructorCallTest() throws Throwable {
        var className = "Test";
        var classDesc = ClassDesc.of(className);
        var fullArgsCtorDesc = MethodTypeDesc.of(CD_void, CD_int, CD_int);
        var classBytes = ClassFile.of().build(classDesc, clb -> clb
                .withField("fPlain", CD_int, ACC_PRIVATE)
                .withField("fs", CD_int, ACC_STRICT)
                .withField("fsf", CD_int, ACC_STRICT | ACC_FINAL)
                // record-style ctor
                .withMethodBody(INIT_NAME, fullArgsCtorDesc, 0, cob -> cob
                        .aload(0)
                        .iload(1)
                        .putfield(classDesc, "fs", CD_int)
                        .aload(0)
                        .iload(2)
                        .putfield(classDesc, "fsf", CD_int)
                        .aload(0)
                        .invokespecial(CD_Object, INIT_NAME, MTD_void)
                        .return_())
                // delegates to the other ctor
                .withMethodBody(INIT_NAME, MTD_void, 0, cob -> cob
                        .aload(0)
                        .iconst_5()
                        .iconst_0()
                        .invokespecial(classDesc, INIT_NAME, fullArgsCtorDesc)
                        .aload(0)
                        .iconst_1()
                        .ifThenElse(thb -> thb
                                .iconst_3()
                                .putfield(classDesc, "fPlain", CD_int), elb -> elb
                                .iconst_2()
                                .putfield(classDesc, "fPlain", CD_int))
                        .return_()));
        var clazz = ByteCodeLoader.load(className, classBytes); // sanity check to pass verification
        var classModel = ClassFile.of().parse(classBytes);
        var ctorModel = classModel.methods().stream()
                .filter(m -> m.methodType().equalsString(MTD_void.descriptorString()))
                .findFirst().orElseThrow();
        var stackMaps = ctorModel.code().orElseThrow().findAttribute(Attributes.stackMapTable()).orElseThrow();
        assertEquals(2 + 1, stackMaps.entries().size()); // assert empty, if -> else, then -> end of if
        var assertFrame = stackMaps.entries().get(0); // assert empty for if -> else
        assertEquals(246, assertFrame.frameType());
        assertEquals(List.of(), assertFrame.unsetFields());
    }

    @Test
    void allowMultiAssignTest() throws Throwable {
        var className = "Test";
        var classDesc = ClassDesc.of(className);
        var classBytes = ClassFile.of().build(classDesc, clb -> clb
                .withField("fs", CD_int, ACC_STRICT)
                .withField("fsf", CD_int, ACC_STRICT | ACC_FINAL)
                .withMethodBody(INIT_NAME, MTD_void, 0, cob -> cob
                        .aload(0)
                        .iconst_1()
                        .ifThenElse(thb -> thb
                                .iconst_3()
                                .putfield(classDesc, "fs", CD_int), elb -> elb
                                // frame 0
                                .iconst_2()
                                .putfield(classDesc, "fsf", CD_int))
                        // frame 1
                        .aload(0)
                        .iconst_5()
                        .putfield(classDesc, "fs", CD_int)
                        .aload(0)
                        .loadConstant(12)
                        .putfield(classDesc, "fsf", CD_int)
                        .aload(0)
                        .invokespecial(CD_Object, INIT_NAME, MTD_void)
                        .return_()));
        var clazz = ByteCodeLoader.load(className, classBytes); // sanity check to pass verification
        var classModel = ClassFile.of().parse(classBytes);
        var ctorModel = classModel.methods().getFirst();
        var stackMaps = ctorModel.code().orElseThrow().findAttribute(Attributes.stackMapTable()).orElseThrow();
        assertEquals(2, stackMaps.entries().size(), () -> stackMaps.entries().toString()); // no assert frames
    }

    @Test
    void failOnUnsetNotClearTest() throws Throwable {
        var className = "Test";
        var classDesc = ClassDesc.of(className);
        assertThrows(IllegalArgumentException.class, () -> ClassFile.of().build(classDesc, clb -> clb
                .withField("fs0", CD_int, ACC_STRICT)
                .withField("fs1", CD_int, ACC_STRICT | ACC_FINAL)
                .withMethodBody(INIT_NAME, MTD_void, 0, cob -> cob
                        .aload(0)
                        .iconst_0()
                        .ifThenElse(thb -> thb
                                .iconst_3()
                                .putfield(classDesc, "fs0", CD_int), elb -> elb
                                .iconst_2()
                                .putfield(classDesc, "fs1", CD_int))
                        .aload(0)
                        .invokespecial(CD_Object, INIT_NAME, MTD_void)
                        .return_())));
    }

    @Test
    void basicTransformToStrictTest() throws Throwable {
        var className = "Test";
        var classDesc = ClassDesc.of(className);
        var classBytes = ClassFile.of().build(classDesc, clb -> clb
                .withField("fs", CD_int, 0)
                .withField("fsf", CD_int, ACC_FINAL)
                .withMethodBody(INIT_NAME, MTD_void, 0, cob -> cob
                        .aload(0)
                        .iconst_5()
                        .putfield(classDesc, "fs", CD_int)
                        .aload(0)
                        .iconst_0()
                        .ifThenElse(thb -> thb
                                .iconst_3()
                                .putfield(classDesc, "fsf", CD_int), elb -> elb
                                .iconst_2()
                                .putfield(classDesc, "fsf", CD_int))
                        .aload(0)
                        .invokespecial(CD_Object, INIT_NAME, MTD_void)
                        .return_()));

        classBytes = ClassFile.of().transformClass(ClassFile.of().parse(classBytes), ClassTransform.transformingFields((fb, fe) -> {
            if (fe instanceof AccessFlags acc) {
                fb.withFlags(acc.flagsMask() | ACC_STRICT);
            } else {
                fb.with(fe);
            }
        }));

        var clazz = ByteCodeLoader.load(className, classBytes); // sanity check to pass verification
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
}
