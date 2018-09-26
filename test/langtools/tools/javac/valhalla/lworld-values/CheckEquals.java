/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support == or !=
 *
 * @compile/fail/ref=CheckEquals.out -XDrawDiagnostics CheckEquals.java
 */

final value class CheckEquals {
    boolean foo(CheckEquals a, CheckEquals b) {
        return (a == b) || (a != b);
    }
    int x = 10;
}
