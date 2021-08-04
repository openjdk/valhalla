/*
 * @test /nodynamiccopyright/
 * @summary Value types may not declare fields of its own type either directly or indirectly.
 *
 * @compile/fail/ref=CheckCyclicMembership.out -XDrawDiagnostics CheckCyclicMembership.java
 */

final primitive class CheckCyclicMembership.val {
    class InnerRef {
        CheckCyclicMembership.val ccm;
    }
    primitive final class InnerValue.val {
        final CheckCyclicMembership.val ccm = CheckCyclicMembership.val.default; // Error.
    }
    final CheckCyclicMembership.val ccm = CheckCyclicMembership.val.default; // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final InnerValue.val iv = InnerValue.val.default; // Error
}
