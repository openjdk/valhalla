/*
 * @test /nodynamiccopyright/
 * @summary Check that a concrete class is not allowed to be the super class of a primitive class
 *
 * @compile/fail/ref=CheckExtends.out -XDrawDiagnostics -XDenablePrimitiveClasses CheckExtends.java
 */

final value class CheckExtends extends Object {
    static class NestedConcrete {}
    static value class NestedPrimitive extends NestedConcrete {}
}
