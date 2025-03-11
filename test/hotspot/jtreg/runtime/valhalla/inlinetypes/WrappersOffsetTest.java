/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:-UseFieldFlattening -XX:-UseArrayFlattening -XX:-UseNullableValueFlattening WrappersOffsetTest
 */

 /*
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:-UseFieldFlattening -XX:-UseArrayFlattening -XX:+UseNullableValueFlattening WrappersOffsetTest
 */

 /*
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:-UseFieldFlattening -XX:+UseArrayFlattening -XX:-UseNullableValueFlattening WrappersOffsetTest
 */

 /*
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:+UseFieldFlattening -XX:-UseArrayFlattening -XX:-UseNullableValueFlattening WrappersOffsetTest
 */

 /*
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:+UseFieldFlattening -XX:-UseArrayFlattening -XX:+UseNullableValueFlattening WrappersOffsetTest
 */

 /*
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:-UseFieldFlattening -XX:+UseArrayFlattening -XX:+UseNullableValueFlattening WrappersOffsetTest
 */

 /*
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:+UseFieldFlattening -XX:+UseArrayFlattening -XX:-UseNullableValueFlattening WrappersOffsetTest
 */

/*
 * @test
 * @summary Test verification of wrappers classes' field offset with various layout
 * @enablePreview
 * @run main/othervm -XX:+UseFieldFlattening -XX:+UseArrayFlattening -XX:+UseNullableValueFlattening WrappersOffsetTest
 */

public class WrappersOffsetTest {
    public static void main(String[] args) {
        // The test just ensures that wrappers classes are loaded
        // The verification code is inside the JVM (javaClasses.cpp)
        // and is executed in VM debug builds
        Boolean z = Boolean.valueOf(true);
        Byte b = Byte.valueOf((byte)1);
        Character c = Character.valueOf('c');
        Short s = Short.valueOf((short)2);
        Integer i = Integer.valueOf(3);
        Float f = Float.valueOf(0.4f);
        Long l = Long.valueOf(5L);
        Double d = Double.valueOf(0.6);
    }
}
