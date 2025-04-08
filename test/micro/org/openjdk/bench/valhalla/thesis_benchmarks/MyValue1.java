package org.openjdk.bench.valhalla.thesis_benchmarks;
import org.openjdk.jmh.annotations.CompilerControl;
value class MyValue1 implements MyInterface1 {
    public int x;

    public MyValue1(int x) {
        this.x = x;
    }
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Override
    public int getValue() {
        return x;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public MyValue1 test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
        return new MyValue1(x + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6);
    }
}