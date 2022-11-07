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

/*
 * @test
 * @compile -XDenablePrimitiveClasses RecursiveValueClass.java
 * @run junit/othervm -Xint -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Djdk.value.recursion.threshold=100000 RecursiveValueClass
 */

/*
 * @ignore 8296056
 * @test
 * @compile -XDenablePrimitiveClasses RecursiveValueClass.java
 * @run junit/othervm -XX:TieredStopAtLevel=1 -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Djdk.value.recursion.threshold=100000 RecursiveValueClass
 */

/*
 * @ignore 8296056
 * @test
 * @compile -XDenablePrimitiveClasses RecursiveValueClass.java
 * @run junit/othervm -Xcomp -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Djdk.value.recursion.threshold=100000 RecursiveValueClass
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RecursiveValueClass {
    static value class Node {
        Node left;
        Node right;

        Node(Node l, Node r) {
            this.left = l;
            this.right = r;
        }
    }

    static primitive class P {
        Node node;
        V v;
        P(Node node) {
            this.node = node;
            this.v = null;
        }
    }

    static value class V {
        P.ref p;
        V() {
            this.p = null;
        }
    }

    static value class A {
        B b;
        E e;
        A(B b, E e) {
            this.b = b;
            this.e = e;
        }
    }
    static value class B {
        C c;
        D d;
        B(C c, D d) {
            this.c = c;
            this.d = d;
        }
    }
    static value class C {
        A a;
        C(A a) {
            this.a = a;
        }
    }
    static value class D {
        int x;
        D(int x) {
            this.x = x;
        }
    }
    static value class E {
        F f;
        E(F f) {
            this.f = f;
        }
    }
    static value class F {
        E e;
        F(E e) {
            this.e = e;
        }
    }

    static Stream<Arguments> objectsProvider() {
        var n1 = Node.default;
        var n2 = new Node(n1, null);
        var n3 = new Node(n2, n1);
        var n4 = new Node(n2, n1);
        var p1 = new P(n3);
        var p2 = new P(n4);

        var e1 = new E(F.default);
        var f1 = new F(e1);
        var e2 = new E(f1);
        var f2 = new F(e2);

        var a = new A(B.default, E.default);

        var d1 = new D(1);
        var d2 = new D(2);
        var c1 = new C(a);
        var c2 = new C(a);

        var b1 = new B(c1, d1);
        var b2 = new B(c1, d2);
        var b3 = new B(c2, d2);
        var a1 = new A(b1, e1);
        var a2 = new A(b2, e2);

        return Stream.of(
                // Node -> Node left & right
                Arguments.of(n1, n1,     true),
                Arguments.of(n1, n2,     false),
                Arguments.of(n2, n3,     false),
                Arguments.of(n3, n4,     true),
                Arguments.of(null, n4,   false),
                Arguments.of(null, null, true),
                Arguments.of(n1, "foo",  false),

                // primitive class P -> value class V -> P.ref
                Arguments.of(p1, p2,     true),

                // E -> F -> E
                Arguments.of(e1, e2,     false),
                Arguments.of(e1, e1,     true),
                Arguments.of(f1, f2,     false),
                Arguments.of(f2, f2,     true),

                // two cyclic memberships from A
                // A -> B -> C -> A and E -> F -> E
                Arguments.of(a1, a2,     false),
                Arguments.of(a2, a2,     true),
                Arguments.of(b1, b2,     false),
                Arguments.of(b2, b3,     true),
                Arguments.of(c1, c2,     true)
        );
    }

    @ParameterizedTest
    @MethodSource("objectsProvider")
    public void testAcmp(Object o1, Object o2, boolean expected) {
        var value = o1 == o2;
        assertEquals(expected, value, o1 + " == " + o2);
    }

    static Stream<Arguments> hashCodeProvider() {
        var n1 = Node.default;
        var n2 = new Node(n1, null);
        var n3 = new Node(n2, n1);
        var p1 = new P(n3);

        var e1 = new E(F.default);
        var f1 = new F(e1);
        var e2 = new E(f1);
        var f2 = new F(e2);

        var a = new A(B.default, E.default);

        var d1 = new D(1);
        var d2 = new D(2);
        var c1 = new C(a);

        var b1 = new B(c1, d1);
        var b2 = new B(c1, d2);
        var a1 = new A(b1, e1);
        var a2 = new A(b2, e2);

        return Stream.of(
                // Node -> Node left & right
                Arguments.of(n1),
                Arguments.of(n2),
                Arguments.of(n3),


                // primitive class P -> value class V -> P.ref
                Arguments.of(p1),

                // E -> F -> E
                Arguments.of(e1),
                Arguments.of(e2),
                Arguments.of(f1),
                Arguments.of(f2),

                // two cyclic memberships from A
                // A -> B -> C -> A and E -> F -> E
                Arguments.of(a1),
                Arguments.of(a2),
                Arguments.of(b1),
                Arguments.of(b2),
                Arguments.of(c1)
        );
    }

    @ParameterizedTest
    @MethodSource("hashCodeProvider")
    public void testHashCode(Object o) {
        int hc = o.hashCode();
        assertEquals(System.identityHashCode(o), hc, o.toString());
    }

    static value class N {
        N l;
        N r;
        int id;
        N(N l, N r, int id) {
            this.l = l;
            this.r = r;
            this.id = id;
        }
    }

    private static N build() {
        N n1 = new N(null, null, 0);
        N n2 = new N(null, null, 0);
        for (int id = 1; id < 100; ++id) {
            N l = new N(n1, n2, id);
            N r = new N(n1, n2, id);
            n1 = l;
            n2 = r;
        }
        return new N(n1, n2, 100);
    }

    /*
     * Throw SOE for large graph
     */
    @Test
    public void largeGraph() {
        N node = build();
        long start = System.nanoTime();
        assertThrows(StackOverflowError.class, () -> { boolean v = node.l == node.r; });
        assertThrows(StackOverflowError.class, () -> { int hc = node.hashCode(); });
        System.out.format("testing large graph: %d ms%n", (System.nanoTime() - start) / 1000);
    }
}