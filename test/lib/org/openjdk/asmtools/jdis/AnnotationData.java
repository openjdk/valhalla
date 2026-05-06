/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.String.format;

/**
 *
 */
public class  AnnotationData {
    /*-------------------------------------------------------- */
    /* AnnotData Fields */

    protected String visAnnotToken = "@+";
    protected String invAnnotToken = "@-";
    protected String dataName = "AnnotationData";
    private boolean invisible = false;
    private int type_cpx = 0;  //an index into the constant pool indicating the annotation type for this annotation.
    private ArrayList<AnnotationElement> array = new ArrayList<>();
    private ClassData cls;
    /*-------------------------------------------------------- */

    public AnnotationData(boolean invisible, ClassData cls) {
        this.cls = cls;
        this.invisible = invisible;
    }

    public void read(DataInputStream in) throws IOException {
        type_cpx = in.readShort();
        int elemValueLength = in.readShort();
        TraceUtils.traceln(3, format(" %s: name[%d]=%s", dataName, type_cpx, cls.pool.getString(type_cpx)),
                format(" %s: %s  num_elems: %d", dataName, cls.pool.getString(type_cpx), elemValueLength));
        for (int evc = 0; evc < elemValueLength; evc++) {
            AnnotationElement elem = new AnnotationElement(cls);
            TraceUtils.traceln(3, format(" %s: %s reading [%d]", dataName, cls.pool.getString(type_cpx), evc));
            elem.read(in, invisible);
            array.add(elem);
        }
    }

    public void print(PrintWriter out, String tab) {
        printHeader(out, tab);
        printBody(out, "");
    }

    protected void printHeader(PrintWriter out, String tab) {
        //Print annotation Header, which consists of the
        // Annotation Token ('@'), visibility ('+', '-'),
        // and the annotation name (type index, CPX).

        // Mark whether it is invisible or not.
        if (invisible) {
            out.print(tab + invAnnotToken);
        } else {
            out.print(tab + visAnnotToken);
        }
        String annoName = cls.pool.getString(type_cpx);

        // converts class type to java class name
        if (annoName.startsWith("L") && annoName.endsWith(";")) {
            annoName = annoName.substring(1, annoName.length() - 1);
        }

        out.print(annoName);
    }

    protected void printBody(PrintWriter out, String tab) {
        // For a standard annotation, print out brackets,
        // and list the name/value pairs.
        out.print(" { ");
        int i = 0;
        for (AnnotationElement elem : array) {
            elem.print(out, tab);
            if (i++ < array.size() - 1) {
                out.print(", ");
            }
        }
        out.print("  }");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String annoName = cls.pool.getString(type_cpx);

        // converts class type to java class name
        if (annoName.startsWith("L") && annoName.endsWith(";")) {
            annoName = annoName.substring(1, annoName.length() - 1);
        }

        //Print annotation
        // Mark whether it is invisible or not.
        if (invisible) {
            sb.append(invAnnotToken);
        } else {
            sb.append(visAnnotToken);
        }

        sb.append(annoName);
        sb.append(" { ");

        int i = 0;
        for (AnnotationElement elem : array) {
            sb.append(elem.toString());

            if (i++ < array.size() - 1) {
                sb.append(", ");
            }
        }

        _toString(sb);

        sb.append("}");
        return sb.toString();
    }

    protected void _toString(StringBuilder sb) {
        // sub-classes override this
    }
}

