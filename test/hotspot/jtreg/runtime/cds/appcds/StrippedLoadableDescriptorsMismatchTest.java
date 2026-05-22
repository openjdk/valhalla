/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Ensure archived nullable flat field metadata is validated without LoadableDescriptors.
 * @requires vm.cds
 * @library /test/lib
 * @enablePreview
 * @modules java.base/jdk.internal.vm.annotation
 * @compile StrippedLoadableDescriptorsMismatchTest.java
 * @run driver jdk.test.lib.helpers.StrictProcessor StrippedLDHolder
 * @run main/othervm StrippedLoadableDescriptorsMismatchTest
 */

import java.io.File;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.attribute.LoadableDescriptorsAttribute;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.test.lib.helpers.StrictInit;

public class StrippedLoadableDescriptorsMismatchTest {
    public static void main(String[] args) throws Exception {
        String appJar = buildJarWithStrippedLoadableDescriptors();
        String pointClassFile = new File(System.getProperty("test.classes", "."),
                                         "StrippedLDPoint.class").getPath();

        TestCommon.dump(appJar, TestCommon.list("StrippedLDPoint", "StrippedLDHolder"),
                        "--enable-preview",
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-XX:+UseNullableNonAtomicValueFlattening");

        TestCommon.checkExec(TestCommon.exec(appJar,
                        "--enable-preview",
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-XX:+UseNullableNonAtomicValueFlattening",
                        "StrippedLDApp", pointClassFile));
    }

    private static String buildJarWithStrippedLoadableDescriptors() throws Exception {
        Path testClasses = Path.of(System.getProperty("test.classes", "."));
        Path jarClasses = testClasses.resolve("stripped_loadable_descriptors_mismatch");
        Files.createDirectories(jarClasses);

        copyClass(testClasses, jarClasses, "StrippedLDApp");
        copyClass(testClasses, jarClasses, "StrippedLDPoint");
        stripLoadableDescriptors(testClasses, jarClasses, "StrippedLDHolder");

        return JarBuilder.build("stripped_loadable_descriptors_mismatch", jarClasses.toFile(), null);
    }

    private static void copyClass(Path fromDir, Path toDir, String className) throws Exception {
        Files.copy(fromDir.resolve(className + ".class"),
                   toDir.resolve(className + ".class"),
                   StandardCopyOption.REPLACE_EXISTING);
    }

    private static void stripLoadableDescriptors(Path fromDir, Path toDir, String className) throws Exception {
        ClassFile cf = ClassFile.of();
        byte[] original = Files.readAllBytes(fromDir.resolve(className + ".class"));
        var model = cf.parse(original);
        if (model.findAttribute(Attributes.loadableDescriptors()).isEmpty()) {
            throw new RuntimeException(className + " must have a LoadableDescriptors attribute before transformation");
        }

        byte[] transformed = cf.transformClass(model,
            ClassTransform.dropping(e -> e instanceof LoadableDescriptorsAttribute));
        if (cf.parse(transformed).findAttribute(Attributes.loadableDescriptors()).isPresent()) {
            throw new RuntimeException("Failed to strip LoadableDescriptors from " + className);
        }
        Files.write(toDir.resolve(className + ".class"), transformed);
    }
}

class StrippedLDApp {
    public static void main(String[] args) throws Throwable {
        Path classFile = Path.of(args[0]);
        byte[] classBytes = Files.readAllBytes(classFile);
        Class<?> fieldClass = MethodHandles.lookup().defineClass(classBytes);

        StrippedLDHolder holder = new StrippedLDHolder();

        if (holder.p.getClass() != fieldClass) {
            throw new RuntimeException("Mismatched field class");
        }
    }
}

class StrippedLDHolder {
    @StrictInit
    final StrippedLDPoint p;

    StrippedLDHolder() {
        p = new StrippedLDPoint();
        super();
    }
}

@LooselyConsistentValue
value class StrippedLDPoint {
    int x = 0;
    int y = 0;
}
