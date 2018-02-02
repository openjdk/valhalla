/*
 * @test /nodynamiccopyright/
 * @summary Values may not extend
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckExtends.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror  -Xlint:values CheckExtends.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckExtends extends Object {
}
