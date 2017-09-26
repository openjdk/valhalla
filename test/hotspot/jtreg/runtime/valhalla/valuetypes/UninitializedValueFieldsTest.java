package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Uninitialized value fields test
 * @modules jdk.incubator.mvt
 * @library /test/lib
 * @compile -XDenableValueTypes Point.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.UninitializedValueFieldsTest
 * @run main/othervm -Xcomp -XX:+EnableValhalla runtime.valhalla.valuetypes.UninitializedValueFieldsTest
 */
public class UninitializedValueFieldsTest {

    static Point staticPoint;
    Point instancePoint;

    UninitializedValueFieldsTest() { }

    public static void main(String[] args) {
        checkUninitializedPoint(UninitializedValueFieldsTest.staticPoint, 0, 0);
        UninitializedValueFieldsTest.staticPoint = Point.createPoint(456, 678);
        checkUninitializedPoint(UninitializedValueFieldsTest.staticPoint, 456, 678);
        UninitializedValueFieldsTest test = new UninitializedValueFieldsTest();
        checkUninitializedPoint(test.instancePoint, 0, 0);
        test.instancePoint = Point.createPoint(123, 345);
        checkUninitializedPoint(test.instancePoint, 123, 345);
    }

    static void checkUninitializedPoint(Point p, int x, int y) {
        Asserts.assertEquals(p.x, x, "invalid x value");
        Asserts.assertEquals(p.y, y, "invalid y value");
    }
}
