/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8187679
 * @summary The VM should exit gracefully when unable to preload an inline type argument
 * @library /test/lib
 * @enablePreview
 * @compile SimpleInlineType.java TestUnresolvedInlineClass.java
 * @run main/othervm TestUnresolvedInlineClass
 */

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestUnresolvedInlineClass {
    final static String TEST_CLASSES = System.getProperty("test.classes") + File.separator;

    // Method with unresolved inline type argument
    static void test1(SimpleInlineType vt) {

    }

    static public void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Delete SimpleInlineType.class
            File unresolved = new File(TEST_CLASSES, "SimpleInlineType.class");
            if (!unresolved.exists() || !unresolved.delete()) {
                throw new RuntimeException("Could not delete: " + unresolved);
            }

            // Run test in new VM instance
            String[] arg = {"--enable-preview", "-XX:+InlineTypePassFieldsAsArgs", "TestUnresolvedInlineClass", "run"};
            OutputAnalyzer oa = ProcessTools.executeTestJvm(arg);

            // Verify that a warning is printed
            String output = oa.getOutput();
            oa.shouldContain("Preloading of class SimpleInlineType during linking of class TestUnresolvedInlineClass (cause: Preload attribute) failed");
        }
    }
}
