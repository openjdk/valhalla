/*
 * @test /nodynamiccopyright/
 * @bug 8244561
 * @summary Javac should not allow instantiation of V.ref or V.val
 * @compile/fail/ref=ProjectionInstantiationTest.out -XDrawDiagnostics ProjectionInstantiationTest.java
 */

final inline class ProjectionInstantiationTest {
    int x = 42;
    public static void main(String[] args) {
        new ProjectionInstantiationTest();
        new ProjectionInstantiationTest.ref();
        new ProjectionInstantiationTest.val();
    }
}
