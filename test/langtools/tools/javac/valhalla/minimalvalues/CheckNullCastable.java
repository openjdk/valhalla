/*
 * @test /nodynamiccopyright/
 * @summary null cannot be casted to and compared with value types.
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckNullCastable.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror  -Xlint:values  CheckNullCastable.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckNullCastable {
    void foo(CheckNullCastable cnc) {
        CheckNullCastable cncl = (CheckNullCastable) null;
        if (cnc != null) {};
        if (null != cnc) {};
    }
}
