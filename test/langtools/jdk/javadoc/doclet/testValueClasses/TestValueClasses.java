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
 * @bug      8308590
 * @summary  value classes
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    toolbox.ToolBox javadoc.tester.*
 * @run main TestValueClasses
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestValueClasses extends JavadocTester {

    public static void main(String... args) throws Exception {
        var tester = new TestValueClasses();
        tester.runTests();
    }

    private final ToolBox tb = new ToolBox();

    @Test
    public void testValueClassModifiers(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public value class ValueClass {}");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/ValueClass.html", true,
                """
                <div class="type-signature"><span class="modifiers">public value final class </span><span class="element-name type-name-label">ValueClass</span>
                """);
    }

    @Test
    public void testIdentityClassModifiers(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public identity class IdentityClass {}");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/IdentityClass.html", true,
                """
                <div class="type-signature"><span class="modifiers">public identity class </span><span class="element-name type-name-label">IdentityClass</span>
                """);
    }

    @Test
    public void testValueInterfaceModifiers(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public value interface ValueInterface {}");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/ValueInterface.html", true,
                """
                <div class="type-signature"><span class="modifiers">public value interface </span><span class="element-name type-name-label">ValueInterface</span></div>
                """);
    }

    @Test
    public void testIdentityInterfaceModifiers(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "package p; public identity interface IdentityInterface {}");

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/IdentityInterface.html", true,
                """
                <div class="type-signature"><span class="modifiers">public identity interface </span><span class="element-name type-name-label">IdentityInterface</span></div>
                """);
    }

    @Test
    public void testImplicitConstModifiers(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                package p;

                public value class ValueClassWithImplicitConst {
                    public implicit ValueClassWithImplicitConst();
                }
                """);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "-XDenableNullRestrictedTypes",
                "p");
        checkExit(Exit.OK);

        checkOutput("p/ValueClassWithImplicitConst.html", true,
                """
                <div class="member-signature"><span class="modifiers">public implicit</span>&nbsp;<span class="element-name">ValueClassWithImplicitConst</span>()</div>
                """);
    }
}
