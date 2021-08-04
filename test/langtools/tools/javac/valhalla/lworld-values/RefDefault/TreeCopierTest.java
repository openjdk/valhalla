/*
 * @test /nodynamiccopyright/
 * @bug 8244231
 * @summary Test that tree copier is able to handle reference and value projection types.
 * @compile/fail/ref=TreeCopierTest.out -XDrawDiagnostics TreeCopierTest.java
 */


final class TreeCopierTest {

    static primitive class RefDefault.val {}
    static primitive class GenericRefDefault.val<T> {}

    static primitive class ValDefault {}
    static primitive class GenericValDefault<T> {}

    public static void main(String[] args) {

        var v1 = (RefDefault.val) new RefDefault();
        var v2 = (GenericRefDefault.val<Object>) new GenericRefDefault<>();
        v1 = v2;

        var v3 = (ValDefault.ref) new ValDefault();
        var v4 = (GenericValDefault.ref<Object>) new GenericValDefault<>();
        v3 = v4;
    }
}
