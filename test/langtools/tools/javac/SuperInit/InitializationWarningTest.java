/*
 * @test /nodynamiccopyright/
 * @bug 8367698
 * @summary New lint category `initialization` for code that would not be allowed in the prologue
 * @compile/fail/ref=InitializationWarningTest.out -XDrawDiagnostics -Xlint:initialization -Werror InitializationWarningTest.java
 * @enablePreview
 */

class InitializationWarningTest implements Iterable<Object> {
    InitializationWarningTest self = this;
    Object o = null;

    InitializationWarningTest(Object oo, InitializationWarningTest other) {
        this.o = oo;                                  // warning, field has initializer
        m();                                          // warning, instance method
        InitializationWarningTest.this.m();           // warning, instance method
        InitializationWarningTest.super.hashCode();   // warning
        other.m();                                    // good
        sm();                                         // good too
        System.identityHashCode(this);                // warning
        Iterable.super.spliterator();                 // warning
        new Inner();
    }

    class Inner {}

    void m() {}
    static void sm() {}
}
