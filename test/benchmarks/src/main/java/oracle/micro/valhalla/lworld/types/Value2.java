package oracle.micro.valhalla.lworld.types;

import oracle.micro.valhalla.types.PNumber;

__ByValue public final class Value2 implements PNumber {

    public final int f0;
    public final int f1;

    private Value2() {
        this.f0 =  0;
        this.f1 =  0;
    }

    public static Value2 of(int f0, int f1) {
        Value2 v = __MakeDefault Value2();
        v = __WithField(v.f0, f0);
        v = __WithField(v.f1, f1);
        return v;
    }

    public int f0() {
        return f0;
    }
    public int f1() {
        return f1;
    }

    public int re() {
        return f0;
    }

    public int im() {
        return f1;
    }

    public Value2 add(Value2 v) {
        return of(this.f0 + v.f0, this.f1 + v.f1);
    }

    public Value2 mul(Value2 v) {
        int tre = this.f0;
        int tim = this.f1;
        int vre = v.f0;
        int vim = v.f1;
        return of(tre * vre - tim * vim, tre * vim + vre * tim);
    }
    // Used to provide usages of both fields in bechmarks
    public int totalsum() {
        return f0 + f1 ;
    }

    @Override
    public int hashCode() {
        return f0 + f1;
    }

    @Override
    public Value2 inc() {
        return of(f1, f0 + 1);
    }

    @Override
    public Value2 dec() {
        return of(f1, f0 - 1);
    }



}
