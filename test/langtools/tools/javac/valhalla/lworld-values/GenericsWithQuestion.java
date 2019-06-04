/*
 * @test /nodynamiccopyright/
 * @bug 8221545
 * @summary Test Generics with ?
 * @compile/fail/ref=GenericsWithQuestion.out -XDrawDiagnostics GenericsWithQuestion.java 
 */

import java.util.HashMap;

public class GenericsWithQuestion {

    inline class V {
        int x = 10;
    }

    HashMap<V?, V?>good1;
    HashMap<V?, GenericsWithQuestion.V?>good2;
    HashMap<V, V>  bad1; // error;
    HashMap<V?, V> bad2; // error;
    HashMap<V, V?> bad3; // error;
}
