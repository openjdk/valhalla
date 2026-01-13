/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for subtyping checks in witness lookups
 * @compile/fail/ref=TypeClassesSubTypingTest.out -XDrawDiagnostics TypeClassesSubTypingTest.java
 */
class TypeClassesSubTypingTest {
    interface A<X> { }
    interface B<X> extends A<X> { }

    interface C<W> { A<W> a(); }
    interface D<X> extends C<X> { }

    static class E {
        __witness B<E> BE = new B<>() { };
        __witness D<E> DE(A<E> az) {
            return new D<>() {
                public A<E> a() {
                    return az;
                }
            };
        }
    }

    void test() {
        A<E> ae = A<E>.__witness; // fail, not an exact match
        B<E> be = B<E>.__witness; // ok, exact match
        C<E> ce = C<E>.__witness; // fail, not an exact match
        D<E> de = D<E>.__witness; // fail, exact match, but D<E> depends on A<E> which is not an exact match
    }
}
