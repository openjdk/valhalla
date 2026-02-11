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

/*
 * @test
 * @summary  Test null restricted types
 * @library  /tools/lib ../../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main NullRestrictions
 */

import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class NullRestrictions extends JavadocTester {
    private static final String JAVA_VERSION = System.getProperty("java.specification.version");

    public static void main(String... args) throws Exception {
        var tester = new NullRestrictions();
        tester.runTests();
    }

    private final ToolBox tb = new ToolBox();


    @Test
    public void testNullRestricted(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, """
            package p;
            import java.util.*;
            public class C<TV> {
                public List<String>! getList(Integer! idx) {
                    return null;
                }
                public String[]! getArray(TV! tidx) {
                    return null;
                }
                public <MethodTV> void run(MethodTV! t) {
                    return null;
                }
            }""");

        javadoc("-d", base.resolve("api").toString(),
                "--source-path", src.toString(),
                "--no-platform-links",
                "--enable-preview", "--release", JAVA_VERSION,
                "p");
        checkExit(Exit.OK);

        checkOutput("p/C.html", true,
                "<code>java.lang.String[]!</code>",
                "(java.lang.Integer!&nbsp;idx)",
                ">TV</a>!&nbsp;tidx)",
                "(MethodTV!&nbsp;t)",
                "<code>java.util.List<wbr>&lt;java.lang.String&gt;!</code>");
    }
}
