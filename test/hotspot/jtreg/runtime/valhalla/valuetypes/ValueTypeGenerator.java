/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Type;
import java.util.Random;

public class ValueTypeGenerator {

    static class FieldDescriptor {

        final public String name;
        final public Type   type;
        final public String typeName;

        public FieldDescriptor(String name, Type type) {
            this.name = name;
            this.type = type;
            String s = type.getTypeName();
            if (s.startsWith("class")) {
                s = s.substring(s.lastIndexOf(".")+1);
            }
            this.typeName = s;
        }
    }

    static Type[] typeArray;
    static String[] defaultArray;
    static int NB_TYPES = 9;

    static {
        typeArray = new Type[NB_TYPES];
        typeArray[0] = byte.class;
        typeArray[1] = short.class;
        typeArray[2] = int.class;
        typeArray[3] = long.class;
        typeArray[4] = char.class;
        typeArray[5] = float.class;
        typeArray[6] = double.class;
        typeArray[7] = boolean.class;
        typeArray[8] = Object.class;
    }

    static String defaultValue(Type t) {
        switch(t.getTypeName()) {
        case "byte": return "(byte)123";
        case "short": return "(short)32056";
        case "int": return "483647";
        case "long": return "922337203685477L";
        case "char": return "(char)65456";
        case "float": return "2.71828f";
        case "double": return "3.14159d";
        case "boolean": return "true";
        case "java.lang.Object": return "new String(\"foo\")";
        default:
            throw new RuntimeException();
        }
    }
    static private String generateValueTypeInternal(Random random, String name, int nfields, int typeLimit) {
        // generate the fields
        FieldDescriptor[] fieldDescArray = new FieldDescriptor[nfields];
        for (int i = 0; i < nfields; i++) {
            int idx = random.nextInt(typeLimit);
            String s =  typeArray[idx].getTypeName();
            if (s.contains(".")) {
                s = s.substring(s.lastIndexOf(".")+1);
            }
            String fieldName = s+"Field"+i;
            fieldDescArray[i] = new FieldDescriptor(fieldName, typeArray[idx]);
        }

        String source = generateSource(name, fieldDescArray);
        return source;
    }

    static public String generateValueType(Random random, String name, int nfields) {
        return generateValueTypeInternal(random, name, nfields, NB_TYPES);
    }

    static public String generateValueTypeNoObjectRef(Random random, String name, int nfields) {
        return generateValueTypeInternal(random, name, nfields, NB_TYPES - 1);
    }

    static String fieldsAsArgs(FieldDescriptor[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i].typeName).append(" ").append(fields[i].name);
            if (i != fields.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    static String generateSource(String name, FieldDescriptor[] fields) {
        StringBuilder sb = new StringBuilder();

        // imports
        sb.append("import java.io.PrintStream;\n\n");

        // class declaration
        sb.append("public __ByValue final class ").append(name).append(" {\n");

        // field declarations
        for (FieldDescriptor f : fields) {
            sb.append("\tfinal public ").append(f.typeName).append(" ");
            sb.append(f.name).append(";\n");
        }
        sb.append("\n");

        // private constructor
        sb.append("\tprivate ").append(name).append("() {\n");
        for (int i = 0 ; i < fields.length; i++) {
            sb.append("\t\t").append(fields[i].name).append(" = ").append(defaultValue(fields[i].type)).append(";\n");
        }
        sb.append("\t}\n");
        sb.append("\n");

        // factory
        sb.append("\tstatic public ").append(name).append(" ").append("make").append(name).append("(");
        sb.append(fieldsAsArgs(fields));
        sb.append(") {\n");
        sb.append("\t\t").append(name).append(" v = ").append("__MakeDefault ").append(name).append("();\n");
        for (int i = 0 ; i < fields.length; i++) {
            sb.append("\t\tv = __WithField(v.").append(fields[i].name).append(", ").append(fields[i].name).append(");\n");
        }
        sb.append("\t\treturn v;\n");
        sb.append("\t};\n");
        sb.append("\n");

        // default factory
        sb.append("\tstatic public ").append(name).append(" ").append("make").append(name).append("() {\n");
        sb.append("\t\t").append(name).append(" v = ").append("__MakeDefault ").append(name).append("();\n");
        for (int i = 0 ; i < fields.length; i++) {
            sb.append("\t\tv = __WithField(v.").append(fields[i].name).append(", ").append(defaultValue(fields[i].type)).append(");\n");
        }
        sb.append("\t\treturn v;\n");
        sb.append("\t}\n");
        sb.append("\n");

        // verify method
        sb.append("\tstatic public boolean verify(").append(name).append(" value) {\n");
        for (FieldDescriptor f : fields) {
            if (f.type.getTypeName().compareTo("java.lang.Object") == 0) {
                sb.append("\t\tif (((String)value.").append(f.name).append(").compareTo(").append(defaultValue(f.type)).append(") != 0) return false;\n");
            } else {
                sb.append("\t\tif (value.").append(f.name).append(" != ").append(defaultValue(f.type)).append(") return false;\n");
            }
        }
        sb.append("\t\treturn true;\n");
        sb.append("\t}\n");

        // printLayout method
        sb.append("\tstatic public void printLayout(PrintStream out) {\n");
        sb.append("\t\tout.println(\"").append(name).append(" fields: ");
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i].typeName);
            if (i != fields.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("\");\n");
        sb.append("\t}\n");

        sb.append("}\n");

        return sb.toString();
    }
}
