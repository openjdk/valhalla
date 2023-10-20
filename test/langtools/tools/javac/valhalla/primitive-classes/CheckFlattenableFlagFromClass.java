/*
 * @test /nodynamiccopyright/
 * @bug 8197911
 * @summary Check that valueness is deduced from class files and has the appropriate effect.
 * @compile -XDenablePrimitiveClasses -XDenableNullRestrictedTypes FlattenableFlagFromClass.java
 * @compile/fail/ref=CheckFlattenableFlagFromClass.out -XDrawDiagnostics -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckFlattenableFlagFromClass.java
 */

public class CheckFlattenableFlagFromClass {
    void foo(FlattenableFlagFromClass f) {
        f.v = null; // Error.
    }
}
