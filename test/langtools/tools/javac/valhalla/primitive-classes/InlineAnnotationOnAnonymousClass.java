/*
 * @test /nodynamiccopyright/
 * @summary Check inlineness via __inline__ annotation
 * @bug 8222745
 * @compile/fail/ref=InlineAnnotationOnAnonymousClass.out -XDrawDiagnostics -XDenablePrimitiveClasses InlineAnnotationOnAnonymousClass.java
 */

class InlineAnnotationOnAnonymousClass {
    interface I {}
    primitive
    public static void main(String args []) {
        new primitive I() {
        };
    }
}
