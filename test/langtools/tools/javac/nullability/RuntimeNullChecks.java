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

/*
 * @test
 * @summary test runtime null checks
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main RuntimeNullChecks
 * @ignore 8316628
 */

import java.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;
import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

public class RuntimeNullChecks extends TestRunner {
    ToolBox tb;

    RuntimeNullChecks() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    public static void main(String... args) throws Exception {
        RuntimeNullChecks t = new RuntimeNullChecks();
        t.runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testRuntimeChecks(Path base) throws Exception {
        for (String code: new String[] {
                """
                value class Point { public implicit Point(); }
                class Test {
                    public static void main(String... args) {
                        Point s = null;
                        Point! o = s; // NPE at runtime, variable initialization
                    }
                }
                """,
                """
                value class Point { public implicit Point(); }
                class Test {
                    public static void main(String... args) {
                        Point s = null;
                        Point! o;
                        o = s; // NPE at runtime, assignment, it doesn't stress the same code path as the case above
                    }
                }
                """,
                """
                value class Point { public implicit Point(); }
                class Test {
                    public static void main(String... args) {
                        Point s = null;
                        Point![] sr = new Point![10];
                        sr[0] = s; // NPE at runtime, assignment
                    }
                }
                """,
                """
                value class Point { public implicit Point(); }
                class Test {
                    static Point id(Point! arg) { return arg; }
                    public static void main(String... args) {
                        Point s = null;
                        Object o = id(s); // NPE at runtime, method invocation
                    }
                }
                """,
                """
                value class Point { public implicit Point(); }
                class Test {
                    public static void main(String... args) {
                        Point s = null;
                        Object o = (Point!) s; // NPE, cast
                    }
                }
                """
        }) {
            testHelper(base, code, true, NullPointerException.class);
        }
    }

    private void testHelper(Path base, String testCode, boolean shouldFail, Class<?> expectedError) throws Exception {
        Path src = base.resolve("src");
        Path testSrc = src.resolve("Test");

        tb.writeJavaFiles(testSrc, testCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(src))
                .run();

        if (shouldFail) {
            // let's check that we get the expected error
            String output = new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("Test")
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutput(Task.OutputKind.STDERR);
            if (!output.startsWith("Exception in thread \"main\" " + expectedError.getName())) {
                throw new AssertionError(expectedError.getName() + " expected");
            }
        } else {
            new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("Test")
                    .run(Task.Expect.SUCCESS);
        }
    }
}
