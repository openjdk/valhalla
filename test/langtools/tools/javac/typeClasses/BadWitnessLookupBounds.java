import java.util.function.Function;

/*
 * @test /nodynamiccopyright/
 * @summary Test for witness lookup which violates type-variable bounds
 * @compile/fail/ref=BadWitnessLookupBounds.out -XDrawDiagnostics BadWitnessLookupBounds.java
 */
class BadWitnessLookupBounds {
    interface Comparator<X> {
        int compare(X a, X b);

        __witness <Z extends Comparable<Z>> Comparator<Z> INT() {
            return (a, b) -> a.compareTo(b);
        }
    }

    interface Convertible<X, Y> {
        Y convertTo(X a);

        __witness <Z> Convertible<Z, Z> IDENTITY() {
            return a -> a;
        }
    }

    void testComparator() {
        var r = Comparator<Runnable>.__witness; // fail
        var s = Comparator<String>.__witness; // ok
        var i = Comparator<Integer>.__witness; // ok
    }

    void testConvertible() {
        var is = Convertible<Integer, String>.__witness; // fail
        var ss = Convertible<String, String>.__witness; // ok
        var ii = Convertible<Integer, Integer>.__witness; // ok
        var si = Convertible<String, Integer>.__witness; // fail
    }
}
