/*
 * @test /nodynamiccopyright/
 * @summary Check behavior of synzhronized key word on primitive class instances and methods.
 *
 * @compile/fail/ref=CheckSynchronized.out -XDrawDiagnostics CheckSynchronized.java
 */

primitive final class CheckSynchronized implements java.io.Serializable {
    synchronized void foo() { // <<-- ERROR, no monitor associated with `this'
    }
    void goo() {
        synchronized(this) {} // <<-- ERROR, no monitor associated with `this'
    }
    synchronized static void zoo(CheckSynchronized cs) { // OK, static method.
        synchronized(cs) {    // <<-- ERROR, no monitor associated with primitive class instance.
        }

        CheckSynchronized.ref csr = cs;
        synchronized(csr) {
            // Error, no identity.
        }

        synchronized(x) {
            // Error, no identity.
        }

        Object o = cs;
        synchronized(o) {
            // Error BUT not discernible at compile time
        }
        java.io.Serializable jis = cs;
        synchronized(jis) {
            // Error BUT not discernible at compile time
        }
    }
    static int x = 10;
}
