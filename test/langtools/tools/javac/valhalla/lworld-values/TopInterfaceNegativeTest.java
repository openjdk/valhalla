/**
 * @test /nodynamiccopyright/
 * @bug 8237069
 * @summary Introduce and wire-in the new top interfaces
 * @compile/fail/ref=TopInterfaceNegativeTest.out -XDrawDiagnostics TopInterfaceNegativeTest.java
 */

public class TopInterfaceNegativeTest  {

    interface ID extends IdentityObject {}
    interface II extends InlineObject {}

    interface IID0 extends IdentityObject, IdentityObject {} // Error.
    interface IID1 extends IdentityObject, InlineObject {} // Error.
    interface IID2 extends IdentityObject, II {} // Error.
    interface IID3 extends IdentityObject, ID {} // OK.
    interface IID4 extends InlineObject, II {} // OK.
    interface IID5 extends ID, II {} // Error

    static class C1 implements InlineObject {} // Error
    static class C2 implements II {} // Error
    static class C3 implements IdentityObject {} // Ok
    static class C4 implements ID {} // Ok
    static class C5 implements IdentityObject, IdentityObject {} // Error.
    static class C6 implements IdentityObject, ID {} // OK
    static class C7 implements II, ID {} // Error

    static inline class V1 implements IdentityObject {} // error.
    static inline class V2 implements InlineObject {} // Ok.
    static inline class V3 implements InlineObject, InlineObject  {} // error.

    void foo(V2 v) {
        if (v instanceof IdentityObject)
            throw new AssertionError("Expected inline object but found identity object");
    }
}
