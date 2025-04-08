package org.openjdk.bench.valhalla.thesis_benchmarks;
import org.openjdk.jmh.annotations.CompilerControl;
value class MyValue3 implements MyInterface1 {
    public double d1;
    public double d2;
    public double d3;
    public double d4;

    public MyValue3(double d) {
        this.d1 = d;
        this.d2 = d;
        this.d3 = d;
        this.d4 = d;
    }
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Override
    public int getValue() {
        return (int)d4;
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public MyValue3 test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
        return new MyValue3(d1 + d2 + d3 + d4 + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6);
    }
}