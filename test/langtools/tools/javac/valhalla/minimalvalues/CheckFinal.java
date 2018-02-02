/*
 * @test /nodynamiccopyright/
 * @summary Value types and their instance fields must be final
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckFinal.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values CheckFinal.java
 */

@jdk.incubator.mvt.ValueCapableClass
class CheckFinal {  // <- error
    int x;          // <- error
    void f(int x) { // <- ok
        int y;      // <- ok
        @jdk.incubator.mvt.ValueCapableClass
        final class CheckLocalFinal {
            int x; // <- error.
        }
    }
    final Object o = new Object() { int i; }; // <- ok
    static int xs; // OK.
}
