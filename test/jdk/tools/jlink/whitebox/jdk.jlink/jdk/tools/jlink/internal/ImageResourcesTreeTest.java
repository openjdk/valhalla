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

package jdk.tools.jlink.internal;

import jdk.tools.jlink.internal.ImageResourcesTree.Node;
import jdk.tools.jlink.internal.ImageResourcesTree.PackageNode;
import jdk.tools.jlink.internal.ImageResourcesTree.PackageNode.PackageReference;
import jdk.tools.jlink.internal.ImageResourcesTree.Tree;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class ImageResourcesTreeTest {

    public static void main(String[] args) {
        throw new RuntimeException("Well at least it got here...");
    }

    private static final String PACKAGE_PREFIX = "/packages/";

    // Copied from ImageResourcesTree.
    private static final int PKG_FLAG_HAS_NORMAL_CONTENT = 0x1;
    private static final int PKG_FLAG_HAS_PREVIEW_CONTENT = 0x2;
    private static final int PKG_FLAG_IS_PREVIEW_ONLY = 0x4;

    @Test
    public void expectedPackages() {
        // Paths are only to resources. Packages are inferred.
        List<String> paths = List.of(
                "/java.base/java/util/SomeClass.class",
                "/java.logging/java/util/logging/SomeClass.class");

        Tree tree = new Tree(paths);
        Map<String, Node> nodes = tree.getMap();
        Node packages = nodes.get("/packages");
        List<String> pkgNames = nodes.keySet().stream()
                .filter(p -> p.startsWith(PACKAGE_PREFIX))
                .map(p -> p.substring(PACKAGE_PREFIX.length()))
                .sorted()
                .toList();

        assertEquals(List.of("java", "java.util", "java.util.logging"), pkgNames);
        for (String pkgName : pkgNames) {
            PackageNode pkgNode = assertInstanceOf(PackageNode.class, packages.getChildren(pkgName));
            assertSame(nodes.get(PACKAGE_PREFIX + pkgNode.getName()), pkgNode);
        }
    }

    @Test
    public void expectedPackageEntries() {
        List<String> paths = List.of(
                "/java.base/java/util/SomeClass.class",
                "/java.logging/java/util/logging/SomeClass.class");

        Tree tree = new Tree(paths);
        Map<String, Node> nodes = tree.getMap();
        PackageNode pkgUtil = getPackageNode(nodes, "java.util");
        assertEquals(2, pkgUtil.moduleCount());
        List<PackageReference> modRefs = pkgUtil.modules().toList();

        List<String> modNames = modRefs.stream().map(PackageReference::name).toList();
        assertEquals(List.of("java.base", "java.logging"), modNames);

        PackageReference baseRef = modRefs.get(0);
        assertNonEmptyRef(baseRef, "java.base");
        assertEquals(PKG_FLAG_HAS_NORMAL_CONTENT, baseRef.flags());

        PackageReference loggingRef = modRefs.get(1);
        assertEmptyRef(loggingRef, "java.logging");
        assertEquals(0, loggingRef.flags());
    }

    @Test
    public void expectedPackageEntries_withPreviewResources() {
        List<String> paths = List.of(
                "/java.base/java/util/SomeClass.class",
                "/java.base/java/util/OtherClass.class",
                "/java.base/META-INF/preview/java/util/OtherClass.class",
                "/java.logging/java/util/logging/SomeClass.class");

        Tree tree = new Tree(paths);
        Map<String, Node> nodes = tree.getMap();
        PackageNode pkgUtil = getPackageNode(nodes, "java.util");
        List<PackageReference> modRefs = pkgUtil.modules().toList();

        PackageReference baseRef = modRefs.get(0);
        assertNonEmptyRef(baseRef, "java.base");
        assertEquals(PKG_FLAG_HAS_NORMAL_CONTENT | PKG_FLAG_HAS_PREVIEW_CONTENT, baseRef.flags());
    }

    @Test
    public void expectedPackageEntries_withPreviewOnlyPackages() {
        List<String> paths = List.of(
                "/java.base/java/util/SomeClass.class",
                "/java.base/META-INF/preview/java/util/preview/only/PreviewClass.class");

        Tree tree = new Tree(paths);
        Map<String, Node> nodes = tree.getMap();

        // Preview only package (with content).
        PackageNode nonEmptyPkg = getPackageNode(nodes, "java.util.preview.only");
        PackageReference nonEmptyRef = nonEmptyPkg.modules().findFirst().orElseThrow();
        assertNonEmptyPreviewOnlyRef(nonEmptyRef, "java.base");
        assertEquals(PKG_FLAG_IS_PREVIEW_ONLY | PKG_FLAG_HAS_PREVIEW_CONTENT, nonEmptyRef.flags());

        // Preview only packages can be empty.
        PackageNode emptyPkg = getPackageNode(nodes, "java.util.preview");
        PackageReference emptyRef = emptyPkg.modules().findFirst().orElseThrow();
        assertEmptyPreviewOnlyRef(emptyRef, "java.base");
        assertEquals(PKG_FLAG_IS_PREVIEW_ONLY, emptyRef.flags());
    }

    @Test
    public void expectedPackageEntries_sharedPackage() {
        // Many modules define the same package, all but one are empty.
        // Order shuffled to show reordering in entry list.
        // Expect: content -> empty{1..6} -> preview{1..2}
        List<String> paths = List.of(
                "/java.empty1/java/shared/one/SomeClass.class",
                "/java.preview1/META-INF/preview/java/shared/foo/SomeClass.class",
                "/java.empty3/java/shared/three/SomeClass.class",
                "/java.empty6/java/shared/six/SomeClass.class",
                "/java.preview2/META-INF/preview/java/shared/bar/SomeClass.class",
                "/java.empty5/java/shared/five/SomeClass.class",
                "/java.content/java/shared/MainPackageClass.class",
                "/java.empty2/java/shared/two/SomeClass.class",
                "/java.empty4/java/shared/four/SomeClass.class");

        Tree tree = new Tree(paths);
        Map<String, Node> nodes = tree.getMap();

        // Preview only package (with content).
        PackageNode sharedPkg = getPackageNode(nodes, "java.shared");
        assertEquals(9, sharedPkg.moduleCount());

        List<PackageReference> refs = sharedPkg.modules().toList();
        assertNonEmptyRef(refs.getFirst(), "java.content");
        assertEquals(PKG_FLAG_HAS_NORMAL_CONTENT, refs.getFirst().flags());

        // Empty non-preview refs after non-empty ref.
        int idx = 1;
        for (PackageReference emptyRef : refs.subList(1, 7)) {
            assertEmptyRef(emptyRef, "java.empty" + idx++);
            assertEquals(0, emptyRef.flags());
        }

        // Empty preview-only refs last.
        idx = 1;
        for (PackageReference emptyRef : refs.subList(7, 9)) {
            assertEmptyPreviewOnlyRef(emptyRef, "java.preview" + idx++);
            assertEquals(PKG_FLAG_IS_PREVIEW_ONLY, emptyRef.flags());
        }
    }

    static PackageNode getPackageNode(Map<String, Node> nodes, String pkgName) {
        return assertInstanceOf(PackageNode.class, nodes.get(PACKAGE_PREFIX + pkgName));
    }

    static void assertNonEmptyRef(PackageReference ref, String modName) {
        assertEquals(modName, ref.name(), "Unexpected module name: " + ref);
        assertFalse(ref.isEmpty(), "Expected non-empty reference: " + ref);
        assertFalse(ref.isPreviewOnly(), "Expected not preview-only: " + ref);
    }

    static void assertEmptyRef(PackageReference ref, String modName) {
        assertEquals(modName, ref.name(), "Unexpected module name: " + ref);
        assertTrue(ref.isEmpty(), "Expected empty reference: " + ref);
        assertFalse(ref.isPreviewOnly(), "Expected not preview-only: " + ref);
    }

    static void assertNonEmptyPreviewOnlyRef(PackageReference ref, String modName) {
        assertEquals(modName, ref.name(), "Unexpected module name: " + ref);
        assertFalse(ref.isEmpty(), "Expected empty reference: " + ref);
        assertTrue(ref.isPreviewOnly(), "Expected preview-only: " + ref);
    }

    static void assertEmptyPreviewOnlyRef(PackageReference ref, String modName) {
        assertEquals(modName, ref.name(), "Unexpected module name: " + ref);
        assertTrue(ref.isEmpty(), "Expected empty reference: " + ref);
        assertTrue(ref.isPreviewOnly(), "Expected preview-only: " + ref);
    }
}
