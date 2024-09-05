import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.CountDownLatch;

public class Test {

    static value class SmallValue {
        int x1;
        int x2;
        
        public SmallValue(int i) {
            this.x1 = i;
            this.x2 = i;
        }

        public String toString() {
            return "x1 = " + x1 + ", x2 = " + x2;
        }

        public void verify(String loc, int i) {
            if (x1 != i || x2 != i) {
                new RuntimeException("Incorrect result at " + loc + " for i = " + i + ": " + this).printStackTrace(System.out);
                System.exit(1);
            }
        }
    }

    // Large value class that requires stack extension/repair
    static value class LargeValue {
        int x1;
        int x2;
        int x3;
        int x4;
        int x5;
        int x6;
        int x7;
        int x8;
        int x9;
        int x10;
        
        public LargeValue(int i) {
            this.x1 = i;
            this.x2 = i;
            this.x3 = i;
            this.x4 = i;
            this.x5 = i;
            this.x6 = i;
            this.x7 = i;
            this.x8 = i;
            this.x9 = i;
            this.x10 = i;
        }

        public String toString() {
            return "x1 = " + x1 + ", x2 = " + x2 + ", x3 = " + x3 + ", x4 = " + x4 + ", x5 = " + x5 +
                   ", x6 = " + x6 + ", x7 = " + x7 + ", x8 = " + x8 + ", x9 = " + x9 + ", x10 = " + x10;
        }

        public void verify(String loc, int i) {
            if (x1 != i || x2 != i || x3 != i || x4 != i || x5 != i ||
                x6 != i || x7 != i || x8 != i || x9 != i || x10 != i) {
                new RuntimeException("Incorrect result at " + loc + " for i = " + i + ": " + this).printStackTrace(System.out);
                System.exit(1);
            }
        }
    }

    // Large value class with oops (and different number of fields) that requires stack extension/repair
    static value class LargeValueWithOops {
        Object x1;
        Object x2;
        Object x3;
        Object x4;
        Object x5;
        Object x6;
        Object x7;
        Object x8;
        Object x9;

        public LargeValueWithOops(Object obj) {
            this.x1 = obj;
            this.x2 = obj;
            this.x3 = obj;
            this.x4 = obj;
            this.x5 = obj;
            this.x6 = obj;
            this.x7 = obj;
            this.x8 = obj;
            this.x9 = obj;
        }

        public String toString() {
            return "x1 = " + x1 + ", x2 = " + x2 + ", x3 = " + x3 + ", x4 = " + x4 + ", x5 = " + x5 +
                   ", x6 = " + x6 + ", x7 = " + x7 + ", x8 = " + x8 + ", x9 = " + x9;
        }

        public void verify(String loc, Object obj) {
            if (x1 != obj || x2 != obj || x3 != obj || x4 != obj || x5 != obj ||
                x6 != obj || x7 != obj || x8 != obj || x9 != obj) {
                new RuntimeException("Incorrect result at " + loc + " for obj = " + obj + ": " + this).printStackTrace(System.out);
                System.exit(1);
            }
        }
    }

    public static void dontInline() { }

    public static SmallValue testSmall(SmallValue val, int i, boolean park) {
        val.verify("entry", i);
        if (park) {
            LockSupport.parkNanos(1000);
        }
        val.verify("exit", i);
        return val;
    }

    public static LargeValue testLarge(LargeValue val, int i, boolean park) {
        val.verify("entry", i);
        if (park) {
            LockSupport.parkNanos(1000);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        val.verify("exit", i);
        return val;
    }

    public static LargeValue testLargeHelper(int i, boolean park) {
        LargeValue val = testLarge(new LargeValue(i), i, park);
        val.verify("helper", i);
        return val;
    }

    // Version that already has values on the stack even before stack extensions
    public static LargeValue testLarge2(LargeValue val1, LargeValue val2, LargeValue val3, LargeValue val4, LargeValue val5, 
                                        LargeValue val6, LargeValue val7, LargeValue val8, LargeValue val9, LargeValue val10, int i, boolean park) {
        val1.verify("entry", i);
        val2.verify("entry", i + 1);
        val3.verify("entry", i + 2);
        val4.verify("entry", i + 3);
        val5.verify("entry", i + 4);
        val6.verify("entry", i + 5);
        val7.verify("entry", i + 6);
        val8.verify("entry", i + 7);
        val9.verify("entry", i + 8);
        val10.verify("entry", i + 9);
        if (park) {
            LockSupport.parkNanos(1000);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        val1.verify("exit", i);
        val2.verify("exit", i + 1);
        val3.verify("exit", i + 2);
        val4.verify("exit", i + 3);
        val5.verify("exit", i + 4);
        val6.verify("exit", i + 5);
        val7.verify("exit", i + 6);
        val8.verify("exit", i + 7);
        val9.verify("exit", i + 8);
        val10.verify("exit", i + 9);
        return val5;
    }

    public static LargeValue testLarge2Helper(int i, boolean park) {
        LargeValue val = testLarge2(new LargeValue(i), new LargeValue(i + 1), new LargeValue(i + 2), new LargeValue(i + 3), new LargeValue(i + 4),
                                    new LargeValue(i + 5), new LargeValue(i + 6), new LargeValue(i + 7), new LargeValue(i + 8), new LargeValue(i + 9), i, park);
        val.verify("helper", i + 4);
        return val;
    }

    public static LargeValueWithOops testLargeWithOops(LargeValueWithOops val, Object obj, boolean park) {
        val.verify("entry", obj);
        if (park) {
            LockSupport.parkNanos(1000);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        val.verify("exit", obj);
        return val;
    }

    public static LargeValueWithOops testLargeWithOopsHelper(Object obj, boolean park) {
        LargeValueWithOops val = testLargeWithOops(new LargeValueWithOops(obj), obj, park);
        val.verify("helper", obj);
        return val;
    }

    // Version that already has values on the stack even before stack extensions
    public static LargeValueWithOops testLargeWithOops2(LargeValueWithOops val1, LargeValueWithOops val2, LargeValueWithOops val3, LargeValueWithOops val4, LargeValueWithOops val5,
                                                        LargeValueWithOops val6, LargeValueWithOops val7, LargeValueWithOops val8, LargeValueWithOops val9, Object obj, boolean park) {
        val1.verify("entry", obj);
        val2.verify("entry", obj);
        val3.verify("entry", obj);
        val4.verify("entry", obj);
        val5.verify("entry", obj);
        val6.verify("entry", obj);
        val7.verify("entry", obj);
        val8.verify("entry", obj);
        val9.verify("entry", obj);
        if (park) {
            LockSupport.parkNanos(1000);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        val1.verify("exit", obj);
        val2.verify("exit", obj);
        val3.verify("exit", obj);
        val4.verify("exit", obj);
        val5.verify("exit", obj);
        val6.verify("exit", obj);
        val7.verify("exit", obj);
        val8.verify("exit", obj);
        val9.verify("exit", obj);
        return val5;
    }

    public static LargeValueWithOops testLargeWithOops2Helper(Object obj, boolean park) {
        LargeValueWithOops val = testLargeWithOops2(new LargeValueWithOops(obj), new LargeValueWithOops(obj), new LargeValueWithOops(obj), new LargeValueWithOops(obj), new LargeValueWithOops(obj),
                                                    new LargeValueWithOops(obj), new LargeValueWithOops(obj), new LargeValueWithOops(obj), new LargeValueWithOops(obj), obj, park);
        val.verify("helper", obj);
        return val;
    }

    public static int testLargePOJO(boolean isInit, int x1, int x2, int x3, int x4, int x5, 
                                    int x6, int x7, int x8, int x9, int x10, int i, boolean park) {
        if (park) {
            LockSupport.parkNanos(1000);
        }
        if (x1 != i || x2 != i || x3 != i || x4 != i || x5 != i ||
            x6 != i || x7 != i || x8 != i || x9 != i || x10 != i) {
            new RuntimeException("Incorrect result for i = " + i).printStackTrace(System.out);
            System.out.println("x1 = " + x1 + ", x2 = " + x2 + ", x3 = " + x3 + ", x4 = " + x4 + ", x5 = " + x5 +
                               ", x6 = " + x6 + ", x7 = " + x7 + ", x8 = " + x8 + ", x9 = " + x9 + ", x10 = " + x10);
            System.exit(1);
        }
        return x1;
    }

    public static int testLargeHelperPOJO(int i, boolean park) {
        return testLargePOJO(true, i, i, i, i, i, i, i, i, i, i, i, park);
    }
    
    // Bottom frame is testLargeHelper (does not need repair)
    // java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::testLargeHelper -XX:CompileCommand=compileonly,*::test* -XX:-UseOnStackReplacement Test.java
    // Bottom frame is
    // java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=compileonly,*::testLarge -XX:-UseOnStackReplacement Test.java

/*
// TODO run with and without PreserveFramePointer
// TODO run with -XX:CompileCommand=dontinline,*::*verify
// Variants of -XX:+StressCallingConvention -XX:-InlineTypePassFieldsAsArgs -XX:+InlineTypeReturnedAsFields

FAILS:
../build/fastdebug/jdk/bin/java --enable-preview -Xcomp -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=compileonly,Test*::* Test.java
../build/fastdebug/jdk/bin/java --enable-preview -Xcomp -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=compileonly,*::testLarge* -XX:+TraceDeoptimization Test.java
../build/fastdebug/jdk/bin/java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=dontinline,*::test* -XX:CompileCommand=compileonly,*::test* -XX:-UseOnStackReplacement Test.java

WORKS:
../build/fastdebug/jdk/bin/java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::dontinline Test.java &&
../build/fastdebug/jdk/bin/java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=dontinline,*::testSmall -XX:CompileCommand=dontinline,*::testLargeHelper -XX:CompileCommand=dontinline,*::testLarge2Helper -XX:CompileCommand=compileonly,*::test* -XX:CompileCommand=dontinline,*::testLargeWithOopsHelper -XX:-UseOnStackReplacement -XX:CompileCommand=dontinline,*::testLargeWithOops2Helper Test.java &&
../build/fastdebug/jdk/bin/java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,*::testLarge -XX:-UseOnStackReplacement Test.java &&
../build/fastdebug/jdk/bin/java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,*::testLarge2 -XX:-UseOnStackReplacement Test.java &&
../build/fastdebug/jdk/bin/java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,*::testLargeWithOops -XX:-UseOnStackReplacement Test.java &&
../build/fastdebug/jdk/bin/java --enable-preview -Xbatch -XX:CompileCommand=quiet -XX:-TieredCompilation -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,*::testLargeWithOops2 -XX:-UseOnStackReplacement Test.java
*/

    public static void main(String[] args) throws Exception {
    // TODO need more variants with interpreted and C1 compiled callers, deopts (including -XX:+DeoptimizeALot), etc.
    // TODO add variants with oops
        CountDownLatch cdl = new CountDownLatch(1);
        Thread.ofVirtual().name("vt1").start(() -> {
            // Trigger compilation
            for (int i = 0; i < 500_000; i++) {
                testSmall(new SmallValue(i), i, (i % 1000) == 0).verify("return", i);
                testLargeHelper(i, (i % 1000) == 0).verify("return", i);
                testLarge2Helper(i, (i % 1000) == 0).verify("return", i + 4);

                // TODO enable
                // testLargeWithOopsHelper(new SmallValue(i), (i % 1000) == 0).verify("return", obj);
                // testLargeWithOops2Helper(new SmallValue(i), (i % 1000) == 0).verify("return", obj);

                //testLargeHelperPOJO(i, (i % 1000) == 0);//.verify("return", i);
            }
            cdl.countDown();
        });
        cdl.await();
    }
}
