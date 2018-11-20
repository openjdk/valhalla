/*
 * @test /nodynamiccopyright/
 * @summary Check for cycles through fields declared flattenable.
 *
 * @compile/fail/ref=CheckFlattenableCycles.out -XDrawDiagnostics CheckFlattenableCycles.java
 */

final value class CheckFlattenableCycles {
    class InnerRef {
        CheckFlattenableCycles cfc;
    }
    value final class InnerValue {
        final CheckFlattenableCycles.val cfc = CheckFlattenableCycles.default; // Error.
    }
    final CheckFlattenableCycles cfc = CheckFlattenableCycles.default; // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final InnerValue.val iv = InnerValue.default; // Error
}
