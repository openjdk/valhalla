/*
 * @test /nodynamiccopyright/
 * @summary null cannot be casted to and compared with value types.
 *
 * @compile/fail/ref=CheckNullCastable.out -XDrawDiagnostics CheckNullCastable.java
 */

__ByValue final class CheckNullCastable {
    void foo(CheckNullCastable cnc) {
        CheckNullCastable cncl = (CheckNullCastable) null;
        if (cnc != null) {};
        if (null != cnc) {};
    }
    int x = 10;
}
