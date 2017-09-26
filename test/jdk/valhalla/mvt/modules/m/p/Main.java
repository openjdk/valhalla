/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jdk.incubator.mvt.ValueType;
import p.internal.Point;

public class Main {
    private static List<String> FIELD_NAMES = List.of("x", "y", "z");
    private static List<Class<?>> FIELD_TYPES =  List.of(int.class, short.class, short.class);

    public static void main(String... args) throws Exception {
        ValueType<?> valueType = ValueType.forClass(Point.class);
        Module module = Point.class.getModule();
        assertTrue(module.isNamed(), "unexpected " + module.toString());
        assertTrue(valueType.boxClass().getModule() == module,
                   "unexpected " + valueType.boxClass().getModule());
        assertTrue(valueType.valueClass().getModule() == module,
            "unexpected " + valueType.valueClass().getModule() );

        Field[] fields = Point.class.getDeclaredFields();
        assertTrue(fields.length == FIELD_NAMES.size(), Arrays.toString(fields));

        // validate field names
        List<String> names = Arrays.stream(fields)
                                   .sorted(Comparator.comparing(Field::getName))
                                   .map(Field::getName)
                                   .collect(Collectors.toList());
        assertTrue(names.equals(FIELD_NAMES), names.toString());

        // validate field types
        List<Class<?>> types = Arrays.stream(fields)
                                     .sorted(Comparator.comparing(Field::getName))
                                     .map(Field::getType)
                                     .collect(Collectors.toList());
        assertTrue(types.equals(FIELD_TYPES), types.toString());

        // getting MethodHandle
        MethodHandles.Lookup lookup =
            MethodHandles.privateLookupIn(Point.class, MethodHandles.lookup());
        for (Field f : fields) {
            MethodHandle mh1 = valueType.findGetter(lookup, f.getName(), f.getType());
            MethodHandle mh2 = valueType.findWither(lookup, f.getName(), f.getType());
            System.out.println(mh1 + " " + mh2);
        }
    }

    private static void assertTrue(boolean b, String msg) {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }
}
