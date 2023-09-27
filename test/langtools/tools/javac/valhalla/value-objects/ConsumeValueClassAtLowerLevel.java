/*
 * @test /nodynamiccopyright/
 * @bug 8282107
 * @summary Check that javac can compile against value classes at lower source levels
 * @compile GenericPoint.java
 * @compile ConsumeValueClassAtLowerLevel.java
 * @compile --source 16 -XDrawDiagnostics ConsumeValueClassAtLowerLevel.java
 */

public class ConsumeValueClassAtLowerLevel {
    void m() {
        GenericPoint<Integer> gl = new GenericPoint<>(0, 0);
    }
}
