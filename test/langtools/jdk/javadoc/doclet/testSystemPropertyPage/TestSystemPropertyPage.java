/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215038 8239487 8240476
 * @summary Add a page that lists all system properties
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox
 * @run main TestSystemPropertyPage
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestSystemPropertyPage extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestSystemPropertyPage tester = new TestSystemPropertyPage();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestSystemPropertyPage() {
        tb = new ToolBox();
    }

    @Test
    public void test(Path base) throws Exception {
        Path outDir = base.resolve("out1");
        Path srcDir = Path.of(testSrc).resolve("src1");
        javadoc("-d", outDir.toString(),
                "-overview", srcDir.resolve("overview.html").toString(),
                "--source-path", srcDir.toString(),
                "pkg1", "pkg2");

        checkExit(Exit.OK);

        checkOutput("index-all.html", true,
                "<a href=\"system-properties.html\">System Properties</a>");

        checkOutput("system-properties.html", true,
                "<div class=\"flex-box\">\n" +
                "<header role=\"banner\" class=\"flex-header\">",

                "<div class=\"flex-content\">\n" +
                "<main role=\"main\">\n" +
                "<div class=\"header\">\n" +
                "<h1>System Properties</h1>\n" +
                "</div>",

                "<table class=\"summary-table\">\n" +
                "<caption><span>System Properties Summary</span></caption>\n" +
                "<thead>\n" +
                "<tr>\n" +
                "<th class=\"col-first\" scope=\"col\">Property</th>\n" +
                "<th class=\"col-last\" scope=\"col\">Referenced In</th>\n" +
                "</tr>\n" +
                "</thead>\n" +
                "<tbody>\n" +
                "<tr class=\"alt-color\">\n" +
                "<th class=\"col-first\" scope=\"row\">user.address</th>\n" +
                "<td class=\"col-last\">\n" +
                "<div class=\"block\">" +
                        "<code><a href=\"pkg2/B.html#user.address\">class pkg2.B</a></code>" +
                        ", <a href=\"pkg1/doc-files/WithTitle.html#user.address\"><code>package pkg1: </code>Example Title</a></div>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr class=\"row-color\">\n" +
                "<th class=\"col-first\" scope=\"row\">user.name</th>\n" +
                "<td class=\"col-last\">\n" +
                "<div class=\"block\">" +
                        "<a href=\"index.html#user.name\">Overview</a>" +
                        ", <code><a href=\"pkg1/A.html#user.name\">class pkg1.A</a></code>" +
                        ", <a href=\"pkg1/doc-files/WithEmptyTitle.html#user.name\"><code>package pkg1: </code>WithEmptyTitle.html</a>" +
                        ", <a href=\"pkg1/doc-files/WithTitle.html#user.name\"><code>package pkg1: </code>Example Title</a>" +
                        ", <a href=\"pkg1/doc-files/WithoutTitle.html#user.name\"><code>package pkg1: </code>WithoutTitle.html</a></div>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</tbody>\n" +
                "</table>");
    }

    /*
     * If there are no system properties, then there has to be
     * no System Properties page and no mentions thereof.
     */
    @Test
    public void testNoProperties(Path base) throws Exception {
        Path outDir = base.resolve("out2");
        Path srcDir = Path.of(testSrc).resolve("src2");
        javadoc("-d", outDir.toString(),
                "--source-path", srcDir.toString(),
                "pkg1");

        checkExit(Exit.OK);
        checkFiles(false, "system-properties.html");

        // Should be conditional on presence of the index file(s)
        checkOutput("index-all.html", false, "<a href=\"system-properties.html\">");
        checkOutput("index-all.html", false, "System Properties");
    }
}
