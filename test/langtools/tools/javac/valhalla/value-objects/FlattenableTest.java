/*
 * @test /nodynamiccopyright/
 * @bug 8197911
 * @summary Test Javac's treatment of null assignment to value instances
 * @compile -XDrawDiagnostics -XDdev FlattenableTest.java
 */

public class FlattenableTest {
    value final class V {
        final int x = 10;

        value final class X {
            final V v;
            final V v2;

            X() {
                this.v = null;
                this.v2 = v;    // OK, null not constant propagated.
            }

            X(V v) {
                this.v = v;
                this.v2 = v;
            }

            V foo(X x) {
                x = new X(null);  // OK
                return x.v;
            }
        }
        V foo(X x) {
            x = new X(null); // OK
            return x.v;
        }

        class Y {
            V v;
            V [] va = { null }; // OK: array initialization
            V [] va2 = new V[] { null }; // OK: array initialization
            void foo(X x) {
                x = new X(null); // OK
                v = null; // legal assignment.
                va[0] = null; // legal.
                va = new V[] { null }; // legal
            }
        }
    }
}
