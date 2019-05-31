package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.bench.valhalla.lworld.types.Val2;

public class NodeBox2 {
    public Val2? f;

    public static NodeBox2[] set(NodeBox2[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeBox2();
        }
        return a;
    }

    public static NodeBox2[] fill(NodeBox2[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 2) {
            a[i].f = new Val2(k, k + 1);
        }
        return a;
    }


}
