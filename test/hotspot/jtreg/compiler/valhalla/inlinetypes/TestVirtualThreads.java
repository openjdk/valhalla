/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @key randomness
 * @summary Test that Virtual Threads work well with Value Objects.
 * @library /test/lib /compiler/whitebox /
 * @enablePreview
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   TestVirtualThreads
*
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xbatch -XX:CompileCommand=compileonly,TestVirtualThreads*::*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xbatch -XX:CompileCommand=compileonly,TestVirtualThreads*::test*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xbatch -XX:CompileCommand=dontinline,*::* -XX:CompileCommand=compileonly,TestVirtualThreads*::*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xbatch -XX:CompileCommand=dontinline,*::* -XX:CompileCommand=compileonly,TestVirtualThreads*::test*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xbatch -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,TestVirtualThreads*::test* -XX:CompileCommand=dontinline,*::test*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xbatch -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,TestVirtualThreads*::test* -XX:CompileCommand=dontinline,*::*Helper
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xbatch -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,TestVirtualThreads*::test* -XX:CompileCommand=exclude,*::*Helper
*                   TestVirtualThreads
*
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xcomp -XX:CompileCommand=compileonly,TestVirtualThreads*::*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xcomp -XX:CompileCommand=compileonly,TestVirtualThreads*::test*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xcomp -XX:CompileCommand=dontinline,*::* -XX:CompileCommand=compileonly,TestVirtualThreads*::*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xcomp -XX:CompileCommand=dontinline,*::* -XX:CompileCommand=compileonly,TestVirtualThreads*::test*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xcomp -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,TestVirtualThreads*::test* -XX:CompileCommand=dontinline,*::test*
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xcomp -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,TestVirtualThreads*::test* -XX:CompileCommand=dontinline,*::*Helper
*                   TestVirtualThreads
* @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
*                   -Xcomp -XX:CompileCommand=dontinline,*::dontinline -XX:CompileCommand=compileonly,TestVirtualThreads*::test* -XX:CompileCommand=exclude,*::*Helper
*                   TestVirtualThreads
**/

// TODO RUN WITH -XX:-TieredCompilation -XX:+PreserveFramePointer -XX:-UseOnStackReplacement -XX:+DeoptimizeALot -XX:+StressCallingConvention -XX:-InlineTypePassFieldsAsArgs -XX:+InlineTypeReturnedAsFields
// TODO add runs with Virtual Calls to exercise computation of stack args
// TODO need more variants with interpreted and C1 compiled callers, deopts (including ), etc.

// TODO clean up includes
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

import jdk.test.whitebox.WhiteBox;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.CountDownLatch;
import java.util.Random;

public class TestVirtualThreads {
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    static final int COMP_LEVEL_FULL_OPTIMIZATION = 4; // C2 or JVMCI
    static final Random RAND = Utils.getRandomInstance();
    // TODO reduce?
    static final int PARK_DURATION = 1000;

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

        public static void verify(SmallValue val, String loc, int i, boolean useNull) {
            if (useNull) {
                if (val != null) {
                    new RuntimeException("Incorrect result at " + loc + " for i = " + i + ": " + val).printStackTrace(System.out);
                }
            } else {
                val.verify(loc, i);
            }
        }
    }

    // Large value class
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

        public static void verify(LargeValue val, String loc, int i, boolean useNull) {
            if (useNull) {
                if (val != null) {
                    new RuntimeException("Incorrect result at " + loc + " for i = " + i + ": " + val).printStackTrace(System.out);
                }
            } else {
                val.verify(loc, i);
            }
        }
    }

    // Large value class with fields of different types
    static value class LargeValue2 {
        byte x1;
        short x2;
        int x3;
        long x4;
        double x5;
        boolean x6;
        char x7;

        public LargeValue2(int i) {
            this.x1 = (byte)i;
            this.x2 = (short)i;
            this.x3 = i;
            this.x4 = i;
            this.x5 = i;
            this.x6 = (i % 2) == 0;
            this.x7 = (char)i;
        }

        public String toString() {
            return "x1 = " + x1 + ", x2 = " + x2 + ", x3 = " + x3 + ", x4 = " + x4 + ", x5 = " + x5 +
                   ", x6 = " + x6 + ", x7 = " + x7;
        }

        public void verify(String loc, int i) {
            if (x1 != (byte)i || x2 != (short)i || x3 != i || x4 != i || x5 != i ||
                x6 != ((i % 2) == 0) || x7 != (char)i) {
                new RuntimeException("Incorrect result at " + loc + " for i = " + i + ": " + this).printStackTrace(System.out);
                System.exit(1);
            }
        }

        public static void verify(LargeValue2 val, String loc, int i, boolean useNull) {
            if (useNull) {
                if (val != null) {
                    new RuntimeException("Incorrect result at " + loc + " for i = " + i + ": " + val).printStackTrace(System.out);
                }
            } else {
                val.verify(loc, i);
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

        public static void verify(LargeValueWithOops val, String loc, Object obj, boolean useNull) {
            if (useNull) {
                if (val != null) {
                    new RuntimeException("Incorrect result at " + loc + " for obj = " + obj + ": " + val).printStackTrace(System.out);
                }
            } else {
                val.verify(loc, obj);
            }
        }
    }

    public static void dontInline() { }

    public static SmallValue testSmall(SmallValue val, int i, boolean useNull, boolean park) {
        SmallValue.verify(val, "entry", i, useNull);
        if (park) {
            LockSupport.parkNanos(PARK_DURATION);
        }
        SmallValue.verify(val, "exit", i, useNull);
        return val;
    }

    public static SmallValue testSmallHelper(int i, boolean useNull, boolean park) {
        SmallValue val = useNull ? null : new SmallValue(i);
        val = testSmall(val, i, useNull, park);
        SmallValue.verify(val, "helper", i, useNull);
        return val;
    }

    public static LargeValue testLarge(LargeValue val, int i, boolean useNull, boolean park) {
        LargeValue.verify(val, "entry", i, useNull);
        if (park) {
            LockSupport.parkNanos(PARK_DURATION);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        LargeValue.verify(val, "exit", i, useNull);
        return val;
    }

    public static LargeValue testLargeHelper(int i, boolean useNull, boolean park) {
        LargeValue val = useNull ? null : new LargeValue(i);
        val = testLarge(val, i, useNull, park);
        LargeValue.verify(val, "helper", i, useNull);
        return val;
    }

    // Version that already has values on the stack even before stack extensions
    public static LargeValue testLargeManyArgs(LargeValue val1, LargeValue val2, LargeValue val3, LargeValue val4, LargeValue val5,
                                               LargeValue val6, LargeValue val7, LargeValue val8, LargeValue val9, LargeValue val10, int i, boolean useNull, boolean park) {
        LargeValue.verify(val1, "entry", i, useNull);
        LargeValue.verify(val2, "entry", i + 1, useNull);
        LargeValue.verify(val3, "entry", i + 2, useNull);
        LargeValue.verify(val4, "entry", i + 3, useNull);
        LargeValue.verify(val5, "entry", i + 4, useNull);
        LargeValue.verify(val6, "entry", i + 5, useNull);
        LargeValue.verify(val7, "entry", i + 6, useNull);
        LargeValue.verify(val8, "entry", i + 7, useNull);
        LargeValue.verify(val9, "entry", i + 8, useNull);
        LargeValue.verify(val10, "entry", i + 9, useNull);
        if (park) {
            LockSupport.parkNanos(PARK_DURATION);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        LargeValue.verify(val1, "exit", i, useNull);
        LargeValue.verify(val2, "exit", i + 1, useNull);
        LargeValue.verify(val3, "exit", i + 2, useNull);
        LargeValue.verify(val4, "exit", i + 3, useNull);
        LargeValue.verify(val5, "exit", i + 4, useNull);
        LargeValue.verify(val6, "exit", i + 5, useNull);
        LargeValue.verify(val7, "exit", i + 6, useNull);
        LargeValue.verify(val8, "exit", i + 7, useNull);
        LargeValue.verify(val9, "exit", i + 8, useNull);
        LargeValue.verify(val10, "exit", i + 9, useNull);
        return val5;
    }

    public static LargeValue testLargeManyArgsHelper(int i, boolean useNull, boolean park) {
        LargeValue val1 = useNull ? null : new LargeValue(i);
        LargeValue val2 = useNull ? null : new LargeValue(i + 1);
        LargeValue val3 = useNull ? null : new LargeValue(i + 2);
        LargeValue val4 = useNull ? null : new LargeValue(i + 3);
        LargeValue val5 = useNull ? null : new LargeValue(i + 4);
        LargeValue val6 = useNull ? null : new LargeValue(i + 5);
        LargeValue val7 = useNull ? null : new LargeValue(i + 6);
        LargeValue val8 = useNull ? null : new LargeValue(i + 7);
        LargeValue val9 = useNull ? null : new LargeValue(i + 8);
        LargeValue val10 = useNull ? null : new LargeValue(i + 9);
        LargeValue val = testLargeManyArgs(val1, val2, val3, val4, val5,
                                           val6, val7, val8, val9, val10, i, useNull, park);
        LargeValue.verify(val, "helper", i + 4, useNull);
        return val;
    }

    public static LargeValue2 testLarge2(LargeValue2 val, int i, boolean useNull, boolean park) {
        LargeValue2.verify(val, "entry", i, useNull);
        if (park) {
            LockSupport.parkNanos(PARK_DURATION);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        LargeValue2.verify(val, "exit", i, useNull);
        return val;
    }

    public static LargeValue2 testLarge2Helper(int i, boolean useNull, boolean park) {
        LargeValue2 val = useNull ? null : new LargeValue2(i);
        val = testLarge2(val, i, useNull, park);
        LargeValue2.verify(val, "helper", i, useNull);
        return val;
    }

    // Version that already has values on the stack even before stack extensions
    public static LargeValue2 testLarge2ManyArgs(LargeValue2 val1, LargeValue2 val2, LargeValue2 val3, LargeValue2 val4, LargeValue2 val5,
                                                 LargeValue2 val6, LargeValue2 val7, LargeValue2 val8, LargeValue2 val9, LargeValue2 val10, int i, boolean useNull, boolean park) {
        LargeValue2.verify(val1, "entry", i, useNull);
        LargeValue2.verify(val2, "entry", i + 1, useNull);
        LargeValue2.verify(val3, "entry", i + 2, useNull);
        LargeValue2.verify(val4, "entry", i + 3, useNull);
        LargeValue2.verify(val5, "entry", i + 4, useNull);
        LargeValue2.verify(val6, "entry", i + 5, useNull);
        LargeValue2.verify(val7, "entry", i + 6, useNull);
        LargeValue2.verify(val8, "entry", i + 7, useNull);
        LargeValue2.verify(val9, "entry", i + 8, useNull);
        LargeValue2.verify(val10, "entry", i + 9, useNull);
        if (park) {
            LockSupport.parkNanos(PARK_DURATION);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        LargeValue2.verify(val1, "exit", i, useNull);
        LargeValue2.verify(val2, "exit", i + 1, useNull);
        LargeValue2.verify(val3, "exit", i + 2, useNull);
        LargeValue2.verify(val4, "exit", i + 3, useNull);
        LargeValue2.verify(val5, "exit", i + 4, useNull);
        LargeValue2.verify(val6, "exit", i + 5, useNull);
        LargeValue2.verify(val7, "exit", i + 6, useNull);
        LargeValue2.verify(val8, "exit", i + 7, useNull);
        LargeValue2.verify(val9, "exit", i + 8, useNull);
        LargeValue2.verify(val10, "exit", i + 9, useNull);
        return val5;
    }

    public static LargeValue2 testLarge2ManyArgsHelper(int i, boolean useNull, boolean park) {
        LargeValue2 val1 = useNull ? null : new LargeValue2(i);
        LargeValue2 val2 = useNull ? null : new LargeValue2(i + 1);
        LargeValue2 val3 = useNull ? null : new LargeValue2(i + 2);
        LargeValue2 val4 = useNull ? null : new LargeValue2(i + 3);
        LargeValue2 val5 = useNull ? null : new LargeValue2(i + 4);
        LargeValue2 val6 = useNull ? null : new LargeValue2(i + 5);
        LargeValue2 val7 = useNull ? null : new LargeValue2(i + 6);
        LargeValue2 val8 = useNull ? null : new LargeValue2(i + 7);
        LargeValue2 val9 = useNull ? null : new LargeValue2(i + 8);
        LargeValue2 val10 = useNull ? null : new LargeValue2(i + 9);
        LargeValue2 val = testLarge2ManyArgs(val1, val2, val3, val4, val5,
                                             val6, val7, val8, val9, val10, i, useNull, park);
        LargeValue2.verify(val, "helper", i + 4, useNull);
        return val;
    }

    public static LargeValueWithOops testLargeValueWithOops(LargeValueWithOops val, Object obj, boolean useNull, boolean park) {
        LargeValueWithOops.verify(val, "entry", obj, useNull);
        if (park) {
            LockSupport.parkNanos(PARK_DURATION);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        LargeValueWithOops.verify(val, "exit", obj, useNull);
        return val;
    }

    public static LargeValueWithOops testLargeValueWithOopsHelper(Object obj, boolean useNull, boolean park) {
        LargeValueWithOops val = useNull ? null : new LargeValueWithOops(obj);
        val = testLargeValueWithOops(val, obj, useNull, park);
        LargeValueWithOops.verify(val, "helper", obj, useNull);
        return val;
    }

    // Version that already has values on the stack even before stack extensions
    public static LargeValueWithOops testLargeValueWithOops2(LargeValueWithOops val1, LargeValueWithOops val2, LargeValueWithOops val3, LargeValueWithOops val4, LargeValueWithOops val5,
                                                        LargeValueWithOops val6, LargeValueWithOops val7, LargeValueWithOops val8, LargeValueWithOops val9, Object obj, boolean useNull, boolean park) {
        LargeValueWithOops.verify(val1, "entry", obj, useNull);
        LargeValueWithOops.verify(val2, "entry", obj, useNull);
        LargeValueWithOops.verify(val3, "entry", obj, useNull);
        LargeValueWithOops.verify(val4, "entry", obj, useNull);
        LargeValueWithOops.verify(val5, "entry", obj, useNull);
        LargeValueWithOops.verify(val6, "entry", obj, useNull);
        LargeValueWithOops.verify(val7, "entry", obj, useNull);
        LargeValueWithOops.verify(val8, "entry", obj, useNull);
        LargeValueWithOops.verify(val9, "entry", obj, useNull);
        if (park) {
            LockSupport.parkNanos(PARK_DURATION);
        }
        dontInline(); // Prevent C2 from optimizing out below checks
        LargeValueWithOops.verify(val1, "exit", obj, useNull);
        LargeValueWithOops.verify(val2, "exit", obj, useNull);
        LargeValueWithOops.verify(val3, "exit", obj, useNull);
        LargeValueWithOops.verify(val4, "exit", obj, useNull);
        LargeValueWithOops.verify(val5, "exit", obj, useNull);
        LargeValueWithOops.verify(val6, "exit", obj, useNull);
        LargeValueWithOops.verify(val7, "exit", obj, useNull);
        LargeValueWithOops.verify(val8, "exit", obj, useNull);
        LargeValueWithOops.verify(val9, "exit", obj, useNull);
        return val5;
    }

    public static LargeValueWithOops testLargeValueWithOops2Helper(Object obj, boolean useNull, boolean park) {
        LargeValueWithOops val1 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val2 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val3 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val4 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val5 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val6 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val7 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val8 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val9 = useNull ? null : new LargeValueWithOops(obj);
        LargeValueWithOops val = testLargeValueWithOops2(val1, val2, val3, val4, val5,
                                                         val6, val7, val8, val9, obj, useNull, park);
        LargeValueWithOops.verify(val, "helper", obj, useNull);
        return val;
    }

    public static void main(String[] args) throws Exception {
        /* TODO enable
        // Sometimes, exclude some methods from compilation with C2 to stress test the calling convention
        if (Utils.getRandomInstance().nextBoolean()) {
            ArrayList<Method> methods = new ArrayList<Method>();
            Collections.addAll(methods, SmallValue.class.getDeclaredMethods());
            Collections.addAll(methods, LargeValue.class.getDeclaredMethods());
            Collections.addAll(methods, LargeValue2.class.getDeclaredMethods());
            Collections.addAll(methods, LargeValueWithOops.class.getDeclaredMethods());
            Collections.addAll(methods, TestVirtualThreads.class.getDeclaredMethods());
            System.out.println("Excluding methods from C2 compilation:");
            for (Method m : methods) {
                if (Utils.getRandomInstance().nextBoolean()) {
                    System.out.println(m);
                    WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_FULL_OPTIMIZATION, false);
                }
            }
        }
        */
        CountDownLatch cdl = new CountDownLatch(1);
        Thread.ofVirtual().name("vt1").start(() -> {
            // Trigger compilation
            for (int i = 0; i < 500_000; i++) {
                boolean park = (i % 1000) == 0;
                boolean useNull = RAND.nextBoolean();
                Object val = useNull ? null : new SmallValue(i);
                SmallValue.verify(testSmallHelper(i, useNull, park), "return", i, useNull);
                LargeValue.verify(testLargeHelper(i, useNull, park), "return", i, useNull);
                LargeValue.verify(testLargeManyArgsHelper(i, useNull, park), "return", i + 4, useNull);
                LargeValue2.verify(testLarge2Helper(i, useNull, park), "return", i, useNull);
                LargeValue2.verify(testLarge2ManyArgsHelper(i, useNull, park), "return", i + 4, useNull);
                LargeValueWithOops.verify(testLargeValueWithOopsHelper(val, useNull, park), "return", val, useNull);
                LargeValueWithOops.verify(testLargeValueWithOops2Helper(val, useNull, park), "return", val, useNull);
            }
            cdl.countDown();
        });
        cdl.await();
    }
}

