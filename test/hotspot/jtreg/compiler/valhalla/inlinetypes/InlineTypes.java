/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Utils;
import compiler.lib.ir_framework.Scenario;
import compiler.lib.ir_framework.TestFramework;

public class InlineTypes {
    public static final int  rI = Utils.getRandomInstance().nextInt() % 1000;
    public static final long rL = Utils.getRandomInstance().nextLong() % 1000;
    public static final double rD = Utils.getRandomInstance().nextDouble() % 1000;

    public static final Scenario[] DEFAULT_SCENARIOS = {
            new Scenario(0,
                         "-XX:+IgnoreUnrecognizedVMOptions",
                         "-XX:-UseACmpProfile",
                         "-XX:+AlwaysIncrementalInline",
                         "-XX:FlatArrayElementMaxOops=5",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:+InlineTypeReturnedAsFields"
            ),
            new Scenario(1,
                         "-XX:+IgnoreUnrecognizedVMOptions",
                         "-XX:-UseACmpProfile",
                         "-XX:-UseCompressedOops",
                         "-XX:FlatArrayElementMaxOops=5",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:-InlineTypePassFieldsAsArgs",
                         "-XX:-InlineTypeReturnedAsFields"
            ),
            new Scenario(2,
                         "-XX:+IgnoreUnrecognizedVMOptions",
                         "-XX:-UseACmpProfile",
                         "-XX:-UseCompressedOops",
                         "-XX:FlatArrayElementMaxOops=0",
                         "-XX:FlatArrayElementMaxSize=0",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:+InlineTypeReturnedAsFields",
                         "-XX:+StressInlineTypeReturnedAsFields"
            ),
            new Scenario(3,
                         "-XX:+IgnoreUnrecognizedVMOptions",
                         "-DVerifyIR=false",
                         "-XX:+AlwaysIncrementalInline",
                         "-XX:FlatArrayElementMaxOops=0",
                         "-XX:FlatArrayElementMaxSize=0",
                         "-XX:InlineFieldMaxFlatSize=0",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:+InlineTypeReturnedAsFields"
            ),
            new Scenario(4,
                         "-XX:+IgnoreUnrecognizedVMOptions",
                         "-DVerifyIR=false",
                         "-XX:FlatArrayElementMaxOops=-1",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:InlineFieldMaxFlatSize=0",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:-InlineTypeReturnedAsFields",
                         "-XX:-ReduceInitialCardMarks"
            ),
            new Scenario(5,
                         "-XX:+IgnoreUnrecognizedVMOptions",
                         "-XX:-UseACmpProfile",
                         "-XX:+AlwaysIncrementalInline",
                         "-XX:FlatArrayElementMaxOops=5",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:-InlineTypePassFieldsAsArgs",
                         "-XX:-InlineTypeReturnedAsFields"
            )
    };

    public static TestFramework getFramework() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return new TestFramework(walker.getCallerClass()).setDefaultWarmup(251);
    }

    static class IRNode {
        // Regular expressions used to match nodes in the PrintIdeal output
        protected static final String START = "(\\d+ (.*";
        protected static final String MID = ".*)+ ===.*";
        protected static final String END = ")";
        // Generic allocation
        protected static final String ALLOC_G  = "(.*call,static.*wrapper for: _new_instance_Java" + END;
        protected static final String ALLOCA_G = "(.*call,static.*wrapper for: _new_array_Java" + END;
        // Inline type allocation
        protected static final String MYVALUE_ARRAY_KLASS = "\\[(precise )?compiler/valhalla/inlinetypes/MyValue";
        protected static final String ALLOC  = "(.*precise compiler/valhalla/inlinetypes/MyValue.*\\R(.*(?i:mov|xorl|nop|spill).*\\R)*.*_new_instance_Java" + END;
        protected static final String ALLOCA = "(.*" + MYVALUE_ARRAY_KLASS + ".*\\R(.*(?i:mov|xorl|nop|spill).*\\R)*.*_new_array_Java" + END;
        protected static final String LOAD   = START + "Load(B|C|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/.*" + END;
        protected static final String LOADK  = START + "LoadK" + MID + END;
        protected static final String STORE  = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/.*" + END;
        protected static final String LOOP   = START + "Loop" + MID + "" + END;
        protected static final String COUNTEDLOOP = START + "CountedLoop\\b" + MID + "" + END;
        protected static final String COUNTEDLOOP_MAIN = START + "CountedLoop\\b" + MID + "main" + END;
        protected static final String TRAP   = START + "CallStaticJava" + MID + "uncommon_trap.*(unstable_if|predicate)" + END;
        protected static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
        protected static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
        protected static final String CALL = START + "CallStaticJava" + MID + END;
        protected static final String CALL_LEAF = "(CALL, runtime leaf|call_leaf,runtime)";
        protected static final String CALL_LEAF_NOFP = "(CALL, runtime leaf nofp|call_leaf_nofp,runtime)";
        protected static final String STORE_INLINE_FIELDS = START + "CallStaticJava" + MID + "store_inline_type_fields" + END;
        protected static final String SCOBJ = "(.*# ScObj.*" + END;
        protected static final String LOAD_UNKNOWN_INLINE = START + "CallStaticJava" + MID + "_load_unknown_inline" + END;
        protected static final String STORE_UNKNOWN_INLINE = "(.*" + CALL_LEAF + ".*store_unknown_inline.*" + END;
        protected static final String INLINE_ARRAY_NULL_GUARD = "(.*call,static.*wrapper for: uncommon_trap.*reason='null_check' action='none'.*" + END;
        protected static final String INTRINSIC_SLOW_PATH = "(.*call,static.*wrapper for: uncommon_trap.*reason='intrinsic_or_type_checked_inlining'.*" + END;
        protected static final String CLONE_INTRINSIC_SLOW_PATH = "(.*call,static.*java.lang.Object::clone.*" + END;
        protected static final String CLASS_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*class_check" + END;
        protected static final String NULL_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_check" + END;
        protected static final String NULL_ASSERT_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_assert" + END;
        protected static final String RANGE_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*range_check" + END;
        protected static final String UNHANDLED_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*unhandled" + END;
        protected static final String PREDICATE_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*predicate" + END;
        protected static final String MEMBAR = START + "MemBar" + MID + END;
        protected static final String CHECKCAST_ARRAY = "(((?i:cmp|CLFI|CLR).*" + MYVALUE_ARRAY_KLASS + ".*:|.*(?i:mov|or).*" + MYVALUE_ARRAY_KLASS + ".*:.*\\R.*(cmp|CMP|CLR))" + END;
        protected static final String CHECKCAST_ARRAYCOPY = "(.*" + CALL_LEAF_NOFP + ".*checkcast_arraycopy.*" + END;
        protected static final String JLONG_ARRAYCOPY = "(.*" + CALL_LEAF_NOFP + ".*jlong_disjoint_arraycopy.*" + END;
        protected static final String FIELD_ACCESS = "(.*Field: *" + END;
        protected static final String SUBSTITUTABILITY_TEST = START + "CallStaticJava" + MID + "java.lang.runtime.PrimitiveObjectMethods::isSubstitutable" + END;
        protected static final String CMPP = START + "(CmpP|CmpN)" + MID + "" + END;
    }
}
