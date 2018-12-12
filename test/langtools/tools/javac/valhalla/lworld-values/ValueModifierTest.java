/*
 * @test /nodynamiccopyright/
 * @bug 8211910 8215246
 * @summary Reinstate support for local value classes.
 * @compile/fail/ref=ValueModifierTest.out -XDrawDiagnostics -XDdev ValueModifierTest.java
 */

public class ValueModifierTest {
    interface value {}
    void foo() {
        new value value() {};
    }
    void goo() {
        value class value {}
        new value() {};
        new value value() {};
        new value();
    }
}
