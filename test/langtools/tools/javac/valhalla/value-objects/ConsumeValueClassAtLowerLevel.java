/*
 * @test /nodynamiccopyright/
 * @bug 8282107
 * @summary Check that javac can compile against value classes at lower source levels
 * @compile GenericPoint.java
 * @compile ConsumeValueClassAtLowerLevel.java
 * @compile/fail/ref=ConsumeValueClassAtLowerLevel.out --source 16 -XDrawDiagnostics ConsumeValueClassAtLowerLevel.java
 */

public class ConsumeValueClassAtLowerLevel {
    void m() {
        /* GenericPoint was compiled with a source that allows value classes but ConsumeValueClassAtLowerLevel was not
         * so GenericPoint has a <vnew> initializer but in a source that doesn't allow value classes we look for <ini>
         * thus the compiler error in the second compilation of ConsumeValueClassAtLowerLevel.java
         */
        GenericPoint<Integer> gl = new GenericPoint<>(0, 0);
    }
}
