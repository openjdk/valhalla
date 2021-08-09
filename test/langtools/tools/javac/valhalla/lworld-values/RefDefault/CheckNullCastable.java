/*
 * @test /nodynamiccopyright/
 * @summary null cannot be casted to and compared with value types.
 *
 * @compile/fail/ref=CheckNullCastable.out -XDrawDiagnostics CheckNullCastable.java
 */

primitive final class CheckNullCastable.val {
    void foo(CheckNullCastable.val cnc) {
        CheckNullCastable.val cncl = (CheckNullCastable.val) null;
        if (cnc != null) {};
        if (null != cnc) {};
    }
    int x = 10;
}
