/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244711
 * @summary Test that primitive classes work well with TWR
 * @run main AutoCloseableTest
 */

public class AutoCloseableTest {

    primitive static class Foo implements AutoCloseable {

        String s = "Exception while closing AutoCloseable";

        @Override
        public void close() throws Exception {
            throw new Exception(s);
        }

    }

    public static void main(String[] args) {
        String expected = "";
        String found = "";
        try (Foo foo = new Foo()) {
            expected = foo.s;
        } catch (Exception e) {
            found = e.getMessage();
        }
        if (expected.length() == 0 || !expected.equals(found))
            throw new AssertionError("Should not get here!");
    }
}
