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
package oracle.micro.valhalla.baseline.listsum;

import oracle.micro.valhalla.ListsumBase;
import oracle.micro.valhalla.baseline.types.Box8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Listsum8 extends ListsumBase {

    static class BoxNode {
        public Box8 value;
        public BoxNode next;


        public BoxNode(Box8 value, BoxNode next) {
            this.value = value;
            this.next = next;
        }
    }


    public static BoxNode setupBoxed(int size) {
        BoxNode list = null;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            list = new BoxNode(new Box8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7), list);

        }
        return list;
    }

    public static int sumScalarized(BoxNode list) {
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (BoxNode n  = list; n!=null; n = n.next) {
            f0 += n.value.f0;
            f1 += n.value.f1;
            f2 += n.value.f2;
            f3 += n.value.f3;
            f4 += n.value.f4;
            f5 += n.value.f5;
            f6 += n.value.f6;
            f7 += n.value.f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    public static int sum(BoxNode list) {
        Box8 sum = new Box8(0,0,0,0,0,0,0,0);
        for (BoxNode n  = list; n!=null; n = n.next) {
            sum = sum.add(n.value);
        }
        return sum.totalsum();
    }

    static class Node {
        public int f0, f1, f2, f3, f4, f5, f6, f7;
        public Node next;


        public Node(int f0, int f1, int f2, int f3, int f4, int f5, int f6, int f7, Node next) {
            this.f0 = f0;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
            this.f5 = f5;
            this.f6 = f6;
            this.f7 = f7;
            this.next = next;
        }
    }


    public static Node setupPrimitive(int size) {
        Node list = null;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            list = new Node(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7, list);
        }
        return list;
    }

    public static int sumPrimitive(Node list) {
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (Node n  = list; n!=null; n = n.next) {
            f0 += n.f0;
            f1 += n.f1;
            f2 += n.f2;
            f3 += n.f3;
            f4 += n.f4;
            f5 += n.f5;
            f6 += n.f6;
            f7 += n.f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @State(Scope.Thread)
    public static class StatePrimitive {
        public Node list;

        @Setup
        public void setup() {
            list = setupPrimitive(size);
        }
    }

    @State(Scope.Thread)
    public static class StateBoxed {
        public BoxNode list;

        @Setup
        public void setup() {
            list = setupBoxed(size);
        }
    }

    @Benchmark
    public int boxedScalarized(StateBoxed st) {
        return sumScalarized(st.list);
    }

    @Benchmark
    public int boxed(StateBoxed st) {
        return sum(st.list);
    }

    @Benchmark
    public int primitive(StatePrimitive st) {
        return sumPrimitive(st.list);
    }

}
