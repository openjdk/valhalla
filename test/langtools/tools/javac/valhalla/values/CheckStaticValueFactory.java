/*
 * @test /nodynamiccopyright/
 * @summary Check various semantic constraints on static value factory method
 *
 * @compile/fail/ref=CheckStaticValueFactory.out -XDenableValueTypes -XDrawDiagnostics CheckStaticValueFactory.java
 */
__ByValue __ValueFactory final class Point { // NO: A type cannot be __ValueFactory

    static class Sinner {
        final int x;
    }

    interface I {
        default __ValueFactory I foo() { // No: an interface method cannot be value factory
            return null;
        }
    }

    __ValueFactory final int x; // NO: A field cannot be value factory

    final int y;
    final int z = 0;

    __ValueFactory Point() { // NO: A constructor cannot be value factory
    }

    __ValueFactory Point badFactory(int x, int y) { // No: factory must be a static method
        return __MakeDefault Point();
    }

    __ValueFactory static String makeString(int x, int y) { // NO: bad return type for factory
        String s = __MakeDefault String(); // NO: String is not a value type
        return s;
    }

    __ValueFactory static Point make(int x, int y, int z) {
       Point p = __MakeDefault Point();
       p.x = x; // OK: allow update to blank final field via copy on write`
       p.y = y; // OK: allow update to blank final field via copy on write`
       p.z = z; // !OK, do not allow update to a non blank final even in a value factory.
       Sinner s = new Sinner();
       s.x = 10; // NO: No write to final field.
       return p;
    }

    static Point nonFactory(int x, int y, int z) {
       Point p = __MakeDefault Point(); // NO: cannot create default value in non-factory
       p.x = x; // Error: No write to final field.
       p.y = y; // Error: No write to final field.
       p.z = z; // Error: No write to final field.
       return p;
    }
}
