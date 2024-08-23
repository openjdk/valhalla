/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for nullability inference
 * @enablePreview
 * @compile/fail/ref=NullabilityInferenceTest.out -XDrawDiagnostics NullabilityInferenceTest.java
 */

import java.util.List;

class Test {
    static class Box<T> { }

    static <T> List<Box<T>> makeInvariant(Box<T>... boxes) { return null; }
    static <T> List<Box<T>> makeCovariant(Box<? extends T>... boxes) { return null; }
    static <T> List<Box<T>> makeContravariant(Box<? super T>... boxes) { return null; }

    void testInvariant() {
        Box<String?> bs_null = null;
        Box<String!> bs_nonnull = null;

        Integer i = null;
        i = (Integer)makeInvariant(bs_null, bs_nonnull); // List<Box<String?>>
        i = (Integer)makeInvariant(bs_nonnull, bs_null); // List<Box<String?>>
        i = (Integer)makeInvariant(bs_null, bs_null); // List<Box<String?>>
        i = (Integer)makeInvariant(bs_nonnull, bs_nonnull); // List<Box<String!>>
    }

    void testCovariant() {
        Box<String?> bs_null = null;
        Box<String!> bs_nonnull = null;

        Integer i = null;
        i = (Integer)makeCovariant(bs_null, bs_nonnull); // List<Box<String?>>
        i = (Integer)makeCovariant(bs_nonnull, bs_null); // List<Box<String?>>
        i = (Integer)makeCovariant(bs_null, bs_null); // List<Box<String?>>
        i = (Integer)makeCovariant(bs_nonnull, bs_nonnull); // List<Box<String!>>
    }

    void testContravariant() {
        Box<String?> bs_null = null;
        Box<String!> bs_nonnull = null;

        Integer i = null;
        i = (Integer)makeContravariant(bs_null, bs_nonnull); // List<Box<String!>>
        i = (Integer)makeContravariant(bs_nonnull, bs_null); // List<Box<String!>>
        i = (Integer)makeContravariant(bs_null, bs_null); // List<Box<String?>>
        i = (Integer)makeContravariant(bs_nonnull, bs_nonnull); // List<Box<String!>>
    }
}
