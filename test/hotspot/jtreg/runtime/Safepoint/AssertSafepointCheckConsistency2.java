/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8047290
 * @summary Ensure that a Monitor::lock fires an assert when it incorrectly acquires a lock which must never have safepoint checks.
 * @requires vm.debug
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver AssertSafepointCheckConsistency2
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

import sun.hotspot.WhiteBox;

public class AssertSafepointCheckConsistency2 {
    public static void main(String args[]) throws Exception {
        if (args.length > 0) {
            WhiteBox.getWhiteBox().assertMatchingSafepointCalls(false, false);
            return;
        }
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
              "-Xbootclasspath/a:.",
              "-XX:+UnlockDiagnosticVMOptions",
              "-XX:+WhiteBoxAPI",
              "-XX:-CreateCoredumpOnCrash",
              "-Xmx128m",
              "AssertSafepointCheckConsistency2",
              "test");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("assert")
              .shouldContain("never")
              .shouldNotHaveExitValue(0);
    }
}
