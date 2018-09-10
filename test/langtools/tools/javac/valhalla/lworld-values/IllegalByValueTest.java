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
        __MakeDefault __ByValue IllegalByValueTest() {};
        __MakeDefault __ByValue __ByValue IllegalByValueTest() {};
        __MakeDefault @Annot __ByValue IllegalByValueTest() {};
        new __ByValue @Annot __ByValue Comparable <String>() {};
        int [] ia = new __ByValue int[10];
        new __ByValue String("Hello");
    }
}
