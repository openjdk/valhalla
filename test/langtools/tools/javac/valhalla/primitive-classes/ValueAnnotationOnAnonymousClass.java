/*
 * @test /nodynamiccopyright/
 * @summary Check valueness via __value__ annotation
 * @bug 8221699
 * @compile/fail/ref=ValueAnnotationOnAnonymousClass.out -XDrawDiagnostics -XDenablePrimitiveClasses ValueAnnotationOnAnonymousClass.java
 */

class ValueAnnotationOnAnonymousClass {
    interface I {}
    primitive
    public static void main(String args []) {
        new primitive I() {
        };
    }
}
