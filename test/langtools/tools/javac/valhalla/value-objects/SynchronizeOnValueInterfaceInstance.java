/*
 * @test /nodynamiccopyright/
 * @bug 8287770
 * @summary [lw4] Javac tolerates synchronizing on an instance of a value interface
 * @compile/fail/ref=SynchronizeOnValueInterfaceInstance.out -XDrawDiagnostics -XDdev SynchronizeOnValueInterfaceInstance.java
 */

public value interface SynchronizeOnValueInterfaceInstance {

    default void foo(SynchronizeOnValueInterfaceInstance sovii) {
        synchronized (sovii) {} // Error
    }

}
