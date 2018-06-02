/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package oracle.micro.valhalla.lword.listsum;

import oracle.micro.valhalla.ListsumBase;
import oracle.micro.valhalla.lword.types.Value2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class Listsum2 extends ListsumBase {

    static class Node {
        public __Flattenable Value2 value;
        public Node next;


        public Node(Value2 value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    Node list = null;

    @Setup
    public void setup() {
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            list = new Node( Value2.of(k, k+1), list);

        }
    }

    public static int sumScalarized(Node list) {
        int f0 = 0;
        int f1 = 0;
        for (Node n  = list; n!=null; n = n.next) {
            f0  += n.value.f0;
            f1  += n.value.f1;
        }
        return f0 + f1;
    }

    public static int sum(Node list) {
        Value2 sum = Value2.of(0,0);
        for (Node n  = list; n!=null; n = n.next) {
            sum = sum.add(n.value);
        }
        return sum.totalsum();
    }

    @Benchmark
    public int valueScalarized() {
        return sumScalarized(list);
    }

    @Benchmark
    public int value() {
        return sum(list);
    }

}
