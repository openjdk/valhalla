/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.value.ValueClass;

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary We hit "graph should be schedulable" in LCM because of wrongly added precedence edges because some fields aren't found to be strict final.
 *          This happens because the type of the object we load from is lost during can_see_stored_value because the initial value of null-free newArray
 *          is not behind a checkcast.
 * @library /test/lib /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @enablePreview
 * @modules java.base/jdk.internal.value
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -Xbatch
 *                   -XX:-UseNullableAtomicValueFlattening -XX:-UseNullFreeAtomicValueFlattening -XX:+UseNullFreeNonAtomicValueFlattening
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:-TieredCompilation -XX:-DoEscapeAnalysis -XX:+AlwaysIncrementalInline
 *                   ${test.main.class}
 */

public class GraphShouldBeSchedulable {
    static value class TwoBytes {
        byte b1;
        byte b2;

        public TwoBytes(byte b1, byte b2) {
            this.b1 = b1;
            this.b2 = b2;
        }

        static final TwoBytes DEFAULT = new TwoBytes((byte)0, (byte)0);
    }

    static final TwoBytes CANARY1 = new TwoBytes((byte)42, (byte)42);
    static Object initVal1 = CANARY1;

    public static void main(String[] args) {
        for (int i = -50_000; i < 50_000; ++i) {
            TwoBytes val1 = new TwoBytes((byte)i, (byte)(i + 1));
            TwoBytes[] nullFreeAtomicArray1 = (TwoBytes[])ValueClass.newNullRestrictedAtomicArray(TwoBytes.class, 3, TwoBytes.DEFAULT);
            nullFreeAtomicArray1[1] = val1;
            TwoBytes[] nullFreeAtomicArray2 = (TwoBytes[])ValueClass.newNullRestrictedAtomicArray(TwoBytes.class, 3, initVal1);
            Asserts.assertEquals(ValueClass.isFlatArray(nullFreeAtomicArray2), false);
            Asserts.assertEquals(nullFreeAtomicArray2[1], CANARY1);
        }
    }
}

