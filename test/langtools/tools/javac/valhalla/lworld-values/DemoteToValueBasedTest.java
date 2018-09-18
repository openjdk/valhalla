/*
 * @test /nodynamiccopyright/
 * @summary Test that value classes get demoted to value based classes in -source 10 compiles.
 * @compile -XDallowWithFieldOperator Point.java
 * @compile/fail/ref=DemoteToValueBasedTest.out -XDrawDiagnostics -XDdev DemoteToValueBasedTest.java
 * @compile/fail/ref=DemoteToValueBasedTest10.out --should-stop=at=FLOW -Werror -XDallowValueBasedClasses -XDrawDiagnostics -source 10 -XDdev DemoteToValueBasedTest.java
 * @compile/fail/ref=DemoteToValueBasedTest3.out --should-stop=at=FLOW -Werror -XDrawDiagnostics -source 10 -XDdev DemoteToValueBasedTest.java
 */

public class DemoteToValueBasedTest {
    Point p = null;
}
