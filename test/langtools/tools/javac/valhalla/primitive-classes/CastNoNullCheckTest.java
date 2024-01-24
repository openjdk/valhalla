/*
 * @test /nodynamiccopyright/
 * @summary Check that casting to a value type involves no null check when values are not recognized in source.
 * @compile -XDenablePrimitiveClasses Point.java
 * @compile/fail/ref=CastNoNullCheckTest.out -source 10 -XDrawDiagnostics CastNoNullCheckTest.java
 * @ignore
 */

public class CastNoNullCheckTest {
    void m() {
        Object o = null;
        Point p = (Point) o;
    }
}
