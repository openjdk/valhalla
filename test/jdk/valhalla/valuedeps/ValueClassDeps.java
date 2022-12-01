/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.List;
import java.util.Optional;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import static jdk.internal.org.objectweb.asm.Opcodes.*;


/*
 * @test
 * @summary scan the jrt file system for classes that use Value Object features
 * including value classes, or refer to value classes with the Preload attribute.
 *
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main/othervm --enable-preview ValueClassDeps --summary java.base
 * @run main/othervm --enable-preview ValueClassDeps --summary
 * @run main/othervm --enable-preview ValueClassDeps --value-classes java.base
 * @run main/othervm --enable-preview ValueClassDeps --preload-classes java.base
 */

/**
 * Scans all classes in the JDK for those matching a certain set of criteria.
 * Scanning is done over the jrt: filesystem.
 * This returns a list of class names, which is most convenient for the caller.
 */
public class ValueClassDeps {
    private static String[] defaultFiles = {""};
    private boolean listValueClasses;
    private boolean listPreloadClasses;
    private boolean summary;

    public static void main(String[] args) {
        ValueClassDeps main = new ValueClassDeps();
        List<String> modules = main.extractOptions(List.of(args));
        if (modules.size() == 0)
            modules = List.of(defaultFiles);
        main.process(modules);
    }

    // Scan the `jrt:` file system for class files of the requested modules
    // Print the summary or requested list of classes
    private void process(List<String> modules) {
        for (String arg : modules) {
            try {
                List<ValueClassStats> matches = findAll(arg, ValueClassDeps::valueClassStats);
                long valueCount = matches.stream().mapToLong(v -> v.isValue ? 1 : 0).sum();
                long preloadCount = matches.stream().mapToLong(v -> v.hasPreload ? 1 : 0).sum();

                if (summary) {
                    System.out.printf("Value class count: %d%n", valueCount);
                    System.out.printf("Preload class count: %d%n", preloadCount);
                }

                matches.stream()
                        .filter(v -> (v.hasPreload() & listPreloadClasses) |
                                (v.isValue & listValueClasses))
                        .forEach(c -> System.out.printf("%s/%s%n", c.module, c.name));
            } catch (IOException | URISyntaxException ioe) {
                System.err.println("Exception " + ": " + arg + ioe.getClass().getName() + ": " + arg + ": " + ioe.getMessage());
            }
        }
    }

    // Extract any command line options and return the remaining args (modules)
    private List<String> extractOptions(List<String> args) {
        List<String> remaining = new ArrayList(args);
        ListIterator<String> iter = remaining.listIterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            switch (arg) {
                case "--preload-classes":
                    listPreloadClasses = true;
                    iter.remove();
                    break;
                case "--value-classes":
                    listValueClasses = true;
                    iter.remove();
                    break;
                case "--summary":
                    summary = true;
                    iter.remove();
                    break;
                default:
                    if (arg.startsWith("-")) { // unknown option
                        System.out.println("Usage: [--value-classes] [-preload] [--summary] <module name> ...");
                        throw new RuntimeException("incorrect command line syntax, arg: " + arg);
                    }
                    return remaining;       // no more options
            }
        }
        return remaining;
    }

    /**
     * Scans classes in the JDK and returns stats of classes using the evalFunction.
     *
     * @return list of matching class names
     * @throws IOException if an unexpected exception occurs
     * @throws URISyntaxException if an unexpected exception occurs
     */
    public static List<ValueClassStats> findAll(String root, Function<Path,Optional<ValueClassStats>> evalFunction) throws IOException, URISyntaxException {
        FileSystem fs = FileSystems.getFileSystem(new URI("jrt:/" ));
        Path dir = fs.getPath("/modules/" + root);
        try (final Stream<Path> paths = Files.walk(dir)) {
            // each path is in the form: /modules/<modname>/<pkg>/<pkg>/.../name.class
            return paths.filter((path) -> path.getNameCount() > 2)
                    .map(evalFunction)
                    .filter(r -> r.isPresent())
                    .map(s -> s.get())
                    .collect(Collectors.toList());
        }
    }

    // Parse out the module and class name from:
    // Paths of the form: /modules/<modname>/<pkg>/<pkg>/.../name.class
    private static final Pattern decodePattern = Pattern.compile("/modules/([^/]*)/(.*)\\.class$");

    // Read the path and extract the stats for value classes
    // Return an optional ValueClassStats
    static Optional<ValueClassStats> valueClassStats(Path path)  {
        String fullname = path.toString();
        if (!fullname.endsWith(".class") ||
                fullname.endsWith("module-info.class"))
            return Optional.empty();

        Matcher m = decodePattern.matcher(fullname);
        if (!m.find())
            throw new IllegalStateException("path does not match pattern: " + fullname);

        String module = m.group(1);
        String name = m.group(1);
        try {
            byte[] bytes = Files.readAllBytes(path);
            return valueClassStats(bytes, module);
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
            return Optional.empty();
        }
    }

    // Read the class using ASM to extract the stats
    static Optional<ValueClassStats> valueClassStats(byte[] bytes, String module) {
        ClassReader reader = new ClassReader(bytes);
        ValueClassVistor visitor = new ValueClassVistor(module);
        reader.accept(visitor, 0);
        return visitor.getStats();
    }

    // Records the module, name, and booleans for whether the class is a value class
    // or has a `Preload` attribute
    record ValueClassStats(String module, String name, boolean isValue, boolean hasPreload) {}

    // Asm class `visitor`
    static final class ValueClassVistor extends ClassVisitor {
        String module;
        String name;
        boolean isValueBased;
        boolean hasPreload;

        ValueClassVistor(String module) {
            super(ASM9, null);
            this.module = module;
            this.name = "";
            isValueBased = false;
            hasPreload = false;
        }

        // Report the accumulated stats for this class
        Optional<ValueClassStats> getStats() {
            return (isValueBased || hasPreload)
                    ? Optional.of(new ValueClassStats(module, name.replaceAll("/", "."), isValueBased, hasPreload))
                    : Optional.empty();
        }

        @Override
        public void visit(final int version,
                          final int access,
                          final String name,
                          final String signature,
                          final String superName,
                          final String[] interfaces) {
            if ((access & Opcodes.ACC_VALUE) != 0) {
                isValueBased = true;
            }
            this.name = name;
        }

        @Override
        public void visitAttribute(final Attribute attribute) {
            if (attribute.type.equals("Preload")) {
                hasPreload = true;
            }
        }
    }
}
