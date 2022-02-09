/*
 * @test /nodynamiccopyright/
 * @bug 8197911
 * @summary Test Javac's treatment of null assignment to value instances
 * @compile -XDallowWithFieldOperator -XDrawDiagnostics -XDdev FlattenableTest.java
 */

public class FlattenableTest {
    value final class V {
        final int x = 10;

        value final class X {
            final V v = null;  // OK: initialization for value classes
            final V v2 = v;    // OK, null not constant propagated.

            V foo(X x) {
                x = __WithField(x.v, null);  // OK: withfield is permitted here.
                return x.v;
            }
        }
        V foo(X x) {
            x = __WithField(x.v, null); // OK: withfield is permitted here.
            return x.v;
        }

        class Y {
            V v;
            V [] va = { null }; // OK: array initialization
            V [] va2 = new V[] { null }; // OK: array initialization
            void foo(X x) {
                x = __WithField(x.v, null); // OK: withfield is permitted here.
                v = null; // legal assignment.
                va[0] = null; // legal.
                va = new V[] { null }; // legal
            }
        }
    }
}
