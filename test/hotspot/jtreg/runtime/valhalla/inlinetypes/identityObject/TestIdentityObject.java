/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
/*
 * @test
 * @summary test that IdentityObject interface is injected correctly
 * @compile IdentityType.jcod
 * @compile Interface.java InterfaceExtendingIdentityObject.java
 * @compile AbstractTypeImplementingIdentityObject.java
 * @compile AbstractTypeWithNonstaticFields.java AbstractTypeWithStaticFields.java
 * @compile AbstractTypeWithSynchronizedNonstaticMethod.java AbstractTypeWithSynchronizedStaticMethod.java
 * @compile InlineType.java IdentityTypeImplementingIdentityObject.java
 * @compile TestIdentityObject.java
 * @run main/othervm -verify TestIdentityObject
 */

public class TestIdentityObject {
    static void checkIdentityObject(Class c, boolean subtype) {
        boolean s;
        try {
            c.asSubclass(IdentityObject.class);
            s = true;
        } catch(ClassCastException e) {
            s = false;
        }
        if (subtype != s) {
            if (subtype) {
                throw new RuntimeException("Type " + c.getName() + " is missing IdentityObject");
            } else {
                throw new RuntimeException("Type " + c.getName() + " should not implements IdentityObject");
            }
        }
    }

    public static void main(String[] args) {
        checkIdentityObject(InlineType.class, false);
        checkIdentityObject(IdentityType.class, true);
        checkIdentityObject(IdentityTypeImplementingIdentityObject.class, true);
        checkIdentityObject(Interface.class, false);
        checkIdentityObject(InterfaceExtendingIdentityObject.class, true);
        checkIdentityObject(AbstractTypeImplementingIdentityObject.class, true);
        checkIdentityObject(AbstractTypeWithNonstaticFields.class, true);
        checkIdentityObject(AbstractTypeWithStaticFields.class, false);
        checkIdentityObject(AbstractTypeWithSynchronizedNonstaticMethod.class, true);
        checkIdentityObject(AbstractTypeWithSynchronizedStaticMethod.class, false);
    }
}
