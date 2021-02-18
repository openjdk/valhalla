package org.openjdk.bench.valhalla.sandbox.corelibs;

public primitive class PrimitiveInt {
    int value;

    PrimitiveInt(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    public String toString() {
        return Integer.toString(value);
    }
}
