/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.valuetypes;

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Asserts;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Repeatable;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap;

// Mark method as test
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Tests.class)
@interface Test {
    // Regular expression used to match forbidden IR nodes
    // in the C2 IR emitted for this test.
    String failOn() default "";
    // Regular expressions used to match and count IR nodes.
    String[] match() default { };
    int[] matchCount() default { };
    int valid() default ValueTypeTest.AllFlags;
}

@Retention(RetentionPolicy.RUNTIME)
@interface Tests {
    Test[] value();
}

// Force method inlining during compilation
@Retention(RetentionPolicy.RUNTIME)
@interface ForceInline { }

// Prevent method inlining during compilation
@Retention(RetentionPolicy.RUNTIME)
@interface DontInline { }

// Prevent method compilation
@Retention(RetentionPolicy.RUNTIME)
@interface DontCompile { }

// Number of warmup iterations
@Retention(RetentionPolicy.RUNTIME)
@interface Warmup {
    int value();
}

public abstract class ValueTypeTest {
    // Run "jtreg -Dtest.c1=true" to enable experimental C1 testing.
    static final boolean TEST_C1 = Boolean.getBoolean("test.c1");

    // Random test values
    public static final int  rI = Utils.getRandomInstance().nextInt() % 1000;
    public static final long rL = Utils.getRandomInstance().nextLong() % 1000;

    // User defined settings
    protected static final boolean XCOMP = Platform.isComp();
    private static final boolean PRINT_GRAPH = true;
    private static final boolean PRINT_TIMES = Boolean.parseBoolean(System.getProperty("PrintTimes", "false"));
    private static       boolean VERIFY_IR = Boolean.parseBoolean(System.getProperty("VerifyIR", "true")) && !TEST_C1 && !XCOMP;
    private static final boolean VERIFY_VM = Boolean.parseBoolean(System.getProperty("VerifyVM", "false"));
    private static final String SCENARIOS = System.getProperty("Scenarios", "");
    private static final String TESTLIST = System.getProperty("Testlist", "");
    private static final String EXCLUDELIST = System.getProperty("Exclude", "");
    private static final int WARMUP = Integer.parseInt(System.getProperty("Warmup", "251"));
    private static final boolean DUMP_REPLAY = Boolean.parseBoolean(System.getProperty("DumpReplay", "false"));

    // Pre-defined settings
    private static final List<String> defaultFlags = Arrays.asList(
        "-XX:-BackgroundCompilation", "-XX:CICompilerCount=1",
        "-XX:CompileCommand=quiet",
        "-XX:CompileCommand=compileonly,java.lang.invoke.*::*",
        "-XX:CompileCommand=compileonly,java.lang.Long::sum",
        "-XX:CompileCommand=compileonly,java.lang.Object::<init>",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.*::*");
    private static final List<String> printFlags = Arrays.asList(
        "-XX:+PrintCompilation", "-XX:+PrintIdeal", "-XX:+PrintOptoAssembly");
    private static final List<String> verifyFlags = Arrays.asList(
        "-XX:+VerifyOops", "-XX:+VerifyStack", "-XX:+VerifyLastFrame", "-XX:+VerifyBeforeGC", "-XX:+VerifyAfterGC",
        "-XX:+VerifyDuringGC", "-XX:+VerifyAdapterSharing", "-XX:+StressValueTypeReturnedAsFields");

    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    protected static final int ValueTypePassFieldsAsArgsOn = 0x1;
    protected static final int ValueTypePassFieldsAsArgsOff = 0x2;
    protected static final int ValueTypeArrayFlattenOn = 0x4;
    protected static final int ValueTypeArrayFlattenOff = 0x8;
    protected static final int ValueTypeReturnedAsFieldsOn = 0x10;
    protected static final int ValueTypeReturnedAsFieldsOff = 0x20;
    protected static final int AlwaysIncrementalInlineOn = 0x40;
    protected static final int AlwaysIncrementalInlineOff = 0x80;
    static final int AllFlags = ValueTypePassFieldsAsArgsOn | ValueTypePassFieldsAsArgsOff | ValueTypeArrayFlattenOn | ValueTypeArrayFlattenOff | ValueTypeReturnedAsFieldsOn;
    protected static final boolean ValueTypePassFieldsAsArgs = (Boolean)WHITE_BOX.getVMFlag("ValueTypePassFieldsAsArgs");
    protected static final boolean ValueTypeArrayFlatten = (Boolean)WHITE_BOX.getVMFlag("ValueArrayFlatten");
    protected static final boolean ValueTypeReturnedAsFields = (Boolean)WHITE_BOX.getVMFlag("ValueTypeReturnedAsFields");
    protected static final boolean AlwaysIncrementalInline = (Boolean)WHITE_BOX.getVMFlag("AlwaysIncrementalInline");
    protected static final int COMP_LEVEL_ANY = -2;
    protected static final int COMP_LEVEL_FULL_OPTIMIZATION = TEST_C1 ? 1 : 4;
    protected static final Hashtable<String, Method> tests = new Hashtable<String, Method>();
    protected static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    protected static final boolean PRINT_IDEAL  = WHITE_BOX.getBooleanVMFlag("PrintIdeal");

    // Regular expressions used to match nodes in the PrintIdeal output
    protected static final String START = "(\\d+\\t(.*";
    protected static final String MID = ".*)+\\t===.*";
    protected static final String END = ")|";
    protected static final String ALLOC  = "(.*precise klass compiler/valhalla/valuetypes/MyValue.*\\R(.*(nop|spill).*\\R)*.*_new_instance_Java" + END;
    protected static final String ALLOCA = "(.*precise klass \\[Lcompiler/valhalla/valuetypes/MyValue.*\\R(.*(nop|spill).*\\R)*.*_new_array_Java" + END;
    protected static final String LOAD   = START + "Load(B|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/valuetypes/MyValue.*" + END;
    protected static final String LOADK  = START + "LoadK" + MID + END;
    protected static final String STORE  = START + "Store(B|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/valuetypes/MyValue.*" + END;
    protected static final String LOOP   = START + "Loop" + MID + "" + END;
    protected static final String TRAP   = START + "CallStaticJava" + MID + "uncommon_trap.*(unstable_if|predicate)" + END;
    protected static final String RETURN = START + "Return" + MID + "returns" + END;
    protected static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
    protected static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
    protected static final String CALL = START + "CallStaticJava" + MID + END;
    protected static final String STOREVALUETYPEFIELDS = START + "CallStaticJava" + MID + "store_value_type_fields" + END;
    protected static final String SCOBJ = "(.*# ScObj.*" + END;

    public static String[] concat(String prefix[], String... extra) {
        ArrayList<String> list = new ArrayList<String>();
        if (prefix != null) {
            for (String s : prefix) {
                list.add(s);
            }
        }
        if (extra != null) {
            for (String s : extra) {
                list.add(s);
            }
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Override getNumScenarios and getVMParameters if you want to run with more than
     * the 5 built-in scenarios
     */
    public int getNumScenarios() {
        if (TEST_C1) {
            return 1;
        } else {
            return 5;
        }
    }

    /**
     * VM paramaters for the 5 built-in test scenarios. If your test needs to append
     * extra parameters for (some of) these scenarios, override getExtraVMParameters().
     */
    public String[] getVMParameters(int scenario) {
        if (TEST_C1) {
            return new String[] {
                    "-XX:+EnableValhallaC1",
            };
        }

        switch (scenario) {
        case 0: return new String[] {
                "-XX:+AlwaysIncrementalInline",
                "-XX:ValueArrayElemMaxFlatOops=-1",
                "-XX:ValueArrayElemMaxFlatSize=-1",
                "-XX:+ValueArrayFlatten",
                "-XX:ValueFieldMaxFlatSize=-1",
                "-XX:+ValueTypePassFieldsAsArgs",
                "-XX:+ValueTypeReturnedAsFields"};
        case 1: return new String[] {
                "-XX:-UseCompressedOops",
                "-XX:ValueArrayElemMaxFlatOops=-1",
                "-XX:ValueArrayElemMaxFlatSize=-1",
                "-XX:+ValueArrayFlatten",
                "-XX:ValueFieldMaxFlatSize=-1",
                "-XX:-ValueTypePassFieldsAsArgs",
                "-XX:-ValueTypeReturnedAsFields"};
        case 2: return new String[] {
                "-DVerifyIR=false",
                "-XX:-UseCompressedOops",
                "-XX:ValueArrayElemMaxFlatOops=0",
                "-XX:ValueArrayElemMaxFlatSize=0",
                "-XX:-ValueArrayFlatten",
                "-XX:ValueFieldMaxFlatSize=0",
                "-XX:+ValueTypePassFieldsAsArgs",
                "-XX:+ValueTypeReturnedAsFields"};
        case 3: return new String[] {
                "-DVerifyIR=false",
                "-XX:+AlwaysIncrementalInline",
                "-XX:ValueArrayElemMaxFlatOops=0",
                "-XX:ValueArrayElemMaxFlatSize=0",
                "-XX:ValueFieldMaxFlatSize=0",
                "-XX:-ValueTypePassFieldsAsArgs",
                "-XX:-ValueTypeReturnedAsFields"};
        case 4: return new String[] {
                "-DVerifyIR=false",
                "-XX:ValueArrayElemMaxFlatOops=-1",
                "-XX:ValueArrayElemMaxFlatSize=-1",
                "-XX:+ValueArrayFlatten",
                "-XX:ValueFieldMaxFlatSize=0",
                "-XX:+ValueTypePassFieldsAsArgs",
                "-XX:-ValueTypeReturnedAsFields"};
        }

        return null;
    }

    /**
     * Override this method to provide extra parameters for selected scenarios
     */
    public String[] getExtraVMParameters(int scenario) {
        return null;
    }

    public static void main(String[] args) throws Throwable {
        if (args.length != 1) {
            throw new RuntimeException("Usage: @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea" +
                                       " -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions" +
                                       " -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+EnableValhalla" +
                                       " compiler.valhalla.valuetypes.ValueTypeTest <YourTestMainClass>");
        }
        String testMainClassName = args[0];
        Class testMainClass = Class.forName(testMainClassName);
        ValueTypeTest test = (ValueTypeTest)testMainClass.newInstance();
        List<String> scenarios = null;
        if (!SCENARIOS.isEmpty()) {
           scenarios = Arrays.asList(SCENARIOS.split(","));
        }
        for (int i=0; i<test.getNumScenarios(); i++) {
            if (scenarios == null || scenarios.contains(Integer.toString(i))) {
                System.out.println("Scenario #" + i + " -------- ");
                String[] cmds = InputArguments.getVmInputArgs();
                cmds = concat(cmds, test.getVMParameters(i));
                cmds = concat(cmds, test.getExtraVMParameters(i));
                cmds = concat(cmds, testMainClassName);

                OutputAnalyzer oa = ProcessTools.executeTestJvm(cmds);
                String output = oa.getOutput();
                oa.shouldHaveExitValue(0);
                System.out.println(output);
            } else {
                System.out.println("Scenario #" + i + " is skipped due to -Dscenarios=" + SCENARIOS);
            }
        }
    }

    protected ValueTypeTest() {
        List<String> list = null;
        List<String> exclude = null;
        if (!TESTLIST.isEmpty()) {
           list = Arrays.asList(TESTLIST.split(","));
        }
        if (!EXCLUDELIST.isEmpty()) {
           exclude = Arrays.asList(EXCLUDELIST.split(","));
        }
        // Gather all test methods and put them in Hashtable
        for (Method m : getClass().getDeclaredMethods()) {
            Test[] annos = m.getAnnotationsByType(Test.class);
            if (annos.length != 0 &&
                ((list == null || list.contains(m.getName())) && (exclude == null || !exclude.contains(m.getName())))) {
                tests.put(getClass().getSimpleName() + "::" + m.getName(), m);
            }
        }
    }

    protected void run(String[] args, Class<?>... classes) throws Throwable {
        if (args.length == 0) {
            // Spawn a new VM instance
            execute_vm();
        } else {
            // Execute tests
            run(classes);
        }
    }

    private void execute_vm() throws Throwable {
        Asserts.assertFalse(tests.isEmpty(), "no tests to execute");
        ArrayList<String> args = new ArrayList<String>(defaultFlags);
        String[] vmInputArgs = InputArguments.getVmInputArgs();
        for (String arg : vmInputArgs) {
            if (arg.startsWith("-XX:CompileThreshold")) {
                // Disable IR verification if non-default CompileThreshold is set
                VERIFY_IR = false;
            }
        }
        if (VERIFY_IR) {
            // Add print flags for IR verification
            args.addAll(printFlags);
            // Always trap for exception throwing to not confuse IR verification
            args.add("-XX:-OmitStackTraceInFastThrow");
        }
        if (VERIFY_VM) {
            args.addAll(verifyFlags);
        }
        // Run tests in own process and verify output
        args.add(getClass().getName());
        args.add("run");
        // Spawn process with default JVM options from the test's run command
        String[] cmds = Arrays.copyOf(vmInputArgs, vmInputArgs.length + args.size());
        System.arraycopy(args.toArray(), 0, cmds, vmInputArgs.length, args.size());
        OutputAnalyzer oa = ProcessTools.executeTestJvm(cmds);
        // If ideal graph printing is enabled/supported, verify output
        String output = oa.getOutput();
        oa.shouldHaveExitValue(0);
        if (VERIFY_IR) {
            if (output.contains("PrintIdeal enabled")) {
                parseOutput(output);
            } else {
                System.out.println(output);
                System.out.println("WARNING: IR verification failed! Running with -Xint, -Xcomp or release build?");
            }
        }
    }

    private void parseOutput(String output) throws Exception {
        Pattern comp_re = Pattern.compile("\\n\\s+\\d+\\s+\\d+\\s+(%| )(s| )(!| )b(n| )\\s+\\S+\\.(?<name>[^.]+::\\S+)\\s+(?<osr>@ \\d+\\s+)?[(]\\d+ bytes[)]\\n");
        Matcher m = comp_re.matcher(output);
        Map<String,String> compilations = new LinkedHashMap<>();
        int prev = 0;
        String methodName = null;
        while (m.find()) {
            if (prev == 0) {
                // Print header
                System.out.print(output.substring(0, m.start()+1));
            } else if (methodName != null) {
                compilations.put(methodName, output.substring(prev, m.start()+1));
            }
            if (m.group("osr") != null) {
                methodName = null;
            } else {
                methodName = m.group("name");
            }
            prev = m.end();
        }
        if (prev == 0) {
            // Print header
            System.out.print(output);
        } else if (methodName != null) {
            compilations.put(methodName, output.substring(prev));
        }
        // Iterate over compilation output
        for (String testName : compilations.keySet()) {
            Method test = tests.get(testName);
            if (test == null) {
                // Skip helper methods
                continue;
            }
            String graph = compilations.get(testName);
            if (PRINT_GRAPH) {
                System.out.println("\nGraph for " + testName + "\n" + graph);
            }
            // Parse graph using regular expressions to determine if it contains forbidden nodes
            Test[] annos = test.getAnnotationsByType(Test.class);
            Test anno = null;
            for (Test a : annos) {
                if ((a.valid() & ValueTypePassFieldsAsArgsOn) != 0 && ValueTypePassFieldsAsArgs) {
                    assert anno == null;
                    anno = a;
                } else if ((a.valid() & ValueTypePassFieldsAsArgsOff) != 0 && !ValueTypePassFieldsAsArgs) {
                    assert anno == null;
                    anno = a;
                } else if ((a.valid() & ValueTypeArrayFlattenOn) != 0 && ValueTypeArrayFlatten) {
                    assert anno == null;
                    anno = a;
                } else if ((a.valid() & ValueTypeArrayFlattenOff) != 0 && !ValueTypeArrayFlatten) {
                    assert anno == null;
                    anno = a;
                } else if ((a.valid() & ValueTypeReturnedAsFieldsOn) != 0 && ValueTypeReturnedAsFields) {
                    assert anno == null;
                    anno = a;
                } else if ((a.valid() & ValueTypeReturnedAsFieldsOff) != 0 && !ValueTypeReturnedAsFields) {
                    assert anno == null;
                    anno = a;
                } else if ((a.valid() & AlwaysIncrementalInlineOn) != 0 && AlwaysIncrementalInline) {
                    assert anno == null;
                    anno = a;
                } else if ((a.valid() & AlwaysIncrementalInlineOff) != 0 && !AlwaysIncrementalInline) {
                    assert anno == null;
                    anno = a;
                }
            }
            assert anno != null;
            String regexFail = anno.failOn();
            if (!regexFail.isEmpty()) {
                Pattern pattern = Pattern.compile(regexFail.substring(0, regexFail.length()-1));
                Matcher matcher = pattern.matcher(graph);
                boolean found = matcher.find();
                Asserts.assertFalse(found, "Graph for '" + testName + "' contains forbidden node:\n" + (found ? matcher.group() : ""));
            }
            String[] regexMatch = anno.match();
            int[] matchCount = anno.matchCount();
            for (int i = 0; i < regexMatch.length; ++i) {
                Pattern pattern = Pattern.compile(regexMatch[i].substring(0, regexMatch[i].length()-1));
                Matcher matcher = pattern.matcher(graph);
                int count = 0;
                String nodes = "";
                while (matcher.find()) {
                    count++;
                    nodes += matcher.group() + "\n";
                }
                if (matchCount[i] < 0) {
                    Asserts.assertLTE(Math.abs(matchCount[i]), count, "Graph for '" + testName + "' contains different number of match nodes:\n" + nodes);
                } else {
                    Asserts.assertEQ(matchCount[i], count, "Graph for '" + testName + "' contains different number of match nodes:\n" + nodes);
                }
            }
            tests.remove(testName);
            System.out.println(testName + " passed");
        }
        // Check if all tests were compiled
        if (tests.size() != 0) {
            for (String name : tests.keySet()) {
                System.out.println("Test '" + name + "' not compiled!");
            }
            throw new RuntimeException("Not all tests were compiled");
        }
    }

    private void setup(Class<?> clazz) {
        if (XCOMP) {
            // Don't control compilation if -Xcomp is enabled
            return;
        }
        if (DUMP_REPLAY) {
            // Generate replay compilation files
            String directive = "[{ match: \"*.*\", DumpReplay: true }]";
            if (WHITE_BOX.addCompilerDirective(directive) != 1) {
                throw new RuntimeException("Failed to add compiler directive");
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(Test.class)) {
                // Don't inline tests
                WHITE_BOX.testSetDontInlineMethod(m, true);
            }
            if (m.isAnnotationPresent(DontCompile.class)) {
                WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_ANY, true);
                WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_ANY, false);
                WHITE_BOX.testSetDontInlineMethod(m, true);
            }
            if (m.isAnnotationPresent(ForceInline.class)) {
                WHITE_BOX.testSetForceInlineMethod(m, true);
            } else if (m.isAnnotationPresent(DontInline.class)) {
                WHITE_BOX.testSetDontInlineMethod(m, true);
            }
        }

        // Compile class initializers
        WHITE_BOX.enqueueInitializerForCompilation(clazz, COMP_LEVEL_FULL_OPTIMIZATION);
    }

    private void run(Class<?>... classes) throws Exception {
        if (USE_COMPILER && PRINT_IDEAL && !XCOMP) {
            System.out.println("PrintIdeal enabled");
        }
        System.out.format("rI = %d, rL = %d\n", rI, rL);

        setup(getClass());
        for (Class<?> clazz : classes) {
            setup(clazz);
        }

        // Execute tests
        TreeMap<Long, String> durations = PRINT_TIMES ? new TreeMap<Long, String>() : null;
        for (Method test : tests.values()) {
            long startTime = System.nanoTime();
            Method verifier = getClass().getMethod(test.getName() + "_verifier", boolean.class);
            // Warmup using verifier method
            Warmup anno = test.getAnnotation(Warmup.class);
            int warmup = anno == null ? WARMUP : anno.value();
            for (int i = 0; i < warmup; ++i) {
                verifier.invoke(this, true);
            }
            // Trigger compilation
            WHITE_BOX.enqueueMethodForCompilation(test, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(!USE_COMPILER || WHITE_BOX.isMethodCompiled(test, false), test + " not compiled");
            // Check result
            verifier.invoke(this, false);
            if (PRINT_TIMES) {
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                durations.put(duration, test.getName());
            }
        }

        // Print execution times
        if (PRINT_TIMES) {
          System.out.println("\n\nTest execution times:");
          for (Map.Entry<Long, String> entry : durations.entrySet()) {
              System.out.format("%-10s%15d ns\n", entry.getValue() + ":", entry.getKey());
          }
        }
    }
}
