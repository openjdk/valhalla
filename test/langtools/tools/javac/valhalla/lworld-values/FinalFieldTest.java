/*
 * @test /nodynamiccopyright/
 * @summary Test that final fields of value classes follow the same assignment rules as vanilla classes.
 * @compile/fail/ref=FinalFieldTest.out --should-stop=at=FLOW -XDrawDiagnostics  FinalFieldTest.java
 */

final inline class Blah {
    final int x = 10;
    final int y;
    Blah() {
        x = 10;
        x = 10;
        y = 10;
        y = 10;
    }
    void foo() {
        x = 10;
        x = 10;
        y = 10;
        y = 10;
    }
}
