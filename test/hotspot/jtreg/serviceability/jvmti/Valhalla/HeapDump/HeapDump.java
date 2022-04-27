/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @library /test/lib
 * @requires vm.jvmti
 * @run main HeapDump
 */

import java.io.File;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.hprof.model.HackJavaValue;
import jdk.test.lib.hprof.model.InlinedJavaObject;
import jdk.test.lib.hprof.model.JavaByte;
import jdk.test.lib.hprof.model.JavaClass;
import jdk.test.lib.hprof.model.JavaField;
import jdk.test.lib.hprof.model.JavaHeapObject;
import jdk.test.lib.hprof.model.JavaObject;
import jdk.test.lib.hprof.model.JavaObjectArray;
import jdk.test.lib.hprof.model.JavaStatic;
import jdk.test.lib.hprof.model.JavaThing;
import jdk.test.lib.hprof.model.JavaValueArray;
import jdk.test.lib.hprof.model.Snapshot;
import jdk.test.lib.process.ProcessTools;

import java.util.Enumeration;

import jdk.test.lib.hprof.parser.Reader;


class TestClass {

    public static primitive class MyPrimitive0 {
        public byte prim0_fld1;
        public MyPrimitive0(int fld1) { prim0_fld1 = (byte)fld1; }
    }
    public static primitive class MyPrimitive {
        public MyPrimitive0 prim_prim0;
        public byte prim_fld1;
        public int prim_fld2;

        public MyPrimitive(int p0, int fld1, int fld2) {
            prim_fld1 = (byte)fld1; prim_fld2 = fld2; prim_prim0 = new MyPrimitive0(p0);
        }
    }

    public static primitive class PrimitiveHolder {
        // offset of the inlined hld_flatObj is the same as offset of inlined PrimitiveHolder
        public MyPrimitive hld_flatObj;

        public PrimitiveHolder(int n) {
            hld_flatObj = new MyPrimitive((byte)n, n+1, n+2);
        }
    }

    // primitive class with reference
    public static primitive class MyPrimitiveRef {
        public byte ref_fld1;
        public int ref_fld2;
        public String ref_strObj;

        public MyPrimitiveRef(int v1, int v2) {
            ref_fld1 = (byte)v1;
            ref_fld2 = v2;
            ref_strObj = "#" + String.valueOf(v2); }
    }

    public static primitive class PrimitiveHolderRef {
        public MyPrimitiveRef[] flatArr = new MyPrimitiveRef[5];
        public MyPrimitiveRef flatObj;
        public String ref_str;
        public PrimitiveHolderRef(int n) {
            ref_str = String.valueOf(n);
            flatObj = new MyPrimitiveRef(n, n + 10);
            for (int i = 0; i < flatArr.length; i++) {
                flatArr[i] = new MyPrimitiveRef(i + n + 1, i + n + 11);
            }
        }
    }

    public MyPrimitive[] main_flatArr = new MyPrimitive[3];
    public MyPrimitive main_flatObj = new MyPrimitive(10, 15, 9);
    public MyPrimitiveRef main_flatObjRef = new MyPrimitiveRef(11, 144);
    public MyPrimitiveRef[] flatArrRef = new MyPrimitiveRef[4];
    public String main_strObj = "targ.strObj";

    public Object main_nullObj;

    public final PrimitiveHolder main_primHolder = new PrimitiveHolder(16);
    // array of compound primitive objects
    public final PrimitiveHolderRef[] primHolderArr = new PrimitiveHolderRef[4];


    // static inlined fields
    public static MyPrimitive main_flatObjStatic = new MyPrimitive(13, 241, 24);
    public static MyPrimitiveRef[] flatArrRefStatic = new MyPrimitiveRef[6];
    static {
        for (int i = 0; i < flatArrRefStatic.length; i++) {
            flatArrRefStatic[i] = new MyPrimitiveRef(i + 200, i + 225);
        }
    }

    public TestClass() {
        for (int i = 0; i < main_flatArr.length; i++) {
            main_flatArr[i] = new MyPrimitive(i + 10, i + 110, i + 35);
        }
        for (int i = 0; i < flatArrRef.length; i++) {
            flatArrRef[i] = new MyPrimitiveRef(i + 100, i + 120);
        }
        for (int i = 0; i < primHolderArr.length; i++) {
            primHolderArr[i] = new PrimitiveHolderRef(20 + i);
        }
    }
}

class HeapDumpTarg extends LingeredApp {

    public static void main(String[] args) {
        TestClass testObj = new TestClass();
        LingeredApp.main(args);
        Reference.reachabilityFence(testObj);
    }

}

public class HeapDump {
    public static void main(String[] args) throws Throwable {
        LingeredApp theApp = null;
        String hprogFile = new File(System.getProperty("test.classes") + "/Myheapdump.hprof").getAbsolutePath();
        try {
            theApp = new HeapDumpTarg();

            // -XX:+PrintInlineLayout is debug-only arg
            LingeredApp.startApp(theApp/*, "-XX:+PrintInlineLayout"*/);

            // jcmd <pid> GC.heap_dump
            JDKToolLauncher launcher = JDKToolLauncher
                                        .createUsingTestJDK("jcmd")
                                        .addToolArg(Long.toString(theApp.getPid()))
                                        .addToolArg("GC.heap_dump")
                                        .addToolArg(hprogFile);
            Process jcmd = ProcessTools.startProcess("jcmd", new ProcessBuilder(launcher.getCommand()));
            // If something goes wrong with heap dumping most likely we'll get crash of the target VM
            while (!jcmd.waitFor(5, TimeUnit.SECONDS)) {
                if (!theApp.getProcess().isAlive()) {
                    log("ERROR: target VM died, killing jcmd...");
                    jcmd.destroyForcibly();
                    throw new Exception("Target VM died");
                }
            }

            if (jcmd.exitValue() != 0) {
                throw new Exception("Jcmd exited with code " + jcmd.exitValue());
            }
        } finally {
            LingeredApp.stopApp(theApp);
        }

        // test object to compare
        TestClass testObj = new TestClass();

        log("Reading " + hprogFile + "...");
        try (Snapshot snapshot = Reader.readFile(hprogFile,true, 0)) {
            log("Snapshot read, resolving...");
            snapshot.resolve(true);
            log("Snapshot resolved.");

            JavaObject dumpObj = findObject(snapshot, testObj.getClass().getName());

            log("");
            print(dumpObj);

            log("");
            log("Verifying object " + testObj.getClass().getName() + " (dumped object " + dumpObj + ")");
            compareObjectFields("  ", testObj, dumpObj);
        }
    }

    private static JavaObject findObject(Snapshot snapshot, String className) throws Exception {
        log("looking for " + className + "...");
        JavaClass jClass = snapshot.findClass(className);
        if (jClass == null) {
            throw new Exception("'" + className + "' not found");
        }
        Enumeration<JavaHeapObject> objects = jClass.getInstances(false);
        if (!objects.hasMoreElements()) {
            throw new Exception("No '" + className + "' instances found");
        }
        JavaHeapObject heapObj = objects.nextElement();
        if (objects.hasMoreElements()) {
            throw new Exception("More than 1 instances of '" + className + "' found");
        }
        if (!(heapObj instanceof JavaObject)) {
            throw new Exception("'" + className + "' instance is not JavaObject (" + heapObj.getClass() + ")");
        }
        return (JavaObject)heapObj;
    }

    // description of the current object for logging and error reporting
    private static String objDescr;
    private static boolean errorReported = false;

    private static void compareObjects(String logPrefix, Object testObj, JavaThing dumpObj) throws Exception {
        if (testObj == null) {
            if (!isNullValue(dumpObj)) {
                throw new Exception("null expected, but dumped object is " + dumpObj);
            }
            log(logPrefix + objDescr + ": null");
        } else if (dumpObj instanceof JavaObject obj) {
            // special handling for Strings
            if (testObj instanceof String testStr) {
                objDescr += " (String, " + obj.getIdString() + ")";
                if (!obj.getClazz().isString()) {
                    throw new Exception("value (" + obj + ")"
                            + " is not String (" + obj.getClazz() + ")");
                }
                String dumpStr = getStringValue(obj);
                if (!testStr.equals(dumpStr)) {
                    throw new Exception("different values:"
                            + " expected \"" + testStr + "\", actual \"" + dumpStr + "\"");
                }
                log(logPrefix + objDescr + ": \"" + testStr + "\" ( == \"" + dumpStr + "\")");
            } else {
                // other Object
                log(logPrefix + objDescr + ": Object " + obj);
                if (isTestClass(obj.getClazz().getName())) {
                    compareObjectFields(logPrefix + "  ", testObj, obj);
                }
            }
        } else {
            throw new Exception("Object expected, but the value (" + dumpObj + ")"
                    + " is not JavaObject (" + dumpObj.getClass() + ")");
        }
    }

    private static void compareObjectFields(String logPrefix, Object testObj, JavaObject dumpObj) throws Exception {
        Field[] fields = testObj.getClass().getDeclaredFields();
        for (Field testField : fields) {
            boolean isStatic = Modifier.isStatic(testField.getModifiers());
            testField.setAccessible(true);
            objDescr = "- " + (isStatic ? "(static) " : "")
                    + (testField.getType().isPrimitiveClass() ? "(primitive) " : "")
                    + testField.getName() + " ('" + testField.getType().descriptorString() + "')";
            try {
                Object testValue = testField.get(testObj);

                JavaField dumpField = getField(dumpObj, testField.getName(), isStatic);
                JavaThing dumpValue = isStatic
                        ? dumpObj.getClazz().getStaticField(dumpField.getName())
                        : dumpObj.getField(dumpField.getName());

                objDescr += ", dump signature '" + dumpField.getSignature() + "'";

                compareType(testField, dumpField);

                if (testValue == null) {
                    if (!isNullValue(dumpValue)) {
                        throw new Exception("null expected, but dumped object is " + dumpValue);
                    }
                    log(logPrefix + objDescr + ": null");
                } else {
                    switch (testField.getType().descriptorString().charAt(0)) {
                    case 'L':
                    case 'Q':
                        compareObjects(logPrefix, testValue, dumpValue);
                        break;
                    case '[':
                        int testLength = Array.getLength(testValue);
                        objDescr += " (Array of '" + testField.getType().getComponentType() + "'"
                                + ", length = " + testLength + ", " + dumpValue + ")";
                        if (dumpValue instanceof JavaValueArray arr) {
                            // array of primitive type
                            char testElementType = testField.getType().getComponentType().descriptorString().charAt(0);
                            if ((char)arr.getElementType() != testElementType) {
                                throw new Exception("wrong element type: '" + (char)arr.getElementType() + "'");
                            }
                            int dumpLength = arr.getLength();
                            if (dumpLength != testLength) {
                                throw new Exception("wrong array size: " + dumpLength);
                            }
                            JavaThing[] dumpElements = arr.getElements();
                            log(logPrefix + objDescr);
                            for (int j = 0; j < testLength; j++) {
                                Object elementValue = Array.get(testValue, j);
                                objDescr = "[" + j + "]";
                                if (arr.isFlatArray()) {
                                    compareObjects(logPrefix + "  ", elementValue, dumpElements[j]);
                                } else {
                                    comparePrimitiveValues(elementValue, dumpElements[j]);
                                    log(logPrefix + "  [" + j + "]: " + elementValue + " ( == " + dumpElements[j] + ")");
                                }
                            }
                        } else if (dumpValue instanceof JavaObjectArray arr) {
                            int dumpLength = arr.getLength();
                            if (dumpLength != testLength) {
                                throw new Exception("wrong array size: " + dumpLength);
                            }
                            JavaThing[] dumpElements = arr.getElements();
                            log(logPrefix + objDescr);
                            for (int j = 0; j < testLength; j++) {
                                Object elementValue = Array.get(testValue, j);
                                objDescr = "[" + j + "]";
                                compareObjects(logPrefix + "  ", elementValue, dumpElements[j]);
                            }
                        } else {
                            throw new Exception("Array expected, but the value (" + dumpValue + ")"
                                    + " is neither JavaValueArray nor JavaObjectArray"
                                    + " (" + dumpValue.getClass() + ")");
                        }
                        break;
                    default:
                        comparePrimitiveValues(testValue, dumpValue);
                        log(logPrefix + objDescr + ": " + testValue + " ( == " + dumpValue + ")");
                        break;
                    }
                }
            } catch (Exception ex) {
                if (!errorReported) {
                    log(logPrefix + objDescr + ": ERROR - " + ex.getMessage());
                    errorReported = true;
                }
                throw ex;
            }
        }
    }

    private static JavaField getField(JavaObject obj, String fieldName, boolean isStatic) throws Exception {
        if (isStatic) {
            JavaStatic[] statics = obj.getClazz().getStatics();
            for (JavaStatic st: statics) {
                if (st.getField().getName().equals(fieldName)) {
                    return st.getField();
                }
            }
        } else {
            JavaField[] fields = obj.getClazz().getFields();
            for (JavaField field : fields) {
                if (fieldName.equals(field.getName())) {
                    return field;
                }
            }
        }
        throw new Exception("field '" + fieldName + "' not found");
    }

    private static void compareType(Field field, JavaField dumpField) throws Exception {
        String sig = field.getType().descriptorString();
        char type = sig.charAt(0);
        if (type == '[' || type == 'Q') {
            type = 'L';
        }
        char dumpType = dumpField.getSignature().charAt(0);
        if (dumpType == 'Q') {
            dumpType = 'L';
        }
        if (dumpType != type) {
            throw new Exception("type mismatch:"
                    + " expected '" + type + "' (" + sig + ")"
                    + ", found '" + dumpField.getSignature().charAt(0) + "' (" + dumpField.getSignature() + ")");
        }
    }

    private static void comparePrimitiveValues(Object testValue, JavaThing dumpValue) throws Exception {
        // JavaByte.toString() returns hex
        String testStr = testValue instanceof Byte byteValue
                ? (new JavaByte(byteValue)).toString()
                : String.valueOf(testValue);
        String dumpStr = dumpValue.toString();
        if (!testStr.equals(dumpStr)) {
            throw new Exception("Wrong value: expected " + testStr + ", actual " + dumpStr);
        }
    }

    private static boolean isNullValue(JavaThing value) {
        return value == null
                // dumped value is HackJavaValue with string representation "<null>"
                || (value instanceof HackJavaValue &&  "<null>".equals(value.toString()));
    }

    private static String getStringValue(JavaObject value) {
        JavaThing valueObj = value.getField("value");
        if (valueObj instanceof JavaValueArray valueArr) {
            try {
                if (valueArr.getElementType() == 'B') {
                    Field valueField = JavaByte.class.getDeclaredField("value");
                    valueField.setAccessible(true);
                    JavaThing[] things = valueArr.getElements();
                    byte[] bytes = new byte[things.length];
                    for (int i = 0; i < things.length; i++) {
                        bytes[i] = valueField.getByte(things[i]);
                    }
                    return new String(bytes);
                }
            } catch (Exception ignored) {
            }
            return valueArr.valueString();
        } else {
            return null;
        }
    }

    private static void print(JavaObject dumpObject) {
        log("Dumped object " + dumpObject + ":");
        print("", dumpObject);
    }

    private static void print(String prefix, JavaObject dumpObject) {
        JavaClass clazz = dumpObject.getClazz();
        // print only test classes
        if (!isTestClass(clazz.getName())) {
            return;
        }

        JavaField[] fields = clazz.getFields();
        for (JavaField field : fields) {
            printFieldValue(prefix, field, false, dumpObject.getField(field.getName()));
        }

        JavaStatic[] statics = clazz.getStatics();
        for (JavaStatic st: statics) {
            printFieldValue(prefix, st.getField(), true, st.getValue());
        }
    }

    private static void printFieldValue(String prefix, JavaField field, boolean isStatic, JavaThing value) {
        String logPrefix = prefix + "- " + (isStatic ? "(static) " : "")
                + field.getName() + " ('" + field.getSignature() + "'): ";
        if (isNullValue(value)) {
            log(logPrefix + "null");
        } else {
            if (value instanceof JavaObject obj) {
                logPrefix += "(class '" + obj.getClazz().getName() + "'): ";
                if (obj.getClazz().isString()) {
                    String dumpStr = getStringValue(obj);
                    log(logPrefix + "\"" + dumpStr + "\"");
                } else {
                    log(logPrefix + (value instanceof InlinedJavaObject ? "inlined " : "") + "object " + obj);
                    print(prefix + "  ", obj);
                }
            } else if (value instanceof JavaObjectArray arr) {
                log(logPrefix + " array " + arr + " length: " + arr.getLength());
                JavaThing[] values = arr.getValues();
                for (int v = 0; v < values.length; v++) {
                    log(prefix + "  [" + v + "]: " + values[v]);
                    if (values[v] instanceof JavaObject obj) {
                        print(prefix + "    ", obj);
                    }
                }
            } else if (value instanceof JavaValueArray arr) { // array of primitive type or flat array
                if (arr.isFlatArray()) {
                    log(logPrefix + " flat array " + arr + " length: " + arr.getLength());
                    JavaThing[] values = arr.getElements();
                    for (int v = 0; v < values.length; v++) {
                        log(prefix + "  [" + v + "]: " + values[v]);
                        if (values[v] instanceof JavaObject obj) {
                            print(prefix + "    ", obj);
                        }
                    }
                } else {
                    log(logPrefix + "(array of '" + (char)arr.getElementType() + "')" + ": " + arr.valueString());
                }
            } else {
                log(logPrefix + "(" + value.getClass() +  ")" + value.toString());
            }
        }
    }

    private static boolean isTestClass(String className) {
        return className.startsWith("TestClass");
    }

    private static void log(String msg) {
        System.out.println(msg);
        System.out.flush();
    }

}
