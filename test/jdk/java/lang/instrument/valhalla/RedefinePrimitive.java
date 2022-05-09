/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.instrument
 * @run driver RedefinePrimitive master
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.RuntimeException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;
import jdk.test.lib.util.ClassTransformer;


primitive class MyPrimitive {

    long sum() {
        // This adds a number of new entries in the beginning of the CP
        // and this causes CP mapping during class redefinition.
        // @2 uncomment System.out.println(RedefinePrimitive.class);
        return x + z;
    }
    public MyPrimitive(int x, long z) {
        this.x = x;
        this.y = String.valueOf(x);
        this.z = z;
        // @1 uncomment z2 = z+1;
    }

    public static MyPrimitive create(int x, long z) {
        return new MyPrimitive(x,z);
    }

    int x;
    public String y;
    public long z;
    // @1 uncomment long z2;

    void method1(CountDownLatch ready, Object wait) {
        ready.countDown();
        try {
            synchronized (wait) {
                wait.wait();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}

primitive   // @1 commentout
class TestClass2 {
    public TestClass2(int x) {
        this.x = x;
    }
    int x;
}

// @1 uncomment primitive
class TestClass3 {
    public TestClass3(int x) {
        this.x = x;
    }
    int x;
}

class InlineHolder {
    MyPrimitive primField;
    InlineHolder() {
        primField = new MyPrimitive(8, 12);
    }
}

public class RedefinePrimitive {

    private static Instrumentation inst;

    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("ToolProvider for jar not found"));

    static class MyTransformer implements ClassFileTransformer {
        private Test test;
        Exception exceptionThrown;

        public MyTransformer(Test t) {
            test = t;
        }

        public byte[] transform(ClassLoader loader, String className,
                                Class classBeingRedefined, ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            byte[] result = null;
            if (className.equals(test.className)) {
                log(">>transform, class=" + className);
                try {
                    result = test.transform(className, classfileBuffer);
                } catch (Exception ex) {
                    exceptionThrown = ex;
                    log("Exception thrown by test.transform:");
                    ex.printStackTrace();
                }
                log("<<transform, class=" + className);
            }
            return result;
        }
    }

    public static void premain(String agentArgs, Instrumentation inst1) throws Exception {
        inst = inst1;
    }

    private static void buildAgent() {
        try {
            ClassFileInstaller.main("RedefinePrimitive");
        } catch (Exception e) {
            throw new RuntimeException("Could not write agent classfile", e);
        }

        try (PrintWriter pw = new PrintWriter("MANIFEST.MF")) {
            pw.println("Premain-Class: RedefinePrimitive");
            //pw.println("Agent-Class: RedefinePrimitive");
            //pw.println("Can-Redefine-Classes: true");
            pw.println("Can-Retransform-Classes: true");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write manifest file for the agent", e);
        }

        if (JAR.run(System.out, System.err, "-cmf", "MANIFEST.MF", "redefineagent.jar", "RedefinePrimitive.class") != 0) {
            throw new RuntimeException("Could not write the agent jar file");
        }
    }

    private static File getClassFile(String className) {
        return new File(System.getProperty("test.classes", "."), className + ".class");
    }

    private static byte[] loadClassBytes(File f) throws IOException {
        log("Reading test class from " + f);
        return Files.readAllBytes(f.toPath());
    }

    private static byte[] loadClassBytes(String className) throws IOException {
        return loadClassBytes(new File(System.getProperty("test.classes", "."), className + ".class"));
    }

    private static void compareClassBytes(String className, byte[] expected, byte[] actual) throws Exception {
        log("comparing class bytes for class " + className);
        if (Arrays.equals(expected, actual)) {
            log("identical");
        } else {
            log("class bytes mismatch");
            File redefinedFile = new File(className + "_redefined.class");
            try (FileOutputStream stream = new FileOutputStream(redefinedFile)) {
                stream.write(actual);
                log(".class saved to " + redefinedFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }

            compareClassFiles(getClassFile(className), redefinedFile);
        }
    }

    private static List<String> disassembleClassFile(File file) throws Exception {
        JDKToolLauncher javap = JDKToolLauncher.create("javap")
                .addToolArg("-c")
                .addToolArg("-s")
                .addToolArg("-verbose")
                .addToolArg(file.toString());
        ProcessBuilder pb = new ProcessBuilder(javap.getCommand());
        OutputAnalyzer out = ProcessTools.executeProcess(pb);
        return out.asLines();
    }

    private static final String[] expectedDifferentStrings = {
            "^Classfile .+$",
            "^[\\s]+SHA-256 checksum .[^\\s]+$"
    };

    private static boolean expectedDifferent(String line) {
        for (String s: expectedDifferentStrings) {
            Pattern p = Pattern.compile(s);
            if (p.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private static void compareClassFiles(File original, File redefined) throws Exception {
        log("Disassembly difference (" + original + " vs " + redefined+"):");
        // compare javap output for the class files
        List<String> out1 = disassembleClassFile(original);
        List<String> out2 = disassembleClassFile(redefined);

        boolean different = false;
        boolean orderChanged = false;
        int lineNum = 0;
        for (String line: out1) {
            if (!expectedDifferent(line)) {
                if (!out2.contains(line)) {
                    different = true;
                    log("< (" + (lineNum + 1) + ") " + line);
                } else {
                    if (lineNum < out2.size() && out1.get(lineNum) != out2.get(lineNum)) {
                        // out2 contains line, but at different position
                        orderChanged = true;
                    }
                }
            }
            lineNum++;
        }
        lineNum = 0;
        for (String line: out2) {
            if (!expectedDifferent(line)) {
                if (!out1.contains(line)) {
                    different = true;
                    log("> (" + (lineNum + 1) + ") " + line);
                }
            }
            lineNum++;
        }
        if (different || orderChanged) {
            log("original:");
            log("-------------------");
            for (String line: out1) {
                log(line);
            }
            log("===================");
            log("-------------------");
            log("redefined:");
            for (String line: out2) {
                log(line);
            }
            log("===================");
        }
        // accordingly the spec orderChanged is fine
        if (different) {
            throw new Exception("classfile bytes mismatch");
        } else if (orderChanged) {
            log("classfile bytes are different - only order changed");
        }
    }

    public static String getPropOpt(String prop) {
        String propVal = System.getProperty(prop);
        if (propVal == null) propVal = "";
        System.out.println(prop + ": '" + propVal  + "'");
        return "-D" + prop + "=" + propVal;
    }

    private static void log(Object msg) {
        System.out.println(String.valueOf(msg));
        System.out.flush();
    }

    public static void main(String argv[]) throws Exception {
        if (argv.length == 1 && argv[0].equals("master")) {
            buildAgent();

            for (int i = 0; i < tests.length; i++) {
                log("Starting " + tests[i].name + "...");
                ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                        "-javaagent:redefineagent.jar",
                        getPropOpt("test.jdk"),
                        getPropOpt("test.classes"),
                        getPropOpt("test.java.opts"),
                        getPropOpt("test.vm.opts"),
                        getPropOpt("test.src"),
                        "RedefinePrimitive", String.valueOf(i));
                OutputAnalyzer output = new OutputAnalyzer(pb.start());
                output.shouldNotContain("processing of -javaagent failed");
                output.shouldHaveExitValue(0);
                log("test stdout: [" + output.getStdout() + "]");
                log("test stderr: [" + output.getStderr() + "]");
                log("");
            }
        } else {
            int i = Integer.parseInt(argv[0]);
            Test t = tests[i];
            t.run();
        }
   }


    private static final String SOURCE_FILE = "RedefinePrimitive.java";
    private static class Test {
        String name;
        String className;
        boolean transformErrorExpected;

        public Test(String name, String className) {
            this.name = name;
            this.className = className;
            transformErrorExpected = false;
        }

        public void prologue() throws Exception {}
        public byte[] transform(String className, byte[] classBytes) throws Exception { return null; }
        public void epilogue() throws Exception {}

        public void run() throws Exception {
            log(">>main (" + name + ")");

            Class theClass = Class.forName(className);

            MyTransformer transformer = new MyTransformer(this);
            inst.addTransformer(transformer, true);

            prologue();

            try {
                log(">>retransformClasses (" + className + ")");
                inst.retransformClasses(theClass);
                log("<<retransformClasses (" + className + ")");

                if (transformErrorExpected) {
                    throw new Exception("retransformClasses is expected to fail, but no exception thrown");
                }
            } catch (Exception ex) {
                if (transformErrorExpected) {
                    log("retransformClasses is expected to fail, got exception:");
                    ex.printStackTrace(System.out);
                } else {
                    throw ex;
                }
            }
            if (transformer.exceptionThrown != null) {
                throw transformer.exceptionThrown;
            }

            inst.removeTransformer(transformer);

            epilogue();

            log("<<main (" + name + ")");
        }
    }

    private static Test[] tests = {
            new Test("Reconstituter sanity", "MyPrimitive") {
                byte[] savedClassBytes;
                @Override
                public void prologue() {
                }

                @Override
                public byte[] transform(String className, byte[] classBytes) {
                    savedClassBytes = classBytes;
                    return null;
                }

                @Override
                public void epilogue() throws Exception {
                    compareClassBytes(className, loadClassBytes(className), savedClassBytes);
                }
            },

            new Test("Class in in use", "MyPrimitive") {
                CountDownLatch ready;
                Object wait;

                @Override
                public void prologue() throws Exception {
                    MyPrimitive o1 = new MyPrimitive(2, 5);
                    CountDownLatch ready = new CountDownLatch(1);
                    wait = new Object();

                    Thread t = new Thread(
                            () -> {
                                log(">>o1.method1");
                                o1.method1(ready, wait);
                                log("<<o1.method1");
                            });
                    t.setDaemon(true);
                    t.start();
                    ready.await();
                }

                @Override
                public byte[] transform(String className, byte[] classBytes) {
                    return null;
                }

                @Override
                public void epilogue() throws Exception {
                    synchronized (wait) {
                        wait.notifyAll();
                    }
                }
            },

            new Test("Redefine primitive with instance", "TestClass2") {
                byte[] redefineClassBytes;
                @Override
                public void prologue() throws Exception {
                    transformErrorExpected = true;
                    String transformedClassFile = ClassTransformer.fromTestSource(SOURCE_FILE)
                            .transform(1, className);
                    redefineClassBytes = loadClassBytes(new File(transformedClassFile));
                }

                @Override
                public byte[] transform(String className, byte[] classBytes) {
                    return redefineClassBytes;
                }

                @Override
                public void epilogue() throws Exception {
                }
            },

            new Test("Redefine instance with primitive", "TestClass3") {
                byte[] redefineClassBytes;
                @Override
                public void prologue() throws Exception {
                    transformErrorExpected = true;
                    String transformedClassFile = ClassTransformer.fromTestSource(SOURCE_FILE)
                            .transform(1, className);
                    redefineClassBytes = loadClassBytes(new File(transformedClassFile));
                }

                @Override
                public byte[] transform(String className, byte[] classBytes) {
                    return redefineClassBytes;
                }

                @Override
                public void epilogue() throws Exception {
                }
            },

            new Test("Change object size", "MyPrimitive") {
                byte[] redefineClassBytes;
                @Override
                public void prologue() throws Exception {
                    transformErrorExpected = true;
                    String transformedClassFile = ClassTransformer.fromTestSource(SOURCE_FILE)
                            .transform(1, className);
                    redefineClassBytes = loadClassBytes(new File(transformedClassFile));
                }

                @Override
                public byte[] transform(String className, byte[] classBytes) {
                    return redefineClassBytes;
                }

                @Override
                public void epilogue() throws Exception {
                }
            },

            new Test("Reconstitute class with inlined field", "InlineHolder") {
                byte[] savedClassBytes;
                @Override
                public void prologue() {
                }

                @Override
                public byte[] transform(String className, byte[] classBytes) {
                    savedClassBytes = classBytes;
                    return null;
                }

                @Override
                public void epilogue() throws Exception {
                    compareClassBytes(className, loadClassBytes(className), savedClassBytes);
                }
            },

            new Test("CP mapping", "MyPrimitive") {
                byte[] redefineClassBytes;

                @Override
                public void prologue() throws Exception {
                    String transformedClassFile = ClassTransformer.fromTestSource(SOURCE_FILE)
                            .transform(2, className);
                    redefineClassBytes = loadClassBytes(new File(transformedClassFile));
                }

                @Override
                public byte[] transform(String className, byte[] classBytes) {
                    // with wrong mapping retransformClasses() fails with InternalError
                    return redefineClassBytes;
                }

                @Override
                public void epilogue() throws Exception {
                }
            },
    };

}