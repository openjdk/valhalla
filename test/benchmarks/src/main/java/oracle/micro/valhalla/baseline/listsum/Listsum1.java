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
import oracle.micro.valhalla.baseline.types.Box1;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Listsum1 extends ListsumBase {

    static class BoxNode {
        public Box1 value;
        public BoxNode next;


        public BoxNode(Box1 value, BoxNode next) {
            this.value = value;
            this.next = next;
        }
    }

    public static BoxNode setupBoxed(int size) {
        BoxNode list = null;
        for(int i=0; i< size; i++) {
            list = new BoxNode(new Box1(i), list);
        }
        return list;
    }

    public static int sumScalarized(BoxNode list) {
        int sum = 0;
        for (BoxNode n  = list; n!=null; n = n.next) {
            sum += n.value.f0;
        }
        return sum;
    }

    public static int sum(BoxNode list) {
        Box1 sum = new Box1(0);
        for (BoxNode n  = list; n!=null; n = n.next) {
            sum = sum.add(n.value);
        }
        return sum.totalsum();
    }

    static class Node {
        public int value;
        public Node next;


        public Node(int value, Node next) {
            this.value = value;
            this.next = next;
        }
    }


    public static Node setupPrimitive(int size) {
        Node list = null;
        for(int i=0; i< size; i++) {
            list = new Node(i, list);
        }
        return list;
    }

    public static int sumPrimitive(Node list) {
        int sum = 0;
        for (Node n  = list; n!=null; n = n.next) {
            sum += n.value;
        }
        return sum;
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
