/*
 * @test /nodynamiccopyright/
 * @bug 8242900
 * @summary Verify various constraints for an inline class's BINARY super types.
 * @compile -XDrawDiagnostics -XDdev SuperclassCollections.java
 * @compile/fail/ref=BinarySuperclassConstraints.out -XDrawDiagnostics -XDdev BinarySuperclassConstraints.java
 */

public class BinarySuperclassConstraints {

    // -------------------------------------------------------------

    // Test that super class cannot be concrete, including express jlO
    primitive class I0 extends SuperclassCollections.BadSuper {} // ERROR: concrete super class

    // Test that abstract class is allowed to be super including when extending jlO
    primitive class I3 extends SuperclassCollections.GoodSuper implements SuperclassCollections.GoodSuperInterface {} // jlO can be indirect super class
    primitive class I4 extends SuperclassCollections.Integer {}
    primitive class I5 extends Number {
        public double doubleValue() { return 0; }
        public float floatValue() { return 0; }
        public long longValue() { return 0; }
        public int intValue() { return 0; }
    }

    // -------------------------------------------------------------

    // Test that super class cannot define instance fields.
    primitive class I6 extends SuperclassCollections.SuperWithInstanceField_01 {} // ERROR:

    primitive class I7 extends SuperclassCollections.SuperWithStaticField {} // OK.

    // -------------------------------------------------------------

    // Test that no-arg constructor must be empty
    primitive class I8 extends SuperclassCollections.SuperWithEmptyNoArgCtor_02 {}

    primitive class I9 extends SuperclassCollections.SuperWithNonEmptyNoArgCtor_01 {} // ERROR:

    primitive class I10 extends SuperclassCollections.SuperWithArgedCtor_01 {} // ERROR:

    primitive class I11 extends SuperclassCollections.SuperWithInstanceInit_01 {} // ERROR:

    primitive class I12 extends SuperclassCollections.SuperWithSynchronizedMethod_1 {} // ERROR:

    primitive class I13 extends SuperclassCollections.InnerSuper {}
}
