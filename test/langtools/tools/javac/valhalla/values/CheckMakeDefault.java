/*
 * @test /nodynamiccopyright/
 * @summary Check various semantic constraints on value creation via __MakeDefault
 *
 * @compile/fail/ref=CheckMakeDefault.out -XDenableValueTypes -XDrawDiagnostics CheckMakeDefault.java
 */
__ByValue final class Point {

    static final class Sinner {
        __ValueFactory static Sinner make() {
            return __MakeDefault Sinner(); // NO: Sinner is not a value class.
        }
    }

    __ByValue static final class SinnerValue {
        __ValueFactory static SinnerValue make() {
            return __MakeDefault SinnerValue(); // OK.
        }
    }

    final int x;
    final int y;

    Point() {}
    Point (int x, int y) {}

    Point badFactory(int x, int y) {
        return __MakeDefault Point(); // NO: Value created in a non-factory.
    }

    __ValueFactory static Point make(int x, int y) {
       Point p = __MakeDefault Point(10, 20); // NO arguments to default value creation
       String s = __MakeDefault String(); // NO: String cannot be produced in this factory.
       __MakeDefault SinnerValue(); // NO: Wrong factory.
       p = __MakeDefault Point();
       p.x = x;
       p.y = y;
       return p;
    }
}
