/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8265423
 * @summary Experimental support for generating a single class file per primitive class
 * @compile -XDunifiedValRefClass UnifiedPrimitiveClassNestHostTest.java
 * @run main UnifiedPrimitiveClassNestHostTest
 */

import java.util.Arrays;

public primitive class UnifiedPrimitiveClassNestHostTest implements java.io.Serializable {

    primitive class Inner {}

    public static void main(String [] args) {

       // check wiring of super types.
        Class<?> superClass = UnifiedPrimitiveClassNestHostTest.class.getSuperclass();
        if (!superClass.equals(Object.class))
            throw new AssertionError("Wrong superclass for UnifiedPrimitiveClassNestHostTest");

        Class<?> [] superInterfaces = UnifiedPrimitiveClassNestHostTest.class.getInterfaces();
        if (superInterfaces.length != 2)
            throw new AssertionError("Wrong super interfaces for UnifiedPrimitiveClassNestHostTest");

        if (!superInterfaces[0].equals(java.io.Serializable.class))
            throw new AssertionError("Wrong super interfaces for UnifiedPrimitiveClassNestHostTest");
        if (!superInterfaces[1].equals(PrimitiveObject.class))
            throw new AssertionError("Wrong super interfaces for UnifiedPrimitiveClassNestHostTest");

        Class<?> nestHost = UnifiedPrimitiveClassNestHostTest.class.getNestHost();
        String hostName = nestHost.getName();

        if (!hostName.equals("UnifiedPrimitiveClassNestHostTest"))
            throw new AssertionError("Wrong nest host: " + hostName);

        Class<?> [] members = nestHost.getNestMembers();
        if (members.length != 2)
            throw new AssertionError("Wrong member count: " + members.length);

        if (!members[0].equals(nestHost))
            throw new AssertionError("Wrong initial member: " + members[0]);

        if (!members[1].equals(Inner.class))
            throw new AssertionError("Wrong initial member: " + members[1]);

        if (!members[1].getNestHost().equals(nestHost))
            throw new AssertionError("Wrong nest host for member[1]: " + members[1]);

        if (!Arrays.equals(members[1].getNestMembers(), members))
            throw new AssertionError("Wrong members for member[1]: " + members[1]);
    }
}
