/*
 * @test /nodynamiccopyright/
 * @bug 8264216
 * @summary [lworld] unknown.Class.default gives misleading compilation error
 * @compile/fail/ref=UnknownTypeDefault.out -Xlint:all -Werror -XDrawDiagnostics -XDdev UnknownTypeDefault.java
 */

public class UnknownTypeDefault {

    public static void main(String [] args) {
        Object d1 = Y.default;
        Object d2 = y.Z.default;
    }
}
