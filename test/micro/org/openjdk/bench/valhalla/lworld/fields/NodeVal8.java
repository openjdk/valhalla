package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.bench.valhalla.lworld.types.Val8;

public class NodeVal8 {
    public Val8 f;

    public static NodeVal8[] set(NodeVal8[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeVal8();
        }
        return a;
    }

    public static NodeVal8[] fill(NodeVal8[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 8) {
            a[i].f = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return a;
    }


}
