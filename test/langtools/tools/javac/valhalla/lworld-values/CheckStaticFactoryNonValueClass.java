/*
 * @test /nodynamiccopyright/
 * @summary Check that a value factory may not be declared for a non-value class
 * @compile/fail/ref=CheckStaticFactoryNonValueClass.out -XDrawDiagnostics -XDdev CheckStaticFactoryNonValueClass.java
 */

final class CheckStaticFactoryNonValueClass {
    final int x;

    __ValueFactory CheckStaticFactoryNonValueClass() { // error
        x = 10;
    }

    static __ValueFactory CheckStaticFactoryNonValueClass foo() { // error
       CheckStaticFactoryNonValueClass x = new CheckStaticFactoryNonValueClass();
       x.x = 100; // error.
       return x;
    }
}
