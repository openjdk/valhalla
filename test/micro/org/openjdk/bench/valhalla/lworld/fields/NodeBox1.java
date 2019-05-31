package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.bench.valhalla.lworld.types.Val1;

public class NodeBox1 {
    public Val1? f;

    public static NodeBox1[] set(NodeBox1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeBox1();
        }
        return a;
    }

    public static NodeBox1[] fill(NodeBox1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i].f = new Val1(i);
        }
        return a;
    }
}
