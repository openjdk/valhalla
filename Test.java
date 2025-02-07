import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

public class Test {

// TODO the null-free version of this is always naturally atomic

    @ImplicitlyConstructible
    @LooselyConsistentValue
    public static value class MyValue1 {
        byte b;

        public MyValue1(byte b) {
            this.b = b;
        }
    }

    public static void testWrite1(MyValue1[] array, int i, MyValue1 val) {
        array[i] = val;
    }

    public static MyValue1 testRead1(MyValue1[] array, int i) {
        return array[i];
    }

    static final MyValue1 CANARY = new MyValue1((byte)42);

    public static void checkCanary(MyValue1[] array) {
        if (array[1] != CANARY) {
            throw new RuntimeException("The canary died :(");
        }
    }

    public static void main(String[] args) {
    // TODO we need to intrinsify all these
        MyValue1[] nullFreeArray = (MyValue1[])ValueClass.newNullRestrictedArray(MyValue1.class, 2);
        MyValue1[] nullFreeAtomicArray = (MyValue1[])ValueClass.newNullRestrictedAtomicArray(MyValue1.class, 2);
        MyValue1[] nullableArray = new MyValue1[2];
        MyValue1[] nullableAtomicArray = (MyValue1[])ValueClass.newNullableAtomicArray(MyValue1.class, 2);

        // Write canary values to detect out of bound writes
        nullFreeArray[1] = CANARY;
        nullFreeAtomicArray[1] = CANARY;
        nullableArray[1] = CANARY;
        nullableAtomicArray[1] = CANARY;

        for (int i = 0; i < 100_000; ++i) {
            byte theB = (byte)i;
            testWrite1(nullFreeArray, 0, new MyValue1(theB));
            if (testRead1(nullFreeArray, 0).b != theB) {
                throw new RuntimeException("FAIL");
            }
            checkCanary(nullFreeArray);
            
            testWrite1(nullFreeAtomicArray, 0, new MyValue1(theB));
            if (testRead1(nullFreeAtomicArray, 0).b != theB) {
                throw new RuntimeException("FAIL");
            }
            checkCanary(nullFreeAtomicArray);

            testWrite1(nullableArray, 0, new MyValue1(theB));
            if (testRead1(nullableArray, 0).b != theB) {
                throw new RuntimeException("FAIL");
            }
            checkCanary(nullableArray);
            testWrite1(nullableArray, 0, null);
            if (testRead1(nullableArray, 0) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary(nullableArray);
            
            testWrite1(nullableAtomicArray, 0, new MyValue1(theB));
            if (testRead1(nullableAtomicArray, 0).b != theB) {
                throw new RuntimeException("FAIL");
            }
            checkCanary(nullableAtomicArray);
            testWrite1(nullableAtomicArray, 0, null);
            if (testRead1(nullableAtomicArray, 0) != null) {
                throw new RuntimeException("FAIL");
            }
            checkCanary(nullableAtomicArray);
        }
        try {
            testWrite1(nullFreeArray, 0, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary(nullFreeArray);
        try {
            testWrite1(nullFreeAtomicArray, 0, null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        checkCanary(nullFreeAtomicArray);
    }
}
