/*
 * @test /nodynamiccopyright/
 * @summary Check various semantic constraints on value creation via default
 *
 * @compile/fail/ref=CheckMakeDefault.out -XDrawDiagnostics CheckMakeDefault.java
 */
primitive final class Point {

    primitive interface I { int x = 10; } // Error
    primitive abstract class A { int x = 10; } // Error
    static final class Sinner {
        static Sinner make() {
            return Sinner.default;
        }
    }

    primitive static final class SinnerValue {
        static SinnerValue make() {
            return SinnerValue.default;
        } int x = 10;
    }

    final int x;
    final int y;

    final int nonbool = boolean.default;
    final boolean nonbyte = byte.default;
    final boolean nonchar = char.default;
    final boolean nonint = int.default;
    final boolean nonshort = short.default;
    final boolean nonlong = long.default;
    final boolean nonfloat = float.default;
    final boolean nondouble = double.default;
    final int nonString = String.default;
    final int nonbyteArray = byte[].default;

    Point() {}
    Point (int x, int y) {}

    static Point make(int x, int y) {
       Point p = Point.default;
       String s = String.default;
       Object o = SinnerValue.default;
       return new Point(x, y);
    }
}
