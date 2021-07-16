/*
 * @test /nodynamiccopyright/
 * @summary Value types cannot parameterize generic types (except under experimental mode)
 * @compile/fail/ref=ValueOverGenericsTest.out -XDrawDiagnostics ValueOverGenericsTest.java
 *
 */

import java.util.ArrayList;
import java.io.Serializable;

primitive class ValueOverGenericsTest.val {
    int x = 10;
    ArrayList<ValueOverGenericsTest.val> ax = null;
    void foo(ArrayList<? extends ValueOverGenericsTest.val> p) {
        new <ValueOverGenericsTest.val> ArrayList<Object>();
        this.<ValueOverGenericsTest.val>foo(null);
        Object o = (ValueOverGenericsTest.val & Serializable) null;
    }
}
