/*
 * @test /nodynamiccopyright/
 * @bug 8210825
 * @summary [lworld] Javac should tolerate legacy idioms for equality (LIFE) when applied to values.
 * @compile/fail/ref=CheckEqualityIdiom.out -XDrawDiagnostics -XDdev CheckEqualityIdiom.java
 */

final __ByValue class CheckEqualityIdiom {

    int x = 42;

    static CheckEqualityIdiom v1;
    static CheckEqualityIdiom v2;

    static boolean b = v1 == v2 || v1.equals(v2); // No error
    static boolean b2 = v1 == null; // Error.

    private CheckEqualityIdiom() {}

    void foo(CheckEqualityIdiom vbci1, CheckEqualityIdiom vbci2) {

        // The snippets below should not trigger a warning
        if (this == vbci1 || equals(vbci1)) {}
        if (this == vbci1 || this.equals(vbci1)) {}
        if (this == vbci1 || vbci1.equals(this)) {}
        if (vbci1 == this || vbci1.equals(this)) {}
        if (vbci1 == this || equals(vbci1)) {}
        if (vbci1 == this || this.equals(vbci1)) {}
        if (vbci1 == vbci2 || vbci1.equals(vbci2)) {}
        if (vbci1 == vbci2 || vbci2.equals(vbci1)) {}
        Object o = null;
        if (vbci1 == o || vbci1.equals(o)) {
        }

        // The snippet below is not an equality idiom.
        if (this == vbci1 || vbci1.equals(vbci2)) {
        }
    }
}
