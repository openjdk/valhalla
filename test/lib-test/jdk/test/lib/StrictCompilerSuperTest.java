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

/* @test
 * @bug 8351362
 * @summary Unit Test for StrictCompiler super rewrite
 * @enablePreview
 * @library /test/lib
 * @run main/othervm jdk.test.lib.value.StrictCompiler --deferSuperCall StrictCompilerSuperTest.java
 * @run junit StrictCompilerSuperTest
 */

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.util.ArrayList;

import jdk.test.lib.value.Strict;
import org.junit.jupiter.api.Test;

import static java.lang.classfile.ClassFile.*;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StrictCompilerSuperTest {
    @Test
    void testReflectRewrittenRecord() throws Throwable {
        for (var field : Rec.class.getDeclaredFields()) {
            assertEquals(ACC_PRIVATE | ACC_STRICT | ACC_FINAL, field.getModifiers(), () -> "For field: " + field.getName());
        }
    }

    @Test
    void testRewrittenStrictAccessInClassFile() throws Throwable {
        ClassModel cm;
        try (var in = StrictCompilerSuperTest.class.getResourceAsStream("/StrictCompilerSuperTest$Rec.class")) {
            cm = ClassFile.of().parse(in.readAllBytes());
        }
        for (var f : cm.fields()) {
            assertEquals(ACC_PRIVATE | ACC_STRICT | ACC_FINAL, f.flags().flagsMask(), () -> "Field " + f);
        }
    }

    @Test
    void testRewrittenCtorBytecode() throws Throwable {
        ClassModel cm;
        try (var in = StrictCompilerSuperTest.class.getResourceAsStream("/StrictCompilerSuperTest$Rec.class")) {
            cm = ClassFile.of().parse(in.readAllBytes());
        }
        var ctor = cm.methods().stream().filter(m -> m.methodName().equalsString(INIT_NAME)).findFirst().orElseThrow();
        var insts = new ArrayList<Instruction>();
        ctor.findAttribute(Attributes.code()).orElseThrow().forEach(ce -> {
            if (ce instanceof Instruction inst) {
                insts.add(inst);
            }
        });
        assertSame(Opcode.RETURN, insts.getLast().opcode());
        assertSame(Opcode.INVOKESPECIAL, insts.get(insts.size() - 2).opcode());
    }

    record Rec(@Strict int a, @Strict long b) {}
}
