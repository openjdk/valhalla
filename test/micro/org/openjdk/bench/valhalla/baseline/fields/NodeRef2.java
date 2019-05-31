package org.openjdk.bench.valhalla.baseline.fields;

import org.openjdk.bench.valhalla.baseline.types.Ref2;

public class NodeRef2 {
    public Ref2 f;

    public static NodeRef2[] set(NodeRef2[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeRef2();
        }
        return a;
    }

    public static NodeRef2[] fill(NodeRef2[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 2) {
            a[i].f = new Ref2(k, k + 1);
        }
        return a;
    }


}
