/*
 * @test /nodynamiccopyright/
 * @summary Check that type annotations in intersection types do not lead to duplicated where clauses
 * @compile/fail/ref=TestDuplicatedIntersection.out -XDrawDiagnostics -XDrawDiagnostics --diags=formatterOptions=where TestDuplicatedIntersection.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

class TestDuplicatedIntersection {
    @Target(ElementType.TYPE_USE)
    @interface A { }

    interface S1 { }
    interface S2 { }

    interface B extends @A S1, @A S2 { }
    interface C extends @A S1, @A S2 { }

    interface D extends S1, S2 { }
    interface E extends S1, S2 { }

    <X> X pick(X a, X b) { return null; }

    void test1(List<B> b, List<C> c, List<D> d, List<E> e) {
        var x = pick(b, c);
        var y = pick(d, e);
        g(x, y);
    }

    void g(String s) { }
}
