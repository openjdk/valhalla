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
 * @compile -XDenableNullRestrictedTypes NullabilityParsingTest.java
 */

import java.util.function.*;
import java.util.*;

class NullabilityParsingTest {
    static value class Point { public implicit Point(); }
    static value class Shape { public implicit Shape(); }
    // fields
    Point! o2;

    // method parameters
    void m2(Point! o) { }

    // method returns
    Point! m2() { return new Point(); }

    // locals
    void testLocals() {
        Point! o2;
    }

    // generics - field
    Consumer<Point!> co2;

    // generics - method param
    void m4(Consumer<Point!> co) { }

    // generics - method return
    Consumer<Point!> m4() { return null; }

    // generics - local
    void testGenericLocals() {
        Consumer<Point!> co2;
    }

    // lambdas
    void testLambdas() {
        Consumer<Point!> co2 = (Point! co) -> {};
    }

    void testGenericLambdas() {
        Consumer<Consumer<Point!>> co2 = (Consumer<Point!> co) -> {};
        Consumer<Function<Point!, Point!>> co3 = (Function<Point!, Point!> co) -> {};
        Consumer<Consumer<Consumer<Consumer<Point!>>>> co6 = (Consumer<Consumer<Consumer<Point!>>> co) -> {};
    }

    // type test patterns

    void testTypeTestPatterns(Object o) {
        switch (o) {
            case Point! i -> throw new AssertionError();
            case Shape! s -> throw new AssertionError();
            default -> throw new AssertionError();
        }
    }

    sealed interface I<X> {}
    final class A implements I<Point> { }

    void genericTypeTestPatterns(A o) {
        switch (o) {
            case I<Point!> i -> { }
        }
    }

    sealed interface I2<X> {}
    final class A2 implements I2<I<Point>> { }

    void genericTypeTestPatterns(A2 o) {
        switch (o) {
            case I2<I<Point!>> i -> { }
        }
    }

    sealed interface I3<X> {}
    final class A3 implements I3<I2<I<Point>>> { }

    void genericTypeTestPatterns(A3 o) {
        switch (o) {
            case I3<I2<I<Point!>>> i -> { }
        }
    }

    // record patterns

    record R(A a) { }

    void genericRecordPatterns(R o) {
        switch (o) {
            case R!(I<Point!> i) -> { }
        }
    }

    record R2(A2 a2) { }

    void genericRecordPatterns(R2 o) {
        switch (o) {
            case R2!(I2<I<Point!>> i) -> { }
        }
    }

    record R3(A3 a3) { }

    void genericRecordPatterns(R3 o) {
        switch (o) {
            case R3!(I3<I2<I<Point!>>> i) -> { }
        }
    }

    // instanceof/cast

    void testInstanceOf(Object o) {
        boolean r2 = o instanceof Point!;
    }

    void testInstanceRecord(R r) {
        boolean r2 = r instanceof R(I<Point!> i);
    }

    void testCast(Object o) {
        Point! s2 = (Point!)o;
    }

    void testGenericCast(A a) {
        I<Point!> i2 = (I<Point!>)a;
    }
/*
    void testGenericCast2(A a) {
        I<Point!> i2 = (I<Point!>)a;
    }
*/
    // arrays

    Point![]![]![]! oarr;
    Function<Point![]![]!, Function<Point![]![]!, Point![]![]!>>[][] garr;

    void mBad1(Object o) {
        Point s1 = o instanceof Point ? (Point)o : null;
        Point s2 = o instanceof Point! ? (Point)o : null;
    }

    void mBad2(Object o) {
        Point s1 = o instanceof Point ? null : null;
        Point s2 = o instanceof Point! ? null : null;
    }

    void testPatternRule(Object o) {
        switch (o) {
            case Point! s -> { }
                default -> { }
        }
    }

    void testPatternCol(Object o) {
        switch (o) {
            case Point! s: { }
            default: { }
        }
    }

    void testInstanceOfAndInfix1(Object a, boolean b) {
        boolean x2 = a instanceof Point! && b;
    }

    void testInstanceOfAndInfix2(Object a, boolean b) {
        boolean x2 = a instanceof Point! s && b;
    }
}
