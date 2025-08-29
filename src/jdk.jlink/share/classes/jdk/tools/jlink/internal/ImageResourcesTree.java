/*
 * Copyright (c) 2014, 2025, Oracle and/or its affiliates. All rights reserved.
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
package jdk.tools.jlink.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * A class to build a sorted tree of Resource paths as a tree of ImageLocation.
 *
 */
// XXX Public only due to the JImageTask / JImageTask code duplication
public final class ImageResourcesTree {
    //

    public static boolean isTreeInfoResource(String path) {
        return path.startsWith("/packages") || path.startsWith("/modules");
    }

    /**
     * Path item tree node.
     */
    // Visible for testing only.
    static class Node {

        private final String name;
        private final Map<String, Node> children = new TreeMap<>();
        private final Node parent;
        private ImageLocationWriter loc;

        private Node(String name, Node parent) {
            this.name = name;
            this.parent = parent;

            if (parent != null) {
                parent.children.put(name, this);
            }
        }

        public String getPath() {
            if (parent == null) {
                return "/";
            }
            return buildPath(this);
        }

        public String getName() {
            return name;
        }

        public Node getChildren(String name) {
            Node item = children.get(name);
            return item;
        }

        private static String buildPath(Node item) {
            if (item == null) {
                return null;
            }
            String path = buildPath(item.parent);
            if (path == null) {
                return item.getName();
            } else {
                return path + "/" + item.getName();
            }
        }
    }

    // Visible for testing only.
    static final class ResourceNode extends Node {

        public ResourceNode(String name, Node parent) {
            super(name, parent);
        }
    }

    /**
     * A 2nd level package directory, {@code "/packages/<package-name>"}.
     *
     * <p>While package paths can exist within many modules, for each package
     * there is at most one module in which that package has resources.
     *
     * <p>For example, the package path {@code java/util} exists in both the
     * {@code java.base} and {@code java.logging} modules. This means both
     * {@code "/packages/java.util/java.base"} and
     * {@code "/packages/java.util/java.logging"} will exist, but only
     * {@code "java.base"} entry will be marked as non-empty.
     *
     * <p>For preview mode however, a package that's empty in non-preview mode
     * can be non-empty in preview mode. Furthermore, packages which only exist
     * in preview mode (empty or not) need to be ignored in non-preview mode.
     *
     * <p>To account for this, the following flags are used for each module
     * entry in a package node:
     * <ul>
     *     <li>{@code HAS_NORMAL_CONTENT}: Packages with resources in normal
     *     mode. At most one entry will have this flag set.
     *     <li>{@code HAS_PREVIEW_CONTENT}: Packages with resources in preview
     *     mode. At most one entry will have this flag set.
     *     <li>{@code IS_PREVIEW_ONLY}: This is set for packages, empty
     *     or not, which exist only in preview mode.
     * </ul>
     *
     * <p>While there are 8 combinations of these 3 flags, some will never
     * occur (e.g. {@code HAS_NORMAL_CONTENT + IS_PREVIEW_ONLY}).
     *
     * <p>Package node entries are ordered such that:
     * <ul>
     *    <li>The unique entry marked as having content will be listed first
     *    (if it exists), regardless of any other flags.
     *    <li>Remaining empty entries are grouped, with preview-only entries
     *    listed at the end.
     *    <li>Within each group, entries are ordered by package name.
     * </ul>
     *
     * <p>When processing entries in normal (non preview) mode, entries marked
     * with {@code IS_PREVIEW_ONLY} must be ignored. If, after filtering, there
     * are no entries left, then the entire package must be ignored.
     *
     * <p>After this, in either mode, check the content flag(s) of the first
     * entry to determine if that module contains resources for the package.
     *
     * <p>If all entries are marked with {@code IS_PREVIEW_ONLY}
     */
    // Visible for testing only.
    static class PackageNode extends Node {
        private static final Comparator<PackageReference> ORDER_BY_FLAG =
                Comparator.comparing(PackageReference::isEmpty)
                        .thenComparing(PackageReference::isPreviewOnly)
                        .thenComparing(PackageReference::name);

        /** If set, the associated module has content in normal (non preview) mode. */
        private static final int PKG_FLAG_HAS_NORMAL_CONTENT = 0x1;
        /** If set, the associated module has content in preview mode. */
        private static final int PKG_FLAG_HAS_PREVIEW_CONTENT = 0x2;
        /** If set, this package only exists in preview mode. */
        private static final int PKG_FLAG_IS_PREVIEW_ONLY = 0x4;

        /**
         * A reference to a package. Empty packages can be located inside one or
         * more modules. A package with content exists in only one module.
         */
        static final class PackageReference {
            private final String name;
            private final int flags;

            PackageReference(String name, int flags) {
                this.name = Objects.requireNonNull(name);
                this.flags = flags;
            }

            String name() {return name;}

            int flags() {return flags;}

            boolean isEmpty() {
                return (flags & (PKG_FLAG_HAS_NORMAL_CONTENT | PKG_FLAG_HAS_PREVIEW_CONTENT)) == 0;
            }

            boolean isPreviewOnly() {
                return (flags & (PKG_FLAG_IS_PREVIEW_ONLY)) != 0;
            }

            @Override
            public String toString() {
                return String.format(Locale.ROOT,
                        "%s [has_normal_content=%s, has_preview_content=%s, is_preview_only=%s]",
                        name(),
                        (flags() & PKG_FLAG_HAS_NORMAL_CONTENT) != 0,
                        (flags() & PKG_FLAG_HAS_PREVIEW_CONTENT) != 0,
                        isPreviewOnly());
            }
        }

        // Outside this class, callers should access via modules() / moduleCount().
        private final Map<String, PackageReference> unsortedReferences = new HashMap<>();

        PackageNode(String name, Node parent) {
            super(name, parent);
        }

        private void addNormalReference(String moduleName, boolean hasContent) {
            if (unsortedReferences.containsKey(moduleName)) {
                throw new IllegalStateException("Reference already exists: " + moduleName);
            }
            int flags = hasContent ? PKG_FLAG_HAS_NORMAL_CONTENT : 0;
            unsortedReferences.put(moduleName, new PackageReference(moduleName, flags));
        }

        private void addOrUpdatePreviewReference(String moduleName, boolean hasContent) {
            PackageReference existingRef = unsortedReferences.get(moduleName);
            int flags = hasContent ? PKG_FLAG_HAS_PREVIEW_CONTENT : 0;
            if (existingRef == null) {
                flags |= PKG_FLAG_IS_PREVIEW_ONLY;
            } else {
                flags |= existingRef.flags();
            }
            // It is possible (but not worth checking for) that these flags are the same
            // as the existing reference (e.g. updating with an empty preview package).
            unsortedReferences.put(moduleName, new PackageReference(moduleName, flags));
        }

        int moduleCount() {
            return unsortedReferences.size();
        }

        Stream<PackageReference> modules() {
            return unsortedReferences.values().stream().sorted(ORDER_BY_FLAG);
        }

        private void validate() {
            // If there's a module for which this package has content, it should be first and unique.
            if (modules().skip(1).anyMatch(ref -> !ref.isEmpty())) {
                throw new RuntimeException("Multiple modules to contain package " + getName());
            }
        }
    }

    /**
     * Tree of nodes.
     */
    // Visible for testing only.
    static final class Tree {
        // When a package name is made for a path with preview resources, it
        // ends up with this prefix ('/' become '.' during conversion).
        private static final String PREVIEW_PACKAGE_PREFIX = "META-INF.preview.";

        private final Map<String, Node> directAccess = new HashMap<>();
        private final List<String> paths;
        private final Node root;
        private Node packages;

        // Visible for testing only.
        Tree(List<String> paths) {
            this.paths = paths;
            root = new Node("", null);
            buildTree();
        }

        private void buildTree() {
            Node modules = new Node("modules", root);
            directAccess.put(modules.getPath(), modules);

            Map<String, Set<String>> moduleToPackage = new TreeMap<>();
            Map<String, Set<String>> moduleToPreviewPackage = new TreeMap<>();
            Map<String, Set<String>> packageToModule = new TreeMap<>();
            Map<String, Set<String>> previewPackageToModule = new TreeMap<>();

            for (String p : paths) {
                if (!p.startsWith("/")) {
                    continue;
                }
                String[] split = p.split("/");
                // minimum length is 3 items: /<mod>/<pkg>
                if (split.length < 3) {
                    System.err.println("Resources tree, invalid data structure, "
                            + "skipping " + p);
                    continue;
                }
                Node current = modules;
                String module = null;
                for (int i = 0; i < split.length; i++) {
                    // When a non terminal node is marked as being a resource, something is wrong.
                    // It has been observed some badly created jar file to contain
                    // invalid directory entry marled as not directory (see 8131762)
                    if (current instanceof ResourceNode) {
                        System.err.println("Resources tree, invalid data structure, "
                                + "skipping " + p);
                        continue;
                    }
                    String s = split[i];
                    if (!s.isEmpty()) {
                        // First item, this is the module, simply add a new node to the
                        // tree.
                        if (module == null) {
                            module = s;
                        }
                        Node n = current.children.get(s);
                        if (n == null) {
                            if (i == split.length - 1) { // Leaf
                                n = new ResourceNode(s, current);
                                String pkg = toPackageName(n.parent);
                                //System.err.println("Adding a resource node. pkg " + pkg + ", name " + s);
                                if (pkg != null) {
                                    if (!pkg.startsWith("META-INF")) {
                                        moduleToPackage.computeIfAbsent(module, k -> new TreeSet<>()).add(pkg);
                                    } else if (pkg.startsWith(PREVIEW_PACKAGE_PREFIX)) {
                                        pkg = pkg.substring(PREVIEW_PACKAGE_PREFIX.length());
                                        moduleToPreviewPackage.computeIfAbsent(module, k -> new TreeSet<>()).add(pkg);
                                    }
                                }
                            } else { // put only sub trees, no leaf
                                n = new Node(s, current);
                                directAccess.put(n.getPath(), n);
                                String pkg = toPackageName(n);
                                if (pkg != null) {
                                    if (!pkg.startsWith("META-INF")) {
                                        packageToModule.computeIfAbsent(pkg, k -> new TreeSet<>()).add(module);
                                    } else if (pkg.startsWith(PREVIEW_PACKAGE_PREFIX)) {
                                        pkg = pkg.substring(PREVIEW_PACKAGE_PREFIX.length());
                                        previewPackageToModule.computeIfAbsent(pkg, k -> new TreeSet<>()).add(module);
                                    }
                                }
                            }
                        }
                        current = n;
                    }
                }
            }
            packages = new Node("packages", root);
            directAccess.put(packages.getPath(), packages);
            // Add all normal mode packages first.
            for (Map.Entry<String, Set<String>> entry : packageToModule.entrySet()) {
                String pkgName = entry.getKey();
                PackageNode pkgNode = getPackageNode(pkgName);
                for (String module : entry.getValue()) {
                    boolean hasContent = moduleToPackage.containsKey(module)
                            && moduleToPackage.get(module).contains(pkgName);
                    pkgNode.addNormalReference(module, hasContent);
                }
            }
            // Then add or update for preview mode.
            for (Map.Entry<String, Set<String>> entry : previewPackageToModule.entrySet()) {
                String pkgName = entry.getKey();
                PackageNode pkgNode = getPackageNode(pkgName);
                for (String module : entry.getValue()) {
                    boolean hasContent = moduleToPreviewPackage.containsKey(module)
                            && moduleToPreviewPackage.get(module).contains(pkgName);
                    pkgNode.addOrUpdatePreviewReference(module, hasContent);
                }
            }

            // Validate that the packages are well-formed.
            for (Node n : packages.children.values()) {
                ((PackageNode)n).validate();
            }

        }

        private PackageNode getPackageNode(String pkgName) {
            PackageNode pkgNode = (PackageNode) packages.getChildren(pkgName);
            if (pkgNode == null) {
                pkgNode = new PackageNode(pkgName, packages);
                if (directAccess.put(pkgNode.getPath(), pkgNode) != null) {
                    throw new IllegalStateException("Package nodes must only be added once: " + pkgNode);
                }
            }
            return pkgNode;
        }

        private String toResourceName(Node node) {
            if (!node.children.isEmpty()) {
                throw new RuntimeException("Node is not a resource");
            }
            return removeRadical(node);
        }

        private String getModule(Node node) {
            if (node.parent == null || node.getName().equals("modules")
                    || node.getName().startsWith("packages")) {
                return null;
            }
            String path = removeRadical(node);
            // "/xxx/...";
            path = path.substring(1);
            int i = path.indexOf("/");
            if (i == -1) {
                return path;
            } else {
                return path.substring(0, i);
            }
        }

        private String toPackageName(Node node) {
            if (node.parent == null) {
                return null;
            }
            String path = removeRadical(node.getPath(), "/modules/");
            String module = getModule(node);
            if (path.equals(module)) {
                return null;
            }
            String pkg = removeRadical(path, module + "/");
            return pkg.replace('/', '.');
        }

        private String removeRadical(Node node) {
            return removeRadical(node.getPath(), "/modules");
        }

        private String removeRadical(String path, String str) {
            if (!(path.length() < str.length())) {
                path = path.substring(str.length());
            }
            return path;
        }

        public Node getRoot() {
            return root;
        }

        public Map<String, Node> getMap() {
            return directAccess;
        }
    }

    private static final class LocationsAdder {

        private long offset;
        private final List<byte[]> content = new ArrayList<>();
        private final BasicImageWriter writer;
        private final Tree tree;

        LocationsAdder(Tree tree, long offset, BasicImageWriter writer) {
            this.tree = tree;
            this.offset = offset;
            this.writer = writer;
            addLocations(tree.getRoot());
        }

        private int addLocations(Node current) {
            if (current instanceof PackageNode) {
                PackageNode pkgNode = (PackageNode) current;
                int size = pkgNode.moduleCount() * 8;
                writer.addLocation(current.getPath(), offset, 0, size);
                offset += size;
            } else {
                int[] ret = new int[current.children.size()];
                int i = 0;
                for (java.util.Map.Entry<String, Node> entry : current.children.entrySet()) {
                    ret[i] = addLocations(entry.getValue());
                    i += 1;
                }
                if (current != tree.getRoot() && !(current instanceof ResourceNode)) {
                    int size = ret.length * 4;
                    writer.addLocation(current.getPath(), offset, 0, size);
                    offset += size;
                }
            }
            return 0;
        }

        private List<byte[]> computeContent() {
            // Map used to associate Tree item with locations offset.
            Map<String, ImageLocationWriter> outLocations = new HashMap<>();
            for (ImageLocationWriter wr : writer.getLocations()) {
                outLocations.put(wr.getFullName(), wr);
            }
            // Attach location to node
            for (Map.Entry<String, ImageLocationWriter> entry : outLocations.entrySet()) {
                Node item = tree.getMap().get(entry.getKey());
                if (item != null) {
                    item.loc = entry.getValue();
                }
            }
            computeContent(tree.getRoot(), outLocations);
            return content;
        }

        private int computeContent(Node current, Map<String, ImageLocationWriter> outLocations) {
            if (current instanceof PackageNode) {
                // /packages/<pkg name>
                PackageNode pkgNode = (PackageNode) current;
                int size = pkgNode.moduleCount() * 8;
                ByteBuffer buff = ByteBuffer.allocate(size);
                buff.order(writer.getByteOrder());
                pkgNode.modules().forEach(mod -> {
                    buff.putInt(mod.flags);
                    buff.putInt(writer.addString(mod.name));
                });
                byte[] arr = buff.array();
                content.add(arr);
                current.loc = outLocations.get(current.getPath());
            } else {
                int[] ret = new int[current.children.size()];
                int i = 0;
                for (java.util.Map.Entry<String, Node> entry : current.children.entrySet()) {
                    ret[i] = computeContent(entry.getValue(), outLocations);
                    i += 1;
                }
                if (ret.length > 0) {
                    int size = ret.length * 4;
                    ByteBuffer buff = ByteBuffer.allocate(size);
                    buff.order(writer.getByteOrder());
                    for (int val : ret) {
                        buff.putInt(val);
                    }
                    byte[] arr = buff.array();
                    content.add(arr);
                } else {
                    if (current instanceof ResourceNode) {
                        // A resource location, remove "/modules"
                        String s = tree.toResourceName(current);
                        current.loc = outLocations.get(s);
                    } else {
                        // empty "/packages" or empty "/modules" paths
                        current.loc = outLocations.get(current.getPath());
                    }
                }
                if (current.loc == null && current != tree.getRoot()) {
                    System.err.println("Invalid path in metadata, skipping " + current.getPath());
                }
            }
            return current.loc == null ? 0 : current.loc.getLocationOffset();
        }
    }

    private final List<String> paths;
    private final LocationsAdder adder;

    public ImageResourcesTree(long offset, BasicImageWriter writer, List<String> paths) {
        this.paths = new ArrayList<>();
        this.paths.addAll(paths);
        Collections.sort(this.paths);
        Tree tree = new Tree(this.paths);
        adder = new LocationsAdder(tree, offset, writer);
    }

    public void addContent(DataOutputStream out) throws IOException {
        List<byte[]> content = adder.computeContent();
        for (byte[] c : content) {
            out.write(c, 0, c.length);
        }
    }
}
