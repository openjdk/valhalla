/*
 * @test /nodynamiccopyright/
 * @summary Check that the witness lookup follows expected order
 * @compile/fail/ref=WitnessLookupPriorityTest.out -XDrawDiagnostics WitnessLookupPriorityTest.java
 */
public class WitnessLookupPriorityTest {
    interface Triple<X, Y, Z> {
        String name();
    }

    class A {
        __witness Triple<A, B, C> W1 = () -> "A";
        __witness Triple<A, C, B> W2 = () -> "A";
        __witness Triple<B, A, C> W3 = () -> "A";
        __witness Triple<B, C, A> W4 = () -> "A";
        __witness Triple<C, A, B> W5 = () -> "A";
        __witness Triple<C, B, A> W6 = () -> "A";
    }

    class B {
        __witness Triple<A, B, C> W1 = () -> "B";
        __witness Triple<A, C, B> W2 = () -> "B";
        __witness Triple<B, A, C> W3 = () -> "B";
        __witness Triple<B, C, A> W4 = () -> "B";
        __witness Triple<C, A, B> W5 = () -> "B";
        __witness Triple<C, B, A> W6 = () -> "B";
    }

    class C {
        __witness Triple<A, B, C> W1 = () -> "C";
        __witness Triple<A, C, B> W2 = () -> "C";
        __witness Triple<B, A, C> W3 = () -> "C";
        __witness Triple<B, C, A> W4 = () -> "C";
        __witness Triple<C, A, B> W5 = () -> "C";
        __witness Triple<C, B, A> W6= () -> "C";
    }

    public static void checkLeftToRight() {
        checkEquals(Triple<A, B, C>.__witness.name(), "A"); // ambiguous
        checkEquals(Triple<A, C, B>.__witness.name(), "A"); // ambiguous
        checkEquals(Triple<B, A, C>.__witness.name(), "B"); // ambiguous
        checkEquals(Triple<B, C, A>.__witness.name(), "B"); // ambiguous
        checkEquals(Triple<C, A, B>.__witness.name(), "C"); // ambiguous
        checkEquals(Triple<C, B, A>.__witness.name(), "C"); // ambiguous
    }

    interface Mono<X> {
        String name();
    }

    class D<X> {
        __witness Mono<D<E<F<String>>>> W1 = () -> "D";
        __witness Mono<D<F<E<String>>>> W2 = () -> "D";
        __witness Mono<E<D<F<String>>>> W3 = () -> "D";
        __witness Mono<E<F<D<String>>>> W4 = () -> "D";
        __witness Mono<F<D<E<String>>>> W5 = () -> "D";
        __witness Mono<F<E<D<String>>>> W6 = () -> "D";
    }

    class E<X> {
        __witness Mono<D<E<F<String>>>> W1 = () -> "E";
        __witness Mono<D<F<E<String>>>> W2 = () -> "E";
        __witness Mono<E<D<F<String>>>> W3 = () -> "E";
        __witness Mono<E<F<D<String>>>> W4 = () -> "E";
        __witness Mono<F<D<E<String>>>> W5 = () -> "E";
        __witness Mono<F<E<D<String>>>> W6 = () -> "E";
    }

    class F<X> {
        __witness Mono<D<E<F<String>>>> W1 = () -> "F";
        __witness Mono<D<F<E<String>>>> W2 = () -> "F";
        __witness Mono<E<D<F<String>>>> W3 = () -> "F";
        __witness Mono<E<F<D<String>>>> W4 = () -> "F";
        __witness Mono<F<D<E<String>>>> W5 = () -> "F";
        __witness Mono<F<E<D<String>>>> W6 = () -> "F";
    }

    public static void checkInnerToOuter() {
        checkEquals(Mono<D<E<F<String>>>>.__witness.name(), "F");
        checkEquals(Mono<D<F<E<String>>>>.__witness.name(), "E");
        checkEquals(Mono<E<D<F<String>>>>.__witness.name(), "F");
        checkEquals(Mono<E<F<D<String>>>>.__witness.name(), "D");
        checkEquals(Mono<F<D<E<String>>>>.__witness.name(), "E");
        checkEquals(Mono<F<E<D<String>>>>.__witness.name(), "D");
    }

    public static void main(String[] args) {
        checkLeftToRight();
        checkInnerToOuter();
    }

    static void checkEquals(Object found, Object expected) {
        if (!found.equals(expected)) throw new AssertionError(String.format("Found %s, expected %s", found, expected));
    }
}
