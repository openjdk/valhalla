/*
 * @test /nodynamiccopyright/
 * @bug 8222792
 * @summary Javac should enforce the latest relationship rules between an inline type and its nullable projection
 * @compile/fail/ref=TypeRelationsNegativeTest.out -XDrawDiagnostics TypeRelationsNegativeTest.java
 */

final inline class TypeRelationsNegativeTest {

    void foo() {
        TypeRelationsNegativeTest x = null; // error
        TypeRelationsNegativeTest? xq = null;

        xq = x;
        xq = (TypeRelationsNegativeTest?) x;
        xq = (TypeRelationsNegativeTest) x;
        x = xq;  // error
        x = (TypeRelationsNegativeTest?) xq; // error
        x = (TypeRelationsNegativeTest) xq;
 
        TypeRelationsNegativeTest [] xa = new TypeRelationsNegativeTest[] { null }; // error
        TypeRelationsNegativeTest? [] xqa = new TypeRelationsNegativeTest?[] { null };

        xqa = xa;
        xqa = (TypeRelationsNegativeTest?[]) xa;
        xa = xqa; // error
        xa = (TypeRelationsNegativeTest []) xqa;
    }
    int x = 10;
}
