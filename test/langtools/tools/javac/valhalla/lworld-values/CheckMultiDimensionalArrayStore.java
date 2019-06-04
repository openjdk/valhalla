/*
 * @test /nodynamiccopyright/
 * @summary Check null store into multidimensional array
 * @compile/fail/ref=CheckMultiDimensionalArrayStore.out -XDrawDiagnostics -XDdev CheckMultiDimensionalArrayStore.java
 */

public class CheckMultiDimensionalArrayStore {
    inline final class V {
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
