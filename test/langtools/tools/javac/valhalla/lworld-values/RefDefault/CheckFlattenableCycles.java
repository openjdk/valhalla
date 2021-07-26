/*
 * @test /nodynamiccopyright/
 * @summary Check for cycles through fields declared flattenable.
 *
 * @compile/fail/ref=CheckFlattenableCycles.out -XDrawDiagnostics CheckFlattenableCycles.java
 */

final primitive class CheckFlattenableCycles.val {
    class InnerRef {
        CheckFlattenableCycles.val cfc;
    }
    primitive final class InnerValue.val {
        final CheckFlattenableCycles.val     cfc = CheckFlattenableCycles.val.default; // Error.
    }
    final CheckFlattenableCycles.val cfc = CheckFlattenableCycles.val.default; // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final InnerValue.val     iv = InnerValue.val.default; // Error
}
