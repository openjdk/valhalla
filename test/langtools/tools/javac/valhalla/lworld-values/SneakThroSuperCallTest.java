/*
 * @test /nodynamiccopyright/
 * @bug 8210310
 * @summary Javac allows invocation of identity sensitive jlO methods on values via super.
 * @compile/fail/ref=SneakThroSuperCallTest.out -XDrawDiagnostics -XDdev SneakThroSuperCallTest.java
 */

public inline class SneakThroSuperCallTest { 

    int x = 10;

    void foo() {
        super.notify();
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String toString () {
        return super.toString();
    }
}
