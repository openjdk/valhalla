/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8274800
 * @library /test/lib
 * @summary [lworld] Primitive classes can't be retransformed
 * @modules java.base/jdk.internal.misc
 * @modules java.instrument
 * @requires vm.jvmti
 * @requires vm.flagless
 * @run driver RedefinePrimitive buildagent
 * @run driver/timeout=6000 RedefinePrimitive runtest
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.RuntimeException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.lang.instrument.IllegalClassFormatException;
import java.util.spi.ToolProvider;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class RedefinePrimitive {

    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("ToolProvider for jar not found"));

    static primitive class PrimitiveTester {
        public PrimitiveTester(int x, String y, long z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public int x;
        public String y;
        public long z;
    }

    static class LoggingTransformer implements ClassFileTransformer {

        public LoggingTransformer() {}

        static int fileNumber=0;

        public byte[] transform(ClassLoader loader, String className,
                                Class classBeingRedefined, ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            return null;
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        LoggingTransformer t = new LoggingTransformer();
        inst.addTransformer(t, true);
        {
            Class demoClass = Class.forName("RedefinePrimitive$PrimitiveTester");
            inst.retransformClasses(demoClass);
        }
    }

    private static void buildAgent() {
        try {
            System.err.println("*** Calling ClassFileInstaller.main(\"RedefinePrimitive\");");
            ClassFileInstaller.main("RedefinePrimitive");
        } catch (Exception e) {
            throw new RuntimeException("Could not write agent classfile", e);
        }

        try {
            PrintWriter pw = new PrintWriter("MANIFEST.MF");
            pw.println("Premain-Class: RedefinePrimitive");
            pw.println("Agent-Class: RedefinePrimitive");
            pw.println("Can-Redefine-Classes: true");
            pw.println("Can-Retransform-Classes: true");
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write manifest file for the agent", e);
        }

        if (JAR.run(System.out, System.err, "-cmf", "MANIFEST.MF", "redefineagent.jar", "RedefinePrimitive.class") != 0) {
            throw new RuntimeException("Could not write the agent jar file");
        }
    }

    public static void main(String argv[]) throws Exception {
        if (argv.length == 1 && argv[0].equals("buildagent")) {
            buildAgent();
            return;
        }
        if (argv.length == 1 && argv[0].equals("runtest")) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:MetaspaceSize=12m",
                "-XX:MaxMetaspaceSize=12m",
                "-javaagent:redefineagent.jar",
                "RedefinePrimitive");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldNotContain("processing of -javaagent failed");
            output.shouldHaveExitValue(0);
        }
    }
}
