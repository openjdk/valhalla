/*
 * @test /nodynamiccopyright/
 * @summary overload resolution tests for universal type variables
 * @compile/ref=UniversalTVarsOverloadResolutionTest.out -XDrawDiagnostics --should-stop=ifError=ATTR --should-stop=ifNoError=ATTR --debug=verboseResolution=applicable,success,deferred-inference UniversalTVarsOverloadResolutionTest.java
 */

class UniversalTVarsOverloadResolutionTest {
    interface MyCollection<__universal T> {}

    static class MyList<__universal T> implements MyCollection<T> {
        static <T> MyList<T> of(T element) { return null; }
    }

    interface Shape {}
    primitive class Point implements Shape {}

    <__universal X> void m1(X x) {}
    void m1(Point.ref p) {}

    void m2(Point p, Object o) {}
    <__universal X> void m2(X x, String s) {}

    void test() {
        MyList.of(new Point());
        MyCollection<Point.ref> refColl = MyList.of(new Point());
        MyCollection<Shape> shapeColl = MyList.of(new Point());

        m1(Point.default);

        m2(Point.default, null);
    }
}
