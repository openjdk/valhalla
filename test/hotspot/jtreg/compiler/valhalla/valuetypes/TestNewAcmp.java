/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test TestNewAcmp
 * @summary Verifies correctness of the new acmp bytecode.
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers TestNewAcmp.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbatch
 *                   -XX:+EnableValhalla -XX:TypeProfileLevel=222
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.valuetypes.TestNewAcmp::test*
 *                   compiler.valhalla.valuetypes.TestNewAcmp 0
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xbatch -XX:+EnableValhalla -XX:TypeProfileLevel=222
 *                   -XX:+AlwaysIncrementalInline
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.valuetypes.TestNewAcmp::test*
 *                   compiler.valhalla.valuetypes.TestNewAcmp 0
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbatch
 *                   -XX:+EnableValhalla -XX:TypeProfileLevel=222
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.valuetypes.TestNewAcmp::test*
 *                   compiler.valhalla.valuetypes.TestNewAcmp 1
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xbatch -XX:+EnableValhalla -XX:TypeProfileLevel=222
 *                   -XX:+AlwaysIncrementalInline
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.valuetypes.TestNewAcmp::test*
 *                   compiler.valhalla.valuetypes.TestNewAcmp 1
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbatch
 *                   -XX:+EnableValhalla -XX:TypeProfileLevel=222
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.valuetypes.TestNewAcmp::test*
 *                   compiler.valhalla.valuetypes.TestNewAcmp 2
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xbatch -XX:+EnableValhalla -XX:TypeProfileLevel=222
 *                   -XX:+AlwaysIncrementalInline
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.valuetypes.TestNewAcmp::test*
 *                   compiler.valhalla.valuetypes.TestNewAcmp 2
 */

package compiler.valhalla.valuetypes;

import jdk.test.lib.Asserts;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import sun.hotspot.WhiteBox;

interface MyInterface {

}

__ByValue class MyValue implements MyInterface {
    final int x = 42;

    static MyValue createDefault() {
        return __MakeDefault MyValue();
    }
}

class MyObject implements MyInterface {
    int x;
}

// Mark test methods that return always false
@Retention(RetentionPolicy.RUNTIME)
@interface AlwaysFalse { }

// Mark test methods that return always true
@Retention(RetentionPolicy.RUNTIME)
@interface AlwaysTrue { }

// Mark test methods that return false if the argument is null
@Retention(RetentionPolicy.RUNTIME)
@interface FalseIfNull { }

// Mark test methods that return true if the argument is null
@Retention(RetentionPolicy.RUNTIME)
@interface TrueIfNull { }

public class TestNewAcmp {

    public boolean testEq01_1(Object u1, Object u2) {
        return get(u1) == u2; // new acmp
    }

    public boolean testEq01_2(Object u1, Object u2) {
        return u1 == get(u2); // new acmp
    }

    public boolean testEq01_3(Object u1, Object u2) {
        return get(u1) == get(u2); // new acmp
    }

    @FalseIfNull
    public boolean testEq01_4(Object u1, Object u2) {
        return getNotNull(u1) == u2; // new acmp without null check
    }

    @FalseIfNull
    public boolean testEq01_5(Object u1, Object u2) {
        return u1 == getNotNull(u2); // new acmp without null check
    }

    @FalseIfNull
    public boolean testEq01_6(Object u1, Object u2) {
        return getNotNull(u1) == getNotNull(u2); // new acmp without null check
    }

    public boolean testEq02_1(MyValue v1, MyValue v2) {
        return get(v1) == (Object)v2; // only true if both null
    }

    public boolean testEq02_2(MyValue v1, MyValue v2) {
        return (Object)v1 == get(v2); // only true if both null
    }

    public boolean testEq02_3(MyValue v1, MyValue v2) {
        return get(v1) == get(v2); // only true if both null
    }

    public boolean testEq03_1(MyValue v, Object u) {
        return get(v) == u; // only true if both null
    }

    public boolean testEq03_2(MyValue v, Object u) {
        return (Object)v == get(u); // only true if both null
    }

    public boolean testEq03_3(MyValue v, Object u) {
        return get(v) == get(u); // only true if both null
    }

    public boolean testEq04_1(Object u, MyValue v) {
        return get(u) == (Object)v; // only true if both null
    }

    public boolean testEq04_2(Object u, MyValue v) {
        return u == get(v); // only true if both null
    }

    public boolean testEq04_3(Object u, MyValue v) {
        return get(u) == get(v); // only true if both null
    }

    public boolean testEq05_1(MyObject o, MyValue v) {
        return get(o) == (Object)v; // only true if both null
    }

    public boolean testEq05_2(MyObject o, MyValue v) {
        return o == get(v); // only true if both null
    }

    public boolean testEq05_3(MyObject o, MyValue v) {
        return get(o) == get(v); // only true if both null
    }

    public boolean testEq06_1(MyValue v, MyObject o) {
        return get(v) == o; // only true if both null
    }

    public boolean testEq06_2(MyValue v, MyObject o) {
        return (Object)v == get(o); // only true if both null
    }

    public boolean testEq06_3(MyValue v, MyObject o) {
        return get(v) == get(o); // only true if both null
    }

    @AlwaysFalse
    public boolean testEq07_1(MyValue v1, MyValue v2) {
        return getNotNull(v1) == (Object)v2; // false
    }

    @AlwaysFalse
    public boolean testEq07_2(MyValue v1, MyValue v2) {
        return (Object)v1 == getNotNull(v2); // false
    }

    @AlwaysFalse
    public boolean testEq07_3(MyValue v1, MyValue v2) {
        return getNotNull(v1) == getNotNull(v2); // false
    }

    @AlwaysFalse
    public boolean testEq08_1(MyValue v, Object u) {
        return getNotNull(v) == u; // false
    }

    @AlwaysFalse
    public boolean testEq08_2(MyValue v, Object u) {
        return (Object)v == getNotNull(u); // false
    }

    @AlwaysFalse
    public boolean testEq08_3(MyValue v, Object u) {
        return getNotNull(v) == getNotNull(u); // false
    }

    @AlwaysFalse
    public boolean testEq09_1(Object u, MyValue v) {
        return getNotNull(u) == (Object)v; // false
    }

    @AlwaysFalse
    public boolean testEq09_2(Object u, MyValue v) {
        return u == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq09_3(Object u, MyValue v) {
        return getNotNull(u) == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq10_1(MyObject o, MyValue v) {
        return getNotNull(o) == (Object)v; // false
    }

    @AlwaysFalse
    public boolean testEq10_2(MyObject o, MyValue v) {
        return o == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq10_3(MyObject o, MyValue v) {
        return getNotNull(o) == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq11_1(MyValue v, MyObject o) {
        return getNotNull(v) == o; // false
    }

    @AlwaysFalse
    public boolean testEq11_2(MyValue v, MyObject o) {
        return (Object)v == getNotNull(o); // false
    }

    @AlwaysFalse
    public boolean testEq11_3(MyValue v, MyObject o) {
        return getNotNull(v) == getNotNull(o); // false
    }

    public boolean testEq12_1(MyObject o1, MyObject o2) {
        return get(o1) == o2; // old acmp
    }

    public boolean testEq12_2(MyObject o1, MyObject o2) {
        return o1 == get(o2); // old acmp
    }

    public boolean testEq12_3(MyObject o1, MyObject o2) {
        return get(o1) == get(o2); // old acmp
    }

    public boolean testEq13_1(Object u, MyObject o) {
        return get(u) == o; // old acmp
    }

    public boolean testEq13_2(Object u, MyObject o) {
        return u == get(o); // old acmp
    }

    public boolean testEq13_3(Object u, MyObject o) {
        return get(u) == get(o); // old acmp
    }

    public boolean testEq14_1(MyObject o, Object u) {
        return get(o) == u; // old acmp
    }

    public boolean testEq14_2(MyObject o, Object u) {
        return o == get(u); // old acmp
    }

    public boolean testEq14_3(MyObject o, Object u) {
        return get(o) == get(u); // old acmp
    }

    public boolean testEq15_1(Object[] a, Object u) {
        return get(a) == u; // old acmp
    }

    public boolean testEq15_2(Object[] a, Object u) {
        return a == get(u); // old acmp
    }

    public boolean testEq15_3(Object[] a, Object u) {
        return get(a) == get(u); // old acmp
    }

    public boolean testEq16_1(Object u, Object[] a) {
        return get(u) == a; // old acmp
    }

    public boolean testEq16_2(Object u, Object[] a) {
        return u == get(a); // old acmp
    }

    public boolean testEq16_3(Object u, Object[] a) {
        return get(u) == get(a); // old acmp
    }

    public boolean testEq17_1(Object[] a, MyValue v) {
        return get(a) == (Object)v; // only true if both null
    }

    public boolean testEq17_2(Object[] a, MyValue v) {
        return a == get(v); // only true if both null
    }

    public boolean testEq17_3(Object[] a, MyValue v) {
        return get(a) == get(v); // only true if both null
    }

    public boolean testEq18_1(MyValue v, Object[] a) {
        return get(v) == a; // only true if both null
    }

    public boolean testEq18_2(MyValue v, Object[] a) {
        return (Object)v == get(a); // only true if both null
    }

    public boolean testEq18_3(MyValue v, Object[] a) {
        return get(v) == get(a); // only true if both null
    }

    @AlwaysFalse
    public boolean testEq19_1(Object[] a, MyValue v) {
        return getNotNull(a) == (Object)v; // false
    }

    @AlwaysFalse
    public boolean testEq19_2(Object[] a, MyValue v) {
        return a == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq19_3(Object[] a, MyValue v) {
        return getNotNull(a) == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq20_1(MyValue v, Object[] a) {
        return getNotNull(v) == a; // false
    }

    @AlwaysFalse
    public boolean testEq20_2(MyValue v, Object[] a) {
        return (Object)v == getNotNull(a); // false
    }

    @AlwaysFalse
    public boolean testEq20_3(MyValue v, Object[] a) {
        return getNotNull(v) == getNotNull(a); // false
    }

    public boolean testEq21_1(MyInterface u1, MyInterface u2) {
        return get(u1) == u2; // new acmp
    }

    public boolean testEq21_2(MyInterface u1, MyInterface u2) {
        return u1 == get(u2); // new acmp
    }

    public boolean testEq21_3(MyInterface u1, MyInterface u2) {
        return get(u1) == get(u2); // new acmp
    }

    @FalseIfNull
    public boolean testEq21_4(MyInterface u1, MyInterface u2) {
        return getNotNull(u1) == u2; // new acmp without null check
    }

    @FalseIfNull
    public boolean testEq21_5(MyInterface u1, MyInterface u2) {
        return u1 == getNotNull(u2); // new acmp without null check
    }

    @FalseIfNull
    public boolean testEq21_6(MyInterface u1, MyInterface u2) {
        return getNotNull(u1) == getNotNull(u2); // new acmp without null check
    }

    public boolean testEq22_1(MyValue v, MyInterface u) {
        return get(v) == u; // only true if both null
    }

    public boolean testEq22_2(MyValue v, MyInterface u) {
        return (Object)v == get(u); // only true if both null
    }

    public boolean testEq22_3(MyValue v, MyInterface u) {
        return get(v) == get(u); // only true if both null
    }

    public boolean testEq23_1(MyInterface u, MyValue v) {
        return get(u) == (Object)v; // only true if both null
    }

    public boolean testEq23_2(MyInterface u, MyValue v) {
        return u == get(v); // only true if both null
    }

    public boolean testEq23_3(MyInterface u, MyValue v) {
        return get(u) == get(v); // only true if both null
    }

    @AlwaysFalse
    public boolean testEq24_1(MyValue v, MyInterface u) {
        return getNotNull(v) == u; // false
    }

    @AlwaysFalse
    public boolean testEq24_2(MyValue v, MyInterface u) {
        return (Object)v == getNotNull(u); // false
    }

    @AlwaysFalse
    public boolean testEq24_3(MyValue v, MyInterface u) {
        return getNotNull(v) == getNotNull(u); // false
    }

    @AlwaysFalse
    public boolean testEq25_1(MyInterface u, MyValue v) {
        return getNotNull(u) == (Object)v; // false
    }

    @AlwaysFalse
    public boolean testEq25_2(MyInterface u, MyValue v) {
        return u == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq25_3(MyInterface u, MyValue v) {
        return getNotNull(u) == getNotNull(v); // false
    }

    public boolean testEq26_1(MyInterface u, MyObject o) {
        return get(u) == o; // old acmp
    }

    public boolean testEq26_2(MyInterface u, MyObject o) {
        return u == get(o); // old acmp
    }

    public boolean testEq26_3(MyInterface u, MyObject o) {
        return get(u) == get(o); // old acmp
    }

    public boolean testEq27_1(MyObject o, MyInterface u) {
        return get(o) == u; // old acmp
    }

    public boolean testEq27_2(MyObject o, MyInterface u) {
        return o == get(u); // old acmp
    }

    public boolean testEq27_3(MyObject o, MyInterface u) {
        return get(o) == get(u); // old acmp
    }

    public boolean testEq28_1(MyInterface[] a, MyInterface u) {
        return get(a) == u; // old acmp
    }

    public boolean testEq28_2(MyInterface[] a, MyInterface u) {
        return a == get(u); // old acmp
    }

    public boolean testEq28_3(MyInterface[] a, MyInterface u) {
        return get(a) == get(u); // old acmp
    }

    public boolean testEq29_1(MyInterface u, MyInterface[] a) {
        return get(u) == a; // old acmp
    }

    public boolean testEq29_2(MyInterface u, MyInterface[] a) {
        return u == get(a); // old acmp
    }

    public boolean testEq29_3(MyInterface u, MyInterface[] a) {
        return get(u) == get(a); // old acmp
    }

    public boolean testEq30_1(MyInterface[] a, MyValue v) {
        return get(a) == (Object)v; // only true if both null
    }

    public boolean testEq30_2(MyInterface[] a, MyValue v) {
        return a == get(v); // only true if both null
    }

    public boolean testEq30_3(MyInterface[] a, MyValue v) {
        return get(a) == get(v); // only true if both null
    }

    public boolean testEq31_1(MyValue v, MyInterface[] a) {
        return get(v) == a; // only true if both null
    }

    public boolean testEq31_2(MyValue v, MyInterface[] a) {
        return (Object)v == get(a); // only true if both null
    }

    public boolean testEq31_3(MyValue v, MyInterface[] a) {
        return get(v) == get(a); // only true if both null
    }

    @AlwaysFalse
    public boolean testEq32_1(MyInterface[] a, MyValue v) {
        return getNotNull(a) == (Object)v; // false
    }

    @AlwaysFalse
    public boolean testEq32_2(MyInterface[] a, MyValue v) {
        return a == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq32_3(MyInterface[] a, MyValue v) {
        return getNotNull(a) == getNotNull(v); // false
    }

    @AlwaysFalse
    public boolean testEq33_1(MyValue v, MyInterface[] a) {
        return getNotNull(v) == a; // false
    }

    @AlwaysFalse
    public boolean testEq33_2(MyValue v, MyInterface[] a) {
        return (Object)v == getNotNull(a); // false
    }

    @AlwaysFalse
    public boolean testEq33_3(MyValue v, MyInterface[] a) {
        return getNotNull(v) == getNotNull(a); // false
    }


    // Null tests

    public boolean testNull01_1(MyValue v) {
        return (Object)v == null; // old acmp
    }

    public boolean testNull01_2(MyValue v) {
        return get(v) == null; // old acmp
    }

    public boolean testNull01_3(MyValue v) {
        return (Object)v == get((Object)null); // old acmp
    }

    public boolean testNull01_4(MyValue v) {
        return get(v) == get((Object)null); // old acmp
    }

    public boolean testNull02_1(MyValue v) {
        return null == (Object)v; // old acmp
    }

    public boolean testNull02_2(MyValue v) {
        return get((Object)null) == (Object)v; // old acmp
    }

    public boolean testNull02_3(MyValue v) {
        return null == get(v); // old acmp
    }

    public boolean testNull02_4(MyValue v) {
        return get((Object)null) == get(v); // old acmp
    }

    public boolean testNull03_1(Object u) {
        return u == null; // old acmp
    }

    public boolean testNull03_2(Object u) {
        return get(u) == null; // old acmp
    }

    public boolean testNull03_3(Object u) {
        return u == get((Object)null); // old acmp
    }

    public boolean testNull03_4(Object u) {
        return get(u) == get((Object)null); // old acmp
    }

    public boolean testNull04_1(Object u) {
        return null == u; // old acmp
    }

    public boolean testNull04_2(Object u) {
        return get((Object)null) == u; // old acmp
    }

    public boolean testNull04_3(Object u) {
        return null == get(u); // old acmp
    }

    public boolean testNull04_4(Object u) {
        return get((Object)null) == get(u); // old acmp
    }

    public boolean testNull05_1(MyObject o) {
        return o == null; // old acmp
    }

    public boolean testNull05_2(MyObject o) {
        return get(o) == null; // old acmp
    }

    public boolean testNull05_3(MyObject o) {
        return o == get((Object)null); // old acmp
    }

    public boolean testNull05_4(MyObject o) {
        return get(o) == get((Object)null); // old acmp
    }

    public boolean testNull06_1(MyObject o) {
        return null == o; // old acmp
    }

    public boolean testNull06_2(MyObject o) {
        return get((Object)null) == o; // old acmp
    }

    public boolean testNull06_3(MyObject o) {
        return null == get(o); // old acmp
    }

    public boolean testNull06_4(MyObject o) {
        return get((Object)null) == get(o); // old acmp
    }

    public boolean testNull07_1(MyInterface u) {
        return u == null; // old acmp
    }

    public boolean testNull07_2(MyInterface u) {
        return get(u) == null; // old acmp
    }

    public boolean testNull07_3(MyInterface u) {
        return u == get((Object)null); // old acmp
    }

    public boolean testNull07_4(MyInterface u) {
        return get(u) == get((Object)null); // old acmp
    }

    public boolean testNull08_1(MyInterface u) {
        return null == u; // old acmp
    }

    public boolean testNull08_2(MyInterface u) {
        return get((Object)null) == u; // old acmp
    }

    public boolean testNull08_3(MyInterface u) {
        return null == get(u); // old acmp
    }

    public boolean testNull08_4(MyInterface u) {
        return get((Object)null) == get(u); // old acmp
    }

    // Same tests as above but negated

    public boolean testNotEq01_1(Object u1, Object u2) {
        return get(u1) != u2; // new acmp
    }

    public boolean testNotEq01_2(Object u1, Object u2) {
        return u1 != get(u2); // new acmp
    }

    public boolean testNotEq01_3(Object u1, Object u2) {
        return get(u1) != get(u2); // new acmp
    }

    @TrueIfNull
    public boolean testNotEq01_4(Object u1, Object u2) {
        return getNotNull(u1) != u2; // new acmp without null check
    }

    @TrueIfNull
    public boolean testNotEq01_5(Object u1, Object u2) {
        return u1 != getNotNull(u2); // new acmp without null check
    }

    @TrueIfNull
    public boolean testNotEq01_6(Object u1, Object u2) {
        return getNotNull(u1) != getNotNull(u2); // new acmp without null check
    }

    public boolean testNotEq02_1(MyValue v1, MyValue v2) {
        return get(v1) != (Object)v2; // only false if both null
    }

    public boolean testNotEq02_2(MyValue v1, MyValue v2) {
        return (Object)v1 != get(v2); // only false if both null
    }

    public boolean testNotEq02_3(MyValue v1, MyValue v2) {
        return get(v1) != get(v2); // only false if both null
    }

    public boolean testNotEq03_1(MyValue v, Object u) {
        return get(v) != u; // only false if both null
    }

    public boolean testNotEq03_2(MyValue v, Object u) {
        return (Object)v != get(u); // only false if both null
    }

    public boolean testNotEq03_3(MyValue v, Object u) {
        return get(v) != get(u); // only false if both null
    }

    public boolean testNotEq04_1(Object u, MyValue v) {
        return get(u) != (Object)v; // only false if both null
    }

    public boolean testNotEq04_2(Object u, MyValue v) {
        return u != get(v); // only false if both null
    }

    public boolean testNotEq04_3(Object u, MyValue v) {
        return get(u) != get(v); // only false if both null
    }

    public boolean testNotEq05_1(MyObject o, MyValue v) {
        return get(o) != (Object)v; // only false if both null
    }

    public boolean testNotEq05_2(MyObject o, MyValue v) {
        return o != get(v); // only false if both null
    }

    public boolean testNotEq05_3(MyObject o, MyValue v) {
        return get(o) != get(v); // only false if both null
    }

    public boolean testNotEq06_1(MyValue v, MyObject o) {
        return get(v) != o; // only false if both null
    }

    public boolean testNotEq06_2(MyValue v, MyObject o) {
        return (Object)v != get(o); // only false if both null
    }

    public boolean testNotEq06_3(MyValue v, MyObject o) {
        return get(v) != get(o); // only false if both null
    }

    @AlwaysTrue
    public boolean testNotEq07_1(MyValue v1, MyValue v2) {
        return getNotNull(v1) != (Object)v2; // true
    }

    @AlwaysTrue
    public boolean testNotEq07_2(MyValue v1, MyValue v2) {
        return (Object)v1 != getNotNull(v2); // true
    }

    @AlwaysTrue
    public boolean testNotEq07_3(MyValue v1, MyValue v2) {
        return getNotNull(v1) != getNotNull(v2); // true
    }

    @AlwaysTrue
    public boolean testNotEq08_1(MyValue v, Object u) {
        return getNotNull(v) != u; // true
    }

    @AlwaysTrue
    public boolean testNotEq08_2(MyValue v, Object u) {
        return (Object)v != getNotNull(u); // true
    }

    @AlwaysTrue
    public boolean testNotEq08_3(MyValue v, Object u) {
        return getNotNull(v) != getNotNull(u); // true
    }

    @AlwaysTrue
    public boolean testNotEq09_1(Object u, MyValue v) {
        return getNotNull(u) != (Object)v; // true
    }

    @AlwaysTrue
    public boolean testNotEq09_2(Object u, MyValue v) {
        return u != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq09_3(Object u, MyValue v) {
        return getNotNull(u) != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq10_1(MyObject o, MyValue v) {
        return getNotNull(o) != (Object)v; // true
    }

    @AlwaysTrue
    public boolean testNotEq10_2(MyObject o, MyValue v) {
        return o != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq10_3(MyObject o, MyValue v) {
        return getNotNull(o) != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq11_1(MyValue v, MyObject o) {
        return getNotNull(v) != o; // true
    }

    @AlwaysTrue
    public boolean testNotEq11_2(MyValue v, MyObject o) {
        return (Object)v != getNotNull(o); // true
    }

    @AlwaysTrue
    public boolean testNotEq11_3(MyValue v, MyObject o) {
        return getNotNull(v) != getNotNull(o); // true
    }

    public boolean testNotEq12_1(MyObject o1, MyObject o2) {
        return get(o1) != o2; // old acmp
    }

    public boolean testNotEq12_2(MyObject o1, MyObject o2) {
        return o1 != get(o2); // old acmp
    }

    public boolean testNotEq12_3(MyObject o1, MyObject o2) {
        return get(o1) != get(o2); // old acmp
    }

    public boolean testNotEq13_1(Object u, MyObject o) {
        return get(u) != o; // old acmp
    }

    public boolean testNotEq13_2(Object u, MyObject o) {
        return u != get(o); // old acmp
    }

    public boolean testNotEq13_3(Object u, MyObject o) {
        return get(u) != get(o); // old acmp
    }

    public boolean testNotEq14_1(MyObject o, Object u) {
        return get(o) != u; // old acmp
    }

    public boolean testNotEq14_2(MyObject o, Object u) {
        return o != get(u); // old acmp
    }

    public boolean testNotEq14_3(MyObject o, Object u) {
        return get(o) != get(u); // old acmp
    }

    public boolean testNotEq15_1(Object[] a, Object u) {
        return get(a) != u; // old acmp
    }

    public boolean testNotEq15_2(Object[] a, Object u) {
        return a != get(u); // old acmp
    }

    public boolean testNotEq15_3(Object[] a, Object u) {
        return get(a) != get(u); // old acmp
    }

    public boolean testNotEq16_1(Object u, Object[] a) {
        return get(u) != a; // old acmp
    }

    public boolean testNotEq16_2(Object u, Object[] a) {
        return u != get(a); // old acmp
    }

    public boolean testNotEq16_3(Object u, Object[] a) {
        return get(u) != get(a); // old acmp
    }

    public boolean testNotEq17_1(Object[] a, MyValue v) {
        return get(a) != (Object)v; // only false if both null
    }

    public boolean testNotEq17_2(Object[] a, MyValue v) {
        return a != get(v); // only false if both null
    }

    public boolean testNotEq17_3(Object[] a, MyValue v) {
        return get(a) != get(v); // only false if both null
    }

    public boolean testNotEq18_1(MyValue v, Object[] a) {
        return get(v) != a; // only false if both null
    }

    public boolean testNotEq18_2(MyValue v, Object[] a) {
        return (Object)v != get(a); // only false if both null
    }

    public boolean testNotEq18_3(MyValue v, Object[] a) {
        return get(v) != get(a); // only false if both null
    }

    @AlwaysTrue
    public boolean testNotEq19_1(Object[] a, MyValue v) {
        return getNotNull(a) != (Object)v; // true
    }

    @AlwaysTrue
    public boolean testNotEq19_2(Object[] a, MyValue v) {
        return a != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq19_3(Object[] a, MyValue v) {
        return getNotNull(a) != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq20_1(MyValue v, Object[] a) {
        return getNotNull(v) != a; // true
    }

    @AlwaysTrue
    public boolean testNotEq20_2(MyValue v, Object[] a) {
        return (Object)v != getNotNull(a); // true
    }

    @AlwaysTrue
    public boolean testNotEq20_3(MyValue v, Object[] a) {
        return getNotNull(v) != getNotNull(a); // true
    }

    public boolean testNotEq21_1(MyInterface u1, MyInterface u2) {
        return get(u1) != u2; // new acmp
    }

    public boolean testNotEq21_2(MyInterface u1, MyInterface u2) {
        return u1 != get(u2); // new acmp
    }

    public boolean testNotEq21_3(MyInterface u1, MyInterface u2) {
        return get(u1) != get(u2); // new acmp
    }

    @TrueIfNull
    public boolean testNotEq21_4(MyInterface u1, MyInterface u2) {
        return getNotNull(u1) != u2; // new acmp without null check
    }

    @TrueIfNull
    public boolean testNotEq21_5(MyInterface u1, MyInterface u2) {
        return u1 != getNotNull(u2); // new acmp without null check
    }

    @TrueIfNull
    public boolean testNotEq21_6(MyInterface u1, MyInterface u2) {
        return getNotNull(u1) != getNotNull(u2); // new acmp without null check
    }

    public boolean testNotEq22_1(MyValue v, MyInterface u) {
        return get(v) != u; // only false if both null
    }

    public boolean testNotEq22_2(MyValue v, MyInterface u) {
        return (Object)v != get(u); // only false if both null
    }

    public boolean testNotEq22_3(MyValue v, MyInterface u) {
        return get(v) != get(u); // only false if both null
    }

    public boolean testNotEq23_1(MyInterface u, MyValue v) {
        return get(u) != (Object)v; // only false if both null
    }

    public boolean testNotEq23_2(MyInterface u, MyValue v) {
        return u != get(v); // only false if both null
    }

    public boolean testNotEq23_3(MyInterface u, MyValue v) {
        return get(u) != get(v); // only false if both null
    }

    @AlwaysTrue
    public boolean testNotEq24_1(MyValue v, MyInterface u) {
        return getNotNull(v) != u; // true
    }

    @AlwaysTrue
    public boolean testNotEq24_2(MyValue v, MyInterface u) {
        return (Object)v != getNotNull(u); // true
    }

    @AlwaysTrue
    public boolean testNotEq24_3(MyValue v, MyInterface u) {
        return getNotNull(v) != getNotNull(u); // true
    }

    @AlwaysTrue
    public boolean testNotEq25_1(MyInterface u, MyValue v) {
        return getNotNull(u) != (Object)v; // true
    }

    @AlwaysTrue
    public boolean testNotEq25_2(MyInterface u, MyValue v) {
        return u != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq25_3(MyInterface u, MyValue v) {
        return getNotNull(u) != getNotNull(v); // true
    }

    public boolean testNotEq26_1(MyInterface u, MyObject o) {
        return get(u) != o; // old acmp
    }

    public boolean testNotEq26_2(MyInterface u, MyObject o) {
        return u != get(o); // old acmp
    }

    public boolean testNotEq26_3(MyInterface u, MyObject o) {
        return get(u) != get(o); // old acmp
    }

    public boolean testNotEq27_1(MyObject o, MyInterface u) {
        return get(o) != u; // old acmp
    }

    public boolean testNotEq27_2(MyObject o, MyInterface u) {
        return o != get(u); // old acmp
    }

    public boolean testNotEq27_3(MyObject o, MyInterface u) {
        return get(o) != get(u); // old acmp
    }

    public boolean testNotEq28_1(MyInterface[] a, MyInterface u) {
        return get(a) != u; // old acmp
    }

    public boolean testNotEq28_2(MyInterface[] a, MyInterface u) {
        return a != get(u); // old acmp
    }

    public boolean testNotEq28_3(MyInterface[] a, MyInterface u) {
        return get(a) != get(u); // old acmp
    }

    public boolean testNotEq29_1(MyInterface u, MyInterface[] a) {
        return get(u) != a; // old acmp
    }

    public boolean testNotEq29_2(MyInterface u, MyInterface[] a) {
        return u != get(a); // old acmp
    }

    public boolean testNotEq29_3(MyInterface u, MyInterface[] a) {
        return get(u) != get(a); // old acmp
    }

    public boolean testNotEq30_1(MyInterface[] a, MyValue v) {
        return get(a) != (Object)v; // only false if both null
    }

    public boolean testNotEq30_2(MyInterface[] a, MyValue v) {
        return a != get(v); // only false if both null
    }

    public boolean testNotEq30_3(MyInterface[] a, MyValue v) {
        return get(a) != get(v); // only false if both null
    }

    public boolean testNotEq31_1(MyValue v, MyInterface[] a) {
        return get(v) != a; // only false if both null
    }

    public boolean testNotEq31_2(MyValue v, MyInterface[] a) {
        return (Object)v != get(a); // only false if both null
    }

    public boolean testNotEq31_3(MyValue v, MyInterface[] a) {
        return get(v) != get(a); // only false if both null
    }

    @AlwaysTrue
    public boolean testNotEq32_1(MyInterface[] a, MyValue v) {
        return getNotNull(a) != (Object)v; // true
    }

    @AlwaysTrue
    public boolean testNotEq32_2(MyInterface[] a, MyValue v) {
        return a != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq32_3(MyInterface[] a, MyValue v) {
        return getNotNull(a) != getNotNull(v); // true
    }

    @AlwaysTrue
    public boolean testNotEq33_1(MyValue v, MyInterface[] a) {
        return getNotNull(v) != a; // true
    }

    @AlwaysTrue
    public boolean testNotEq33_2(MyValue v, MyInterface[] a) {
        return (Object)v != getNotNull(a); // true
    }

    @AlwaysTrue
    public boolean testNotEq33_3(MyValue v, MyInterface[] a) {
        return getNotNull(v) != getNotNull(a); // true
    }

    // Null tests

    public boolean testNotNull01_1(MyValue v) {
        return (Object)v != null; // old acmp
    }

    public boolean testNotNull01_2(MyValue v) {
        return get(v) != null; // old acmp
    }

    public boolean testNotNull01_3(MyValue v) {
        return (Object)v != get((Object)null); // old acmp
    }

    public boolean testNotNull01_4(MyValue v) {
        return get(v) != get((Object)null); // old acmp
    }

    public boolean testNotNull02_1(MyValue v) {
        return null != (Object)v; // old acmp
    }

    public boolean testNotNull02_2(MyValue v) {
        return get((Object)null) != (Object)v; // old acmp
    }

    public boolean testNotNull02_3(MyValue v) {
        return null != get(v); // old acmp
    }

    public boolean testNotNull02_4(MyValue v) {
        return get((Object)null) != get(v); // old acmp
    }

    public boolean testNotNull03_1(Object u) {
        return u != null; // old acmp
    }

    public boolean testNotNull03_2(Object u) {
        return get(u) != null; // old acmp
    }

    public boolean testNotNull03_3(Object u) {
        return u != get((Object)null); // old acmp
    }

    public boolean testNotNull03_4(Object u) {
        return get(u) != get((Object)null); // old acmp
    }

    public boolean testNotNull04_1(Object u) {
        return null != u; // old acmp
    }

    public boolean testNotNull04_2(Object u) {
        return get((Object)null) != u; // old acmp
    }

    public boolean testNotNull04_3(Object u) {
        return null != get(u); // old acmp
    }

    public boolean testNotNull04_4(Object u) {
        return get((Object)null) != get(u); // old acmp
    }

    public boolean testNotNull05_1(MyObject o) {
        return o != null; // old acmp
    }

    public boolean testNotNull05_2(MyObject o) {
        return get(o) != null; // old acmp
    }

    public boolean testNotNull05_3(MyObject o) {
        return o != get((Object)null); // old acmp
    }

    public boolean testNotNull05_4(MyObject o) {
        return get(o) != get((Object)null); // old acmp
    }

    public boolean testNotNull06_1(MyObject o) {
        return null != o; // old acmp
    }

    public boolean testNotNull06_2(MyObject o) {
        return get((Object)null) != o; // old acmp
    }

    public boolean testNotNull06_3(MyObject o) {
        return null != get(o); // old acmp
    }

    public boolean testNotNull06_4(MyObject o) {
        return get((Object)null) != get(o); // old acmp
    }

    public boolean testNotNull07_1(MyInterface u) {
        return u != null; // old acmp
    }

    public boolean testNotNull07_2(MyInterface u) {
        return get(u) != null; // old acmp
    }

    public boolean testNotNull07_3(MyInterface u) {
        return u != get((Object)null); // old acmp
    }

    public boolean testNotNull07_4(MyInterface u) {
        return get(u) != get((Object)null); // old acmp
    }

    public boolean testNotNull08_1(MyInterface u) {
        return null != u; // old acmp
    }

    public boolean testNotNull08_2(MyInterface u) {
        return get((Object)null) != u; // old acmp
    }

    public boolean testNotNull08_3(MyInterface u) {
        return null != get(u); // old acmp
    }

    public boolean testNotNull08_4(MyInterface u) {
        return get((Object)null) != get(u); // old acmp
    }

    // The following methods are used with -XX:+AlwaysIncrementalInline to hide exact types during parsing

    public Object get(Object u) {
        return u;
    }

    public Object getNotNull(Object u) {
        return (u != null) ? u : new Object();
    }

    public Object get(MyValue v) {
        return v;
    }

    public Object getNotNull(MyValue v) {
        return ((Object)v != null) ? v : MyValue.createDefault();
    }

    public Object get(MyObject o) {
        return o;
    }

    public Object getNotNull(MyObject o) {
        return (o != null) ? o : MyValue.createDefault();
    }

    public Object get(Object[] a) {
        return a;
    }

    public Object getNotNull(Object[] a) {
        return (a != null) ? a : new Object[1];
    }

    public boolean trueIfNull(Method m) {
        return m.isAnnotationPresent(TrueIfNull.class);
    }

    public boolean falseIfNull(Method m) {
        return m.isAnnotationPresent(FalseIfNull.class);
    }

    public boolean alwaysTrue(Method m) {
        return m.isAnnotationPresent(AlwaysTrue.class);
    }

    public boolean alwaysFalse(Method m) {
        return m.isAnnotationPresent(AlwaysFalse.class);
    }

    public boolean isNegated(Method m) {
        return m.getName().startsWith("testNot");
    }

    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    protected static final int COMP_LEVEL_FULL_OPTIMIZATION = 4;

    public void runTest(Method m, Object[] args, int warmup, int nullMode) throws Exception {
        Class<?>[] parameterTypes = m.getParameterTypes();
        int parameterCount = parameterTypes.length;
        // Nullness mode for first argument
        // 0: default, 1: never null, 2: always null
        int start = (nullMode != 1) ? 0 : 1;
        int end = (nullMode != 2) ? args.length : 1;
        for (int i = start; i < end; ++i) {
            if (args[i] != null && !parameterTypes[0].isInstance(args[i])) {
                continue;
            }
            if (parameterCount == 1) {
                // Null checks
                System.out.print("Testing " + m.getName() + "(" + args[i] + ")");
                // Avoid acmp in the computation of the expected result!
                boolean expected = isNegated(m) ? (i != 0) : (i == 0);
                for (int run = 0; run < warmup; ++run) {
                    Boolean result = (Boolean)m.invoke(this, args[i]);
                    if (result != expected) {
                        System.out.println(" = " + result);
                        throw new RuntimeException("Test failed: should return " + expected);
                    }
                }
                System.out.println(" = " + expected);
            } else {
                // Equality checks
                for (int j = 0; j < args.length; ++j) {
                    if (args[j] != null && !parameterTypes[1].isInstance(args[j])) {
                        continue;
                    }
                    System.out.print("Testing " + m.getName() + "(" + args[i] + ", " + args[j] + ")");
                    // Avoid acmp in the computation of the expected result!
                    boolean equal = (i == j) && (i != 3);
                    equal = isNegated(m) ? !equal : equal;
                    boolean expected = alwaysTrue(m) || ((i == 0 || j == 0) && trueIfNull(m)) || (!alwaysFalse(m) && equal && !(i == 0 && falseIfNull(m)));
                    for (int run = 0; run < warmup; ++run) {
                        Boolean result = (Boolean)m.invoke(this, args[i], args[j]);
                        if (result != expected) {
                            System.out.println(" = " + result);
                            throw new RuntimeException("Test failed: should return " + expected);
                        }
                    }
                    System.out.println(" = " + expected);
                }
            }
        }
    }

    public void run(int nullMode) throws Exception {
        // Prepare test arguments
        Object[] args = new Object[6];
        args[0] = null;
        args[1] = new Object();
        args[2] = new MyObject();
        args[3] = MyValue.createDefault();
        args[4] = new Object[10];
        args[5] = new MyObject[10];

        // Run tests
        for (Method m : getClass().getMethods()) {
            if (m.getName().startsWith("test")) {
                // Do same warmup runs
                runTest(m, args, 1000, nullMode);
                // Make sure method is compiled
                WHITE_BOX.enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
                Asserts.assertTrue(WHITE_BOX.isMethodCompiled(m, false), m + " not compiled");
                // Run again to verify correctness of compiled code
                runTest(m, args, 1, nullMode);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int nullMode = Integer.valueOf(args[0]);
        TestNewAcmp t = new TestNewAcmp();
        t.run(nullMode);
    }
}

