package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.bench.valhalla.lworld.types.Val1;

public class NodeVal1 {
    public Val1 f;

    public static NodeVal1[] set(NodeVal1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeVal1();
        }
        return a;
    }

    public static NodeVal1[] fill(NodeVal1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i].f = new Val1(i);
        }
        return a;
    }
}
