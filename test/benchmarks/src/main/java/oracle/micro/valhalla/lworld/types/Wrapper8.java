package oracle.micro.valhalla.lworld.types;

import oracle.micro.valhalla.types.Total;

public class Wrapper8 implements Total {

    public __Flattenable final Value8 value;

    public Wrapper8(Value8 value) {
        this.value = value;
    }

    public Wrapper8(int f0, int f1, int f2, int f3, int f4, int f5, int f6, int f7) {
        this.value = Value8.of(f0, f1, f2, f3, f4, f5, f6, f7);
    }

    public int f0() {   return value.f0();   }
    public int f1() {   return value.f1();   }
    public int f2() {   return value.f2();   }
    public int f3() {   return value.f3();   }
    public int f4() {   return value.f4();   }
    public int f5() {   return value.f5();   }
    public int f6() {   return value.f6();   }
    public int f7() {   return value.f7();   }

    // Used to provide usages of both fields in bechmarks
    public int totalsum() {
        return value.totalsum();
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
