/*
 * @test /nodynamiccopyright/
 * @summary Check for cycles through fields declared flattenable.
 *
 * @compile/fail/ref=CheckFlattenableCycles.out -XDrawDiagnostics -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckFlattenableCycles.java
 */

final value class CheckFlattenableCycles {
    class InnerRef {
        CheckFlattenableCycles cfc;
    }
    static final value class InnerValue {
        final CheckFlattenableCycles! cfc; // Error.
        public implicit InnerValue();
    }
    final CheckFlattenableCycles! cfc; // Error.
    final InnerValue! iv; // Error

    public implicit CheckFlattenableCycles();
}
