/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support clone
 *
 * @compile/fail/ref=CheckClone.out -XDrawDiagnostics CheckClone.java
 */

final inline class CheckClone {
    final inline class InnerValue {
        void foo(InnerValue iv) {
            iv.clone();
            clone();
        }
    }
    void foo(CheckClone v) {
        v.clone();
        clone();
    }
    @Override
    protected Object clone() { return null; }
}
