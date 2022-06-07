/*
 * @test /nodynamiccopyright/
 * @bug 8287767
 * @summary [lw4] Javac tolerates mutually incompatible super types.
 * @compile/fail/ref=MutuallyIncompatibleSupers.out -XDrawDiagnostics -XDdev MutuallyIncompatibleSupers.java
 */

public class MutuallyIncompatibleSupers {

    identity interface II {}
    value interface VI {}

    static abstract class X implements II, VI {} // mutually incompatible supers.

    interface GII extends II {} // OK.
    value interface BVI extends GII {} // Error

    interface GVI extends VI {} // OK.
    identity interface BII extends GVI {} // Error

    static value class BVC implements II {} // Error
    class BIC implements VI {} // Error
}
