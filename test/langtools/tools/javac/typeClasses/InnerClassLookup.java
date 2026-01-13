/*
 * @test /nodynamiccopyright/
 * @summary check that outer classes can't witness inner classes
 * @compile/fail/ref=InnerClassLookup.out -XDrawDiagnostics InnerClassLookup.java
 */
class InnerClassLookup {
    static class Outer {
        __witness U<Inner> U = null;

        class Inner { }

        void test() {
            var x = U<Inner>.__witness;
        }
    }
}
