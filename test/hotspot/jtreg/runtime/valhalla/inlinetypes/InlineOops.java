/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes;

import java.lang.invoke.*;
import java.lang.ref.*;
import java.util.concurrent.*;

import static jdk.test.lib.Asserts.*;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;
import test.java.lang.invoke.lib.InstructionHelper;

/**
 * @test InlineOops_int_Serial
 * @requires vm.gc.Serial
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xint -XX:+UseSerialGC -Xmx128m -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops
 */

/**
 * @test InlineOops_int_G1
 * @requires vm.gc.G1
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xint  -XX:+UseG1GC -Xmx128m -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops 20
 */

/**
 * @test InlineOops_int_Parallel
 * @requires vm.gc.Parallel
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xint -XX:+UseParallelGC -Xmx128m -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops
 */

/**
 * @test InlineOops_int_Z
 * @requires vm.gc.Z
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xint -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+ZVerifyViews -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops
 */

/**
 * @test InlineOops_comp_serial
 * @requires vm.gc.Serial
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xcomp -XX:+UseSerialGC -Xmx128m -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops
 */

/**
 * @test InlineOops_comp_G1
 * @requires vm.gc.G1
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xcomp -XX:+UseG1GC -Xmx128m -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops 20
 */

/**
 * @test InlineOops_comp_Parallel
 * @requires vm.gc.Parallel
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xcomp -XX:+UseParallelGC -Xmx128m -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops
 */

/**
 * @test InlineOops_comp_Z
 * @requires vm.gc.Z
 * @summary Test embedding oops into Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile -XDallowWithFieldOperator Person.java
 * @compile -XDallowWithFieldOperator InlineOops.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                   sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xcomp -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xmx128m
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+ZVerifyViews -XX:InlineFieldMaxFlatSize=128
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   runtime.valhalla.inlinetypes.InlineOops
 */
public class InlineOops {

    // Extra debug: -XX:+VerifyOops -XX:+VerifyStack -XX:+VerifyLastFrame -XX:+VerifyBeforeGC -XX:+VerifyAfterGC -XX:+VerifyDuringGC -XX:VerifySubSet=threads,heap
    // Even more debugging: -XX:+TraceNewOopMapGeneration -Xlog:gc*=info

    static final int NOF_PEOPLE = 10000; // Exercise arrays of this size

    static int MIN_ACTIVE_GC_COUNT = 10; // Run active workload for this number of GC passes

    static int MED_ACTIVE_GC_COUNT = 4;  // Medium life span in terms of GC passes

    static final String TEST_STRING1 = "Test String 1";
    static final String TEST_STRING2 = "Test String 2";

    static boolean USE_COMPILER = WhiteBox.getWhiteBox().getBooleanVMFlag("UseCompiler");

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void main(String[] args) {
        if (args.length > 0) {
            MIN_ACTIVE_GC_COUNT = Integer.parseInt(args[0]);
        }
        testClassLoad();
        testValues();

        if (!USE_COMPILER) {
            testOopMaps();
        }

        // Check we survive GC...
        testOverGc();   // Exercise root scan / oopMap
        testActiveGc(); // Brute force
    }

    /**
     * Test ClassFileParser can load inline types with reference fields
     */
    public static void testClassLoad() {
        String s = Person.class.toString();
        new Bar();
        new BarWithValue();
        s = BarValue.class.toString();
        s = ObjectWithObjectValue.class.toString();
        s = ObjectWithObjectValues.class.toString();
    }


    static class Couple {
        public Person onePerson;
        public Person otherPerson;
    }

    static final primitive class Composition {
        public final Person onePerson;
        public final Person otherPerson;

        private Composition() {
            onePerson   = Person.create(0, null, null);
            otherPerson = Person.create(0, null, null);
        }

        public static Composition create(Person onePerson, Person otherPerson) {
            Composition comp = Composition.default;
            comp = __WithField(comp.onePerson, onePerson);
            comp = __WithField(comp.otherPerson, otherPerson);
            return comp;
        }
    }

    /**
     * Check inline type operations with "Valhalla Inline Types" (VVT)
     */
    public static void testValues() {
        // Exercise creation, getfield, vreturn with null refs
        validateDefaultPerson(createDefaultPerson());

        // anewarray, aaload, aastore
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

    public static Object[] getOopMap() {
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
     * Just some check sanity checks with defaultvalue, withfield, astore and aload
     *
     * Changes to javac slot usage may well break this test
     */
    public static void testFrameOopsVBytecodes() {
        int nofOopMaps = 4;
        Object[][] oopMaps = new Object[nofOopMaps][];
        String[] inputArgs = new String[] { "aName", "aDescription", "someNotes" };

        FooValue.testFrameOopsDefault(oopMaps);

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

        // Test-R0 Slots=RR Stack=Q(RRR)RV
        assertTrue(oopMaps[0].length == 6 &&
                oopMaps[0][2] == name &&
                oopMaps[0][3] == desc &&
                oopMaps[0][4] == note, "Test-R0 incorrect");

        /**
         * TODO: vwithfield from method handle cooked from anonymous class within the inline class
         *       even with "MethodHandles.privateLookupIn()" will fail final putfield rules
         */
    }

    /**
     * Check forcing GC for combination of VT on stack/LVT etc works
     */
    public static void testOverGc() {
        try {
            Class<?> vtClass = Person.class;

            System.out.println("vtClass="+vtClass);

            doGc();

            // VT on stack and lvt, null refs, see if GC flies
            MethodHandle moveValueThroughStackAndLvt = InstructionHelper.loadCode(
                    LOOKUP,
                    "gcOverPerson",
                    MethodType.methodType(vtClass, vtClass),
                    CODE->{
                        CODE
                        .aload(0)
                        .invokestatic(InlineOops.class, "doGc", "()V", false) // Stack
                        .astore(0)
                        .invokestatic(InlineOops.class, "doGc", "()V", false) // LVT
                        .aload(0)
                        .astore(1024) // LVT wide index
                        .aload(1024)
                        .iconst_1()  // push a litte further down
                        .invokestatic(InlineOops.class, "doGc", "()V", false) // Stack,LVT
                        .pop()
                        .areturn();
                    });
            Person person = (Person) moveValueThroughStackAndLvt.invokeExact(createDefaultPerson());
            validateDefaultPerson(person);
            doGc();

            int index = 4711;
            person = (Person) moveValueThroughStackAndLvt.invokeExact(createIndexedPerson(index));
            validateIndexedPerson(person, index);
            doGc();
            person = createDefaultPerson();
            doGc();
        }
        catch (Throwable t) { fail("testOverGc", t); }
    }

    static void submitNewWork(ForkJoinPool fjPool, int size) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < 100; j++) {
                fjPool.execute(InlineOops::testValues);
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

            Object medLivedObjects = createLongLived();
            Object medLivedPeople = createPeople();

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

                        medLivedObjects = createLongLived();
                        medLivedPeople = createPeople();
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
            medLivedObjects = null;
            medLivedPeople = null;

            validateLongLived(longLivedObjects);
            validatePeople(longLivedPeople);

            longLivedObjects = null;
            longLivedPeople = null;

            doGc();
        }
        catch (Throwable t) { fail("testActiveGc", t); }
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
        Object[] population = new Object[1];
        population[0] = createPeople();
        return population;
    }

    static void validateLongLived(Object pop) throws Throwable {
        Object[] population = (Object[]) pop;
        validatePeople(population[0]);
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

    static final primitive class ObjectValue {
        final Object object;

        private ObjectValue(Object obj) {
            object = obj;
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

    public static final primitive class FooValue {
        public final int id;
        public final String name;
        public final String description;
        public final long timestamp;
        public final String notes;

        private FooValue() {
            id          = 0;
            name        = null;
            description = null;
            timestamp   = 0L;
            notes       = null;
        }

        public static FooValue create(int id, String name, String description, long timestamp, String notes) {
            FooValue f = FooValue.default;
            f = __WithField(f.id, id);
            f = __WithField(f.name, name);
            f = __WithField(f.description, description);
            f = __WithField(f.timestamp, timestamp);
            f = __WithField(f.notes, notes);
            return f;
        }

        public static void testFrameOopsDefault(Object[][] oopMaps) {
            MethodType mt = MethodType.methodType(Void.TYPE, oopMaps.getClass());
            int oopMapsSlot   = 0;
            int vtSlot        = 1;

            // Slots 1=oopMaps
            // OopMap Q=RRR (.name .description .someNotes)
            try {
                InstructionHelper.loadCode(
                        LOOKUP, "exerciseVBytecodeExprStackWithDefault", mt,
                        CODE->{
                            CODE
                            .defaultvalue(FooValue.class)
                            .aload(oopMapsSlot)
                            .iconst_0()  // Test-D0 Slots=R Stack=Q(RRR)RV
                            .invokestatic(InlineOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                            .aastore()
                            .pop()
                            .aload(oopMapsSlot)
                            .iconst_1()  // Test-D1 Slots=R Stack=RV
                            .invokestatic(InlineOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                            .aastore()
                            .defaultvalue(FooValue.class)
                            .astore(vtSlot)
                            .aload(oopMapsSlot)
                            .iconst_2()  // Test-D2 Slots=RQ(RRR) Stack=RV
                            .invokestatic(InlineOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                            .aastore()
                            .aload(vtSlot)
                            .aconst_null()
                            .astore(vtSlot) // Storing null over the Q slot won't remove the ref, but should be single null ref
                            .aload(oopMapsSlot)
                            .iconst_3()  // Test-D3 Slots=R Stack=Q(RRR)RV
                            .invokestatic(InlineOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                            .aastore()
                            .pop()
                            .return_();
                        }).invoke(oopMaps);
            } catch (Throwable t) { fail("exerciseVBytecodeExprStackWithDefault", t); }
        }

        public static void testFrameOopsRefs(String name, String description, String notes, Object[][] oopMaps) {
            FooValue f = create(4711, name, description, 9876543231L, notes);
            FooValue[] fa = new FooValue[] { f };
            MethodType mt = MethodType.methodType(Void.TYPE, fa.getClass(), oopMaps.getClass());
            int fooArraySlot  = 0;
            int oopMapsSlot   = 1;
            try {
                InstructionHelper.loadCode(LOOKUP, "exerciseVBytecodeExprStackWithRefs", mt,
                        CODE->{
                            CODE
                            .aload(fooArraySlot)
                            .iconst_0()
                            .aaload()
                            .aload(oopMapsSlot)
                            .iconst_0()  // Test-R0 Slots=RR Stack=Q(RRR)RV
                            .invokestatic(InlineOops.class, GET_OOP_MAP_NAME, GET_OOP_MAP_DESC, false)
                            .aastore()
                            .pop()
                            .return_();
                        }).invoke(fa, oopMaps);
            } catch (Throwable t) { fail("exerciseVBytecodeExprStackWithRefs", t); }
        }
    }

    static class BarWithValue {
        FooValue foo;
        long extendedId;
        String moreNotes;
        int count;
        String otherStuff;
    }

    static final primitive class BarValue {
        final FooValue foo;
        final long extendedId;
        final String moreNotes;
        final int count;
        final String otherStuff;

        private BarValue(FooValue f, long extId, String mNotes, int c, String other) {
            foo = f;
            extendedId = extId;
            moreNotes = mNotes;
            count = c;
            otherStuff = other;
        }
    }

}

