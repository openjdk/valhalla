package org.openjdk.bench.valhalla.baseline.fields;

public class NodePrim2 {
    public int f0;
    public int f1;

    public static NodePrim2[] set(NodePrim2[] a) {
        for (int i = 0, k=0; i < a.length; i++, k+=2) {
            a[i] = new NodePrim2();
        }
        return a;
    }

    public static NodePrim2[] fill(NodePrim2[] a) {
        for (int i = 0, k=0; i < a.length; i++, k+=2) {
            a[i].f0 = k;
            a[i].f1 = k+1;
        }
        return a;
    }

}
