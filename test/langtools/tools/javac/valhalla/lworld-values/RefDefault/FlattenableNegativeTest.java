/*
 * @test /nodynamiccopyright/
 * @bug 8197911
 * @summary Test Javac's treatment of null assignment to value instances
 * @compile/fail/ref=FlattenableNegativeTest.out -XDallowWithFieldOperator -XDrawDiagnostics -XDdev FlattenableNegativeTest.java
 */

public class FlattenableNegativeTest {
    primitive final class V.val {
        final int x = 10;

        primitive final class X.val {
            final V.val v = null;  // Error: initialization illegal
            final V.val v2 = v;    // OK, null not constant propagated.

            V.val foo(X.val x) {
                x = __WithField(x.v, null);  // Error: withfield attempt is illegal.
                return x.v;
            }
        }
        V.val foo(X.val x) {
            x = __WithField(x.v, null); // withfield attempt is illegal
            return x.v;
        }

        class Y {
            V.val v;
            V.val [] va = { null }; // Illegal array initialization
            V.val [] va2 = new V.val[] { null }; // Illegal array initialization
            void foo(X.val x) {
                x = __WithField(x.v, null); // illegal withfield attempt
                v = null; // illegal assignment.
                va[0] = null; // Illegal.
                va = new V.val[] { null }; // Illegal
            }
        }
    }
}
