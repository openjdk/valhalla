/*
 * @test /nodynamiccopyright/
 * @bug 8221545
 * @summary Test Generics with ?
 * @compile/fail/ref=GenericsWithQuestion.out -XDrawDiagnostics GenericsWithQuestion.java
 */

import java.util.HashMap;

public class GenericsWithQuestion {

    primitive class V.val {
        int x = 10;
    }

    HashMap<V, V>good1;
    HashMap<V.ref, GenericsWithQuestion.V.ref>good2;
    HashMap<V.val, V.val>  bad1; // error;
    HashMap<V.ref, V.val> bad2; // error
    HashMap<V.val, V.ref> bad3; // error
}
