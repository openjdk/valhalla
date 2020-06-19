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
 * @summary Hello World test for using inline classes with CDS
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build HelloInlineClassApp
 * @run driver ClassFileInstaller -jar hello_inline.jar HelloInlineClassApp HelloInlineClassApp$Point HelloInlineClassApp$Point$ref HelloInlineClassApp$Rectangle HelloInlineClassApp$Rectangle$ref
 * @run driver HelloInlineClassTest
 */

import jdk.test.lib.process.OutputAnalyzer;

public class HelloInlineClassTest {
    public static void main(String[] args) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("hello_inline.jar");
        String mainClass = "HelloInlineClassApp";
        OutputAnalyzer output =
            TestCommon.dump(appJar, TestCommon.list(mainClass,
                                                    "HelloInlineClassApp$Point",
                                                    "HelloInlineClassApp$Rectangle"));
        output.shouldHaveExitValue(0);

        TestCommon.run("-Xint", "-cp", appJar,  mainClass)
            .assertNormalExit();

        TestCommon.run("-cp", appJar,  mainClass)
            .assertNormalExit();

        String compFlag = "-XX:CompileCommand=compileonly,HelloInlineClassApp*::*";

        TestCommon.run("-Xcomp", compFlag,
                       "-cp", appJar,  mainClass)
            .assertNormalExit();

        TestCommon.run("-Xcomp", compFlag,
                       "-XX:TieredStopAtLevel=1",
                       "-XX:+TieredCompilation",
                       "-XX:-Inline",
                       "-cp", appJar,  mainClass)
            .assertNormalExit();

        TestCommon.run("-Xcomp", compFlag,
                       "-XX:TieredStopAtLevel=4",
                       "-XX:-TieredCompilation",
                       "-XX:-Inline",
                       "-cp", appJar,  mainClass)
            .assertNormalExit();
    }
}
