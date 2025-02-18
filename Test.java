import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

public class Test {

    // Using two bytes such that null-free fields will not be naturally atomic
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class TwoBytes {
        byte b1;
        byte b2;

        public TwoBytes(byte b1, byte b2) {
            this.b1 = b1;
            this.b2 = b2;
        }
    }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class TwoShorts {
        short s1;
        short s2;

        public TwoShorts(short s1, short s2) {
            this.s1 = s1;
            this.s2 = s2;
        }
    }

    public static void testWrite1(TwoBytes[] array, int i, TwoBytes val) {
        array[i] = val;
    }

    public static void testWrite2(TwoShorts[] array, int i, TwoShorts val) {
        array[i] = val;
    }

    public static TwoBytes testRead1(TwoBytes[] array, int i) {
        return array[i];
    }

    public static TwoShorts testRead2(TwoShorts[] array, int i) {
        return array[i];
    }

    static final TwoBytes CANARY1 = new TwoBytes((byte)42, (byte)42);

    public static void checkCanary1(TwoBytes[] array) {
        if (array[0] != CANARY1) {
            throw new RuntimeException("The canary died :(");
        }
        if (array[2] != CANARY1) {
            throw new RuntimeException("The canary died :(");
        }
    }

    static final TwoShorts CANARY2 = new TwoShorts((short)42, (short)42);

    public static void checkCanary2(TwoShorts[] array) {
        if (array[0] != CANARY2) {
            throw new RuntimeException("The canary died :(");
        }
        if (array[2] != CANARY2) {
            throw new RuntimeException("The canary died :(");
        }
    }

// TODO we need to check that the thing that is returned is correct (especially for atomic!)

    public static TwoBytes[] testNullRestrictedArrayIntrinsic(int size, int idx, TwoBytes val) {
        TwoBytes[] nullFreeArray = (TwoBytes[])ValueClass.newNullRestrictedArray(TwoBytes.class, size);
        testWrite1(nullFreeArray, idx, val);
        if (testRead1(nullFreeArray, idx) != val) {
            throw new RuntimeException("FAIL");
        }
        return nullFreeArray;
    }

    public static TwoBytes[] testNullRestrictedAtomicArrayIntrinsic(int size, int idx, TwoBytes val) {
        TwoBytes[] nullFreeArray = (TwoBytes[])ValueClass.newNullRestrictedAtomicArray(TwoBytes.class, size);
        testWrite1(nullFreeArray, idx, val);
        if (testRead1(nullFreeArray, idx) != val) {
            throw new RuntimeException("FAIL");
        }
        return nullFreeArray;
    }

    public static TwoBytes[] testNullableAtomicArrayIntrinsic(int size, int idx, TwoBytes val) {
        TwoBytes[] nullFreeArray = (TwoBytes[])ValueClass.newNullableAtomicArray(TwoBytes.class, size);
        testWrite1(nullFreeArray, idx, val);
        if (testRead1(nullFreeArray, idx) != val) {
            throw new RuntimeException("FAIL");
        }
        return nullFreeArray;
    }

    public static void main(String[] args) {
        TwoBytes[] nullFreeArray1 = (TwoBytes[])ValueClass.newNullRestrictedArray(TwoBytes.class, 3);
        TwoBytes[] nullFreeAtomicArray1 = (TwoBytes[])ValueClass.newNullRestrictedAtomicArray(TwoBytes.class, 3);
        TwoBytes[] nullableArray1 = new TwoBytes[3];
        TwoBytes[] nullableAtomicArray1 = (TwoBytes[])ValueClass.newNullableAtomicArray(TwoBytes.class, 3);

        TwoShorts[] nullFreeArray2 = (TwoShorts[])ValueClass.newNullRestrictedArray(TwoShorts.class, 3);
        TwoShorts[] nullFreeAtomicArray2 = (TwoShorts[])ValueClass.newNullRestrictedAtomicArray(TwoShorts.class, 3);
        TwoShorts[] nullableArray2 = new TwoShorts[3];
        TwoShorts[] nullableAtomicArray2 = (TwoShorts[])ValueClass.newNullableAtomicArray(TwoShorts.class, 3);

        // Write canary values to detect out of bound writes
        nullFreeArray1[0] = CANARY1;
        nullFreeArray1[2] = CANARY1;
        nullFreeAtomicArray1[0] = CANARY1;
        nullFreeAtomicArray1[2] = CANARY1;
        nullableArray1[0] = CANARY1;
        nullableArray1[2] = CANARY1;
        nullableAtomicArray1[0] = CANARY1;
        nullableAtomicArray1[2] = CANARY1;

        nullFreeArray2[0] = CANARY2;
        nullFreeArray2[2] = CANARY2;
        nullFreeAtomicArray2[0] = CANARY2;
        nullFreeAtomicArray2[2] = CANARY2;
        nullableArray2[0] = CANARY2;
        nullableArray2[2] = CANARY2;
        nullableAtomicArray2[0] = CANARY2;
        nullableAtomicArray2[2] = CANARY2;

        for (int i = 0; i < 100_000; ++i) {
            TwoBytes val1 = new TwoBytes((byte)i, (byte)(i + 1));
            TwoShorts val2 = new TwoShorts((short)i, (short)(i + 1));
            testWrite1(nullFreeArray1, 1, val1);
            if (testRead1(nullFreeArray1, 1) != val1) {
                throw new RuntimeException("FAIL");
            }
            checkCanary1(nullFreeArray1);

            testWrite1(nullFreeAtomicArray1, 1, val1);
            if (testRead1(nullFreeAtomicArray1, 1) != val1) {
                throw new RuntimeException("FAIL");
            }
            checkCanary1(nullFreeAtomicArray1);

            testWrite1(nullableArray1, 1, val1);
            if (testRead1(nullableArray1, 1) != val1) {
                throw new RuntimeException("FAIL");
            }
            checkCanary1(nullableArray1);
            testWrite1(nullableArray1, 1, null);
            if (testRead1(nullableArray1, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary1(nullableArray1);
            
            testWrite1(nullableAtomicArray1, 1, val1);
            if (testRead1(nullableAtomicArray1, 1) != val1) {
                throw new RuntimeException("FAIL");
            }
            checkCanary1(nullableAtomicArray1);
            testWrite1(nullableAtomicArray1, 1, null);
            if (testRead1(nullableAtomicArray1, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary1(nullableAtomicArray1);

            testWrite2(nullFreeArray2, 1, val2);
            if (testRead2(nullFreeArray2, 1) != val2) {
                throw new RuntimeException("FAIL");
            }
            checkCanary2(nullFreeArray2);

            testWrite2(nullFreeAtomicArray2, 1, val2);
            if (testRead2(nullFreeAtomicArray2, 1) != val2) {
                throw new RuntimeException("FAIL");
            }
            checkCanary2(nullFreeAtomicArray2);

            testWrite2(nullableArray2, 1, val2);
            if (testRead2(nullableArray2, 1) != val2) {
                throw new RuntimeException("FAIL");
            }
            checkCanary2(nullableArray2);
            testWrite2(nullableArray2, 1, null);
            if (testRead2(nullableArray2, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary2(nullableArray2);
            
            testWrite2(nullableAtomicArray2, 1, val2);
            if (testRead2(nullableAtomicArray2, 1) != val2) {
                throw new RuntimeException("FAIL");
            }
            checkCanary2(nullableAtomicArray2);
            testWrite2(nullableAtomicArray2, 1, null);
            if (testRead2(nullableAtomicArray2, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary2(nullableAtomicArray2);

            // Test intrinsics
            TwoBytes[] res = testNullRestrictedArrayIntrinsic(3, 1, val1);
            if (testRead1(res, 1) != val1) {
                throw new RuntimeException("FAIL");
            }
            res = testNullRestrictedAtomicArrayIntrinsic(3, 1, val1);
            if (testRead1(res, 1) != val1) {
                throw new RuntimeException("FAIL");
            }
            res = testNullableAtomicArrayIntrinsic(3, 1, val1);
            if (testRead1(res, 1) != val1) {
                throw new RuntimeException("FAIL");
            }
            res = testNullableAtomicArrayIntrinsic(3, 2, null);
            if (testRead1(res, 2) != null) {
                throw new RuntimeException("FAIL");
            }
        }
        try {
            testWrite1(nullFreeArray1, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary1(nullFreeArray1);
        try {
            testWrite1(nullFreeAtomicArray1, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary1(nullFreeAtomicArray1);

        try {
            testWrite2(nullFreeArray2, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary2(nullFreeArray2);
        try {
            testWrite2(nullFreeAtomicArray2, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary2(nullFreeAtomicArray2);

        // Test intrinsics
        try {
            testNullRestrictedArrayIntrinsic(3, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            testNullRestrictedAtomicArrayIntrinsic(3, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }
}
