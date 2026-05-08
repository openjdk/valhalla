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
package org.openjdk.asmtools.jdis;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 */
public class ParameterAnnotationData {
    /*-------------------------------------------------------- */
    /* AnnotData Fields */

    private boolean invisible = false;
    private static final String initialTab = "";
    private ArrayList<ArrayList<AnnotationData>> array = null;

    private ClassData cls;
    /*-------------------------------------------------------- */

    public ParameterAnnotationData(ClassData cls, boolean invisible) {
        this.cls = cls;
        this.invisible = invisible;
    }

    public int numParams() {
        if (array == null) {
            return 0;
        }

        return array.size();
    }

    public ArrayList<AnnotationData> get(int i) {
        return array.get(i);
    }

    public void read(DataInputStream in) throws IOException {
        int numParams = in.readByte();
        TraceUtils.traceln("             ParameterAnnotationData[" + numParams + "]");
        array = new ArrayList<>(numParams);
        for (int paramNum = 0; paramNum < numParams; paramNum++) {

            int numAnnots = in.readShort();
            TraceUtils.traceln("             Param#[" + paramNum + "]: numAnnots=" + numAnnots);

            if (numAnnots > 0) {
                // read annotation
                ArrayList<AnnotationData> p_annots = new ArrayList<>(numAnnots);
                for (int annotIndex = 0; annotIndex < numAnnots; annotIndex++) {
                    AnnotationData annot = new AnnotationData(invisible, cls);
                    annot.read(in);
                    p_annots.add(annot);
                }
                array.add(paramNum, p_annots);
            } else {
                array.add(paramNum, null);
            }
        }
    }

    // Don't need to do this --
    // we need to print annotations (both vis and invisible) per each param number
    public void print(PrintWriter out, String tab) {
        if (array != null && array.size() > 0) {
            out.println();
            int paramNum = 0;
            for (ArrayList<AnnotationData> p_annot : array) {
                if (p_annot != null && p_annot.size() > 0) {
                    out.print("\t" + paramNum + ": ");
                    boolean firstTime = true;
                    for (AnnotationData annot : p_annot) {
                        if (!firstTime) {
                            out.print("\t   ");
                        }
                        annot.print(out, initialTab);
                        firstTime = false;
                    }
                }

                paramNum += 1;
                out.println();
            }
        }
    }

}
