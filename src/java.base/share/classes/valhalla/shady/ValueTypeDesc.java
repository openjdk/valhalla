
/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package valhalla.shady;

public class ValueTypeDesc {

    public static class Field {
        public String name;
        public String type;
        public int    modifiers;

        public Field(String name, String type, int modifiers) {
            this.name = name;
            this.type = type;
            this.modifiers = modifiers;
        }

        public String toString() {
            return "Field " + name + ":" + type +
                " mod=0x" + Integer.toHexString(modifiers);
        }
    }

    String name;
    Field[] fields;

    public ValueTypeDesc(String name, String[] fds, int[] fmods) {
        this.name = name;
        this.fields = new Field[fds.length / 2];
        for (int i = 0; i < fds.length; i += 2) {
            int f = i / 2;
            this.fields[f] = new Field(fds[i], fds[i + 1], fmods[f]);
        }
    }

    public ValueTypeDesc(String name, Field[] fields) {
        this.name   = name;
        this.fields = fields;
    }

    public String getName()          { return name; }
    public Field[] getFields()       { return fields; }

    public String getConstructorDesc() {
        String desc = "(";
        for (Field field : getFields()) {
            desc += field.type;
        }
        return desc + ")V";
    }

    public String toString() {
        String s = getName() + "\n";
        for (Field f : fields) {
            s += "\t" + f.toString() + "\n";
        }
        return s;
    }
}
