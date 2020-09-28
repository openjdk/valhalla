/*
 * @test /nodynamiccopyright/
 * @bug 8253312
 * @summary Enable JVM experiments in specialization under an opt-in mode
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile -XDflattenWithTypeRestrictions -XDrawDiagnostics RestrictedFieldCodegenTest.java
 * @compile/fail/ref=RestrictedFieldTypeTest.out -XDflattenWithTypeRestrictions -XDrawDiagnostics RestrictedFieldTypeTest.java
 */

public class RestrictedFieldTypeTest {
    PointBox rft = new PointBox();
    void foo() {
        rft.p = null;
    }
}
