/*
 * @test /nodynamiccopyright/
 * @bug 8211910 8215246
 * @summary Reinstate support for local value classes.
 * @compile/fail/ref=ValueModifierTest.out -XDrawDiagnostics -XDdev ValueModifierTest.java
 */

public class ValueModifierTest {
    interface Value {}
    void foo() {
        new primitive Value() {};
    }
    void goo() {
        primitive class Value {}
        new Value() {};
        new primitive Value() {};
        new Value();
    }
}
