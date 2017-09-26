/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import javax.tools.JavaFileObject;

import jdk.test.lib.combo.ComboInstance;
import jdk.test.lib.combo.ComboParameter;
import jdk.test.lib.combo.ComboTask.Result;
import jdk.test.lib.combo.ComboTestHelper;
import jdk.test.lib.combo.ComboTestHelper.ArrayDimensionKind;
import jdk.incubator.mvt.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodType.methodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

/**
 * Test combinations of value type field layouts.
 *
 * Testing all permutations of all 8 primitive types and a reference type is
 * prohibitive both in terms of resource usage on testing infrastructure, and
 * development time. Sanity or "check-in" level testing should be in the order
 * of seconds in terms of wall-clock execution time.
 *
 * ### Combinations vs Permutations
 *
 * For a given number of fields, or "set of cardinality 'K'" ("arity" in code
 * here), of a set of "N" types ("BasicType"), the number of test cases can be
 * expressed as:
 *
 * Combinations: "(N + K - 1) ! / K ! (N - 1)!", for K=4, N=9: test cases =  496
 * Permutations: "N ^ K",                        for K=4, N=9: test cases = 6561
 *
 * Given the knowledge that the VM always reorders field declarations to suit
 * the given hardware, order of fields doesn't actually matter. I.e. for
 * N={int, long}, useful test cases are:
 *
 *  Test-0: {int , int}
 *  Test-1: {int , long}
 *  Test-2: {long, long}
 *
 * Where as {long, int} is unnecessary given "Test-1".
 *
 * # TLDR; Combinations give considerable savings.
 *
 * ### Maintain the ability to repoduce single test case
 *
 * Given the large number of test cases, ensure this class is always capable of
 * reporting the specific test case when something goes wrong, and allow running
 * of that single test case to enable efficent debugging.
 *
 * Note: upon crash the generated test class should be present in $CWD/Test.class
 */
public class MVTCombo extends ComboInstance<MVTCombo> {

    // Set of fields types to test
    enum BasicType implements ComboParameter {
        BOOLEAN(Boolean.TYPE),
        BYTE(Byte.TYPE),
        CHAR(Character.TYPE),
        SHORT(Short.TYPE),
        FLOAT(Float.TYPE),
        DOUBLE(Double.TYPE),
        INT(Integer.TYPE),
        LONG(Long.TYPE),
        STRING(String.class);

        // Reduced set of 'N' types for large number of fields ('K')
        public static final BasicType[] REDUCED_SET = new BasicType[] {
            INT,    // Single slot
            DOUBLE, // Double slot FP
            STRING  // Reference
        };

        Class<?> clazz;

        BasicType(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public String expand(String optParameter) {
            if (optParameter == null) {
                return clazz.getName();
            } else if (optParameter.startsWith("FROM_INT_")) {
                String varName = optParameter.substring(9);
                switch (this) {
                case BOOLEAN:
                    return varName + " == 0 ? false : true";
                case STRING:
                    return "String.valueOf(" + varName + ")";
                default:
                    return "(" + clazz.getName() + ")" + varName;
                }
            } else if (optParameter.startsWith("EQUALS_")) {
                String varName = "f_" + optParameter.substring(7);
                switch (this) {
                case STRING:
                    return "this." + varName + ".equals(that." + varName + ")";
                default:
                    return "this." + varName + " == that." + varName;
                }
            } else throw new IllegalStateException("optParameter=" + optParameter);
        }
    }

    // Nof fields to test
    static class Arity implements ComboParameter {

        int arity;
        int maxArity;

        Arity(int arity, int maxArity) {
            this.arity = arity;
            this.maxArity = maxArity;
        }

        @Override
        public String expand(String optParameter) {
            for (Snippet s : Snippet.values()) {
                if (s.name().equals(optParameter)) {
                    return s.expand(arity);
                }
            }
            throw new IllegalStateException("Cannot get here!");
        }

        public String toString() { return "Arity " + arity + "/" + maxArity; }

        // Produce 1..K arity
        public static Arity[] values(int maxArity) {
            Arity[] vals = new Arity[maxArity];
            for (int i = 0; i < maxArity; i++) {
                vals[i] = new Arity(i + 1, maxArity);
            }
            return vals;
        }
    }

    enum Snippet {
        FIELD_DECL("public final #{TYPE[#IDX]} f_#IDX;", "\n    "),
        FIELD_ASSIGN("this.f_#IDX = f_#IDX;", "\n        "),
        FIELD_EQUALS("if (!(#{TYPE[#IDX].EQUALS_#IDX})) return false;", "\n        "),
        CONSTR_FORMALS("#{TYPE[#IDX]} f_#IDX", ","),
        CONSTR_ACTUALS("f_#IDX", ","),
        CONSTR_ACTUALS_INDEXED("#{TYPE[#IDX].FROM_INT_INDEX}", ",");

        String snippetStr;
        String sep;

        Snippet(String snippetStr, String sep) {
            this.snippetStr = snippetStr;
            this.sep = sep;
        }

        String expand(int arity) {
            StringBuilder buf = new StringBuilder();
            String tempSep = "";
            for (int i = 0 ; i < arity ; i++) {
                buf.append(tempSep);
                buf.append(snippetStr.replaceAll("#IDX", String.valueOf(i)));
                tempSep = sep;
            }
            return buf.toString();
        }
    }

    public static final String VCC_TEMPLATE =
        "@jdk.incubator.mvt.ValueCapableClass\n" +
        "public final class Test {\n\n" +
        "    // Declare fields...\n" +
        "    #{ARITY.FIELD_DECL}\n" +
        "    // Private Constructor...\n" +
        "    private Test(#{ARITY.CONSTR_FORMALS}) {\n" +
        "        #{ARITY.FIELD_ASSIGN}\n" +
        "    }\n" +
        "    public boolean equals(Object o) {\n" +
        "        Test that = (Test) o;\n" +
        "        #{ARITY.FIELD_EQUALS}\n" +
        "        return true;\n" +
        "    }\n" +
        "    // Public factory method\n" +
        "    public static Test create(#{ARITY.CONSTR_FORMALS}) {\n" +
        "        return new Test(#{ARITY.CONSTR_ACTUALS});\n" +
        "    }\n" +
        "    // Public indexed test case factory method\n" +
        "    public static Test createIndexed(int INDEX) {\n" +
        "        return new Test(#{ARITY.CONSTR_ACTUALS_INDEXED});\n" +
        "    }\n" +
        "}\n";

    public static void runTests(boolean reduceTypes, int nofFields, int specificTestCase) throws Exception {
        ComboTestHelper<MVTCombo> test = new ComboTestHelper<MVTCombo>()
            .withDimension("ARITY", (x, expr) -> x.setArity(expr), Arity.values(nofFields))
            .withArrayDimension("TYPE",
                                (x, t, idx) -> x.basicTypes[idx] = t,
                                nofFields,
                                ArrayDimensionKind.COMBINATIONS,
                                reduceTypes ? BasicType.REDUCED_SET : BasicType.values());
        if (specificTestCase == -1) {
            test.withFilter(MVTCombo::redundantFilter);
        } else {
            test.withFilter((x)->test.info().getComboCount() == specificTestCase);
        }
        test.run(MVTCombo::new);
    }

    public static final String ARG_REDUCE_TYPES = "-reducetypes";

    // main "-reducetypes <nofFields> <specific-test-number>"
    public static void main(String... args) throws Exception {
        // Default args
        boolean reduceTypes  = false;
        int nofFields        = 4;
        int specificTestCase = -1;

        // Parse
        int argIndex = 0;
        if (args.length > argIndex && (args[argIndex].equals(ARG_REDUCE_TYPES)))  {
            reduceTypes = true;
            argIndex++;
        }
        if (args.length > argIndex) {
            nofFields = Integer.parseInt(args[argIndex]);
            argIndex++;
        }
        if (args.length > argIndex) {
            specificTestCase = Integer.parseInt(args[argIndex]);
            argIndex++;
        }

        runTests(reduceTypes, nofFields, specificTestCase);
    }

    Arity arity;
    BasicType[] basicTypes;

    public String toString() {
        String s = "MVTCombo " + arity + " types: ";
        for (int i = 0 ; i < basicTypes.length; i++) {
            s += " " + basicTypes[i];
        }
        return s;
    }

    void setArity(Arity arity) {
        this.arity = arity;
        // Even if we are testing 1..K fields, combo needs K fields
        this.basicTypes = new BasicType[arity.maxArity];
    }

    /*
       The way the 'combo' package works, it produces combinations or permutations
       for each dimension, so for arity we don't care for basicTypes[arity...maxArity]
     */
    boolean redundantFilter() {
        BasicType lastArityType = basicTypes[arity.arity - 1];
        for (int i = arity.arity ; i < arity.maxArity ; i++) {
            if (basicTypes[i] != lastArityType) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void doWork() throws Throwable {
        Result<Iterable<? extends JavaFileObject>> result = newCompilationTask()
                .withSourceFromTemplate(VCC_TEMPLATE)
                .withOption("--add-modules=jdk.incubator.mvt")
                .generate();
        //System.out.println("COMP: " + result.compilationInfo()); // Print the generated source
        if (result.hasErrors()) {
            fail("ERROR " + result.compilationInfo());
        }
        JavaFileObject jfo = result.get().iterator().next(); // Blindly assume one
        String url = jfo.toUri().toURL().toString();
        url = url.substring(0, url.length() - jfo.getName().length());
        Class<?> clazz = new URLClassLoader(new URL[] { new URL(url) }).loadClass("Test");
        try {
            doTestMvtClasses(clazz);
            doTestSingleInstance(clazz);
            doTestArray(clazz);
        } catch (Throwable ex) {
            throw new AssertionError("ERROR: " + result.compilationInfo(), ex);
        }
    }

    protected void doTestMvtClasses(Class<?> testSubject) throws Throwable {
        if (!ValueType.classHasValueType(testSubject)) {
            throw new IllegalArgumentException("Not a VCC: " + testSubject);
        }
        ValueType<?> vt = ValueType.forClass(testSubject);
        Class<?> boxClass    = vt.boxClass();
        Class<?> vtClass     = vt.valueClass();
        Class<?> arrayClass  = vt.arrayValueClass();
        Class<?> mArrayClass = vt.arrayValueClass(4);
        if (boxClass != testSubject) {
            throw new RuntimeException("Box class != VCC");
        }
        if (vt.toString() == null) {
            throw new RuntimeException("No toString() return");
        }
    }

    protected void doTestSingleInstance(Class<?> testSubject) throws Throwable {
        ValueType<?> vt = ValueType.forClass(testSubject);
        Object obj = MethodHandles.filterReturnValue(vt.defaultValueConstant(), vt.box()).invoke();
        obj = MethodHandles.filterReturnValue(vt.unbox(), vt.box()).invoke(obj);
        int hashCode = (int) MethodHandles.filterReturnValue(vt.defaultValueConstant(), vt.substitutabilityHashCode()).invoke();

        //test(default(), default())
        MethodHandle test0 = MethodHandles.collectArguments(vt.substitutabilityTest(), 0, vt.defaultValueConstant());
        boolean isEqual = (boolean) MethodHandles.collectArguments(test0, 0, vt.defaultValueConstant()).invoke();
        if (!isEqual) {
            throw new RuntimeException("test(default(), default()) failed");
        }
    }

    protected void doTestArray(Class<?> testSubject) throws Throwable {
        ValueType<?> vt = ValueType.forClass(testSubject);
        MethodHandle arrayGetter = vt.arrayGetter();
        MethodHandle arraySetter = vt.arraySetter();
        MethodHandle unbox = vt.unbox();
        MethodHandle box = vt.box();
        int testArrayLen = 7;
        Object array = vt.newArray().invoke(testArrayLen);
        for (int i = 0; i < testArrayLen; i++) {
            MethodHandle equalsDefault0 = MethodHandles.collectArguments(vt.substitutabilityTest(), 0, vt.defaultValueConstant());
            boolean isEqual = (boolean) MethodHandles.collectArguments(equalsDefault0, 0, arrayGetter).invoke(array, i);
            if (!isEqual) {
                System.out.println("PROBLEM:");
                printFieldValues(MethodHandles.filterReturnValue(vt.defaultValueConstant(), box));
                System.out.println("VERSUS value from array:");
                printFieldValues(MethodHandles.filterReturnValue(arrayGetter, box).invoke(array, i));
                throw new IllegalStateException("Failed equality test for class: " + vt.boxClass().getName()  + " at index: " + i);
            }
        }

        // populate the last element with some values...
        int testIndex = testArrayLen - 1;
        /*
           Do the following in MHs...

          Object testObj = Test.createIndexed(testIndex);
          array[testIndex] = unbox(testObj);
          if (!testObj.equals(array[testIndex])) throw...
        */
        MethodHandle createIndexed = MethodHandles.privateLookupIn(testSubject, mhLookup)
            .findStatic(testSubject, "createIndexed", methodType(testSubject, Integer.TYPE));
        Object testObj = createIndexed.invoke(testIndex);
        arraySetter.invoke(array, testIndex, testObj);
        Object testElem = MethodHandles.filterReturnValue(arrayGetter, box).invoke(array, testIndex);
        if (!testObj.equals(testElem)) {
            System.out.println("PROBLEM:");
            printFieldValues(testObj);
            System.out.println("VERSUS:");
            printFieldValues(testElem);
            throw new RuntimeException("Inequality after value array store and load");
        }
    }

    // Some general helper methods...
    public static void printFieldValues(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        System.out.println("Object: " +  obj + " class: " + clazz.getName());
        Field[] fields = reflectPublicFinalInstanceFields(clazz);
        for (Field f : fields) {
            System.out.printf("\t%s %s = %s\n", f.getType().getName(), f.getName(), f.get(obj));
        }
    }

    public static Field[] reflectPublicFinalInstanceFields(Class<?> clazz) {
        return reflectInstanceFields(clazz, Modifier.PUBLIC | Modifier.FINAL);
    }

    public static Field[] reflectInstanceFields(Class<?> clazz, int mask) {
        return Stream.of(clazz.getDeclaredFields())
            .filter(f -> (f.getModifiers() & (mask)) == mask)
            .toArray(Field[]::new);
    }

    static final MethodHandles.Lookup mhLookup = MethodHandles.lookup();
}
