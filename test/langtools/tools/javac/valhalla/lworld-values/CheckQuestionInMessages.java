/*
 * @test /nodynamiccopyright/
 * @bug 8222790
 * @summary javac diagnostics don't discriminate between inline types and there nullable projection types.
 *
 * @compile/fail/ref=CheckQuestionInMessages.out -XDrawDiagnostics CheckQuestionInMessages.java
 */

import java.util.List;

inline class X {
    List<X?> ls = new Object();    
    X?[] xa = new Object?[10];
}
