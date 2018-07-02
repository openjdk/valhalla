/*
 * @test /nodynamiccopyright/
 * @summary Value types do not support identityHashCode
 *
 * @compile/fail/ref=CheckIdentityHash.out -XDrawDiagnostics CheckIdentityHash.java
 */

final __ByValue class CheckIdentityHash {
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
    int x = 10;
}
