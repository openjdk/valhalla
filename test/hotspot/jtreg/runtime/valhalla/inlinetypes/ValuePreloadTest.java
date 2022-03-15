/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @test ValuePreloadTest
 * @library /test/lib
 * @compile ValuePreloadClient0.java PreloadValue0.java ValuePreloadClient1.jcod
 * @run driver ValuePreloadTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValuePreloadTest {

    static ProcessBuilder exec(String... args) throws Exception {
        List<String> argsList = new ArrayList<>();
        Collections.addAll(argsList, "-Dtest.class.path=" + System.getProperty("test.class.path", "."));
        Collections.addAll(argsList, args);
        return ProcessTools.createJavaProcessBuilder(argsList);
    }

    static void checkFor(ProcessBuilder pb, String... outputStrings) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        for (String s: outputStrings) {
            out.shouldContain(s);
        }
        out.shouldHaveExitValue(0);
    }

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = exec("-Xlog:class+preload","ValuePreloadClient0");
        checkFor(pb, "[info][class,preload] Preloading class PreloadValue0 during linking of class ValuePreloadClient0 because of its Preload attribute");

        pb = exec("-Xlog:class+preload","ValuePreloadClient1");
        checkFor(pb, "[warning][class,preload] Preloading of class PreloadValue1 during linking of class ValuePreloadClient1 (Preload attribute) failed");
    }
}
