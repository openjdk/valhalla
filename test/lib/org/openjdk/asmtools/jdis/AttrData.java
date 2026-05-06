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

import org.openjdk.asmtools.jasm.Tables;

import java.io.DataInputStream;
import java.io.IOException;

import static java.lang.String.format;

/**
 *
 */
public class AttrData {

    int name_cpx;
    byte data[];
    ClassData cls;
    public AttrData(ClassData cls) {
        this.cls = cls;
    }

    /**
     * attributeTag
     * <p>
     * returns either -1 (not found), or the hashed integer tag tag.
     */
    public static int attributeTag(String tagname) {
        int intgr = Tables.attrtagValue(tagname);

        if (intgr == 0) {
            return -1;
        }

        return intgr;
    }

    public void read(int name_cpx, int attrlen, DataInputStream in) throws IOException {
        this.name_cpx = name_cpx;
        data = new byte[attrlen];
        TraceUtils.traceln(format("AttrData:#%d len=%d", name_cpx, attrlen));
        in.readFully(data);
    }
}
