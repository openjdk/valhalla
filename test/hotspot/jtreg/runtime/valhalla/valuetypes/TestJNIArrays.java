/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


import jdk.test.lib.Asserts;

import java.lang.reflect.*;
import java.util.Random;
import java.util.Arrays;
import java.util.Comparator;

import jdk.internal.misc.Unsafe;

/*
 * @test
 * @summary Test flattened arrays accesses through JNI
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary /test/lib
 * @requires (os.simpleArch == "x64")
 * @requires (os.family == "linux" | os.family == "mac")
 * @compile -XDallowGenericsOverValues -XDallowWithFieldOperator TestJNIArrays.java
 * @run main/othervm/native/timeout=3000 -XX:ValueArrayElemMaxFlatSize=128 TestJNIArrays
 */

public class TestJNIArrays {

    static final Unsafe U = Unsafe.getUnsafe();
    
    public static final int ARRAY_SIZE = 1024;
    static long seed;
    static Random random;

    static {
	seed = System.nanoTime();
	System.out.println("Seed = " + seed); 
	random = new Random(seed);
    }

    static {
        System.loadLibrary("TestJNIArrays");
    }

    static inline class IntInt {
	int i0;
	int i1;

	public IntInt(int i0, int i1) {
	    this.i0 = i0;
	    this.i1 = i1;
	}
    }

    static class IntIntComparator implements Comparator<IntInt> {
	public int compare(IntInt a, IntInt b) {
	    if (a.i0 < b.i0) return -1;
	    if (a.i0 > b.i0) return 1;
	    if (a.i1 < b.i1) return -1;
	    if (a.i1 > b.i1) return 1;
	    return 0;
	}
    }

    static inline class Containee {
	float f;
	short s;

	Containee(float f, short s) {
	    this.f = f;
	    this.s = s;
	}
    }
    
    static inline class Container {
	double d;
	Containee c;
	byte b;

	Container(double d, Containee c, byte b) {
	    this.d = d ;
	    this.c = c;
	    this.b = b;
	}

	Container setc(Containee c) {
            Container res = __WithField(this.c, c);
            return res;
        }

    }

    static inline class LongLongLongLong {
	long l0, l1, l2, l3;

	public LongLongLongLong(long l0, long l1, long l2, long l3) {
	    this.l0 = l0;
	    this.l1 = l1;
	    this.l2 = l2;
	    this.l3 = l3;
	}
    }

    static inline class BigValue {
	long l0 = 0, l1 = 0,   l2 = 0, l3 = 0, l4 = 0, l5 = 0, l6 = 0, l7 = 0, l8 = 0, l9 = 0;
	long l10 = 0, l11 = 0, l12 = 0, l13 = 0, l14 = 0, l15 = 0, l16 = 0, l17 = 0, l18 = 0, l19 = 0;
	long l20 = 0, l21 = 0, l22 = 0, l23 = 0, l24 = 0, l25 = 0, l26 = 0, l27 = 0, l28 = 0, l29 = 0;
	long l30 = 0, l31 = 0, l32 = 0, l33 = 0, l34 = 0, l35 = 0, l36 = 0, l37 = 0, l38 = 0, l39 = 0;
    }

    static inline class ValueWithOops {
	String o = null;
	int i = 0;
    }
    
    public static void main(String[] args) {
	TestJNIArrays test = new TestJNIArrays();
	test.checkGetFlattenedArrayElementSize();
	test.checkGetFlattenedArrayElementClass();
	test.checkGetFieldOffsetInFlattenedLayout();
	test.checkGetFlattenedArrayElements();
	test.checkBehaviors();
	// test.mesureInitializationTime(1024 * 1024 * 10 , 1000);
	// test.mesureInitializationTime2(1024 * 1024 * 10 , 1000);
	// test.mesureUpdateTime2(1024 * 1024 * 10, 1000);
	// test.mesureSortingTime(1024 * 1024, 100); // reduced number of iterations because Java sorting is slow (because of generics?)
	test.mesureInitializationTime3(1024 * 1024 * 2 , 1000);
    }

    void checkGetFlattenedArrayElementSize() {
	Throwable exception = null;
	try {
	    Object o = new Object();
	    GetFlattenedArrayElementSizeWrapper(o);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    int[] array = new int[16];
	    Object o = array;
	    GetFlattenedArrayElementSizeWrapper(o);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    GetFlattenedArrayElementSizeWrapper(new String[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    // Array of BigValue should not be flattened because BigValue is *big*
	    GetFlattenedArrayElementSizeWrapper(new BigValue[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertTrue(exception instanceof IllegalArgumentException , "Exception should be a IAE");
	exception = null;
	int size = -1;
	try {
	    size = GetFlattenedArrayElementSizeWrapper(new IntInt[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNull(exception, "No exception should have been thrown");
	Asserts.assertEquals(size, 8, "Wrong size");
    }

    void checkGetFlattenedArrayElementClass() {
	Throwable exception = null;
	try {
	    Object o = new Object();
	    GetFlattenedArrayElementClassWrapper(o);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    int[] array = new int[16];
	    Object o = array;
	    GetFlattenedArrayElementClassWrapper(o);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    GetFlattenedArrayElementClassWrapper(new String[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    // Array of BigValue should not be flattened because BigValue is *big*
	    GetFlattenedArrayElementClassWrapper(new BigValue[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertTrue(exception instanceof IllegalArgumentException , "Exception should be a IAE");
	exception = null;
	Class c = null;
	try {
	    c = GetFlattenedArrayElementClassWrapper(new IntInt[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNull(exception, "No exception should have been thrown");
	Asserts.assertEquals(c, IntInt.class, "Wrong class");
    }

    void checkGetFieldOffsetInFlattenedLayout() {
	Throwable exception = null;
	try {
	    Object o = new Object();
	    GetFieldOffsetInFlattenedLayoutWrapper(o.getClass(), "foo", "I", true);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    int[] array = new int[16];
	    GetFieldOffsetInFlattenedLayoutWrapper(array.getClass(), "foo", "I", true);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    String[] array = new String[16];
	    GetFieldOffsetInFlattenedLayoutWrapper(array.getClass(), "foo", "I", true);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	Containee ce  = new Containee(3.4f, (short)5);
	Container c = new Container(123.876, ce, (byte)7);
	Class<?> cclass = c.getClass();
	Container[] containerArray = new Container[32];
	int elementSize = GetFlattenedArrayElementSizeWrapper(containerArray);
	int offset = -1;
	try {
	    offset = GetFieldOffsetInFlattenedLayoutWrapper(cclass, "d", "D", false);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNull(exception, "No exception should have been thrown");
	Field f = null;
	try {
	    f = cclass.getDeclaredField("d");
	} catch(NoSuchFieldException e) {
	    e.printStackTrace();
	    return;
	}
	Asserts.assertEquals(U.valueHeaderSize(cclass) + offset, U.objectFieldOffset(cclass, f.getName()),
			     "Incorrect offset");
	Asserts.assertLessThan(offset, elementSize, "Invalid offset");
	exception = null;
	try {
	    // Field c should be flattened, so last argument is true, no exception expected
	    GetFieldOffsetInFlattenedLayoutWrapper(cclass, "c", "QTestJNIArrays$Containee;", true);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNull(exception, "No exception should have been thrown");
	Asserts.assertLessThan(offset, elementSize, "Invalid offset");
	exception = null;
	try {
	    // Field c should be flattened, with last argument being false, exception expected from the wrapper 
	    GetFieldOffsetInFlattenedLayoutWrapper(cclass, "c", "QTestJNIArrays$Containee;", false);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "Wrapper should have thrown a RuntimeException");
	Asserts.assertEquals(exception.getClass(), RuntimeException.class , "Wrong exception type");
    }
    
     void checkGetFlattenedArrayElements() {
	Throwable exception = null;
	Object o = new Object();
	try {
	    GetFlattenedArrayElementsWrapper(o);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	int[] a1 = new int[16];
	o = a1;
	try {
	    GetFlattenedArrayElementsWrapper(o);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    GetFlattenedArrayElementsWrapper(new String[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertEquals(exception.getClass(), IllegalArgumentException.class , "Wrong exception type");
	exception = null;
	try {
	    // Array of BigValue should not be flattened because BigValue is *big*
	    GetFlattenedArrayElementsWrapper(new BigValue[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertTrue(exception instanceof IllegalArgumentException , "Exception should be a IAE");
	exception = null;
	try {
	    // Direct native access to flattened arrays is not allowed if elements contain oops
	    GetFlattenedArrayElementsWrapper(new ValueWithOops[16]);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "An IAE should have been thrown");
	Asserts.assertTrue(exception instanceof IllegalArgumentException , "Exception should be a IAE");
	exception = null;
	long addr = 0;
	IntInt[] a2 = new IntInt[16];
	try {
	    addr = GetFlattenedArrayElementsWrapper(a2);
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNull(exception, "No exception should have been thrown");
	if (exception == null) {
	    ReleaseFlattenedArrayElementsWrapper(a2, addr, 0);
	}
    }
    
    void checkBehaviors() {
	System.out.println("Check 1");
	IntInt[] array = new IntInt[ARRAY_SIZE];
	int value = getIntFieldAtIndex(array, 1, "i0", "I");
	Asserts.assertEquals(value, 0, "Initial value must be zero");
	printArrayInformation(array);
	int i0_value = 42;
	int i1_value = -314;
	initializeIntIntArrayBuffer(array, i0_value, i1_value);
	System.gc();
	for (int i = 0; i < array.length; i++) {
	    Asserts.assertEquals(array[i].i0, i0_value, "Bad value of i0 at index " + i);
	    Asserts.assertEquals(array[i].i1, i1_value, "Bad value of i1 at index " + i);
	}
	System.out.println("Check 2");
	array = new IntInt[ARRAY_SIZE];
	i0_value = -194;
	i1_value = 91;
	initializeIntIntArrayFields(array, i0_value, i1_value);
	System.gc();
	for (int i = 0; i < array.length; i++) {
	    Asserts.assertEquals(array[i].i0, i0_value, "Bad value of i0 at index " + i);
	    Asserts.assertEquals(array[i].i1, i1_value, "Bad value of i1 at index " + i);
	}
	System.out.println("Check 3");
	array = new IntInt[ARRAY_SIZE];
	initializeIntIntArrayJava(array, i0_value, i1_value);
	System.gc();
	for (int i = 0; i < array.length; i++) {
	    Asserts.assertEquals(array[i].i0, i0_value, "Bad value of i0 at index " + i);
	    Asserts.assertEquals(array[i].i1, i1_value, "Bad value of i1 at index " + i);
	}
	System.out.println("Check 4");
	random = new Random(seed);
	array = new IntInt[ARRAY_SIZE];
	for (int i = 0; i < ARRAY_SIZE; i++) {
	    array[i] = new IntInt(random.nextInt(), random.nextInt());
	}
	sortIntIntArray(array);
	System.gc();
	for (int i = 0; i < array.length - 1; i++) {
	    Asserts.assertLessThanOrEqual(array[i].i0, array[i+1].i0, "Incorrect i0 fields ordering at index " + i);
	    if (array[i].i0 == array[i+1].i0) {
		Asserts.assertLessThanOrEqual(array[i].i1, array[i+1].i1, "Incorrect i1 fields ordering at index " + i);
	    }
	}
	System.out.println("Check 5");
	random = new Random(seed);
	array = new IntInt[ARRAY_SIZE];
	for (int i = 0; i < ARRAY_SIZE; i++) {
	    array[i] = new IntInt(random.nextInt(), random.nextInt());
	}
	Arrays.sort(array, new IntIntComparator());
	System.gc();
	for (int i = 0; i < array.length - 1; i++) {
	    Asserts.assertLessThanOrEqual(array[i].i0, array[i+1].i0, "Incorrect i0 fields ordering at index " + i);
	    if (array[i].i0 == array[i+1].i0) {
		Asserts.assertLessThanOrEqual(array[i].i1, array[i+1].i1, "Incorrect i1 fields ordering at index " + i);
	    }
	}
	System.out.println("Check 6");
	Container[] array2 = new Container[ARRAY_SIZE];
	double d  = 1.23456789;
	float f = -987.654321f;
	short s = -512;
	byte b = 127;
	Containee c = new Containee(f,s);
	Container c2 = new Container(d, c, b);
	initializeContainerArray(array2, d, f, s, b);
	System.gc();
	for (int i = 0; i < array2.length; i++) {
	    Asserts.assertEquals(array2[i], c2, "Incorrect value at index " + i);
	    Asserts.assertEquals(array2[i].d, d, "Incorrect d value at index " + i);
	    Asserts.assertEquals(array2[i].c.f, f, "Incorrect f value at index " + i);
	    Asserts.assertEquals(array2[i].c.s, s, "Incorrect s value at index " + i);
	    Asserts.assertEquals(array2[i].b, b, "Incorrect b value at inde " +i);
	}
	System.out.println("Check 7");
	f = 19.2837465f;
	s = 231;
	updateContainerArray(array2, f, s);
	System.gc();
	for (int i = 0; i < array2.length; i++) {
	    Asserts.assertEquals(array2[i].d, d, "Incorrect d value at index " + i);
	    Asserts.assertEquals(array2[i].c.f, f, "Incorrect f value at index " + i);
	    Asserts.assertEquals(array2[i].c.s, s, "Incorrect s value at index " + i);
	    Asserts.assertEquals(array2[i].b, b, "Incorrect b value at inde " +i);
	}
	System.out.println("Check 8");
	long l0 = 52467923;
	long l1= -7854022;
	long l2 = 230947485;
	long l3 = -752497024;
	LongLongLongLong[] array3 = new LongLongLongLong[ARRAY_SIZE/8];
	initializeLongLongLongLongArray(array3, l0, l1, l2, l3);
	System.gc();
	for (int i = 0; i < array3.length; i++) {
	    Asserts.assertEquals(array3[i].l0, l0, "Bad value of l0 at index " + i);
	    Asserts.assertEquals(array3[i].l1, l1, "Bad value of l1 at index " + i);
	    Asserts.assertEquals(array3[i].l2, l2, "Bad value of l2 at index " + i);
	    Asserts.assertEquals(array3[i].l3, l3, "Bad value of l3 at index " + i);
	}
    }

    void initializeIntIntArrayJava(IntInt[] array, int i0, int i1) {
	IntInt ii = new IntInt(i0, i1);
	for (int i = 0; i < array.length; i++) {
	    array[i] = ii;
	}
    }

    void initializeContainerArrayJava(Container[] array, double d, float f, short s, byte b) {
	Containee c = new Containee(f,s);
	Container c2 = new Container(d, c, b);
	for (int i = 0; i < array.length; i++) {
	    array[i] = c2;
	}
    }

    void updateContainerArrayJava(Container[] array, float f, short s) {
	Containee c = new Containee(f, s);
	for (int i = 0; i < array.length; i++) {
	    array[i] = array[i].setc(c);;
	}
    }

    void initializeLongLongLongLongArrayJava(LongLongLongLong[] array, long l0, long l1, long l2, long l3) {
	LongLongLongLong llll = new LongLongLongLong(l0, l1, l2, l3);
	for (int i = 0; i < array.length; i++) {
	    array[i] = llll;
	}
    }
    
    void mesureInitializationTime(int array_size, int iterations) {
	System.out.println("\nInitialization time for IntInt[]:");
	long[] start = new long[iterations];
	long[] end = new long[iterations];

	
	System.out.println("\nJava:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    IntInt[] array = new IntInt[array_size];
	    initializeIntIntArrayJava(array, 42, -314);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    IntInt[] array = new IntInt[array_size];
	    start[i] = System.nanoTime();
	    initializeIntIntArrayJava(array, 42, -314);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);

	System.out.println("\nNative(memcpy):");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    IntInt[] array = new IntInt[array_size];
	    initializeIntIntArrayBuffer(array, 42, -314);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    IntInt[] array = new IntInt[array_size];
	    start[i] = System.nanoTime();
	    initializeIntIntArrayBuffer(array, 42, -314);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);
	
	
	System.out.println("\nNative(fields):");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    IntInt[] array = new IntInt[array_size];
	    initializeIntIntArrayFields(array, 42, -314);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    IntInt[] array = new IntInt[array_size];
	    start[i] = System.nanoTime();
	    initializeIntIntArrayFields(array, 42, -314);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);
    }

    void mesureInitializationTime2(int array_size, int iterations) {
	System.out.println("\nInitialization time for Container[]:");
	long[] start = new long[iterations];
	long[] end = new long[iterations];

	double d = 0.369852147;
	float f = -321.654987f;
	short s = -3579;
	byte b = 42;
	
	System.out.println("\nJava:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    Container[] array = new Container[array_size];
	    initializeContainerArrayJava(array, d, f, s, b);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    Container[] array = new Container[array_size];
	    start[i] = System.nanoTime();
	    initializeContainerArrayJava(array, d, f, s, b);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);

	System.out.println("\nNative:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    Container[] array = new Container[array_size];
	    initializeContainerArray(array, d, f, s, b);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    Container[] array = new Container[array_size];
	    start[i] = System.nanoTime();
	    initializeContainerArray(array, d, f, s, b);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);
    }

    void mesureUpdateTime2(int array_size, int iterations) {
	System.out.println("\nUpdating Container[]:");
	long[] start = new long[iterations];
	long[] end = new long[iterations];

	double d = 0.369852147;
	float f = -321.654987f;
	short s = -3579;
	byte b = 42;

	Containee c = new Containee(f,s);
	Container c2 = new Container(d, c, b);
	
	System.out.println("\nJava:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    Container[] array = new Container[array_size];
	    for (int j = 0; j < array.length; j++) {
		array[j] = c2;
	    }
	    updateContainerArrayJava(array, f, s);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    Container[] array = new Container[array_size];
	    for (int j = 0; j < array.length; j++) {
		array[i] = c2;
	    }
	    start[i] = System.nanoTime();
	    updateContainerArrayJava(array, f, s);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);

	System.out.println("\nNative:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    Container[] array = new Container[array_size];
	    for (int j = 0; j < array.length; j++) {
		array[i] = c2;
	    }
	    updateContainerArray(array, f, s);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    Container[] array = new Container[array_size];
	    for (int j = 0; j < array.length; j++) {
		array[i] = c2;
	    }
	    start[i] = System.nanoTime();
	    updateContainerArray(array, f, s);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);
    }
    
    void mesureSortingTime(int array_size, int iterations) {
	System.out.println("\nSorting time:");
	long[] start = new long[iterations];
	long[] end = new long[iterations];

	random = new Random(seed);
	System.out.println("\nJava:");
	IntIntComparator comparator = new IntIntComparator();
	// Warmup
	for (int i = 0; i < 10; i++) {
	    IntInt[] array = new IntInt[array_size];
	    array = new IntInt[array_size];
	    for (int j = 0; j < array_size; j++) {
		array[j] = new IntInt(random.nextInt(), random.nextInt());
	    }
	    Arrays.sort(array, comparator);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    IntInt[] array = new IntInt[array_size];
	    for (int j = 0; j < array_size; j++) {
		array[j] = new IntInt(random.nextInt(), random.nextInt());
	    }
	    start[i] = System.nanoTime();
	    Arrays.sort(array, comparator);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);

	random = new Random(seed);
	System.out.println("\nNative:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    IntInt[] array = new IntInt[array_size];
	    array = new IntInt[array_size];
	    for (int j = 0; j < array_size; j++) {
		array[j] = new IntInt(random.nextInt(), random.nextInt());
	    }
	    sortIntIntArray(array);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    IntInt[] array = new IntInt[array_size];
	    for (int j = 0; j < array_size; j++) {
		array[j] = new IntInt(random.nextInt(), random.nextInt());
	    }
	    start[i] = System.nanoTime();
	    sortIntIntArray(array);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);
    }


    void mesureInitializationTime3(int array_size, int iterations) {
	System.out.println("\nInitialization time for LongLongLongLong[]:");
	long[] start = new long[iterations];
	long[] end = new long[iterations];

	long l0 = 123456;
	long l1 = -987654;
	long l2 = 192837;
	long l3 = -56473829;
	
	System.out.println("\nJava:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    LongLongLongLong[] array = new LongLongLongLong[array_size];
	    initializeLongLongLongLongArrayJava(array, l0, l1, l2, l3);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    LongLongLongLong[] array = new LongLongLongLong[array_size];
	    start[i] = System.nanoTime();
	    initializeLongLongLongLongArrayJava(array, l0, l1, l2, l3);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);

	System.out.println("\nNative:");
	// Warmup
	for (int i = 0; i < 10; i++) {
	    LongLongLongLong[] array = new LongLongLongLong[array_size];
	    initializeLongLongLongLongArray(array, l0, l1, l2, l3);
	}
	// Measure
	for (int i = 0; i < iterations; i++) {
	    LongLongLongLong[] array = new LongLongLongLong[array_size];
	    start[i] = System.nanoTime();
	    initializeLongLongLongLongArray(array, l0, l1, l2, l3);
	    end[i] = System.nanoTime();
	}
	// Results
	computeStatistics(start, end);
    }
    
    void computeStatistics(long[] start, long[] end) {
	int iterations = start.length;
	long[] duration = new long[iterations];
	long sum = 0;
	long min = end[0] - start[0];
	long max = min;
	double var = 0.0;
	for (int i = 0 ; i < iterations; i++) {
	    duration[i] = end[i] - start[i];
	    if (duration[i] < min) min = duration[i];
	    if (duration[i] > max) max = duration[i];
	    sum += duration[i];
	    double d = (double) duration[i];
	    var += Math.pow(d, 2);
	}
	double avg = (sum/iterations) / 1000;
	double std = (Math.sqrt(var/iterations - Math.pow(sum/iterations, 2))) / 1000;
	System.out.println(String.format("Avg: %8.2f us", avg));
	System.out.println(String.format("Std: %8.2f us", std));
	System.out.println(String.format("Min: %8d us", (min/1000)));
	System.out.println(String.format("Max: %8d us", (max/1000)));
    }

    native int GetFlattenedArrayElementSizeWrapper(Object array);
    native Class GetFlattenedArrayElementClassWrapper(Object array);
    native long GetFlattenedArrayElementsWrapper(Object array);
    native void ReleaseFlattenedArrayElementsWrapper(Object array, long addr,int mode);
    native int GetFieldOffsetInFlattenedLayoutWrapper(Class klass, String name, String signature, boolean flattened);
    
    native int getIntFieldAtIndex(Object[] array, int index, String fieldName, String FieldSignature);
    native void printArrayInformation(Object[] array);

    native void initializeIntIntArrayBuffer(Object[] array, int i0, int i1);
    native void initializeIntIntArrayFields(Object[] array, int i0, int i1);
    native void sortIntIntArray(Object[] array);

    native void initializeContainerArray(Object[] array, double d, float f, short s, byte b);
    native void updateContainerArray(Object[] array, float f, short s);

    native void initializeLongLongLongLongArray(Object[] array, long l0, long l1, long l2, long l3);
}
