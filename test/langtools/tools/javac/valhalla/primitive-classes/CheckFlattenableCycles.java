/*
 * @test /nodynamiccopyright/
 * @summary Check for cycles through fields declared flattenable.
 *
 * @compile/fail/ref=CheckFlattenableCycles.out -XDrawDiagnostics -XDenablePrimitiveClasses CheckFlattenableCycles.java
 * @ignore
 */

final primitive class CheckFlattenableCycles {
    class InnerRef {
        CheckFlattenableCycles cfc;
    }
    primitive final class InnerValue {
        final CheckFlattenableCycles     cfc = CheckFlattenableCycles.default; // Error.
    }
    final CheckFlattenableCycles cfc = CheckFlattenableCycles.default; // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final InnerValue     iv = InnerValue.default; // Error
}
