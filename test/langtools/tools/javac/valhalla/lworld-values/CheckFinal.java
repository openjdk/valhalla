/*
 * @test /nodynamiccopyright/
 * @summary Value types and their instance fields are implicitly final
 *
 * @compile/fail/ref=CheckFinal.out -XDrawDiagnostics CheckFinal.java
 */

__ByValue class CheckFinal { // implicitly final
    int fi;  // implicitly final
    final int fe; // explicitly final
    void f(int x) {
        int y;
        x = y = 0;
        this.fi = 100;  // Error.
        this.fe = 100;  // Error.
        this.xs = 100; // OK.
        this.xsf = 100; // Error

        CheckFinal cf = new CheckFinal() {}; // Error, final class cannot be extended.
        __ByValue final class CheckLocalFinal { // Explicitly final
            int x;
        }
    }
    final Object o = new Object() { int i;
                         void foo() {
                             i = 100;
                         };
                     };
    static int xs; // OK.
    static final int xsf; // OK.
}
