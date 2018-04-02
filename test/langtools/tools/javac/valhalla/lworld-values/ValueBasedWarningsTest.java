/*
 * @test /nodynamiccopyright/
 * @summary Check that javac emits warnings rather than errors for null violation with value based classes.
 * @compile/fail/ref=ValueBasedWarningsTest.out -Werror -XDrawDiagnostics -XDdev ValueBasedWarningsTest.java
 */

public class ValueBasedWarningsTest {
    @ValueBased
    final __ByValue class X {
        final int x = 10;
        void foo(X x1, X x2) {
            x1 = null;
            x2 = (X) null;
            if (null instanceof X) {}
        }
    }
}
