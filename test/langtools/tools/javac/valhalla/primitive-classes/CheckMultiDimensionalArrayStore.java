/*
 * @test /nodynamiccopyright/
 * @summary Check null store into multidimensional array
 * @compile/fail/ref=CheckMultiDimensionalArrayStore.out -XDrawDiagnostics -XDdev -XDenablePrimitiveClasses CheckMultiDimensionalArrayStore.java
 * @ignore
 */

public class CheckMultiDimensionalArrayStore {
    primitive final class V {
        final int x = 10;
        class Y {
            V [][][] va = new V[][][] {{{ null }}};
            V [][] va2 =  {{ null }};
            void foo() {
                va = new V[][][] {{{ null }}};
                va[0][0][0] = null;
            }
        }
    }
}
