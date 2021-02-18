/*
 * @test /nodynamiccopyright/
 * @bug 8211910 8215246
 * @summary Reinstate support for local value classes.
 * @compile/fail/ref=ValueModifierTest.out -XDrawDiagnostics -XDdev ValueModifierTest.java
 */

public class ValueModifierTest {
    interface value {}
    void foo() {
        new primitive value() {};
    }
    void goo() {
        primitive class value {}
        new value() {};
        new primitive value() {};
        new value();
    }
}
