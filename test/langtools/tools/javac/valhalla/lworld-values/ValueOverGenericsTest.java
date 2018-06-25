/*
 * @test /nodynamiccopyright/
 * @summary Value types cannot parameterize generic types (except under experimental mode)
 * @compile/fail/ref=ValueOverGenericsTest.out -XDrawDiagnostics ValueOverGenericsTest.java
 * @compile/fail/ref=ValueOverGenericsTest2.out -XDrawDiagnostics -XDallowGenericsOverValues ValueOverGenericsTest.java
 */

import java.util.ArrayList;
import java.io.Serializable;

__ByValue class ValueOverGenericsTest {
    int x = 10;
    ArrayList<ValueOverGenericsTest> ax = null;
    void foo(ArrayList<? extends ValueOverGenericsTest> p) {
        new <ValueOverGenericsTest> ArrayList<Object>();
        this.<ValueOverGenericsTest>foo(null);
        Object o = (ValueOverGenericsTest & Serializable) null;
    }
}
