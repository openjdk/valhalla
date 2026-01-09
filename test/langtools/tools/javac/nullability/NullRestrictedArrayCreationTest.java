/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for null-restricted array creation type-checking
 * @enablePreview
 * @compile/fail/ref=NullRestrictedArrayCreationTest.out -XDrawDiagnostics NullRestrictedArrayCreationTest.java
 */

public class NullRestrictedArrayCreationTest {
    void testSimple(int n) {
        var t1 = new String![10];             // Fail (no init, n > 0)
        var t2 = new String![0];              // Pass (no init, n == 0)
        var t3 = new String![] { "x", "y" };  // Pass (init)
        var t4 = new String![n];              // Fail (no init, n not constant)
    }

    void testMulti(int n) {
        var t1 = new String![3][2];           // Fail (no init, innermost length > 0)
        var t2 = new String![3][0];           // Pass (no init, innermost length == 0)
        var t3 = new String![0][10];          // Pass (no init, outer length == 0, no array created)
        var t4 = new String![][] { {"x"} };   // Pass (init)
        var t5 = new String![10][];           // Pass (no init, no innermost length)
        var t6 = new String![2][][];          // Pass (no init, no innermost length)
        var t7 = new String![3][n];           // Fail (no init, innermost length not constant)
    }
}
