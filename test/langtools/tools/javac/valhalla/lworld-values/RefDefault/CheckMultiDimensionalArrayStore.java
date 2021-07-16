/*
 * @test /nodynamiccopyright/
 * @summary Check null store into multidimensional array
 * @compile/fail/ref=CheckMultiDimensionalArrayStore.out -XDrawDiagnostics -XDdev CheckMultiDimensionalArrayStore.java
 */

public class CheckMultiDimensionalArrayStore {

    primitive final class V {

        class Y { // null usage inside class Y are NOT problematic.
            V.ref [][][] va = new V.ref[][][] {{{ null }}};
            V.ref [][] va2 =  {{ null }};
            void foo() {
                va = new V.ref[][][] {{{ null }}};
                va[0][0][0] = null;
            }
        }

        class Z { // null usage inside class Z ARE ALL problematic.
            V [][][] va = new V[][][] {{{ null }}};
            V [][] va2 =  {{ null }};
            void foo() {
                va = new V[][][] {{{ null }}};
                va[0][0][0] = null;
            }
        }
    }

    primitive final class R.val {

        class Y { // null usage inside class Y NOT problematic.
            R [][][] va = new R[][][] {{{ null }}};
            R [][] va2 =  {{ null }};
            void foo() {
                va = new R[][][] {{{ null }}};
                va[0][0][0] = null;
            }
        }

        class Z { // null usage inside class Z ARE ALL problematic.
            R.val [][][] va = new R.val[][][] {{{ null }}};
            R.val [][] va2 =  {{ null }};
            void foo() {
                va = new R.val[][][] {{{ null }}};
                va[0][0][0] = null;
            }
        }
    }
}
