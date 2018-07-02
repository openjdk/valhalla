/*
 * @test /nodynamiccopyright/
 * @summary Check various semantic constraints on value creation via __MakeDefault
 *
 * @compile/fail/ref=CheckMakeDefault.out -XDrawDiagnostics CheckMakeDefault.java
 */
__ByValue final class Point {

    __ByValue interface I { int x = 10; } // Error
    __ByValue abstract class A { int x = 10; } // Error
    static final class Sinner {
        static Sinner make() {
            return __MakeDefault Sinner(); // NO: Sinner is not a value class.
        }
    }

    __ByValue static final class SinnerValue {
        static SinnerValue make() {
            return __MakeDefault SinnerValue(); // OK.
        } int x = 10;
    }

    final int x;
    final int y;

    Point() {}
    Point (int x, int y) {}

    Point badFactory(int x, int y) {
        return __MakeDefault Point();
    }

    static Point make(int x, int y) {
       Point p = __MakeDefault Point(10, 20); // NO arguments to default value creation
       String s = __MakeDefault String(); // NO: String cannot be produced in this factory.
       __MakeDefault SinnerValue();
       p = __MakeDefault Point();
       p = __WithField(p.x, x);
       p = __WithField(p.y, y);
       return p;
    }
}
