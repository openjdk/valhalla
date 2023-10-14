/*
 * @test /nodynamiccopyright/
 * @summary Value types may not declare fields of its own type either directly or indirectly.
 * @compile/fail/ref=CheckCyclicMembership.out -XDrawDiagnostics -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckCyclicMembership.java
 */

final value class CheckCyclicMembership {
    class InnerRef {
        CheckCyclicMembership ccm;
    }
    static value final class InnerValue {
        final CheckCyclicMembership! ccm;
        public implicit InnerValue();
        InnerValue(boolean dummy) {
            ccm = CheckCyclicMembership.default;  // Error.
        }
    }
    final CheckCyclicMembership! ccm;
    final int i;
    final String s;
    final InnerRef ir;
    final InnerValue! iv;

    public implicit CheckCyclicMembership();
    CheckCyclicMembership(boolean foo) {
        ccm = CheckCyclicMembership.default; // Error.
        i = 10;
        s = "blah";
        ir = new InnerRef(); // OK.
        iv = InnerValue.default; // Error
    }
}
