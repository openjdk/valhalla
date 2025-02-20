import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

public class Test {

    // Has null-free, non-atomic, flat (2 bytes), null-free, atomic, flat (2 bytes) and nullable, atomic, flat (4 bytes) layouts
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

    // Has null-free, non-atomic, flat (4 bytes), null-free, atomic, flat (4 bytes) and nullable, atomic, flat (8 bytes) layouts
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

    // Has null-free, non-atomic flat (8 bytes) and null-free, atomic, flat (8 bytes) layouts
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class TwoInts {
        int i1;
        int i2;

        public TwoInts(int i1, int i2) {
            this.i1 = i1;
            this.i2 = i2;
        }
    }

    // Has null-free, non-atomic flat (16 bytes) layout
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class TwoLongs {
        long l1;
        long l2;

        public TwoLongs(int l1, int l2) {
            this.l1 = l1;
            this.l2 = l2;
        }
    }

    // Has null-free, non-atomic, flat (5 bytes), null-free, atomic, flat (8 bytes) and nullable, atomic, flat (8 bytes) layouts
    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class ByteAndOop {
        byte b;
        MyClass obj;

        public ByteAndOop(byte b, MyClass obj) {
            this.b = b;
            this.obj = obj;
        }
    }

    static class MyClass {
        int x;

        public MyClass(int x) {
            this.x = x;
        }
    }

    public static void testWrite1(TwoBytes[] array, int i, TwoBytes val) {
        array[i] = val;
    }

    public static void testWrite2(TwoShorts[] array, int i, TwoShorts val) {
        array[i] = val;
    }

    public static void testWrite3(TwoInts[] array, int i, TwoInts val) {
        array[i] = val;
    }

    public static void testWrite4(TwoLongs[] array, int i, TwoLongs val) {
        array[i] = val;
    }

    public static void testWrite5(ByteAndOop[] array, int i, ByteAndOop val) {
        array[i] = val;
    }

    public static TwoBytes testRead1(TwoBytes[] array, int i) {
        return array[i];
    }

    public static TwoShorts testRead2(TwoShorts[] array, int i) {
        return array[i];
    }

    public static TwoInts testRead3(TwoInts[] array, int i) {
        return array[i];
    }

    public static TwoLongs testRead4(TwoLongs[] array, int i) {
        return array[i];
    }

    public static ByteAndOop testRead5(ByteAndOop[] array, int i) {
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

    static final TwoInts CANARY3 = new TwoInts(42, 42);

    public static void checkCanary3(TwoInts[] array) {
        if (array[0] != CANARY3) {
            throw new RuntimeException("The canary died :(");
        }
        if (array[2] != CANARY3) {
            throw new RuntimeException("The canary died :(");
        }
    }

    static final TwoLongs CANARY4 = new TwoLongs(42, 42);

    public static void checkCanary4(TwoLongs[] array) {
        if (array[0] != CANARY4) {
            throw new RuntimeException("The canary died :(");
        }
        if (array[2] != CANARY4) {
            throw new RuntimeException("The canary died :(");
        }
    }

    static final ByteAndOop CANARY5 = new ByteAndOop((byte)42, new MyClass(42));

    public static void checkCanary5(ByteAndOop[] array) {
        if (array[0] != CANARY5) {
            throw new RuntimeException("The canary died :(");
        }
        if (array[2] != CANARY5) {
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

    static void produceGarbage() {
        for (int i = 0; i < 100; ++i) {
            Object[] arrays = new Object[1024];
            for (int j = 0; j < arrays.length; j++) {
                arrays[j] = new int[1024];
            }
        }
        System.gc();
    }

    static TwoShorts[] array1 = (TwoShorts[])ValueClass.newNullRestrictedAtomicArray(TwoShorts.class, 1);
    static TwoShorts[] array2 = (TwoShorts[])ValueClass.newNullableAtomicArray(TwoShorts.class, 1);
    static {
        array2[0] = new TwoShorts((short)0, (short)0);
    }
    static TwoShorts[] array3 = new TwoShorts[] { new TwoShorts((short)0, (short)0) };

    // Catches an issue with type speculation based on profiling
    public static void testProfiling() {
        array1[0] = new TwoShorts(array1[0].s1, (short)0);
        array2[0] = new TwoShorts(array2[0].s1, (short)0);
        array3[0] = new TwoShorts(array3[0].s1, (short)0);
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

        TwoInts[] nullFreeArray3 = (TwoInts[])ValueClass.newNullRestrictedArray(TwoInts.class, 3);
        TwoInts[] nullFreeAtomicArray3 = (TwoInts[])ValueClass.newNullRestrictedAtomicArray(TwoInts.class, 3);
        TwoInts[] nullableArray3 = new TwoInts[3];
        TwoInts[] nullableAtomicArray3 = (TwoInts[])ValueClass.newNullableAtomicArray(TwoInts.class, 3);

        TwoLongs[] nullFreeArray4 = (TwoLongs[])ValueClass.newNullRestrictedArray(TwoLongs.class, 3);
        TwoLongs[] nullFreeAtomicArray4 = (TwoLongs[])ValueClass.newNullRestrictedAtomicArray(TwoLongs.class, 3);
        TwoLongs[] nullableArray4 = new TwoLongs[3];
        TwoLongs[] nullableAtomicArray4 = (TwoLongs[])ValueClass.newNullableAtomicArray(TwoLongs.class, 3);

        ByteAndOop[] nullFreeArray5 = (ByteAndOop[])ValueClass.newNullRestrictedArray(ByteAndOop.class, 3);
        ByteAndOop[] nullFreeAtomicArray5 = (ByteAndOop[])ValueClass.newNullRestrictedAtomicArray(ByteAndOop.class, 3);
        ByteAndOop[] nullableArray5 = new ByteAndOop[3];
        ByteAndOop[] nullableAtomicArray5 = (ByteAndOop[])ValueClass.newNullableAtomicArray(ByteAndOop.class, 3);

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

        nullFreeArray3[0] = CANARY3;
        nullFreeArray3[2] = CANARY3;
        nullFreeAtomicArray3[0] = CANARY3;
        nullFreeAtomicArray3[2] = CANARY3;
        nullableArray3[0] = CANARY3;
        nullableArray3[2] = CANARY3;
        nullableAtomicArray3[0] = CANARY3;
        nullableAtomicArray3[2] = CANARY3;

        nullFreeArray4[0] = CANARY4;
        nullFreeArray4[2] = CANARY4;
        nullFreeAtomicArray4[0] = CANARY4;
        nullFreeAtomicArray4[2] = CANARY4;
        nullableArray4[0] = CANARY4;
        nullableArray4[2] = CANARY4;
        nullableAtomicArray4[0] = CANARY4;
        nullableAtomicArray4[2] = CANARY4;

        nullFreeArray5[0] = CANARY5;
        nullFreeArray5[2] = CANARY5;
        nullFreeAtomicArray5[0] = CANARY5;
        nullFreeAtomicArray5[2] = CANARY5;
        nullableArray5[0] = CANARY5;
        nullableArray5[2] = CANARY5;
        nullableAtomicArray5[0] = CANARY5;
        nullableAtomicArray5[2] = CANARY5;

        final int LIMIT = 50_000;
        for (int i = -50_000; i < LIMIT; ++i) {
            TwoBytes val1 = new TwoBytes((byte)i, (byte)(i + 1));
            TwoShorts val2 = new TwoShorts((short)i, (short)(i + 1));
            TwoInts val3 = new TwoInts(i, i + 1);
            TwoLongs val4 = new TwoLongs(i, i + 1);

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

            testWrite3(nullFreeArray3, 1, val3);
            if (testRead3(nullFreeArray3, 1) != val3) {
                throw new RuntimeException("FAIL");
            }
            checkCanary3(nullFreeArray3);

            testWrite3(nullFreeAtomicArray3, 1, val3);
            if (testRead3(nullFreeAtomicArray3, 1) != val3) {
                throw new RuntimeException("FAIL");
            }
            checkCanary3(nullFreeAtomicArray3);

            testWrite3(nullableArray3, 1, val3);
            if (testRead3(nullableArray3, 1) != val3) {
                throw new RuntimeException("FAIL");
            }
            checkCanary3(nullableArray3);
            testWrite3(nullableArray3, 1, null);
            if (testRead3(nullableArray3, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary3(nullableArray3);

            testWrite3(nullableAtomicArray3, 1, val3);
            if (testRead3(nullableAtomicArray3, 1) != val3) {
                throw new RuntimeException("FAIL");
            }
            checkCanary3(nullableAtomicArray3);
            testWrite3(nullableAtomicArray3, 1, null);
            if (testRead3(nullableAtomicArray3, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary3(nullableAtomicArray3);

            testWrite4(nullFreeArray4, 1, val4);
            if (testRead4(nullFreeArray4, 1) != val4) {
                throw new RuntimeException("FAIL");
            }
            checkCanary4(nullFreeArray4);

            testWrite4(nullFreeAtomicArray4, 1, val4);
            if (testRead4(nullFreeAtomicArray4, 1) != val4) {
                throw new RuntimeException("FAIL");
            }
            checkCanary4(nullFreeAtomicArray4);

            testWrite4(nullableArray4, 1, val4);
            if (testRead4(nullableArray4, 1) != val4) {
                throw new RuntimeException("FAIL");
            }
            checkCanary4(nullableArray4);
            testWrite4(nullableArray4, 1, null);
            if (testRead4(nullableArray4, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary4(nullableArray4);

            testWrite4(nullableAtomicArray4, 1, val4);
            if (testRead4(nullableAtomicArray4, 1) != val4) {
                throw new RuntimeException("FAIL");
            }
            checkCanary4(nullableAtomicArray4);
            testWrite4(nullableAtomicArray4, 1, null);
            if (testRead4(nullableAtomicArray4, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary4(nullableAtomicArray4);

            ByteAndOop val5 = new ByteAndOop((byte)i, new MyClass(i));
            testWrite5(nullFreeArray5, 1, val5);
            testWrite5(nullFreeAtomicArray5, 1, val5);
            testWrite5(nullableArray5, 1, val5);
            testWrite5(nullableAtomicArray5, 1, val5);

            if (i > (LIMIT - 50)) {
                // After warmup, produce some garbage to trigger GC
                produceGarbage();
            }

            if (testRead5(nullFreeArray5, 1) != val5) {
                throw new RuntimeException("FAIL");
            }
            checkCanary5(nullFreeArray5);

            if (testRead5(nullFreeAtomicArray5, 1) != val5) {
                throw new RuntimeException("FAIL");
            }
            checkCanary5(nullFreeAtomicArray5);

            if (testRead5(nullableArray5, 1) != val5) {
                throw new RuntimeException("FAIL");
            }
            checkCanary5(nullableArray5);

            testWrite5(nullableArray5, 1, null);
            if (testRead5(nullableArray5, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary5(nullableArray5);

            if (testRead5(nullableAtomicArray5, 1) != val5) {
                throw new RuntimeException("FAIL");
            }
            checkCanary5(nullableAtomicArray5);

            testWrite5(nullableAtomicArray5, 1, null);
            if (testRead5(nullableAtomicArray5, 1) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary5(nullableAtomicArray5);

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

            testProfiling();
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

        try {
            testWrite3(nullFreeArray3, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary3(nullFreeArray3);
        try {
            testWrite3(nullFreeAtomicArray3, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary3(nullFreeAtomicArray3);

        try {
            testWrite4(nullFreeArray4, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary4(nullFreeArray4);
        try {
            testWrite4(nullFreeAtomicArray4, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary4(nullFreeAtomicArray4);

        try {
            testWrite5(nullFreeArray5, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary5(nullFreeArray5);
        try {
            testWrite5(nullFreeAtomicArray5, 1, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary5(nullFreeAtomicArray5);

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
