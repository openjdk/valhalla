/*
 * @test /nodynamiccopyright/
 * @summary Check that a concrete class is not allowed to be the super class of a primitive class
 *
 * @compile/fail/ref=CheckExtends.out -XDrawDiagnostics CheckExtends.java
 */

final primitive class CheckExtends extends Object {
    static class NestedConcrete {}
    static primitive class NestedPrimitive extends NestedConcrete {}
}
