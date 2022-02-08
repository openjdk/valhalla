/*
 * @test /nodynamiccopyright/
 * @bug 8279368
 * @summary Test that value classes can be declared using annotations instead of modifiers
 * @compile/fail/ref=ValueAnnotationTest.out -XDrawDiagnostics -XDdev ValueAnnotationTest.java
 */

public class ValueAnnotationTest {
    @__value__ public class X {}
    @java.lang.__value__  public class Y extends X {}
    public class Z extends Y {}
}
