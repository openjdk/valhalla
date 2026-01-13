/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for diagnostics for uninitialized non-nullable fields
 * @enablePreview
 * @compile/fail/ref=NonNullFieldInitTest.out -XDrawDiagnostics NonNullFieldInitTest.java
 */

class NonNullFieldInitTest {
    static String! s;

    static class WithInit {
        String! s;

        WithInit() { }
    }

    static class WithoutInit {
        String! s;
    }
}
