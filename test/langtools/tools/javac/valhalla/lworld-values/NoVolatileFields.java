/*
 * @test /nodynamiccopyright/
 * @bug 8230082
 * @summary Javac should not allow inline type's fields to be volatile (as they are final)
 * @compile/fail/ref=NoVolatileFields.out -XDrawDiagnostics NoVolatileFields.java
 */

public class NoVolatileFields {

    static class Foo {
        volatile final int i = 0; // Error
    }

    static primitive class Bar {
        volatile int i = 0; // Error
    }
}

