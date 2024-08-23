/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for nullability warnings
 * @enablePreview
 * @compile/fail/ref=NullabilityWarningsTest.out -XDrawDiagnostics -Werror -Xlint:null NullabilityWarningsTest.java
 * @compile/fail/ref=NullabilityWarningsTest_parametric.out -XDrawDiagnostics -Werror -Xlint:null -XDtvarUnspecifiedNullity NullabilityWarningsTest.java
 */

public class NullabilityWarningsTest {
    static class Box<T> {
        T t;
        Box(T t) {
            this.t = t;
        }
        T get() { return t; }
        void set(T t) { this.t = t; }
    }

    void test() {
        String? s_null = null;
        String! s_nonnull = "";
        String s_unknown = null;
        s_null = s_nonnull;
        s_nonnull = s_null; //warn
        s_unknown = s_null;
        s_unknown = s_nonnull;

        Box<String?> bs_null = null;
        Box<String!> bs_nonnull = null;
        Box<String> bs_unknown = null;
        bs_null = bs_nonnull; //warn
        bs_nonnull = bs_null; //warn
        bs_unknown = bs_null;
        bs_unknown = bs_nonnull;

        Box<? super String?> bss_null = null;
        Box<? super String!> bss_nonnull = null;
        Box<? super String!> bss_unknown = null;

        bss_nonnull = bss_null;
        bss_null = bss_nonnull; //warn
        bss_unknown = bss_null;
        bss_unknown = bss_nonnull;

        Box<? extends String?> bes_null = null;
        Box<? extends String!> bes_nonnull = null;
        Box<? extends String> bes_unknown = null;

        bes_nonnull = bes_null; //warn
        bes_null = bes_nonnull;
        bes_unknown = bes_null;
        bes_unknown = bes_nonnull;
    }

    void testMember() {
        Box<String?> bs_null = null;
        Box<String!> bs_nonnull = null;
        Box<String> bs_unknown = null;

        String s_unknown = bs_null.get();
        s_unknown = bs_nonnull.get();
        s_unknown = bs_unknown.get();

        String? s_null = bs_null.get();
        s_null = bs_nonnull.get();
        s_null = bs_unknown.get();

        String! s_nonnull = bs_null.get(); //warn
        s_nonnull = bs_nonnull.get();
        s_nonnull = bs_unknown.get();
    }
}
