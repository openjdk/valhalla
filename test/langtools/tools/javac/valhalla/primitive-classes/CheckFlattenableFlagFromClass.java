/*
 * @test /nodynamiccopyright/
 * @bug 8197911
 * @summary Check that valueness is deduced from class files and has the appropriate effect.
 * @compile -XDenablePrimitiveClasses FlattenableFlagFromClass.java
 * @compile/fail/ref=CheckFlattenableFlagFromClass.out -XDrawDiagnostics -XDenablePrimitiveClasses CheckFlattenableFlagFromClass.java
 * @ignore
 */

public class CheckFlattenableFlagFromClass {
    void foo(FlattenableFlagFromClass f) {
        f.v = null; // Error.
        f.va[0] = null; // Error.
    }
}
