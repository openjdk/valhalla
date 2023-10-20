/*
 * @test /nodynamiccopyright/
 * @summary Check null store into multidimensional array
 * @compile/fail/ref=CheckMultiDimensionalArrayStore.out -XDrawDiagnostics -XDdev -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckMultiDimensionalArrayStore.java
 */

public class CheckMultiDimensionalArrayStore {
    static value final class V {
        public implicit V();
        class Y {
            V! []![]![]! va = new V![][][] {{{ null }}};
            V! []![]![]! vb = new V[][][] {{{ null }}};  // OK?
            V! []![]! va2 =  {{ null }};
            void foo() {
                va = new V![][][] {{{ null }}};
                va[0][0][0] = null;
            }
        }
    }
}
