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
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/*
 * @test id=normal
 * @summary A parity test for the default runtime JRT file system.
 * @run junit/othervm -esa JrtFileSystemClassParityTest
 */

/*
 * @test id=preview
 * @summary A parity test for the default runtime JRT file system (in preview mode).
 * @run junit/othervm -esa --enable-preview JrtFileSystemClassParityTest
 */
public class JrtFileSystemClassParityTest {
    private static final String CLASS_SUFFIX = ".class";
    private static final Path MODULES_ROOT =
            FileSystems.getFileSystem(URI.create("jrt:/")).getPath("/modules");

    @Test
    public void testResourceAsStreamParity() throws IOException {
        List<String> modNames;
        try (Stream<Path> modDirs = Files.list(MODULES_ROOT)) {
            modNames = modDirs.map(MODULES_ROOT::relativize).map(Object::toString).toList();
        }
        for (String modName : modNames) {
            Path moduleInfo = MODULES_ROOT.resolve(modName, "module-info.class");
            try (Stream<Path> classFiles = Files.walk(MODULES_ROOT.resolve(modName))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .filter(p -> !p.equals(moduleInfo))) {
                classFiles.forEach(JrtFileSystemClassParityTest::testParity);
            }
        }
    }

    private static void testParity(Path jrtClassFile) {
        try {
            byte[] jrtBytes = Files.readAllBytes(jrtClassFile);
            // Remove /modules/<mod-name>/ leading segments.
            String relPath = jrtClassFile.subpath(2, jrtClassFile.getNameCount()).toString();
            String fqn = relPath.substring(0, relPath.length() - CLASS_SUFFIX.length()).replace('/', '.');
            String baseName = fqn.substring(fqn.lastIndexOf('.') + 1);
            Class<?> cls = Class.forName(fqn, false, null);
            byte[] classBytes;
            try (InputStream is = cls.getResourceAsStream(baseName + CLASS_SUFFIX)) {
                classBytes = Objects.requireNonNull(is).readAllBytes();
            }
            assertArrayEquals(jrtBytes, classBytes, "Class: " + fqn);
        } catch (ClassNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
