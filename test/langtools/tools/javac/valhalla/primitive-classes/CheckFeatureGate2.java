/*
 * @test /nodynamiccopyright/
 * @bug 8237067
 * @summary Check that .default is not allowed in previous versions.
 * @compile/fail/ref=CheckFeatureGate2.out --release=13 -XDrawDiagnostics CheckFeatureGate2.java
 */

public class CheckFeatureGate2 {

    static <T> void checkDefaultT(Class<T> clazz) throws Exception {
        while (T.default != null)
            throw new AssertionError("Generic object should default to null");
    }

    public static void main(String[] args) throws Exception {
        int a = int.default;
        String s = String.default;
        int[] ia = int[].default;
        checkDefaultT(Object.class);
    }
}
