/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support finalize
 *
 * @compile/fail/ref=CheckFinalize.out -XDenableValueTypes -XDrawDiagnostics CheckFinalize.java
 */

final __ByValue class CheckFinalize {
    @Override
    protected void finalize() {} // <-- error

    final __ByValue class CheckFinalizeInner {}

    void foo(CheckFinalizeInner cfi, CheckFinalize cf) {
        cfi.finalize();          // Error
        cf.finalize();           // OK.
    }
}
