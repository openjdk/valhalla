/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support clone
 *
 * @compile/fail/ref=CheckClone.out -XDenableValueTypes -XDrawDiagnostics CheckClone.java
 */

final __ByValue class CheckClone {
    final __ByValue class InnerValue {
        void foo(InnerValue iv) {
            iv.clone();
        }
    }
    @Override
    protected Object clone() { return null; } // <-- error
}
