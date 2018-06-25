/*
 * @test /nodynamiccopyright/
 * @summary Check that javac warns on potential null pollution of value based instances.
 * @compile/fail/ref=NullPollutionTest.out -Werror -XDrawDiagnostics -XDallowValueBasedClasses -XDdev NullPollutionTest.java
 * @compile -Werror -XDrawDiagnostics -XDdev NullPollutionTest.java
 */

public class NullPollutionTest {
    @ValueBased
    public class X {
        void foo(X x) {
            Object o = null;
            x = (X) o;
        }
    }
}
