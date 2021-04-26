/*
 * @test /nodynamiccopyright/
 * @bug 8264977
 * @summary A primitive class field by name val confuses javac
 * @compile/fail/ref=ValRefTokensNegativeTest.out -XDrawDiagnostics ValRefTokensNegativeTest.java
 */

public class ValRefTokensNegativeTest  {

    ValRefTokensNegativeTest.ref aa = null;
    static ValRefTokensNegativeTest.val bb = ValRefTokensNegativeTest.default;

    EmptyValue empty = EmptyValue.default;

    static class ValRefTokensTestWrapper {
       ValRefTokensNegativeTest val = ValRefTokensNegativeTest.default;
       ValRefTokensNegativeTest ref = ValRefTokensNegativeTest.default;
    }

    public EmptyValue test139(int x) {
        ValRefTokensTestWrapper w = new ValRefTokensTestWrapper();
        return x == 0 ? w.val.empty : w.ref.empty;
    }

    int valx() {
        return EmptyValue.val.x;
    }

    int refx() {
        return EmptyValue.ref.x;
    }

    static class EmptyValue {
        static int x = 42;
    }

    public static void main(String [] args) {
        if (new ValRefTokensNegativeTest().valx() != new ValRefTokensNegativeTest().refx())
            throw new AssertionError("Broken");
        if (new ValRefTokensNegativeTest().test139(0).x != new ValRefTokensNegativeTest().test139(1).x)
            throw new AssertionError("Broken");
    }
}
