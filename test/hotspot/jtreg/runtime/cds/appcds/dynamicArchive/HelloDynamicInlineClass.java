/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Hello World test for dynamic archive
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build HelloInlineClassApp
 * @run driver ClassFileInstaller -jar hello_inline.jar HelloInlineClassApp HelloInlineClassApp$Point
 * @run driver HelloDynamicInlineClass
 */

public class HelloDynamicInlineClass extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(HelloDynamicInlineClass::testDefaultBase);
        runTest(HelloDynamicInlineClass::testCustomBase);
    }

    // (1) Test with default base archive + top archive
    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(null, topArchiveName);
    }

    // (2) Test with custom base archive + top archive
    static void testCustomBase() throws Exception {
        String topArchiveName = getNewArchiveName("top2");
        String baseArchiveName = getNewArchiveName("base");
        dumpBaseArchive(baseArchiveName);
        doTest(baseArchiveName, topArchiveName);
    }

    private static void doTest(String baseArchiveName, String topArchiveName) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("hello_inline.jar");
        String mainClass = "HelloInlineClassApp";
        dump2(baseArchiveName, topArchiveName,
             "-Xlog:cds",
             "-Xlog:cds+dynamic=debug",
             "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("Buffer-space to target-space delta")
                           .shouldContain("Written dynamic archive 0x");
                });
        run2(baseArchiveName, topArchiveName,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("HelloInlineClassApp$Point source: shared objects file")
                          .shouldHaveExitValue(0);
              });
    }
}
