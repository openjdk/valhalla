/*
 * @test /nodynamiccopyright/
 * @bug 8210346
 * @summary inferred value typed `type arguments' are allowed by Javac even without -XDallowGenericsOverValues
 * @compile/fail/ref=InferredValueParameterizationTest.out -XDrawDiagnostics -XDdev InferredValueParameterizationTest.java
 *
 */

import java.util.List;

public inline class InferredValueParameterizationTest {
    int x = 10;

    static class Y<T> {
        Y(T t) {}
    }

    static <K> List<K> foo(K k) {
        return null;
    }

    public static void main(String [] args) {
       var list = List.of(new InferredValueParameterizationTest());
       Object o = new Y<>(new InferredValueParameterizationTest());
       o = new Y<>(new InferredValueParameterizationTest()) {};
       foo(new InferredValueParameterizationTest());
    }
}
