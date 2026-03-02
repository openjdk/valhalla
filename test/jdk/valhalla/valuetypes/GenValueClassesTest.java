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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.spi.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * @test
 * @summary Tests build tool GenValueClasses annotation processor
 * @library /tools/lib
 * @run junit GenValueClassesTest
 */
public class GenValueClassesTest {
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(() -> new RuntimeException("javac tool not found"));

    // We cannot access compiled build tools in JTREG tests, but we can find the
    // sources and compile them. See findBuildToolsSrcRoot() for more details.
    private static final Path SRC_ROOT = Path.of("make", "jdk", "src", "classes");
    private static final Path PROCESSOR_SRC = Path.of(
            "build", "tools", "valhalla", "valuetypes", "GenValueClasses.java");

    // Compile the annotation processor once for all test cases.
    @TempDir
    private static Path processorDir = null;

    @BeforeAll
    static void compileAnnotationProcessor() {
        compile(findBuildToolsSrcRoot().resolve(PROCESSOR_SRC), processorDir);
    }

    @TempDir
    Path testDir = null;

    @Test
    public void simpleValueClass() throws IOException {
        String simpleValueClass =
                """
                package test;

                @jdk.internal.MigratedValueClass
                public /*VALUE*/ class SimpleValueClass {
                }
                """;
        Path relPath = writeTestSource("SimpleValueClass", simpleValueClass);
        compileTestClass(relPath);
        String transformedSrc = readTransformedSource(relPath);
        assertEquals(simpleValueClass.replace("/*VALUE*/", "value"), transformedSrc);
        assertTrue(transformedSrc.contains(" value class SimpleValueClass "));
    }

    @Test
    public void nestedValueClass() throws IOException {
        String nestedValueClass =
                """
                package test;

                public class NestedValueClass {
                    @jdk.internal.MigratedValueClass
                    private static final /*VALUE*/ class Nested {
                    }
                }
                """;
        Path relPath = writeTestSource("NestedValueClass", nestedValueClass);
        compileTestClass(relPath);
        String transformedSrc = readTransformedSource(relPath);
        assertEquals(nestedValueClass.replace("/*VALUE*/", "value"), transformedSrc);
        assertTrue(transformedSrc.contains(" value class Nested "));
    }

    @Test
    public void multipleValueClasses() throws IOException {
        String multipleValueClasses =
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

        Path relPath = writeTestSource("MultipleValueClasses", multipleValueClasses);
        compileTestClass(relPath);
        String transformedSrc = readTransformedSource(relPath);
        assertEquals(multipleValueClasses.replace("/*VALUE*/", "value"), transformedSrc);
        assertTrue(transformedSrc.contains(" value class MultipleValueClasses "));
        assertTrue(transformedSrc.contains(" value class First "));
        assertFalse(transformedSrc.contains(" value class Second "));
        assertTrue(transformedSrc.contains(" value class Third "));
    }

    @Test
    public void multilineClassDeclaration() throws IOException {
        // A slightly extreme case to show that the annotation processor can cope
        // with multiline class declarations and interleaved comments. The value
        // keyword is inserted after the last modifier with a leading space.
        String multilineClassDeclaration =
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
        Path relPath = writeTestSource("MultilineClassDeclaration", multilineClassDeclaration);
        compileTestClass(relPath);
        String transformedSrc = readTransformedSource(relPath);
        assertEquals(multilineClassDeclaration.replace("/*VALUE*/", "value"), transformedSrc);
    }

    private Path writeTestSource(String className, String source) throws IOException {
        // Remove the VALUE tokens to leave "clean" source (otherwise the
        // token will persist in the transformed output and get messy).
        // Tokens have a leading space to match how the keyword is injected.
        assertTrue(source.contains(" /*VALUE*/"), "invalid test source");
        String actualSrc = source.replace(" /*VALUE*/", "");

        Path relPath = Path.of("test", className + ".java");
        Path srcFile = testDir.resolve("src").resolve(relPath);
        Files.createDirectories(srcFile.getParent());
        Files.writeString(srcFile, actualSrc);
        return relPath;
    }

    private String readTransformedSource(Path relPath) throws IOException {
        return Files.readString(testDir.resolve("out").resolve(relPath));
    }

    private void compileTestClass(Path srcPath) {
        compile(testDir.resolve("src").resolve(srcPath), processorDir.resolve("compiled"),
                "--add-exports", "java.base/jdk.internal=ALL-UNNAMED",
                "-Avalueclasses.outdir=" + testDir.resolve("out"),
                "--processor-path", processorDir.toString(),
                "-processor", "build.tools.valhalla.valuetypes.GenValueClasses");
    }

    // NOTE: Since the in-memory compiler is limited and doesn't support getting
    // the Path of the "memo:" URIs is uses, it cannot be used for testing this
    // annotation processor, so we must perform on-disk compilation.
    private static void compile(Path srcFile, Path outDir, String... extraArgs) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        List<String> allArgs = new ArrayList<>();
        allArgs.add("-d");
        allArgs.add(outDir.toString());
        allArgs.addAll(Arrays.asList(extraArgs));
        allArgs.add(srcFile.toString());
        int exitValue = JAVAC_TOOL.run(
                new PrintWriter(out),
                new PrintWriter(err),
                allArgs.toArray(String[]::new));
        Assertions.assertEquals(0, exitValue, String.format(
                """
                Compilation failed: %s
                Stdout: %s
                Stderr: %s
                """, srcFile, out, err));
    }

    /**
     * The source root is {@code make/jdk/src/classes} from the JDK root, but
     * this may not be available in all test environments (and if it isn't, the
     * test should be skipped).
     *
     * <p>Similar to {@code test/langtools/tools/all/RunCodingRules.java}, we
     * attempt to locate this directory by walking from the {@code test.src}
     * directory.
     */
    private static Path findBuildToolsSrcRoot() {
        Path testSrc = Path.of(System.getProperty("test.src", "."));
        for (Path d = testSrc; d != null; d = d.getParent()) {
            if (Files.exists(d.resolve("TEST.ROOT"))) {
                d = d.getParent();
                Path srcRoot = d.resolve(SRC_ROOT);
                if (!Files.isDirectory(srcRoot)) {
                    break;
                }
                return srcRoot;
            }
        }
        return Assumptions.abort("Build tools source root not found; skipping test");
    }
}
