/*
 * @test /nodynamiccopyright/
 * @summary negative tests for bad provides declarations
 * @compile/fail/ref=BadWitnessDecls.out -XDrawDiagnostics BadWitnessDecls.java
 */

import java.util.List;

public class BadWitnessDecls {
    interface Foo<X> {
        __witness List<String> W1 = List.of(); // ok, but no reference to Foo
        __witness Foo<? extends String> W2 = new Foo<String>() { }; // error, wildcards
    }

    static class Bar<X> {
        __witness Bar<String> W3 = new Bar<String>() { }; // error, not an interface
    }

    interface Barf {
        __witness Barf W4 = new Barf() { }; // error, not generic
    }
}
