/*
 * @test /nodynamiccopyright/
 * @bug 8244513
 * @summary Test conditional expression typing involving inlines.
 * @compile/fail/ref=ConditionalTypeTest.out -XDrawDiagnostics ConditionalTypeTest.java
 */

final class ConditionalTypeTest {
    interface I {}
    static primitive class Node implements I {}
    static void foo(int i) {
        var ret1 = (i == 0) ? new XNodeWrapper() : new Node();
        ret1 = "String cannot be assigned to I";
        var ret2 = (i == 0) ? 10 : new XNodeWrapper();
        ret2 = "String can be assigned to I";
        var ret3 = (i == 0) ? new XNodeWrapper() : 10;
        ret3 = "String can be assigned to Object";
        var ret4 = (i == 0) ? new XNodeWrapper() : new ConditionalTypeTest();
        ret4 = "String can be assigned to Object";
        var ret5 = (i == 0) ? Integer.valueOf(10) : new ConditionalTypeTest();
        ret5 = "String can be assigned to Object";

        var ret6 = (i == 0) ? new Node() : new Node();
        ret6 = "String cannot be assigned to Node";

        var ret7 = (i == 0) ? (Node.ref) new Node() : (Node.ref) null;
        ret7 = "String cannot be assigned to Node.ref";

        var ret8 = (i == 0) ? new Node() : (Node.ref) null;
        ret8 = "String cannot be assigned to Node";

        var ret9 = (i == 0) ? (Node.ref) new Node() : new Node();
        ret9 = "String cannot be assigned to Node";
    }
    static primitive class XNodeWrapper implements I {}
}
