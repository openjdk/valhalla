/*
 * @test /nodynamiccopyright/
 * @enablePreview
 * @summary Smoke test for signature attribute parsing
 * @compile pkg/Foo.java
 * @compile/fail/ref=SeparateCompilationTest.out -Werror -Xlint:null -XDrawDiagnostics SeparateCompilationTest.java
 */

import pkg.Foo;

public class SeparateCompilationTest {
    void test(Foo<String> foo) {
        foo.s = null;
        foo.s_arr = null;
        foo.x = null;
        foo.x_arr = null;
        foo.list_x = null;
    }
}
