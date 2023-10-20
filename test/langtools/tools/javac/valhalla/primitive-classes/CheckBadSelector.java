/*
 * @test /nodynamiccopyright/
 * @bug 8237067
 * @summary [lworld] Check good and bad selectors on a type name
 * @compile/fail/ref=CheckBadSelector.out -XDrawDiagnostics -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckBadSelector.java
 */

value final class Point {
    public implicit Point();
    void badSelector() {
        Class<?> c = int.class;
        int i = int.default;
        int x = int.whatever;
    }
}
