package oracle.micro.valhalla.lworld.types;

import oracle.micro.valhalla.types.Total;

public class Wrapper1 implements Total {

    public __Flattenable final Value1 value;

    public Wrapper1(Value1 value) {
        this.value = value;
    }

    public Wrapper1(int f0) {
        this.value = Value1.of(f0);
    }

    public int f0() {
        return value.f0();
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
