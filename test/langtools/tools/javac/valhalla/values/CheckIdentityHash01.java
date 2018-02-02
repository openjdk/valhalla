/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support identityHashCode
 *
 * @compile/fail/ref=CheckIdentityHash01.out -XDenableValueTypes -XDrawDiagnostics CheckIdentityHash01.java
 */

import static java.lang.System.*;

final __ByValue class CheckIdentityHash01 {
    void test(CheckIdentityHash01 v) {

        identityHashCode(v);      // <- error
        identityHashCode(this);   // <- error

        System system = null;
        system.identityHashCode(v);      // <- error
        system.identityHashCode(this);   // <- error

        System.identityHashCode(v);      // <- error
        System.identityHashCode(this);   // <- error

        java.lang.System.identityHashCode(v);    // <- error
        java.lang.System.identityHashCode(this); // <- error
    }
}
