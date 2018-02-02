/*
 * @test /nodynamiccopyright/
 * @summary Assignment of null to value types should be disallowed.
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckNullAssign.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values CheckNullAssign.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckNullAssign {
    CheckNullAssign foo(CheckNullAssign cna) {
        // All of the below involve subtype/assignability checks and should be rejected.
        CheckNullAssign cnal = null;
        cna = null;
        foo(null);
        if (null instanceof CheckNullAssign) {}
        return null;
    }
}
