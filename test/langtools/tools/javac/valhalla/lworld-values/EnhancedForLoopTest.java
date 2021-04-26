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
 * @bug 8244711 8244712
 * @summary Test that inline types work well with enhanced for loop.
 * @run main EnhancedForLoopTest
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/* This test covers/verifies that the asSuper calls in

   com.sun.tools.javac.comp.Lower.visitIterableForeachLoop
   com.sun.tools.javac.comp.Attr#visitForeachLoop

   work properly with primitive class types.
*/

public class EnhancedForLoopTest {

    static primitive class PrimitiveIterator<V> implements Iterator<V> {

        Iterator<V> iv;

        public PrimitiveIterator(List<V> lv) {
            this.iv = lv.iterator();
        }

        @Override
        public boolean hasNext() {
            return iv.hasNext();
        }

        @Override
        public V next() {
            return iv.next();
        }

    }

    primitive static class Foo<V> implements Iterable<V> {

        List<V> lv;

        public Foo() {
            lv = new ArrayList<>();
        }

        public void add(V v) {
            lv.add(v);
        }

       public PrimitiveIterator<V> iterator() {
            return new PrimitiveIterator<V>(lv);
        }
    }

    public static void main(String[] args) {
        Foo<String> foo = new Foo<String>();
        foo.add ("Hello");
        foo.add (" ");
        foo.add ("World");
        foo.add ("!");
        String output = "";
        for (var s : foo) {
            output += s;
        }
        if (!output.equals("Hello World!"))
            throw new AssertionError("Unexpected: " + output);
    }
}
