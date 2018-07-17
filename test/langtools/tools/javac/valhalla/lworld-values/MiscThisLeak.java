/*
 * @test /nodynamiccopyright/
 * @bug 8205910
 * @summary Complain when `this' of a value class is leaked from constructor before all instance fields are definitely assigned.
 * @compile/fail/ref=MiscThisLeak.out -XDrawDiagnostics -XDdev MiscThisLeak.java
 */

public class MiscThisLeak {
    interface I {
        void foo();
    }
    __ByValue class V {
        class K {}
        int f;
        V() {
            I i = this::foo; // !OK.
            i = MiscThisLeak.this::foo; // OK.
            new K(); // !OK.
            this.new K(); // !OK.
            f = 10;
            i = this::foo;   // OK.
        }
        void foo() {
        }
    }
    void foo() {
    }
}
