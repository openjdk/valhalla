/*
 * @test /nodynamiccopyright/
 * @summary Values may not extend
 *
 * @compile/fail/ref=CheckExtends.out -XDrawDiagnostics CheckExtends.java
 */

final __ByValue class CheckExtends extends Object {
}
