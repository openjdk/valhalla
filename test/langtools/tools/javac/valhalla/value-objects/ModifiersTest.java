/*
 * @test /nodynamiccopyright/
 * @bug 8279840
 * @summary [lworld] Inconsistent treatment of repeated modifiers.
 * @compile/fail/ref=ModifiersTest.out -XDrawDiagnostics -XDdev ModifiersTest.java
 */

public class ModifiersTest {

    static static class StaticTest {
    }

    native native class NativeTest {
    }

    value value primitive class ValueTest {
    }

    primitive primitive value class PrimitiveTest {
    }

}
