/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support finalize
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckFinalize.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values  CheckFinalize.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckFinalize {
    @Override
    protected void finalize() {} // <-- error

    @jdk.incubator.mvt.ValueCapableClass
    final class CheckFinalizeInner {}

    void foo(CheckFinalizeInner cfi, CheckFinalize cf) {
        cfi.finalize();          // Error
        cf.finalize();           // OK.
    }
}
