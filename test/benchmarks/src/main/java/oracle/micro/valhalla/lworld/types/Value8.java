package oracle.micro.valhalla.lworld.types;

import oracle.micro.valhalla.types.PNumber;

value public final class Value8 implements PNumber {

    public final int f0;
    public final int f1;
    public final int f2;
    public final int f3;
    public final int f4;
    public final int f5;
    public final int f6;
    public final int f7;

    private Value8() {
        this.f0 = 0;
        this.f1 = 0;
        this.f2 = 0;
        this.f3 = 0;
        this.f4 = 0;
        this.f5 = 0;
        this.f6 = 0;
        this.f7 = 0;
    }

    public static Value8 of(int f0,int f1,int f2,int f3,int f4,int f5,int f6,int f7) {
        Value8 v = Value8.default;
        v = __WithField(v.f0, f0);
        v = __WithField(v.f1, f1);
        v = __WithField(v.f2, f2);
        v = __WithField(v.f3, f3);
        v = __WithField(v.f4, f4);
        v = __WithField(v.f5, f5);
        v = __WithField(v.f6, f6);
        v = __WithField(v.f7, f7);
        return v;
    }

    public int f0() {   return f0;   }
    public int f1() {   return f1;   }
    public int f2() {   return f2;   }
    public int f3() {   return f3;   }
    public int f4() {   return f4;   }
    public int f5() {   return f5;   }
    public int f6() {   return f6;   }
    public int f7() {   return f7;   }

    public Value8 add(Value8 v) {
        return of(this.f0 + v.f0, this.f1 + v.f1, this.f2 + v.f2, this.f3 + v.f3, this.f4 + v.f4, this.f5 + v.f5, this.f6 + v.f6, this.f7 + v.f7);
    }

    public int totalsum() {
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @Override
    public int hashCode() {
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @Override
    public Value8 inc() {
        return of(f1, f2, f3, f4, f5, f6, f7, f0 + 1);
    }

    @Override
    public Value8 dec() {
        return of(f1, f2, f3, f4, f5, f6, f7, f0 - 1);
    }

}
