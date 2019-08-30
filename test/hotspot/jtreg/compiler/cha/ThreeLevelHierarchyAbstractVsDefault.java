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

public class ThreeLevelHierarchyAbstractVsDefault extends ATest<ThreeLevelHierarchyAbstractVsDefault.I> {
    public ThreeLevelHierarchyAbstractVsDefault() {
        super(I.class, C.class);
    }

    interface J1                { default Object m() { return WRONG; } } // intf J1.m DEFAULT
    interface J2 extends J1     { Object m(); }                          // intf J2.m ABSTRACT <: intf J1
    interface I  extends J1, J2 {}                                       // intf  I.m OVERPASS <: intf J1,J2

    static class C  implements I { public Object m() { return CORRECT; }}

    @DontInline
    public Object test(I i) {
        return i.m(); // intf I.m OVERPASS
    }

    static class DI implements I { public Object m() { return WRONG;   }}

    static class DJ11 implements J1 {}
    static class DJ12 implements J1 { public Object m() { return WRONG; }}

    static class DJ2 implements J2 { public Object m() { return WRONG;   }}

    interface K11 extends J1 {}
    interface K12 extends J1 { Object m(); }
    interface K13 extends J1 { default Object m() { return WRONG; }}
    interface K21 extends J2 {}
    interface K22 extends J2 { Object m(); }
    interface K23 extends J2 { default Object m() { return WRONG; }}

    public void testMega1() {
        // 0. Trigger compilation of megamorphic call site
        compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I.m OVERPASS <: intf J2.m ABSTRACT <: intf J1.m DEFAULT
        assertCompiled();

        // Dependency: type = unique_concrete_method, context = I, method = C.m

        checkInvalidReceiver(); // ensure proper type check on receiver is preserved

        // 1. No deopt/invalidation on not-yet-seen receiver
        repeat(100, () -> call(new C(){})); // Cn <: C.m <: intf I.m OVERPASS <: intf J2.m ABSTRACT <: intf J1.m DEFAULT
        assertCompiled();

        // 2. No dependency invalidation: different context
        initialize(K11.class, K12.class, K13.class,
                K21.class, K22.class, K23.class);

        // 3. Dependency invalidation: Cn.m <: C <: I
        call(new C() { public Object m() { return CORRECT; }}); // Cn.m <: C.m <: intf I.m OVERPASS <: intf J2.m ABSTRACT <: intf J1.m DEFAULT
        assertNotCompiled();

        // 4. Recompilation w/o a dependency
        compile(megamorphic());
        call(new C() { public Object m() { return CORRECT; }});
        assertCompiled(); // no inlining

        checkInvalidReceiver(); // ensure proper type check on receiver is preserved
    }

    public void testMega2() {
        // 0. Trigger compilation of a megamorphic call site
        compile(megamorphic());
        assertCompiled();

        // Dependency: type = unique_concrete_method, context = I, method = C.m

        checkInvalidReceiver(); // ensure proper type check on receiver is preserved

        // 1. No dependency invalidation: different context
        initialize(DJ11.class,
                DJ12.class,
                DJ2.class);
        assertCompiled();

        // 2. Dependency invalidation: DI.m <: I
        initialize(DI.class);
        assertNotCompiled();

        // 3. Recompilation w/o a dependency
        compile(megamorphic());
        call(new C() { public Object m() { return CORRECT; }});
        assertCompiled(); // no inlining

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
            I j = (I) unsafeCastMH(I.class).invokeExact((Object) new J1() {
            }); // super interface
            test(j);
        });
        assertCompiled();

        shouldThrow(IncompatibleClassChangeError.class, () -> {
            I j = (I) unsafeCastMH(I.class).invokeExact((Object) new J2() {
                public Object m() {
                    return WRONG;
                }
            }); // super interface
            test(j);
        });
        assertCompiled();
    }
}

