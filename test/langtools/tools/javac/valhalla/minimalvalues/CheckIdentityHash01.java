/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support identityHashCode
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckIdentityHash01.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values  CheckIdentityHash01.java
 */

import static java.lang.System.*;
@jdk.incubator.mvt.ValueCapableClass
final class CheckIdentityHash01 {
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
