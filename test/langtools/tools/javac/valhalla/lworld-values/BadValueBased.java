/*
 * @test /nodynamiccopyright/
 * @summary Check that improper application of "@ValueBased" annotation are caught
 * @compile/fail/ref=BadValueBased.out -XDrawDiagnostics -XDdev BadValueBased.java
 */

public class BadValueBased {
    @ValueBased 
    interface X {}

    @ValueBased 
    @interface A {}

    @ValueBased
    enum E {}

    @ValueBased 
    class Y {
        @ValueBased int x;
    }
}
