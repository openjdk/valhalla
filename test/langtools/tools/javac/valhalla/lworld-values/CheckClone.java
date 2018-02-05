/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support clone
 *
 * @compile/fail/ref=CheckClone.out -XDrawDiagnostics CheckClone.java
 */

final __ByValue class CheckClone {
    final __ByValue class InnerValue {
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