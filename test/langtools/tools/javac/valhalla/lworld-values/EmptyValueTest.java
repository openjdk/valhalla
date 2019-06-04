/*
 * @test /nodynamiccopyright/
 * @summary Test behavior with empty value type.
 * @compile/fail/ref=EmptyValueTest.out -XDrawDiagnostics -XDdev EmptyValueTest.java
 * @compile -XDrawDiagnostics -XDdev -XDallowEmptyValues EmptyValueTest.java
 */
public final inline class EmptyValueTest {
    static int x;
}
