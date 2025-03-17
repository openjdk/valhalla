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

package jdk.test.lib.value;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.test.lib.compiler.InMemoryJavaCompiler;

import static java.lang.classfile.ClassFile.*;
import static java.lang.constant.ConstantDescs.INIT_NAME;

/**
 * Compile a java file with InMemoryJavaCompiler, and then modify the resulting
 * class file to include strict modifier and null restriction attributes.
 */
public final class StrictCompiler {
    public static final String TEST_SRC = System.getProperty("test.src", "").trim();
    public static final String TEST_CLASSES = System.getProperty("test.classes", "").trim();
    private static final ClassDesc CD_Strict = ClassDesc.of("jdk.test.lib.value.Strict");
    // NR will stay in jdk.internal for now until we expose as a more formal feature
    private static final ClassDesc CD_NullRestricted = ClassDesc.of("jdk.internal.vm.annotation.NullRestricted");

    /**
     * @param args source and destination
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        Map<String, String> ins = new HashMap<>();
        List<String> javacOpts = new ArrayList<>();
        boolean encounteredSeparator = false;
        boolean deferSuperCall = false;
        for (var a : args) {
            if (encounteredSeparator) {
                javacOpts.add(a);
                continue;
            }
            if (a.endsWith(".java")) {
                String className = a.substring(0, a.length() - 5);
                Path src = Path.of(TEST_SRC, a);
                ins.put(className, Files.readString(src));
                continue;
            }
            switch (a) {
                case "--" -> encounteredSeparator = true;
                case "--deferSuperCall" -> deferSuperCall = true;
                default -> throw new IllegalArgumentException("Unknown option " + a);
            }
        }
        if (!javacOpts.contains("--source")) {
            javacOpts.add("--source");
            javacOpts.add(String.valueOf(Runtime.version().feature()));
        }
        if (!javacOpts.contains("--enable-preview")) {
            javacOpts.add("--enable-preview");
        }
        System.out.println(javacOpts);
        var classes = InMemoryJavaCompiler.compile(ins, javacOpts.toArray(String[]::new));
        Files.createDirectories(Path.of(TEST_CLASSES));
        for (var entry : classes.entrySet()) {
            if (deferSuperCall) {
                fixSuperAndDumpClass(entry.getKey(), entry.getValue());
            } else {
                dumpClass(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void fixSuperAndDumpClass(String name, byte[] rawBytes) throws IOException {
        var cm = ClassFile.of().parse(rawBytes);
        record FieldKey(Utf8Entry name, Utf8Entry type) {}
        Set<FieldKey> strictFinals = new HashSet<>();
        for (var f : cm.fields()) {
            var rvaa = f.findAttribute(Attributes.runtimeVisibleAnnotations());
            if (rvaa.isPresent()) {
                for (var anno : rvaa.get().annotations()) {
                    var descString = anno.className();
                    if (descString.equalsString(CD_Strict.descriptorString())) {
                        strictFinals.add(new FieldKey(f.fieldName(), f.fieldType()));
                    }
                }
            }
        }

        var thisClass = cm.thisClass();
        var superName = cm.superclass().orElseThrow().name();

        var rewritten = ClassFile.of().transformClass(cm, (clb, cle) -> {
            cond:
            if (cle instanceof MethodModel mm
                    && mm.methodName().equalsString(INIT_NAME)) {
                var code = mm.findAttribute(Attributes.code()).orElseThrow();
                var elements = code.elementList();
                int len = elements.size();
                int superCallPos = -1;
                int returnPos = -1;
                boolean deferSuperCall = false;
                for (int i = 0; i < len; i++) {
                    var e = elements.get(i);
                    if (superCallPos == -1) {
                        if (e instanceof InvokeInstruction inv &&
                                inv.opcode() == Opcode.INVOKESPECIAL &&
                                inv.method().name().equalsString(INIT_NAME) &&
                                inv.method().type().equalsString("()V") &&
                                inv.owner().name().equals(superName)) {
                            // Assume we are calling on uninitializedThis...
                            superCallPos = i;
                        }
                    } else if (!deferSuperCall) {
                        if (e instanceof FieldInstruction ins &&
                                ins.opcode() == Opcode.PUTFIELD &&
                                ins.owner().equals(thisClass) &&
                                strictFinals.contains(new FieldKey(ins.name(), ins.type()))) {
                            deferSuperCall = true;
                        }
                    }
                    if (e instanceof ReturnInstruction inst && inst.opcode() == Opcode.RETURN) {
                        if (returnPos != -1) {
                            throw new IllegalArgumentException("Control flow too complex");
                        } else {
                            returnPos = i;
                        }
                    }
                }
                if (elements.reversed().stream()
                        .<Instruction>mapMulti((e, sink) -> {
                            if (e instanceof Instruction i) {
                                sink.accept(i);
                            }
                        })
                        .findFirst()
                        .orElseThrow()
                        .opcode() != Opcode.RETURN) {
                    throw new IllegalArgumentException("Control flow too complex");
                }
                if (!deferSuperCall) {
                    break cond;
                }
                var suppliedElements = new ArrayList<>(elements);
                var foundLoad = suppliedElements.remove(superCallPos - 1);
                var foundSuperCall = suppliedElements.remove(superCallPos - 1);
                var foundReturnInst = suppliedElements.remove(returnPos - 2);
                suppliedElements.add(foundLoad);
                suppliedElements.add(foundSuperCall);
                suppliedElements.add(foundReturnInst);
                clb.withMethod(INIT_NAME, mm.methodTypeSymbol(), mm.flags().flagsMask(), mb -> mb
                        .transform(mm, MethodTransform.dropping(ce -> ce instanceof CodeModel))
                        .withCode(suppliedElements::forEach));
                return;
            }
            clb.with(cle);
        });

        dumpClass(name, rewritten);
    }

    private static void dumpClass(String name, byte[] rawBytes) throws IOException {
        var cm = ClassFile.of().parse(rawBytes);
        var transformed = ClassFile.of().transformClass(cm, ClassTransform.transformingFields(new FieldTransform() {
            int oldAccessFlags;
            boolean nullRestricted;
            boolean strict;

            @Override
            public void accept(FieldBuilder builder, FieldElement element) {
                if (element instanceof AccessFlags af) {
                    oldAccessFlags = af.flagsMask();
                    return;
                }
                builder.with(element);
                if (element instanceof RuntimeVisibleAnnotationsAttribute rvaa) {
                    for (var anno : rvaa.annotations()) {
                        var descString = anno.className();
                        if (descString.equalsString(CD_Strict.descriptorString())) {
                            strict = true;
                        } else if (descString.equalsString(CD_NullRestricted.descriptorString())) {
                            nullRestricted = true;
                        }
                    }
                }
            }

            @Override
            public void atEnd(FieldBuilder builder) {
                if (strict) {
                    oldAccessFlags |= ACC_STRICT;
                }
                builder.withFlags(oldAccessFlags);
                assert !nullRestricted || strict : name;
            }
        }));

        // Force preview
        transformed[4] = (byte) 0xFF;
        transformed[5] = (byte) 0xFF;
        Path dst = Path.of(TEST_CLASSES, name + ".class");
        Files.write(dst, transformed);
    }
}
