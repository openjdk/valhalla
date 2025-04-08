package org.openjdk.bench.valhalla.thesis_benchmarks;

import org.openjdk.jmh.annotations.CompilerControl;

interface MyInterface1 {

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public MyInterface1 test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6);

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int getValue();
}