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
 * @bug 8294969
 * @summary javap test safeguarding malformed class file
 * @build Malformed
 * @run main MalformedTest
 * @modules jdk.jdeps/com.sun.tools.javap
 */
import java.io.PrintWriter;
import java.io.StringWriter;

public class MalformedTest {

    public static void main(String args[]) throws Exception {
        var sw = new StringWriter();
        int res = com.sun.tools.javap.Main.run(
                new String[]{"-c", "-v", System.getProperty("test.classes") + "/Malformed.class"},
                new PrintWriter(sw));
        System.out.println(sw);
        if (res == 0)
            throw new AssertionError("Failure exit code expected");
        if (sw.toString().contains("Fatal error"))
            throw new AssertionError("Unguarded fatal error");
        if (sw.toString().contains("error while reading constant pool"))
            throw new AssertionError("Unguarded constant pool error");
    }
}
