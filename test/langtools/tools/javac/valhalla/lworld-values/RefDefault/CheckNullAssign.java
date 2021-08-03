/*
 * @test /nodynamiccopyright/
 * @summary Assignment of null to value types should be disallowed.
 *
 * @compile/fail/ref=CheckNullAssign.out -XDrawDiagnostics CheckNullAssign.java
 */

final primitive class CheckNullAssign.val {
    CheckNullAssign.val foo(CheckNullAssign.val cna) {
        // All of the below involve subtype/assignability checks and should be rejected.
        cna = null;
        foo(null);
        if (null instanceof CheckNullAssign.val) {}
        return null;
    }
    boolean b = null instanceof CheckNullAssign; // OK.
}
