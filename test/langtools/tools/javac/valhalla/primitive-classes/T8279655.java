/*
 * @test /nodynamiccopyright/
 * @bug 8279655
 * @summary Bogus error: incompatible types: Object cannot be converted to Foo
 * @compile T8279655.java
 */

public class T8279655 {

    sealed interface Foo permits Bar { }
    primitive class Bar implements Foo { }

    class Test {
        void test(Object o) {
            Foo foo = (Foo)o;
        }
    }
}
