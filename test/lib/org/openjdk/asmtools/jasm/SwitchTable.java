/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
class SwitchTable {

    Argument deflabel = null;
    ArrayList<Argument> labels = new ArrayList<>();
    ArrayList<Integer> keys = new ArrayList<>();

// for tableswitch:
    Argument[] resLabels;
    int high, low;

    int pc, pad;
    Environment env;

    SwitchTable(Environment env) {
        this.env = env;
    }

    void addEntry(int key, Argument label) {
        keys.add(key);
        labels.add(label);
    }

// for lookupswitch:
    int calcLookupSwitch(int pc) {
        this.pc = pc;
        pad = ((3 - pc) & 0x3);
        int len = 1 + pad + (keys.size() + 1) * 8;
        if (deflabel == null) {
            deflabel = new Argument(pc + len);
        }
        return len;
    }

    void writeLookupSwitch(CheckedDataOutputStream out) throws IOException {
        env.traceln("  writeLookupSwitch: pc=" + pc + " pad=" + pad + " deflabel=" + deflabel.arg);
        int k;
        for (k = 0; k < pad; k++) {
            out.writeByte(0);
        }
        out.writeInt(deflabel.arg - pc);
        out.writeInt(keys.size());
        for (k = 0; k < keys.size(); k++) {
            out.writeInt(keys.get(k));
            out.writeInt((labels.get(k)).arg - pc);
        }
    }

    int recalcTableSwitch(int pc) {
        int k;
        int numpairs = keys.size();
        int high1 = Integer.MIN_VALUE, low1 = Integer.MAX_VALUE;
        int numslots = 0;
        if (numpairs > 0) {
            for (k = 0; k < numpairs; k++) {
                int key = keys.get(k);
                if (key > high1) {
                    high1 = key;
                }
                if (key < low1) {
                    low1 = key;
                }
            }
            numslots = high1 - low1 + 1;
        }
//      if (numslots>2000) env.error("long.switchtable", "2000");
        env.traceln("  recalcTableSwitch: low=" + low1 + " high=" + high1);
        this.pc = pc;
        pad = ((3 - pc) & 0x3);
        int len = 1 + pad + (numslots + 3) * 4;
        if (deflabel == null) {
            deflabel = new Argument(pc + len);
        }
        Argument[] resLabels1 = new Argument[numslots];
        for (k = 0; k < numslots; k++) {
            resLabels1[k] = deflabel;
        }
        for (k = 0; k < numpairs; k++) {
            env.traceln("   keys.data[" + k + "]=" + keys.get(k));
            resLabels1[keys.get(k) - low1] = labels.get(k);
        }
        this.resLabels = resLabels1;
        this.labels = null;
        this.keys = null;
        this.high = high1;
        this.low = low1;
        return len;
    }

    void writeTableSwitch(CheckedDataOutputStream out) throws IOException {
        int k;
        for (k = 0; k < pad; k++) {
            out.writeByte(0);
        }
        out.writeInt(deflabel.arg - pc);
        out.writeInt(low);
        out.writeInt(high);
        for (k = 0; k < resLabels.length; k++) {
            out.writeInt(resLabels[k].arg - pc);
        }
    }
}
