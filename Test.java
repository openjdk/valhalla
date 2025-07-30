// ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::test* -Xbatch -XX:-MonomorphicArrayCheck Test.java && ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::* -Xbatch -XX:-MonomorphicArrayCheck Test.java &&  ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::main -XX:CompileCommand=dontinline,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation -XX:CompileCommand=quiet -Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:-TieredCompilation -J-XX:CompileCommand=quiet -J-Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:-TieredCompilation -J-XX:CompileCommand=quiet -J-Xcomp -J-XX:+StressReflectiveCode Test.java && ../build/fastdebug/jdk/bin/java -XX:TieredStopAtLevel=3 -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::test* -Xbatch -XX:-MonomorphicArrayCheck Test.java && ../build/fastdebug/jdk/bin/java -XX:TieredStopAtLevel=3 -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::* -Xbatch -XX:-MonomorphicArrayCheck Test.java &&  ../build/fastdebug/jdk/bin/java -XX:TieredStopAtLevel=3 -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::main -XX:CompileCommand=dontinline,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java -XX:TieredStopAtLevel=3 -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java -XX:TieredStopAtLevel=3 -XX:CompileCommand=quiet -Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:TieredStopAtLevel=3 -J-XX:CompileCommand=quiet -J-Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:TieredStopAtLevel=3 -J-XX:CompileCommand=quiet -J-Xcomp -J-XX:+StressReflectiveCode Test.java && ../build/fastdebug/jdk/bin/java  -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::test* -Xbatch -XX:-MonomorphicArrayCheck Test.java && ../build/fastdebug/jdk/bin/java  -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::* -Xbatch -XX:-MonomorphicArrayCheck Test.java &&  ../build/fastdebug/jdk/bin/java  -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::main -XX:CompileCommand=dontinline,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java  -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java  -XX:CompileCommand=quiet -Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:CompileCommand=quiet -J-Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:CompileCommand=quiet -J-Xcomp -J-XX:+StressReflectiveCode Test.java && ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation --enable-preview -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::test* -Xbatch -XX:-MonomorphicArrayCheck Test.java && ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation --enable-preview -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::* -XX:CompileCommand=dontinline,Test::* -Xbatch -XX:-MonomorphicArrayCheck Test.java &&  ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation --enable-preview -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::main -XX:CompileCommand=dontinline,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation --enable-preview -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,Test::test* -Xbatch Test.java && ../build/fastdebug/jdk/bin/java -XX:-TieredCompilation --enable-preview -XX:CompileCommand=quiet -Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:-TieredCompilation -source 25 --enable-preview -J-XX:CompileCommand=quiet -J-Xcomp Test.java && ../build/fastdebug/jdk/bin/javac -J-XX:-TieredCompilation -source 25 --enable-preview -J-XX:CompileCommand=quiet -J-Xcomp -J-XX:+StressReflectiveCode Test.java

// TODO Tobias run with MonomorphicArrayCheck enabled
// TODO Tobias with and without -XX:-UseTLAB and -XX:MultiArrayExpandLimit=0
// TODO Tobias run with +StressReflectiveCode
// TODO Tobias run with large PerMethodTrapLimit and also spec trap one
// TODO Tobias add result verification
// TODO Tobias value class (arrays), also check if result is flat, null-free etc.

// TODO Tobias run with enable preview

import java.lang.reflect.Array;
import java.util.Arrays;

public class Test {

    static interface MyInterface {

    }

    public static Object[] testArrayAllocation1() {
        return new Object[1];
    }

    public static Object[][] testArrayAllocation2() {
        return new Object[1][1];
    }

    public static Class getClass1() {
        return Object.class;
    }

    public static Object[] testArrayAllocation3() {
        return (Object[])Array.newInstance(getClass1(), 1);
    }

    public static Class getClass2() {
        return Test.class;
    }

    public static Object[] testArrayAllocation4() {
        return (Test[])Array.newInstance(getClass2(), 1);
    }

    public static Object[] testArrayAllocation5() {
        return new MyInterface[1];
    }

    public static Object[] testArrayAllocation6() {
        return new Integer[1];
    }

    public static Object[] testCheckcast1(Object arg) {
        return (Object[])arg;
    }

    public static Object[] testCheckcast2(Object arg) {
        return (Test[])arg;
    }

    public static Cloneable testCheckcast3(Object arg) {
        return (Cloneable)arg;
    }

    public static Object testCheckcast4(Object arg) {
        return (Object)arg;
    }

    public static Object[][] testCheckcast5(Object arg) {
        return (Object[][])arg;
    }

    public static MyInterface[] testCheckcast6(Object arg) {
        return (MyInterface[])arg;
    }

    public static Integer[] testCheckcast7(Object arg) {
        return (Integer[])arg;
    }

    public static Class getArrayClass1() {
        return Object[].class;
    }

    public static boolean testIsInstance1(Object arg) {
        return getArrayClass1().isInstance(arg);
    }

    public static Class getArrayClass2() {
        return Test[].class;
    }

    public static boolean testIsInstance2(Object arg) {
        return getArrayClass2().isInstance(arg);
    }

    public static Class getArrayClass3() {
        return Cloneable.class;
    }

    public static boolean testIsInstance3(Object arg) {
        return getArrayClass3().isInstance(arg);
    }

    public static Class getArrayClass4() {
        return Object.class;
    }

    public static boolean testIsInstance4(Object arg) {
        return getArrayClass4().isInstance(arg);
    }

    public static Class getArrayClass5() {
        return Object[][].class;
    }

    public static boolean testIsInstance5(Object arg) {
        return getArrayClass5().isInstance(arg);
    }

    public static Object[] testCopyOf1(Object[] array, Class<? extends Object[]> clazz) {
        return Arrays.copyOf(array, 1, clazz);
    }

    public static Object[] testCopyOf2(Object[] array) {
        return Arrays.copyOf(array, array.length, array.getClass());
    }

    public static Class testGetSuperclass1(Object[] array) {
        return array.getClass().getSuperclass();
    }

    public static Class testGetSuperclass2() {
        return Object[].class.getSuperclass();
    }

    public static Class testGetSuperclass3() {
        return Test[].class.getSuperclass();
    }

    public static Object[] testClassCast1(Object array) {
        return Object[].class.cast(array);
    }

    public static Object[] testClassCast2(Object array) {
        return Test[].class.cast(array);
    }

    public static Object testClassCast3(Class c, Object array) {
        return c.cast(array);
    }

    public static void test5(Object[] array, Object obj) {
        array[0] = obj;
    }

    public static void test6(Object[][] array, Object[] obj) {
        array[0] = obj;
    }

    public static void test7(Object[][][] array, Object[][] obj) {
        array[0] = obj;
    }

    public static void test8(Object[][][][] array, Object[][][] obj) {
        array[0] = obj;
    }

    public static void test9(Object[][] array) {
        array[0] = (Object[]) new Object[0];
    }

    public static void test10(Object[][] array) {
        array[0] = new String[0];
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100_000; ++i) {
            Object[] array1 = testArrayAllocation1();
            Object[][] array2 = testArrayAllocation2();
            Object[] array3 = testArrayAllocation3();
            Object[] array4 = testArrayAllocation4();
            Object[] array5 = testArrayAllocation5();
            Object[] array6 = testArrayAllocation6();

            testCheckcast1(new Object[0]);
            testCheckcast1(new Test[0]);
            testCheckcast1(array1);
            testCheckcast1(array3);
            testCheckcast1(array4);
            testCheckcast1(array5);
            testCheckcast1(array6);
            try {
                testCheckcast1(42);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }
            try {
                testCheckcast2(new Object[0]);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }
            try {
                testCheckcast2(array1);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }
            testCheckcast2(new Test[0]);
            testCheckcast2(array4);
            try {
                testCheckcast2(array5);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }
            try {
                testCheckcast2(42);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }
            testCheckcast3(new Object[0]);
            testCheckcast3(new Test[0]);
            testCheckcast3(array1);
            testCheckcast3(array3);
            testCheckcast3(array4);
            testCheckcast3(array5);
            testCheckcast3(array6);
            try {
                testCheckcast3(42);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }

            testCheckcast4(new Object[0]);
            testCheckcast4(new Test[0]);
            testCheckcast4(array1);
            testCheckcast4(array3);
            testCheckcast4(array4);
            testCheckcast4(array5);
            testCheckcast4(array6);
            testCheckcast4(42);

            testCheckcast5(new Object[0][0]);
            testCheckcast5(new Test[0][0]);
            testCheckcast5(array2);
            try {
                testCheckcast5(42);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }

            testCheckcast6(array5);

            testCheckcast7(array6);

            testCopyOf1(new Object[1], Object[].class);
            testCopyOf1(new Test[1], Object[].class);
            testCopyOf1(new Object[1], Test[].class);
            testCopyOf1(new Test[1], Test[].class);
            try {
                testCopyOf1(new Test[]{new Test()}, Integer[].class);
                throw new RuntimeException("No exception thrown");
            } catch (ArrayStoreException e) {
                // Expected
            }

            testCopyOf2(new Object[1]);
            testCopyOf2(new Test[1]);

            testClassCast1(new Object[0]);
            testClassCast1(new Test[0]);
            try {
                testClassCast1(new int[0]);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }

            testClassCast2(new Test[0]);
            try {
                testClassCast2(new Object[0]);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }
            try {
                testClassCast2(new int[0]);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }

            testClassCast3(Test[].class, new Test[0]);
            testClassCast3(Object[].class, new Test[0]);
            testClassCast3(Object[].class, new Object[0]);
            testClassCast3(int[].class, new int[0]);
            try {
                testClassCast3(Test[].class, new int[0]);
                throw new RuntimeException("No exception thrown");
            } catch (ClassCastException e) {
                // Expected
            }

            if (testGetSuperclass1(new Object[1]) != Object.class)  throw new RuntimeException("FAIL");
            if (testGetSuperclass1(new Test[1]) != Object.class)  throw new RuntimeException("FAIL");
            if (testGetSuperclass2() != Object.class)  throw new RuntimeException("FAIL");
            if (testGetSuperclass3() != Object.class)  throw new RuntimeException("FAIL");

            if (!testIsInstance1(new Object[0])) throw new RuntimeException("FAIL");
            if (!testIsInstance1(new Test[0])) throw new RuntimeException("FAIL");
            if (testIsInstance1(42)) throw new RuntimeException("FAIL");
            if (!testIsInstance1(array1)) throw new RuntimeException("FAIL");
            if (!testIsInstance1(array3)) throw new RuntimeException("FAIL");
            if (!testIsInstance1(array4)) throw new RuntimeException("FAIL");
            if (!testIsInstance1(array5)) throw new RuntimeException("FAIL");
            if (!testIsInstance1(array6)) throw new RuntimeException("FAIL");

            if (testIsInstance2(new Object[0])) throw new RuntimeException("FAIL");
            if (!testIsInstance2(new Test[0])) throw new RuntimeException("FAIL");
            if (testIsInstance2(42)) throw new RuntimeException("FAIL");
            if (testIsInstance2(array1)) throw new RuntimeException("FAIL");
            if (testIsInstance2(array3)) throw new RuntimeException("FAIL");
            if (!testIsInstance2(array4)) throw new RuntimeException("FAIL");
            if (testIsInstance2(array5)) throw new RuntimeException("FAIL");
            if (testIsInstance2(array6)) throw new RuntimeException("FAIL");

            if (!testIsInstance3(new Object[0])) throw new RuntimeException("FAIL");
            if (!testIsInstance3(new Test[0])) throw new RuntimeException("FAIL");
            if (testIsInstance3(42)) throw new RuntimeException("FAIL");
            if (!testIsInstance3(array1)) throw new RuntimeException("FAIL");
            if (!testIsInstance3(array3)) throw new RuntimeException("FAIL");
            if (!testIsInstance3(array4)) throw new RuntimeException("FAIL");
            if (!testIsInstance3(array5)) throw new RuntimeException("FAIL");
            if (!testIsInstance3(array6)) throw new RuntimeException("FAIL");

            if (!testIsInstance4(new Object[0])) throw new RuntimeException("FAIL");
            if (!testIsInstance4(new Test[0])) throw new RuntimeException("FAIL");
            if (!testIsInstance4(42)) throw new RuntimeException("FAIL");
            if (!testIsInstance4(array1)) throw new RuntimeException("FAIL");
            if (!testIsInstance4(array3)) throw new RuntimeException("FAIL");
            if (!testIsInstance4(array4)) throw new RuntimeException("FAIL");
            if (!testIsInstance4(array5)) throw new RuntimeException("FAIL");
            if (!testIsInstance4(array6)) throw new RuntimeException("FAIL");

            if (!testIsInstance5(new Object[0][0])) throw new RuntimeException("FAIL");
            if (!testIsInstance5(new Test[0][0])) throw new RuntimeException("FAIL");
            if (!testIsInstance5(array2)) throw new RuntimeException("FAIL");
            if (testIsInstance5(42)) throw new RuntimeException("FAIL");

            test5(new Object[1], new Test());
            test5((new Object[1][1])[0], (new Test[1])[0]);
            test5(new String[1], "42");
            test5((new String[1][1])[0], (new String[1])[0]);
            test5(array1, new Test());
            test5(array3, new Test());
            test5(array4, new Test());
            try {
                test5(array5, new Test());
                throw new RuntimeException("No exception thrown");
            } catch (ArrayStoreException e) {
                // Expected
            }

            test6(new Object[1][1], new Test[0]);
            test6((new Object[1][1][1])[0], (new Test[1][0])[0]);
            test6(new String[1][1], new String[0]);
            test6((new String[1][1][1])[0], (new String[1][0])[0]);
            test6(array2, new Test[0]);

            test7(new Object[1][1][1], new Test[0][0]);
            test7((new Object[1][1][1][1])[0], (new Test[1][0][0])[0]);
            test7(new String[1][1][1], new String[0][0]);
            test7((new String[1][1][1][1])[0], (new String[1][0][0])[0]);

            test8(new Object[1][1][1][1], new Test[0][0][0]);
            test8((new Object[1][1][1][1][1])[0], (new Test[1][0][0][0])[0]);
            test8(new String[1][1][1][1], new String[0][0][0]);
            test8((new String[1][1][1][1][1])[0], (new String[1][0][0][0])[0]);

            test9(new Object[1][1]);
            test9(array2);

            test10(new String[1][1]);
            test10(array2);
        }
    }
}
