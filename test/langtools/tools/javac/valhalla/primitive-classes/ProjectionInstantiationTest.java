/*
 * @test /nodynamiccopyright/
 * @bug 8244561 8250997
 * @summary Javac should not allow instantiation of V.ref or V.val
 * @compile/fail/ref=ProjectionInstantiationTest.out -XDrawDiagnostics ProjectionInstantiationTest.java
 */
import java.util.function.Supplier;

final primitive class ProjectionInstantiationTest {
    int x = 42;
    public static void main(String[] args) {
        new ProjectionInstantiationTest();
        new ProjectionInstantiationTest.ref();
        new ProjectionInstantiationTest.val();
        foo(ProjectionInstantiationTest::new);
        foo(ProjectionInstantiationTest.ref::new);
        foo(ProjectionInstantiationTest.val::new);
    }
    static void foo(Supplier<ProjectionInstantiationTest.ref> sx) {
        sx.get();
    }
}
