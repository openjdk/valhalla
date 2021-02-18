/*
 * @test /nodynamiccopyright/
 * @bug 8221545
 * @summary Test Generics with ?
 * @compile/fail/ref=GenericsWithQuestion.out -XDrawDiagnostics GenericsWithQuestion.java
 */

import java.util.HashMap;

public class GenericsWithQuestion {

    primitive class V {
        int x = 10;
    }

    HashMap<V.ref, V.ref>good1;
    HashMap<V.ref, GenericsWithQuestion.V.ref>good2;
    HashMap<V, V>  bad1; // error;
    HashMap<V.ref, V> bad2; // error
    HashMap<V, V.ref> bad3; // error
}
