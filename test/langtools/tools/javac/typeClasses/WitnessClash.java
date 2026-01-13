/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for witness declaration clashes
 * @compile/fail/ref=WitnessClash.out -XDrawDiagnostics WitnessClash.java
 */
class WitnessClash {
    interface T<X> { }

    interface U<X> extends T<X> { }
    interface W<X> extends T<X> { }

    interface G<X> extends T<K> { }
    interface H<X> extends T<K> { }

    static class K { }

    // witness field only
    static class A {
        __witness T<A> A1 = null; // clash with A2
        __witness T<A> A2 = null; // clash with A1 (but no duplicate error)
    }

    // witness method only
    static class B {
        __witness T<B> B1() { return null; } // clash with B2
        __witness T<B> B2() { return null; } // clash with B1 (but no duplicate error)
    }

    // witness field and method
    static class C {
        __witness T<C> C1 = null; // clash with C2
        __witness T<C> C2() { return null; } // clash with C1 (but no duplicate error)
    }

    // generic vs. non-generic clash
    static class D<X> {
        __witness T<D<Integer>> D1 = null; // clash with D2 (where Z == Integer)
        __witness <Z extends Number> T<D<Z>> D2() { return null; } // clash by D1 (where Z == Integer, but no duplicate error)
        __witness <Z extends StringBuilder> T<D<Z>> D3() { return null; } // ok
    }
}
