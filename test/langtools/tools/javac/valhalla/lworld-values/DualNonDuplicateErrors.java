/*
 * @test /nodynamiccopyright/
 * @bug 8267843
 * @summary Check that javac diagnoses `this` being passed around and instance method being invoked before primitive class instance is fully initialized.
 * @compile/fail/ref=DualNonDuplicateErrors.out -XDrawDiagnostics DualNonDuplicateErrors.java
 */

public primitive class DualNonDuplicateErrors {

    int x;

    DualNonDuplicateErrors() {
        // The call below should trigger two errors - they are not duplicates really.
        // First one is for `this` being passed around ("exposed")
        // Second is for instance method being invoked thereby allowing that method to
        // observe the primitive class instance in a partially initialized state.
        foo(this);
        x = 10;
        foo(this); // No error here.
    }

    void foo(DualNonDuplicateErrors x) {
    }
}
