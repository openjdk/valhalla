/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support == or !=
 *
 * @compile/fail/ref=CheckEquals.out -XDenableValueTypes -XDrawDiagnostics CheckEquals.java
 */

final __ByValue class CheckEquals {
    boolean foo(CheckEquals a, CheckEquals b) {
        return (a == b) || (a != b);
    }
}
