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
 */

/**
 * @test
 * @bug 8237072
 * @summary Test compiler on various constructs it had issues with.
 */

import java.util.ArrayList;
import java.util.Arrays;

public class NoCrashTest {

    interface I {}
    static class C implements I {}
    static primitive final class V implements I { int x = 0; }

    static void triggerNPE(V.ref [] vra) {
        vra[0] = null;
    }

    static String foo(V[] va) {
        return "array of nonnull v's";
    }

    static String foo(Object [] oa) {
        return "array of nullable o's";
    }

    static public void main(String[] args) {
        I arg = args.length == 0 ? new V() : new C();
        V [] xs = new V[0];
        Object [] os = new Object [0];
        Object [] o = args.length == 0 ? xs : os;
        Object o2 = (o == null) ? new V()  : new Object();

        triggerNPE(new V.ref[1]); // NO NPE.
        try {
            triggerNPE(new V[1]);
            throw new RuntimeException("Should not get here!");
        } catch (NullPointerException npe) {
            // all is well.
        }

        V [] v = new V[0];
        if (!foo((V.ref []) v).equals("array of nullable o's"))
            throw new AssertionError("Broken");

        ArrayList<V.ref> vList = new ArrayList<V.ref>(Arrays.asList(new V.ref[10]));
    }
}
