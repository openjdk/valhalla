/*
 * @test /nodynamiccopyright/
 * @summary Check various withfield constrains
 *
 * @compile/fail/ref=WithFieldNegativeTests.out -XDrawDiagnostics WithFieldNegativeTests.java
 */

__ByValue final class A {
    final int x = 10;
    static final int sx = 10;

    __ByValue final class B {

        final A a = __MakeDefault A();

        void foo(A a) {
            a.x = 100; // OK, same nest.
            a.sx = 100; // Error.
        }
    }

    void withfield(B b) {
            b.a.x = 11; // Error, at least for now.
    }

    void foo(A a, final A fa) {
        a.x = 100; // OK.
        (a).x = 100; // OK.
        fa.x = 100; // Error.
        x = 100;  // Error, this is const.
        this.x = 100; // Error.
        A.this.x = 100; // Error.
    }
}

class C {
    void foo(A a) {
        a.x = 100; // Not OK.
        a.sx = 100; // Error
    }
}
