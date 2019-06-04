/*
 * @test /nodynamiccopyright/
 * @summary Check various semantic constraints on value creation via default
 *
 * @compile/fail/ref=CheckMakeDefault.out -XDallowWithFieldOperator -XDrawDiagnostics CheckMakeDefault.java
 */
inline final class Point {

    inline interface I { int x = 10; } // Error
    inline abstract class A { int x = 10; } // Error
    static final class Sinner {
        static Sinner make() {
            return Sinner.default; // NO: Sinner is not a value class.
        }
    }

    inline static final class SinnerValue {
        static SinnerValue make() {
            return SinnerValue.default; // OK.
        } int x = 10;
    }

    final int x;
    final int y;

    Point() {}
    Point (int x, int y) {}

    Point badFactory(int x, int y) {
        return Point.default;
    }

    static Point make(int x, int y) {
       Point p = Point.default;
       String s = String.default;
       Object o = SinnerValue.default;
       p = Point.default;
       p = __WithField(p.x, x);
       p = __WithField(p.y, y);
       return p;
    }
}
