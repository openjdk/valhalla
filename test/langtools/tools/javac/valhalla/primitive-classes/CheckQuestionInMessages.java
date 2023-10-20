/*
 * @test /nodynamiccopyright/
 * @bug 8222790
 * @summary javac diagnostics don't discriminate between inline types and there nullable projection types.
 *
 * @compile/fail/ref=CheckQuestionInMessages.out -XDrawDiagnostics -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckQuestionInMessages.java
 */

import java.util.List;

value class X {
    void m() {
        List<X> ls = new Object();
        X[] xa = new Object[10];  // no support for Object.ref yet, but they are the same.
    }
    public implicit X();
}
