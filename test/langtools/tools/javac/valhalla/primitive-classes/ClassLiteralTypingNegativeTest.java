/*
 * @test /nodynamiccopyright/
 * @bug 8221323
 * @summary  Javac should support class literals for projection types.
 * @modules java.base/jdk.internal.value
 * @compile/fail/ref=ClassLiteralTypingNegativeTest.out -XDrawDiagnostics -XDenablePrimitiveClasses ClassLiteralTypingNegativeTest.java
 */

import jdk.internal.value.PrimitiveClass;

public class ClassLiteralTypingNegativeTest {

    public static primitive class Foo {
        final int value = 0;

        public static void main(String[] args) {
            Class<? extends Foo.ref> cFooRef = PrimitiveClass.asValueType(Foo.class); // Error
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
            Class<? extends Bar.ref> cBarRef = PrimitiveClass.asValueType(Bar.class); // Error
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
