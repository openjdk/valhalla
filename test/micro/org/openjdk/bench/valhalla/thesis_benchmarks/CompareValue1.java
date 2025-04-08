package org.openjdk.bench.valhalla.thesis_benchmarks;


public class CompareValue1 {
    int i;
    long l;
    Object o;
    CompareValue2 myValue2;

    public CompareValue1() {
        this.i = 3;
        this.l = 4;
        this.o = 5;
        this.myValue2 = new CompareValue2();
    }

    public CompareValue1(long l) {
        this.i = 3;
        this.l = l;
        this.o = 5;
        this.myValue2 = new CompareValue2();
    }
}