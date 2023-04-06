/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8301007
 * @key randomness
 * @summary Verify that mismatches of the preload attribute are properly handled in the calling convention.
 * @library /test/lib /compiler/whitebox /
 * @compile -XDenablePrimitiveClasses TestMismatchHandling.jcod TestMismatchHandling.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Xbatch
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-Inline -XX:-InlineAccessors -XX:-UseBimorphicInlining -XX:-UseCHA -XX:-UseTypeProfile
 *                   -XX:CompileCommand=compileonly,TestMismatchHandling::test*
 *                   TestMismatchHandling
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Xbatch
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-Inline -XX:-InlineAccessors -XX:-UseBimorphicInlining -XX:-UseCHA -XX:-UseTypeProfile
 *                   -XX:CompileCommand=compileonly,*::method
 *                   TestMismatchHandling
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Xbatch
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-Inline -XX:-InlineAccessors -XX:-UseBimorphicInlining -XX:-UseCHA -XX:-UseTypeProfile
 *                   TestMismatchHandling
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Xbatch
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-Inline -XX:-InlineAccessors -XX:-UseBimorphicInlining -XX:-UseCHA -XX:-UseTypeProfile
 *                   -XX:-InlineTypePassFieldsAsArgs
 *                   TestMismatchHandling
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Xbatch
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-Inline -XX:-InlineAccessors -XX:-UseBimorphicInlining -XX:-UseCHA -XX:-UseTypeProfile
 *                   -XX:-InlineTypeReturnedAsFields
 *                   TestMismatchHandling
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Xbatch
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeNMethodBarriersALot
 *                   TestMismatchHandling
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EnableValhalla -XX:+EnablePrimitiveClasses -Xbatch
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   TestMismatchHandling
 */

// Use below script to re-generate TestMismatchHandling.jcod

/*
  #!/bin/bash
  export PATH=/oracle/valhalla/build/fastdebug/jdk/bin/:$PATH
  ASMTOOLS=/oracle/valhalla/open/test/lib

  # With preload attribute
  javac TestMismatchHandlingGenerator.java
  java -cp $ASMTOOLS org.openjdk.asmtools.Main jdec MyValue1.class MyValue2.class MyValue3.class MyValue4.class MyValue5.class Verifiable.class B.class I3.class I4.class E.class G.class J.class K.class L.class TestMismatchHandlingHelper.class > TestMismatchHandling.jcod

  # Without preload attribute
  sed -i 's/value class MyValue/class MyValue/g' TestMismatchHandlingGenerator.java
  javac TestMismatchHandlingGenerator.java
  java -cp $ASMTOOLS org.openjdk.asmtools.Main jdec A.class C.class I1.class I2.class D.class F.class H.class I5.class M.class N.class >> TestMismatchHandling.jcod

  sed -i 's/class MyValue/value class MyValue/g' TestMismatchHandlingGenerator.java
*/

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import jdk.test.lib.Utils;
import jdk.test.whitebox.WhiteBox;

public class TestMismatchHandling {
    public static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public static void main(String[] args) throws Exception {
    
        M m = new M();
        // Make sure M::method is C1 compiled once with unloaded MyValue4 and not re-compiled
        for (int i = 0; i < 1000; ++i) {
            TestMismatchHandlingHelper.test4(m, true);
        }
        Method disable = M.class.getDeclaredMethod("method", boolean.class);
        WHITE_BOX.makeMethodNotCompilable(disable, 1, false);
        WHITE_BOX.makeMethodNotCompilable(disable, 2, false);
        WHITE_BOX.makeMethodNotCompilable(disable, 3, false);
        WHITE_BOX.makeMethodNotCompilable(disable, 4, false);
        
        // Sometimes, exclude some methods from compilation with C2 to stress test the calling convention
        // WARNING: This triggers class loading of argument/return types of all methods!
        if (Utils.getRandomInstance().nextBoolean()) {
            ArrayList<Method> methods = new ArrayList<Method>();
            Collections.addAll(methods, TestMismatchHandlingHelper.class.getDeclaredMethods());
            Collections.addAll(methods, A.class.getDeclaredMethods());
            Collections.addAll(methods, B.class.getDeclaredMethods());
            Collections.addAll(methods, C.class.getDeclaredMethods());
            Collections.addAll(methods, E.class.getDeclaredMethods());
            Collections.addAll(methods, F.class.getDeclaredMethods());
            Collections.addAll(methods, G.class.getDeclaredMethods());
            Collections.addAll(methods, H.class.getDeclaredMethods());
            Collections.addAll(methods, J.class.getDeclaredMethods());
            Collections.addAll(methods, K.class.getDeclaredMethods());
            Collections.addAll(methods, L.class.getDeclaredMethods());
            // Don't do this because it would load MyValue5
            // Collections.addAll(methods, N.class.getDeclaredMethods());
            System.out.println("Excluding methods from C2 compilation:");
            for (Method method : methods) {
                if (Utils.getRandomInstance().nextBoolean()) {
                    System.out.println(method);
                    WHITE_BOX.makeMethodNotCompilable(method, 4, false);
                }
            }
        }

        A a = new A();
        B b = new B();
        C c = new C();
        D d = new D();
        E e = new E();
        H h = new H();
        J j = new J();
        K k = new K();
        N n = new N();

        // Warmup
        for (int i = 0; i < 50_000; ++i) {
            TestMismatchHandlingHelper.test1(a, a, a, b, b, c);
            TestMismatchHandlingHelper.test1(b, a, a, b, b, c);
            TestMismatchHandlingHelper.test1(c, b, a, c, b, c);
            TestMismatchHandlingHelper.test2(d, d, d, d, d, d, e, e, e, e, e, e, d, e);
            TestMismatchHandlingHelper.test3(h, h, h, j, j, j, k);
            TestMismatchHandlingHelper.test3(h, h, h, k, k, j, k);
            TestMismatchHandlingHelper.test4(m, true);
            TestMismatchHandlingHelper.test5(n, true);
        } 

        // Only load these now
        F f = new F();
        G g = new G();
        L l = new L();

        for (int i = 0; i < 50_000; ++i) {
            TestMismatchHandlingHelper.test1(a, a, a, b, b, c);
            TestMismatchHandlingHelper.test1(b, a, a, b, b, c);
            TestMismatchHandlingHelper.test1(c, b, a, c, b, c);
            TestMismatchHandlingHelper.test2(d, f, g, d, f, g, e, e, g, e, e, g, d, e);
            TestMismatchHandlingHelper.test2(f, f, g, f, f, g, f, e, g, f, e, g, d, e);
            TestMismatchHandlingHelper.test2(g, g, g, g, g, g, g, g, g, g, f, g, d, e);
            TestMismatchHandlingHelper.test3(h, l, h, j, j, l, k);
            TestMismatchHandlingHelper.test3(h, l, h, k, j, l, k);
            TestMismatchHandlingHelper.test3(l, l, h, l, k, l, l);
            TestMismatchHandlingHelper.test4(m, false);
            TestMismatchHandlingHelper.test5(n, false);
            TestMismatchHandlingHelper.test6(f, g, l);
        }    
    }
}
