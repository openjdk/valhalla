/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8003562
 * @summary Basic tests for jdeps -dotoutput option
 * @modules java.management
 *          jdk.jdeps/com.sun.tools.jdeps
 * @library /tools/lib
 * @build toolbox.ToolBox Test p.Foo p.Bar
 * @run main DotFileTest
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import toolbox.ToolBox;

public class DotFileTest {
    public static void main(String... args) throws Exception {
        int errors = 0;
        errors += new DotFileTest().run();
        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    final ToolBox toolBox;
    final Path dir;
    final Path dotoutput;
    DotFileTest() {
        this.toolBox = new ToolBox();
        this.dir = Paths.get(System.getProperty("test.classes", "."));
        this.dotoutput = dir.resolve("dots");
    }

    int run() throws IOException {
        File testDir = dir.toFile();
        // test a .class file
        test(new File(testDir, "Test.class"),
             new String[] {"java.lang", "p"});
        // test a directory
        test(new File(testDir, "p"),
             new String[] {"java.lang", "java.util", "java.lang.management", "javax.crypto"},
             new String[] {"-classpath", testDir.getPath()});
        // test class-level dependency output
        test(new File(testDir, "Test.class"),
             new String[] {"java.lang.Object", "java.lang.String", "p.Foo", "p.Bar"},
             new String[] {"-verbose:class"});
        // test -filter:none option
        test(new File(testDir, "p"),
             new String[] {"java.lang", "java.util", "java.lang.management", "javax.crypto", "p"},
             new String[] {"-classpath", testDir.getPath(), "-verbose:package", "-filter:none"});
        // test -filter:archive option
        test(new File(testDir, "p"),
             new String[] {"java.lang", "java.util", "java.lang.management", "javax.crypto"},
             new String[] {"-classpath", testDir.getPath(), "-verbose:package", "-filter:archive"});
        // test -e option
        test(new File(testDir, "Test.class"),
             new String[] {"p.Foo", "p.Bar"},
             new String[] {"-verbose:class", "-e", "p\\..*"});
        test(new File(testDir, "Test.class"),
             new String[] {"java.lang"},
             new String[] {"-verbose:package", "-e", "java\\.lang\\..*"});
        // test -classpath options
        test(new File(testDir, "Test.class"),
             new String[] {"java.lang.Object", "java.lang.String", "p.Foo", "p.Bar"},
             new String[] {"-v", "-classpath", testDir.getPath()});

        testSummary(new File(testDir, "Test.class"),
             new String[] {"java.base", testDir.getName()},
             new String[] {"-classpath", testDir.getPath()});
        testSummary(new File(testDir, "Test.class"),
             new String[] {"java.lang", "p"},
             new String[] {"-v", "-classpath", testDir.getPath()});
        return errors;
    }

    void test(File file, String[] expect) throws IOException {
        test(file, expect, new String[0]);
    }

    void test(File file, String[] expect, String[] options)
        throws IOException
    {
        Path dotfile = dotoutput.resolve(file.toPath().getFileName().toString() + ".dot");

        List<String> args = new ArrayList<>(Arrays.asList(options));
        args.add("-dotoutput");
        args.add(dotoutput.toString());
        if (file != null) {
            args.add(file.getPath());
        }

        Map<String,String> result = jdeps(args, dotfile);
        checkResult("dependencies", expect, result.keySet());
    }

    void testSummary(File file, String[] expect, String[] options)
        throws IOException
    {
        Path dotfile = dotoutput.resolve("summary.dot");

        List<String> args = new ArrayList<>(Arrays.asList(options));
        args.add("-dotoutput");
        args.add(dotoutput.toString());
        if (file != null) {
            args.add(file.getPath());
        }

        Map<String,String> result = jdeps(args, dotfile);
        checkResult("dependencies", expect, result.keySet());
    }

    Map<String,String> jdeps(List<String> args, Path dotfile) throws IOException {
        if (Files.exists(dotoutput)) {
            // delete contents of directory, then directory,
            // waiting for confirmation on Windows
            toolBox.cleanDirectory(dotoutput);
            toolBox.deleteFiles(dotoutput);
        }
        // invoke jdeps
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        System.err.println("jdeps " + args.stream().collect(Collectors.joining(" ")));
        int rc = com.sun.tools.jdeps.Main.run(args.toArray(new String[0]), pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Error("jdeps failed: rc=" + rc);

        // check output files
        if (Files.notExists(dotfile)) {
            throw new RuntimeException(dotfile + " doesn't exist");
        }
        return parse(dotfile);
    }
    private static Pattern pattern = Pattern.compile("(.*) -> +([^ ]*) (.*)");
    private Map<String,String> parse(Path outfile) throws IOException {
        Map<String,String> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(outfile)) {
            line = line.replace('"', ' ').replace(';', ' ');
            Matcher pm = pattern.matcher(line);
            if (pm.find()) {
                String origin = pm.group(1).trim();
                String target = pm.group(2).trim();
                String module = pm.group(3).replace('(', ' ').replace(')', ' ').trim();
                result.put(target, module);
            }
        }
        return result;
    }

    void checkResult(String label, String[] expect, Collection<String> result) {
        // check the dependencies
        if (!isEqual(expect, result))
            error("Unexpected " + label + " found: '" + result +
                    "', expected: '" + Arrays.toString(expect) + "'");
    }

    boolean isEqual(String[] expected, Collection<String> found) {
        if (expected.length != found.size())
            return false;

        List<String> list = new ArrayList<>(found);
        list.removeAll(Arrays.asList(expected));
        return list.isEmpty();
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
