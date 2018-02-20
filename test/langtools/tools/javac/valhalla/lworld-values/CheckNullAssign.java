/*
 * @test /nodynamiccopyright/
 * @summary Check assignment of null to value types - legal scenarios.
 *
 * @compile -XDrawDiagnostics CheckNullAssign.java
 */

final __ByValue class CheckNullAssign {
    CheckNullAssign foo(CheckNullAssign cna) {
        cna = null;
        foo(null);
        if (null instanceof CheckNullAssign) {}
        return null;
    }
}
