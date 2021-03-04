/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.inlinetypes;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

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
    int compLevel() default InlineTypeTest.COMP_LEVEL_ANY;
    int valid() default 0;
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

// Force method compilation
@Retention(RetentionPolicy.RUNTIME)
@interface ForceCompile {
    int compLevel() default InlineTypeTest.COMP_LEVEL_ANY;
}

// Number of warmup iterations
@Retention(RetentionPolicy.RUNTIME)
@interface Warmup {
    int value();
}

// Do not enqueue the test method for compilation immediately after warmup loops have finished. Instead
// let the test method be compiled with on-stack-replacement.
@Retention(RetentionPolicy.RUNTIME)
@interface OSRCompileOnly {}

// Skip this test temporarily for C1 testing
@Retention(RetentionPolicy.RUNTIME)
@interface TempSkipForC1 {
    String reason() default "";
}

public abstract class InlineTypeTest {
    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    protected static final int COMP_LEVEL_ANY               = -2;
    protected static final int COMP_LEVEL_ALL               = -2;
    protected static final int COMP_LEVEL_AOT               = -1;
    protected static final int COMP_LEVEL_NONE              =  0;
    protected static final int COMP_LEVEL_SIMPLE            =  1;     // C1
    protected static final int COMP_LEVEL_LIMITED_PROFILE   =  2;     // C1, invocation & backedge counters
    protected static final int COMP_LEVEL_FULL_PROFILE      =  3;     // C1, invocation & backedge counters + mdo
    protected static final int COMP_LEVEL_FULL_OPTIMIZATION =  4;     // C2 or JVMCI

    protected static final boolean TieredCompilation = (Boolean)WHITE_BOX.getVMFlag("TieredCompilation");
    protected static final long TieredStopAtLevel = (Long)WHITE_BOX.getVMFlag("TieredStopAtLevel");
    static final boolean TEST_C1 = TieredCompilation && TieredStopAtLevel < COMP_LEVEL_FULL_OPTIMIZATION;

    // Random test values
    public static final int  rI = Utils.getRandomInstance().nextInt() % 1000;
    public static final long rL = Utils.getRandomInstance().nextLong() % 1000;
    public static final double rD = Utils.getRandomInstance().nextDouble() % 1000;

    // User defined settings
    protected static final boolean XCOMP = Platform.isComp();
    private static final boolean PRINT_GRAPH = true;
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "false"));
    private static final boolean PRINT_TIMES = Boolean.parseBoolean(System.getProperty("PrintTimes", "false"));
    private static final boolean COMPILE_COMMANDS = Boolean.parseBoolean(System.getProperty("CompileCommands", "true")) && !XCOMP;
    private static       boolean VERIFY_IR = Boolean.parseBoolean(System.getProperty("VerifyIR", "true")) && !XCOMP && !TEST_C1 && COMPILE_COMMANDS;
    private static final String SCENARIOS = System.getProperty("Scenarios", "");
    private static final String TESTLIST = System.getProperty("Testlist", "");
    private static final String EXCLUDELIST = System.getProperty("Exclude", "");
    private static final int WARMUP = Integer.parseInt(System.getProperty("Warmup", "251"));
    private static final boolean DUMP_REPLAY = Boolean.parseBoolean(System.getProperty("DumpReplay", "false"));
    private static final boolean FLIP_C1_C2 = Boolean.parseBoolean(System.getProperty("FlipC1C2", "false"));
    private static final boolean GC_AFTER = Boolean.parseBoolean(System.getProperty("GCAfter", "false"));
    private static final int OSR_TEST_TIMEOUT = Integer.parseInt(System.getProperty("OSRTestTimeOut", "5000"));
    protected static final boolean STRESS_CC = Boolean.parseBoolean(System.getProperty("StressCC", "false"));
    private static final boolean SHUFFLE_TESTS = Boolean.parseBoolean(System.getProperty("ShuffleTests", "true"));
    private static final boolean PREFER_CL_FLAGS = Boolean.parseBoolean(System.getProperty("PreferCommandLineFlags", "false"));

    // Pre-defined settings
    private static final String[] defaultFlags = {
        "-XX:-BackgroundCompilation"};
    private static final String[] compileCommandFlags = {
        "-XX:CompileCommand=quiet",
        "-XX:CompileCommand=compileonly,java.lang.invoke.*::*",
        "-XX:CompileCommand=compileonly,java.lang.Long::sum",
        "-XX:CompileCommand=compileonly,java.lang.Object::<init>",
        "-XX:CompileCommand=inline,compiler.valhalla.inlinetypes.MyValue*::<init>",
        "-XX:CompileCommand=compileonly,compiler.valhalla.inlinetypes.*::*"};
    private static final String[] printFlags = {
        "-XX:+PrintCompilation", "-XX:+PrintIdeal", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintOptoAssembly"};

    protected static final int InlineTypePassFieldsAsArgsOn = 0x1;
    protected static final int InlineTypePassFieldsAsArgsOff = 0x2;
    protected static final int InlineTypeArrayFlattenOn = 0x4;
    protected static final int InlineTypeArrayFlattenOff = 0x8;
    protected static final int InlineTypeReturnedAsFieldsOn = 0x10;
    protected static final int InlineTypeReturnedAsFieldsOff = 0x20;
    protected static final int AlwaysIncrementalInlineOn = 0x40;
    protected static final int AlwaysIncrementalInlineOff = 0x80;
    protected static final int G1GCOn = 0x100;
    protected static final int G1GCOff = 0x200;
    protected static final int ZGCOn = 0x400;
    protected static final int ZGCOff = 0x800;
    protected static final int ArrayLoadStoreProfileOn = 0x1000;
    protected static final int ArrayLoadStoreProfileOff = 0x2000;
    protected static final int TypeProfileOn = 0x4000;
    protected static final int TypeProfileOff = 0x8000;
    protected static final int ACmpProfileOn = 0x10000;
    protected static final int ACmpProfileOff = 0x20000;
    protected static final boolean InlineTypePassFieldsAsArgs = (Boolean)WHITE_BOX.getVMFlag("InlineTypePassFieldsAsArgs");
    protected static final boolean InlineTypeArrayFlatten = (WHITE_BOX.getIntxVMFlag("FlatArrayElementMaxSize") == -1);
    protected static final boolean InlineTypeReturnedAsFields = (Boolean)WHITE_BOX.getVMFlag("InlineTypeReturnedAsFields");
    protected static final boolean AlwaysIncrementalInline = (Boolean)WHITE_BOX.getVMFlag("AlwaysIncrementalInline");
    protected static final boolean G1GC = (Boolean)WHITE_BOX.getVMFlag("UseG1GC");
    protected static final boolean ZGC = (Boolean)WHITE_BOX.getVMFlag("UseZGC");
    protected static final boolean VerifyOops = (Boolean)WHITE_BOX.getVMFlag("VerifyOops");
    protected static final boolean UseArrayLoadStoreProfile = (Boolean)WHITE_BOX.getVMFlag("UseArrayLoadStoreProfile");
    protected static final long TypeProfileLevel = (Long)WHITE_BOX.getVMFlag("TypeProfileLevel");
    protected static final boolean UseACmpProfile = (Boolean)WHITE_BOX.getVMFlag("UseACmpProfile");
    protected static final long PerMethodTrapLimit = (Long)WHITE_BOX.getVMFlag("PerMethodTrapLimit");
    protected static final boolean ProfileInterpreter = (Boolean)WHITE_BOX.getVMFlag("ProfileInterpreter");

    protected static final Hashtable<String, Method> tests = new Hashtable<String, Method>();
    protected static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    protected static final boolean PRINT_IDEAL  = WHITE_BOX.getBooleanVMFlag("PrintIdeal");

    // Regular expressions used to match nodes in the PrintIdeal output
    protected static final String START = "(\\d+ (.*";
    protected static final String MID = ".*)+ ===.*";
    protected static final String END = ")|";
    // Generic allocation
    protected static final String ALLOC_G  = "(.*call,static  wrapper for: _new_instance_Java" + END;
    protected static final String ALLOCA_G = "(.*call,static  wrapper for: _new_array_Java" + END;
    // Inline type allocation
    protected static final String ALLOC  = "(.*precise klass compiler/valhalla/inlinetypes/MyValue.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_instance_Java" + END;
    protected static final String ALLOCA = "(.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_array_Java" + END;
    protected static final String LOAD   = START + "Load(B|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/MyValue.*" + END;
    protected static final String LOADK  = START + "LoadK" + MID + END;
    protected static final String STORE  = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/MyValue.*" + END;
    protected static final String LOOP   = START + "Loop" + MID + "" + END;
    protected static final String COUNTEDLOOP = START + "CountedLoop\\b" + MID + "" + END;
    protected static final String COUNTEDLOOP_MAIN = START + "CountedLoop\\b" + MID + "main" + END;
    protected static final String TRAP   = START + "CallStaticJava" + MID + "uncommon_trap.*(unstable_if|predicate)" + END;
    protected static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
    protected static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
    protected static final String CALL = START + "CallStaticJava" + MID + END;
    protected static final String STORE_INLINE_FIELDS = START + "CallStaticJava" + MID + "store_inline_type_fields" + END;
    protected static final String SCOBJ = "(.*# ScObj.*" + END;
    protected static final String LOAD_UNKNOWN_INLINE = "(.*call_leaf,runtime  load_unknown_inline.*" + END;
    protected static final String STORE_UNKNOWN_INLINE = "(.*call_leaf,runtime  store_unknown_inline.*" + END;
    protected static final String INLINE_ARRAY_NULL_GUARD = "(.*call,static  wrapper for: uncommon_trap.*reason='null_check' action='none'.*" + END;
    protected static final String INTRINSIC_SLOW_PATH = "(.*call,static  wrapper for: uncommon_trap.*reason='intrinsic_or_type_checked_inlining'.*" + END;
    protected static final String CLONE_INTRINSIC_SLOW_PATH = "(.*call,static.*java.lang.Object::clone.*" + END;
    protected static final String CLASS_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*class_check" + END;
    protected static final String NULL_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_check" + END;
    protected static final String NULL_ASSERT_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_assert" + END;
    protected static final String RANGE_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*range_check" + END;
    protected static final String UNHANDLED_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*unhandled" + END;
    protected static final String PREDICATE_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*predicate" + END;
    protected static final String MEMBAR = START + "MemBar" + MID + END;
    protected static final String CHECKCAST_ARRAY = "(cmp.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*" + END;
    protected static final String CHECKCAST_ARRAYCOPY = "(.*call_leaf_nofp,runtime  checkcast_arraycopy.*" + END;
    protected static final String JLONG_ARRAYCOPY = "(.*call_leaf_nofp,runtime  jlong_disjoint_arraycopy.*" + END;
    protected static final String FIELD_ACCESS = "(.*Field: *" + END;
    protected static final String SUBSTITUTABILITY_TEST = START + "CallStaticJava" + MID + "java.lang.invoke.ValueBootstrapMethods::isSubstitutable" + END;

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
     * the 6 built-in scenarios
     */
    public int getNumScenarios() {
        return 6;
    }

    /**
     * VM parameters for the 5 built-in test scenarios. If your test needs to append
     * extra parameters for (some of) these scenarios, override getExtraVMParameters().
     */
    public String[] getVMParameters(int scenario) {
        switch (scenario) {
        case 0: return new String[] {
                "-XX:-UseACmpProfile",
                "-XX:+AlwaysIncrementalInline",
                "-XX:FlatArrayElementMaxOops=5",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:+InlineTypeReturnedAsFields"};
        case 1: return new String[] {
                "-XX:-UseACmpProfile",
                "-XX:-UseCompressedOops",
                "-XX:FlatArrayElementMaxOops=5",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:-InlineTypePassFieldsAsArgs",
                "-XX:-InlineTypeReturnedAsFields"};
        case 2: return new String[] {
                "-XX:-UseACmpProfile",
                "-XX:-UseCompressedOops",
                "-XX:FlatArrayElementMaxOops=0",
                "-XX:FlatArrayElementMaxSize=0",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:+InlineTypeReturnedAsFields",
                "-XX:+StressInlineTypeReturnedAsFields"};
        case 3: return new String[] {
                "-DVerifyIR=false",
                "-XX:+AlwaysIncrementalInline",
                "-XX:FlatArrayElementMaxOops=0",
                "-XX:FlatArrayElementMaxSize=0",
                "-XX:InlineFieldMaxFlatSize=0",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:+InlineTypeReturnedAsFields"};
        case 4: return new String[] {
                "-DVerifyIR=false",
                "-XX:FlatArrayElementMaxOops=-1",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:InlineFieldMaxFlatSize=0",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:-InlineTypeReturnedAsFields",
                "-XX:-ReduceInitialCardMarks"};
        case 5: return new String[] {
                "-XX:-UseACmpProfile",
                "-XX:+AlwaysIncrementalInline",
                "-XX:FlatArrayElementMaxOops=5",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:-InlineTypePassFieldsAsArgs",
                "-XX:-InlineTypeReturnedAsFields"};
        }
        return null;
    }

    /**
     * Override this method and return a non-null reason if the given scenario should be
     * ignored (due to an existing bug, etc).
     */
    String isScenarioIgnored(int scenario) {
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
            throw new RuntimeException("Usage: @run main/othervm/timeout=120 -Xbootclasspath/a:." +
                                       " -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions" +
                                       " -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI" +
                                       " compiler.valhalla.inlinetypes.InlineTypeTest <YourTestMainClass>");
        }
        String testMainClassName = args[0];
        Class testMainClass = Class.forName(testMainClassName);
        InlineTypeTest test = (InlineTypeTest)testMainClass.newInstance();
        List<String> scenarios = null;
        if (!SCENARIOS.isEmpty()) {
           scenarios = Arrays.asList(SCENARIOS.split(","));
        }
        for (int i=0; i<test.getNumScenarios(); i++) {
            String reason;
            if ((reason = test.isScenarioIgnored(i)) != null) {
                System.out.println("Scenario #" + i + " is ignored: " + reason);
            } else if (scenarios != null && !scenarios.contains(Integer.toString(i))) {
                System.out.println("Scenario #" + i + " is skipped due to -Dscenarios=" + SCENARIOS);
            } else {
                System.out.println("Scenario #" + i + " -------- ");
                String[] cmds = new String[0];
                if (!PREFER_CL_FLAGS) {
                    cmds = InputArguments.getVmInputArgs();
                }
                cmds = concat(cmds, test.getVMParameters(i));
                cmds = concat(cmds, test.getExtraVMParameters(i));
                if (PREFER_CL_FLAGS) {
                    // Prefer flags set via the command line over the ones set by the test scenarios
                    cmds = concat(cmds, InputArguments.getVmInputArgs());
                }
                cmds = concat(cmds, testMainClassName);

                OutputAnalyzer oa = ProcessTools.executeTestJvm(cmds);
                String output = oa.getOutput();
                oa.shouldHaveExitValue(0);
                System.out.println(output);
            }
        }
    }

    // To exclude test cases, use -DExclude=<case1>,<case2>,...
    // Each case can be just the method name, or can be <class>.<method>. The latter form is useful
    // when you are running several tests at the same time.
    //
    // jtreg -DExclude=test12 TestArrays.java
    // jtreg -DExclude=test34 TestLWorld.java
    // -- or --
    // jtreg -DExclude=TestArrays.test12,TestLWorld.test34 TestArrays.java TestLWorld.java
    //
    private List<String> buildExcludeList() {
        List<String> exclude = null;
        String classPrefix = getClass().getSimpleName() + ".";
        if (!EXCLUDELIST.isEmpty()) {
            exclude = new ArrayList(Arrays.asList(EXCLUDELIST.split(",")));
            for (int i = exclude.size() - 1; i >= 0; i--) {
                String ex = exclude.get(i);
                if (ex.indexOf(".") > 0) {
                    if (ex.startsWith(classPrefix)) {
                        ex = ex.substring(classPrefix.length());
                        exclude.set(i, ex);
                    } else {
                        exclude.remove(i);
                    }
                }
            }
        }
        return exclude;
    }

    protected InlineTypeTest() {
        List<String> list = null;
        if (!TESTLIST.isEmpty()) {
           list = Arrays.asList(TESTLIST.split(","));
        }
        List<String> exclude = buildExcludeList();

        // Gather all test methods and put them in Hashtable
        for (Method m : getClass().getDeclaredMethods()) {
            Test[] annos = m.getAnnotationsByType(Test.class);
            if (annos.length != 0 &&
                ((list == null || list.contains(m.getName())) && (exclude == null || !exclude.contains(m.getName())))) {
                tests.put(getClass().getSimpleName() + "::" + m.getName(), m);
            } else if (annos.length == 0 && m.getName().startsWith("test")) {
                try {
                    getClass().getMethod(m.getName() + "_verifier", boolean.class);
                    throw new RuntimeException(m.getName() + " has a verifier method but no @Test annotation");
                } catch (NoSuchMethodException e) {
                    // Expected
                }
            }
        }
    }

    protected void run(String[] args, Class<?>... classes) throws Throwable {
        if (args.length == 0) {
            // Spawn a new VM instance
            execute_vm();
        } else {
            // Execute tests in the VM spawned by the above code.
            Asserts.assertTrue(args.length == 1 && args[0].equals("run"), "must be");
            run(classes);
        }
    }

    private void execute_vm() throws Throwable {
        Asserts.assertFalse(tests.isEmpty(), "no tests to execute");
        String[] vmInputArgs = InputArguments.getVmInputArgs();
        for (String arg : vmInputArgs) {
            if (arg.startsWith("-XX:CompileThreshold")) {
                // Disable IR verification if non-default CompileThreshold is set
                VERIFY_IR = false;
            }
        }
        // Each VM is launched with flags in this order, so the later ones can override the earlier one:
        //     VERIFY_IR flags specified below
        //     vmInputArgs, which consists of:
        //        @run options
        //        getVMParameters()
        //        getExtraVMParameters()
        //     defaultFlags
        //     compileCommandFlags
        String cmds[] = null;
        if (VERIFY_IR) {
            // Add print flags for IR verification
            cmds = concat(cmds, printFlags);
            // Always trap for exception throwing to not confuse IR verification
            cmds = concat(cmds, "-XX:-OmitStackTraceInFastThrow");
        }
        cmds = concat(cmds, vmInputArgs);
        cmds = concat(cmds, defaultFlags);
        if (COMPILE_COMMANDS) {
          cmds = concat(cmds, compileCommandFlags);
        }

        // Run tests in own process and verify output
        cmds = concat(cmds, getClass().getName(), "run");
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

    static final class TestAnnotation {
        private final int flag;
        private final BooleanSupplier predicate;

        private static final TestAnnotation testAnnotations[] = {
            new TestAnnotation(InlineTypePassFieldsAsArgsOn, () -> InlineTypePassFieldsAsArgs),
            new TestAnnotation(InlineTypePassFieldsAsArgsOff, () -> !InlineTypePassFieldsAsArgs),
            new TestAnnotation(InlineTypeArrayFlattenOn, () -> InlineTypeArrayFlatten),
            new TestAnnotation(InlineTypeArrayFlattenOff, () -> !InlineTypeArrayFlatten),
            new TestAnnotation(InlineTypeReturnedAsFieldsOn, () -> InlineTypeReturnedAsFields),
            new TestAnnotation(InlineTypeReturnedAsFieldsOff, () -> !InlineTypeReturnedAsFields),
            new TestAnnotation(AlwaysIncrementalInlineOn, () -> AlwaysIncrementalInline),
            new TestAnnotation(AlwaysIncrementalInlineOff, () -> !AlwaysIncrementalInline),
            new TestAnnotation(G1GCOn, () -> G1GC),
            new TestAnnotation(G1GCOff, () -> !G1GC),
            new TestAnnotation(ZGCOn, () -> ZGC),
            new TestAnnotation(ZGCOff, () -> !ZGC),
            new TestAnnotation(ArrayLoadStoreProfileOn, () -> UseArrayLoadStoreProfile),
            new TestAnnotation(ArrayLoadStoreProfileOff, () -> !UseArrayLoadStoreProfile),
            new TestAnnotation(TypeProfileOn, () -> TypeProfileLevel == 222),
            new TestAnnotation(TypeProfileOff, () -> TypeProfileLevel == 0),
            new TestAnnotation(ACmpProfileOn, () -> UseACmpProfile),
            new TestAnnotation(ACmpProfileOff, () -> !UseACmpProfile),
        };

        private TestAnnotation(int flag, BooleanSupplier predicate) {
            this.flag = flag;
            this.predicate = predicate;
        }

        private boolean match(Test a) {
            return (a.valid() & flag) != 0 && predicate.getAsBoolean();
        }

        static boolean find(Test a) {
            Stream<TestAnnotation> s = Arrays.stream(testAnnotations).filter(t -> t.match(a));
            long c = s.count();
            if (c > 1) {
                throw new RuntimeException("At most one Test annotation should match");
            }
            return c > 0;
        }
    }

    private void parseOutput(String output) throws Exception {
        Pattern comp_re = Pattern.compile("\\n\\s+\\d+\\s+\\d+\\s+(%| )(s| )(!| )b(n| )\\s+\\d?\\s+\\S+\\.(?<name>[^.]+::\\S+)\\s+(?<osr>@ \\d+\\s+)?[(]\\d+ bytes[)]");
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
            System.out.println("\nGraph for " + testName + "\n" + graph);
            // Parse graph using regular expressions to determine if it contains forbidden nodes
            Test[] annos = test.getAnnotationsByType(Test.class);
            Test anno = Arrays.stream(annos).filter(TestAnnotation::find).findFirst().orElse(null);
            if (anno == null) {
                Object[] res = Arrays.stream(annos).filter(a -> a.valid() == 0).toArray();
                if (res.length != 1) {
                    throw new RuntimeException("Only one Test annotation should match");
                }
                anno = (Test)res[0];
            }
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
                    Asserts.assertLTE(Math.abs(matchCount[i]), count, "Graph for '" + testName + "' contains different number of match nodes (expected >= " + Math.abs(matchCount[i]) + " but got " + count + "):\n" + nodes);
                } else {
                    Asserts.assertEQ(matchCount[i], count, "Graph for '" + testName + "' contains different number of match nodes (expected " + matchCount[i] + " but got " + count + "):\n" + nodes);
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
                Asserts.assertFalse(m.isAnnotationPresent(DontInline.class), "Method " + m.getName() + " has contradicting DontInline annotation");
                WHITE_BOX.testSetForceInlineMethod(m, true);
            }
            if (m.isAnnotationPresent(DontInline.class)) {
                Asserts.assertFalse(m.isAnnotationPresent(ForceInline.class), "Method " + m.getName() + " has contradicting ForceInline annotation");
                WHITE_BOX.testSetDontInlineMethod(m, true);
            }
            if (STRESS_CC) {
                // Exclude some methods from compilation with C2 to stress test the calling convention
                if (Utils.getRandomInstance().nextBoolean()) {
                    System.out.println("Excluding from C2 compilation: " + m);
                    WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_FULL_OPTIMIZATION, false);
                }
            }
        }
        // Only force compilation now because above annotations affect inlining
        for (Method m : methods) {
            if (m.isAnnotationPresent(ForceCompile.class)) {
                Asserts.assertFalse(m.isAnnotationPresent(DontCompile.class), "Method " + m.getName() + " has contradicting DontCompile annotation");
                int compLevel = getCompLevel(m.getAnnotation(ForceCompile.class));
                enqueueMethodForCompilation(m, compLevel);
            }
        }
        // Compile class initializers
        int compLevel = getCompLevel(null);
        WHITE_BOX.enqueueInitializerForCompilation(clazz, compLevel);
    }

    private void run(Class<?>... classes) throws Exception {
        if (USE_COMPILER && PRINT_IDEAL && !XCOMP && !STRESS_CC) {
            System.out.println("PrintIdeal enabled");
        }
        System.out.format("rI = %d, rL = %d\n", rI, rL);

        setup(getClass());
        for (Class<?> clazz : classes) {
            setup(clazz);
        }

        TreeMap<Long, String> durations = (PRINT_TIMES || VERBOSE) ? new TreeMap<Long, String>() : null;
        List<Method> testList = new ArrayList<Method>(tests.values());
        if (SHUFFLE_TESTS) {
            // Execute tests in random order (execution sequence affects profiling)
            Collections.shuffle(testList, Utils.getRandomInstance());
        }
        for (Method test : testList) {
            if (VERBOSE) {
                System.out.println("Starting " + test.getName());
            }
            TempSkipForC1 c1skip = test.getAnnotation(TempSkipForC1.class);
            if (TEST_C1 && c1skip != null) {
                System.out.println("Skipped " + test.getName() + " for C1 testing: " + c1skip.reason());
                continue;
            }
            long startTime = System.nanoTime();
            Method verifier = getClass().getMethod(test.getName() + "_verifier", boolean.class);
            // Warmup using verifier method
            Warmup anno = test.getAnnotation(Warmup.class);
            int warmup = anno == null ? WARMUP : anno.value();
            for (int i = 0; i < warmup; ++i) {
                verifier.invoke(this, true);
            }
            boolean osrOnly = (test.getAnnotation(OSRCompileOnly.class) != null);
            int compLevel = getCompLevel(test.getAnnotation(Test.class));

            // C1 generates a lot of code when VerifyOops is enabled and may run out of space (for a small
            // number of test cases).
            boolean maybeCodeBufferOverflow = (TEST_C1 && VerifyOops);

            if (osrOnly) {
                long started = System.currentTimeMillis();
                boolean stateCleared = false;
                for (;;) {
                    long elapsed = System.currentTimeMillis() - started;
                    int level = WHITE_BOX.getMethodCompilationLevel(test);
                    if (maybeCodeBufferOverflow && elapsed > 5000 && (!WHITE_BOX.isMethodCompiled(test, false) || level != compLevel)) {
                        System.out.println("Temporarily disabling VerifyOops");
                        try {
                            WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
                            if (!stateCleared) {
                                WHITE_BOX.clearMethodState(test);
                                stateCleared = true;
                            }
                            verifier.invoke(this, false);
                        } finally {
                            WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
                            System.out.println("Re-enabled VerifyOops");
                        }
                    } else {
                        verifier.invoke(this, false);
                    }

                    boolean b = WHITE_BOX.isMethodCompiled(test, false);
                    if (VERBOSE) {
                        System.out.println("Is " + test.getName() + " compiled? " + b);
                    }
                    if (b || XCOMP || STRESS_CC || !USE_COMPILER) {
                        // Don't control compilation if -Xcomp is enabled, or if compiler is disabled
                        break;
                    }
                    Asserts.assertTrue(OSR_TEST_TIMEOUT < 0 || elapsed < OSR_TEST_TIMEOUT, test + " not compiled after " + OSR_TEST_TIMEOUT + " ms");
                }
            } else {
                // Trigger compilation
                enqueueMethodForCompilation(test, compLevel);
                if (maybeCodeBufferOverflow && !WHITE_BOX.isMethodCompiled(test, false)) {
                    // Let's disable VerifyOops temporarily and retry.
                    WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
                    WHITE_BOX.clearMethodState(test);
                    enqueueMethodForCompilation(test, compLevel);
                    WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
                }
                if (!STRESS_CC && USE_COMPILER) {
                    Asserts.assertTrue(WHITE_BOX.isMethodCompiled(test, false), test + " not compiled");
                    int level = WHITE_BOX.getMethodCompilationLevel(test);
                    Asserts.assertEQ(level, compLevel, "Unexpected compilation level for " + test);
                }
                // Check result
                verifier.invoke(this, false);
            }
            if (PRINT_TIMES || VERBOSE) {
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                durations.put(duration, test.getName());
                if (VERBOSE) {
                    System.out.println("Done " + test.getName() + ": " + duration + " ns = " + (duration / 1000000) + " ms");
                }
            }
            if (GC_AFTER) {
                System.out.println("doing GC");
                System.gc();
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

    // Get the appropriate compilation level for a method, according to the
    // given annotation, as well as the current test scenario and VM options.
    //
    private int getCompLevel(Object annotation) {
        int compLevel;
        if (annotation == null) {
            compLevel = COMP_LEVEL_ANY;
        } else if (annotation instanceof Test) {
            compLevel = ((Test)annotation).compLevel();
        } else {
            compLevel = ((ForceCompile)annotation).compLevel();
        }

        return restrictCompLevel(compLevel);
    }

    // Get the appropriate level as permitted by the test scenario and VM options.
    private static int restrictCompLevel(int compLevel) {
        if (compLevel == COMP_LEVEL_ANY) {
            compLevel = COMP_LEVEL_FULL_OPTIMIZATION;
        }
        if (FLIP_C1_C2) {
            // Effectively treat all (compLevel = C1) as (compLevel = C2), and
            //                       (compLevel = C2) as (compLevel = C1).
            if (compLevel == COMP_LEVEL_SIMPLE) {
                compLevel = COMP_LEVEL_FULL_OPTIMIZATION;
            } else if (compLevel == COMP_LEVEL_FULL_OPTIMIZATION) {
                compLevel = COMP_LEVEL_SIMPLE;
            }
        }
        if (!TEST_C1 && compLevel < COMP_LEVEL_FULL_OPTIMIZATION) {
            compLevel = COMP_LEVEL_FULL_OPTIMIZATION;
        }
        if (TieredCompilation && compLevel > (int)TieredStopAtLevel) {
            compLevel = (int)TieredStopAtLevel;
        }
        return compLevel;
    }

    public static void enqueueMethodForCompilation(Method m, int level) {
        level = restrictCompLevel(level);
        if (VERBOSE) {
            System.out.println("enqueueMethodForCompilation " + m + ", level = " + level);
        }
        WHITE_BOX.enqueueMethodForCompilation(m, level);
    }

    enum TriState {
        Maybe,
        Yes,
        No
    }

    static private TriState compiledByC2(Method m) {
        if (!USE_COMPILER || XCOMP || TEST_C1 ||
            (STRESS_CC && !WHITE_BOX.isMethodCompilable(m, COMP_LEVEL_FULL_OPTIMIZATION, false))) {
            return TriState.Maybe;
        }
        if (WHITE_BOX.isMethodCompiled(m, false) &&
            WHITE_BOX.getMethodCompilationLevel(m, false) >= COMP_LEVEL_FULL_OPTIMIZATION) {
            return TriState.Yes;
        }
        return TriState.No;
    }

    static boolean isCompiledByC2(Method m) {
        return compiledByC2(m) == TriState.Yes;
    }

    static void assertDeoptimizedByC2(Method m) {
        if (compiledByC2(m) == TriState.Yes && PerMethodTrapLimit != 0 && ProfileInterpreter) {
            throw new RuntimeException("Expected to have deoptimized");
        }
    }

    static void assertCompiledByC2(Method m) {
        if (compiledByC2(m) == TriState.No) {
            throw new RuntimeException("Expected to be compiled");
        }
    }
}
