/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test VTBufferTest
 * @summary Value Type interpreter value buffering test
 * @library /test/lib
 * @build ValueTypeGenerator
 * @run main/othervm -Xint -XX:+EnableValhalla VTBufferTest generate-and-run
 * @run main/othervm -Xint -XX:+EnableValhalla -XX:ValueTypesBufferMaxMemory=0 VTBufferTest generate-and-run
 * @run main/othervm -Xint -XX:+EnableValhalla -XX:BigValueTypeThreshold=196 VTBufferTest generate-and-run
 * @run main/othervm -Xint -XX:+EnableValhalla -XX:-ValueTypesThreadLocalRecycling VTBufferTest generate-and-run
 */

/* This test generates its source code.
 * To reproduce a run (for instance to investigate a failure), look at
 * the test output and search for a line starting with "Seed=". The value
 * at the end of the line is the seed used to generate the test.
 * It possible to re-generate the same test with the following commande
 * line:
 *  $ java <VMOptions> VTBufferTest generate-and-run -seed <seed>
 * where <seed> is the seed value from the test output.
 * The test source code is generated in the current directory with
 * names Value[0-9][0-9].java and Loop.java.
 * Once generated, the test can be run again without going through
 * the generation phase with the following commande line:
 *  $ java <VMOptions> VTBufferTest run
 */

import javax.management.*;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class VTBufferTest implements Runnable {
    static Random random;
    static boolean generate;
    static boolean execute;
    static long seed = 0;
    long startTime;
    boolean verbose = false;
    String[] valueNames;
    File[] valueSources;
    File[] loopSource;


    static void usage() {
        System.out.println("Usage:\n");
        System.out.println("\tVTBufferTest <command> [options]...\n");
        System.out.println("\nWhere <command> is one of the following: generate | generate-and-run | run\n");
        System.out.println("Where [options] can be: -seed <long value>\n");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            System.exit(-1);
        }

        if (args[0].compareTo("generate") == 0) {
            generate = true;
            execute = false;
        } else if (args[0].compareTo("generate-and-run") == 0) {
            generate = true;
            execute = true;
        } else if (args[0].compareTo("run") == 0) {
            generate = false;
            execute = true;
        } else {
            System.out.println("Unknown command\n");
            usage();
            System.exit(-1);
        }

        if (args.length > 1) {
            int cursor = 1;
            if (args[cursor].compareTo("-seed") == 0) {
                if (args.length < 3) {
                    usage();
                    System.exit(-1);
                }
                seed = Long.valueOf(args[cursor+1]);
                cursor++;
            } else {
                System.out.println("Unknown option\n");
                usage();
                System.exit(-1);
            }
        }

        if (generate) {
            if (seed == 0) {
                seed = System.nanoTime();
            }
            random = new Random(seed);
            System.out.println("Seed= " + seed);
        }

        VTBufferTest test = new VTBufferTest(true);
        test.run();
    }

    public VTBufferTest(boolean verbose) {
        this.verbose = verbose;
    }

    static private String[] generateValueNames() {
        int nvalues = random.nextInt(16) + 4;
        String[] names = new String[nvalues];
        for (int i = 0; i < nvalues; i++) {
            names[i] = new String("Value"+i);
        }
        return names;
    }

    static private File writeSource(String filename, String source) {
        try{
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println(source);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Writing source file failed");
        }
        return new File(filename);
    }

    static private File[] generateValueSources(String[] valueNames) {
        File[] sources = new File[valueNames.length];
        for (int i = 0; i < valueNames.length; i++) {
            int nfields = random.nextInt(6) + 1;
            String s = ValueTypeGenerator.generateValueType(random, valueNames[i], nfields);
            String filename = valueNames[i]+".java";
            sources[i] = writeSource(filename, s);
        }
        return sources;
    }

    static private File[] generateLoopSource(String[] names) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Seed = ").append(seed).append("\n");
        // class declaration
        sb.append("public final class Loop {\n");
        sb.append("\n");

        sb.append("\tstatic {\n");
        int i = 0;
        for (String name : names) {
            sb.append("\t\t").append(names[i]).append(" lv").append(i).append(" = ");
            sb.append(names[i]).append(".make").append(names[i]).append("();\n");
            sb.append("\t\tlv").append(i).append(".printLayout(System.out);\n");
            i++;
        }
        sb.append("\t}\n\n");

        // loop method
        sb.append("\tstatic public void loop(int iterations) { \n");
        i = 0;
        for (String name : names) {
            sb.append("\t\t").append(names[i]).append(" lv").append(i).append(" = ");
            sb.append(names[i]).append(".make").append(names[i]).append("();\n");
            i++;
        }
        sb.append("\t\tfor (int i = 0; i < iterations; i++) {\n");
        i = 0;
        for (String name : names) {
            sb.append("\t\t\tif (!").append(names[i]).append(".verify(lv").append(i).append("))\n");
            sb.append("\t\t\t\tthrow new RuntimeException(\"Error in ").append(names[i]).append("\");\n");
            i++;
        }
        i = 0;
        for (String name : names) {
            if (i != 0) {
                sb.append("\t\t\tif (i % ").append(i).append(" != 0) {\n");
                sb.append("\t\t\t\tlv").append(i).append(" = ");
                sb.append(names[i]).append(".make").append(names[i]).append("();\n");
                sb.append("\t\t\t}\n");
            }
            i++;
        }
        sb.append("\t\t}\n");
        sb.append("\t}\n");
        sb.append("}\n");

        String source = sb.toString();

        File[] files = new File[1];
        files[0] = writeSource("Loop.java", source);
        return files;
    }

    public void run() {
        if (generate) {
            valueNames = generateValueNames();
            valueSources = generateValueSources(valueNames);
            loopSource = generateLoopSource(valueNames);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            List<String> optionList = new ArrayList<String>();
            optionList.addAll(Arrays.asList("-classpath",".","-XDenableValueTypes"));

            Iterable<? extends JavaFileObject> compilationUnits1 =
                    fileManager.getJavaFileObjectsFromFiles(Arrays.asList(valueSources));
            compiler.getTask(null, fileManager, null, optionList, null, compilationUnits1).call();

            Iterable<? extends JavaFileObject> compilationUnits2 =
                    fileManager.getJavaFileObjectsFromFiles(Arrays.asList(loopSource));
            compiler.getTask(null, fileManager, null, optionList, null, compilationUnits2).call();
        }

        if (execute) {
            startTime = ManagementFactory.getRuntimeMXBean().getUptime();

            ClassLoader cl = createClassLoader();
            try {
                iterate(100, 5000, cl);
            } catch(InvocationTargetException e) {
                e.getCause().printStackTrace();
                System.exit(-1);
            }

            if (verbose) {
                printVTBufferStats();

                System.out.println("\nGC Statistics:");
                List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
                for (GarbageCollectorMXBean gc : gcs) {
                    System.out.println("Name=" + gc.getName());
                    System.out.println("GC counts=" + gc.getCollectionCount());
                    System.out.println("GC time=" + gc.getCollectionTime() + "ms");
                }

                System.out.println("\nHeap Statistics");
                List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();
                for (MemoryPoolMXBean memPool : memPools) {
                    if (memPool.getType() == MemoryType.HEAP) {
                        System.out.println("\nName: " + memPool.getName());
                        System.out.println("Usage: " + memPool.getUsage());
                        System.out.println("Collection Usage: " + memPool.getCollectionUsage());
                        System.out.println("Peak Usage: " + memPool.getPeakUsage());
                    }
                }
            }
        }
    }


    ClassLoader createClassLoader() {
        try{
            File file = new File(".");
            URL url = file.toURI().toURL();
            URL[] urls = new URL[]{url};
            ClassLoader cl = new URLClassLoader(urls);
            return cl;
        } catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public void iterate(int n, int m, ClassLoader cl) throws InvocationTargetException {
        for (int i = 0; i < n; i++) {
            Class loop = null;
            try {
                loop = Class.forName("Loop", true, cl);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Method method = null;
            try {
                method = loop.getMethod("loop", int.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return;
            }
            try {
                method.invoke(null, m);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            } catch (InvocationTargetException e) {
                throw e;
            }
        }
    }

    public void printVTBufferStats() {
        MBeanServerConnection mbs = ManagementFactory.getPlatformMBeanServer();
        String MBeanName = "com.sun.management:type=DiagnosticCommand";
        ObjectName beanName;
        try {
            beanName = new ObjectName(MBeanName);
        } catch (MalformedObjectNameException e) {
            String message = "MBean not found: " + MBeanName;
            throw new RuntimeException(message, e);
        }
        String result = null;
        try {
            result = (String)mbs.invoke(beanName,"vtbufferStats",new Object[0],new String[0]);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println(result);
    }
}
