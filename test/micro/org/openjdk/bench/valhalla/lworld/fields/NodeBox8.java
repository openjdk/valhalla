package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.bench.valhalla.lworld.types.Val8;

public class NodeBox8 {
    public Val8? f;

    public static NodeBox8[] set(NodeBox8[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeBox8();
        }
        return a;
    }

    public static NodeBox8[] fill(NodeBox8[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 8) {
            a[i].f = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return a;
    }


}
