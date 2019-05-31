package org.openjdk.bench.valhalla.baseline.types;

public class Utils {

    public static int[] fill(int[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        return a;
    }

    public static Ref1[] fill(Ref1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new Ref1(i);
        }
        return a;
    }

    public static Ref2[] fill(Ref2[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 2) {
            a[i] = new Ref2(k, k + 1);
        }
        return a;
    }

    public static Ref8[] fill(Ref8[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 8) {
            a[i] = new Ref8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return a;
    }

}
