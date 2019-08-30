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

import jdk.internal.vm.annotation.DontInline;
import static compiler.cha.StrengthReduceInterfaceCall.*;

public class TwoLevelHierarchyLinear extends ATest<TwoLevelHierarchyLinear.I> {
    public TwoLevelHierarchyLinear() {
        super(I.class, C.class);
    }

    interface J { default Object m() { return WRONG; } }

    interface I extends J { Object m(); }
    static class C implements I { public Object m() { return CORRECT; }}

    interface K1 extends I {}
    interface K2 extends I { Object m(); }
    interface K3 extends I { default Object m() { return WRONG; }}

    static class D implements I { public Object m() { return WRONG;   }}

    static class DJ1 implements J {}
    static class DJ2 implements J { public Object m() { return WRONG; }}

    @DontInline
    public Object test(I i) {
        return i.m();
    }

    @TestCase
    public void testMega1() {
        // 0. Trigger compilation of a megamorphic call site
        compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I.m ABSTRACT <: intf J.m ABSTRACT
        assertCompiled();

        // Dependency: type = unique_concrete_method, context = I, method = C.m

        checkInvalidReceiver(); // ensure proper type check is preserved

        // 1. No deoptimization/invalidation on not-yet-seen receiver
        repeat(100, () -> call(new C(){})); // Cn <: C.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
        assertCompiled();

        // 2. No dependency invalidation on class loading of unrelated classes: different context
        initialize(K1.class,   // intf  K1            <: intf I.m ABSTRACT <: intf J.m DEFAULT
                   K2.class,   // intf  K2.m ABSTRACT <: intf I.m ABSTRACT <: intf J.m DEFAULT
                   DJ1.class,  //      DJ1                                 <: intf J.m DEFAULT
                   DJ2.class); //      DJ2.m                               <: intf J.m DEFAULT
        assertCompiled();

        // 3. Dependency invalidation on D <: I
        initialize(D.class); // D.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
        assertNotCompiled();

        // 4. Recompilation: no inlining, no dependencies
        compile(megamorphic());
        call(new C() { public Object m() { return CORRECT; }}); // Cn.m <: C.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
        assertCompiled();

        checkInvalidReceiver(); // ensure proper type check on receiver is preserved
    }

    @TestCase
    public void testMega2() {
        // 0. Trigger compilation of a megamorphic call site
        compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
        assertCompiled();

        // Dependency: type = unique_concrete_method, context = I, method = C.m

        checkInvalidReceiver(); // ensure proper type check on receiver is preserved

        // 1. Dependency invalidation
        initialize(K3.class); // intf K3.m DEFAULT <: intf I.m ABSTRACT <: intf J.m DEFAULT
        assertNotCompiled();

        // 2. Recompilation: still inlines
        // FIXME: no default method support in CHA yet
        compile(megamorphic());
        call(new K3() { public Object m() { return CORRECT; }}); // K3n.m <: intf K3.m DEFAULT <: intf I.m ABSTRACT <: intf J.m ABSTRACT
        assertNotCompiled();

        // 3. Recompilation: no inlining, no dependencies
        compile(megamorphic());
        call(new K3() { public Object m() { return CORRECT; }}); // Kn.m <: intf K3.m DEFAULT  <: intf I.m ABSTRACT <: intf J.m DEFAULT
        assertCompiled();

        checkInvalidReceiver(); // ensure proper type check on receiver is preserved
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

