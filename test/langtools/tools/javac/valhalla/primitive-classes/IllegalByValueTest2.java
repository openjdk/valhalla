/*
 * @test /nodynamiccopyright/
 * @bug 8209400 8215246
 * @summary Allow anonymous classes to be primitive class types
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
        new @Annot primitive @Annot IllegalByValueTest2() {};
    }
}
