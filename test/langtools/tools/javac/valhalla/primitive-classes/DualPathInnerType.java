/**
 * @test /nodynamiccopyright/
 * @bug 8244233
 * @summary Nested types are not handled properly across projections
 * @compile/fail/ref=DualPathInnerType.out -XDrawDiagnostics DualPathInnerType.java
 */

public primitive class DualPathInnerType  {

    class Inner { }

    static DualPathInnerType.Inner xi = new DualPathInnerType().new Inner();
    DualPathInnerType.ref.Inner xri = xi;

    void f (DualPathInnerType.Inner xri) {}
    void f (DualPathInnerType.ref.Inner xri) {}

    public static void main(String [] args) {
        new DualPathInnerType();
    }
}
