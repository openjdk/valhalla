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
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.compiler.InMemoryJavaCompiler;

import static java.lang.classfile.ClassFile.ACC_STRICT;

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
        List<String> opts = new ArrayList<>();
        for (var a : args) {
            if (a.endsWith(".java")) {
                String className = a.substring(0, a.length() - 5);
                Path src = Path.of(TEST_SRC, a);
                ins.put(className, Files.readString(src));
            } else {
                opts.add(a);
            }
        }
        if (!opts.contains("--source")) {
            opts.add("--source");
            opts.add(String.valueOf(Runtime.version().feature()));
        }
        if (!opts.contains("--enable-preview")) {
            opts.add("--enable-preview");
        }
        var classes = InMemoryJavaCompiler.compile(ins, opts.toArray(String[]::new));
        Files.createDirectories(Path.of(TEST_CLASSES));
        for (var entry : classes.entrySet()) {
            dumpClass(entry.getKey(), entry.getValue());
        }
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

        Path dst = Path.of(TEST_CLASSES, name + ".class");
        Files.write(dst, transformed);
    }
}
