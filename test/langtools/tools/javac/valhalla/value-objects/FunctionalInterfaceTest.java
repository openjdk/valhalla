/*
 * @test /nodynamiccopyright/
 * @bug 8279901
 * @summary Javac should verify/ensure that a Functional interface implements neither IdentityObject nor ValueObject
 * @compile/fail/ref=FunctionalInterfaceTest.out -XDrawDiagnostics -XDdev FunctionalInterfaceTest.java
 */

public class FunctionalInterfaceTest {

    @FunctionalInterface
    interface I extends IdentityObject  { // Error
        void m();
    }

    @FunctionalInterface
    interface J extends I  {} // Error.

    @FunctionalInterface
    interface K extends ValueObject  { // Error
        void m();
    }

    interface L extends IdentityObject {
        void m();
    }

    interface M extends ValueObject {
        void m();
    }

    void foo() {
        var t = (L) () -> {}; // Error
        var u = (M) () -> {}; // Error
    }
}
