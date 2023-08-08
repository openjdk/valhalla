/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @enablePreview
 * @summary Smoke test for parsing of bang types
 * @compile NullabilityParsingTest.java
 */

import java.util.function.*;
import java.util.*;

class NullabilityParsingTest {
    // fields
    Object! o2;

    // method parameters
    void m2(Object! o) { }

    // method returns
    Object! m2() { return new Object(); }

    // locals
    void testLocals() {
        Object! o2;
    }

    // generics - field
    Consumer<Object!> co2;

    // generics - method param
    void m4(Consumer<Object!> co) { }

    // generics - method return
    Consumer<Object!> m4() { return null; }

    // generics - local
    void testGenericLocals() {
        Consumer<Object!> co2;
    }

    // lambdas
    void testLambdas() {
        Consumer<Object!> co2 = (Object! co) -> {};
    }

    void testGenericLambdas() {
        Consumer<Consumer<Object!>> co2 = (Consumer<Object!> co) -> {};
        Consumer<Function<Object!, Object!>> co3 = (Function<Object!, Object!> co) -> {};
        Consumer<Consumer<Consumer<Consumer<Object!>>>> co6 = (Consumer<Consumer<Consumer<Object!>>> co) -> {};
    }

    // type test patterns

    void testTypeTestPatterns(Object o) {
        switch (o) {
            case Integer! i -> throw new AssertionError();
            case String! s -> throw new AssertionError();
            default -> throw new AssertionError();
        }
    }

    sealed interface I<X> {}
    final class A implements I<Integer> { }

    void genericTypeTestPatterns(A o) {
        switch (o) {
            case I<Integer!> i -> { }
        }
    }

    sealed interface I2<X> {}
    final class A2 implements I2<I<Integer>> { }

    void genericTypeTestPatterns(A2 o) {
        switch (o) {
            case I2<I<Integer!>> i -> { }
        }
    }

    sealed interface I3<X> {}
    final class A3 implements I3<I2<I<Integer>>> { }

    void genericTypeTestPatterns(A3 o) {
        switch (o) {
            case I3<I2<I<Integer!>>> i -> { }
        }
    }

    // record patterns

    record R(A a) { }

    void genericRecordPatterns(R o) {
        switch (o) {
            case R!(I<Integer!> i) -> { }
        }
    }

    record R2(A2 a2) { }

    void genericRecordPatterns(R2 o) {
        switch (o) {
            case R2!(I2<I<Integer!>> i) -> { }
        }
    }

    record R3(A3 a3) { }

    void genericRecordPatterns(R3 o) {
        switch (o) {
            case R3!(I3<I2<I<Integer!>>> i) -> { }
        }
    }

    // instanceof/cast

    void testInstanceOf(Object o) {
        boolean r2 = o instanceof String!;
    }

    void testInstanceRecord(R r) {
        boolean r2 = r instanceof R(I<Integer!> i);
    }

    void testCast(Object o) {
        String! s2 = (String!)o;
    }

    void testGenericCast(A a) {
        I<Integer!> i2 = (I<Integer!>)a;
    }

    void testGenericCast2(A a) {
        I!<Integer!> i2 = (I!<Integer!>)a;
    }

    // arrays

    Object![]![]![]! oarr;
    Function!<Object![]![]!, Function<Object![]![]!, Object![]![]!>>[]![]! garr;

    void mBad1(Object o) {
        String s1 = o instanceof String ? (String)o : null;
        String s2 = o instanceof String! ? (String)o : null;
    }

    void mBad2(Object o) {
        String s1 = o instanceof String ? "" : null;
        String s2 = o instanceof String! ? "" : null;
    }

    void testPatternRule(Object o) {
        switch (o) {
            case String! s -> { }
                default -> { }
        }
    }

    void testPatternCol(Object o) {
        switch (o) {
            case String! s: { }
            default: { }
        }
    }

    void testInstanceOfAndInfix1(Object a, boolean b) {
        boolean x2 = a instanceof String! && b;
    }

    void testInstanceOfAndInfix2(Object a, boolean b) {
        boolean x2 = a instanceof String! s && b;
    }
}
