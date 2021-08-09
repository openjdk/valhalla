/*
 * @test /nodynamiccopyright/
 * @bug 8244796 8244799
 * @summary Value class literal tests
 * @compile/fail/ref=ClassLiteralNegativeTest.out -XDrawDiagnostics ClassLiteralNegativeTest.java
 */

final primitive class ClassLiteralNegativeTest.val {
    Class<ClassLiteralNegativeTest.val> c1 = null; // error
    Class<? extends ClassLiteralNegativeTest.val> c2 = null; // error
    Class<? super ClassLiteralNegativeTest.val> c3 = null; // error
}
