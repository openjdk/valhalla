package oracle.micro.valhalla.lworld.types;

import oracle.micro.valhalla.types.Total;

public class Wrapper2 implements Total {

    public __Flattenable final Value2 value;

    public Wrapper2(Value2 value) {
        this.value = value;
    }

    public Wrapper2(int f0, int f1) {
        this.value = Value2.of(f0, f1);
    }

    public int f0() {
        return value.f0();
    }
    public int f1() {
        return value.f1();
    }

    public int re() {
        return value.re();
    }

    public int im() {
        return value.im();
    }

    public Wrapper2 add(Wrapper2 v) {
        return new Wrapper2(value.add(v.value));
    }

    public Wrapper2 mul(Wrapper2 v) {
        return new Wrapper2(value.mul(v.value));
    }

    // Used to provide usages of both fields in bechmarks
    public int totalsum() {
        return value.totalsum();
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
