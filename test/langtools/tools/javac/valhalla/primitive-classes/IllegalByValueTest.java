/*
 * @test /nodynamiccopyright/
 * @bug 8209400
 * @summary Allow anonymous classes to be value types
 * @compile/fail/ref=IllegalByValueTest.out -XDrawDiagnostics -XDdev IllegalByValueTest.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Comparator;

public class IllegalByValueTest {

    @Target(ElementType.TYPE_USE)
    @interface Annot {
    }

    public static void main(String[] args) {
        // Error cases.
        new primitive @Annot primitive Comparable <String>() {};
        int [] ia = new primitive int[10];
        new primitive String("Hello");
    }
}
