/*
 * @test /nodynamiccopyright/
 * @bug 8267835
 * @summary  Javac tolerates vacuous chaining to super constructor from primitive class constructor
 * @compile/fail/ref=SuperCallInCtor.out -XDrawDiagnostics SuperCallInCtor.java
 */

final class SuperCallInCtor {

    primitive class P {
       // generated ctor with super() call is OK.
    }

    primitive class Q {
        Q() {
            System.out.println("Construct Q"); // Ok, no express super();
        }

        Q(int x) {
            this();   // chaining with this is OK.
        }

        Q(String s) {
            super();  // Error.
        }
    }
}
