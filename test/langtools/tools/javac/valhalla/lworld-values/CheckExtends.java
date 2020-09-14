/*
 * @test /nodynamiccopyright/
 * @summary Values may not extend an identity class
 *
 * @compile/fail/ref=CheckExtends.out -XDrawDiagnostics CheckExtends.java
 */

final inline class CheckExtends extends Object {
    static class Nested {}
    static inline class NestedValue extends Nested {}
}
