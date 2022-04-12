/*
 * @test /nodynamiccopyright/
 * @summary unchecked warning test for universal type variables
 * @compile/ref=UncheckedWarningsTest.out -Xlint:unchecked -XDrawDiagnostics UncheckedWarningsTest.java
 */

class UncheckedWarningsTest {
    static primitive class Atom { }

    static class Box<__universal X> { }
    static class Pair<__universal X, __universal Y> { }
    static class Triple<__universal X, __universal Y, __universal Z> { }

    public static void main(String[] args) {
        Box<Box<Box<Atom>>> val = null;
        Box<Box<Box<Atom.ref>>> ref = null;
        val = ref;
    }
}
