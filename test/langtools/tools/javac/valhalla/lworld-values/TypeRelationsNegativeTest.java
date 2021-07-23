/*
 * @test /nodynamiccopyright/
 * @bug 8222792
 * @summary Javac should enforce the latest relationship rules between an inline type and its nullable projection
 * @compile/fail/ref=TypeRelationsNegativeTest.out -XDrawDiagnostics TypeRelationsNegativeTest.java
 */

final primitive class TypeRelationsNegativeTest {

    void foo() {
        TypeRelationsNegativeTest x = null; // error
        TypeRelationsNegativeTest.ref xq = null;

        xq = x;
        xq = (TypeRelationsNegativeTest.ref) x;
        xq = (TypeRelationsNegativeTest) x;
        x = xq;
        x = (TypeRelationsNegativeTest.ref) xq;
        x = (TypeRelationsNegativeTest) xq;

        TypeRelationsNegativeTest [] xa = new TypeRelationsNegativeTest[] { null }; // error
        TypeRelationsNegativeTest.ref [] xqa = new TypeRelationsNegativeTest.ref[] { null };

        xqa = xa;
        xqa = (TypeRelationsNegativeTest.ref[]) xa;
        xa = xqa;// error
        xa = (TypeRelationsNegativeTest []) xqa;
    }
    int x = 10;
}
