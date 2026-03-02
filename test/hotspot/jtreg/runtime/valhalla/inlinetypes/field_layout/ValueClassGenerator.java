/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes.field_layout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class ValueClassGenerator {

    final int NUM_PREDEFINED_VALUES;
    final Random random;
    ArrayList<PrimitiveDesc> primitiveTypes = new ArrayList<>();
    ArrayList<ValueClassDesc> valueTypes = new ArrayList<>();
    ArrayList<TypeDesc> referenceTypes = new ArrayList<>();
    static String classTemplate;

    static {
        try {
            String path = System.getProperty("test.src");
            classTemplate = Files.readString(Path.of(path + File.separator + "TestValueTemplate.java.template"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public ArrayList<String> getValueClassesNames() {
        ArrayList<String> res  = new ArrayList<>(valueTypes.size());
        for (ValueClassDesc cd : valueTypes) {
            res.add(cd.typeName);
        }
        return res;
    }

    public static abstract class TypeDesc {
        String typeName;
        boolean allowDuplicates;

        public TypeDesc(String name, boolean duplicates) {
            typeName = name;
            allowDuplicates = duplicates;
        }

        public abstract String getPrecomputedValueAsString(int i);
        public abstract boolean isPrimitiveType();
        public boolean allowDuplicates() { return allowDuplicates; }
    }

    public static class PrimitiveDesc extends TypeDesc {
        String[] precomputedValues;

        public PrimitiveDesc(String name, boolean allowDuplicates, String[] values) {
            super(name, allowDuplicates);
            precomputedValues = values;
        }

        public String getPrecomputedValueAsString(int i) {
            return precomputedValues[i];
        }
        public boolean isPrimitiveType() { return true; }
    }

    void generatePrimitiveTypes() {
        // Generating boolean values
        String[] boolVals = new String[NUM_PREDEFINED_VALUES];
        boolVals[0] = "true";
        boolVals[1] = "false";
        for (int i = 2; i < NUM_PREDEFINED_VALUES; i++) {
            boolVals[i] = Boolean.toString(random.nextBoolean());
        }
        primitiveTypes.add(new PrimitiveDesc("boolean", true, boolVals));

        // Generating byte values
        String[] byteVals = new String[NUM_PREDEFINED_VALUES];
        byteVals[0] = "(byte)0";
        byteVals[1] = "(byte)-1";
        byteVals[2] = "(byte)1";
        byteVals[3] = "Byte.MAX_VALUE";
        byteVals[4] = "Byte.MIN_VALUE";
        for (int i = 5; i < NUM_PREDEFINED_VALUES; i++) {
            byteVals[i] = "(byte)"+Byte.toString((byte)random.nextInt());
        }
        primitiveTypes.add(new PrimitiveDesc("byte", true, byteVals));

        // Generating char values
        String[] charVals = new String[NUM_PREDEFINED_VALUES];
        charVals[0] = "(char)1";
        charVals[1] = "Character.MAX_VALUE";
        charVals[2] = "Character.MIN_VALUE";
        for (int i = 3; i < NUM_PREDEFINED_VALUES; i++) {
            boolean foundNewVal = false;
            String s = null;
            while (!foundNewVal) {
              char c = (char)random.nextInt();
              if (c == Character.MAX_VALUE || c == Character.MIN_VALUE) continue;
              s = "(char)"+Integer.toString(c);
              boolean alreadyExist = false;
              for (int j = 0; j < i; j++) {
                if (s.compareTo(charVals[j]) == 0) {
                  alreadyExist = true;
                }
              }
              foundNewVal = !alreadyExist;
            }
            charVals[i] = s;
        }
        primitiveTypes.add(new PrimitiveDesc("char", false, charVals));

        // Generating short values
        String[] shortVals = new String[NUM_PREDEFINED_VALUES];
        shortVals[0] = "(short)0";
        shortVals[1] = "(short)-1";
        shortVals[2] = "(short)1";
        shortVals[3] = "Short.MAX_VALUE";
        shortVals[4] = "Short.MIN_VALUE";
        for (int i = 5; i < NUM_PREDEFINED_VALUES; i++) {
            boolean foundNewVal = false;
            String s = null;
            while (!foundNewVal) {
              short v = (short)random.nextInt();
              if (v == Short.MAX_VALUE || v == Short.MIN_VALUE) continue;
              s = "(short)"+Integer.toString(v);
              boolean alreadyExist = false;
              for (int j = 0; j < i; j++) {
                if (s.compareTo(shortVals[j]) == 0) {
                  alreadyExist = true;
                }
              }
              foundNewVal = !alreadyExist;
            }
            shortVals[i] = s;
        }
        primitiveTypes.add(new PrimitiveDesc("short", false, shortVals));

        // Generating int values
        String[] intVals = new String[NUM_PREDEFINED_VALUES];
        intVals[0] = "0";
        intVals[1] = "-1";
        intVals[2] = "1";
        intVals[3] = "Integer.MAX_VALUE";
        intVals[4] = "Integer.MIN_VALUE";
        for (int i = 5; i < NUM_PREDEFINED_VALUES; i++) {
            boolean foundNewVal = false;
            String s = null;
            while (!foundNewVal) {
              int v = random.nextInt();
              if (v == Integer.MAX_VALUE || v == Integer.MIN_VALUE) continue;
              s = Integer.toString(v);
              boolean alreadyExist = false;
              for (int j = 0; j < i; j++) {
                if (s.compareTo(intVals[j]) == 0) {
                  alreadyExist = true;
                }
              }
              foundNewVal = !alreadyExist;
            }
            intVals[i] = s;
        }
        primitiveTypes.add(new PrimitiveDesc("int", false, intVals));

        // Generating long values
        String[] longVals = new String[NUM_PREDEFINED_VALUES];
        longVals[0] = "0L";
        longVals[1] = "-1L";
        longVals[2] = "1L";
        longVals[3] = "Long.MAX_VALUE";
        longVals[4] = "Long.MIN_VALUE";
        for (int i = 5; i < NUM_PREDEFINED_VALUES; i++) {
            boolean foundNewVal = false;
            String s = null;
            while (!foundNewVal) {
              long v = random.nextLong();
              if (v == Long.MAX_VALUE || v == Long.MIN_VALUE) continue;
              s = Long.toString(v)+"L";
              boolean alreadyExist = false;
              for (int j = 0; j < i; j++) {
                if (s.compareTo(longVals[j]) == 0) {
                  alreadyExist = true;
                }
              }
              foundNewVal = !alreadyExist;
            }
            longVals[i] = s;
        }
        primitiveTypes.add(new PrimitiveDesc("long", false, longVals));

        // Generating float values
        String[] floatVals = new String[NUM_PREDEFINED_VALUES];
        floatVals[0] = "0.0f";
        floatVals[1] = "-1.0f";
        floatVals[2] = "1.0f";
        floatVals[3] = "Float.MAX_VALUE";
        floatVals[4] = "Float.MIN_VALUE";
        floatVals[5] = "Float.MIN_NORMAL";
        floatVals[6] = "Float.NEGATIVE_INFINITY";
        floatVals[7] = "Float.POSITIVE_INFINITY";
        // Should NaN be part of the pre-computed values?
        for (int i = 8; i < NUM_PREDEFINED_VALUES; i++) {
            boolean foundNewVal = false;
            String s = null;
            while (!foundNewVal) {
              float v = random.nextFloat();
              if (v == Float.MAX_VALUE || v == Float.MIN_NORMAL || v == Float.MIN_NORMAL
                  || v == Float.NEGATIVE_INFINITY || v == Float.POSITIVE_INFINITY) continue;
              s = Float.toString(v)+"f";
              boolean alreadyExist = false;
              for (int j = 0; j < i; j++) {
                if (s.compareTo(floatVals[j]) == 0) {
                  alreadyExist = true;
                }
              }
              foundNewVal = !alreadyExist;
            }
            floatVals[i] = s;
        }
        primitiveTypes.add(new PrimitiveDesc("float", false, floatVals));

        // Generating double values
        String[] doubleVals = new String[NUM_PREDEFINED_VALUES];
        doubleVals[0] = "0.0";
        doubleVals[1] = "-1.0";
        doubleVals[2] = "1.0";
        doubleVals[3] = "Double.MAX_VALUE";
        doubleVals[4] = "Double.MIN_VALUE";
        doubleVals[5] = "Double.MIN_NORMAL";
        doubleVals[6] = "Double.NEGATIVE_INFINITY";
        doubleVals[7] = "Double.POSITIVE_INFINITY";
        // Should NaN be part of the pre-computed values?
        for (int i = 8; i < NUM_PREDEFINED_VALUES; i++) {
            boolean foundNewVal = false;
            String s = null;
            while (!foundNewVal) {
              double v = random.nextDouble();
              if (v == Double.MAX_VALUE || v == Double.MIN_NORMAL || v == Double.MIN_NORMAL
                  || v == Double.NEGATIVE_INFINITY || v == Double.POSITIVE_INFINITY) continue;
              s = Double.toString(v);
              boolean alreadyExist = false;
              for (int j = 0; j < i; j++) {
                if (s.compareTo(doubleVals[j]) == 0) {
                  alreadyExist = true;
                }
              }
              foundNewVal = !alreadyExist;
            }
            doubleVals[i] = s;
        }
        primitiveTypes.add(new PrimitiveDesc("double", false, doubleVals));
    }

    void generateStringClass() {
        referenceTypes.add(new StringClassDesc());
    }

    void printPredefinedPrimitiveValues() {
        for (PrimitiveDesc desc : primitiveTypes) {
            System.out.print(desc.typeName + ": ");
            for (String s : desc.precomputedValues) {
                System.out.print(s + " ");
            }
            System.out.println("");
        }
    }

    public static class FieldDesc {
        String name;
        TypeDesc type;
        String initVal;
        // Qualifiers

        FieldDesc(String name, TypeDesc type, String initval) {
            this.name = name;
            this.type = type;
            this.initVal = initval;
        }
    }

    public class StringClassDesc extends TypeDesc {
        StringClassDesc() {
            super("String", false);
        }

        public String getPrecomputedValueAsString(int i) {
            return "\"" + Integer.toString(i) + "\"";
        }

        public boolean isPrimitiveType() { return false; }
    }

    public class ValueClassDesc extends TypeDesc {
        ValueClassDesc superClass;
        ArrayList<FieldDesc> fields;

        public ValueClassDesc(String name, boolean allowDuplicates, ArrayList<FieldDesc> fields) {
            super(name, allowDuplicates);
            this.fields = fields;
            superClass = null; // no inheritance yet
        }

        public String getPrecomputedValueAsString(int i) {
            return typeName+".getPredefinedValue("+i+")";
        }

        public boolean isPrimitiveType() { return false; }

        String generateFieldsDeclarations() {
            StringBuilder sb = new StringBuilder();
            int counter = 0;
            for (FieldDesc fd : fields) {
                sb.append("\t").append(fd.type.typeName).append(" ").append(fd.name).append(";\n");
            }
            return sb.toString();
        }

        String generateConstructorArguments() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fields.size(); i++) {
                FieldDesc fd = fields.get(i);
                sb.append(fd.type.typeName).append(" a").append(i);
                if (i != fields.size() - 1) sb.append(", ");
            }
            return sb.toString();
        }

        String generateFieldInitialization() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fields.size(); i++) {
                FieldDesc fd = fields.get(i);
                sb.append("\t").append(fd.name).append(" = a").append(i).append(";\n");
            }
            return sb.toString();
        }

        String generatePrecomputedValues() {
            String[] rndVal = new String[NUM_PREDEFINED_VALUES];
            int nfields = fields.size();
            StringBuilder sb = new StringBuilder();
            // While null is not strictly speaking part of the value set of the class, inserting
            // it in the list of precomputed values makes the code more streamlined when picking up
            // values later
            sb.append("\tpredefined[0] = null;\n");
            rndVal[0] = "null";
            for (int i = 1; i < NUM_PREDEFINED_VALUES; i++) {
                sb.append("\tpredefined[").append(i).append("] = new ").append(typeName).append("(");
                boolean validNewVal = false;
                while (!validNewVal) {
                  StringBuffer sb2 = new StringBuffer();
                  for (int j = 0; j < nfields; j++) {
                    FieldDesc fd = fields.get(j);
                    String initArg = fd.type.getPrecomputedValueAsString(random.nextInt(NUM_PREDEFINED_VALUES));
                    sb2.append(initArg);
                    if (j != nfields - 1) sb2.append(", ");
                  }
                  rndVal[i] = sb2.toString();
                  if (allowDuplicates()) {
                    validNewVal = true;
                  } else {
                    boolean alreadyUsed = false;
                    for (int k = 0; k < i; k++) {
                      if (rndVal[i].compareTo(rndVal[k]) == 0) alreadyUsed = true;
                    }
                    validNewVal = !alreadyUsed;
                  }
                }
                sb.append(rndVal[i]);
                sb.append(");\n");
            }
            return sb.toString();
        }

        String generateSource() {
            String src = classTemplate.replaceAll("<class_modifiers>", "public value class");
            src = src.replaceAll("<class_name>", typeName);
            src = src.replaceAll("<allow_duplicates>", allowDuplicates() ? "true" : "false");
            src = src.replaceAll("<fields declarations>", generateFieldsDeclarations());
            src = src.replaceAll("<NUM_PREDEFINED>", Integer.toString(NUM_PREDEFINED_VALUES));
            // replace precomputed value generation
            src = src.replaceAll("<predefined_values_generation>", generatePrecomputedValues());
            src = src.replaceAll("<constructor_args>", generateConstructorArguments());
            src = src.replaceAll("<fields_initialization>", generateFieldInitialization());
            return src;
         }
    }

    ValueClassDesc generateValueClass(int n) {
        String name = "ValueClass"+n;
        int nfields = n == 0 ? 0 : randomFieldNumber(); // always create the empty value as Value0
        int nPrimitive = 0;
        int nValues = 0;
        if (nfields != 0) {
          nPrimitive = random.nextInt(nfields+1);
          nValues = nfields - nPrimitive;
        }
        ArrayList<FieldDesc> fields = new ArrayList<>(nfields);
        boolean allowDuplicates = nfields == 0 ? true : false;
        for (int i = 0; i < nPrimitive; i++) {
          String fieldName = "field"+i;
            TypeDesc fieldType = primitiveTypes.get(random.nextInt(primitiveTypes.size()));
            if (fieldType.allowDuplicates) allowDuplicates = true;
            String initval = fieldType.getPrecomputedValueAsString(random.nextInt(NUM_PREDEFINED_VALUES));
            FieldDesc fd = new FieldDesc(fieldName, fieldType, initval);
            fields.add(fd);
        }
        for (int i = nPrimitive; i < nfields; i++) {
            String fieldName = "field"+i;
            TypeDesc fieldType = referenceTypes.get(random.nextInt(referenceTypes.size()));
            if (fieldType.allowDuplicates) allowDuplicates = true;
            String initval = fieldType.getPrecomputedValueAsString(random.nextInt(NUM_PREDEFINED_VALUES));
            FieldDesc fd = new FieldDesc(fieldName, fieldType, initval);
            fields.add(fd);
        }
        ValueClassDesc cd = new ValueClassDesc(name, allowDuplicates, fields);
        return cd;
    }

    void generateValueClasses(int n) {
        for (int i = 0; i < n; i++) {
            ValueClassDesc c = generateValueClass(i);
            valueTypes.add(c);
            referenceTypes.add(c);
        }
    }

    int randomFieldNumber() {
        return (int)Math.round(0.4/Math.exp(-random.nextInt(16)*0.2)+0.6);
    }

    void printStatistics() {
        int nClasses = valueTypes.size();
        System.out.println("Number of value classes generated: " + nClasses);
        final int FIELDS_BUCKETS = 16;
        int maxFields = 0;
        int[] numberOfFields = new int[FIELDS_BUCKETS];
        int[] primOnly = new int[FIELDS_BUCKETS];
        int[] valOnly = new int[FIELDS_BUCKETS];
        int[] mixed = new int[FIELDS_BUCKETS];
        for (ValueClassDesc cd : valueTypes) {
            int n = cd.fields.size();
            if (n > FIELDS_BUCKETS - 1) n = FIELDS_BUCKETS - 1;
            numberOfFields[n]++;
            if (n > maxFields) maxFields = n;
            int nPrim = 0;
            int nVal = 0;
            for (FieldDesc fd : cd.fields) {
              if (fd.type.isPrimitiveType()) {
                nPrim++;
              } else {
                nVal++;
              }
            }
            if (nPrim == 0) primOnly[n]++;
            else if (nVal == 0) valOnly[n]++;
            else mixed[n]++;
        }
        System.out.println("Number of fields distribution:");
        for (int i = 0; i <= maxFields; i++) {
            System.out.print(i + " fields: " + numberOfFields[i] + " (" + (numberOfFields[i]*100/nClasses) + "%)");
            if (numberOfFields[i] != 0) {
              System.out.print(" : primOnly: " + primOnly[i] + " (" + primOnly[i]*100/numberOfFields[i] + "%)");
              System.out.print(" valOnly: " + valOnly[i] + " (" + valOnly[i]*100/numberOfFields[i] + "%)");
              System.out.print(" mixed: " + mixed[i] + " (" + mixed[i]*100/numberOfFields[i] + "%)");
            }
            System.out.println("");
        }
    }

    void writeValueClasses() {
        for (ValueClassDesc cd : valueTypes) {
            String filename = cd.typeName + ".java";
            try (PrintWriter out = new PrintWriter(filename)) {
                out.println(cd.generateSource());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    void compileValueClasses() {
        // File sourceFile = new File("test/Test.java");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // compiler.run(null, null, null, sourceFile.getPath());
        File[] files = new File[valueTypes.size()];
        String path = System.getProperty("java.io.tmpdir");
        for (int i = 0; i < valueTypes.size(); i++) {
          //System.out.println(path + File.separator + valueTypes.get(i).typeName + ".java");
          files[i] = new File(valueTypes.get(i).typeName + ".java");
        }
        ArrayList<String> optionList = new ArrayList<>();
        optionList.addAll(Arrays.asList("-source", "27"));
        optionList.addAll(Arrays.asList("--enable-preview"));
        StandardJavaFileManager sjfm = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects = sjfm.getJavaFileObjects(files);
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, null, optionList, null, fileObjects);
        if (!task.call()) {
            throw new AssertionError("test failed due to a compilation error");
        }
        try {
          sjfm.close();
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }

    }

    public void generateAll() {
        generatePrimitiveTypes();
        generateStringClass();
        generateValueClasses(256);
        printStatistics();
        writeValueClasses();
        compileValueClasses();
    }

    public ValueClassGenerator(long seed, int nPredefined) {
        random = new Random(seed);
        NUM_PREDEFINED_VALUES = nPredefined;
    }

    public static void main(String[] args) {
        long seed = 0;
        String seedString = System.getProperty("CLASS_GENERATION_SEED");
        if (seedString != null) {
            try {
                seed = Long.parseLong(seedString);
            } catch(NumberFormatException e) { }
        }
        if (seed == 0) {
            seed = System.nanoTime();
        }
        System.out.println("Random seed for class generation: " + seed);
        var gen = new ValueClassGenerator(seed, 8);
        gen.generateAll();
    }
}