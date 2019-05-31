package org.openjdk.bench.valhalla.baseline.fields;

import org.openjdk.bench.valhalla.baseline.types.Ref8;

public class NodeRef8 {
    public Ref8 f;

    public static NodeRef8[] set(NodeRef8[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeRef8();
        }
        return a;
    }

    public static NodeRef8[] fill(NodeRef8[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 8) {
            a[i].f = new Ref8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return a;
    }


}
