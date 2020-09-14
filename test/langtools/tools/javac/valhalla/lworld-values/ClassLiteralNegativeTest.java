/*
 * @test /nodynamiccopyright/
 * @bug 8244796 8244799
 * @summary Value class literal tests
 * @compile/fail/ref=ClassLiteralNegativeTest.out -XDrawDiagnostics ClassLiteralNegativeTest.java
 */

final inline class ClassLiteralNegativeTest {
    Class<ClassLiteralNegativeTest> c1 = null; // error
    Class<? extends ClassLiteralNegativeTest> c2 = null; // error
    Class<? super ClassLiteralNegativeTest> c3 = null; // error
}
