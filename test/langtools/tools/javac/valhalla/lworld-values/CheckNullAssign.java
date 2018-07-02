/*
 * @test /nodynamiccopyright/
 * @summary Assignment of null to value types should be disallowed.
 *
 * @compile/fail/ref=CheckNullAssign.out -XDrawDiagnostics CheckNullAssign.java
 */

final __ByValue class CheckNullAssign {
    CheckNullAssign foo(CheckNullAssign cna) {
        // All of the below involve subtype/assignability checks and should be rejected.
        cna = null;
        foo(null);
        if (null instanceof CheckNullAssign) {}
        return null;
    }
    int x = 10;
}
