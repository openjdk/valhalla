/*
 * @test /nodynamiccopyright/
 * @summary Check behavior of synzhronized key word on value instances and methods.
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckSynchronized.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values  CheckSynchronized.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckSynchronized {
    synchronized void foo() { // <<-- ERROR, no monitor associated with `this'
    }
    void goo() {
        synchronized(this) {} // <<-- ERROR, no monitor associated with `this'
    }
    synchronized static void zoo(CheckSynchronized cs) { // OK, static method.
        synchronized(cs) {    // <<-- ERROR, no monitor associated with value instance.
        }
    }
}
