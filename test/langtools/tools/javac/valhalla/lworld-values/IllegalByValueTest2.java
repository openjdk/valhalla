/*
 * @test /nodynamiccopyright/
 * @bug 8209400
 * @summary Allow anonymous classes to be value types
 * @compile/fail/ref=IllegalByValueTest2.out -XDrawDiagnostics -XDdev IllegalByValueTest2.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Comparator;

public class IllegalByValueTest2 {

    @Target(ElementType.TYPE_USE)
    @interface Annot {
    }

    public static void main(String[] args) {
        new @Annot __ByValue @Annot IllegalByValueTest2() {};
    }
}
