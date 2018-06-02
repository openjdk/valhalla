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
import oracle.micro.valhalla.lword.types.Value1;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class Listsum1 extends ListsumBase {

    static class Node {
        public __Flattenable Value1 value;
        public Node next;


        public Node(Value1 value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    Node list = null;

    @Setup
    public void setup() {
        for (int i = 0; i < size; i++) {
            list = new Node(Value1.of(i), list);
        }
    }

    public static int sumScalarized(Node list) {
        int sum = 0;
        for (Node n  = list; n!=null; n = n.next) {
            sum += n.value.f0;
        }
        return sum;
    }

    public static int sum(Node list) {
        Value1 sum = Value1.of(0);
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
