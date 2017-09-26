/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support clone
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckClone.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values CheckClone.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckClone {
    @jdk.incubator.mvt.ValueCapableClass
    final class InnerValue {
        void foo(InnerValue iv) {
            iv.clone(); // <-- error
        }
    }
    @Override
    protected Object clone() { return null; } // <-- error
}
