/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support == or !=
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckEquals.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror  -Xlint:values CheckEquals.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckEquals {
    boolean foo(CheckEquals a, CheckEquals b) {
        return (a == b) || (a != b);
    }
}
