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
 *
 */

/*
 * @test
 * @summary Test archived flat arrays
 * @requires vm.cds.write.archived.java.heap
 * @library /test/jdk/lib/testlibrary /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @enablePreview
 * @modules java.base/jdk.internal.value
 * @compile ArchivedFlatArrayApp.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar archived_flat_array.jar ArchivedFlatArrayApp
 * @run main/othervm ArchivedFlatArrayTest
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ArchivedFlatArrayTest {

    public static void main(String[] args) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("archived_flat_array.jar");
        String mainClass = "ArchivedFlatArrayApp";

        OutputAnalyzer output = TestCommon.dump(appJar,
                                                TestCommon.list("ArchivedFlatArrayApp"),
                                                "--enable-preview",
                                                "-Xbootclasspath/a:" + appJar,
                                                "-XX:ArchiveHeapTestClass=ArchivedFlatArrayApp",
                                                "--add-exports",
                                                "java.base/jdk.internal.value=ALL-UNNAMED",
                                                "-Xlog:aot+heap");
        output.shouldHaveExitValue(0);
        output.shouldContain("Archived field ArchivedFlatArrayApp::archivedObjects");

        output = TestCommon.exec(appJar,
                                "--enable-preview",
                                "-Xbootclasspath/a:" + appJar,
                                "-XX:ArchiveHeapTestClass=ArchivedFlatArrayApp",
                                "--add-exports",
                                "java.base/jdk.internal.value=ALL-UNNAMED",
                                "-Xlog:aot+heap",
                                mainClass);
        output.shouldHaveExitValue(0);
        output.shouldContain("init subgraph ArchivedFlatArrayApp");
        output.shouldContain("Initialized from CDS");
    }
}
