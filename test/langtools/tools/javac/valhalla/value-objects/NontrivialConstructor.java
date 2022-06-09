/*
 * @test /nodynamiccopyright/
 * @bug 8287763
 * @summary [lw4] Javac does not implement the spec for non-trivial constructors in toto
 * @compile/fail/ref=NontrivialConstructor.out -XDrawDiagnostics -XDdev NontrivialConstructor.java
 */

public class NontrivialConstructor {

    abstract static value class I0 {
        public I0() { // trivial ctor.
        }
    }

    abstract static value class I1 {
        private I1() {} // non-trivial, more restrictive access than the class.
    }

    abstract static value class I2 {
        public I2(int x) {} // non-trivial ctor as it declares formal parameters.
    }

    abstract static value class I3 {
        <T> I3() {} // non trivial as it declares type parameters.
    }


    abstract static value class I4 {
        I4() throws Exception {} // non-trivial as it throws
    }

    abstract static value class I5 {
        I5() {
            System.out.println("");
        } // non-trivial as it has a body.
    }
}
