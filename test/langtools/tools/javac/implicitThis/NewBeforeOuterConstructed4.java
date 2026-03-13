/*
 * @test /nodynamiccopyright/
 * @bug 8379833
 * @summary Javac is rejecting valid constructor code
 *
 * @compile NewBeforeOuterConstructed4.java
 * @run main NewBeforeOuterConstructed4
 */
public class NewBeforeOuterConstructed4 {
    NewBeforeOuterConstructed4(NewBeforeOuterConstructed4 t) {}

    NewBeforeOuterConstructed4(String s) {
        this(new NewBeforeOuterConstructed4());
    }

    NewBeforeOuterConstructed4() {}

    public static void main(String[] args) {
        new NewBeforeOuterConstructed4("test");
    }
}
