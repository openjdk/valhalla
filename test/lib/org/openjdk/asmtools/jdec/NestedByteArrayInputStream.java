/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdec;

import java.io.ByteArrayInputStream;
import java.util.Stack;

/**
 * this class provides functionality needed to read class files:
 * <ul>
 * <li>methods to read unsigned integers of various length
 * <li>counts bytes read so far
 * </ul>
 */
public class NestedByteArrayInputStream extends ByteArrayInputStream {

    NestedByteArrayInputStream(byte buf[]) {
        super(buf);
    }

    NestedByteArrayInputStream(byte buf[], int offset, int length) {
        super(buf, offset, length);
    }

    public int getPos() {
        return pos;
    }
    Stack savedStates = new Stack();

    public void enter(int range) {
        savedStates.push(count);
        if (pos + range < count) {
            count = pos + range;
        }
    }

    public void leave() {
        pos = count;
        count = ((Integer) savedStates.pop()).intValue();
    }
} // end class NestedByteArrayInputStream

