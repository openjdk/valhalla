/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary check that speculative attribution doesn't cause issues with type class op resolution
 *          when witness methods are present
 * @modules jdk.compiler
 */

import java.nio.file.Path;
import java.util.List;

public class TestBootstrap {
    static String testSrc = System.getProperty("test.src", ".");

    public static void main(String... args) {
        Path x = Path.of(testSrc, "x");
        String[] jcArgs = { "-d", ".", "--patch-module", "java.base=" + x.toAbsolutePath(),
                x.resolve("TestClass.java").toString(),
                x.resolve("java").resolve("lang").resolve("Integral.java").toString() };
        compile(jcArgs);
    }

    static void compile(String... args) {
        int rc = com.sun.tools.javac.Main.compile(args);
        switch (rc) {
            case 0 -> throw new AssertionError("javac passed unexpectedly: " + List.of(args));
            case 4 -> throw new AssertionError("javac crashed unexpectedly: " + List.of(args));
        }
    }
}
