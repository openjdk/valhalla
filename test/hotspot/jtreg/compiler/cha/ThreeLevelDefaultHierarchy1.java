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

public class ThreeLevelDefaultHierarchy1 extends ATest<ThreeLevelDefaultHierarchy1.I> {
    public ThreeLevelDefaultHierarchy1() {
        super(I.class, C.class);
    }

    interface J1                { Object m();}
    interface J2 extends J1     { default Object m() { return WRONG; }  }
    interface I  extends J1, J2 {}

    static class C  implements I { public Object m() { return CORRECT; }}

    interface K1 extends I {}
    interface K2 extends I { Object m(); }
    interface K3 extends I { default Object m() { return WRONG; }}

    static class DI implements I { public Object m() { return WRONG; }}
    static class DJ1 implements J1 { public Object m() { return WRONG; }}
    static class DJ2 implements J2 { public Object m() { return WRONG; }}

    @DontInline
    public Object test(I i) {
        return i.m(); // no inlining since J.m is a default method
    }

    @TestCase
    public void testMega() {
        // 0. Trigger compilation of a megamorphic call site
        compile(megamorphic());
        assertCompiled();

        // Dependency: none

        checkInvalidReceiver(); // ensure proper type check on receiver is preserved

        // 1. No deoptimization/invalidation on not-yet-seen receiver
        repeat(100, () -> call(new C() {}));
        assertCompiled();

        // 2. No dependency, no inlining
        // CHA doesn't support default methods yet.
        initialize(DJ1.class,
                   DJ2.class,
                   DI.class,
                   K1.class,
                   K2.class,
                   K3.class);
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
            I j = (I) unsafeCastMH(I.class).invokeExact((Object) new J1() {
                public Object m() {
                    return WRONG;
                }
            }); // super interface
            test(j);
        });
        assertCompiled();

        shouldThrow(IncompatibleClassChangeError.class, () -> {
            I j = (I) unsafeCastMH(I.class).invokeExact((Object) new J2() {
            }); // super interface
            test(j);
        });
        assertCompiled();
    }
}
