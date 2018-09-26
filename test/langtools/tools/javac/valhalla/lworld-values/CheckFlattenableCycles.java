/*
 * @test /nodynamiccopyright/
 * @summary Check for cycles through fields declared flattenable.
 *
 * @compile/fail/ref=CheckFlattenableCycles.out -XDrawDiagnostics -XDallowFlattenabilityModifiers CheckFlattenableCycles.java
 */

final value class CheckFlattenableCycles {
    class InnerRef {
        CheckFlattenableCycles cfc;
    }
    value final class InnerValue {
        final __Flattenable CheckFlattenableCycles cfc = CheckFlattenableCycles.default; // Error.
    }
    final __Flattenable CheckFlattenableCycles cfc = CheckFlattenableCycles.default; // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final __Flattenable InnerValue iv = InnerValue.default; // Error
}
