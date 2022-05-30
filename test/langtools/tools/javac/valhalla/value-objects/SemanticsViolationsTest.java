/*
 * @test /nodynamiccopyright/
 * @bug 8279672
 * @summary Implement semantic checks for value classes
 * @compile/fail/ref=SemanticsViolationsTest.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev SemanticsViolationsTest.java
 */

public class SemanticsViolationsTest {

    // A value class is implicitly final, so cannot be extended.
    value class Base {}
    class Subclass extends Base {} // Error: Base is implicitly final, cannot be extended.



    abstract value class AbsValue {}  // Error: value class inner
    value interface ValueInterface {} // Error: interface cannot modified with value.

    // All instance fields are implicitly final, so must be assigned exactly
    // once by constructors or initializers, and cannot be assigned outside
    // of a constructor or initializer.
    value class Point {

        int x = 10;
        int y;
        int z;

        Point (int x, int y, int z) {
            this.x = x; // Error, final field 'x' is already assigned to.
            this.y = y; // OK.
            // Error, final z is unassigned.
        }
        void foo(Point p) {
            this.y = p.y; // Error, y is final and can't be written outside of ctor.
        }
    }


    // A value identity class is an oxymoron
    value identity class IdentityValue { // Error, bad modifier combination.
    }
    value class IdentityValue2 extends SemanticsViolationsTest { // Error, can't extend identity class
    }
    abstract static class AbstractWithState {
       int xx;
    }
    value class BrokenValue3 extends AbstractWithState { // Error, super class has state.
    }
    abstract static class AbstractWithoutState {
        static int ss;
    }
    value class GoodValue1 extends AbstractWithoutState {}
    value class GoodValue2 extends Object {} // allowed.

    // No constructor makes a super constructor call. Instance creation will
    // occur without executing any superclass initialization code.
    value class BrokenValue4 {
        BrokenValue4() {
            super(); // Error, can't chain to super's ctor.
        }
    }

    // No instance methods are declared synchronized.
    value class BrokenValue5 {
        synchronized void foo() {} // Error;
        synchronized static void soo() {} // OK.
        { synchronized(this) { /* Error.*/  } }
    }

    // The class does not declare a finalize() method.
    value class BrokenValue6 {
        public void finalize() {} // Error
    }

    // (Possibly) The constructor does not make use of this except to set
    // the fields in the constructor body, or perhaps after all fields are
    // definitely assigned.
    value class BrokenValue7 {
        int x;
        BrokenValue7() {
            foo(this); // Error.
            x = 10;
            foo(this); // Ok.
        }
        void foo(BrokenValue7 bv) {
        }
    }

    value record BrokenValue8(int x, int y) {
        synchronized void foo() { } // Error;
        synchronized static void soo() {} // OK.
    }
}
