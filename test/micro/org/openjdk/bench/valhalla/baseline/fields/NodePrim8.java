package org.openjdk.bench.valhalla.baseline.fields;

public class NodePrim8 {
    public int f0;
    public int f1;
    public int f2;
    public int f3;
    public int f4;
    public int f5;
    public int f6;
    public int f7;

    public static NodePrim8[] set(NodePrim8[] a) {
        for (int i = 0, k=0; i < a.length; i++, k+=8) {
            a[i] = new NodePrim8();
        }
        return a;
    }

    public static NodePrim8[] fill(NodePrim8[] a) {
        for (int i = 0, k=0; i < a.length; i++, k+=8) {
            a[i].f0 = k;
            a[i].f1 = k+1;
            a[i].f2 = k+2;
            a[i].f3 = k+3;
            a[i].f4 = k+4;
            a[i].f5 = k+5;
            a[i].f6 = k+6;
            a[i].f7 = k+7;
        }
        return a;
    }

}
