/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.nvdimm;

/*
 * @test TestOldObjectsOnNvdimm
 * @summary Check that objects in old generation reside in dram.
 * @requires vm.gc=="null" & os.family != "aix"
 * @requires test.vm.gc.nvdimm
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.nvdimm.TestOldObjectsOnNvdimm
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.util.ArrayList;
import java.util.Collections;

/*
 * Test spawns OldObjectTest in a separate VM and expects that it
 * completes without a RuntimeException.
 */
public class TestOldObjectsOnNvdimm {

    public static final int ALLOCATION_SIZE = 100;
    private static String[] commonFlags;

    public static void main(String args[]) throws Exception {
        commonFlags = new String[] {
            "-Xbootclasspath/a:.",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-XX:AllocateOldGenAt=" + System.getProperty("test.dir", "."),
            "-Xms10M", "-Xmx10M",
            "-XX:MaxTenuringThreshold=1" // Promote objects to Old Gen
        };
        runTest("-XX:+UseG1GC");
        runTest("-XX:+UseParallelGC");
    }

    private static void runTest(String... extraFlags) throws Exception {
        ArrayList<String> flags = new ArrayList<>();
        Collections.addAll(flags, commonFlags);
        Collections.addAll(flags, extraFlags);
        flags.add(OldObjectTest.class.getName());

        ProcessBuilder pb = ProcessTools.createTestJvm(flags);

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println(output.getStdout());
        output.shouldHaveExitValue(0);
    }
}

/*
 * This class tests that object is in Old generation after tenuring and resides in NVDIMM.
 * The necessary condition for this test is running in VM with the following flags:
 * -XX:AllocateOldGenAt=, -XX:MaxTenuringThreshold=1
 */
class OldObjectTest {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static void validateOldObject(Object o) {
        Asserts.assertTrue(WB.isObjectInOldGen(o),
                "Object is supposed to be in OldGen");

        long oldObj_addr = WB.getObjectAddress(o);
        long nvdimm_heap_start = WB.nvdimmReservedStart();
        long nvdimm_heap_end = WB.nvdimmReservedEnd();

        Asserts.assertTrue(oldObj_addr >= nvdimm_heap_start && oldObj_addr <= nvdimm_heap_end,
                "Old object does not reside in NVDIMM");
    }

    public static void main(String args[]) throws Exception {
        // allocate an object and perform Young GCs to promote it to Old
        byte[] oldObj = new byte[TestOldObjectsOnNvdimm.ALLOCATION_SIZE];
        WB.youngGC();
        WB.youngGC();
        validateOldObject(oldObj);
    }
}
