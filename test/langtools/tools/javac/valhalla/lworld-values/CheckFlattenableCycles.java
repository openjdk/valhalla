/*
 * @test /nodynamiccopyright/
 * @summary Check for cycles through fields declared flattenable.
 *
 * @compile/fail/ref=CheckFlattenableCycles.out -XDrawDiagnostics -XDallowFlattenabilityModifiers CheckFlattenableCycles.java
 */

final __ByValue class CheckFlattenableCycles {
    class InnerRef {
        CheckFlattenableCycles cfc;
    }
    __ByValue final class InnerValue {
        final __Flattenable CheckFlattenableCycles cfc = __MakeDefault CheckFlattenableCycles(); // Error.
    }
    final __Flattenable CheckFlattenableCycles cfc = __MakeDefault CheckFlattenableCycles(); // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final __Flattenable InnerValue iv = __MakeDefault InnerValue(); // Error
}
