/**
 * @test TestFieldNullability
 * @library /test/lib
 * @compile -XDenableValueTypes -XDallowWithFieldOperator -XDallowFlattenabilityModifiers TestFieldNullability.java
 * @run main/othervm -Xint -Xmx128m -XX:+EnableValhalla -XX:-ShowMessageBoxOnError -XX:ValueFieldMaxFlatSize=32
 *                   runtime.valhalla.valuetypes.TestFieldNullability
 */

package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

public class TestFieldNullability {
    static value class MyValue {
	int x;

	public MyValue() {
	    x = 314;
	}
    }

    static value class MyBigValue {
	long l0, l1, l2, l3, l4, l5, l6, l7, l8, l9;
	long l10, l11, l12, l13, l14, l15, l16, l17, l18, l19;

	public MyBigValue() {
	    l0 = l1 = l2 = l3 = l4 = l5 = l6 = l7 = l8 = l9 = 271;
	    l10 = l11 = l12 = l13 = l14 = l15 = l16 = l17 = l18 = l19 = 271;
	}
    }

    static value class TestValue {
	final __NotFlattened MyValue nullableField;
	final __Flattenable  MyValue nullfreeField;       // flattened
	final __NotFlattened MyValue nullField;           // src of null
	final __Flattenable  MyBigValue nullfreeBigField; // not flattened
	final __NotFlattened MyBigValue nullBigField;     // src of null

	public void test() {
	    Asserts.assertNull(nullField, "Invalid non null value for for unitialized non flattenable field");
	    Asserts.assertNull(nullBigField, "Invalid non null value for for unitialized non flattenable field");
	    boolean NPE = false;
	    try {
		TestValue tv = __WithField(this.nullableField, nullField);
	    } catch(NullPointerException e) {
		NPE = true;
	    }
	    Asserts.assertFalse(NPE, "Invalid NPE when assigning null to a non flattenable field");
	    try {
		TestValue tv = __WithField(this.nullfreeField, nullField);
	    } catch(NullPointerException e) {
		NPE = true;
	    }
	    Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattened field");
	    try {
		TestValue tv = __WithField(this.nullfreeBigField, nullBigField);
	    } catch(NullPointerException e) {
		NPE = true;
	    }
	    Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattenable field");
	}

	public TestValue() {
	    nullableField = MyValue.default;
	    nullfreeField = MyValue.default;
	    nullField = MyValue.default;         // fake assignment
	    nullfreeBigField = MyBigValue.default;
	    nullBigField = MyBigValue.default;      // fake assignment
	    
	}
    }

    static class TestClass {
	__NotFlattened MyValue nullableField;
	__Flattenable  MyValue nullfreeField;       // flattened
	__NotFlattened MyValue nullField;
	__Flattenable  MyBigValue nullfreeBigField; // not flattened
	__NotFlattened MyBigValue nullBigField;

	public void test() {
	    Asserts.assertNull(nullField, "Invalid non null value for for unitialized non flattenable field");
	    Asserts.assertNull(nullBigField, "Invalid non null value for for unitialized non flattenable field");
	    boolean NPE = false;
	    try {
		nullableField = nullField;
	    } catch(NullPointerException e) {
		NPE = true;
	    }
	    Asserts.assertFalse(NPE, "Invalid NPE when assigning null to a non flattenable field");
	    try {
		this.nullfreeField = nullField;
	    } catch(NullPointerException e) {
		NPE = true;
	    }	    
	    Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattened field");
	    try {
		this.nullfreeBigField = nullBigField;
	    } catch(NullPointerException e) {
		NPE = true;
	    }	    
	    Asserts.assertTrue(NPE, "Missing NPE when assigning null to a flattenable field");
	}
    }

    public static void main(String[] args) {
	TestClass tc = new TestClass();
	tc.test();
	TestValue tv =
	    TestValue.default;
	tv.test();
    }
    
}
