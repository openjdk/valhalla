/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Check default setting of ACC_FLATTENABLE
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile CheckDefaultFlattenable.java
 * @run main CheckDefaultFlattenable
 */

import com.sun.tools.classfile.*;

public final class CheckDefaultFlattenable {

    final __ByValue class X {
        final int x = 10;
    }
    @ValueBased
    final __ByValue class Y {
        final int x = 10;
    }

    X x1;
    __NotFlattened X x2;
    __Flattenable X x3;

    Y y1;
    __NotFlattened Y y2;
    __Flattenable Y y3;

    public static void main(String[] args) throws Exception {
        ClassFile cls = ClassFile.read(CheckDefaultFlattenable.class.getResourceAsStream("CheckDefaultFlattenable.class"));

        int checks = 0;
        for (Field field : cls.fields) {
            if (field.getName(cls.constant_pool).equals("x1")) {
                checks ++;
                if ((field.access_flags.flags & AccessFlags.ACC_FLATTENABLE) == 0)
                    throw new AssertionError("Expected flattenable flag missing");
            }
            if (field.getName(cls.constant_pool).equals("x2")) {
                checks ++;
                if ((field.access_flags.flags & AccessFlags.ACC_FLATTENABLE) != 0)
                    throw new AssertionError("Unexpected flattenable flag found");
            }
            if (field.getName(cls.constant_pool).equals("x3")) {
                checks ++;
                if ((field.access_flags.flags & AccessFlags.ACC_FLATTENABLE) == 0)
                    throw new AssertionError("Expected flattenable flag missing");
            }
            if (field.getName(cls.constant_pool).equals("y1")) {
                checks ++;
                if ((field.access_flags.flags & AccessFlags.ACC_FLATTENABLE) != 0)
                    throw new AssertionError("Unexpected flattenable flag found");
            }
            if (field.getName(cls.constant_pool).equals("y2")) {
                checks ++;
                if ((field.access_flags.flags & AccessFlags.ACC_FLATTENABLE) != 0)
                    throw new AssertionError("Unexpected flattenable flag found");
            }
            if (field.getName(cls.constant_pool).equals("y3")) {
                checks ++;
                if ((field.access_flags.flags & AccessFlags.ACC_FLATTENABLE) == 0)
                    throw new AssertionError("Expected flattenable flag missing");
            }
        }
        if (checks != 6)
            throw new AssertionError("Expected fields not found");
    }
}
