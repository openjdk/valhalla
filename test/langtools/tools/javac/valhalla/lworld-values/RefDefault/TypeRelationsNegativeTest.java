/*
 * @test /nodynamiccopyright/
 * @bug 8222792
 * @summary Javac should enforce the latest relationship rules between an inline type and its nullable projection
 * @compile/fail/ref=TypeRelationsNegativeTest.out -XDrawDiagnostics TypeRelationsNegativeTest.java
 */

final primitive class TypeRelationsNegativeTest.val {

    void foo() {
        TypeRelationsNegativeTest.val x = null; // error
        TypeRelationsNegativeTest xq = null;

        xq = x;
        xq = (TypeRelationsNegativeTest) x;
        xq = (TypeRelationsNegativeTest.val) x;
        x = xq;
        x = (TypeRelationsNegativeTest) xq;
        x = (TypeRelationsNegativeTest.val) xq;

        TypeRelationsNegativeTest.val [] xa = new TypeRelationsNegativeTest.val[] { null }; // error
        TypeRelationsNegativeTest [] xqa = new TypeRelationsNegativeTest.ref[] { null };

        xqa = xa;
        xqa = (TypeRelationsNegativeTest[]) xa;
        xa = xqa;// error
        xa = (TypeRelationsNegativeTest.val []) xqa;
    }
    int x = 10;
}
