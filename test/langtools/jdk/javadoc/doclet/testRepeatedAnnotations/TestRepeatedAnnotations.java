/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8005092 6469562 8182765
 * @summary  Test repeated annotations output.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestRepeatedAnnotations
 */

import javadoc.tester.JavadocTester;

public class TestRepeatedAnnotations extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestRepeatedAnnotations tester = new TestRepeatedAnnotations();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg", "pkg1");
        checkExit(Exit.OK);

        checkOutput("pkg/C.html", true,
                """
                    <a href="ContaineeSynthDoc.html" title="annotation in pkg">@ContaineeSynthDoc</a\
                    > <a href="ContaineeSynthDoc.html" title="annotation in pkg">@ContaineeSynthDoc<\
                    /a>""",
                """
                    <a href="ContaineeRegDoc.html" title="annotation in pkg">@ContaineeRegDoc</a> <a\
                     href="ContaineeRegDoc.html" title="annotation in pkg">@ContaineeRegDoc</a>""",
                """
                    <a href="RegContainerDoc.html" title="annotation in pkg">@RegContainerDoc</a>({<\
                    a href="RegContaineeNotDoc.html" title="annotation in pkg">@RegContaineeNotDoc</\
                    a>,<a href="RegContaineeNotDoc.html" title="annotation in pkg">@RegContaineeNotD\
                    oc</a>})""");

        checkOutput("pkg/D.html", true,
                """
                    <a href="RegDoc.html" title="annotation in pkg">@RegDoc</a>(<a href="RegDoc.html#x()">x</a>=1)""",
                """
                    <a href="RegArryDoc.html" title="annotation in pkg">@RegArryDoc</a>(<a href="RegArryDoc.html#y()">y</a>=1)""",
                """
                    <a href="RegArryDoc.html" title="annotation in pkg">@RegArryDoc</a>(<a href="RegArryDoc.html#y()">y</a>={1,2})""",
                """
                    <a href="NonSynthDocContainer.html" title="annotation in pkg">@NonSynthDocContai\
                    ner</a>(<a href="RegArryDoc.html" title="annotation in pkg">@RegArryDoc</a>(<a h\
                    ref="RegArryDoc.html#y()">y</a>=1))""");

        checkOutput("pkg1/C.html", true,
                """
                    <a href="RegContainerValDoc.html" title="annotation in pkg1">@RegContainerValDoc\
                    </a>(<a href="RegContainerValDoc.html#value()">value</a>={<a href="RegContaineeN\
                    otDoc.html" title="annotation in pkg1">@RegContaineeNotDoc</a>,<a href="RegConta\
                    ineeNotDoc.html" title="annotation in pkg1">@RegContaineeNotDoc</a>},<a href="Re\
                    gContainerValDoc.html#y()">y</a>=3)""",
                """
                    <a href="ContainerValDoc.html" title="annotation in pkg1">@ContainerValDoc</a>(<\
                    a href="ContainerValDoc.html#value()">value</a>={<a href="ContaineeNotDoc.html" \
                    title="annotation in pkg1">@ContaineeNotDoc</a>,<a href="ContaineeNotDoc.html" t\
                    itle="annotation in pkg1">@ContaineeNotDoc</a>},<a href="ContainerValDoc.html#x(\
                    )">x</a>=1)""");

        checkOutput("pkg/C.html", false,
                """
                    <a href="RegContaineeDoc.html" title="annotation in pkg">@RegContaineeDoc</a> <a\
                     href="RegContaineeDoc.html" title="annotation in pkg">@RegContaineeDoc</a>""",
                """
                    <a href="RegContainerNotDoc.html" title="annotation in pkg">@RegContainerNotDoc<\
                    /a>(<a href="RegContainerNotDoc.html#value()">value</a>={<a href="RegContaineeNo\
                    tDoc.html" title="annotation in pkg">@RegContaineeNotDoc</a>,<a href="RegContain\
                    eeNotDoc.html" title="annotation in pkg">@RegContaineeNotDoc</a>})""");

        checkOutput("pkg1/C.html", false,
                """
                    <a href="ContaineeSynthDoc.html" title="annotation in pkg1">@ContaineeSynthDoc</\
                    a> <a href="ContaineeSynthDoc.html" title="annotation in pkg1">@ContaineeSynthDo\
                    c</a>""",
                """
                    <a href="RegContainerValNotDoc.html" title="annotation in pkg1">@RegContainerVal\
                    NotDoc</a>(<a href="RegContainerValNotDoc.html#value()">value</a>={<a href="RegC\
                    ontaineeDoc.html" title="annotation in pkg1">@RegContaineeDoc</a>,<a href="RegCo\
                    ntaineeDoc.html" title="annotation in pkg1">@RegContaineeDoc</a>},<a href="RegCo\
                    ntainerValNotDoc.html#y()">y</a>=4)""",
                """
                    <a href="ContainerValNotDoc.html" title="annotation in pkg1">@ContainerValNotDoc\
                    </a>(<a href="ContainerValNotDoc.html#value()">value</a>={<a href="ContaineeNotD\
                    oc.html" title="annotation in pkg1">@ContaineeNotDoc</a>,<a href="ContaineeNotDo\
                    c.html" title="annotation in pkg1">@ContaineeNotDoc</a>},<a href="ContainerValNo\
                    tDoc.html#x()">x</a>=2)""",
                """
                    <a href="ContainerSynthNotDoc.html" title="annotation in pkg1">@ContainerSynthNo\
                    tDoc</a>(<a href="ContainerSynthNotDoc.html#value()">value</a>=<a href="Containe\
                    eSynthDoc.html" title="annotation in pkg1">@ContaineeSynthDoc</a>)""");
    }
}
