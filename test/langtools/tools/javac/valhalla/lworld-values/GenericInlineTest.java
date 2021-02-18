/*
 * @test /nodynamiccopyright/
 * @bug 8237072
 * @summary Test various relationships between a value type and its reference projection.
 * @compile/fail/ref=GenericInlineTest.out -XDrawDiagnostics GenericInlineTest.java
 */

abstract class Low<T, U> {}
abstract class Mid<T, U> extends Low<U, T> {}
abstract class High<T, U> extends Mid<U, T> {}

primitive
class GenericInlineTest<T, U> extends High<U, T> {

    int x = 0;

    void foo() {

        GenericInlineTest<String, Integer> g = new GenericInlineTest<String, Integer>();

        High<String, Integer> h1 = g; // error.

        High<Integer, String> h2 = g; // Ok.

        Mid<String, Integer> m1 = g; // Ok

        Mid<Integer, String> m2 = g; // error.

        Low<String, Integer> l1 = g; // error.

        Low<Integer, String> l2 = g; // Ok.

        g = l2; // error.
        g = (GenericInlineTest<String, Integer>) l2; // OK.

        GenericInlineTest.ref<String, Integer> r1 = g; // ok.
        GenericInlineTest.ref<Integer, String> r2 = g; // error

        g = r1; // ok.
        g = r2; // error.
        g = (GenericInlineTest<String, Integer>) r2; // still error.

    }
}
