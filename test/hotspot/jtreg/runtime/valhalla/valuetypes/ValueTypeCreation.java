package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

/*
 * @test ValueTypeCreation
 * @summary Value Type creation test
 * @library /test/lib
 * @compile  -XDenableValueTypes ValueTypeCreation.java Point.java Long8Value.java Person.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeCreation
 * @run main/othervm -Xcomp -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeCreation
 */
public class ValueTypeCreation {
    public static void main(String[] args) {
        ValueTypeCreation valueTypeCreation = new ValueTypeCreation();
        valueTypeCreation.run();
    }

    public void run() {
        testPoint();
        testLong8();
        // Embedded oops not yet supported
        //testPerson();
    }

    void testPoint() {
        Point p = Point.createPoint(1, 2);
        Asserts.assertEquals(p.x, 1, "invalid point x value");
        Asserts.assertEquals(p.y, 2, "invalid point y value");
        Point p2 = clonePoint(p);
        Asserts.assertEquals(p2.x, 1, "invalid point clone x value");
        Asserts.assertEquals(p2.y, 2, "invalid point clone y value");
    }

    static Point clonePoint(Point p) {
        Point q = p;
        return q;
    }

    void testLong8() {
        Long8Value long8Value = Long8Value.create(1, 2, 3, 4, 5, 6, 7, 8);
        Asserts.assertEquals(long8Value.getLongField1(), 1L, "Field 1 incorrect");
        Asserts.assertEquals(long8Value.getLongField8(), 8L, "Field 8 incorrect");
        Long8Value.check(long8Value, 1, 2, 3, 4, 5, 6, 7, 8);
    }

    void testPerson() {
        Person person = Person.create(1, "John", "Smith");
        Asserts.assertEquals(person.getId(), 1L, "Id field incorrect");
        Asserts.assertEquals(person.getFirstName(), "John", "First name incorrect");
        Asserts.assertEquals(person.getLastName(), "Smith", "Last name incorrect");
    }
}
