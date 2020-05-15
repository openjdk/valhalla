/*
 * @test /nodynamiccopyright/
 * @summary Values may not extend a concrete type other than jlO
 *
 * @compile/fail/ref=CheckExtends.out -XDrawDiagnostics CheckExtends.java
 */

final inline class CheckExtends extends Object {
}
