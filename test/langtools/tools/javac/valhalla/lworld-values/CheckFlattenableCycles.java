/*
 * @test /nodynamiccopyright/
 * @summary Check for cycles through fields declared flattenable.
 *
 * @compile/fail/ref=CheckFlattenableCycles.out -XDrawDiagnostics CheckFlattenableCycles.java
 */

final __ByValue class CheckFlattenableCycles {
    class InnerRef {
        CheckFlattenableCycles cfc;
    }
    __ByValue final class InnerValue {
        final CheckFlattenableCycles cfc = __MakeDefault CheckFlattenableCycles(); // Error.
    }
    final CheckFlattenableCycles cfc = __MakeDefault CheckFlattenableCycles(); // Error.
    final int i = 10;
    final String s = "blah";
    final InnerRef ir = new InnerRef(); // OK.
    final InnerValue iv = __MakeDefault InnerValue();
}
