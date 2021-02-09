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
 * @bug 8244982
 * @summary Javac has trouble compiling method references
 * @run main StreamsTest
 */

import java.util.Arrays;

public class StreamsTest {

    public static primitive class X {

        String data;

        X(String data) {
            this.data = data;
        }

        String data() { return data; }

        static String accumulate = "";

        static void accumulate(String s) {
            accumulate += s;
        }

        static String streamedData() {

            X [] xs = new X[] {
                                 new X("Streams "),
                                 new X("test "),
                                 new X("passed OK!")
                      };

            Arrays.stream(xs)
                        .map(X.ref::data)
                        .filter(p -> p != null)
                        .forEach(X::accumulate);

            return accumulate;
        }
    }

    public static void main(String [] args) {
        if (!X.streamedData().equals("Streams test passed OK!"))
            throw new AssertionError("Unexpected data in stream");
    }
}
