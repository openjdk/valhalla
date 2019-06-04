/*
 * @test /nodynamiccopyright/
 * @summary Check valueness via __value__ annotation
 * @bug 8221699
 * @compile/fail/ref=ValueAnnotationOnAnonymousClass.out -XDrawDiagnostics ValueAnnotationOnAnonymousClass.java
 */

class ValueAnnotationOnAnonymousClass {
    interface I {}
    @__inline__
    public static void main(String args []) {
        new @__inline__ I() {
        };
    }
}
