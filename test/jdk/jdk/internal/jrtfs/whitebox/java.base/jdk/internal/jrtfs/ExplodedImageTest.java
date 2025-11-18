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

package jdk.internal.jrtfs;

import jdk.internal.jimage.ImageReader;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests an {@link ExplodedImage} view of a class-file hierarchy.
 *
 * <p>For simplicity and performance, only a subset of the JRT files are copied
 * to disk for testing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExplodedImageTest {
    // The '@' prefix marks the entry as a preview entry which will be placed in
    // the '/modules/<module>/META-INF/preview/...' path.
    private static final Map<String, List<String>> IMAGE_ENTRIES = Map.of(
            "modfoo", Arrays.asList(
                    "com.foo.HasPreviewVersion",
                    "com.foo.NormalFoo",
                    "com.foo.bar.NormalBar",
                    // Replaces original class in preview mode.
                    "@com.foo.HasPreviewVersion",
                    // New class in existing package in preview mode.
                    "@com.foo.bar.IsPreviewOnly"),
            "modbar", Arrays.asList(
                    "com.bar.One",
                    "com.bar.Two",
                    // Two new packages in preview mode (new symbolic links).
                    "@com.bar.preview.stuff.Foo",
                    "@com.bar.preview.stuff.Bar"),
            "modgus", Arrays.asList(
                    // A second module with a preview-only empty package (preview).
                    "@com.bar.preview.other.Gus"));

    private static final Path PREVIEW_DIR = Path.of("META-INF", "preview");

    private Path modulesRoot;
    private SystemImage explodedImage;
    private String pathSeparator;

    @BeforeAll
    public void createTestDirectory(@TempDir Path modulesRoot) throws IOException {
        this.modulesRoot = modulesRoot;
        this.pathSeparator = modulesRoot.getFileSystem().getSeparator();
        buildExplodedImageContent(IMAGE_ENTRIES);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/modules",
            "/modules/modfoo",
            "/modules/modbar",
            "/modules/modfoo/com",
            "/modules/modfoo/com/foo",
            "/modules/modfoo/com/foo/bar"})
    public void testModuleDirectories_expected(String name) throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        assertDir(image, name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "//",
            "/modules/",
            "/modules/unknown",
            "/modules/modbar/",
            "/modules/modfoo//com",
            "/modules/modfoo/com/"})
    public void testModuleNodes_absent(String name) throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        assertAbsent(image, name);
    }

    @Test
    public void testModuleResources() throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        assertNode(image, "/modules/modfoo/com/foo/HasPreviewVersion.class");
        assertNode(image, "/modules/modbar/com/bar/One.class");

        assertNonPreviewVersion(image, "modfoo", "com.foo.HasPreviewVersion");
        assertNonPreviewVersion(image, "modfoo", "com.foo.NormalFoo");
        assertNonPreviewVersion(image, "modfoo", "com.foo.bar.NormalBar");
        assertNonPreviewVersion(image, "modbar", "com.bar.One");
    }

    @ParameterizedTest
    @CsvSource(delimiter = ':', value = {
            "modfoo:com/foo/HasPreviewVersion.class",
            "modbar:com/bar/One.class",
    })
    public void testResource_present(String modName, String resPath) throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        String canonicalNodeName = "/modules/" + modName + "/" + resPath;
        ImageReader.Node node = image.findNode(canonicalNodeName);
        assertTrue(node != null && node.isResource());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ':', value = {
            // Absolute resource names are not allowed.
            "modfoo:/com/bar/One.class",
            // Resource in wrong module.
            "modfoo:com/bar/One.class",
            "modbar:com/foo/HasPreviewVersion.class",
            // Directories are not returned.
            "modfoo:com/foo",
            "modbar:com/bar",
            // JImage entries exist for these, but they are not resources.
            "modules:modfoo/com/foo/HasPreviewVersion.class",
            "packages:com.foo/modfoo",
            // Empty module names/paths do not find resources.
            "'':modfoo/com/foo/HasPreviewVersion.class",
            "modfoo:''"})
    public void testResource_absent(String modName, String resPath) throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        // Non-existent resources names should either not be found,
        // or (in the case of directory nodes) not be resources.
        String canonicalNodeName = "/modules/" + modName + "/" + resPath;
        ImageReader.Node node = image.findNode(canonicalNodeName);
        assertTrue(node == null || !node.isResource());
    }

    @Test
    public void testPackageDirectories() throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        ImageReader.Node root = assertDir(image, "/packages");
        Set<String> pkgNames = root.getChildNames().collect(toSet());
        assertTrue(pkgNames.contains("/packages/com"));
        assertTrue(pkgNames.contains("/packages/com.foo"));
        assertTrue(pkgNames.contains("/packages/com.bar"));

        // Even though no classes exist directly in the "com" package, it still
        // creates a directory with links back to all the modules which contain it.
        Set<String> comLinks = assertDir(image, "/packages/com").getChildNames().collect(Collectors.toSet());
        assertTrue(comLinks.contains("/packages/com/modfoo"));
        assertTrue(comLinks.contains("/packages/com/modbar"));
    }

    @Test
    public void testPackageLinks() throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        ImageReader.Node moduleFoo = assertDir(image, "/modules/modfoo");
        ImageReader.Node moduleBar = assertDir(image, "/modules/modbar");
        assertSame(assertLink(image, "/packages/com.foo/modfoo").resolveLink(), moduleFoo);
        assertSame(assertLink(image, "/packages/com.bar/modbar").resolveLink(), moduleBar);
    }

    @Test
    public void testPreviewResources_disabled() throws IOException {
        var image = new ExplodedImage(modulesRoot, false);

        // No preview classes visible.
        assertNonPreviewVersion(image, "modfoo", "com.foo.HasPreviewVersion");
        assertNonPreviewVersion(image, "modfoo", "com.foo.NormalFoo");
        assertNonPreviewVersion(image, "modfoo", "com.foo.bar.NormalBar");

        // NormalBar exists but IsPreviewOnly doesn't.
        assertResource(image, "/modules/modfoo/com/foo/bar/NormalBar.class");
        assertAbsent(image, "/modules/modfoo/com/foo/bar/IsPreviewOnly.class");
        assertDirContents(image, "/modules/modfoo/com/foo", "HasPreviewVersion.class", "NormalFoo.class", "bar");
        assertDirContents(image, "/modules/modfoo/com/foo/bar", "NormalBar.class");
    }

    @Test
    public void testPreviewResources_enabled() throws IOException {
        var image = new ExplodedImage(modulesRoot, true);

        // Preview version of classes either overwrite existing entries or are added to directories.
        assertPreviewVersion(image, "modfoo", "com.foo.HasPreviewVersion");
        assertNonPreviewVersion(image, "modfoo", "com.foo.NormalFoo");
        assertNonPreviewVersion(image, "modfoo", "com.foo.bar.NormalBar");
        assertPreviewVersion(image, "modfoo", "com.foo.bar.IsPreviewOnly");

        // Both NormalBar and IsPreviewOnly exist (direct lookup and as child nodes).
        assertResource(image, "/modules/modfoo/com/foo/bar/NormalBar.class");
        assertResource(image, "/modules/modfoo/com/foo/bar/IsPreviewOnly.class");
        assertDirContents(image, "/modules/modfoo/com/foo", "HasPreviewVersion.class", "NormalFoo.class", "bar");
        assertDirContents(image, "/modules/modfoo/com/foo/bar", "NormalBar.class", "IsPreviewOnly.class");
    }

    @Test
    public void testPreviewOnlyPackages_disabled() throws IOException {
        var image = new ExplodedImage(modulesRoot, false);

        // No 'preview' package or anything inside it.
        assertDirContents(image, "/modules/modbar/com/bar", "One.class", "Two.class");
        assertAbsent(image, "/modules/modbar/com/bar/preview");
        assertAbsent(image, "/modules/modbar/com/bar/preview/stuff/Foo.class");

        // And no package link.
        assertAbsent(image, "/packages/com.bar.preview");
    }

    @Test
    public void testPreviewOnlyPackages_enabled() throws IOException {
        var image = new ExplodedImage(modulesRoot, true);

        // In preview mode 'preview' package exists with preview only content.
        assertDirContents(image, "/modules/modbar/com/bar", "One.class", "Two.class", "preview");
        assertDirContents(image, "/modules/modbar/com/bar/preview/stuff", "Foo.class", "Bar.class");
        assertResource(image, "/modules/modbar/com/bar/preview/stuff/Foo.class");

        // And package links exists.
        assertDirContents(image, "/packages/com.bar.preview", "modbar", "modgus");
    }

    @Test
    public void testPreviewModeLinks_disabled() throws IOException {
        var image = new ExplodedImage(modulesRoot, false);
        assertDirContents(image, "/packages/com.bar", "modbar");
        // Missing symbolic link and directory when not in preview mode.
        assertAbsent(image, "/packages/com.bar.preview");
        assertAbsent(image, "/packages/com.bar.preview.stuff");
        assertAbsent(image, "/modules/modbar/com/bar/preview");
        assertAbsent(image, "/modules/modbar/com/bar/preview/stuff");
    }

    @Test
    public void testPreviewModeLinks_enabled() throws IOException {
        var image = new ExplodedImage(modulesRoot, true);
        // In preview mode there is a new preview-only module visible.
        assertDirContents(image, "/packages/com.bar", "modbar", "modgus");
        // And additional packages are present.
        assertDirContents(image, "/packages/com.bar.preview", "modbar", "modgus");
        assertDirContents(image, "/packages/com.bar.preview.stuff", "modbar");
        assertDirContents(image, "/packages/com.bar.preview.other", "modgus");
        // And the preview-only content appears as we expect.
        assertDirContents(image, "/modules/modbar/com/bar", "One.class", "Two.class", "preview");
        assertDirContents(image, "/modules/modbar/com/bar/preview", "stuff");
        assertDirContents(image, "/modules/modbar/com/bar/preview/stuff", "Foo.class", "Bar.class");
        // In both modules in which it was added.
        assertDirContents(image, "/modules/modgus/com/bar", "preview");
        assertDirContents(image, "/modules/modgus/com/bar/preview", "other");
        assertDirContents(image, "/modules/modgus/com/bar/preview/other", "Gus.class");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testPreviewEntriesAlwaysHidden(boolean previewMode) throws IOException {
        var image = new ExplodedImage(modulesRoot, previewMode);
        // The META-INF directory exists, but does not contain the preview directory.
        ImageReader.Node dir = assertDir(image, "/modules/modfoo/META-INF");
        assertEquals(0, dir.getChildNames().filter(n -> n.endsWith("/preview")).count());
        // Neither the preview directory, nor anything in it, can be looked-up directly.
        assertAbsent(image, "/modules/modfoo/META-INF/preview");
        assertAbsent(image, "/modules/modfoo/META-INF/preview/com/foo");
        // HasPreviewVersion.class is a preview class in the test data, and thus appears in
        // two places in the jimage). Ensure the preview version is always hidden.
        String previewPath = "com/foo/HasPreviewVersion.class";
        assertNode(image, "/modules/modfoo/" + previewPath);
        assertAbsent(image, "/modules/modfoo/META-INF/preview/" + previewPath);
    }


    private static ImageReader.Node assertNode(SystemImage image, String name) throws IOException {
        ImageReader.Node node = image.findNode(name);
        assertNotNull(node, "Could not find node: " + name);
        return node;
    }

    private static ImageReader.Node assertResource(SystemImage image, String name) throws IOException {
        ImageReader.Node node = assertNode(image, name);
        assertTrue(node.isResource(), "Node was not a resource: " + name);
        return node;
    }

    private static String loadClassContent(SystemImage image, String module, String fqn) throws IOException {
        String name = "/modules/" + module + "/" + fqn.replace('.', '/') + ".class";
        ImageReader.Node node = assertResource(image, name);
        return new String(image.getResource(node), UTF_8);
    }

    private static void assertNonPreviewVersion(SystemImage image, String module, String fqn) throws IOException {
        assertEquals("Class: " + fqn, loadClassContent(image, module, fqn));
    }

    private static void assertPreviewVersion(SystemImage image, String module, String fqn) throws IOException {
        assertEquals("Preview: " + fqn, loadClassContent(image, module, fqn));
    }

    private static ImageReader.Node assertDir(SystemImage image, String name) throws IOException {
        ImageReader.Node dir = assertNode(image, name);
        assertTrue(dir.isDirectory(), "Node was not a directory: " + name);
        return dir;
    }

    private static void assertDirContents(SystemImage image, String name, String... expectedChildNames) throws IOException {
        ImageReader.Node dir = assertDir(image, name);
        Set<String> localChildNames = dir.getChildNames()
                .peek(s -> assertTrue(s.startsWith(name + "/")))
                .map(s -> s.substring(name.length() + 1))
                .collect(toSet());
        assertEquals(
                Set.of(expectedChildNames),
                localChildNames,
                String.format("Unexpected child names in directory '%s'", name));
    }

    private static ImageReader.Node assertLink(SystemImage image, String name) throws IOException {
        ImageReader.Node link = assertNode(image, name);
        assertTrue(link.isLink(), "Node should be a symbolic link: " + link.getName());
        return link;
    }

    private static void assertAbsent(SystemImage image, String name) throws IOException {
        assertNull(image.findNode(name), "Should not be able to find node: " + name);
    }

    private void buildExplodedImageContent(Map<String, List<String>> entries)
            throws IOException {
        for (var e : entries.entrySet()) {
            Path modDir = modulesRoot.resolve(e.getKey());
            Files.createDirectory(modDir);
            writeMarker(modDir);
            for (var fqn : e.getValue()) {
                boolean isPreviewEntry = fqn.startsWith("@");
                if (isPreviewEntry) {
                    fqn = fqn.substring(1);
                }
                Path p = Path.of(fqn.replace(".", pathSeparator) + ".class");
                if (isPreviewEntry) {
                    p = PREVIEW_DIR.resolve(p);
                }
                p = modDir.resolve(p);
                Files.createDirectories(p.getParent());
                writeMarker(p.getParent());
                Files.writeString(p, (isPreviewEntry ? "Preview" : "Class") + ": " + fqn, UTF_8);
            }
        }
    }

    private static void writeMarker(Path dir) throws IOException {
        Files.writeString(dir.resolve("_the.ignored.marker"), "Ignored", UTF_8);
    }
}
