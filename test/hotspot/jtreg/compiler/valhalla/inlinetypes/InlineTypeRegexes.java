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

package compiler.valhalla.inlinetypes;

public class InlineTypeRegexes {
    // Regular expressions used to match nodes in the PrintIdeal output
    private static final String START = "(\\d+ (.*";
    private static final String MID = ".*)+ ===.*";
    private static final String END = ")";
    // Generic allocation
    public static final String ALLOC_G = "(.*call,static.*wrapper for: _new_instance_Java" + END;
    public static final String ALLOCA_G = "(.*call,static.*wrapper for: _new_array_Java" + END;
    // Inline type allocation
    public static final String MYVALUE_ARRAY_KLASS = "\\[(precise )?compiler/valhalla/inlinetypes/MyValue";
    public static final String ALLOC = "(.*precise compiler/valhalla/inlinetypes/MyValue.*\\R(.*(?i:mov|xorl|nop|spill).*\\R)*.*_new_instance_Java" + END;
    public static final String ALLOCA = "(.*" + MYVALUE_ARRAY_KLASS + ".*\\R(.*(?i:mov|xorl|nop|spill).*\\R)*.*_new_array_Java" + END;
    public static final String LOAD = START + "Load(B|C|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/.*" + END;
    public static final String LOADK = START + "LoadK" + MID + END;
    public static final String STORE = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/.*" + END;
    public static final String LOOP = START + "Loop" + MID + "" + END;
    public static final String COUNTEDLOOP = START + "CountedLoop\\b" + MID + "" + END;
    public static final String COUNTEDLOOP_MAIN = START + "CountedLoop\\b" + MID + "main" + END;
    public static final String TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*(unstable_if|predicate)" + END;
    public static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
    public static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
    public static final String CALL = START + "CallStaticJava" + MID + END;
    public static final String CALL_LEAF = "(CALL, runtime leaf|call_leaf,runtime)";
    public static final String CALL_LEAF_NOFP = "(CALL, runtime leaf nofp|call_leaf_nofp,runtime)";
    protected static final String CALL_UNSAFE = START + "CallStaticJava" + MID + "# Static  jdk.internal.misc.Unsafe::" + END;
    public static final String STORE_INLINE_FIELDS = START + "CallStaticJava" + MID + "store_inline_type_fields" + END;
    public static final String SCOBJ = "(.*# ScObj.*" + END;
    public static final String LOAD_UNKNOWN_INLINE = START + "CallStaticJava" + MID + "_load_unknown_inline" + END;
    public static final String STORE_UNKNOWN_INLINE = "(.*" + CALL_LEAF + ".*store_unknown_inline.*" + END;
    public static final String INLINE_ARRAY_NULL_GUARD = "(.*call,static.*wrapper for: uncommon_trap.*reason='null_check' action='none'.*" + END;
    public static final String INTRINSIC_SLOW_PATH = "(.*call,static.*wrapper for: uncommon_trap.*reason='intrinsic_or_type_checked_inlining'.*" + END;
    public static final String CLONE_INTRINSIC_SLOW_PATH = "(.*call,static.*java.lang.Object::clone.*" + END;
    public static final String CLASS_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*class_check" + END;
    public static final String NULL_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_check" + END;
    public static final String NULL_ASSERT_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_assert" + END;
    public static final String RANGE_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*range_check" + END;
    public static final String UNHANDLED_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*unhandled" + END;
    public static final String PREDICATE_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*predicate" + END;
    public static final String MEMBAR = START + "MemBar" + MID + END;
    public static final String CHECKCAST_ARRAY = "(((?i:cmp|CLFI|CLR).*" + MYVALUE_ARRAY_KLASS + ".*:|.*(?i:mov|or).*" + MYVALUE_ARRAY_KLASS + ".*:.*\\R.*(cmp|CMP|CLR))" + END;
    public static final String CHECKCAST_ARRAYCOPY = "(.*" + CALL_LEAF_NOFP + ".*checkcast_arraycopy.*" + END;
    public static final String JLONG_ARRAYCOPY = "(.*" + CALL_LEAF_NOFP + ".*jlong_disjoint_arraycopy.*" + END;
    public static final String FIELD_ACCESS = "(.*Field: *" + END;
    public static final String SUBSTITUTABILITY_TEST = START + "CallStaticJava" + MID + "java.lang.runtime.ValueObjectMethods::isSubstitutable" + END;
    public static final String CMPP = START + "(CmpP|CmpN)" + MID + "" + END;
}
