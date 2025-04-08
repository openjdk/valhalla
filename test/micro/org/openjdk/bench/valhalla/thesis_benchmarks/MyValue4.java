package org.openjdk.bench.valhalla.thesis_benchmarks;
import org.openjdk.jmh.annotations.CompilerControl;
value class MyValue4 implements MyInterface1 {
    public int x1;
    public int x2;
    public int x3;
    public int x4;

    public MyValue4(int i) {
        this.x1 = i;
        this.x2 = i;
        this.x3 = i;
        this.x4 = i;
    }
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Override
    public int getValue() {
        return x4;
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public MyValue4 test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
        return new MyValue4(x1 + x2 + x3 + x4 + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6);
    }
}