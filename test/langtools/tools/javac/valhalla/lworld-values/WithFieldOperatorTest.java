/*
 * @test /nodynamiccopyright/
 * @summary Verify that various errors related to __WithField operator are caught.
 * @compile/fail/ref=WithFieldOperatorTest.out -XDallowWithFieldOperator -XDrawDiagnostics -XDdev WithFieldOperatorTest.java
 */

public class WithFieldOperatorTest {
    static int xs;
    int ifld;
    class Y {}
    public final __ByValue class V { int x = 10; }

    public final __ByValue class X {

        final int x;
        final V v;

        X() {
            x = 10;
            v = V.default;
        }
        
        X getX(int xVal, WithFieldOperatorTest wfot) {
            X x = X.default;
            x = __WithField(new Y(), null);  // not a variable at all.
            x = __WithField(wfot.xs, 10); // not an instance field.
            x = __WithField(wfot.ifld, 10); // not a field of value type
            x = __WithField(xVal, xVal); // not a field
            x = __WithField(this, this); // not a field
            x = __WithField(X.this, this); // not a field
            x = __WithField(x.x, 12.0); // float cannot be assigned to int
            x = __WithField(x.v, null); // null cannot be assigned to value 
            return x;
        }
    }
}

class WithFieldOperatorTest_aux {
    void foo(WithFieldOperatorTest.X x) {
        x = __WithField(x.x, 10); // outside the nest
    }
}
