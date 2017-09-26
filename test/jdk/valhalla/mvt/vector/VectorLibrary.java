/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.mvt.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

public class VectorLibrary {
    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static public class Scalar {
        static final MethodHandle ZERO_L = MethodHandles.constant(long.class, 0L);
        static final MethodHandle ADD_L  = Utils.compute(() -> LOOKUP.findStatic(Long.class, "sum", methodType(long.class, long.class, long.class)));
    }

    static public class L2 {
        static final MethodHandle FACTORY = Long2.FACTORY;
        static final ValueType<?> VT = ValueType.forClass(Long2.class);

        public static final MethodHandle ADD_L  = VectorUtils.lift(VT, Scalar.ADD_L, FACTORY);          // (QLong2,QLong2)QLong2
        public static final MethodHandle HADD_L = VectorUtils.reducer(VT, Scalar.ADD_L, Scalar.ZERO_L); // (long,QLong2)long
    }
}

