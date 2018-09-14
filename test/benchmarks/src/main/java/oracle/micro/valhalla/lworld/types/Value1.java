package oracle.micro.valhalla.lworld.types;

import oracle.micro.valhalla.types.PNumber;

__ByValue public final class Value1 implements PNumber {

    public final int f0;

    private Value1() {
        this.f0 =  0;
    }

    public static Value1 of(int f0) {
        Value1 v = Value1.default;
        v = __WithField(v.f0, f0);
        return v;
    }

    public Value1 add(Value1 v) {
        return of(this.f0 + v.f0);
    }

    public int f0() {
        return f0;
    }

    // Used to provide usages of both fields in bechmarks
    public int totalsum() {
        return f0 ;
    }

    @Override
    public int hashCode() {
        return f0;
    }

    @Override
    public Value1 inc() {
        return of(f0 + 1);
    }

    @Override
    public Value1 dec() {
        return of(f0 - 1);
    }
}
