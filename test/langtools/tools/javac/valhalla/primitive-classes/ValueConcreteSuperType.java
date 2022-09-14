/*
 * @test /nodynamiccopyright/
 * @bug 8287136
 * @summary [lw4] Javac tolerates abstract value classes that violate constraints for qualifying to be value super classes
 * @compile/fail/ref=ValueConcreteSuperType.out -XDrawDiagnostics ValueConcreteSuperType.java
 */

public class ValueConcreteSuperType {
    static abstract value class H extends ValueConcreteSuperType {}  // Error: concrete super.
}
