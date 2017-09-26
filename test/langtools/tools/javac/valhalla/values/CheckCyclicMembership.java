/*
 * @test /nodynamiccopyright/
 * @summary Value types may not declare fields of its own type either directly or indirectly.
 *
 * @compile/fail/ref=CheckCyclicMembership.out -XDenableValueTypes -XDrawDiagnostics CheckCyclicMembership.java
 */

final __ByValue class CheckCyclicMembership {
    class InnerRef {
        CheckCyclicMembership ccm;
    }
    __ByValue final class InnerValue {
        final CheckCyclicMembership ccm = __MakeDefault CheckCyclicMembership(); // Error.
    }
    final CheckCyclicMembership ccm = __MakeDefault CheckCyclicMembership(); // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final InnerValue iv = __MakeDefault InnerValue(); // Error. Some Order dependancy hides this. FIXME.
}
