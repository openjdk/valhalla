/*
 * @test /nodynamiccopyright/
 * @summary negative test for bad witness lookup
 * @compile/fail/ref=BadWitnessLookup.out -XDrawDiagnostics BadWitnessLookup.java
 */
class BadWitnessLookup {
    interface Foo<X> { }

    static class Bar {
        __witness Foo<Bar> FOO_W = new Foo<Bar>() { };
    }

    static class Baz<X> { }

    interface Barf { }

    <Z> void test() {
        Foo<Bar> fb = Foo<Bar>.__witness; // ok
        Foo<? extends Bar> fwb = Foo<? extends Bar>.__witness; // error, captured type-variable
        Foo<Z> fwb = Foo<Z>.__witness; // error, declared type-variable
        Foo<String> fs = Foo<String>.__witness; // error, not found
        Baz<Bar> bt = Baz<Bar>.__witness; // error, not an interface
        Barf b = Barf.__witness; // error, not generic
    }
}
