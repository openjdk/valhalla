/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support identityHashCode
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckIdentityHash.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values  CheckIdentityHash.java
 */
@jdk.incubator.mvt.ValueCapableClass
final class CheckIdentityHash {
    int identityHashCode(CheckIdentityHash x) {
        return 0;
    }
    void test(CheckIdentityHash v) {
        this.identityHashCode(v);      // <- ok
        System.identityHashCode(v);    // <- error
        System.identityHashCode(this); // <- error
        java.lang.System.identityHashCode(v);    // <- error
        java.lang.System.identityHashCode(this); // <- error
    }
}
