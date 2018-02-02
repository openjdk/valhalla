/*
 * @test /nodynamiccopyright/
 * @summary Values may not extend
 *
 * @compile/fail/ref=CheckExtends.out -XDenableValueTypes -XDrawDiagnostics CheckExtends.java
 */

final __ByValue class CheckExtends extends Object {
}
