/*
 * @test /nodynamiccopyright/
 * @summary Check various semantic constraints on value creation via default
 *
 * @compile/fail/ref=CheckMakeDefault.out -XDrawDiagnostics -XDenablePrimitiveClasses -XDenableNullRestrictedTypes CheckMakeDefault.java
 */
value final class Point {
    value interface I { int x = 10; } // Error
    value abstract class A { int x = 10; } // Error
    static final class Sinner {
        static Sinner make() {
            return Sinner.default;
        }
    }

    value static final class SinnerValue {
        public implicit SinnerValue();
        static SinnerValue make() {
            return SinnerValue.default;
        }
    }

    final int x;
    final int y;

    final int nonbool;
    final boolean nonbyte;
    final boolean nonchar;
    final boolean nonint;
    final boolean nonshort;
    final boolean nonlong;
    final boolean nonfloat;
    final boolean nondouble;
    final int nonString;
    final int nonbyteArray;

    public implicit Point();
    Point (int x, int y) {
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
    }

    static Point make(int x, int y) {
       Point p = Point.default;
       String s = String.default;
       Object o = SinnerValue.default;
       return new Point(x, y);
    }
}
