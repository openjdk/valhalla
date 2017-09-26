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

import jdk.incubator.mvt.ValueCapableClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

@ValueCapableClass
//@jvm.internal.Vector
public final class Long2 {
    public static final int SIZE = Long.SIZE << 1;

    public static final int BYTES = SIZE / Byte.SIZE;

    public final long lo, hi; // FIXME: endianness

    private static final Class<?> THIS_CLASS = Long2.class;

    public static final MethodHandle FACTORY = VectorUtils.valueFactory(THIS_CLASS, MethodHandles.lookup());

    public Long2(long lo, long hi) {
        this.lo = lo;
        this.hi = hi;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Long2) {
            Long2 v = (Long2)o;
            return (this.lo == v.lo) && (this.hi == v.hi);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lo, hi);
    }

    @Override
    public String toString() {
        return String.format("Long2 {128: 0x%08x | 0x%08x | 0x%08x | 0x%08x :0}",
                hi >>> 32, hi & 0xFFFFFFFFL,
                lo >>> 32, lo & 0xFFFFFFFFL);
    }
}

