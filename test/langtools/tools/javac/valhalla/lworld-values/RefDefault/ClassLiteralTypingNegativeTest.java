/*
 * @test /nodynamiccopyright/
 * @bug 8221323
 * @summary  Javac should support class literals for projection types.
 * @compile/fail/ref=ClassLiteralTypingNegativeTest.out -XDrawDiagnostics ClassLiteralTypingNegativeTest.java
 */

public class ClassLiteralTypingNegativeTest {

    public static primitive class Foo.val {
        final int value = 0;

        public static void main(String[] args) {
            Class<? extends Foo> cFooRef = Foo.val.class; // Error
            cFooRef = ((Foo.val) new Foo()).getClass(); // OK
            cFooRef = Foo.class; // OK.
            cFooRef = Foo.val.class; // Error.
            Foo.val xv = new Foo();
            cFooRef = xv.getClass(); // OK
            Foo xr = new Foo();
            cFooRef = xr.getClass(); // OK.
        }
    }

    interface I {}

    public static primitive class Bar.val implements I {
        final int value = 0;

        public static void main(String[] args) {
            Class<? extends Bar> cBarRef = Bar.val.class; // Error
            cBarRef = ((Bar.val) new Bar()).getClass(); // OK
            cBarRef = Bar.class; // OK.
            cBarRef = Bar.val.class; // Error.
            Bar.val xv = new Bar();
            cBarRef = xv.getClass(); // OK.
            Bar xr = new Bar();
            cBarRef = xr.getClass(); // OK.
        }
    }
}
