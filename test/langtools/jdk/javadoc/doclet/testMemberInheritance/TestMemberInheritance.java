/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4638588 4635809 6256068 6270645 8025633 8026567 8162363 8175200
 *      8192850 8182765 8220217 8224052 8237383
 * @summary Test to make sure that members are inherited properly in the Javadoc.
 *          Verify that inheritance labels are correct.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestMemberInheritance
 */

import javadoc.tester.JavadocTester;

public class TestMemberInheritance extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestMemberInheritance tester = new TestMemberInheritance();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg", "diamond", "inheritDist", "pkg1", "pkg2", "pkg3");
        checkExit(Exit.OK);

        checkOutput("pkg/SubClass.html", true,
                // Public field should be inherited
                "<a href=\"BaseClass.html#pubField\">",
                // Public method should be inherited
                "<a href=\"BaseClass.html#pubMethod()\">",
                // Public inner class should be inherited.
                "<a href=\"BaseClass.pubInnerClass.html\" title=\"class in pkg\">",
                // Protected field should be inherited
                "<a href=\"BaseClass.html#proField\">",
                // Protected method should be inherited
                "<a href=\"BaseClass.html#proMethod()\">",
                // Protected inner class should be inherited.
                "<a href=\"BaseClass.proInnerClass.html\" title=\"class in pkg\">",
                // New labels as of 1.5.0
                "Nested classes/interfaces inherited from class&nbsp;pkg."
                + "<a href=\"BaseClass.html\" title=\"class in pkg\">BaseClass</a>",
                "Nested classes/interfaces inherited from interface&nbsp;pkg."
                + "<a href=\"BaseInterface.html\" title=\"interface in pkg\">BaseInterface</a>");

        checkOutput("pkg/BaseClass.html", true,
                // Test overriding/implementing methods with generic parameters.
                "<dl class=\"notes\">\n"
                + "<dt>Specified by:</dt>\n"
                + "<dd><code><a href=\"BaseInterface.html#getAnnotation(java.lang.Class)\">"
                + "getAnnotation</a></code>&nbsp;in interface&nbsp;<code>"
                + "<a href=\"BaseInterface.html\" title=\"interface in pkg\">"
                + "BaseInterface</a></code></dd>\n"
                + "</dl>");

        checkOutput("diamond/Z.html", true,
                // Test diamond inheritance member summary (6256068)
                "<code><a href=\"A.html#aMethod()\">aMethod</a></code>");

        checkOutput("inheritDist/C.html", true,
                // Test that doc is inherited from closed parent (6270645)
                "<div class=\"block\">m1-B</div>");

        checkOutput("pkg/SubClass.html", false,
                "<a href=\"BaseClass.html#staticMethod()\">staticMethod</a></code>");

        checkOutput("pkg1/Implementer.html", true,
                // ensure the method makes it
                "<td class=\"col-first\"><code>static java.time.Period</code></td>\n"
                + "<th class=\"col-second\" scope=\"row\"><code><span class=\"member-name-link\">"
                + "<a href=\"#between(java.time.LocalDate,java.time.LocalDate)\">"
                + "between</a></span>&#8203;(java.time.LocalDate&nbsp;startDateInclusive,\n"
                + "java.time.LocalDate&nbsp;endDateExclusive)</code></th>");

        checkOutput("pkg1/Implementer.html", false,
                "<h3>Methods inherited from interface&nbsp;pkg1.<a href=\"Interface.html\""
                + " title=\"interface in pkg1\">Interface</a></h3>\n"
                + "<code><a href=\"Interface.html#between(java.time.chrono.ChronoLocalDate"
                + ",java.time.chrono.ChronoLocalDate)\">between</a></code>"
        );

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                "<section class=\"description\">\n<hr>\n"
                + "<pre>public abstract class <span class=\"type-name-label\">"
                + "DocumentedNonGenericChild</span>\n"
                + "extends java.lang.Object</pre>\n"
                + "</section>");

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                "<td class=\"col-first\"><code>protected abstract java.lang.String</code></td>\n"
                + "<th class=\"col-second\" scope=\"row\"><code><span class=\"member-name-link\">"
                + "<a href=\"#parentMethod(T)\">parentMethod</a></span>&#8203;(java.lang.String&nbsp;t)</code></th>\n"
                + "<td class=\"col-last\">\n"
                + "<div class=\"block\">Returns some value with an inherited search tag.</div>\n"
                + "</td>\n");

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                "<section class=\"detail\" id=\"parentMethod(T)\">\n"
                + "<h3 id=\"parentMethod(java.lang.Object)\">parentMethod</h3>\n"
                + "<div class=\"member-signature\"><span class=\"modifiers\">protected abstract</span>"
                + "&nbsp;<span class=\"return-type\">java.lang.String</span>&nbsp;"
                + "<span class=\"member-name\">parentMethod</span>&#8203;"
                + "(<span class=\"parameters\">java.lang.String&nbsp;t)</span>\n"
                + "                                          "
                + "throws <span class=\"exceptions\">java.lang.IllegalArgumentException,\n"
                + "java.lang.InterruptedException,\n"
                + "java.lang.IllegalStateException</span></div>\n"
                + "<div class=\"block\">Returns some value with an <span id=\"inheritedsearchtag\" "
                + "class=\"search-tag-result\">inherited search tag</span>.</div>");

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                "<dt>Throws:</dt>\n"
                + "<dd><code>java.lang.InterruptedException</code> - a generic error</dd>\n"
                + "<dd><code>java.lang.IllegalStateException</code> - illegal state</dd>\n"
                + "<dd><code>java.lang.IllegalArgumentException</code></dd>");

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                "<td class=\"col-first\"><code>java.lang.String</code></td>\n"
                + "<th class=\"col-second\" scope=\"row\"><code><span class=\"member-name-link\">"
                + "<a href=\"#parentField\">parentField</a></span></code></th>\n"
                + "<td class=\"col-last\">\n"
                + "<div class=\"block\">A field.</div>",
                "<section class=\"detail\" id=\"parentField\">\n"
                + "<h3>parentField</h3>\n"
                + "<div class=\"member-signature\"><span class=\"modifiers\">public</span>&nbsp;"
                + "<span class=\"return-type\">java.lang.String</span>&nbsp;<span class=\"member-name\">parentField</span></div>\n"
                + "<div class=\"block\">A field.</div>\n"
                + "</section>");

        checkOutput("pkg3/PrivateGenericParent.PublicChild.html", true,
                "<td class=\"col-first\"><code>java.lang.String</code></td>\n"
                + "<th class=\"col-second\" scope=\"row\"><code><span class=\"member-name-link\">"
                + "<a href=\"#method(T)\">method</a></span>&#8203;(java.lang.String&nbsp;t)</code></th>",
                "<section class=\"detail\" id=\"method(T)\">\n"
                + "<h3 id=\"method(java.lang.Object)\">method</h3>\n"
                + "<div class=\"member-signature\"><span class=\"modifiers\">public</span>"
                + "&nbsp;<span class=\"return-type\">java.lang.String</span>&nbsp;"
                + "<span class=\"member-name\">method</span>&#8203;(<span class=\"parameters\">"
                + "java.lang.String&nbsp;t)</span></div>\n"
                + "</section>");

        checkOutput("index-all.html", true,
                "<dt><span class=\"member-name-link\"><a href=\"pkg2/DocumentedNonGenericChild.html#parentField\">"
                + "parentField</a></span> - Variable in class pkg2.<a href=\"pkg2/DocumentedNonGenericChild.html\" "
                + "title=\"class in pkg2\">DocumentedNonGenericChild</a></dt>\n"
                + "<dd>\n<div class=\"block\">A field.</div>\n"
                + "</dd>\n",
                "<dt><span class=\"member-name-link\"><a href=\"pkg2/DocumentedNonGenericChild.html#parentMethod(T)\">"
                + "parentMethod(String)</a></span> - Method in class pkg2.<a "
                + "href=\"pkg2/DocumentedNonGenericChild.html\" title=\"class in pkg2\">DocumentedNonGenericChild</a></dt>\n"
                + "<dd>\n<div class=\"block\">Returns some value with an inherited search tag.</div>\n"
                + "</dd>");
        checkOutput("member-search-index.js", true,
                "{\"p\":\"pkg2\",\"c\":\"DocumentedNonGenericChild\",\"l\":\"parentField\"}",
                "{\"p\":\"pkg2\",\"c\":\"DocumentedNonGenericChild\",\"l\":\"parentMethod(String)"
                + "\",\"u\":\"parentMethod(T)\"}");
        checkOutput("tag-search-index.js", true,
                "{\"l\":\"inherited search tag\",\"h\":\"pkg2.UndocumentedGenericParent.parentMethod(String)\","
                + "\"u\":\"pkg2/DocumentedNonGenericChild.html#inheritedsearchtag\"}");

    }

    @Test
    public void testSplitIndex() {
        javadoc("-d", "out-split",
                "-splitindex",
                "-sourcepath", testSrc,
                "pkg", "diamond", "inheritDist", "pkg1", "pkg2", "pkg3");
        checkExit(Exit.OK);

        checkOutput("pkg2/DocumentedNonGenericChild.html", true,
                "<section class=\"detail\" id=\"parentMethod(T)\">\n"
                + "<h3 id=\"parentMethod(java.lang.Object)\">parentMethod</h3>\n"
                + "<div class=\"member-signature\"><span class=\"modifiers\">protected abstract</span>"
                + "&nbsp;<span class=\"return-type\">java.lang.String</span>&nbsp;"
                + "<span class=\"member-name\">parentMethod</span>&#8203;"
                + "(<span class=\"parameters\">java.lang.String&nbsp;t)</span>\n"
                + "                                          "
                + "throws <span class=\"exceptions\">java.lang.IllegalArgumentException,\n"
                + "java.lang.InterruptedException,\n"
                + "java.lang.IllegalStateException</span></div>\n"
                + "<div class=\"block\">Returns some value with an <span id=\"inheritedsearchtag\" "
                + "class=\"search-tag-result\">inherited search tag</span>.</div>");

        checkOutput("index-files/index-9.html", true,
                "<dt><span class=\"member-name-link\"><a href=\"../pkg2/DocumentedNonGenericChild.html#parentField\">"
                + "parentField</a></span> - Variable in class pkg2.<a href=\"../pkg2/DocumentedNonGenericChild.html\" "
                + "title=\"class in pkg2\">DocumentedNonGenericChild</a></dt>\n"
                + "<dd>\n<div class=\"block\">A field.</div>\n"
                + "</dd>\n",
                "<dt><span class=\"member-name-link\"><a href=\"../pkg2/DocumentedNonGenericChild.html#parentMethod(T)\">"
                + "parentMethod(String)</a></span> - Method in class pkg2.<a "
                + "href=\"../pkg2/DocumentedNonGenericChild.html\" title=\"class in pkg2\">DocumentedNonGenericChild</a></dt>\n"
                + "<dd>\n<div class=\"block\">Returns some value with an inherited search tag.</div>\n"
                + "</dd>");
        checkOutput("member-search-index.js", true,
                "{\"p\":\"pkg2\",\"c\":\"DocumentedNonGenericChild\",\"l\":\"parentField\"}",
                "{\"p\":\"pkg2\",\"c\":\"DocumentedNonGenericChild\",\"l\":\"parentMethod(String)"
                + "\",\"u\":\"parentMethod(T)\"}");
        checkOutput("tag-search-index.js", true,
                "{\"l\":\"inherited search tag\",\"h\":\"pkg2.UndocumentedGenericParent.parentMethod(String)\","
                + "\"u\":\"pkg2/DocumentedNonGenericChild.html#inheritedsearchtag\"}");
    }

}
