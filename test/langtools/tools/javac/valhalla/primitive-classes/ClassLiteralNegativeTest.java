/*
 * @test /nodynamiccopyright/
 * @bug 8244796 8244799
 * @summary Value class literal tests
 * @compile -XDenablePrimitiveClasses ClassLiteralNegativeTest.java
 */

final value class ClassLiteralNegativeTest {
    Class<ClassLiteralNegativeTest> c1 = null; // OK
    Class<? extends ClassLiteralNegativeTest> c2 = null; // OK
    Class<? super ClassLiteralNegativeTest> c3 = null; // OK
}
