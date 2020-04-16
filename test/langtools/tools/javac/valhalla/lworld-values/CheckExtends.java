/*
 * @test /nodynamiccopyright/
 * @summary Values may not extend
 *
 * @compile/fail/ref=CheckExtends.out -XDallowEmptyValues -XDrawDiagnostics CheckExtends.java
 */

final inline class CheckExtends extends Object {
}
