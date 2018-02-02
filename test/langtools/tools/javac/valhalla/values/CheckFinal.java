/*
 * @test /nodynamiccopyright/
 * @summary Value types and their instance fields must be final
 *
 * @compile/fail/ref=CheckFinal.out -XDenableValueTypes -XDrawDiagnostics CheckFinal.java
 */

__ByValue class CheckFinal {  // <- error
    int x;          // <- error
    void f(int x) { // <- ok
        int y;      // <- ok
        __ByValue final class CheckLocalFinal {
            int x; // <- error.
        }
    }
    final Object o = new Object() { int i; }; // <- ok
    static int xs; // OK.
}
