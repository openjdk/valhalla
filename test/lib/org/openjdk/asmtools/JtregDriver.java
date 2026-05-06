/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.io.PrintWriter;
import java.io.IOException;
import org.openjdk.asmtools.jdis.uEscWriter;

/**
 * An entry point to AsmTools for use with the jtreg '@run driver' command.
 * Unlike 'Main', never invokes 'System.exit' (which crashes jtreg).
 *
 * Also adjusts file paths:
 *
 * - For jasm and jcoder, source files are expected to appear in ${test.src},
 *   and output is sent to ${test.classes}
 *
 * - For other tools, class files are expected to appear in ${test.classes},
 *   and output is sent to the scratch working directory
 *
 * Example jtreg usage:
 *
 * @library /test/lib
 * @build org.openjdk.asmtools.* org.openjdk.asmtools.jasm.*
 * @run driver org.openjdk.asmtools.JtregDriver jasm -strict TestFile.jasm
 */
public class JtregDriver {

    public static void main(String... args) throws Throwable {
        // run AltLoaderMain from a class loader that prefers the modified AsmTools classes
        ClassLoader loader = JtregDriver.class.getClassLoader();
        Path file = Path.of(loader.getResource("org/openjdk/asmtools/JtregDriver.class").toURI());
        Path root = file.getParent().getParent().getParent().getParent();
        ClassLoader altLoader = new AsmToolsClassLoader(root);
        Class<?> altMain = altLoader.loadClass(AltLoaderMain.class.getName());
        try {
            altMain.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public static class AltLoaderMain {

        public static void main(String... args) throws IOException {
            if (args.length == 0) {
                throw new IllegalArgumentException("Missing asmtools command");
            }
            String cmd = args[0];
            if (!cmd.equals("jasm") && !cmd.equals("jdis") && !cmd.equals("jcoder")
                    && !cmd.equals("jdec") && !cmd.equals("jcdec")) {
                throw new IllegalArgumentException("Unrecognized asmtools command: " + cmd);
            }
            boolean isAssembler = cmd.equals("jasm") || cmd.equals("jcoder");
            String srcDir = System.getProperty("test.src", ".");
            String clsDir = System.getProperty("test.classes", ".");
            String fileDir = isAssembler ? srcDir : clsDir;

            ArrayList<String> toolArgList = new ArrayList<String>();

            if (isAssembler) {
                Path destPath = Path.of(clsDir);
                if (!Files.exists(destPath)) {
                    // jtreg creates classes dir on demand, might not have happened yet
                    Files.createDirectories(destPath);
                }
                toolArgList.add("-d");
                toolArgList.add(clsDir);
            }

            boolean isOptionArg = false; // marks an argument to a previous option
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if (isOptionArg) {
                    isOptionArg = false; // reset for next
                } else {
                    if (arg.equals("-d")) {
                        isOptionArg = true;
                    } else if (!arg.startsWith("-") && !arg.startsWith("/")) {
                        // adjust filename
                        arg = Path.of(fileDir, arg).toString();
                    }
                }
                toolArgList.add(arg);
            }

            String[] toolArgs = toolArgList.toArray(new String[0]);
            boolean success = switch (cmd) {
                case "jasm" -> {
                    PrintWriter out = new PrintWriter(System.out);
                    yield new org.openjdk.asmtools.jasm.Main(out, "jasm").compile(toolArgs);
                }
                case "jdis" -> {
                    PrintWriter out = new PrintWriter(new uEscWriter(System.out));
                    PrintWriter err = new PrintWriter(System.err);
                    yield new org.openjdk.asmtools.jdis.Main(out, err, "jdis").disasm(toolArgs);
                }
                case "jcoder" -> {
                    PrintWriter out = new PrintWriter(System.out);
                    yield new org.openjdk.asmtools.jcoder.Main(out, "jcoder").compile(toolArgs);
                }
                case "jdec" -> {
                    PrintWriter out = new PrintWriter(new uEscWriter(System.out));
                    PrintWriter err = new PrintWriter(System.err);
                    yield new org.openjdk.asmtools.jdec.Main(out, err, "jdec").decode(toolArgs);
                }
                case "jcdec" -> {
                    PrintWriter out = new PrintWriter(new uEscWriter(System.out));
                    yield new org.openjdk.asmtools.jcdec.Main(out, "jcdec").decode(toolArgs);
                }
                default -> throw new AssertionError();
            };
            if (!success) {
                throw new RuntimeException("asmtools execution failed");
            }
        }

    }

    /**
     * Class loader for the AsmTools classes. Allows classes colocated with
     * JtregDriver to have priority over versions of the classes loaded by
     * jtreg (which includes its own copy of AsmTools).
     */
    private static class AsmToolsClassLoader extends ClassLoader {
        private final Path root;
        private final String separator;

        public AsmToolsClassLoader(Path root) {
            super(AsmToolsClassLoader.class.getClassLoader());
            this.root = root;
            this.separator = root.getFileSystem().getSeparator();
        }

        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("org.openjdk.asmtools.")) {
                Class<?> result = findClass(name);
                if (resolve) resolveClass(result);
                return result;
            } else {
                return super.loadClass(name, resolve);
            }
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String filename = name.replace(".",separator) + ".class";
            try {
                Path classFile = root.resolve(filename);
                byte[] bytes = Files.readAllBytes(classFile);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (InvalidPathException | IOException e) {
                throw new ClassNotFoundException("can't read class " + filename, e);
            }
        }

    }

}
