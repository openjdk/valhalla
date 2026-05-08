/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8301720
 * @summary Test expected value of SUPER AccessFlag for pre-ValueClass .class file
 * @requires !java.enablePreview
 * @compile -source 20 -target 20 SuperAccessFlagTest.java
 * @comment  Cannot use --release 20 because the accessFlags() method is
 *           not found in release 20; therefore -source and -target are used instead.
 * @run main SuperAccessFlagTest
 */

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/*
 * Test expected value of ACC_SUPER access flag on an earlier release.
 */
@ExpectedClassFlags("[PUBLIC, SUPER]")
public class SuperAccessFlagTest {
    public static void main(String... args) {
        checkClass(SuperAccessFlagTest.class);
    }

    private static void checkClass(Class<?> clazz) {
        ExpectedClassFlags expected =
                clazz.getAnnotation(ExpectedClassFlags.class);
        if (expected != null) {
            String actual = clazz.accessFlags().toString();
            if (!expected.value().equals(actual)) {
                throw new RuntimeException("On " + clazz +
                        " expected " + expected.value() +
                        " got " + actual);
            }
        }
    }
}

@Retention(RetentionPolicy.RUNTIME)
@ExpectedClassFlags("[INTERFACE, ABSTRACT, ANNOTATION]")
@interface ExpectedClassFlags {
    String value();
}
