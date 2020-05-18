/*
 * @test /nodynamiccopyright/
 * @bug 8237955
 * @summary Identity types that have no declaration sites fail to be IdentityObjects
 * @compile -XDrawDiagnostics UndeclaredIdentityObjectsTest.java
 */

public class UndeclaredIdentityObjectsTest {
    static class G<T> {}
    public static void main(String [] args) {
        Object [] oa = new UndeclaredIdentityObjectsTest[] {
                                new UndeclaredIdentityObjectsTest()
                       };
        if (!(oa instanceof IdentityObject))
            throw new AssertionError("Arrays are broken");
        Object o = new G<String>();
        if (!(o instanceof IdentityObject))
            throw new AssertionError("Parameterized type are broken");
        if (!(oa[0] instanceof IdentityObject)) // can only be determined at runtime
            System.out.println("Arrays are broken!");
    }
}
