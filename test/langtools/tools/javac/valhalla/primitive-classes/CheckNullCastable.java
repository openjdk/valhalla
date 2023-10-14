/*
 * @test /nodynamiccopyright/
 * @summary null cannot be casted to and compared with value types.
 *
 * @compile/fail/ref=CheckNullCastable.out -XDrawDiagnostics -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckNullCastable.java
 */

value final class CheckNullCastable {
    void foo(CheckNullCastable! cnc) {
        CheckNullCastable! cncl = (CheckNullCastable!) null;
        if (cnc != null) {};
        if (null != cnc) {};
    }
    public implicit CheckNullCastable();
}
