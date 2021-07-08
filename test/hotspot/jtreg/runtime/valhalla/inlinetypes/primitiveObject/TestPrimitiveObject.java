/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test that PrimitiveObject interface is injected correctly
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder
 * @compile TestPrimitiveObject.java
 * @compile PrimitiveType.java PrimitiveTypeSpecified.java
 * @compile AbstractSpecified.java InterfaceSpecified.java
 * @compile PrimitiveWithSuper.java PrimitiveWithInterface.java
 * @run main/othervm -verify TestPrimitiveObject
 */

import java.lang.invoke.*;
import jdk.experimental.bytecode.*;

public class TestPrimitiveObject {

    public static void main(String[] args) {
        checkNegativePrimitiveObjects();
        checkPositivePrimitiveObjects();
        checkIcceOnInvalidSupers();
    }

    static void checkNegativePrimitiveObjects() {
        Class[] clazzes = new Class[] {
            String.class, Comparable.class, Number.class
        };
        for (Class clazz : clazzes) {
            checkPrimitiveObject(clazz, false);
        }
    }

    static void checkPositivePrimitiveObjects() {
        Class[] clazzes = new Class[] {
            PrimitiveType.class, PrimitiveTypeSpecified.class,
            AbstractSpecified.class, InterfaceSpecified.class,
            PrimitiveWithSuper.class, PrimitiveWithInterface.class
        };
        for (Class clazz : clazzes) {
            checkPrimitiveObject(clazz, true);
        }
    }

    static void checkPrimitiveObject(Class c, boolean subtype) {
        boolean s;
        try {
            c.asSubclass(PrimitiveObject.class);
            s = true;
        } catch(ClassCastException e) {
            s = false;
        }
        if (subtype != s) {
            if (subtype) {
                throw new RuntimeException("Type " + c.getName() + " is missing PrimitiveObject");
            } else {
                throw new RuntimeException("Type " + c.getName() + " should not implement PrimitiveObject");
            }
        }
    }

    // Define classes that implement PrimitiveObject but are invalid supers
    static void checkIcceOnInvalidSupers() {
        MethodHandles.Lookup mhLookup = MethodHandles.lookup();
        checkIcce(mhLookup, createClass().build());
        checkIcce(mhLookup, createAbstractWithField().build());
        checkIcce(mhLookup, createAbstractIdentity().build());
        checkIcce(mhLookup, createIdentity().build());
    }

    static ClassBuilder createClass() {
        return new BasicClassBuilder("ANormalClass", 62, 0)
            .withSuperclass("java/lang/Object")
            .withSuperinterface("java/lang/PrimitiveObject");
    }

    static ClassBuilder createAbstractWithField() {
        return new BasicClassBuilder("AbstractWithField", 62, 0)
            .withSuperclass("java/lang/Object")
            .withFlags(Flag.ACC_ABSTRACT)
            .withField("aFieldWhichIsIllegalAsAnAbstractSuperToPrimitiveObject", "I")
            .withSuperinterface("java/lang/PrimitiveObject");
    }

    static ClassBuilder createAbstractIdentity() {
        return new BasicClassBuilder("AbstractIdentity", 62, 0)
            .withSuperclass("java/lang/Object")
            .withFlags(Flag.ACC_ABSTRACT)
            .withSuperinterface("java/lang/IdentityObject")
            .withSuperinterface("java/lang/PrimitiveObject");
    }

  static ClassBuilder createIdentity() {
        return new BasicClassBuilder("Identity", 62, 0)
            .withSuperclass("java/lang/Object")
            .withSuperinterface("java/lang/IdentityObject")
            .withSuperinterface("java/lang/PrimitiveObject");
    }

    static void checkIcce(MethodHandles.Lookup mhLookup, byte[] clazzBytes) {
        try {
            mhLookup.defineClass(clazzBytes);
            throw new RuntimeException("Expected IncompatibleClassChangeError");
        }
        catch (IllegalAccessException ill) { throw new RuntimeException(ill); }
        catch (IncompatibleClassChangeError icce) { System.out.println(icce); }
    }

}
