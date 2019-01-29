/*
 * @test /nodynamiccopyright/
 * @bug 8217958
 * @summary Trouble assigning/casting to a value array type with parameterized element type
 * @compile/fail/ref=GenericArrayTest.out -Xlint:all -Werror -XDrawDiagnostics -XDdev GenericArrayTest.java
 */

public class GenericArrayTest {

    public value class Value<T> {

        T t = null;

        void foo() {
            Value<T>[] v = new Value[1024];
            Value<GenericArrayTest>[] vx = new Value[1024];
            Value<String>[] vs = new Value[1024];
            v = (Value<T> []) new Value[1024];
            vx = (Value <GenericArrayTest> [])new Value[1024];
            vs = (Value <String> []) new Value[1024];
            vx = vs;
        }
    }
}
