/*
 * @test /nodynamiccopyright/
 * @bug 8279901
 * @summary Javac should verify/ensure that a Functional interface proclaims neither identity nor valueness
 * @compile/fail/ref=FunctionalInterfaceTest.out -XDrawDiagnostics -XDdev FunctionalInterfaceTest.java
 */

public class FunctionalInterfaceTest {

    @FunctionalInterface
    identity interface I { // Error
        void m();
    }

    @FunctionalInterface
    interface J extends I  {} // Error.

    @FunctionalInterface
    value interface K { // Error
        void m();
    }

    identity interface L {
        void m();
    }

    value interface M {
        void m();
    }

    void foo() {
        var t = (L) () -> {}; // Error
        var u = (M) () -> {}; // Error
    }
}
