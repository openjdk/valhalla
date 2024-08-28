/*
 * @test /nodynamiccopyright/
 * @enablePreview
 * @summary Smoke test for nullable types in array types and array creation expressions
 * @compile/fail/ref=TestArrays.out -Xlint:null -Werror -XDrawDiagnostics TestArrays.java
 */

public class TestArrays {
    void test1() {
        String! []?[]? arr_local = null;
        arr_local = new String! [3]? [4]?;
        arr_local = new String! [3]! [4]?;
        arr_local = new String! [3]? [4]!;
        arr_local = new String! [3]! [4]!;
    }

    void test2() {
        String! []?[]! arr_local = null;
        arr_local = new String! [3]? [4]?; // warn
        arr_local = new String! [3]! [4]?; // warn
        arr_local = new String! [3]? [4]!;
        arr_local = new String! [3]! [4]!;
    }

    void test3() {
        String! []![]? arr_local = new String! [0]![]?;
        arr_local = new String! [3]? [4]?; // warn
        arr_local = new String! [3]! [4]?;
        arr_local = new String! [3]? [4]!; // warn
        arr_local = new String! [3]! [4]!;
    }

    void test4() {
        String! []![]! arr_local = new String! [0]![]!;
        arr_local = new String! [3]? [4]?; // warn
        arr_local = new String! [3]! [4]?; // warn
        arr_local = new String! [3]? [4]!; // warn
        arr_local = new String! [3]! [4]!;
    }

    void test5() {
        String! []?[]? arr_local = null;
        arr_local = new String! [3]? []?;
        arr_local = new String! [3]! []?;
        arr_local = new String! [3]? []!;
        arr_local = new String! [3]! []!;
    }

    void test6() {
        String! []?[]! arr_local = null;
        arr_local = new String! [3]? []?; // warn
        arr_local = new String! [3]! []?; // warn
        arr_local = new String! [3]? []!;
        arr_local = new String! [3]! []!;
    }

    void test7() {
        String! []![]? arr_local = new String! [0]![]?;
        arr_local = new String! [3]? []?; // warn
        arr_local = new String! [3]! []?;
        arr_local = new String! [3]? []!; // warn
        arr_local = new String! [3]! []!;
    }

    void test8() {
        String! []![]! arr_local = new String! [0]![]!;
        arr_local = new String! [3]? []?; // warn
        arr_local = new String! [3]! []?; // warn
        arr_local = new String! [3]? []!; // warn
        arr_local = new String! [3]! []!;
    }
}
