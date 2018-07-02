/*
 * @test /nodynamiccopyright/
 * @summary Check behavior of synzhronized key word on value instances and methods.
 *
 * @compile/fail/ref=CheckSynchronized.out -XDrawDiagnostics CheckSynchronized.java
 */

__ByValue final class CheckSynchronized {
    synchronized void foo() { // <<-- ERROR, no monitor associated with `this'
    }
    void goo() {
        synchronized(this) {} // <<-- ERROR, no monitor associated with `this'
    }
    synchronized static void zoo(CheckSynchronized cs) { // OK, static method.
        synchronized(cs) {    // <<-- ERROR, no monitor associated with value instance.
        }
    }
    int x = 10;
}
