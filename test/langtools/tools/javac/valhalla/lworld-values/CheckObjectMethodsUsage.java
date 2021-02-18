/*
 * @test /nodynamiccopyright/
 * @bug 8237066
 * @summary  Adjust inline type's interactions with jlO methods per latest spec
 * @compile/fail/ref=CheckObjectMethodsUsage.out -XDrawDiagnostics CheckObjectMethodsUsage.java
 */

public primitive class CheckObjectMethodsUsage {
    int x = 42;

    public void finalize() {}
    public Object clone() {}

    void testObjectMethods() {
        super.finalize();
        wait(0L, 0);
        super.wait(0L, 0);
        wait(0L);
        super.wait(0L);
        wait();
        super.wait();
        notify();
        super.notify();
        notifyAll();
        super.notifyAll();
        super.clone();
        super.hashCode();
        super.toString();
        super.equals(this);
        super.getClass();
    }
    public static void main(String [] args) {
        new CheckObjectMethodsUsage().testObjectMethods();
    }
}
