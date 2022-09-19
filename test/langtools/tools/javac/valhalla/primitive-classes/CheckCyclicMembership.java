/*
 * @test /nodynamiccopyright/
 * @summary Value types may not declare fields of its own type either directly or indirectly.
 *
 * @compile/fail/ref=CheckCyclicMembership.out -XDrawDiagnostics CheckCyclicMembership.java
 */

final primitive class CheckCyclicMembership {
    class InnerRef {
        CheckCyclicMembership ccm;
    }
    primitive final class InnerValue {
        final CheckCyclicMembership ccm = CheckCyclicMembership.default; // Error.
    }
    final CheckCyclicMembership ccm = CheckCyclicMembership.default; // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final InnerValue iv = InnerValue.default; // Error
}
