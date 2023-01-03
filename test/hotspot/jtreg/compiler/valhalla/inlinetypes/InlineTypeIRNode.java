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

import compiler.lib.ir_framework.IRNode;

public class InlineTypeIRNode {
    private static final String PREFIX = "_#";
    private static final String POSTFIX = "#I_";
    public static final String ALLOC_G = PREFIX + "ALLOC_G" + POSTFIX;
    static {
        IRNode.optoOnly(ALLOC_G, InlineTypeRegexes.ALLOC_G);
    }

    public static final String ALLOCA_G = PREFIX + "ALLOCA_G" + POSTFIX;
    static {
        IRNode.optoOnly(ALLOCA_G, InlineTypeRegexes.ALLOCA_G);
    }

    public static final String MYVALUE_ARRAY_KLASS = PREFIX + "MYVALUE_ARRAY_KLASS" + POSTFIX;    static {
        IRNode.optoOnly(MYVALUE_ARRAY_KLASS, InlineTypeRegexes.MYVALUE_ARRAY_KLASS);
    }

    public static final String ALLOC = PREFIX + "ALLOC" + POSTFIX;
    static {
        IRNode.optoOnly(ALLOC, InlineTypeRegexes.ALLOC);
    }

    public static final String ALLOCA = PREFIX + "ALLOCA" + POSTFIX;
    static {
        IRNode.optoOnly(ALLOCA, InlineTypeRegexes.ALLOCA);
    }

    public static final String LOAD = PREFIX + "LOAD" + POSTFIX;
    static {
        IRNode.beforeMatching(LOAD, InlineTypeRegexes.LOAD);
    }

    public static final String LOADK = PREFIX + "LOADK" + POSTFIX;
    static {
        IRNode.beforeMatching(LOADK, InlineTypeRegexes.LOADK);
    }

    public static final String STORE = PREFIX + "STORE" + POSTFIX;
    static {
        IRNode.beforeMatching(STORE, InlineTypeRegexes.STORE);
    }

    public static final String LOOP = PREFIX + "LOOP" + POSTFIX;
    static {
        IRNode.beforeMatching(LOOP, InlineTypeRegexes.LOOP);
    }

    public static final String COUNTEDLOOP = PREFIX + "COUNTEDLOOP" + POSTFIX;
    static {
        IRNode.beforeMatching(COUNTEDLOOP, InlineTypeRegexes.COUNTEDLOOP);
    }

    public static final String COUNTEDLOOP_MAIN = PREFIX + "COUNTEDLOOP_MAIN" + POSTFIX;
    static {
        IRNode.beforeMatching(COUNTEDLOOP_MAIN, InlineTypeRegexes.COUNTEDLOOP_MAIN);
    }

    public static final String TRAP = PREFIX + "TRAP" + POSTFIX;
    static {
        IRNode.beforeMatching(TRAP, InlineTypeRegexes.TRAP);
    }

    public static final String LINKTOSTATIC = PREFIX + "LINKTOSTATIC" + POSTFIX;
    static {
        IRNode.beforeMatching(LINKTOSTATIC, InlineTypeRegexes.LINKTOSTATIC);
    }

    public static final String NPE = PREFIX + "NPE" + POSTFIX;
    static {
        IRNode.beforeMatching(NPE, InlineTypeRegexes.NPE);
    }

    public static final String CALL = PREFIX + "CALL" + POSTFIX;
    static {
        IRNode.beforeMatching(CALL, InlineTypeRegexes.CALL);
    }

    public static final String CALL_LEAF = PREFIX + "CALL_LEAF" + POSTFIX;
    static {
        IRNode.optoOnly(CALL_LEAF, InlineTypeRegexes.CALL_LEAF);
    }

    public static final String CALL_LEAF_NOFP = PREFIX + "CALL_LEAF_NOFP" + POSTFIX;
    static {
        IRNode.optoOnly(CALL_LEAF_NOFP, InlineTypeRegexes.CALL_LEAF_NOFP);
    }

    public static final String CALL_UNSAFE = PREFIX + "CALL_UNSAFE" + POSTFIX;
    static {
        IRNode.beforeMatching(CALL_UNSAFE, InlineTypeRegexes.CALL_UNSAFE);
    }

    public static final String STORE_INLINE_FIELDS = PREFIX + "STORE_INLINE_FIELDS" + POSTFIX;
    static {
        IRNode.beforeMatching(STORE_INLINE_FIELDS, InlineTypeRegexes.STORE_INLINE_FIELDS);
    }

    public static final String SCOBJ = PREFIX + "SCOBJ" + POSTFIX;
    static {
        IRNode.optoOnly(SCOBJ, InlineTypeRegexes.SCOBJ);
    }

    public static final String LOAD_UNKNOWN_INLINE = PREFIX + "LOAD_UNKNOWN_INLINE" + POSTFIX;
    static {
        IRNode.beforeMatching(LOAD_UNKNOWN_INLINE, InlineTypeRegexes.LOAD_UNKNOWN_INLINE);
    }

    public static final String STORE_UNKNOWN_INLINE = PREFIX + "STORE_UNKNOWN_INLINE" + POSTFIX;
    static {
        IRNode.optoOnly(STORE_UNKNOWN_INLINE, InlineTypeRegexes.STORE_UNKNOWN_INLINE);
    }

    public static final String INLINE_ARRAY_NULL_GUARD = PREFIX + "INLINE_ARRAY_NULL_GUARD" + POSTFIX;
    static {
        IRNode.optoOnly(INLINE_ARRAY_NULL_GUARD, InlineTypeRegexes.INLINE_ARRAY_NULL_GUARD);
    }

    public static final String INTRINSIC_SLOW_PATH = PREFIX + "INTRINSIC_SLOW_PATH" + POSTFIX;
    static {
        IRNode.optoOnly(INTRINSIC_SLOW_PATH, InlineTypeRegexes.INTRINSIC_SLOW_PATH);
    }

    public static final String CLONE_INTRINSIC_SLOW_PATH = PREFIX + "CLONE_INTRINSIC_SLOW_PATH" + POSTFIX;
    static {
        IRNode.optoOnly(CLONE_INTRINSIC_SLOW_PATH, InlineTypeRegexes.CLONE_INTRINSIC_SLOW_PATH);
    }

    public static final String CLASS_CHECK_TRAP = PREFIX + "CLASS_CHECK_TRAP" + POSTFIX;
    static {
        IRNode.beforeMatching(CLASS_CHECK_TRAP, InlineTypeRegexes.CLASS_CHECK_TRAP);
    }

    public static final String NULL_CHECK_TRAP = PREFIX + "NULL_CHECK_TRAP" + POSTFIX;
    static {
        IRNode.beforeMatching(NULL_CHECK_TRAP, InlineTypeRegexes.NULL_CHECK_TRAP);
    }

    public static final String NULL_ASSERT_TRAP = PREFIX + "NULL_ASSERT_TRAP" + POSTFIX;
    static {
        IRNode.beforeMatching(NULL_ASSERT_TRAP, InlineTypeRegexes.NULL_ASSERT_TRAP);
    }

    public static final String RANGE_CHECK_TRAP = PREFIX + "RANGE_CHECK_TRAP" + POSTFIX;
    static {
        IRNode.beforeMatching(RANGE_CHECK_TRAP, InlineTypeRegexes.RANGE_CHECK_TRAP);
    }

    public static final String UNHANDLED_TRAP = PREFIX + "UNHANDLED_TRAP" + POSTFIX;
    static {
        IRNode.beforeMatching(UNHANDLED_TRAP, InlineTypeRegexes.UNHANDLED_TRAP);
    }

    public static final String PREDICATE_TRAP = PREFIX + "PREDICATE_TRAP" + POSTFIX;
    static {
        IRNode.beforeMatching(PREDICATE_TRAP, InlineTypeRegexes.PREDICATE_TRAP);
    }

    public static final String MEMBAR = PREFIX + "MEMBAR" + POSTFIX;
    static {
        IRNode.beforeMatching(MEMBAR, InlineTypeRegexes.MEMBAR);
    }

    public static final String CHECKCAST_ARRAY = PREFIX + "CHECKCAST_ARRAY" + POSTFIX;
    static {
        IRNode.optoOnly(CHECKCAST_ARRAY, InlineTypeRegexes.CHECKCAST_ARRAY);
    }

    public static final String CHECKCAST_ARRAYCOPY = PREFIX + "CHECKCAST_ARRAYCOPY" + POSTFIX;
    static {
        IRNode.optoOnly(CHECKCAST_ARRAYCOPY, InlineTypeRegexes.CHECKCAST_ARRAYCOPY);
    }

    public static final String JLONG_ARRAYCOPY = PREFIX + "JLONG_ARRAYCOPY" + POSTFIX;
    static {
        IRNode.optoOnly(JLONG_ARRAYCOPY, InlineTypeRegexes.JLONG_ARRAYCOPY);
    }

    public static final String FIELD_ACCESS = PREFIX + "FIELD_ACCESS" + POSTFIX;
    static {
        IRNode.optoOnly(FIELD_ACCESS, InlineTypeRegexes.FIELD_ACCESS);
    }

    public static final String SUBSTITUTABILITY_TEST = PREFIX + "SUBSTITUTABILITY_TEST" + POSTFIX;
    static {
        IRNode.beforeMatching(SUBSTITUTABILITY_TEST, InlineTypeRegexes.SUBSTITUTABILITY_TEST);
    }

    public static final String CMPP = PREFIX + "CMPP" + POSTFIX;
    static {
        IRNode.beforeMatching(CMPP, InlineTypeRegexes.CMPP);
    }

    // Dummy method to call to force the static initializer blocks to be run before starting the IR framework.
    public static void forceStaticInitialization() {}
}
