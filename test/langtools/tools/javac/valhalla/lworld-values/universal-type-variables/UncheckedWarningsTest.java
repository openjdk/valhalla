/*
 * @test /nodynamiccopyright/
 * @summary unchecked warning test for universal type variables
 * @compile/ref=UncheckedWarningsTest.out -Xlint:unchecked -XDrawDiagnostics UncheckedWarningsTest.java
 */

class UncheckedWarningsTest {
    static primitive class Atom { }

    static class Box<__universal X> { }

    public static void main(String[] args) {
        Box<Box<Box<Atom>>> val = null;
        Box<Box<Box<Atom.ref>>> ref = null;
        val = ref;
    }
}
