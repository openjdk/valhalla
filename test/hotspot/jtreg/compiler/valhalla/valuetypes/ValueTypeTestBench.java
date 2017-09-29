/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

// TODO add bugid and summary

/*
 * @test
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          java.base/jdk.internal.misc:+open
 *          jdk.incubator.mvt
 * @compile -XDenableValueTypes ValueCapableClass1.java ValueCapableClass2.java ValueTypeTestBench.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run main ClassFileInstaller jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-TieredCompilation -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:+EnableMVT -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -XX:ValueTypesBufferMaxMemory=0
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.ValueTypeTestBench
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-TieredCompilation -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:+EnableMVT -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -XX:ValueTypesBufferMaxMemory=0
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.ValueTypeTestBench
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-TieredCompilation -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:+EnableMVT -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -XX:ValueTypesBufferMaxMemory=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.ValueTypeTestBench
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-TieredCompilation -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:+EnableMVT -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -XX:ValueTypesBufferMaxMemory=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.ValueTypeTestBench
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-TieredCompilation
 *                   -XX:+EnableValhalla -XX:+EnableMVT -XX:+ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -XX:ValueTypesBufferMaxMemory=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.ValueTypeTestBench
 */

// TODO remove -XX:ValueTypesBufferMaxMemory=0 when interpreter buffering is fixed

package compiler.valhalla.valuetypes;

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.experimental.value.MethodHandleBuilder;
import jdk.incubator.mvt.ValueType;
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

// Test value types
__ByValue final class MyValue1 {
    static int s;
    static final long sf = ValueTypeTestBench.rL;
    final int x;
    final long y;
    final short z;
    final Integer o;
    final int[] oa;
    final MyValue2 v1;
    final MyValue2 v2;
    static final MyValue2 v3 = MyValue2.createWithFieldsInline(ValueTypeTestBench.rI, true);
    final int c;

    private MyValue1() {
        s = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.o = null;
        this.oa = null;
        this.v1 = MyValue2.createDefaultInline();
        this.v2 = MyValue2.createDefaultInline();
        this.c = 0;
    }

    @DontInline
    __ValueFactory static MyValue1 createDefaultDontInline() {
        return createDefaultInline();
    }

    @ForceInline
    __ValueFactory static MyValue1 createDefaultInline() {
        return __MakeDefault MyValue1();
    }

    @DontInline
    static MyValue1 createWithFieldsDontInline(int x, long y) {
        return createWithFieldsInline(x, y);
    }

    @ForceInline
    static MyValue1 createWithFieldsInline(int x, long y) {
        MyValue1 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, y);
        v = setZ(v, (short)x);
        v = setO(v, new Integer(x));
        int[] oa = {x};
        v = setOA(v, oa);
        v = setV1(v, MyValue2.createWithFieldsInline(x, true));
        v = setV2(v, MyValue2.createWithFieldsInline(x, false));
        v = setC(v, ValueTypeTestBench.rI);
        return v;
    }

    // Hash only primitive and value type fields to avoid NullPointerException
    @ForceInline
    public long hashPrimitive() {
        return s + sf + x + y + z + c + v1.hash() + v2.hash() + v3.hash();
    }

    @ForceInline
    public long hash() {
        return hashPrimitive() + o + oa[0];
    }

    @DontCompile
    public long hashInterpreted() {
        return s + sf + x + y + z + o + oa[0] + c + v1.hashInterpreted() + v2.hashInterpreted() + v3.hashInterpreted();
    }

    @ForceInline
    public void print() {
        System.out.print("s=" + s + ", sf=" + sf + ", x=" + x + ", y=" + y + ", z=" + z + ", o=" + (o != null ? (Integer)o : "NULL") + ", oa=" + oa[0] + ", v1[");
        v1.print();
        System.out.print("], v2[");
        v2.print();
        System.out.print("], v3[");
        v3.print();
        System.out.print("], c=" + c);
    }

    @ForceInline
    __ValueFactory static MyValue1 setX(MyValue1 v, int x) {
        v.x = x;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue1 setY(MyValue1 v, long y) {
        v.y = y;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue1 setZ(MyValue1 v, short z) {
        v.z = z;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue1 setO(MyValue1 v, Integer o) {
        v.o = o;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue1 setOA(MyValue1 v, int[] oa) {
        v.oa = oa;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue1 setC(MyValue1 v, int c) {
        v.c = c;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue1 setV1(MyValue1 v, MyValue2 v1) {
        v.v1 = v1;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue1 setV2(MyValue1 v, MyValue2 v2) {
        v.v2 = v2;
        return v;
    }
}

__ByValue final class MyValue2Inline {
    final boolean b;
    final long c;

    private MyValue2Inline() {
        this.b = false;
        this.c = 0;
    }

    @ForceInline
    __ValueFactory static MyValue2Inline setB(MyValue2Inline v, boolean b) {
        v.b = b;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue2Inline setC(MyValue2Inline v, long c) {
        v.c = c;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue2Inline createDefault() {
        return __MakeDefault MyValue2Inline();
    }

    @ForceInline
    public static MyValue2Inline createWithFieldsInline(boolean b, long c) {
        MyValue2Inline v = MyValue2Inline.createDefault();
        v = MyValue2Inline.setB(v, b);
        v = MyValue2Inline.setC(v, c);
        return v;
    }
}

__ByValue final class MyValue2 {
    final int x;
    final byte y;
    final MyValue2Inline v1;

    private MyValue2() {
        this.x = 0;
        this.y = 0;
        this.v1 = MyValue2Inline.createDefault();
    }

    @ForceInline
    __ValueFactory public static MyValue2 createDefaultInline() {
        return __MakeDefault MyValue2();
    }

    @ForceInline
    public static MyValue2 createWithFieldsInline(int x, boolean b) {
        MyValue2 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, (byte)x);
        v = setV1(v, MyValue2Inline.createWithFieldsInline(b, ValueTypeTestBench.rL));
        return v;
    }

    @ForceInline
    public long hash() {
        return x + y + (v1.b ? 0 : 1) + v1.c;
    }

    @DontInline
    public long hashInterpreted() {
        return x + y + (v1.b ? 0 : 1) + v1.c;
    }

    @ForceInline
    public void print() {
        System.out.print("x=" + x + ", y=" + y + ", b=" + v1.b + ", c=" + v1.c);
    }

    @ForceInline
    __ValueFactory static MyValue2 setX(MyValue2 v, int x) {
        v.x = x;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue2 setY(MyValue2 v, byte y) {
        v.y = y;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue2 setV1(MyValue2 v, MyValue2Inline v1) {
        v.v1 = v1;
        return v;
    }
}

__ByValue final class MyValue3Inline {
    final float f7;
    final double f8;

    private MyValue3Inline() {
        this.f7 = 0;
        this.f8 = 0;
    }

    @ForceInline
    __ValueFactory static MyValue3Inline setF7(MyValue3Inline v, float f7) {
        v.f7 = f7;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3Inline setF8(MyValue3Inline v, double f8) {
        v.f8 = f8;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue3Inline createDefault() {
        return __MakeDefault MyValue3Inline();
    }

    @ForceInline
    public static MyValue3Inline createWithFieldsInline(float f7, double f8) {
        MyValue3Inline v = createDefault();
        v = setF7(v, f7);
        v = setF8(v, f8);
        return v;
    }
}

// Value type definition to stress test return of a value in registers
// (uses all registers of calling convention on x86_64)
__ByValue final class MyValue3 {
    final char c;
    final byte bb;
    final short s;
    final int i;
    final long l;
    final Object o;
    final float f1;
    final double f2;
    final float f3;
    final double f4;
    final float f5;
    final double f6;
    final MyValue3Inline v1;

    private MyValue3() {
        this.c = 0;
        this.bb = 0;
        this.s = 0;
        this.i = 0;
        this.l = 0;
        this.o = null;
        this.f1 = 0;
        this.f2 = 0;
        this.f3 = 0;
        this.f4 = 0;
        this.f5 = 0;
        this.f6 = 0;
        this.v1 = MyValue3Inline.createDefault();
    }

    @ForceInline
    __ValueFactory static MyValue3 setC(MyValue3 v, char c) {
        v.c = c;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setBB(MyValue3 v, byte bb) {
        v.bb = bb;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setS(MyValue3 v, short s) {
        v.s = s;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setI(MyValue3 v, int i) {
        v.i = i;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setL(MyValue3 v, long l) {
        v.l = l;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setO(MyValue3 v, Object o) {
        v.o = o;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF1(MyValue3 v, float f1) {
        v.f1 = f1;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF2(MyValue3 v, double f2) {
        v.f2 = f2;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF3(MyValue3 v, float f3) {
        v.f3 = f3;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF4(MyValue3 v, double f4) {
        v.f4 = f4;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF5(MyValue3 v, float f5) {
        v.f5 = f5;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF6(MyValue3 v, double f6) {
        v.f6 = f6;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setV1(MyValue3 v, MyValue3Inline v1) {
        v.v1 = v1;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue3 createDefault() {
        return __MakeDefault MyValue3();
    }

    @ForceInline
    public static MyValue3 create() {
        java.util.Random r = Utils.getRandomInstance();
        MyValue3 v = createDefault();
        v = setC(v, (char)r.nextInt());
        v = setBB(v, (byte)r.nextInt());
        v = setS(v, (short)r.nextInt());
        v = setI(v, r.nextInt());
        v = setL(v, r.nextLong());
        v = setO(v, new Object());
        v = setF1(v, r.nextFloat());
        v = setF2(v, r.nextDouble());
        v = setF3(v, r.nextFloat());
        v = setF4(v, r.nextDouble());
        v = setF5(v, r.nextFloat());
        v = setF6(v, r.nextDouble());
        v = setV1(v, MyValue3Inline.createWithFieldsInline(r.nextFloat(), r.nextDouble()));
        return v;
    }

    @DontInline
    public static MyValue3 createDontInline() {
        return create();
    }

    @ForceInline
    public static MyValue3 copy(MyValue3 other) {
        MyValue3 v = createDefault();
        v = setC(v, other.c);
        v = setBB(v, other.bb);
        v = setS(v, other.s);
        v = setI(v, other.i);
        v = setL(v, other.l);
        v = setO(v, other.o);
        v = setF1(v, other.f1);
        v = setF2(v, other.f2);
        v = setF3(v, other.f3);
        v = setF4(v, other.f4);
        v = setF5(v, other.f5);
        v = setF6(v, other.f6);
        v = setV1(v, other.v1);
        return v;
    }

    @DontInline
    public void verify(MyValue3 other) {
        Asserts.assertEQ(c, other.c);
        Asserts.assertEQ(bb, other.bb);
        Asserts.assertEQ(s, other.s);
        Asserts.assertEQ(i, other.i);
        Asserts.assertEQ(l, other.l);
        Asserts.assertEQ(o, other.o);
        Asserts.assertEQ(f1, other.f1);
        Asserts.assertEQ(f2, other.f2);
        Asserts.assertEQ(f3, other.f3);
        Asserts.assertEQ(f4, other.f4);
        Asserts.assertEQ(f5, other.f5);
        Asserts.assertEQ(f6, other.f6);
        Asserts.assertEQ(v1.f7, other.v1.f7);
        Asserts.assertEQ(v1.f8, other.v1.f8);
    }
}

// Value type definition with too many fields to return in registers
__ByValue final class MyValue4 {
    final MyValue3 v1;
    final MyValue3 v2;

    private MyValue4() {
        this.v1 = MyValue3.createDefault();
        this.v2 = MyValue3.createDefault();
    }

    @ForceInline
    __ValueFactory static MyValue4 setV1(MyValue4 v, MyValue3 v1) {
        v.v1 = v1;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue4 setV2(MyValue4 v, MyValue3 v2) {
        v.v2 = v2;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue4 createDefault() {
        return __MakeDefault MyValue4();
    }

    @ForceInline
    public static MyValue4 create() {
        MyValue4 v = createDefault();
        MyValue3 v1 = MyValue3.create();
        v = setV1(v, v1);
        MyValue3 v2 = MyValue3.create();
        v = setV2(v, v2);
        return v;
    }

    public void verify(MyValue4 other) {
        v1.verify(other.v1);
        v2.verify(other.v2);
    }
}


public class ValueTypeTestBench {
    // Print ideal graph after execution of each test
    private static final boolean PRINT_GRAPH = true;

    // Random test values
    public static final int  rI = Utils.getRandomInstance().nextInt() % 1000;
    public static final long rL = Utils.getRandomInstance().nextLong() % 1000;

    public ValueTypeTestBench() {
        val3 = MyValue1.createWithFieldsInline(rI, rL);
    }

    // MethodHandles and value-capable class instance needed for testing vbox/vunbox
    private static final MethodHandle vccUnboxLoadLongMH = generateVCCUnboxLoadLongMH();
    private static final MethodHandle vccUnboxLoadIntMH = generateVCCUnboxLoadIntMH();
    private static final MethodHandle vccUnboxBoxMH = generateVCCUnboxBoxMH();
    private static final MethodHandle vccUnboxBoxLoadIntMH = generateVCCUnboxBoxLoadIntMH();
    private static final MethodHandle nullvccUnboxLoadLongMH = generateNullVCCUnboxLoadLongMH();
    private static final MethodHandle objectUnboxLoadLongMH = generateObjectUnboxLoadLongMH();
    private static final MethodHandle objectBoxMH = generateObjectBoxMH();
    private static final MethodHandle checkedvccUnboxLoadLongMH = generateCheckedVCCUnboxLoadLongMH();
    private static final MethodHandle vastoreMH = generateVastore();
    private static final MethodHandle invalidVastoreMH = generateInvalidVastore();

    private static final ValueCapableClass1 vcc = ValueCapableClass1.create(rL, rI, (short)rI, (short)rI);
    private static final ValueCapableClass2 vcc2 = ValueCapableClass2.create(rL + 1);

    // ========== Helper methods ==========

    public long hash() {
        return hash(rI, rL);
    }

    public long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    // ========== Test definitions ==========

    // Receive value type through call to interpreter
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test1() {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        long result = test1();
        Asserts.assertEQ(result, hash());
    }

    // Receive value type from interpreter via parameter
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test2(MyValue1 v) {
        return v.hash();
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        long result = test2(v);
        Asserts.assertEQ(result, hash());
    }

    // Return incoming value type without accessing fields
    @Test(valid = ValueTypePassFieldsAsArgsOn, match = {ALLOC, STORE}, matchCount = {1, 11}, failOn = LOAD + TRAP)
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = ALLOC + LOAD + STORE + TRAP)
    public MyValue1 test3(MyValue1 v) {
        return v;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1 v2 = test3(v1);
        Asserts.assertEQ(v1.x, v2.x);
        Asserts.assertEQ(v1.y, v2.y);
    }

    // Create a value type in compiled code and only use fields.
    // Allocation should go away because value type does not escape.
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public long test4() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        long result = test4();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to
    // an inlined compiled method via a call.
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public long test5() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return test5Inline(v);
    }

    @ForceInline
    public long test5Inline(MyValue1 v) {
        return v.hash();
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        long result = test5();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to
    // the interpreter via a call.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = LOAD + TRAP + ALLOC)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public long test6() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        // Pass to interpreter
        return v.hashInterpreted();
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        long result = test6();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to
    // the interpreter by returning.
    @Test(match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public MyValue1 test7(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y);
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        MyValue1 v = test7(rI, rL);
        Asserts.assertEQ(v.hash(), hash());
    }

    // Merge value types created from two branches
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test8(boolean b) {
        MyValue1 v;
        if (b) {
            v = MyValue1.createWithFieldsInline(rI, rL);
        } else {
            v = MyValue1.createWithFieldsDontInline(rI + 1, rL + 1);
        }
        return v.hash();
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        Asserts.assertEQ(test8(true), hash());
        Asserts.assertEQ(test8(false), hash(rI + 1, rL + 1));
    }

    // Merge value types created from two branches
    @Test(valid = ValueTypePassFieldsAsArgsOn, match = {LOAD}, matchCount = {10}, failOn = TRAP + ALLOC + STORE)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC, STORE}, matchCount = {1, 3}, failOn = LOAD + TRAP)
    public MyValue1 test9(boolean b) {
        MyValue1 v;
        if (b) {
            // Value type is not allocated
            v = MyValue1.createWithFieldsInline(rI, rL);
        } else {
            // Value type is allocated by the callee
            v = MyValue1.createWithFieldsDontInline(rI + 1, rL + 1);
        }
        // Need to allocate value type if 'b' is true
        long sum = v.hashInterpreted();
        if (b) {
            v = MyValue1.createWithFieldsDontInline(rI, sum);
        } else {
            v = MyValue1.createWithFieldsDontInline(rI, sum + 1);
        }
        // Don't need to allocate value type because both branches allocate
        return v;
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        MyValue1 v = test9(true);
        Asserts.assertEQ(v.x, rI);
        Asserts.assertEQ(v.y, hash());
        v = test9(false);
        Asserts.assertEQ(v.x, rI);
        Asserts.assertEQ(v.y, hash(rI + 1, rL + 1) + 1);
    }

    // Merge value types created in a loop (not inlined)
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test10(int x, long y) {
        MyValue1 v = MyValue1.createWithFieldsDontInline(x, y);
        for (int i = 0; i < 10; ++i) {
            v = MyValue1.createWithFieldsDontInline(v.x + 1, v.y + 1);
        }
        return v.hash();
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        long result = test10(rI, rL);
        Asserts.assertEQ(result, hash(rI + 10, rL + 10));
    }

    // Merge value types created in a loop (inlined)
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public long test11(int x, long y) {
        MyValue1 v = MyValue1.createWithFieldsInline(x, y);
        for (int i = 0; i < 10; ++i) {
            v = MyValue1.createWithFieldsInline(v.x + 1, v.y + 1);
        }
        return v.hash();
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        long result = test11(rI, rL);
        Asserts.assertEQ(result, hash(rI + 10, rL + 10));
    }

    // Test loop with uncommon trap referencing a value type
    @Test(match = {SCOBJ}, matchCount = {-1 /* at least 1 */}, failOn = LOAD)
    public long test12(boolean b) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1[] va = new MyValue1[Math.abs(rI) % 10];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        long result = rL;
        for (int i = 0; i < 1000; ++i) {
            if (b) {
                result += v.x;
            } else {
                // Uncommon trap referencing v. We delegate allocation to the
                // interpreter by adding a SafePointScalarObjectNode.
                result = v.hashInterpreted();
                for (int j = 0; j < va.length; ++j) {
                    result += va[j].hash();
                }
            }
        }
        return result;
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        long result = test12(warmup);
        Asserts.assertEQ(result, warmup ? rL + (1000 * rI) : ((Math.abs(rI) % 10) + 1) * hash());
    }

    // Test loop with uncommon trap referencing a value type
    @Test
    public long test13(boolean b) {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1[] va = new MyValue1[Math.abs(rI) % 10];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }
        long result = rL;
        for (int i = 0; i < 1000; ++i) {
            if (b) {
                result += v.x;
            } else {
                // Uncommon trap referencing v. Should not allocate
                // but just pass the existing oop to the uncommon trap.
                result = v.hashInterpreted();
                for (int j = 0; j < va.length; ++j) {
                    result += va[j].hashInterpreted();
                }
            }
        }
        return result;
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        long result = test13(warmup);
        Asserts.assertEQ(result, warmup ? rL + (1000 * rI) : ((Math.abs(rI) % 10) + 1) * hash());
    }

    // Create a value type in a non-inlined method and then call a
    // non-inlined method on that value type.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = (ALLOC + STORE + TRAP), match = {LOAD}, matchCount = {10})
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = (ALLOC + LOAD + STORE + TRAP))
    public long test14() {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        return v.hashInterpreted();
    }

    @DontCompile
    public void test14_verifier(boolean b) {
        long result = test14();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in an inlined method and then call a
    // non-inlined method on that value type.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = (LOAD + TRAP + ALLOC))
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = (LOAD + TRAP), match = {ALLOC}, matchCount = {1})
    public long test15() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return v.hashInterpreted();
    }

    @DontCompile
    public void test15_verifier(boolean b) {
        long result = test15();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in a non-inlined method and then call an
    // inlined method on that value type.
    @Test(failOn = (ALLOC + STORE + TRAP))
    public long test16() {
        MyValue1 v = MyValue1.createWithFieldsDontInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test16_verifier(boolean b) {
        long result = test16();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in an inlined method and then call an
    // inlined method on that value type.
    @Test(failOn = (ALLOC + LOAD + STORE + TRAP))
    public long test17() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return v.hash();
    }

    @DontCompile
    public void test17_verifier(boolean b) {
        long result = test17();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to the
    // interpreter via a call. The value is live at the first call so
    // debug info should include a reference to all its fields.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public long test18() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        v.hashInterpreted();
        return v.hashInterpreted();
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        long result = test18();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type in compiled code and pass it to the
    // interpreter via a call. The value type is passed twice but
    // should only be allocated once.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {1}, failOn = LOAD + TRAP)
    public long test19() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        return sumValue(v, v);
    }

    @DontCompile
    public long sumValue(MyValue1 v, MyValue1 dummy) {
        return v.hash();
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        long result = test19();
        Asserts.assertEQ(result, hash());
    }

    // Create a value type (array) in compiled code and pass it to the
    // interpreter via a call. The value type is live at the uncommon
    // trap: verify that deoptimization causes the value type to be
    // correctly allocated.
    @Test(valid = ValueTypePassFieldsAsArgsOn, failOn = LOAD + ALLOC + STORE)
    @Test(valid = ValueTypePassFieldsAsArgsOff, match = {ALLOC}, matchCount = {2}, failOn = LOAD)
    public long test20(boolean deopt) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue2[] va = new MyValue2[3];
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get("ValueTypeTestBench::test20"));
        }
        return v.hashInterpreted() + va[0].hashInterpreted() +
                va[1].hashInterpreted() + va[2].hashInterpreted();
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
        MyValue2[] va = new MyValue2[42];
        long result = test20(!warmup);
        Asserts.assertEQ(result, hash() + va[0].hash() + va[1].hash() + va[2].hash());
    }

    // Value type fields in regular object
    MyValue1 val1;
    MyValue2 val2;
    final MyValue1 val3;
    static MyValue1 val4;
    static final MyValue1 val5 = MyValue1.createWithFieldsInline(rI, rL);

    // Test value type fields in objects
    @Test(match = {ALLOC}, matchCount = {1}, failOn = (TRAP))
    public long test21(int x, long y) {
        // Compute hash of value type fields
        long result = val1.hash() + val2.hash() + val3.hash() + val4.hash() + val5.hash();
        // Update fields
        val1 = MyValue1.createWithFieldsInline(x, y);
        val2 = MyValue2.createWithFieldsInline(x, true);
        val4 = MyValue1.createWithFieldsInline(x, y);
        return result;
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        // Check if hash computed by test18 is correct
        val1 = MyValue1.createWithFieldsInline(rI, rL);
        val2 = val1.v2;
        // val3 is initialized in the constructor
        val4 = val1;
        // val5 is initialized in the static initializer
        long hash = val1.hash() + val2.hash() + val3.hash() + val4.hash() + val5.hash();
        long result = test21(rI + 1, rL + 1);
        Asserts.assertEQ(result, hash);
        // Check if value type fields were updated
        Asserts.assertEQ(val1.hash(), hash(rI + 1, rL + 1));
        Asserts.assertEQ(val2.hash(), MyValue2.createWithFieldsInline(rI + 1, true).hash());
        Asserts.assertEQ(val4.hash(), hash(rI + 1, rL + 1));
    }

    // Test folding of constant value type fields
    @Test(failOn = ALLOC + LOAD + STORE + LOOP + TRAP)
    public long test22() {
        // This should be constant folded
        return val5.hash() + val5.v3.hash();
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        long result = test22();
        Asserts.assertEQ(result, val5.hash() + val5.v3.hash());
    }

    // Test OSR compilation
    @Test()
    @Slow
    public long test23() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1[] va = new MyValue1[Math.abs(rI) % 3];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        long result = 0;
        // Long loop to trigger OSR compilation
        for (int i = 0 ; i < 100_000; ++i) {
            // Reference local value type in interpreter state
            result = v.hash();
            for (int j = 0; j < va.length; ++j) {
                result += va[j].hash();
            }
        }
        return result;
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        long result = test23();
        Asserts.assertEQ(result, ((Math.abs(rI) % 3) + 1) * hash());
    }

    // Test interpreter to compiled code with various signatures
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test24(MyValue2 v) {
        return v.hash();
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test24(v);
        Asserts.assertEQ(result, v.hashInterpreted());
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test25(int i1, MyValue2 v, int i2) {
        return v.hash() + i1 - i2;
    }

    @DontCompile
    public void test25_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test25(rI, v, 2*rI);
        Asserts.assertEQ(result, v.hashInterpreted() - rI);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test26(long l1, MyValue2 v, long l2) {
        return v.hash() + l1 - l2;
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test26(rL, v, 2*rL);
        Asserts.assertEQ(result, v.hashInterpreted() - rL);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test27(int i, MyValue2 v, long l) {
        return v.hash() + i + l;
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test27(rI, v, rL);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test28(long l, MyValue2 v, int i) {
        return v.hash() + i + l;
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test28(rL, v, rI);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test29(long l, MyValue1 v1, int i, MyValue2 v2) {
        return v1.hash() + i + l + v2.hash();
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue2 v2 = MyValue2.createWithFieldsInline(rI, true);
        long result = test29(rL, v1, rI, v2);
        Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
    }

    // Test compiled code to interpreter with various signatures
    @DontCompile
    public long test30_interp(MyValue2 v) {
        return v.hash();
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test30(MyValue2 v) {
        return test30_interp(v);
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test30(v);
        Asserts.assertEQ(result, v.hashInterpreted());
    }

    @DontCompile
    public long test31_interp(int i1, MyValue2 v, int i2) {
        return v.hash() + i1 - i2;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test31(int i1, MyValue2 v, int i2) {
        return test31_interp(i1, v, i2);
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test31(rI, v, 2*rI);
        Asserts.assertEQ(result, v.hashInterpreted() - rI);
    }

    @DontCompile
    public long test32_interp(long l1, MyValue2 v, long l2) {
        return v.hash() + l1 - l2;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test32(long l1, MyValue2 v, long l2) {
        return test32_interp(l1, v, l2);
    }

    @DontCompile
    public void test32_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test32(rL, v, 2*rL);
        Asserts.assertEQ(result, v.hashInterpreted() - rL);
    }

    @DontCompile
    public long test33_interp(int i, MyValue2 v, long l) {
        return v.hash() + i + l;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test33(int i, MyValue2 v, long l) {
        return test33_interp(i, v, l);
    }

    @DontCompile
    public void test33_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test33(rI, v, rL);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @DontCompile
    public long test34_interp(long l, MyValue2 v, int i) {
        return v.hash() + i + l;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test34(long l, MyValue2 v, int i) {
        return test34_interp(l, v, i);
    }

    @DontCompile
    public void test34_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        long result = test34(rL, v, rI);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @DontCompile
    public long test35_interp(long l, MyValue1 v1, int i, MyValue2 v2) {
        return v1.hash() + i + l + v2.hash();
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test35(long l, MyValue1 v1, int i, MyValue2 v2) {
        return test35_interp(l, v1, i, v2);
    }

    @DontCompile
    public void test35_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue2 v2 = MyValue2.createWithFieldsInline(rI, true);
        long result = test35(rL, v1, rI, v2);
        Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
    }

    // test that debug info at a call is correct
    @DontCompile
    public long test36_interp(MyValue2 v, MyValue1[] va, boolean deopt) {
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get("ValueTypeTestBench::test36"));
        }
        return v.hash() + va[0].hash() + va[1].hash();
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test36(MyValue2 v, MyValue1[] va, boolean flag, long l) {
        return test36_interp(v, va, flag) + l;
    }

    @DontCompile
    public void test36_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        MyValue1[] va = new MyValue1[2];
        va[0] = MyValue1.createWithFieldsDontInline(rI, rL);
        va[1] = MyValue1.createWithFieldsDontInline(rI, rL);
        long result = test36(v, va, !warmup, rL);
        Asserts.assertEQ(result, v.hashInterpreted() + va[0].hash() + va[1].hash() + rL);
    }

    // Test vbox and vunbox
    @Test
    public long test37() throws Throwable {
        return (long)vccUnboxLoadLongMH.invokeExact(vcc);
    }

    @DontCompile
    public void test37_verifier(boolean warmup) {
        try {
            long result = test37();
            Asserts.assertEQ(vcc.t, result, "Field t of input and result must be equal.");
        } catch (Throwable t) {
            throw new RuntimeException("test 37 failed", t);
        }
    }

    // Generate a MethodHandle that obtains field t of the
    // derived value type
    private static MethodHandle generateVCCUnboxLoadLongMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "vccUnboxLoadLong",
                MethodType.methodType(long.class, ValueCapableClass1.class),
                CODE -> {
                    CODE.
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                    lreturn();
                }
                );
    }

    @Test
    public int test38() throws Throwable {
        return (int)vccUnboxLoadIntMH.invokeExact(vcc);
    }

    @DontCompile
    public void test38_verifier(boolean warmup) {
        try {
            int result = test38();
            Asserts.assertEQ(vcc.x, result, "Field x of input and result must be equal.");
        } catch (Throwable t) {
            throw new RuntimeException("test 38 failed", t);
        }
    }

    // Generate a MethodHandle that obtains field x of the
    // derived value type
    private static MethodHandle generateVCCUnboxLoadIntMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "vccUnboxLoadInt",
                MethodType.methodType(int.class, ValueCapableClass1.class),
                CODE -> {
                    CODE.
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "x", "I").
                    ireturn();
                }
                );
    }

    @Test
    public ValueCapableClass1 test39() throws Throwable {
        return (ValueCapableClass1)vccUnboxBoxMH.invokeExact(vcc);
    }

    @DontCompile
    public void test39_verifier(boolean warmup) {
        try {
            ValueCapableClass1 result = test39();
            Asserts.assertEQ(vcc.value(), result.value(), "Value of VCC and returned VCC must be equal");
        } catch (Throwable t) {
            throw new RuntimeException("test 39 failed", t);
        }
    }

    // Generate a MethodHandle that takes a value-capable class,
    // unboxes it, then boxes it again and returns it.
    private static MethodHandle generateVCCUnboxBoxMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "vccUnboxBox",
                MethodType.methodType(ValueCapableClass1.class, ValueCapableClass1.class),
                CODE -> {
                    CODE.
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    vbox(ValueCapableClass1.class).
                    areturn();
                }
                );
    }

    @Test
    public int test40() throws Throwable {
        return (int)vccUnboxBoxLoadIntMH.invokeExact(vcc);
    }

    @DontCompile
    public void test40_verifier(boolean warmup) {
        try {
            int result = test40();
            Asserts.assertEQ(vcc.x, result, "Field x of VCC and result must be equal");
        } catch (Throwable t) {
            throw new RuntimeException("Test failed in the interpeter", t);
        }
    }

    // Generate a MethodHandle that takes a value-capable class,
    // unboxes it, boxes it, reads a field from it, and returns the
    // field.
    private static MethodHandle generateVCCUnboxBoxLoadIntMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "vccUnboxBoxLoadInt",
                MethodType.methodType(int.class, ValueCapableClass1.class),
                CODE -> {
                    CODE.
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    vbox(ValueCapableClass1.class).
                    getfield(ValueCapableClass1.class, "x", "I").
                    ireturn();
                }
                );

    }

    // Test value type array creation and initialization
    @Test(valid = ValueTypeArrayFlattenOff, failOn = (LOAD))
    @Test(valid = ValueTypeArrayFlattenOn)
    public MyValue1[] test41(int len) {
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }
        return va;
    }

    @DontCompile
    public void test41_verifier(boolean warmup) {
        int len = Math.abs(rI % 10);
        MyValue1[] va = test41(len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i].hash(), hash());
        }
    }

    // Test creation of a value type array and element access
    @Test(valid = ValueTypeArrayFlattenOff, failOn = (LOOP + TRAP))
    @Test(valid = ValueTypeArrayFlattenOn, failOn = (ALLOC + ALLOCA + LOOP + LOAD + LOADP + STORE + TRAP))
    public long test42() {
        MyValue1[] va = new MyValue1[1];
        va[0] = MyValue1.createWithFieldsInline(rI, rL);
        return va[0].hash();
    }

    @DontCompile
    public void test42_verifier(boolean warmup) {
        long result = test42();
        Asserts.assertEQ(result, hash());
    }

    // Test receiving a value type array from the interpreter,
    // updating its elements in a loop and computing a hash.
    @Test(failOn = (ALLOCA))
    public long test43(MyValue1[] va) {
        long result = 0;
        for (int i = 0; i < 10; ++i) {
            result += va[i].hash();
            va[i] = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        }
        return result;
    }

    @DontCompile
    public void test43_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[10];
        long expected = 0;
        for (int i = 0; i < 10; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL + i);
            expected += va[i].hash();
        }
        long result = test43(va);
        Asserts.assertEQ(expected, result);
        for (int i = 0; i < 10; ++i) {
            if (va[i].hash() != hash(rI + 1, rL + 1)) {
                Asserts.assertEQ(va[i].hash(), hash(rI + 1, rL + 1));
            }
        }
    }

    // Test returning a value type array received from the interpreter
    @Test(failOn = ALLOC + ALLOCA + LOAD + LOADP + STORE + LOOP + TRAP)
    public MyValue1[] test44(MyValue1[] va) {
        return va;
    }

    @DontCompile
    public void test44_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[10];
        for (int i = 0; i < 10; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL + i);
        }
        va = test44(va);
        for (int i = 0; i < 10; ++i) {
            Asserts.assertEQ(va[i].hash(), hash(rI + i, rL + i));
        }
    }

    // Merge value type arrays created from two branches
    @Test
    public MyValue1[] test45(boolean b) {
        MyValue1[] va;
        if (b) {
            va = new MyValue1[5];
            for (int i = 0; i < 5; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI, rL);
            }
        } else {
            va = new MyValue1[10];
            for (int i = 0; i < 10; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI + i, rL + i);
            }
        }
        long sum = va[0].hashInterpreted();
        if (b) {
            va[0] = MyValue1.createWithFieldsDontInline(rI, sum);
        } else {
            va[0] = MyValue1.createWithFieldsDontInline(rI + 1, sum + 1);
        }
        return va;
    }

    @DontCompile
    public void test45_verifier(boolean warmup) {
        MyValue1[] va = test45(true);
        Asserts.assertEQ(va.length, 5);
        Asserts.assertEQ(va[0].hash(), hash(rI, hash()));
        for (int i = 1; i < 5; ++i) {
            Asserts.assertEQ(va[i].hash(), hash());
        }
        va = test45(false);
        Asserts.assertEQ(va.length, 10);
        Asserts.assertEQ(va[0].hash(), hash(rI + 1, hash(rI, rL) + 1));
        for (int i = 1; i < 10; ++i) {
            Asserts.assertEQ(va[i].hash(), hash(rI + i, rL + i));
        }
    }

    // Test creation of value type array with single element
    @Test(valid = ValueTypeArrayFlattenOff, failOn = (LOAD + LOOP + TRAP))
    @Test(valid = ValueTypeArrayFlattenOn, failOn = (ALLOCA + LOAD + LOOP + TRAP))
    public MyValue1 test46() {
        MyValue1[] va = new MyValue1[1];
        return va[0];
    }

    @DontCompile
    public void test46_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        MyValue1 v = test46();
        Asserts.assertEQ(v.hashPrimitive(), va[0].hashPrimitive());
    }

    // Test default initialization of value type arrays
    @Test(failOn = LOAD)
    public MyValue1[] test47(int len) {
        return new MyValue1[len];
    }

    @DontCompile
    public void test47_verifier(boolean warmup) {
        int len = Math.abs(rI % 10);
        MyValue1[] va = new MyValue1[len];
        MyValue1[] var = test47(len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // Test creation of value type array with zero length
    @Test(failOn = ALLOC + LOAD + STORE + LOOP + TRAP)
    public MyValue1[] test48() {
        return new MyValue1[0];
    }

    @DontCompile
    public void test48_verifier(boolean warmup) {
        MyValue1[] va = test48();
        Asserts.assertEQ(va.length, 0);
    }

    static MyValue1[] test49_va;

    // Test that value type array loaded from field has correct type
    @Test(failOn = (LOOP))
    public long test49() {
        return test49_va[0].hash();
    }

    @DontCompile
    public void test49_verifier(boolean warmup) {
        test49_va = new MyValue1[1];
        test49_va[0] = MyValue1.createWithFieldsInline(rI, rL);
        long result = test49();
        Asserts.assertEQ(result, hash());
    }

    // test vdefault
    @Test(failOn = ALLOC + LOAD + LOADP + STORE + LOOP + TRAP)
    public long test50() {
        MyValue2 v = MyValue2.createDefaultInline();
        return v.hash();
    }

    @DontCompile
    public void test50_verifier(boolean warmup) {
        long result = test50();
        Asserts.assertEQ(result, MyValue2.createDefaultInline().hash());
    }

    // test vdefault
    @Test(failOn = ALLOC + STORE + LOOP + TRAP)
    public long test51() {
        MyValue1 v1 = MyValue1.createDefaultInline();
        MyValue1 v2 = MyValue1.createDefaultDontInline();
        return v1.hashPrimitive() + v2.hashPrimitive();
    }

    @DontCompile
    public void test51_verifier(boolean warmup) {
        long result = test51();
        Asserts.assertEQ(result, 2 * MyValue1.createDefaultInline().hashPrimitive());
    }

    // test vwithfield
    @Test(failOn = ALLOC + LOAD + LOADP + STORE + LOOP + TRAP)
    public long test52() {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        return v.hash();
    }

    @DontCompile
    public void test52_verifier(boolean warmup) {
        long result = test52();
        Asserts.assertEQ(result, MyValue2.createWithFieldsInline(rI, true).hash());
    }

    // test vwithfield
    @Test(failOn = ALLOC + STORE + LOOP + TRAP)
    public long test53() {
        MyValue1 v1 = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 v2 = MyValue1.createWithFieldsDontInline(rI, rL);
        return v1.hash() + v2.hash();
    }

    @DontCompile
    public void test53_verifier(boolean warmup) {
        long result = test53();
        Asserts.assertEQ(result, 2 * hash());
    }

    // multi-dimensional arrays
    @Test
    public MyValue1[][][] test54(int len1, int len2, int len3) {
        MyValue1[][][] arr = new MyValue1[len1][len2][len3];
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                for (int k = 0; k < len3; k++) {
                    arr[i][j][k] = MyValue1.createWithFieldsDontInline(rI + i , rL + j + k);
                }
            }
        }
        return arr;
    }

    @DontCompile
    public void test54_verifier(boolean warmup) {
        MyValue1[][][] arr = test54(2, 3, 4);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    Asserts.assertEQ(arr[i][j][k].hash(), MyValue1.createWithFieldsDontInline(rI + i , rL + j + k).hash());
                }
            }
        }
    }

    @Test
    public void test55(MyValue1[][][] arr, long[] res) {
        int l = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                for (int k = 0; k < arr[i][j].length; k++) {
                    res[l] = arr[i][j][k].hash();
                    l++;
                }
            }
        }
    }

    @DontCompile
    public void test55_verifier(boolean warmup) {
        MyValue1[][][] arr = new MyValue1[2][3][4];
        long[] res = new long[2*3*4];
        long[] verif = new long[2*3*4];
        int l = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    arr[i][j][k] = MyValue1.createWithFieldsDontInline(rI + i, rL + j + k);
                    verif[l] = arr[i][j][k].hash();
                    l++;
                }
            }
        }
        test55(arr, res);
        for (int i = 0; i < verif.length; i++) {
            Asserts.assertEQ(res[i], verif[i]);
        }
    }

    class TestClass56 {
        public MyValue1 v;
    }

    // Test allocation elimination of unused object with initialized value type field
    @Test(failOn = ALLOC + LOAD + STORE + LOOP)
    public void test56(boolean deopt) {
        TestClass56 unused = new TestClass56();
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        unused.v = v;
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get("ValueTypeTestBench::test56"));
        }
    }

    @DontCompile
    public void test56_verifier(boolean warmup) {
        test56(!warmup);
    }

    // Test loop peeling
    @Test(failOn = ALLOC + LOAD + STORE)
    @Slow
    public void test57() {
        MyValue1 v = MyValue1.createWithFieldsInline(0, 1);
        // Trigger OSR compilation and loop peeling
        for (int i = 0; i < 100_000; ++i) {
            if (v.x != i || v.y != i + 1) {
                // Uncommon trap
                throw new RuntimeException("test57 failed");
            }
            v = MyValue1.createWithFieldsInline(i + 1, i + 2);
        }
    }

    @DontCompile
    public void test57_verifier(boolean warmup) {
        test57();
    }

    // Test loop peeling and unrolling
    @Test()
    @Slow
    public void test58() {
        MyValue1 v1 = MyValue1.createWithFieldsInline(0, 0);
        MyValue1 v2 = MyValue1.createWithFieldsInline(1, 1);
        // Trigger OSR compilation and loop peeling
        for (int i = 0; i < 100_000; ++i) {
            if (v1.x != 2*i || v2.x != i+1 || v2.y != i+1) {
                // Uncommon trap
                throw new RuntimeException("test58 failed");
            }
            v1 = MyValue1.createWithFieldsInline(2*(i+1), 0);
            v2 = MyValue1.createWithFieldsInline(i+2, i+2);
        }
    }

    @DontCompile
    public void test58_verifier(boolean warmup) {
        test58();
    }

    // When calling a method on __Value, passing fields as arguments is impossible
    @Test(failOn = ALLOC + STORE + LOAD)
    public String test59(MyValue1 v) {
        return v.toString();
    }

    @DontCompile
    public void test59_verifier(boolean warmup) {
        boolean failed = false;
        try {
            test59(val1);
            failed = true;
        } catch (UnsupportedOperationException uoe) {
        }
        Asserts.assertFalse(failed);
    }

    // Same as above, but the method on __Value is inlined
    // hashCode allocates an exception so can't really check the graph shape
    @Test()
    public int test60(MyValue1 v) {
        return v.hashCode();
    }

    @DontCompile
    public void test60_verifier(boolean warmup) {
        boolean failed = false;
        try {
            test60(val1);
            failed = true;
        } catch (UnsupportedOperationException uoe) {
        }
        Asserts.assertFalse(failed);
    }

    /* The compiler is supposed to determine that the value to be
     * unboxed in nullcvvUnboxLoadLong is always null. Therefore, the
     * compiler generates only the path leading to the corresponding
     * uncommon trap. */
    @Test(failOn = RETURN)
    public long test61() throws Throwable {
        return (long)nullvccUnboxLoadLongMH.invokeExact();
    }

    @DontCompile
    public void test61_verifier(boolean warmup) throws Throwable {
        try {
            long result = test61();
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (NullPointerException e) {
        }
    }

    public static MethodHandle generateNullVCCUnboxLoadLongMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "nullvccUnboxLoadLong",
                MethodType.methodType(long.class),
                CODE -> {
                    CODE.
                    aconst_null().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                    lreturn();
                }
                );
    }

    /* The compiler is supposed to determine that the allocated
     * ValueCapableClass1 instance is never null (and therefore not
     * generate a null check). Also, the source and target type match
     * (known at compile time), so no type check is needed either.*/
    @Test(failOn = NPE)
    public long test62() throws Throwable {
        return (long)checkedvccUnboxLoadLongMH.invokeExact();
    }

    @DontCompile
    public void test62_verifier(boolean warmup) throws Throwable {
        long result = test62();
        Asserts.assertEQ(result, 17L);
    }

    public static MethodHandle generateCheckedVCCUnboxLoadLongMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "checkedVCCUnboxLoadLongMH",
                MethodType.methodType(long.class),
                CODE -> {
                    CODE.
                    invokestatic(ValueCapableClass1.class, "createInline", "()Lcompiler/valhalla/valuetypes/ValueCapableClass1;", false).
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                    lreturn();
                }
                );
    }

    /* The compiler is supposed to emit a runtime null check because
     * it does not have enough information to determine that the value
     * to be unboxed is not null (and either that it is null). The
     * declared type of the */
    @Test(match = {NPE}, matchCount = {1})
    public long test63(ValueCapableClass1 vcc) throws Throwable {
        return (long)vccUnboxLoadLongMH.invokeExact(vcc);
    }

    @DontCompile
    public void test63_verifier(boolean warmup) throws Throwable {
        try {
            long result = test63(null);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (NullPointerException e) {
        }
    }

    /* Attempt to unbox an object that is not a subclass of the
     * value-capable class derived from the value type specified in
     * the vunbox bytecode. */
    @Test(match = {NPE,CCE}, matchCount = {1,1})
    public long test64(Object vcc) throws Throwable {
        return (long)objectUnboxLoadLongMH.invokeExact(vcc);
    }

    @DontCompile
    public void test64_verifier(boolean warmup) throws Throwable {
        try {
            long result = test64(new Object());
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        try {
            long result = test64(vcc2);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        Asserts.assertEQ(test64(vcc), rL);
    }

    private static MethodHandle generateObjectUnboxLoadLongMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "ObjectUnboxLoadLong",
                MethodType.methodType(long.class, Object.class),
                CODE -> {
                    CODE.
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    getfield(ValueType.forClass(ValueCapableClass1.class).valueClass(), "t", "J").
                    lreturn();
                }
                );
    }

    /* Generate an if-then-else construct with one path that contains
     * an invalid boxing operation (boxing of a value-type to a
     * non-matching value-capable class).*/
    @Test(match = {NPE, CCE}, matchCount = {2, 2})
    public long test65(Object obj, boolean warmup) throws Throwable {
        return (long)objectBoxMH.invokeExact(obj, warmup);
    }

    @DontCompile
    public void test65_verifier(boolean warmup) throws Throwable {
        try {
            long result = test65(vcc2, true);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        Asserts.assertEQ(test65(vcc, true), rL);

        try {
            long result = test65(vcc2, false);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }

        try {
            long result = test65(vcc, false);
            throw new RuntimeException("Test failed because no exception was thrown");
        } catch (ClassCastException e) {
        }
    }

    private static MethodHandle generateObjectBoxMH() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "ObjectBox",
                MethodType.methodType(long.class, Object.class, boolean.class),
                CODE -> {
                    CODE.
                    iload_1().
                    iconst_1().
                    ifcmp(TypeTag.I, CondKind.NE, "not_equal").
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    vbox(ValueCapableClass1.class).
                    getfield(ValueCapableClass1.class, "t", "J").
                    lreturn().
                    label("not_equal").
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass2.class).valueClass()).
                    vbox(ValueCapableClass1.class).
                    getfield(ValueCapableClass1.class, "t", "J").
                    lreturn();
                }
                );
    }

    // Test deoptimization at call return with return value in registers
    @DontCompile
    public MyValue2 test66_interp(boolean deopt) {
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get("ValueTypeTestBench::test66"));
        }
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @Test()
    public MyValue2 test66(boolean flag) {
        return test66_interp(flag);
    }

    @DontCompile
    public void test66_verifier(boolean warmup) {
        MyValue2 result = test66(!warmup);
        MyValue2 v = MyValue2.createWithFieldsInline(rI, true);
        Asserts.assertEQ(result.hash(), v.hash());
    }

    // Return value types in registers from interpreter -> compiled
    final MyValue3 test67_vt = MyValue3.create();
    @DontCompile
    public MyValue3 test67_interp() {
        return test67_vt;
    }

    MyValue3 test67_vt2;
    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    public void test67() {
        test67_vt2 = test67_interp();
    }

    @DontCompile
    public void test67_verifier(boolean warmup) {
        test67();
        test67_vt.verify(test67_vt2);
    }

    // Return value types in registers from compiled -> interpreter
    final MyValue3 test68_vt = MyValue3.create();
    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + STORE + TRAP)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    public MyValue3 test68() {
        return test68_vt;
    }

    @DontCompile
    public void test68_verifier(boolean warmup) {
        MyValue3 vt = test68();
        test68_vt.verify(vt);
    }

    // Return value types in registers from compiled -> compiled
    final MyValue3 test69_vt = MyValue3.create();
    @DontInline
    public MyValue3 test69_comp() {
        return test69_vt;
    }

    MyValue3 test69_vt2;
    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    public void test69() {
        test69_vt2 = test69_comp();
    }

    @DontCompile
    public void test69_verifier(boolean warmup) throws Exception {
        Method helper_m = getClass().getDeclaredMethod("test69_comp");
        if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
            WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test69_comp not compiled");
        }
        test69();
        test69_vt.verify(test69_vt2);
    }

    // Same tests as above but with a value type that cannot be returned in registers

    // Return value types in registers from interpreter -> compiled
    final MyValue4 test70_vt = MyValue4.create();
    @DontCompile
    public MyValue4 test70_interp() {
        return test70_vt;
    }

    MyValue4 test70_vt2;
    @Test
    public void test70() {
        test70_vt2 = test70_interp();
    }

    @DontCompile
    public void test70_verifier(boolean warmup) {
        test70();
        test70_vt.verify(test70_vt2);
    }

    // Return value types in registers from compiled -> interpreter
    final MyValue4 test71_vt = MyValue4.create();
    @Test
    public MyValue4 test71() {
        return test71_vt;
    }

    @DontCompile
    public void test71_verifier(boolean warmup) {
        MyValue4 vt = test71();
        test71_vt.verify(vt);
    }

    // Return value types in registers from compiled -> compiled
    final MyValue4 test72_vt = MyValue4.create();
    @DontInline
    public MyValue4 test72_comp() {
        return test72_vt;
    }

    MyValue4 test72_vt2;
    @Test
    public void test72() {
        test72_vt2 = test72_comp();
    }

    @DontCompile
    public void test72_verifier(boolean warmup) throws Exception {
        Method helper_m = getClass().getDeclaredMethod("test72_comp");
        if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
            WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test72_comp not compiled");
        }
        test72();
        test72_vt.verify(test72_vt2);
    }

    // Return values and method handles tests

    // Everything inlined
    final MyValue3 test73_vt = MyValue3.create();
    @ForceInline
    MyValue3 test73_target() {
        return test73_vt;
    }

    static final MethodHandle test73_mh;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + STORE + CALL)
    @Test(valid = ValueTypeReturnedAsFieldsOff, match = { ALLOC, STORE }, matchCount = { 1, 11 })
    MyValue3 test73() throws Throwable {
        return (MyValue3)test73_mh.invokeExact(this);
    }

    @DontCompile
    public void test73_verifier(boolean warmup) throws Throwable {
        MyValue3 vt = test73();
        test73_vt.verify(vt);
    }

    // Leaf method not inlined but returned type is known
    final MyValue3 test74_vt = MyValue3.create();
    @DontInline
    MyValue3 test74_target() {
        return test74_vt;
    }

    static final MethodHandle test74_mh;

    @Test
    MyValue3 test74() throws Throwable {
        return (MyValue3)test74_mh.invokeExact(this);
    }

    @DontCompile
    public void test74_verifier(boolean warmup) throws Throwable {
        Method helper_m = getClass().getDeclaredMethod("test74_target");
        if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
            WHITE_BOX.enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test74_target not compiled");
        }
        MyValue3 vt = test74();
        test74_vt.verify(vt);
    }

    // Leaf method not inlined and returned type not known
    final MyValue3 test75_vt = MyValue3.create();
    @DontInline
    MyValue3 test75_target() {
        return test75_vt;
    }

    static final MethodHandle test75_mh;

    @Test
    MyValue3 test75() throws Throwable {
        return (MyValue3)test75_mh.invokeExact(this);
    }

    @DontCompile
    public void test75_verifier(boolean warmup) throws Throwable {
        // hack so C2 doesn't know the target of the invoke call
        Class c = Class.forName("java.lang.invoke.DirectMethodHandle");
        Method m = c.getDeclaredMethod("internalMemberName", Object.class);
        WHITE_BOX.testSetDontInlineMethod(m, warmup);
        MyValue3 vt = test75();
        test75_vt.verify(vt);
    }

    // Test no result from inlined method for incremental inlining
    final MyValue3 test76_vt = MyValue3.create();
    public MyValue3 test76_inlined() {
        throw new RuntimeException();
    }

    @Test
    public MyValue3 test76() {
        try {
            return test76_inlined();
        } catch (RuntimeException ex) {
            return test76_vt;
        }
    }

    @DontCompile
    public void test76_verifier(boolean warmup) {
        MyValue3 vt = test76();
        test76_vt.verify(vt);
    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue3;", ValueTypeTestBench.class.getClassLoader());
            test73_mh = lookup.findVirtual(ValueTypeTestBench.class, "test73_target", mt);
            test74_mh = lookup.findVirtual(ValueTypeTestBench.class, "test74_target", mt);
            test75_mh = lookup.findVirtual(ValueTypeTestBench.class, "test75_target", mt);
        } catch (NoSuchMethodException|IllegalAccessException e) {
            throw new RuntimeException("method handle lookup fails");
        }
    }

    /* Array load out of bounds (upper bound) at compile time.*/
    @Test
    public int test77() {
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
        }

        try {
            return va[arraySize + 1].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test77_verifier(boolean warmup) {
        Asserts.assertEQ(test77(), rI);
    }

    /* Array load  out of bounds (lower bound) at compile time.*/
    @Test
    public int test78() {
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL);
        }

        try {
            return va[-arraySize].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test78_verifier(boolean warmup) {
        Asserts.assertEQ(test78(), rI);
    }

    /* Array load out of bound not known to compiler (both lower and upper bound). */
    @Test
    public int test79(MyValue1[] va, int index)  {
        return va[index].x;
    }

    public void test79_verifier(boolean warmup) {
        int arraySize = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }

        int result;
        for (int i = -20; i < 20; i++) {
            try {
                result = test79(va, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                result = rI;
            }
            Asserts.assertEQ(result, rI);
        }
    }

    /* Array store out of bounds (upper bound) at compile time.*/
    @Test
    public int test80() {
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        try {
            for (int i = 0; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test80_verifier(boolean warmup) {
        Asserts.assertEQ(test80(), rI);
    }

    /* Array store out of bounds (lower bound) at compile time.*/
    @Test
    public int test81() {
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        try {
            for (int i = -1; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test81_verifier(boolean warmup) {
        Asserts.assertEQ(test81(), rI);
    }

    /* Array store out of bound not known to compiler (both lower and upper bound). */
    @Test
    public int test82(MyValue1[] va, int index, MyValue1 vt)  {
        va[index] = vt;
        return va[index].x;
    }

    @DontCompile
    public void test82_verifier(boolean warmup) {
        int arraySize = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }

        MyValue1 vt = MyValue1.createWithFieldsDontInline(rI + 1, rL);
        int result;
        for (int i = -20; i < 20; i++) {
            try {
                result = test82(va, i, vt);
            } catch (ArrayIndexOutOfBoundsException e) {
                result = rI + 1;
            }
            Asserts.assertEQ(result, rI + 1);
        }

        for (int i = 0; i < arraySize; i++) {
            Asserts.assertEQ(va[i].x, rI + 1);
        }
    }

    /* Create a new value type array and store a value type into
     * it. The test should pass without throwing an exception. */
    @Test
    public void test83() throws Throwable {
        vastoreMH.invokeExact(vcc);
    }

    public void test83_verifier(boolean warmup) throws Throwable {
        test83();
    }

    private static MethodHandle generateVastore() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "Vastore",
                MethodType.methodType(void.class, ValueCapableClass1.class),
                CODE -> {
                    CODE.
                    iconst_1().
                    anewarray(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    iconst_0().
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    vastore().
                    return_();
                }
                );
    }

    /* Create a new value type array with element type
     * ValueCapableClass1 and attempt to store a value type of type
     * ValueCapableClass2 into it. */
    @Test
    public void test84() throws Throwable {
        invalidVastoreMH.invokeExact(vcc2);
    }

    public void test84_verifier(boolean warmup) throws Throwable {
        boolean exceptionThrown = false;
        try {
            test84();
        } catch (ArrayStoreException e) {
            exceptionThrown = true;
        }
        Asserts.assertTrue(exceptionThrown, "ArrayStoreException must be thrown");
    }

    private static MethodHandle generateInvalidVastore() {
        return MethodHandleBuilder.loadCode(MethodHandles.lookup(),
                "Vastore",
                MethodType.methodType(void.class, ValueCapableClass2.class),
                CODE -> {
                    CODE.
                    iconst_1().
                    anewarray(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                    iconst_0().
                    aload_0().
                    vunbox(ValueType.forClass(ValueCapableClass2.class).valueClass()).
                    vastore().
                    return_();
                }
                );
    }

    // When test85_helper1 is inlined in test85, the method handle
    // linker that called it is passed a pointer to a copy of vt
    // stored in memory. The method handle linker needs to load the
    // fields from memory before it inlines test85_helper1.
    static public int test85_helper1(MyValue1 vt) {
        return vt.x;
    }

    static MyValue1 test85_vt = MyValue1.createWithFieldsInline(rI, rL);
    static public MyValue1 test85_helper2() {
        return test85_vt;
    }

    static final MethodHandle test85_mh;

    @Test
    public int test85() throws Throwable {
        return (int)test85_mh.invokeExact();
    }

    @DontCompile
    public void test85_verifier(boolean warmup) throws Throwable {
        int i = test85();
        Asserts.assertEQ(i, test85_vt.x);
    }

    // Test method handle call with value type argument
    public int test86_target(MyValue1 vt) {
        return vt.x;
    }

    static final MethodHandle test86_mh;
    MyValue1 test86_vt = MyValue1.createWithFieldsInline(rI, rL);

    @Test
    public int test86() throws Throwable {
        return (int)test86_mh.invokeExact(this, test86_vt);
    }

    @DontCompile
    public void test86_verifier(boolean warmup) throws Throwable {
        int i = test86();
        Asserts.assertEQ(i, test86_vt.x);
    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType test85_mt1 = MethodType.fromMethodDescriptorString("(Qcompiler/valhalla/valuetypes/MyValue1;)I", ValueTypeTestBench.class.getClassLoader());
            MethodType test85_mt2 = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue1;", ValueTypeTestBench.class.getClassLoader());
            MethodHandle test85_mh1 = lookup.findStatic(ValueTypeTestBench.class, "test85_helper1", test85_mt1);
            MethodHandle test85_mh2 = lookup.findStatic(ValueTypeTestBench.class, "test85_helper2", test85_mt2);
            test85_mh = MethodHandles.filterReturnValue(test85_mh2, test85_mh1);

            MethodType test86_mt = MethodType.fromMethodDescriptorString("(Qcompiler/valhalla/valuetypes/MyValue1;)I", ValueTypeTestBench.class.getClassLoader());
            test86_mh = lookup.findVirtual(ValueTypeTestBench.class, "test86_target", test86_mt);
        } catch (NoSuchMethodException|IllegalAccessException e) {
            throw new RuntimeException("method handle lookup fails");
        }
    }

    static MyValue3 staticVal3;
    static MyValue3 staticVal3_copy;

    // Check elimination of redundant value type allocations
    @Test(match = {ALLOC}, matchCount = {1})
    public MyValue3 test87(MyValue3[] va) {
        // Create value type and force allocation
        MyValue3 vt = MyValue3.create();
        va[0] = vt;
        staticVal3 = vt;
        vt.verify(staticVal3);

        // Value type is now allocated, make a copy and force allocation.
        // Because copy is equal to vt, C2 should remove this redundant allocation.
        MyValue3 copy = MyValue3.setC(vt, vt.c);
        va[0] = copy;
        staticVal3_copy = copy;
        copy.verify(staticVal3_copy);
        return copy;
    }

    @DontCompile
    public void test87_verifier(boolean warmup) {
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test87(va);
        staticVal3.verify(vt);
        staticVal3.verify(va[0]);
        staticVal3_copy.verify(vt);
        staticVal3_copy.verify(va[0]);
    }

    // Verify that only dominating allocations are re-used
    @Test()
    public MyValue3 test88(boolean warmup) {
        MyValue3 vt = MyValue3.create();
        if (warmup) {
            staticVal3 = vt; // Force allocation
        }
        // Force allocation to verify that above
        // non-dominating allocation is not re-used
        MyValue3 copy = MyValue3.setC(vt, vt.c);
        staticVal3_copy = copy;
        copy.verify(vt);
        return copy;
    }

    @DontCompile
    public void test88_verifier(boolean warmup) {
        MyValue3 vt = test88(warmup);
        if (warmup) {
            staticVal3.verify(vt);
        }
    }

    // Verify that C2 recognizes value type loads and re-uses the oop to avoid allocations
    @Test(failOn = ALLOC + ALLOCA + STORE)
    public MyValue3 test89(MyValue3[] va) {
        // C2 can re-use the oop of staticVal3 because staticVal3 is equal to copy
        MyValue3 copy = MyValue3.copy(staticVal3);
        va[0] = copy;
        staticVal3 = copy;
        copy.verify(staticVal3);
        return copy;
    }

    @DontCompile
    public void test89_verifier(boolean warmup) {
        staticVal3 = MyValue3.create();
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test89(va);
        staticVal3.verify(vt);
        staticVal3.verify(va[0]);
    }

    // Verify that C2 recognizes value type loads and re-uses the oop to avoid allocations
    @Test(valid = ValueTypeReturnedAsFieldsOff, failOn = ALLOC + ALLOCA + STORE)
    @Test(valid = ValueTypeReturnedAsFieldsOn)
    public MyValue3 test90(MyValue3[] va) {
        // C2 can re-use the oop returned by createDontInline()
        // because the corresponding value type is equal to 'copy'.
        MyValue3 copy = MyValue3.copy(MyValue3.createDontInline());
        va[0] = copy;
        staticVal3 = copy;
        copy.verify(staticVal3);
        return copy;
    }

    @DontCompile
    public void test90_verifier(boolean warmup) {
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test90(va);
        staticVal3.verify(vt);
        staticVal3.verify(va[0]);
    }

    // Verify that C2 recognizes value type loads and re-uses the oop to avoid allocations
    @Test(valid = ValueTypePassFieldsAsArgsOff, failOn = ALLOC + ALLOCA + STORE)
    @Test(valid = ValueTypePassFieldsAsArgsOn)
    public MyValue3 test91(MyValue3 vt, MyValue3[] va) {
        // C2 can re-use the oop of vt because vt is equal to 'copy'.
        MyValue3 copy = MyValue3.copy(vt);
        va[0] = copy;
        staticVal3 = copy;
        copy.verify(staticVal3);
        return copy;
    }

    @DontCompile
    public void test91_verifier(boolean warmup) {
        MyValue3 vt = MyValue3.create();
        MyValue3[] va = new MyValue3[1];
        MyValue3 result = test91(vt, va);
        staticVal3.verify(vt);
        va[0].verify(vt);
        result.verify(vt);
    }

    // Test correct identification of value type copies
    @Test()
    public MyValue3 test92(MyValue3[] va) {
        MyValue3 vt = MyValue3.copy(staticVal3);
        vt = MyValue3.setI(vt, (int)vt.c);
        // vt is not equal to staticVal3, so C2 should not re-use the oop
        va[0] = vt;
        staticVal3 = vt;
        vt.verify(staticVal3);
        return vt;
    }

    @DontCompile
    public void test92_verifier(boolean warmup) {
        staticVal3 = MyValue3.create();
        MyValue3[] va = new MyValue3[1];
        MyValue3 vt = test92(va);
        Asserts.assertEQ(staticVal3.i, (int)staticVal3.c);
        Asserts.assertEQ(va[0].i, (int)staticVal3.c);
        Asserts.assertEQ(vt.i, (int)staticVal3.c);
    }

    // Test correct handling of __Value merges through PhiNodes
    @Test()
    @Slow
    public long test93() throws Throwable {
        // Create a new value type
        final MethodHandle dvt = MethodHandleBuilder.loadCode(MethodHandles.lookup(), "createValueType",
                                     MethodType.methodType(ValueType.forClass(ValueCapableClass1.class).valueClass()),
                                     CODE -> {
                                         CODE.
                                         iconst_1().
                                         anewarray(ValueType.forClass(ValueCapableClass1.class).valueClass()).
                                         iconst_0().
                                         vaload().
                                         vreturn();
                                     });
        // Box the value type
        final MethodHandle box = MethodHandleBuilder.loadCode(MethodHandles.lookup(), "boxValueType",
                                     MethodType.methodType(ValueCapableClass1.class, ValueType.forClass(ValueCapableClass1.class).valueClass()),
                                     CODE -> {
                                         CODE.
                                         vload(0).
                                         vbox(ValueCapableClass1.class).
                                         areturn();
                                     });
        long result = 0;
        for (int i = 0; i < 10_000; ++i) {
            // Merge __Value (ValueCapableClass1) from the two GWT branches, box to the VCC and access field
            MethodHandle gwt = MethodHandles.guardWithTest(MethodHandles.constant(boolean.class, i % 2 == 0), dvt, dvt);
            ValueCapableClass1 vcc = (ValueCapableClass1) MethodHandles.filterReturnValue(gwt, box).invokeExact();
            result += vcc.t;
        }
        return result;
    }

    @DontCompile
    public void test93_verifier(boolean warmup) throws Throwable {
        long result = test93();
        Asserts.assertEQ(result, 0L);
    }

    // Test correctness of the Class::isAssignableFrom intrinsic
    @Test()
    public boolean test94(Class<?> supercls, Class<?> subcls) {
        return supercls.isAssignableFrom(subcls);
    }

    public void test94_verifier(boolean warmup) {
        Asserts.assertTrue(test94(__Value.class, MyValue1.class), "test94_1 failed");
        Asserts.assertTrue(test94(MyValue1.class, MyValue1.class), "test94_2 failed");
        Asserts.assertTrue(test94(Object.class, java.util.ArrayList.class), "test94_3 failed");
        Asserts.assertTrue(test94(java.util.ArrayList.class, java.util.ArrayList.class), "test94_4 failed");
        Asserts.assertTrue(!test94(Object.class, MyValue1.class), "test94_5 failed");
        Asserts.assertTrue(!test94(__Value.class, java.util.ArrayList.class), "test94_6 failed");
    }

    // Verify that Class::isAssignableFrom checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test95() {
        boolean check1 = java.util.AbstractList.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check2 = MyValue1.class.isAssignableFrom(MyValue1.class);
        boolean check3 = Object.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check4 = java.lang.__Value.class.isAssignableFrom(MyValue1.class);
        boolean check5 = !Object.class.isAssignableFrom(MyValue1.class);
        boolean check6 = !MyValue1.class.isAssignableFrom(Object.class);
        return check1 && check2 && check3 && check4 && check5 && check6;
    }

    public void test95_verifier(boolean warmup) {
        Asserts.assertTrue(test95(), "test95 failed");
    }

    // Test correctness of the Class::getSuperclass intrinsic
    @Test()
    public Class<?> test96(Class<?> cls) {
        return cls.getSuperclass();
    }

    public void test96_verifier(boolean warmup) {
        Asserts.assertTrue(test96(__Value.class) == null, "test94_1 failed");
        Asserts.assertTrue(test96(Object.class) == null, "test94_2 failed");
        Asserts.assertTrue(test96(MyValue1.class) == __Value.class, "test94_3 failed");
        Asserts.assertTrue(test96(Class.class) == Object.class, "test94_4 failed");
    }

    // Verify that Class::getSuperclass checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test97() {
        boolean check1 = __Value.class.getSuperclass() == null;
        boolean check2 = Object.class.getSuperclass() == null;
        boolean check3 = MyValue1.class.getSuperclass() == __Value.class;
        boolean check4 = Class.class.getSuperclass() == Object.class;
        return check1 && check2 && check3 && check4;
    }

    public void test97_verifier(boolean warmup) {
        Asserts.assertTrue(test97(), "test97 failed");
    }

    // Test Class::cast intrinsic
    @Test()
    public Object test98(Class<?> cls, Object o) throws ClassCastException {
        return cls.cast(o);
    }

    public void test98_verifier(boolean warmup) {
        try {
            test98(ValueCapableClass1.class, vcc);
        } catch (ClassCastException e) {
            throw new RuntimeException("test98_1 failed");
        }
        try {
            test98(__Value.class, new Object());
            throw new RuntimeException("test98_2 failed");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // method handle combinators
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType test99_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue3;", ValueTypeTestBench.class.getClassLoader());
            MethodHandle test99_mh1 = lookup.findVirtual(ValueTypeTestBench.class, "test99_target1", test99_mt);
            MethodHandle test99_mh2 = lookup.findVirtual(ValueTypeTestBench.class, "test99_target2", test99_mt);
            MethodType boolean_mt = MethodType.methodType(boolean.class);
            MethodHandle test99_mh_test = lookup.findVirtual(ValueTypeTestBench.class, "test99_test", boolean_mt);
            test99_mh = MethodHandles.guardWithTest(test99_mh_test, test99_mh1, test99_mh2);

            MethodType myvalue2_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue2;", ValueTypeTestBench.class.getClassLoader());
            test100_mh1 = lookup.findStatic(ValueTypeTestBench.class, "test100_target1", myvalue2_mt);
            MethodHandle test100_mh2 = lookup.findStatic(ValueTypeTestBench.class, "test100_target2", myvalue2_mt);
            MethodHandle test100_mh_test = lookup.findStatic(ValueTypeTestBench.class, "test100_test", boolean_mt);
            test100_mh = MethodHandles.guardWithTest(test100_mh_test,
                                                    MethodHandles.invoker(myvalue2_mt),
                                                    MethodHandles.dropArguments(test100_mh2, 0, MethodHandle.class));

            MethodHandle test101_mh1 = lookup.findStatic(ValueTypeTestBench.class, "test101_target1", myvalue2_mt);
            test101_mh2 = lookup.findStatic(ValueTypeTestBench.class, "test101_target2", myvalue2_mt);
            MethodHandle test101_mh_test = lookup.findStatic(ValueTypeTestBench.class, "test101_test", boolean_mt);
            test101_mh = MethodHandles.guardWithTest(test101_mh_test,
                                                    MethodHandles.dropArguments(test101_mh1, 0, MethodHandle.class),
                                                    MethodHandles.invoker(myvalue2_mt));

            MethodHandle test102_count = MethodHandles.constant(int.class, 100);
            ValueType<?> test102_VT = ValueType.forClass(ValueCapableClass2.class);
            MethodHandle test102_init = test102_VT.defaultValueConstant();
            MethodHandle test102_getfield = test102_VT.findGetter(lookup, "u", long.class);
            MethodHandle test102_add = lookup.findStatic(Long.class, "sum", MethodType.methodType(long.class, long.class, long.class));
            MethodHandle test102_body = MethodHandles.collectArguments(ValueCapableClass2.FACTORY,
                                                                      0,
                                                                      MethodHandles.dropArguments(MethodHandles.collectArguments(MethodHandles.insertArguments(test102_add,
                                                                                                                                                               0,
                                                                                                                                                               1L),
                                                                                                                                 0,
                                                                                                                                 test102_getfield),
                                                                                                  1,
                                                                                                  int.class));
            test102_mh = MethodHandles.collectArguments(test102_getfield,
                                                       0,
                                                       MethodHandles.countedLoop(test102_count, test102_init, test102_body));

            MethodType test103_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue3;", ValueTypeTestBench.class.getClassLoader());
            MethodHandle test103_mh1 = lookup.findVirtual(ValueTypeTestBench.class, "test103_target1", test103_mt);
            MethodHandle test103_mh2 = lookup.findVirtual(ValueTypeTestBench.class, "test103_target2", test103_mt);
            MethodHandle test103_mh3 = lookup.findVirtual(ValueTypeTestBench.class, "test103_target3", test103_mt);
            MethodType test103_mt2 = MethodType.methodType(boolean.class);
            MethodHandle test103_mh_test1 = lookup.findVirtual(ValueTypeTestBench.class, "test103_test1", test103_mt2);
            MethodHandle test103_mh_test2 = lookup.findVirtual(ValueTypeTestBench.class, "test103_test2", test103_mt2);
            test103_mh = MethodHandles.guardWithTest(test103_mh_test1,
                                                    test103_mh1,
                                                    MethodHandles.guardWithTest(test103_mh_test2, test103_mh2, test103_mh3));

            MethodType test104_mt = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue2;", ValueTypeTestBench.class.getClassLoader());
            MethodHandle test104_mh1 = lookup.findStatic(ValueTypeTestBench.class, "test104_target1", test104_mt);
            test104_mh2 = lookup.findStatic(ValueTypeTestBench.class, "test104_target2", test104_mt);
            test104_mh3 = lookup.findStatic(ValueTypeTestBench.class, "test104_target3", test104_mt);
            MethodType test104_mt2 = MethodType.methodType(boolean.class);
            MethodType test104_mt3 = MethodType.fromMethodDescriptorString("()Qcompiler/valhalla/valuetypes/MyValue2;", ValueTypeTestBench.class.getClassLoader());
            MethodHandle test104_mh_test1 = lookup.findStatic(ValueTypeTestBench.class, "test104_test1", test104_mt2);
            MethodHandle test104_mh_test2 = lookup.findStatic(ValueTypeTestBench.class, "test104_test2", test104_mt2);
            test104_mh = MethodHandles.guardWithTest(test104_mh_test1,
                                                    MethodHandles.dropArguments(test104_mh1, 0, MethodHandle.class, MethodHandle.class),
                                                    MethodHandles.guardWithTest(test104_mh_test2,
                                                                                MethodHandles.dropArguments(MethodHandles.invoker(test104_mt3), 1, MethodHandle.class),
                                                                                MethodHandles.dropArguments(MethodHandles.invoker(test104_mt3), 0, MethodHandle.class))
                                                    );

            MethodHandle test105_mh1 = lookup.findStatic(ValueTypeTestBench.class, "test105_target1", myvalue2_mt);
            test105_mh2 = lookup.findStatic(ValueTypeTestBench.class, "test105_target2", myvalue2_mt);
            MethodHandle test105_mh_test = lookup.findStatic(ValueTypeTestBench.class, "test105_test", boolean_mt);
            test105_mh = MethodHandles.guardWithTest(test105_mh_test,
                                                    MethodHandles.dropArguments(test105_mh1, 0, MethodHandle.class),
                                                    MethodHandles.invoker(myvalue2_mt));
        } catch (NoSuchMethodException|IllegalAccessException|NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException("method handle lookup fails");
        }
    }

    // Return of target1 and target2 merged in a Lambda Form as an
    // __Value. Shouldn't cause any allocation
    final MyValue3 test99_vt1 = MyValue3.create();
    @ForceInline
    MyValue3 test99_target1() {
        return test99_vt1;
    }

    final MyValue3 test99_vt2 = MyValue3.create();
    @ForceInline
    MyValue3 test99_target2() {
        return test99_vt2;
    }

    boolean test99_bool = true;
    @ForceInline
    boolean test99_test() {
        return test99_bool;
    }

    static final MethodHandle test99_mh;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    MyValue3 test99() throws Throwable {
        return (MyValue3)test99_mh.invokeExact(this);
    }

    @DontCompile
    public void test99_verifier(boolean warmup) throws Throwable {
        test99_bool = !test99_bool;
        MyValue3 vt = test99();
        vt.verify(test99_bool ? test99_vt1 : test99_vt2);
    }

    // Similar as above but with the method handle for target1 not
    // constant. Shouldn't cause any allocation.
    @ForceInline
    static MyValue2 test100_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test100_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    static boolean test100_bool = true;
    @ForceInline
    static boolean test100_test() {
        return test100_bool;
    }

    static final MethodHandle test100_mh;
    static MethodHandle test100_mh1;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    long test100() throws Throwable {
        return ((MyValue2)test100_mh.invokeExact(test100_mh1)).hash();
    }

    @DontCompile
    public void test100_verifier(boolean warmup) throws Throwable {
        test100_bool = !test100_bool;
        long hash = test100();
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+(test100_bool ? 0 : 1), test100_bool).hash());
    }

    // Same as above but with the method handle for target2 not
    // constant. Shouldn't cause any allocation.
    @ForceInline
    static MyValue2 test101_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test101_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    static boolean test101_bool = true;
    @ForceInline
    static boolean test101_test() {
        return test101_bool;
    }

    static final MethodHandle test101_mh;
    static MethodHandle test101_mh2;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    long test101() throws Throwable {
        return ((MyValue2)test101_mh.invokeExact(test101_mh2)).hash();
    }

    @DontCompile
    public void test101_verifier(boolean warmup) throws Throwable {
        test101_bool = !test101_bool;
        long hash = test101();
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+(test101_bool ? 0 : 1), test101_bool).hash());
    }

    // Simple reduction with intermediate result merged in a Lambda
    // Form as an __Value. Shouldn't cause any allocations. The entire
    // loop should go away as the result is a constant.
    static final MethodHandle test102_mh;

    @Test(failOn = ALLOC + STORE + LOOP + STOREVALUETYPEFIELDS)
    long test102() throws Throwable {
        return (long)test102_mh.invokeExact();
    }

    @DontCompile
    public void test102_verifier(boolean warmup) throws Throwable {
        long v = test102();
        Asserts.assertEQ(v, 100L);
    }

    // Return of target1, target2 and target3 merged in Lambda Forms
    // as an __Value. Shouldn't cause any allocation
    final MyValue3 test103_vt1 = MyValue3.create();
    @ForceInline
    MyValue3 test103_target1() {
        return test103_vt1;
    }

    final MyValue3 test103_vt2 = MyValue3.create();
    @ForceInline
    MyValue3 test103_target2() {
        return test103_vt2;
    }

    final MyValue3 test103_vt3 = MyValue3.create();
    @ForceInline
    MyValue3 test103_target3() {
        return test103_vt3;
    }

    boolean test103_bool1 = true;
    @ForceInline
    boolean test103_test1() {
        return test103_bool1;
    }

    boolean test103_bool2 = true;
    @ForceInline
    boolean test103_test2() {
        return test103_bool2;
    }

    static final MethodHandle test103_mh;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    MyValue3 test103() throws Throwable {
        return (MyValue3)test103_mh.invokeExact(this);
    }

    static int test103_i = 0;
    @DontCompile
    public void test103_verifier(boolean warmup) throws Throwable {
        test103_i++;
        test103_bool1 = (test103_i % 2) == 0;
        test103_bool2 = (test103_i % 3) == 0;
        MyValue3 vt = test103();
        vt.verify(test103_bool1 ? test103_vt1 : (test103_bool2 ? test103_vt2 : test103_vt3));
    }

    // Same as above but with non constant target2 and target3
    @ForceInline
    static MyValue2 test104_target1() {
        return MyValue2.createWithFieldsInline(rI, true);
    }

    @ForceInline
    static MyValue2 test104_target2() {
        return MyValue2.createWithFieldsInline(rI+1, false);
    }

    @ForceInline
    static MyValue2 test104_target3() {
        return MyValue2.createWithFieldsInline(rI+2, true);
    }

    static boolean test104_bool1 = true;
    @ForceInline
    static boolean test104_test1() {
        return test104_bool1;
    }

    static boolean test104_bool2 = true;
    @ForceInline
    static boolean test104_test2() {
        return test104_bool2;
    }

    static final MethodHandle test104_mh;
    static MethodHandle test104_mh2;
    static MethodHandle test104_mh3;

    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    long test104() throws Throwable {
        return ((MyValue2)test104_mh.invokeExact(test104_mh2, test104_mh3)).hash();
    }

    static int test104_i = 0;
    @DontCompile
    public void test104_verifier(boolean warmup) throws Throwable {
        test104_i++;
        test104_bool1 = (test104_i % 2) == 0;
        test104_bool2 = (test104_i % 3) == 0;
        long hash = test104();
        int i = rI+(test104_bool1 ? 0 : (test104_bool2 ? 1 : 2));
        boolean b = test104_bool1 ? true : (test104_bool2 ? false : true);
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(i, b).hash());
    }

    @ForceInline
    static MyValue2 test105_target1() {
        return MyValue2.createWithFieldsInline(rI+test105_i, true);
    }

    @ForceInline
    static MyValue2 test105_target2() {
        return MyValue2.createWithFieldsInline(rI-test105_i, false);
    }

    static int test105_i = 0;
    @ForceInline
    static boolean test105_test() {
        return (test105_i % 100) == 0;
    }

    static final MethodHandle test105_mh;
    static MethodHandle test105_mh2;

    // Check that a buffered value returned by a compiled lambda form
    // is properly handled by the caller.
    @Test(valid = ValueTypeReturnedAsFieldsOn, failOn = ALLOC + ALLOCA + STORE + STOREVALUETYPEFIELDS)
    @Test(valid = ValueTypeReturnedAsFieldsOff)
    @Warmup(11000)
    long test105() throws Throwable {
        return ((MyValue2)test105_mh.invokeExact(test105_mh2)).hash();
    }

    @DontCompile
    public void test105_verifier(boolean warmup) throws Throwable {
        test105_i++;
        long hash = test105();
        boolean b = (test105_i % 100) == 0;
        Asserts.assertEQ(hash, MyValue2.createWithFieldsInline(rI+test105_i * (b ? 1 : -1), b).hash());
    }


    // OSR compilation with __Value local
    @DontCompile
    public __Value test106_init() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @DontCompile
    public __Value test106_body() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @Test()
    @Slow
    public __Value test106() throws Throwable {
        __Value vt = test106_init();
        for (int i = 0; i < 50_000; i++) {
            if (i % 2 == 1) {
                vt = test106_body();
            }
        }
        return vt;
    }

    @DontCompile
    public void test106_verifier(boolean warmup) throws Throwable {
        test106();
    }

    // ========== Test infrastructure ==========

    // User defined settings
    private static final boolean SKIP_SLOW = Boolean.parseBoolean(System.getProperty("SkipSlow", "false"));
    private static final boolean PRINT_TIMES = Boolean.parseBoolean(System.getProperty("PrintTimes", "false"));
    private static final boolean VERIFY_IR = Boolean.parseBoolean(System.getProperty("VerifyIR", "true"));
    private static final boolean VERIFY_VM = Boolean.parseBoolean(System.getProperty("VerifyVM", "false"));
    private static final String TESTLIST = System.getProperty("Testlist", "");
    private static final int WARMUP = Integer.parseInt(System.getProperty("Warmup", "251"));

    // Pre defined settings
    private static final List<String> defaultFlags = Arrays.asList(
        "-XX:-BackgroundCompilation", "-XX:CICompilerCount=1",
        "-XX:+PrintCompilation", "-XX:+PrintInlining", "-XX:+PrintIdeal", "-XX:+PrintOptoAssembly",
        "-XX:CompileCommand=quiet",
        "-XX:CompileCommand=compileonly,java.lang.invoke.*::*",
        "-XX:CompileCommand=compileonly,java.lang.Long::sum",
        "-XX:CompileCommand=compileonly,java.lang.Object::<init>",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.MyValue1::*",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.MyValue2::*",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.MyValue2Inline::*",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.MyValue3::*",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.MyValue3Inline::*",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.MyValue4::*",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.ValueCapableClass2_*::*",
        "-XX:CompileCommand=compileonly,compiler.valhalla.valuetypes.ValueTypeTestBench::*",
        "-XX:CompileCommand=inline,java.lang.__Value::hashCode");
    private static final List<String> verifyFlags = Arrays.asList(
        "-XX:+VerifyOops", "-XX:+VerifyStack", "-XX:+VerifyLastFrame", "-XX:+VerifyBeforeGC", "-XX:+VerifyAfterGC",
        "-XX:+VerifyDuringGC", "-XX:+VerifyAdapterSharing", "-XX:+StressValueTypeReturnedAsFields");

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int ValueTypePassFieldsAsArgsOn = 0x1;
    private static final int ValueTypePassFieldsAsArgsOff = 0x2;
    private static final int ValueTypeArrayFlattenOn = 0x4;
    private static final int ValueTypeArrayFlattenOff = 0x8;
    private static final int ValueTypeReturnedAsFieldsOn = 0x10;
    private static final int ValueTypeReturnedAsFieldsOff = 0x20;
    static final int AllFlags = ValueTypePassFieldsAsArgsOn | ValueTypePassFieldsAsArgsOff | ValueTypeArrayFlattenOn | ValueTypeArrayFlattenOff | ValueTypeReturnedAsFieldsOn;
    private static final boolean ValueTypePassFieldsAsArgs = (Boolean)WHITE_BOX.getVMFlag("ValueTypePassFieldsAsArgs");
    private static final boolean ValueTypeArrayFlatten = (Boolean)WHITE_BOX.getVMFlag("ValueArrayFlatten");
    private static final boolean ValueTypeReturnedAsFields = (Boolean)WHITE_BOX.getVMFlag("ValueTypeReturnedAsFields");
    private static final int COMP_LEVEL_ANY = -2;
    private static final int COMP_LEVEL_FULL_OPTIMIZATION = 4;
    private static final Hashtable<String, Method> tests = new Hashtable<String, Method>();
    private static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    private static final boolean PRINT_IDEAL  = WHITE_BOX.getBooleanVMFlag("PrintIdeal");
    private static final boolean XCOMP = Platform.isComp();

    // Regular expressions used to match nodes in the PrintIdeal output
    private static final String START = "(\\d+\\t(.*";
    private static final String MID = ".*)+\\t===.*";
    private static final String END = ")|";
    private static final String ALLOC  = "(.*precise klass compiler/valhalla/valuetypes/MyValue.*\\R(.*(nop|spill).*\\R)*.*_new_instance_Java" + END;
    private static final String ALLOCA = "(.*precise klass \\[Qcompiler/valhalla/valuetypes/MyValue.*\\R(.*(nop|spill).*\\R)*.*_new_array_Java" + END;
    private static final String LOAD   = START + "Load(B|S|I|L|F|D)" + MID + "valuetype\\*" + END;
    private static final String LOADP  = START + "Load(P|N)" + MID + "valuetype\\*" + END;
    private static final String LOADK  = START + "LoadK" + MID + END;
    private static final String STORE  = START + "Store(B|S|I|L|F|D)" + MID + "valuetype\\*" + END;
    private static final String STOREP = START + "Store(P|N)" + MID + "valuetype\\*" + END;
    private static final String LOOP   = START + "Loop" + MID + "" + END;
    private static final String TRAP   = START + "CallStaticJava" + MID + "uncommon_trap.*(unstable_if|predicate)" + END;
    private static final String RETURN = START + "Return" + MID + "returns" + END;
    private static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
    private static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
    private static final String CCE = START + "CallStaticJava" + MID + "class_check" + END;
    private static final String CALL = START + "CallStaticJava" + MID + END;
    private static final String STOREVALUETYPEFIELDS = START + "CallStaticJava" + MID + "store_value_type_fields" + END;
    private static final String SCOBJ = "(.*# ScObj.*" + END;

    static {
        List<String> list = null;
        if (!TESTLIST.isEmpty()) {
           list = Arrays.asList(TESTLIST.split(","));
        }
        // Gather all test methods and put them in Hashtable
        for (Method m : ValueTypeTestBench.class.getDeclaredMethods()) {
            Test[] annos = m.getAnnotationsByType(Test.class);
            if (annos.length != 0 &&
                (list == null || list.contains(m.getName())) &&
                !(SKIP_SLOW && m.isAnnotationPresent(Slow.class))) {
                tests.put("ValueTypeTestBench::" + m.getName(), m);
            }
        }
    }

    private static void execute_vm() throws Throwable {
        Asserts.assertFalse(tests.isEmpty(), "no tests to execute");
        ArrayList<String> args = new ArrayList<String>(defaultFlags);
        if (VERIFY_VM) {
            args.addAll(verifyFlags);
        }
        // Run tests in own process and verify output
        args.add(ValueTypeTestBench.class.getName());
        args.add("run");
        // Spawn process with default JVM options from the test's run command
        String[] vmInputArgs = InputArguments.getVmInputArgs();
        String[] cmds = Arrays.copyOf(vmInputArgs, vmInputArgs.length + args.size());
        System.arraycopy(args.toArray(), 0, cmds, vmInputArgs.length, args.size());
        OutputAnalyzer oa = ProcessTools.executeTestJvm(cmds);
        // If ideal graph printing is enabled/supported, verify output
        String output = oa.getOutput();
        oa.shouldHaveExitValue(0);
        boolean verifyIR = VERIFY_IR && output.contains("PrintIdeal enabled") &&
                !output.contains("ValueTypePassFieldsAsArgs is not supported on this platform");
        if (verifyIR) {
            parseOutput(output);
        } else {
            System.out.println(output);
            System.out.println("WARNING: IR verification disabled! Running with -Xint, -Xcomp or release build?");
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            // Spawn a new VM instance
            execute_vm();
        } else {
            // Execute tests
            ValueTypeTestBench bench = new ValueTypeTestBench();
            bench.run();
        }
    }

    private static void parseOutput(String output) throws Exception {
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

    public void setup(Method[] methods) {
        if (XCOMP) {
          // Don't control compilation if -Xcomp is enabled
          return;
        }
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
    }

    public void run() throws Exception {
        if (USE_COMPILER && PRINT_IDEAL && !XCOMP) {
            System.out.println("PrintIdeal enabled");
        }
        System.out.format("rI = %d, rL = %d\n", rI, rL);
        setup(this.getClass().getDeclaredMethods());
        setup(MyValue1.class.getDeclaredMethods());
        setup(MyValue2.class.getDeclaredMethods());
        setup(MyValue2Inline.class.getDeclaredMethods());
        setup(MyValue3.class.getDeclaredMethods());
        setup(MyValue3Inline.class.getDeclaredMethods());
        setup(MyValue4.class.getDeclaredMethods());

        // Compile class initializers
        WHITE_BOX.enqueueInitializerForCompilation(this.getClass(), COMP_LEVEL_FULL_OPTIMIZATION);
        WHITE_BOX.enqueueInitializerForCompilation(MyValue1.class, COMP_LEVEL_FULL_OPTIMIZATION);
        WHITE_BOX.enqueueInitializerForCompilation(MyValue2.class, COMP_LEVEL_FULL_OPTIMIZATION);
        WHITE_BOX.enqueueInitializerForCompilation(MyValue2Inline.class, COMP_LEVEL_FULL_OPTIMIZATION);
        WHITE_BOX.enqueueInitializerForCompilation(MyValue3.class, COMP_LEVEL_FULL_OPTIMIZATION);
        WHITE_BOX.enqueueInitializerForCompilation(MyValue3Inline.class, COMP_LEVEL_FULL_OPTIMIZATION);
        WHITE_BOX.enqueueInitializerForCompilation(MyValue4.class, COMP_LEVEL_FULL_OPTIMIZATION);

        // Execute tests
        TreeMap<Long, String> durations = PRINT_TIMES ? new TreeMap<Long, String>() : null;
        for (Method test : tests.values()) {
            long startTime = System.nanoTime();
            Method verifier = getClass().getDeclaredMethod(test.getName() + "_verifier", boolean.class);
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
    int valid() default ValueTypeTestBench.AllFlags;
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

// Mark test as slow
@Retention(RetentionPolicy.RUNTIME)
@interface Slow { }
