package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

/*
 * @test ValueTypeGetField
 * @summary Value Type get field test
 * @library /test/lib
 * @compile -XDenableValueTypes Point.java ValueTypeGetField.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeGetField
 * @run main/othervm -Xcomp -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeGetField
 */
public class ValueTypeGetField {

    static Point staticPoint0;
    static Point staticPoint1;
    Point instancePoint0;
    Point instancePoint1;

    static {
        staticPoint0 = Point.createPoint(358, 406);
        staticPoint1 = Point.createPoint(101, 2653);
    }

    ValueTypeGetField() {
        instancePoint0 = Point.createPoint(1890, 1918);
        instancePoint1 = Point.createPoint(91, 102);
    }

    public static void main(String[] args) {
        ValueTypeGetField valueTypeGetField = new ValueTypeGetField();
        System.gc(); // check that VTs survive GC
        valueTypeGetField.run();
    }

    public void run() {
        // testing initial configuration
        checkPoint(staticPoint0, 358, 406);
        checkPoint(staticPoint1, 101, 2653);
        checkPoint(instancePoint0, 1890, 1918);
        checkPoint(instancePoint1, 91, 102);
        // swapping static fields
        Point p = staticPoint1;
        staticPoint1 = staticPoint0;
        staticPoint0 = p;
        System.gc();
        checkPoint(staticPoint0, 101, 2653);
        checkPoint(staticPoint1, 358, 406);
        //swapping instance fields
        p = instancePoint1;
        instancePoint1 = instancePoint0;
        instancePoint0 = p;
        System.gc();
        checkPoint(instancePoint0, 91, 102);
        checkPoint(instancePoint1, 1890, 1918);
        // instance to static
        staticPoint0 = instancePoint0;
        System.gc();
        checkPoint(staticPoint0, 91, 102);
        // static to instance
        instancePoint1 = staticPoint1;
        System.gc();
        checkPoint(instancePoint1, 358, 406);
    }

    static void checkPoint(Point p , int x, int y) {
        Asserts.assertEquals(p.x, x, "invalid x value");
        Asserts.assertEquals(p.y, y, "invalid y value");
    }
}
