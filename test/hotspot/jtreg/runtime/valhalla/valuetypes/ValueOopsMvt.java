/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.valuetypes;

import java.lang.invoke.*;
import java.lang.ref.*;
import java.util.concurrent.*;
import jdk.experimental.value.MethodHandleBuilder;
import jdk.incubator.mvt.ValueType;

import static jdk.test.lib.Asserts.*;
import jdk.test.lib.Utils;

/**
 * @test ValueOopsMvt
 * @summary Test embedding oops into Minimal Value Types
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          jdk.incubator.mvt
 * @library /test/lib
 * @compile PersonVcc.java
 * @compile ValueOopsMvt.java
 * @run main/othervm -Xint -XX:+UseSerialGC -Xmx128m -XX:+EnableMVT
 *                   runtime.valhalla.valuetypes.ValueOopsMvt
 * @run main/othervm -Xint -XX:+UseG1GC -Xmx128m -XX:+EnableMVT
 *                   -XX:-ValueArrayFlatten
 *                   runtime.valhalla.valuetypes.ValueOopsMvt
 * @run main/othervm -Xint -XX:+UseG1GC -Xmx128m -XX:+EnableMVT
 *                   runtime.valhalla.valuetypes.ValueOopsMvt 100
 * @run main/othervm -Xint -XX:+UseParallelGC -Xmx128m -XX:+EnableMVT
 *                   runtime.valhalla.valuetypes.ValueOopsMvt
 * @run main/othervm -Xcomp -XX:+UseSerialGC -Xmx128m -XX:+EnableMVT
 *                   runtime.valhalla.valuetypes.ValueOopsMvt
 * @run main/othervm -Xcomp -XX:+UseG1GC -Xmx128m -XX:+EnableMVT
 *                   runtime.valhalla.valuetypes.ValueOopsMvt 100
 * @run main/othervm -Xcomp -XX:+UseParallelGC -Xmx128m -XX:+EnableMVT
 *                   runtime.valhalla.valuetypes.ValueOopsMvt
 */
public class ValueOopsMvt {

    // Extra debug: -XX:+VerifyOops -XX:+VerifyStack -XX:+VerifyLastFrame -XX:+VerifyBeforeGC -XX:+VerifyAfterGC -XX:+VerifyDuringGC -XX:VerifySubSet=threads,heap
    // Even more debugging: -XX:+TraceNewOopMapGeneration -Xlog:gc*=info


    /*
     * TODO: Crashes with -Xcomp -XX:+UseG1GC -Xmx128m -XX:+EnableMVT -XX:-ValueArrayFlatten runtime.valhalla.valuetypes.ValueOopsMvt
     */

    static final int NOF_PEOPLE = 1000; // Exercise arrays of this size

    static int MIN_ACTIVE_GC_COUNT = 10; // Run active workload for this number of GC passes

    static int MED_ACTIVE_GC_COUNT = 4;  // Medium life span in terms of GC passes

    static final String TEST_STRING1 = "Test String 1";
    static final String TEST_STRING2 = "Test String 2";

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void main(String[] args) {
        if (args.length > 0) {
            MIN_ACTIVE_GC_COUNT = Integer.parseInt(args[0]);
        }
        testClassLoad();
        testBytecodes();
        testMvt();

        // Check we survive GC...
        testOverGc();   // Exercise root scan / oopMap
        testActiveGc(); // Brute force
    }

    /**
     * Test ClassFileParser can load values with reference fields
     */
    public static void testClassLoad() {
        // MVT
        Class<?> vccClass = PersonVcc.class;
        ValueType<?> vt = ValueType.forClass(vccClass);
        Class<?> boxClass = vt.boxClass();
        Class<?> dvtClass = vt.valueClass();
        Class<?> arrayClass = vt.arrayValueClass();
        dvtClass.toString();
    }


    /*
      Following tests are broken down into different use cases, and used in
      multi-threaded stress tests.

      Keeping the use cases separated helps isolate problems when debugging.

      Since we are testing the VM here, no the ValueType API is mostly ignored
      and the test exercises specific bytecode
     */

    /*
      Method Handle generation is mixed into the invokation code as an attempt
      to increase readability.
     */

    /**
     * Test Values with Oops with specific bytecodes for various use cases
     *
     * Value type specific bytecodes...
     *
     * vload
     * vstore
     * vaload
     * vastore
     * vreturn
     * vdefault
     * vwithfield
     * vbox
     * vunbox
     *
     * Bytecode accepting value types (QTypes)
     *
     * anewarray
     * multianewarray
     * getfield
     */
    public static void testBytecodes() {
        try {
            testBytecodesStackVDefault();
            testBytecodesStackVUnbox();
            testBytecodesStackAndSlots();
            testBytecodesStackAndSlotsDeep();
            testBytecodesVBox();
            testBytecodesVReturn();
            testBytecodesGetField();
            testBytecodesVwithfield();
            testBytecodesValueArray();
            testBytecodesField();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static MethodHandle stacksVDefaultTest;

    // vdefault on stack and pop
    public static void testBytecodesStackVDefault() throws Throwable {
        if (stacksVDefaultTest == null) { // Gen MH once
            stacksVDefaultTest = MethodHandleBuilder.loadCode(LOOKUP,
                "stacksVDefaultTest", MethodType.methodType(Void.TYPE),
                CODE->{
                CODE
                .vdefault(ValueType.forClass(PersonVcc.class).valueClass())
                .pop()
                .return_();
                });
        }
        stacksVDefaultTest.invokeExact();
    }

    static MethodHandle stackVUnboxTest;

    // vunbox on stack and pop
    public static void testBytecodesStackVUnbox() throws Throwable {
        if (stackVUnboxTest == null) {
            stackVUnboxTest = MethodHandleBuilder.loadCode(LOOKUP,
               "stackVUnboxTest", MethodType.methodType(Void.TYPE, PersonVcc.class),
                CODE->{
                CODE
                .aload(0)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .pop()
                .return_();
                });
        }
        stackVUnboxTest.invokeExact(createIndexedPersonVcc(7341));
    }

    static MethodHandle stackAndSlotsTest;

    // load/store with stack and slots
    public static void testBytecodesStackAndSlots() throws Throwable {
        if (stackAndSlotsTest == null) {
            stackAndSlotsTest = MethodHandleBuilder.loadCode(LOOKUP,
               "stackAndSlotsTest", MethodType.methodType(Void.TYPE, PersonVcc.class),
                CODE->{
                CODE
                .aload(0)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .vstore(1)
                .vload(1)
                .vstore(2)
                .return_();
                });
        }
        stackAndSlotsTest.invokeExact(createIndexedPersonVcc(7342));
    }

    static MethodHandle stackAndSlotsDeepTest;

    // load/store with stack and slots, and call deeper
    public static void testBytecodesStackAndSlotsDeep() throws Throwable {
        if (stackAndSlotsDeepTest == null) {
            stackAndSlotsDeepTest = MethodHandleBuilder.loadCode(LOOKUP,
               "stackAndSlotsDeepTest", MethodType.methodType(Void.TYPE, PersonVcc.class),
                CODE->{
                CODE
                .aload(0)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .vstore(1)
                .vload(1)
                .vstore(2)
                .vload(2)
                .invokestatic(ValueOopsMvt.class, "testBytecodesStackAndSlots", "()V", false)
                .pop()
                .return_();
                });
        }
        stackAndSlotsDeepTest.invokeExact(createIndexedPersonVcc(7343));
    }

    static MethodHandle vboxTest;

    // vbox/vunbox, value on stack
    public static void testBytecodesVBox() throws Throwable {
        if (vboxTest == null) {
            vboxTest = MethodHandleBuilder.loadCode(LOOKUP,
                "unboxBox", MethodType.methodType(PersonVcc.class, PersonVcc.class),
                CODE->{
                CODE
                .aload(0)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .vbox(PersonVcc.class)
                .areturn();
                });
        }
        int index = 7344;
        PersonVcc person = (PersonVcc) vboxTest.invokeExact(createIndexedPersonVcc(index));
        validateIndexedPersonVcc(person, index);
    }

    static MethodHandle vreturnTest;

    // vreturn and pass value as arg
    public static void testBytecodesVReturn() throws Throwable {
        if (vreturnTest == null) {
            MethodHandle vunboxVReturn = MethodHandleBuilder.loadCode(LOOKUP,
                "vunboxVReturn", MethodType.methodType(ValueType.forClass(PersonVcc.class).valueClass(), PersonVcc.class),
                CODE->{
                CODE
                .aload(0)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .vreturn();
                });
            MethodHandle vboxAReturn = MethodHandleBuilder.loadCode(LOOKUP,
                "vboxAReturn", MethodType.methodType(PersonVcc.class, ValueType.forClass(PersonVcc.class).valueClass()),
                CODE->{
                CODE
                .vload(0)
                .vbox(PersonVcc.class)
                .areturn();
                });
            vreturnTest = MethodHandles.filterReturnValue(vunboxVReturn, vboxAReturn);
        }
        int index = 7345;
        PersonVcc person = (PersonVcc) vreturnTest.invokeExact(createIndexedPersonVcc(index));
        validateIndexedPersonVcc(person, index);
    }

    static MethodHandle getFieldTest;

    public static void testBytecodesGetField() throws Throwable {
        if (getFieldTest == null) {
            getFieldTest = MethodHandleBuilder.loadCode(LOOKUP,
                "getFieldTest", MethodType.methodType(String.class, PersonVcc.class),
                CODE->{
                CODE
                .aload(0)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .getfield(ValueType.forClass(PersonVcc.class).valueClass(), "lastName", "Ljava/lang/String;")
                .areturn();
                });
        }
        int index = 7346;
        String lastName = (String) getFieldTest.invokeExact(createIndexedPersonVcc(index));
        assertEquals(lastName, lastName(index));
    }

    static MethodHandle vwtihfieldTest;

    public static void testBytecodesVwithfield() throws Throwable {
        if (vwtihfieldTest == null) {
            Class<?> dvtClass = ValueType.forClass(PersonVcc.class).valueClass();
            vwtihfieldTest = MethodHandleBuilder.loadCode(MethodHandles.privateLookupIn(dvtClass, LOOKUP),
                "vwtihfieldTest", MethodType.methodType(PersonVcc.class, PersonVcc.class, Integer.TYPE, String.class, String.class),
                CODE->{
                CODE
                .aload(0)
                .vunbox(dvtClass)
                .iload(1)
                .vwithfield(dvtClass, "id", "I")
                .aload(2)
                .vwithfield(dvtClass, "firstName", "Ljava/lang/String;")
                .aload(3)
                .vwithfield(dvtClass, "lastName", "Ljava/lang/String;")
                .vbox(PersonVcc.class)
                .areturn();
                });
        }
        int index = 7347;
        int diffIndex = 4711;
        PersonVcc person = (PersonVcc) vwtihfieldTest.invokeExact(createIndexedPersonVcc(index), diffIndex, firstName(diffIndex), lastName(diffIndex));
        validateIndexedPersonVcc(person, diffIndex);
    }

    // load/store with arrays
    public static void testBytecodesValueArray() throws Throwable {
        testBytecodesArrayNew();
        testBytecodesArrayLoad();
        testBytecodesArrayStore();
        testBytecodesArrayStoreLoad();

        // multidim...
    }

    static MethodHandle anewarrayTest;

    public static void testBytecodesArrayNew() throws Throwable {
        if (anewarrayTest == null) {
            anewarrayTest = MethodHandleBuilder.loadCode(LOOKUP,
                "anewarrayTest", MethodType.methodType(Integer.TYPE, Integer.TYPE),
                CODE->{
                CODE
                .iload(0)
                .anewarray(ValueType.forClass(PersonVcc.class).valueClass())
                .arraylength()
                .ireturn();
                });
        }
        int asize = (int) anewarrayTest.invokeExact(NOF_PEOPLE);
        assertTrue(asize == NOF_PEOPLE, "Invariant");
    }

    static MethodHandle valoadTest;

    public static void testBytecodesArrayLoad() throws Throwable {
        if (valoadTest == null) {
            valoadTest = MethodHandleBuilder.loadCode(LOOKUP,
                "valoadTest", MethodType.methodType(PersonVcc.class, Integer.TYPE, Integer.TYPE),
                CODE->{
                CODE
                .iload(0)
                .anewarray(ValueType.forClass(PersonVcc.class).valueClass())
                .iload(1)
                .vaload()
                .vbox(PersonVcc.class)
                .areturn();
                });
        }
        PersonVcc person = (PersonVcc) valoadTest.invokeExact(NOF_PEOPLE, 7);
        validateDefaultPersonVcc(person);
    }

    static MethodHandle varrayStoreTest;

    public static void testBytecodesArrayStore() throws Throwable {
        if (varrayStoreTest == null) {
            varrayStoreTest = MethodHandleBuilder.loadCode(LOOKUP,
                "varrayStoreTest", MethodType.methodType(Void.TYPE, Integer.TYPE, Integer.TYPE, PersonVcc.class),
                CODE->{
                CODE
                .iload(0)
                .anewarray(ValueType.forClass(PersonVcc.class).valueClass())
                .iload(1)
                .aload(2)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .vastore()
                .return_();
                });
        }
        int index = 11;
        varrayStoreTest.invokeExact(NOF_PEOPLE, index, createIndexedPersonVcc(index));
    }

    static MethodHandle varrayStoreLoadTest;

    public static void testBytecodesArrayStoreLoad() throws Throwable {
        if (varrayStoreLoadTest == null) {
            varrayStoreLoadTest = MethodHandleBuilder.loadCode(LOOKUP,
                "varrayStoreLoadTest", MethodType.methodType(PersonVcc.class, Integer.TYPE, Integer.TYPE, PersonVcc.class),
                CODE->{
                CODE
                .iload(0)
                .anewarray(ValueType.forClass(PersonVcc.class).valueClass())
                .dup()
                .iload(1)
                .aload(2)
                .vunbox(ValueType.forClass(PersonVcc.class).valueClass())
                .vastore()
                .iload(1)
                .vaload()
                .vbox(PersonVcc.class)
                .areturn();
                });
        }
        int index = NOF_PEOPLE - 1;
        PersonVcc person = (PersonVcc) varrayStoreLoadTest.invokeExact(NOF_PEOPLE, index, createIndexedPersonVcc(index));
        validateIndexedPersonVcc(person, index);
    }

    // load/store with Object fields
    public static void testBytecodesField() throws Throwable {
        // CMH: no MVT support yet
    }

    /**
     * Check value type operations with "Minimal Value Types" (MVT) API
     */
    public static void testMvt() {
        try {
            // MVT...
            ValueType<?> vt = ValueType.forClass(PersonVcc.class);
            Class<?> vtClass = vt.valueClass();
            Class<?> arrayClass = vt.arrayValueClass();

            Object obj = vt.defaultValueConstant().invoke();
            validateDefaultPersonVcc(obj);

            obj = MethodHandles.filterReturnValue(vt.unbox(), vt.box())
                .invoke(createDefaultPersonVcc());
            validateDefaultPersonVcc(obj);

            int index = 11;
            obj = MethodHandles.filterReturnValue(vt.unbox(), vt.box())
                .invoke(createIndexedPersonVcc(index));
            validateIndexedPersonVcc(obj, index);

            testMvtArray("testMvt.array.1", 1);
        }
        catch (Throwable t) {
            fail("testMvtfailed", t);
        }
    }

    /**
     * MVT array operations...
     */
    public static void testMvtPeopleArray() {
        testMvtArray("testMvtPeopleArray", NOF_PEOPLE);
    }

    public static void testMvtArray(String testName, int arrayLength) {
        try {
            Class<?> vcc = PersonVcc.class;
            ValueType<?> vt = ValueType.forClass(vcc);
            Class<?> dvtClass = vt.valueClass();
            Class<?> arrayClass = vt.arrayValueClass();

            MethodHandle arrayElemGet = MethodHandles.arrayElementGetter(arrayClass);
            MethodHandle arrayElemSet = MethodHandles.arrayElementSetter(arrayClass);

            MethodHandle getId = LOOKUP.findGetter(dvtClass, "id", Integer.TYPE);
            MethodHandle getFirstName = LOOKUP.findGetter(dvtClass, "firstName", String.class);
            MethodHandle getLastName = LOOKUP.findGetter(dvtClass, "lastName", String.class);

            MethodHandle getIdFromArray = MethodHandles.filterReturnValue(arrayElemGet, getId);
            MethodHandle getFnFromArray = MethodHandles.filterReturnValue(arrayElemGet, getFirstName);
            MethodHandle getLnFromArray = MethodHandles.filterReturnValue(arrayElemGet, getLastName);

            Object people = vt.newArray().invoke(arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                arrayElemSet.invoke(people, i, createIndexedPersonVcc(i));
            }

            for (int i = 0; i < arrayLength; i++) {
                validateIndexedPersonVcc(arrayElemGet.invoke(people, i), i);

                int id = (int) getIdFromArray.invoke(people, i);
                assertTrue(id == i, "Invalid field: Id, should be: " + i + " got " + id);
                String fn = (String) getFnFromArray.invoke(people, i);
                assertTrue(fn.equals(firstName(i)), "Invalid field: firstName");
                String ln = (String) getLnFromArray.invoke(people, i);
                assertTrue(ln.equals(lastName(i)), "Invalid field: lastName");
            }
        }
        catch (Throwable t) {
            fail(testName + " failed", t);
        }
    }

    /**
     * Check forcing GC for combination of VT on stack/LVT etc works
     */
    public static void testOverGc() {
        try {
            Class<?> vccClass = PersonVcc.class;
            ValueType<?> vt = ValueType.forClass(vccClass);
            Class<?> vtClass = vt.valueClass();
            Class<?> arrayClass = vt.arrayValueClass();

            doGc();

            // VT on stack and lvt, null refs, see if GC flies
            MethodHandle moveValueThroughStackAndLvt = MethodHandleBuilder.loadCode(
                LOOKUP,
                "gcOverPerson",
                MethodType.methodType(vccClass, vccClass),
                CODE->{
                CODE
                .aload(0)
                .vunbox(vtClass)
                .invokestatic(ValueOopsMvt.class, "doGc", "()V", false) // Stack
                .vstore(0)
                .invokestatic(ValueOopsMvt.class, "doGc", "()V", false) // LVT
                .vload(0)
                .iconst_1()  // push a litte further down
                .invokestatic(ValueOopsMvt.class, "doGc", "()V", false) // Stack,LVT
                .pop()
                .vbox(vccClass)
                .areturn();
            });
            Object obj = moveValueThroughStackAndLvt.invoke(createDefaultPersonVcc());
            validateDefaultPersonVcc(obj);
            doGc();
            obj = null;
            doGc();

            int index = 4711;
            obj = moveValueThroughStackAndLvt.invoke(createIndexedPersonVcc(index));
            validateIndexedPersonVcc(obj, index);
            doGc();
            obj = null;
            doGc();
        }
        catch (Throwable t) { fail("testOverGc", t); }
    }

    static void submitNewWork(ForkJoinPool fjPool, int size) {
        for (int i = 0; i < size; i++) {
            fjPool.execute(ValueOopsMvt::testMvtPeopleArray);
            for (int j = 0; j < 100; j++) {
                // JDK-8186718 random crashes in interpreter vbox and vunbox (with G1)
                // test needs refactoring to more specific use cases for debugging.
                fjPool.execute(ValueOopsMvt::testBytecodes);
                fjPool.execute(ValueOopsMvt::testMvt);
            }
        }
    }

    static void sleepNoThrow(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (Throwable t) {}
    }

    /**
     * Run some workloads with different object/value life times...
     */
    public static void testActiveGc() {
        try {
            int nofThreads = 7;
            int workSize = nofThreads * 10;

            Object longLivedObjects = createLongLived();
            Object longLivedPeopleVcc = createPeopleVcc();

            Object medLivedObjects = createLongLived();
            Object medLivedPeopleVcc = createPeopleVcc();

            doGc();

            ForkJoinPool fjPool = new ForkJoinPool(nofThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

            // submit work until we see some GC
            Reference ref = createRef();
            submitNewWork(fjPool, workSize);
            while (ref.get() != null) {
                if (fjPool.hasQueuedSubmissions()) {
                    sleepNoThrow(1L);
                }
                else {
                    workSize *= 2; // Grow the submission size
                    submitNewWork(fjPool, workSize);
                }
            }

            // Keep working and actively GC, until MIN_ACTIVE_GC_COUNT
            int nofActiveGc = 1;
            ref = createRef();
            while (nofActiveGc < MIN_ACTIVE_GC_COUNT) {
                if (ref.get() == null) {
                    nofActiveGc++;
                    ref = createRef();
                    if (nofActiveGc % MED_ACTIVE_GC_COUNT == 0) {
                        validateLongLived(medLivedObjects);
                        validatePeopleVcc(medLivedPeopleVcc);

                        medLivedObjects = createLongLived();
                        medLivedPeopleVcc = createPeopleVcc();
                    }
                }
                else if (fjPool.hasQueuedSubmissions()) {
                    sleepNoThrow((long) Utils.getRandomInstance().nextInt(1000));
                    doGc();
                }
                else {
                    submitNewWork(fjPool, workSize);
                }
            }
            fjPool.shutdown();

            validateLongLived(medLivedObjects);
            validatePeopleVcc(medLivedPeopleVcc);
            medLivedObjects = null;
            medLivedPeopleVcc = null;

            validateLongLived(longLivedObjects);
            validatePeopleVcc(longLivedPeopleVcc);

            longLivedObjects = null;
            longLivedPeopleVcc = null;

            doGc();
        }
        catch (Throwable t) { fail("testMvtActiveGc", t); }
    }

    static final ReferenceQueue<Object> REFQ = new ReferenceQueue<>();

    public static void doGc() {
        // Create Reference, wait until it clears...
        Reference ref = createRef();
        while (ref.get() != null) {
            System.gc();
        }
    }

    static Reference createRef() {
        return new WeakReference<Object>(new Object(), REFQ);
    }

    static void validatePersonVcc(Object obj, int id, String fn, String ln, boolean equals) {
        assertTrue(obj.getClass() == PersonVcc.class, "Expected VCC class");
        PersonVcc person = (PersonVcc) obj;
        assertTrue(person.id == id);
        if (equals) {
            assertTrue(fn.equals(person.getFirstName()), "Invalid field firstName");
            assertTrue(ln.equals(person.getLastName()), "Invalid  field lastName");
        }
        else {
            assertTrue(person.getFirstName() == fn, "Invalid field firstName");
            assertTrue(person.getLastName() == ln, "Invalid  field lastName");
        }
    }

    static PersonVcc createIndexedPersonVcc(int i) {
        return PersonVcc.create(i, firstName(i), lastName(i));
    }

    static void validateIndexedPersonVcc(Object obj, int i) {
        validatePersonVcc(obj, i, firstName(i), lastName(i), true);
    }

    static PersonVcc createDefaultPersonVcc() {
        return PersonVcc.create(0, null, null);
    }

    static void validateDefaultPersonVcc(Object obj) {
        validatePersonVcc(obj, 0, null, null, false);
    }

    static String firstName(int i) {
        return "FirstName-" + i;
    }

    static String lastName(int i) {
        return "LastName-" + i;
    }

    static Object createLongLived()  throws Throwable {
        Object[] population = new Object[1];
        population[0] = createPeopleVcc();
        return population;
    }

    static void validateLongLived(Object pop) throws Throwable {
        Object[] population = (Object[]) pop;
        validatePeopleVcc(population[0]);
    }

    static Object createPeopleVcc() throws Throwable {
        int arrayLength = NOF_PEOPLE;
        Class<?> vccClass = PersonVcc.class;
        ValueType<?> vt = ValueType.forClass(vccClass);
        MethodHandle arrayElemSet = MethodHandles.arrayElementSetter(vt.arrayValueClass());

        Object people = vt.newArray().invoke(arrayLength);
        for (int i = 0; i < arrayLength; i++) {
            arrayElemSet.invoke(people, i, createIndexedPersonVcc(i));
        }
        return people;
    }

    static void validatePeopleVcc(Object people) throws Throwable {
        MethodHandle arrayElemGet = MethodHandles.arrayElementGetter(
            ValueType.forClass((Class<?>)PersonVcc.class).arrayValueClass());

        int arrayLength = java.lang.reflect.Array.getLength(people);
        assertTrue(arrayLength == NOF_PEOPLE);
        for (int i = 0; i < arrayLength; i++) {
            validateIndexedPersonVcc(arrayElemGet.invoke(people, i), i);
        }
    }

}

