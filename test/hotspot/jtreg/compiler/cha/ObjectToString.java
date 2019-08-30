/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package compiler.cha;

import static compiler.cha.StrengthReduceInterfaceCall.*;

public class ObjectToString extends ATest<ObjectToString.I> {
    public ObjectToString() {
        super(I.class, C.class);
    }

    interface J           { String toString(); }
    interface I extends J {}

    static class C implements I {}

    interface K1 extends I {}
    interface K2 extends I { String toString(); } // K2.tS() ABSTRACT
    // interface K3 extends I { default String toString() { return "K3"; } // K2.tS() DEFAULT

    static class D implements I { public String toString() { return "D"; }}

    static class DJ1 implements J {}
    static class DJ2 implements J { public String toString() { return "DJ2"; }}

    @Override
    public Object test(I i) { return ObjectToStringHelper.test(i); /* invokeinterface I.toString() */ }

    @TestCase
    public void testMono() {
        // 0. Trigger compilation of a monomorphic call site
        compile(monomophic()); // C1 <: C <: intf I <: intf J <: Object.toString()
        assertCompiled();

        // Dependency: none

        call(new C() { public String toString() { return "Cn"; }}); // Cn.tS <: C.tS <: intf I
        assertCompiled();
    }

    @TestCase
    public void testBi() {
        // 0. Trigger compilation of a bimorphic call site
        compile(bimorphic()); // C1 <: C <: intf I <: intf J <: Object.toString()
        assertCompiled();

        // Dependency: none

        call(new C() { public String toString() { return "Cn"; }}); // Cn.tS <: C.tS <: intf I
        assertCompiled();
    }

    @TestCase
    public void testMega() {
        // 0. Trigger compilation of a megamorphic call site
        compile(megamorphic()); // C1,C2,C3 <: C <: intf I <: intf J <: Object.toString()
        assertCompiled();

        // Dependency: none
        // compiler.cha.StrengthReduceInterfaceCall$ObjectToString::test (5 bytes)
        //     @ 1   compiler.cha.StrengthReduceInterfaceCall$ObjectToStringHelper::test (7 bytes)   inline (hot)
        //       @ 1   java.lang.Object::toString (36 bytes)   virtual call

        // No dependency - no invalidation
        repeat(100, () -> call(new C(){})); // Cn <: C <: intf I
        assertCompiled();

        initialize(K1.class,   // intf  K1             <: intf I <: intf J
                   K2.class,   // intf  K2.tS ABSTRACT <: intf I <: intf J
                   DJ1.class,  //      DJ1                       <: intf J
                   DJ2.class); //      DJ2.tS                    <: intf J
        assertCompiled();

        initialize(D.class); // D.tS <: intf I <: intf J
        assertCompiled();

        call(new C() { public String toString() { return "Cn"; }}); // Cn.tS <: C.tS <: intf I
        assertCompiled();
    }

    @Override
    public void checkInvalidReceiver() {
        shouldThrow(IncompatibleClassChangeError.class, () -> {
            I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
            test(o);
        });
        assertCompiled();

        shouldThrow(IncompatibleClassChangeError.class, () -> {
            I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); // super interface
            test(j);
        });
        assertCompiled();
    }
}


