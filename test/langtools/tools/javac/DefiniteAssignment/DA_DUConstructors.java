/*
 * @test /nodynamiccopyright/
 * @bug 8324873
 * @summary [lworld] implementation of value classes construction
 * @enablePreview
 * @compile/fail/ref=DA_DUConstructors.out -XDrawDiagnostics DA_DUConstructors.java
 */

public class DA_DUConstructors {
    // identity
    class C1 {
        final int x;
        final int y = x + 1;
        C1() {
            x = 12;
            super();
        }
    }

    class C2 {
        final int x;
        C2() {
            this(x = 3); // error
        }
        C2(int i) {
            x = 4;
        }
    }

    class C3 {
        C3(int i) {}
    }
    class C4 extends C3 {
        final int x;
        C4() {
            super(x = 3); // ok
        }
    }

    class C5 {
        final int x;
        final int y = x + 1; // x is not DA
        C5() {
            x = 12; super();
        }
        C5(int i) {
            /* no prologue */
            x = i;
        }
    }

    // value classes
    value class V1 {
        int x;
        int y = x + 1; // allowed
        V1() {
            x = 12;
            // super();
        }
    }

    value class V2 {
        int x;
        V2() { this(x = 3); } // error
        V2(int i) { x = 4; }
    }

    abstract value class AV1 {
        AV1(int i) {}
    }

    value class V3 extends AV1 {
        int x;
        V3() {
            super(x = 3); // ok
        }
    }

    value class V4 { // OK
        int x;
        int y = x + 1;

        V4() {
            x = 12;
        }

        V4(int i) {
            x = i;
        }
    }
}
