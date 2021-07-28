/*
 * @test /nodynamiccopyright/
 * @bug 8221323
 * @summary  Javac should support class literals for projection types.
 * @compile/fail/ref=ClassLiteralTypingNegativeTest.out -XDrawDiagnostics ClassLiteralTypingNegativeTest.java
 */

public class ClassLiteralTypingNegativeTest {

    public static primitive class Foo {
        final int value = 0;

        public static void main(String[] args) {
            Class<? extends Foo.ref> cFooRef = Foo.class.asValueType(); // Error
            cFooRef = new Foo().getClass(); // OK.
            cFooRef = Foo.ref.class; // OK.
            cFooRef = Foo.val.class; // Error.
            Foo.val xv = new Foo();
            cFooRef = xv.getClass(); // OK.
            Foo.ref xr = new Foo();
            cFooRef = xr.getClass(); // OK.
        }
    }

    interface I {}

    public static primitive class Bar implements I {
        final int value = 0;

        public static void main(String[] args) {
            Class<? extends Bar.ref> cBarRef = Bar.class.asValueType(); // Error
            cBarRef = new Bar().getClass(); // OK.
            cBarRef = Bar.ref.class; // OK.
            cBarRef = Bar.val.class; // Error.
            Bar.val xv = new Bar();
            cBarRef = xv.getClass(); // OK
            Bar.ref xr = new Bar();
            cBarRef = xr.getClass(); // OK.
        }
    }
}
