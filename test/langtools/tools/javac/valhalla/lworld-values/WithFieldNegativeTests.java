/*
 * @test /nodynamiccopyright/
 * @summary Check various withfield constrains
 *
 * @compile/fail/ref=WithFieldNegativeTests.out -XDrawDiagnostics WithFieldNegativeTests.java
 */

inline final class A {
    final int x = 10;
    static final int sx = 10;

    inline final class B {

        final A a = A.default;

        void foo(A a) {
            a.x = 100;
            a.sx = 100;
        }
    }

    void withfield(B b) {
            b.a.x = 11;
    }

    void foo(A a, final A fa) {
        a.x = 100;
        (a).x = 100;
        fa.x = 100;
        x = 100;
        this.x = 100;
        A.this.x = 100;
    }
}

class C {
    void foo(A a) {
        a.x = 100;
        a.sx = 100;
    }
}
