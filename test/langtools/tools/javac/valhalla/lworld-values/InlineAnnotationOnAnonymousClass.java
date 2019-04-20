/*
 * @test /nodynamiccopyright/
 * @summary Check inlineness via __inline__ annotation
 * @bug 8222745
 * @compile/fail/ref=InlineAnnotationOnAnonymousClass.out -XDrawDiagnostics InlineAnnotationOnAnonymousClass.java
 */

class InlineAnnotationOnAnonymousClass {
    interface I {}
    @__inline__
    public static void main(String args []) {
        new @__inline__ I() {
        };
    }
}
