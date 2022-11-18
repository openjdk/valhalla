/*
 * @test /nodynamiccopyright/
 * @bug 8267821
 * @summary Javac's handling of `primitive' modifier is unlike the handling of other restricted identifiers
 * @compile/fail/ref=PrimitiveAsTypeName.out --source 16 -XDrawDiagnostics PrimitiveAsTypeName.java
 */

public class PrimitiveAsTypeName {
    public class primitive {
        primitive x;
        primitive foo(int l) {}
        Object o = new primitive primitive() {};
    }
}
