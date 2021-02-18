/*
 * @test /nodynamiccopyright/
 * @summary Values may not extend an identity class
 *
 * @compile/fail/ref=CheckExtends.out -XDrawDiagnostics CheckExtends.java
 */

final primitive class CheckExtends extends Object {
    static class Nested {}
    static primitive class NestedValue extends Nested {}
}
