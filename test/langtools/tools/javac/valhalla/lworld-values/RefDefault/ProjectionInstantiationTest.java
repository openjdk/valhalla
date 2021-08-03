/*
 * @test /nodynamiccopyright/
 * @bug 8244561 8250997
 * @summary Javac should not allow instantiation of V.ref or V.val
 * @compile/fail/ref=ProjectionInstantiationTest.out -XDrawDiagnostics ProjectionInstantiationTest.java
 */
import java.util.function.Supplier;

final class ProjectionInstantiationTest {

    static primitive class ValDefault {}

    static primitive class RefDefault.val<T> {}

    public static void main(String[] args) {

        // Next two instantiations are good.
        var v1 = new ValDefault();
        var v2 = new RefDefault<>();

        v1 = v2;

        // Next four instantiations are problematic
        new ValDefault.ref();
        new RefDefault.ref<>();
        new ValDefault.val();
        new RefDefault.val<>();

        // Next two references are good.
        voo(ValDefault::new);
        roo(RefDefault::new);

        // Next four references are problematic
        voo(ValDefault.ref::new);
        voo(ValDefault.val::new);
        roo(RefDefault.ref::new);
        roo(RefDefault.val::new);
    }

    static void voo(Supplier<ValDefault.ref> sx) {
        sx.get();
    }

    static void roo(Supplier<RefDefault.ref> sx) {
        sx.get();
    }
}
