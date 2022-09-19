/*
 * @test /nodynamiccopyright/
 * @bug 8244711
 * @summary Javac should complain about an inline class with conflicting super interfaces.
 * @compile/fail/ref=ConflictingSuperInterfaceTest.out -XDrawDiagnostics ConflictingSuperInterfaceTest.java
 */

public class ConflictingSuperInterfaceTest {

    interface I<T> {}
    static abstract class S implements I<String> {}
    primitive static class Foo extends S implements I<Integer> {
        String s = "";
    }
}
