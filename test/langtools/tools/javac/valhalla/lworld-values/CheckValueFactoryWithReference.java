/*
 * @test /nodynamiccopyright/
 * @summary Do not allow mismatched instantiation syntax between value & reference types.
 *
 * @compile/fail/ref=CheckValueFactoryWithReference.out -XDrawDiagnostics -XDdisallowValueConstructors CheckValueFactoryWithReference.java
 */

final class CheckValueFactoryWithReference {
    final Object o = Object.default;
    value final class Point { int x = 10; }
    Point p = new Point();
}
