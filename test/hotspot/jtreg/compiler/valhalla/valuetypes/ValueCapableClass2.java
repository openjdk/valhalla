/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * A value-capable class (VCC) from which HotSpot derives a value
 * type. The derived value type (DVT) is referred to as
 * ValueCapableClass2$Value.
 */
package compiler.valhalla.valuetypes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import jdk.incubator.mvt.ValueType;

@jdk.incubator.mvt.ValueCapableClass
public final class ValueCapableClass2 {
    public final long u;

    public static final MethodHandle FACTORY;
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        ValueType<?> VT = ValueType.forClass(ValueCapableClass2.class);
        try {
            FACTORY = VT.unreflectWithers(lookup, true, VT.valueFields());
        } catch (NoSuchFieldException|IllegalAccessException e) {
            throw new RuntimeException("method handle lookup fails");
        }
    }

    private ValueCapableClass2(long u) {
        this.u = u;
    }

    public static ValueCapableClass2 create(long u) {
        return new ValueCapableClass2(u);
    }
}
