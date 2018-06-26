/*
 * @test /nodynamiccopyright/
 * @bug 8197911
 * @summary Test Javac's treatment of null assignment to value instances
 * @compile/fail/ref=FlattenableNegativeTest.out -XDrawDiagnostics -XDdev FlattenableNegativeTest.java
 */

public class FlattenableNegativeTest {
    __ByValue final class V {
        final int x = 10;
        
        __ByValue final class X {
            final V v = null;  // Error: initialization illegal
            final V v2 = v;    // OK, null not constant propagated.

            V foo(X x) {
                x = __WithField(x.v, null);  // Error: withfield attempt is illegal.
                return x.v;
            }
        }
        V foo(X x) {
            x = __WithField(x.v, null); // withfield attempt is illegal
            return x.v;
        }

        class Y {
            V v;
            V [] va = { null }; // Illegal array initialization
            V [] va2 = new V[] { null }; // Illegal array initialization
            void foo(X x) {
                x = __WithField(x.v, null); // illegal withfield attempt
                v = null; // illegal assignment.
                va[0] = null; // Illegal.
                va = new V[] { null }; // Illegal
            }
        }
    }
}
