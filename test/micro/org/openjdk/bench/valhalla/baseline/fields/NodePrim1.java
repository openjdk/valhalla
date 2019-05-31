package org.openjdk.bench.valhalla.baseline.fields;

public class NodePrim1 {
    public int f0;

    public static NodePrim1[] set(NodePrim1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new NodePrim1();
        }
        return a;
    }

    public static NodePrim1[] fill(NodePrim1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i].f0 = i;
        }
        return a;
    }

}
