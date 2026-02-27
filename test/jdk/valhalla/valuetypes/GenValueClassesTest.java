/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * @test
 * @summary Ensure annotation processing generates expected source files
 * @library /tools/lib
 * @compile ../../../../make/jdk/src/classes/build/tools/valhalla/valuetypes/GenValueClasses.java
 * @run junit GenValueClassesTest
 */
public class GenValueClassesTest {
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(() -> new RuntimeException("javac tool not found"));

    private static final String SIMPLE_VALUE_CLASS =
            """
            package test;

            @jdk.internal.MigratedValueClass
            public /*VALUE*/ class SimpleValueClass {
            }
            """;

    private static final String NESTED_VALUE_CLASS =
            """
            package test;

            public class NestedValueClass {
                @jdk.internal.MigratedValueClass
                private static final /*VALUE*/ class Nested {
                }
            }
            """;

    private static final String MULTIPLE_VALUE_CLASSES =
            """
            package test;

            @jdk.internal.MigratedValueClass
            public /*VALUE*/ class MultipleValueClasses {

                @jdk.internal.MigratedValueClass
                private static final /*VALUE*/ class First { }

                static final class Second { }

                @jdk.internal.MigratedValueClass
                private static /*VALUE*/ class Third { }
            }
            """;

    // A slightly extreme case to show that the annotation processor can cope
    // with multiline class declarations and interleaved comments. The value
    // keyword is insert after the last modifier with a leading space.
    private static final String MULTILINE_CLASS_DECLARATION =
            """
            package test;

            @jdk.internal.MigratedValueClass
            public
            /* Some comment */
            final /*VALUE*/
            /* Some other comment */
            class

            MultilineClassDeclaration {
            }
            """;

    @TempDir
    Path testDir = null;
    Path srcDir = null;
    Path outDir = null;

    @BeforeEach
    void setupDirs() throws IOException {
        this.srcDir = testDir.resolve("src");
        Files.createDirectories(srcDir);
        this.outDir = testDir.resolve("out");
        Files.createDirectories(outDir);
    }

    @Test
    public void simpleValueClass() throws IOException {
        Path relPath = writeTestSource("SimpleValueClass", SIMPLE_VALUE_CLASS);
        compileTestClass(relPath);
        String transformedSrc = Files.readString(outDir.resolve(relPath));
        assertEquals(SIMPLE_VALUE_CLASS.replace("/*VALUE*/", "value"), transformedSrc);
        assertTrue(transformedSrc.contains(" value class SimpleValueClass "));
    }

    @Test
    public void nestedValueClass() throws IOException {
        Path relPath = writeTestSource("NestedValueClass", NESTED_VALUE_CLASS);
        compileTestClass(relPath);
        String transformedSrc = Files.readString(outDir.resolve(relPath));
        assertEquals(NESTED_VALUE_CLASS.replace("/*VALUE*/", "value"), transformedSrc);
        assertTrue(transformedSrc.contains(" value class Nested "));
    }

    @Test
    public void multipleValueClasses() throws IOException {
        Path relPath = writeTestSource("MultipleValueClasses", MULTIPLE_VALUE_CLASSES);
        compileTestClass(relPath);
        String transformedSrc = Files.readString(outDir.resolve(relPath));
        assertEquals(MULTIPLE_VALUE_CLASSES.replace("/*VALUE*/", "value"), transformedSrc);
        assertTrue(transformedSrc.contains(" value class MultipleValueClasses "));
        assertTrue(transformedSrc.contains(" value class First "));
        assertFalse(transformedSrc.contains(" value class Second "));
        assertTrue(transformedSrc.contains(" value class Third "));
    }

    @Test
    public void multilineClassDeclaration() throws IOException {
        Path relPath = writeTestSource("MultilineClassDeclaration", MULTILINE_CLASS_DECLARATION);
        compileTestClass(relPath);
        String transformedSrc = Files.readString(outDir.resolve(relPath));
        assertEquals(MULTILINE_CLASS_DECLARATION.replace("/*VALUE*/", "value"), transformedSrc);
    }

    private Path writeTestSource(String className, String source) throws IOException {
        // Remove the VALUE tokens to leave "clean" source (otherwise the
        // token will persist in the transformed output and get messy).
        // Tokens have a leading space to match how the keyword is injected.
        assertTrue(source.contains(" /*VALUE*/"), "invalid test source");
        String actualSrc = source.replace(" /*VALUE*/", "");

        Path relPath = Path.of("test", className + ".java");
        Path srcFile = srcDir.resolve(relPath);
        Files.createDirectories(srcFile.getParent());
        Files.writeString(srcFile, actualSrc);
        return relPath;
    }

    // NOTE: Since the in-memory compiler is limited and doesn't support getting
    // the Path of the "memo:" URIs is uses, it cannot be used for testing this
    // annotation processor. Thus, we must perform on-disk compilation.
    private void compileTestClass(Path srcPath) {
        int exitValue = JAVAC_TOOL.run(System.out, System.err,
                "-d", testDir.resolve("compiled").toString(),
                "--add-exports", "java.base/jdk.internal=ALL-UNNAMED",
                "-Avalueclasses.outdir=" + outDir,
                "--processor-path", System.getProperty("test.classes"),
                "-processor", "build.tools.valhalla.valuetypes.GenValueClasses",
                srcDir.resolve(srcPath).toString());
        assertEquals(0, exitValue);
    }
}
