/**
 * @test /nodynamiccopyright/
 * @bug 8237069
 * @summary Introduce and wire-in the new top interfaces
 * @compile/fail/ref=TopInterfaceNegativeTest.out -XDrawDiagnostics TopInterfaceNegativeTest.java
 */

public class TopInterfaceNegativeTest  {

    interface ID extends IdentityObject {}
    interface II extends InlineObject {}

    interface IID0 extends IdentityObject, IdentityObject {}
    interface IID1 extends IdentityObject, InlineObject {}
    interface IID2 extends IdentityObject, II {}
    interface IID3 extends IdentityObject, ID {}
    interface IID4 extends InlineObject, II {}
    interface IID5 extends ID, II {}

    static class C1 implements InlineObject {}
    static class C2 implements II {}
    static class C3 implements IdentityObject {}
    static class C4 implements ID {}
    static class C5 implements IdentityObject, IdentityObject {}
    static class C6 implements IdentityObject, ID {}
    static class C7 implements II, ID {}

    static inline class V1 implements IdentityObject { int x = 0; }
    static inline class V2 implements InlineObject {}
    static inline class V3 implements InlineObject, InlineObject  {}

    void foo(V2 v) {
        if (v instanceof IdentityObject)
            throw new AssertionError("Expected inline object but found identity object");
    }
    abstract class abs implements IdentityObject {}
}
