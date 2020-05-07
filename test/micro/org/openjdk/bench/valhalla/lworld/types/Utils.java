package org.openjdk.bench.valhalla.lworld.types;


public class Utils {

    public static Val1[] fillV(Val1[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new Val1(i);
        }
        return a;
    }

    public static Val1.ref[] fillB(Val1.ref[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = new Val1(i);
        }
        return a;
    }

    public static Val2[] fillV(Val2[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 2) {
            a[i] = new Val2(k, k + 1);
        }
        return a;
    }

    public static Val2.ref[] fillB(Val2.ref[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 2) {
            a[i] = new Val2(k, k + 1);
        }
        return a;
    }

    public static Val8[] fillV(Val8[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 8) {
            a[i] = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return a;
    }

    public static Val8.ref[] fillB(Val8.ref[] a) {
        for (int i = 0, k = 0; i < a.length; i++, k += 8) {
            a[i] = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return a;
    }

}
