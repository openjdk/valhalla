/*
 * @test /nodynamiccopyright/
 * @bug 8244231
 * @summary Javac should reject P<T>.{ref,val} preferring P.{ref,val}<T> instead
 * @compile/fail/ref=MalformedParameterizedType.out -XDrawDiagnostics MalformedParameterizedType.java
 */

final class MalformedParameterizedType {

    static primitive class P<T> {}
    static primitive class RDP.val<T> {}

    P.ref<String> pr1; // OK
    P.val<String> pv1; // OK

    P<String>.ref pr2; // Malformed
    P<String>.val pv2; // Malformed

    RDP.ref<String> rdpr1; // OK
    RDP.val<String> rdpv1; // OK

    RDP<String>.ref rdpr2; // Malformed
    RDP<String>.val rdpv2; // Malformed
}
