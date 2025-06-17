/*
 * @test /nodynamiccopyright/
 * @bug 8359370
 * @summary [lworld] allow instance fields of identity classes to be readable in the prologue phase
 * @compile/fail/ref=EarlyAssignments2.out -XDrawDiagnostics EarlyAssignments2.java
 * @enablePreview
 */

public class EarlyAssignments2 {
    public static class Inner1 {
        public int x;
    }

    public Inner1(int y) {
        y = x;                          // FAIL - x might not have been initialized
        y = this.x;                     // FAIL - x might not have been initialized
        y = Inner1.this.x;              // FAIL - x might not have been initialized
        super();
    }
}
