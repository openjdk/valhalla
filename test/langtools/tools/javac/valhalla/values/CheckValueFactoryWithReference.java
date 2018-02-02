/*
 * @test /nodynamiccopyright/
 * @summary Do not allow mismatched instantiation syntax between value & reference types.
 *
 * @compile/fail/ref=CheckValueFactoryWithReference.out -XDenableValueTypes -XDrawDiagnostics CheckValueFactoryWithReference.java
 */

final class CheckValueFactoryWithReference {
    final Object o = __MakeDefault Object();
    __ByValue final class Point {}
    Point p = new Point();
}
