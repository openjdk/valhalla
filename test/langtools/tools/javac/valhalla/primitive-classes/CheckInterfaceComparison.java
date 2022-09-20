/*
 * @test /nodynamiccopyright/
 * @bug 8237074
 * @summary Result of .getClass() should never be compared to an interface class literal
 *
 * @compile/ref=CheckInterfaceComparison.out -XDrawDiagnostics -Xlint:migration CheckInterfaceComparison.java
 */
public class CheckInterfaceComparison {
    public boolean bogusCompareLeft(Object o) { // Should be warned against
        return (o.getClass()) == Runnable.class;
    }

    public boolean bogusCompareNELeft(Object o) { // Should be warned against
        return (o.getClass()) != Runnable.class;
    }

    public boolean bogusCompareRight(Object o) { // Should be warned against
        return Iterable.class == o.getClass();
    }

    public boolean bogusCompareNERight(Object o) { // Should be warned against
        return Iterable.class != o.getClass();
    }

    public boolean goodCompareLeft(Object o) { // Is fine, no warning required
        return o.getClass() == Integer.class;
    }

    public boolean goodCompareNELeft(Object o) { // Is fine, no warning required
        return o.getClass() != Integer.class;
    }

    public boolean goodCompareRight(Object o) { // Is fine, no warning required
        return Long.class == o.getClass();
    }

    public boolean goodCompareNERight(Object o) { // Is fine, no warning required
        return Long.class != o.getClass();
    }

    public boolean rawCompareLeft(Object o, Class<?> clazz) { // Is fine, no warning required
        return o.getClass() == clazz;
    }

    public boolean rawCompareNELeft(Object o, Class<?> clazz) { // Is fine, no warning required
        return o.getClass() != clazz;
    }

    public boolean rawCompareRight(Object o, Class<?> clazz) { // Is fine, no warning required
        return clazz == o.getClass();
    }

    public boolean rawCompareNERight(Object o, Class<?> clazz) { // Is fine, no warning required
        return clazz != o.getClass();
    }

    static Class<?> getClass(int x) {
        return null;
    }

    public static boolean compare(Object o, Class<?> clazz) {
        return getClass(0) == Runnable.class; // No warning required for static getClass
    }

    public Class<?> getClass(String x) {
        return null;
    }

    public boolean compare(Object o, String arg, Class<?> clazz) {
        return getClass(arg) == Runnable.class; // No warning required for non-object.getClass()
    }
}
