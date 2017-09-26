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
import sun.hotspot.WhiteBox;

/**
 * @test ValueOops
 * @summary Test embedding oops into Value types
 * @modules java.base/jdk.experimental.value
 *          jdk.incubator.mvt
 * @library /test/lib
 * @compile PersonVcc.java
 * @compile -XDenableValueTypes Person.java
 * @compile -XDenableValueTypes ValueOops.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xint -noverify -XX:+UseSerialGC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 * @run main/othervm -Xint -noverify -XX:+UseG1GC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 * @run main/othervm -Xint -noverify -XX:+UseParallelGC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 * @run main/othervm -Xint -noverify -XX:+UseConcMarkSweepGC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 * @run main/othervm -Xcomp -noverify -XX:+UseSerialGC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 * @run main/othervm -Xcomp -noverify -XX:+UseG1GC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 * @run main/othervm -Xcomp -noverify -XX:+UseParallelGC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 * @run main/othervm -Xcomp -noverify -XX:+UseConcMarkSweepGC -Xmx128m -XX:+EnableMVT -XX:+EnableValhalla
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.valuetypes.ValueOops
 */
public class ValueOops {

    // Note: -noverify can not be eliminated. Possible issue with ValueType::mhName(),
    //       does not translate '.' to '/' for JVM.
    //       java.lang.ClassFormatError: Illegal method name "runtime.valhalla.valuetypes.PersonVcc_default"
    //
    // Extra debug: -XX:+VerifyOops -XX:+VerifyStack -XX:+VerifyLastFrame -XX:+VerifyBeforeGC -XX:+VerifyAfterGC -XX:+VerifyDuringGC -XX:VerifySubSet=threads,heap
    // Even more debugging: -XX:+TraceNewOopMapGeneration -Xlog:gc*=info

    static final int NOF_PEOPLE = 1000; // Exercise arrays of this size

    static int MIN_ACTIVE_GC_COUNT = 10; // Run active workload for this number of GC passes

    static int MED_ACTIVE_GC_COUNT = 4;  // Medium life span in terms of GC passes

    static final String TEST_STRING1 = "Test String 1";
    static final String TEST_STRING2 = "Test String 2";

    static boolean USE_COMPILER = WhiteBox.getWhiteBox().getBooleanVMFlag("UseCompiler");

    public static void main(String[] args) {
        if (args.length > 0) {
            MIN_ACTIVE_GC_COUNT = Integer.parseInt(args[0]);
        }
        testClassLoad();
        testBytecodes();
        testVvt();
        testMvt();

        if (!USE_COMPILER) {
            testOopMaps();
        }

        // Check we survive GC...
        testOverGc();   // Exercise root scan / oopMap
        testActiveGc(); // Brute force
    }

    /**
     * Test ClassFileParser can load values with reference fields
     */
    public static void testClassLoad() {
        // VVT
        String s = Person.class.toString();
        new Bar();
        new BarWithValue();
        s = BarValue.class.toString();
        s = ObjectWithObjectValue.class.toString();
        s = ObjectWithObjectValues.class.toString();

        // MVT
        Class<?> vccClass = PersonVcc.class;
        ValueType<?> vt = ValueType.forClass(vccClass);
        Class<?> boxClass = vt.boxClass();
        Class<?> dvtClass = vt.valueClass();
        Class<?> arrayClass = vt.arrayValueClass();
        s = dvtClass.toString();
    }

    /**
     * Test value type opcodes are okay with reference fields in values
     * I.e. ValueKlass::value_store()
     */
    public static void testBytecodes() {
        try {
            // Craft Value Type class how you will, using MVT class for simplicity just here
            ValueType<?> vt = ValueType.forClass(PersonVcc.class);
            Class<?> vtClass = vt.valueClass();
            Class<?> arrayClass = vt.arrayValueClass();
            Class<?> boxClass = vt.boxClass();
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // Exercise with null refs...

            // anewarray
            Object array = MethodHandleBuilder.loadCode(
                lookup,
                "anewarrayPerson",
                MethodType.methodType(Object.class, Integer.TYPE),
                CODE->{
                CODE
                .iload(0)
                .anewarray(vtClass)
                .astore(0)
                .aload(0)
                .areturn();
            }).invoke(NOF_PEOPLE);

            // vaload
            // vstore
            // vload
            // vastore
            // vbox
            int arraySlot = 0;
            int indexSlot = 1;
            int valueSlot = 2;
            Object obj = MethodHandleBuilder.loadCode(
                lookup,
                "valoadBoxPerson",
                MethodType.methodType(Object.class, arrayClass, Integer.TYPE),
                CODE->{
                CODE
                .aload(arraySlot)
                .iload(indexSlot)
                .vaload()
                .vstore(valueSlot)
                .aload(arraySlot)
                .iload(indexSlot)
                .vload(valueSlot)
                .vastore()
                .vload(valueSlot)
                .vbox(boxClass)
                .areturn();
            }).invoke(array, 7);
            validateDefaultPersonVcc(obj);

            // vreturn
            MethodHandle loadValueFromArray = MethodHandleBuilder.loadCode(
                lookup,
                "valoadVreturnPerson",
                MethodType.methodType(vtClass, arrayClass, Integer.TYPE),
                CODE->{
                CODE
                .aload(arraySlot)
                .iload(indexSlot)
                .vaload()
                .vreturn();
            });
            MethodHandle box = MethodHandleBuilder.loadCode(
                lookup,
                "boxPersonVcc",
                MethodType.methodType(boxClass, vtClass),
                CODE->{
                CODE
                .vload(0)
                .vbox(boxClass)
                .areturn();
            });
            MethodHandle loadValueFromArrayBoxed =
                MethodHandles.filterReturnValue(loadValueFromArray, box);
            obj = loadValueFromArrayBoxed.invoke(array, 0);
            validateDefaultPersonVcc(obj);

            // vunbox
            MethodHandle unbox = MethodHandleBuilder.loadCode(
                lookup,
                "unboxPersonVcc",
                MethodType.methodType(vtClass, boxClass),
                CODE->{
                CODE
                .aload(0)
                .vunbox(vtClass)
                .vreturn();
            });
            MethodHandle unboxBox = MethodHandles.filterReturnValue(unbox, box);
            obj = unboxBox.invoke(createDefaultPersonVcc());
            validateDefaultPersonVcc(obj);

            /*
               vgetfield
               qputfield
               qgetfield

               going to need VVT for VT fields and vgetfield
               check test coverage in testVvt()
            */

            // Exercise with live refs...
            Thread.holdsLock("Debug here");
            // vunbox
            // vastore
            // vaload
            // vstore
            // vload
            // vbox
            int index = 3;
            obj = MethodHandleBuilder.loadCode(
                lookup,
                "unboxStoreLoadPersonVcc",
                MethodType.methodType(boxClass, arrayClass, Integer.TYPE, boxClass),
                CODE->{
                CODE
                .aload(arraySlot)
                .iload(indexSlot)
                .aload(valueSlot)
                .vunbox(vtClass)
                .vastore()
                .aload(arraySlot)
                .iload(indexSlot)
                .vaload()
                .vstore(valueSlot)
                .vload(valueSlot)
                .vbox(boxClass)
                .areturn();
            }).invoke(array, index, createIndexedPersonVcc(index));
            validateIndexedPersonVcc(obj, index);

            // Check the neighbours
            validateDefaultPersonVcc(loadValueFromArrayBoxed.invoke(array, index - 1));
            validateDefaultPersonVcc(loadValueFromArrayBoxed.invoke(array, index + 1));

            // vreturn
            validateIndexedPersonVcc(unboxBox.invoke((PersonVcc)obj), index);
        }
        catch (Throwable t) { fail("testBytecodes", t); }
    }

    static class Couple {
        public Person onePerson;
        public Person otherPerson;
    }

    static final __ByValue class Composition {
        public final Person onePerson;
        public final Person otherPerson;

        private Composition() {
            this.onePerson   = Person.create(0, null, null);
            this.otherPerson = Person.create(0, null, null);
        }

        __ValueFactory public static Composition create(Person onePerson, Person otherPerson) {
            Composition comp = __MakeDefault Composition();
            comp.onePerson   = onePerson;
            comp.otherPerson = otherPerson;
            return comp;
        }
    }

    /**
     * Check value type operations with "Valhalla Value Types" (VVT)
     */
    public static void testVvt() {
        // Exercise creation, vgetfield, vreturn with null refs
        validateDefaultPerson(createDefaultPerson());

        // anewarray, vaload, vastore
        int index = 7;
        Person[] array =  new Person[NOF_PEOPLE];
        validateDefaultPerson(array[index]);

        // Now with refs...
        validateIndexedPerson(createIndexedPerson(index), index);
        array[index] = createIndexedPerson(index);
        validateIndexedPerson(array[index], index);

        // Check the neighbours
        validateDefaultPerson(array[index - 1]);
        validateDefaultPerson(array[index + 1]);

        // getfield/putfield
        Couple couple = new Couple();
        validateDefaultPerson(couple.onePerson);
        validateDefaultPerson(couple.otherPerson);

        couple.onePerson = createIndexedPerson(index);
        validateIndexedPerson(couple.onePerson, index);

        Composition composition = Composition.create(couple.onePerson, couple.onePerson);
        validateIndexedPerson(composition.onePerson, index);
        validateIndexedPerson(composition.otherPerson, index);
    }

    /**
     * Check value type operations with "Minimal Value Types" (MVT)
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

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle getId = lookup.findGetter(dvtClass, "id", Integer.TYPE);
            MethodHandle getFirstName = lookup.findGetter(dvtClass, "firstName", String.class);
            MethodHandle getLastName = lookup.findGetter(dvtClass, "lastName", String.class);

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
                assertTrue(id == i, "Invalid field: Id");
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
     * Check oop map generation for klass layout and frame...
     */
    public static void testOopMaps() {
        Object[] objects = WhiteBox.getWhiteBox().getObjectsViaKlassOopMaps(new Couple());
        assertTrue(objects.length == 4, "Expected 4 oops");
        for (int i = 0; i < objects.length; i++) {
            assertTrue(objects[i] == null, "not-null");
        }

        String fn1 = "Sam";
        String ln1 = "Smith";
        String fn2 = "Jane";
        String ln2 = "Jones";
        Couple couple = new Couple();
        couple.onePerson = Person.create(0, fn1, ln1);
        couple.otherPerson = Person.create(1, fn2, ln2);
        objects = WhiteBox.getWhiteBox().getObjectsViaKlassOopMaps(couple);
        assertTrue(objects.length == 4, "Expected 4 oops");
        assertTrue(objects[0] == fn1, "Bad oop fn1");
        assertTrue(objects[1] == ln1, "Bad oop ln1");
        assertTrue(objects[2] == fn2, "Bad oop fn2");
        assertTrue(objects[3] == ln2, "Bad oop ln2");

        objects = WhiteBox.getWhiteBox().getObjectsViaOopIterator(couple);
        assertTrue(objects.length == 4, "Expected 4 oops");
        assertTrue(objects[0] == fn1, "Bad oop fn1");
        assertTrue(objects[1] == ln1, "Bad oop ln1");
        assertTrue(objects[2] == fn2, "Bad oop fn2");
        assertTrue(objects[3] == ln2, "Bad oop ln2");

        // Array..
        objects = WhiteBox.getWhiteBox().getObjectsViaOopIterator(createPeople());
        assertTrue(objects.length == NOF_PEOPLE * 2, "Unexpected length: " + objects.length);
        int o = 0;
        for (int i = 0; i < NOF_PEOPLE; i++) {
            assertTrue(objects[o++].equals(firstName(i)), "Bad firstName");
            assertTrue(objects[o++].equals(lastName(i)), "Bad lastName");
        }

        // Sanity check, FixMe need more test cases
        objects = testFrameOops(couple);
        assertTrue(objects.length == 5, "Number of frame oops incorrect = " + objects.length);
        assertTrue(objects[0] == couple, "Bad oop 0");
        assertTrue(objects[1] == fn1, "Bad oop 1");
        assertTrue(objects[2] == ln1, "Bad oop 2");
        assertTrue(objects[3] == TEST_STRING1, "Bad oop 3");
        assertTrue(objects[4] == TEST_STRING2, "Bad oop 4");

        testFrameOopsVBytecodes();
    }

    static final String GET_OOP_MAP_NAME = "getOopMap";
    static final String GET_OOP_MAP_DESC = "()[Ljava/lang/Object;";

    static Object[] getOopMap() {
        Object[] oopMap = WhiteBox.getWhiteBox().getObjectsViaFrameOopIterator(2);
        /* Remove this frame (class mirror for this method), and above class mirror */
        Object[] trimmedOopMap = new Object[oopMap.length - 2];
        System.arraycopy(oopMap, 2, trimmedOopMap, 0, trimmedOopMap.length);
        return trimmedOopMap;
    }

    // Expecting Couple couple, Person couple.onePerson, and Person (created here)
    public static Object[] testFrameOops(Couple couple) {
        int someId = 89898;
        Person person = couple.onePerson;
        assertTrue(person.getId() == 0, "Bad Person");
        Person anotherPerson = Person.create(someId, TEST_STRING1, TEST_STRING2);
        assertTrue(anotherPerson.getId() == someId, "Bad Person");
        return getOopMap();
    }

    // Debug...
    static void dumpOopMap(Object[] oopMap) {
        System.out.println("Oop Map len: " + oopMap.length);
        for (int i = 0; i < oopMap.length; i++) {
            System.out.println("[" + i + "] = " + oopMap[i]);
        }
    }

    /**
     * Just some check sanity checks with vdefault, vwithfield, vstore and vload
     *
     * Changes to javac slot usage may well break this test
     */
    public static void testFrameOopsVBytecodes() {
        int nofOopMaps = 4;
        Object[][] oopMaps = new Object[nofOopMaps][];
        String[] inputArgs = new String[] { "aName", "aDescription", "someNotes" };

        FooValue.testFrameOopsDefault(oopMaps);

        dumpOopMap(oopMaps[0]);
        dumpOopMap(oopMaps[1]);
        dumpOopMap(oopMaps[2]);
        dumpOopMap(oopMaps[3]);

        // Test-D0 Slots=R Stack=Q(RRR)RV
        assertTrue(oopMaps[0].length == 5 &&
                   oopMaps[0][1] == null &&
                   oopMaps[0][2] == null &&
                   oopMaps[0][3] == null, "Test-D0 incorrect");

        // Test-D1 Slots=R Stack=RV
        assertTrue(oopMaps[1].length == 2, "Test-D1 incorrect");

        // Test-D2 Slots=RQ(RRR) Stack=RV
        assertTrue(oopMaps[2].length == 5 &&
                   oopMaps[2][1] == null &&
                   oopMaps[2][2] == null &&
                   oopMaps[2][3] == null, "Test-D2 incorrect");

        // Test-D3 Slots=R Stack=Q(RRR)RV
        assertTrue(oopMaps[3].length == 6 &&
                   oopMaps[3][1] == null &&
                   oopMaps[3][2] == null &&
                   oopMaps[3][3] == null &&
                   oopMaps[3][4] == null, "Test-D3 incorrect");

        // With ref fields...
        String name = "TestName";
        String desc = "TestDesc";
        String note = "TestNotes";
        FooValue.testFrameOopsRefs(name, desc, note, oopMaps);
        dumpOopMap(oopMaps[0]);

        // Test-R0 Slots=RR Stack=Q(RRR)RV
        assertTrue(oopMaps[0].length == 6 &&
                   oopMaps[0][2] == name &&
                   oopMaps[0][3] == desc &&
                   oopMaps[0][4] == note, "Test-R0 incorrect");

        /**
         * TODO: vwithfield from method handle cooked from anonymous class within the value class
         *       even with "MethodHandles.privateLookupIn()" will fail final putfield rules
         */
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

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            doGc();

            // VT on stack and lvt, null refs, see if GC flies
            MethodHandle moveValueThroughStackAndLvt = MethodHandleBuilder.loadCode(
                lookup,
                "gcOverPerson",
                MethodType.methodType(vccClass, vccClass),
                CODE->{
                CODE
                .aload(0)
                .vunbox(vtClass)
                .invokestatic(ValueOops.class, "doGc", "()V", false) // Stack
                .vstore(0)
                .invokestatic(ValueOops.class, "doGc", "()V", false) // LVT
                .vload(0)
                .iconst_1()  // push a litte further down
                .invokestatic(ValueOops.class, "doGc", "()V", false) // Stack,LVT
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
            fjPool.execute(ValueOops::testMvtPeopleArray);
            for (int j = 0; j < 100; j++) {
                // JDK-8186718 random crashes in interpreter vbox and vunbox (with G1)
                // test needs refactoring to more specific use cases for debugging.
                //fjPool.execute(ValueOops::testBytecodes);
                fjPool.execute(ValueOops::testVvt);
                fjPool.execute(ValueOops::testMvt);
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
            Object longLivedPeople = createPeople();
            Object longLivedPeopleVcc = createPeopleVcc();

            Object medLivedObjects = createLongLived();
            Object medLivedPeople = createPeople();
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
                        validatePeople(medLivedPeople);
                        validatePeopleVcc(medLivedPeopleVcc);

                        medLivedObjects = createLongLived();
                        medLivedPeople = createPeople();
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
            validatePeople(medLivedPeople);
            validatePeopleVcc(medLivedPeopleVcc);
            medLivedObjects = null;
            medLivedPeople = null;
            medLivedPeopleVcc = null;

            validateLongLived(longLivedObjects);
            validatePeople(longLivedPeople);
            validatePeopleVcc(longLivedPeopleVcc);

            longLivedObjects = null;
            longLivedPeople = null;
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


    static void validatePerson(Person person, int id, String fn, String ln, boolean equals) {
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

    static Person createIndexedPerson(int i) {
        return Person.create(i, firstName(i), lastName(i));
    }

    static void validateIndexedPerson(Person person, int i) {
        validatePerson(person, i, firstName(i), lastName(i), true);
    }

    static Person createDefaultPerson() {
        return Person.create(0, null, null);
    }

    static void validateDefaultPerson(Person person) {
        validatePerson(person, 0, null, null, false);
    }

    static String firstName(int i) {
        return "FirstName-" + i;
    }

    static String lastName(int i) {
        return "LastName-" + i;
    }

    static Object createLongLived()  throws Throwable {
        Object[] population = new Object[2];
        population[0] = createPeople();
        population[1] = createPeopleVcc();
        return population;
    }

    static void validateLongLived(Object pop) throws Throwable {
        Object[] population = (Object[]) pop;
        validatePeople(population[0]);
        validatePeopleVcc(population[1]);
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

    static Object createPeople() {
        int arrayLength = NOF_PEOPLE;
        Person[] people = new Person[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            people[i] = createIndexedPerson(i);
        }
        return people;
    }

    static void validatePeople(Object array) {
        Person[] people = (Person[]) array;
        int arrayLength = people.length;
        assertTrue(arrayLength == NOF_PEOPLE);
        for (int i = 0; i < arrayLength; i++) {
            validateIndexedPerson(people[i], i);
        }
    }

    // Various field layouts...sanity testing, see MVTCombo testing for full-set

    static final __ByValue class ObjectValue {
        final Object object;

        private ObjectValue(Object object) {
            this.object = object;
        }
    }

    static class ObjectWithObjectValue {
        ObjectValue value1;
        Object      ref1;
    }

    static class ObjectWithObjectValues {
        ObjectValue value1;
        ObjectValue value2;
        Object      ref1;
    }

    static class Foo {
        int id;
        String name;
        String description;
        long timestamp;
        String notes;
    }

    static class Bar extends Foo {
        long extendedId;
        String moreNotes;
        int count;
        String otherStuff;
    }

    static final __ByValue class FooValue {
        final int id;
        final String name;
        final String description;
        final long timestamp;
        final String notes;

        private FooValue() {
            this.id          = 0;
            this.name        = null;
            this.description = null;
            this.timestamp   = 0L;
            this.notes       = null;
        }

        __ValueFactory public static FooValue create(int id, String name, String description, long timestamp, String notes) {
            FooValue f = __MakeDefault FooValue();
            f.id          = id;
            f.name        = name;
            f.description = description;
            f.timestamp   = timestamp;
            f.notes       = notes;
            return f;
        }

        public static void testFrameOopsDefault(Object[][] oopMaps) {
            Class<?> fooValueCls = FooValue.class;
            MethodType mt = MethodType.methodType(Void.TYPE, oopMaps.getClass());
            int oopMapsSlot   = 0;
            int vtSlot        = 1;

            // Slots 1=oopMaps
            // OopMap Q=RRR (.name .description .someNotes)
            try {
                MethodHandleBuilder
                    .loadCode(MethodHandles.lookup(),
                              "exerciseVBytecodeExprStackWithDefault", mt,
                              CODE->{
                                  CODE
                                      .vdefault(fooValueCls)
                                      .aload(oopMapsSlot)
                                      .iconst_0()  // Test-D0 Slots=R Stack=Q(RRR)RV
                                      .invokestatic(ValueOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                                      .aastore()
                                      .pop()
                                      .aload(oopMapsSlot)
                                      .iconst_1()  // Test-D1 Slots=R Stack=RV
                                      .invokestatic(ValueOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                                      .aastore()
                                      .vdefault(fooValueCls)
                                      .vstore(vtSlot)
                                      .aload(oopMapsSlot)
                                      .iconst_2()  // Test-D2 Slots=RQ(RRR) Stack=RV
                                      .invokestatic(ValueOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                                      .aastore()
                                      .vload(vtSlot)
                                      .aconst_null()
                                      .astore(vtSlot) // Storing null over the Q slot won't remove the ref, but should be single null ref
                                      .aload(oopMapsSlot)
                                      .iconst_3()  // Test-D3 Slots=R Stack=Q(RRR)RV
                                      .invokestatic(ValueOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                                      .aastore()
                                      .pop()
                                      .return_();
                              }).invoke(oopMaps);
            } catch (Throwable t) { fail("exerciseVBytecodeExprStackWithDefault", t); }
        }

        public static void testFrameOopsRefs(String name, String description, String notes, Object[][] oopMaps) {
            Class<?> fooValueCls = FooValue.class;
            FooValue f = create(4711, name, description, 9876543231L, notes);
            FooValue[] fa = new FooValue[] { f };
            MethodType mt = MethodType.methodType(Void.TYPE, fa.getClass(), oopMaps.getClass());
            int fooArraySlot  = 0;
            int oopMapsSlot   = 1;
            try {
                MethodHandleBuilder
                    .loadCode(MethodHandles.lookup(),
                              "exerciseVBytecodeExprStackWithRefs", mt,
                              CODE->{
                                  CODE
                                      .aload(fooArraySlot)
                                      .iconst_0()
                                      .vaload()
                                      .aload(oopMapsSlot)
                                      .iconst_0()  // Test-R0 Slots=RR Stack=Q(RRR)RV
                                      .invokestatic(ValueOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                                      .aastore()
                                      .pop()
                                      .return_();
                              }).invoke(fa, oopMaps);
            } catch (Throwable t) { fail("exerciseVBytecodeExprStackWithDefault", t); }
        }
    }

    static class BarWithValue {
        FooValue foo;
        long extendedId;
        String moreNotes;
        int count;
        String otherStuff;
    }

    static final __ByValue class BarValue {
        final FooValue foo;
        final long extendedId;
        final String moreNotes;
        final int count;
        final String otherStuff;

        private BarValue(FooValue foo, long extendedId, String moreNotes, int count, String otherStuff) {
            this.foo = foo;
            this.extendedId = extendedId;
            this.moreNotes = moreNotes;
            this.count = count;
            this.otherStuff = otherStuff;
        }
    }

}

