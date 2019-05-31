package org.openjdk.bench.valhalla.baseline.fields;

import org.openjdk.bench.valhalla.baseline.types.Ref1;

public class NodeRef1 {
    public Ref1 f;

    public static NodeRef1[] set(NodeRef1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodeRef1();
        }
        return a;
    }

    public static NodeRef1[] fill(NodeRef1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i].f = new Ref1(i);
        }
        return a;
    }
}
