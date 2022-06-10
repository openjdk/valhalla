/*
 * @test /nodynamiccopyright/
 * @bug 8222790
 * @summary javac diagnostics don't discriminate between inline types and there nullable projection types.
 *
 * @compile/fail/ref=CheckQuestionInMessages.out -XDrawDiagnostics CheckQuestionInMessages.java
 */

import java.util.List;

primitive class X {
    List<X.ref> ls = new Object();
    X.ref[] xa = new Object[10];  // no support for Object.ref yet, but they are the same.
}
