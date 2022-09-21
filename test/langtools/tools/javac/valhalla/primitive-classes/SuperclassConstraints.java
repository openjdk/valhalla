/*
 * @test /nodynamiccopyright/
 * @bug 8242900
 * @summary Verify various constraints for an inline class's super types.
 * @compile/fail/ref=SuperclassConstraints.out -XDrawDiagnostics -XDdev SuperclassConstraints.java
 */

public class SuperclassConstraints {

    // -------------------------------------------------------------

    // Test that super class cannot be concrete, including express jlO
    static class BadSuper {}
    primitive class I0 extends BadSuper {} // ERROR: concrete super class
    primitive class I1 extends Object {}   // OK: concrete jlO can be express-superclass
    primitive class I2 {} // OK

    // Test that abstract class is allowed to be super including when extending jlO
    interface GoodSuperInterface {}
    static abstract class GoodSuper extends Object {}
    primitive class I3 extends GoodSuper implements GoodSuperInterface {} // jlO can be indirect super class
    static abstract class Integer extends Number {
        public double doubleValue() { return 0; }
        public float floatValue() { return 0; }
        public long longValue() { return 0; }
        public int intValue() { return 0; }
    }
    primitive class I4 extends Integer {}
    primitive class I5 extends Number {
        public double doubleValue() { return 0; }
        public float floatValue() { return 0; }
        public long longValue() { return 0; }
        public int intValue() { return 0; }
    }

    // -------------------------------------------------------------

    // Test that super class cannot define instance fields.
    static abstract class SuperWithInstanceField {
        int x;
    }
    static abstract class SuperWithInstanceField_01 extends SuperWithInstanceField {}

    primitive class I6 extends SuperWithInstanceField_01 {} // ERROR:

    // Test that super class can define static fields.
    static abstract class SuperWithStaticField {
        static int x;
    }
    primitive class I7 extends SuperWithStaticField {} // OK.

    // -------------------------------------------------------------

    // Test that no-arg constructor must be empty
    static abstract class SuperWithEmptyNoArgCtor {
        SuperWithEmptyNoArgCtor() {
            // Programmer supplied ctor but injected super call
        }
    }
    static abstract class SuperWithEmptyNoArgCtor_01 extends SuperWithEmptyNoArgCtor {
        SuperWithEmptyNoArgCtor_01() {
            super();  // programmer coded chaining no-arg constructor
        }
    }
    static abstract class SuperWithEmptyNoArgCtor_02 extends SuperWithEmptyNoArgCtor_01 {
        // Synthesized chaining no-arg constructor
    }
    primitive class I8 extends SuperWithEmptyNoArgCtor_02 {}

    static abstract class SuperWithNonEmptyNoArgCtor {
        SuperWithNonEmptyNoArgCtor() {
            System.out.println("Non-Empty");
        }
    }
    static abstract class SuperWithNonEmptyNoArgCtor_01 extends SuperWithNonEmptyNoArgCtor {}
    primitive class I9 extends SuperWithNonEmptyNoArgCtor_01 {} // ERROR:

    // Test that there can be no other constructors.
    static abstract class SuperWithArgedCtor {
        SuperWithArgedCtor() {}
        SuperWithArgedCtor(String s) {
        }
    }
    static abstract class SuperWithArgedCtor_01 extends SuperWithArgedCtor {}
    primitive class I10 extends SuperWithArgedCtor_01 {} // ERROR:

    // Test that instance initializers are not allowed in supers
    static abstract class SuperWithInstanceInit {
        {
            System.out.println("Disqualified from being super");
        }
    }
    static abstract class SuperWithInstanceInit_01 extends SuperWithInstanceInit {
        {
            // Not disqualified since it is a meaningless empty block.
        }
    }
    primitive class I11 extends SuperWithInstanceInit_01 {} // ERROR:

    // Test that synchronized methods are not allowed in supers.
    static abstract class SuperWithSynchronizedMethod {
        synchronized void foo() {}
    }
    static abstract class SuperWithSynchronizedMethod_1 extends SuperWithSynchronizedMethod {
    }
    primitive class I12 extends SuperWithSynchronizedMethod_1 {} // ERROR:

    // No instance fields and no arged constructor also means inner classes cannot be supers
    abstract class InnerSuper {}
    primitive class I13 extends InnerSuper {}
}
