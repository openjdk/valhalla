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

/*
 * @test PreloadCircularityTest
 * @modules java.base/jdk.internal.vm.annotation
 *          java.base/jdk.internal.value
 * @library /test/lib
 * @enablePreview
 * @compile PreloadCircularityTest.java
 * @run main/othervm -XX:+EnableValhalla PreloadCircularityTest
 */

 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;

import javax.management.relation.RelationServiceNotRegisteredException;

 import jdk.internal.value.ValueClass;
 import jdk.internal.vm.annotation.ImplicitlyConstructible;
 import jdk.internal.vm.annotation.LooselyConsistentValue;
 import jdk.internal.vm.annotation.NullRestricted;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class PreloadCircularityTest {

    // Testing preload due to non-static fields

    static value class Class0a {
        @NullRestricted
        Class0b vb = new Class0b();
    }

    @ImplicitlyConstructible
    static value class Class0b {
        @NullRestricted
        Class0c vc = new Class0c();
    }

    @ImplicitlyConstructible
    static value class Class0c {
        int i = 0;
    }

    void test_0() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class0a");
        out.shouldHaveExitValue(0);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class0b during loading of class PreloadCircularityTest$Class0a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class0c during loading of class PreloadCircularityTest$Class0b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class0c during loading of class PreloadCircularityTest$Class0b (cause: null-free non-static field) succeeded");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class0b during loading of class PreloadCircularityTest$Class0a (cause: null-free non-static field) succeeded");
        out.shouldNotContain("(cause: null-free non-static field) failed");
    }

    static value class Class1a {
        @NullRestricted
        Class1b vb = new Class1b();
    }

    @ImplicitlyConstructible
    static value class Class1b {
        Class1c vc = new Class1c();
    }

    static value class Class1c {
        int i = 0;
    }

    void test_1() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class1a");
        out.shouldHaveExitValue(0);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class1b during loading of class PreloadCircularityTest$Class1a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class1c during loading of class PreloadCircularityTest$Class1b. Cause: field type in Preload attribute");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class1c during loading of class PreloadCircularityTest$Class1b (cause: field type in Preload attribute) succeeded");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class1b during loading of class PreloadCircularityTest$Class1a (cause: null-free non-static field) succeeded");
    }

    static value class Class2a {
        @NullRestricted
        Class2b vb = new Class2b();
    }

    @ImplicitlyConstructible
    static value class Class2b {
        @NullRestricted
        Class2c vc = new Class2c();
    }

    @ImplicitlyConstructible
    static value class Class2c {
        @NullRestricted
        Class2b vb = new Class2b();
    }

    void test_2() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class2a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class2b during loading of class PreloadCircularityTest$Class2a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class2c during loading of class PreloadCircularityTest$Class2b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class2b during loading of class PreloadCircularityTest$Class2c. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class2b during loading of class PreloadCircularityTest$Class2c (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class2c during loading of class PreloadCircularityTest$Class2b (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class2b during loading of class PreloadCircularityTest$Class2a (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
    }

    static value class Class3a {
        @NullRestricted
        Class3b vb = new Class3b();
    }

    @ImplicitlyConstructible
    static value class Class3b {
        @NullRestricted
        Class3c vc = new Class3c();
    }

    @ImplicitlyConstructible
    static value class Class3c {
        Class3b vb = new Class3b();
    }

    void test_3() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class3a");
        out.shouldHaveExitValue(0);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class3b during loading of class PreloadCircularityTest$Class3a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class3c during loading of class PreloadCircularityTest$Class3b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class3b during loading of class PreloadCircularityTest$Class3c. Cause: field type in Preload attribute");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class3b during loading of class PreloadCircularityTest$Class3c (cause: field type in Preload attribute) failed : java/lang/ClassCircularityError");
        out.shouldContain("[info   ][class,preload] Preloading of class PreloadCircularityTest$Class3c during loading of class PreloadCircularityTest$Class3b (cause: null-free non-static field) succeeded");
        out.shouldContain("[info   ][class,preload] Preloading of class PreloadCircularityTest$Class3b during loading of class PreloadCircularityTest$Class3a (cause: null-free non-static field) succeeded");
    }

    static value class Class4a {
        @NullRestricted
        Class4b vb = new Class4b();
    }

    @ImplicitlyConstructible
    static value class Class4b {
        @NullRestricted
        Class4a vc = new Class4a();
    }

    void test_4() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class4a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class4b during loading of class PreloadCircularityTest$Class4a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class4a during loading of class PreloadCircularityTest$Class4b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class4a during loading of class PreloadCircularityTest$Class4b (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class4b during loading of class PreloadCircularityTest$Class4a (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
    }

    static value class Class5a {
        Class5b vb = new Class5b();

        @NullRestricted
        Class5c vc = new Class5c();
    }

    static value class Class5b {
        @NullRestricted
        Class5d vd = new Class5d();
    }

    @ImplicitlyConstructible
    static value class Class5c {
        Class5b vb = new Class5b();
    }

    static value class Class5d {
        @NullRestricted
        Class5b vb = new Class5b();
    }

    void test_5() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class5a");
        out.shouldHaveExitValue(0);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5a. Cause: field type in Preload attribute");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class5d during loading of class PreloadCircularityTest$Class5b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5d. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5d (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class5d during loading of class PreloadCircularityTest$Class5b (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5a (cause: field type in Preload attribute) failed : java/lang/ClassCircularityError");
        out.shouldContain("[info   ][class,preload] Preloading class PreloadCircularityTest$Class5c during loading of class PreloadCircularityTest$Class5a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info   ][class,preload] Preloading class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5c. Cause: field type in Preload attribute");
        out.shouldContain("[info   ][class,preload] Preloading class PreloadCircularityTest$Class5d during loading of class PreloadCircularityTest$Class5b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info   ][class,preload] Preloading class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5d. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5d (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class5d during loading of class PreloadCircularityTest$Class5b (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class5b during loading of class PreloadCircularityTest$Class5c (cause: field type in Preload attribute) failed : java/lang/ClassCircularityError");
        out.shouldContain("[info   ][class,preload] Preloading of class PreloadCircularityTest$Class5c during loading of class PreloadCircularityTest$Class5a (cause: null-free non-static field) succeeded");
    }

    static value class Class6a {
        @NullRestricted
        Class6b vb = new Class6b();
    }

    @ImplicitlyConstructible
    static value class Class6b {
        Class6c vc = new Class6c();

        @NullRestricted
        Class6d vd = new Class6d();
    }

    static value class Class6c {
        int i = 0;
    }

    @ImplicitlyConstructible
    static value class Class6d {
        @NullRestricted
        Class6b vb = new Class6b();
    }

    void test_6() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class6a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class6b during loading of class PreloadCircularityTest$Class6a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class6c during loading of class PreloadCircularityTest$Class6b. Cause: field type in Preload attribute");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class6c during loading of class PreloadCircularityTest$Class6b (cause: field type in Preload attribute) succeeded");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class6d during loading of class PreloadCircularityTest$Class6b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class6b during loading of class PreloadCircularityTest$Class6d. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class6b during loading of class PreloadCircularityTest$Class6d (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class6d during loading of class PreloadCircularityTest$Class6b (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class6b during loading of class PreloadCircularityTest$Class6a (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
    }

    @ImplicitlyConstructible
    static value class Class7a {
        @NullRestricted
        Class7a va = new Class7a();
    }

    void test_7() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class7a");
        out.shouldHaveExitValue(1);
        out.shouldNotContain("[info][class,preload] Preloading class PreloadCircularityTest$Class7a during loading of class PreloadCircularityTest$Class7a. Cause: a null-free non-static field is declared with this type");
    }

    static value class Class8a {
        Class8a va = new Class8a();
    }

    void test_8() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class8a");
        out.shouldHaveExitValue(0);
        out.shouldNotContain("[info][class,preload] Preloading class PreloadCircularityTest$Class8a during loading of class PreloadCircularityTest$Class8a. Cause: a null-free non-static field is declared with this type");
    }

    static value class Class9a {
        @NullRestricted
        Class9b vb = new Class9b();
    }

    static class Class9b { }

    void test_9() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class9a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class9b during loading of class PreloadCircularityTest$Class9a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("java.lang.IncompatibleClassChangeError: Class PreloadCircularityTest$Class9a expects class PreloadCircularityTest$Class9b to be a value class, but it is an identity class");
    }

    static value class Class10a {
        @NullRestricted
        Class10b vb = new Class10b();
    }

    static value class Class10b { }

    void test_10() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class10a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class10b during loading of class PreloadCircularityTest$Class10a. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("java.lang.IncompatibleClassChangeError: class PreloadCircularityTest$Class10b is not implicitly constructible and it is used in a null restricted non-static field (not supported)");
    }

    // Testing preloading due to static fields

    @ImplicitlyConstructible
    static value class Class50a {
        @NullRestricted
        static Class50a sa;
    }

    void test_50() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class50a");
        out.shouldHaveExitValue(0);
        out.shouldNotContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class50a");
    }

    @ImplicitlyConstructible
    static value class Class51a {
        @NullRestricted
        static Class51b sb;

        @NullRestricted
        static Class51c sc;
    }

    @ImplicitlyConstructible
    static value class Class51b {
        @NullRestricted
        static Class51a sa;
    }

    @ImplicitlyConstructible
    static value class Class51c {
        @NullRestricted
        Class51a sa = new Class51a();
    }

    void test_51() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class51a");
        out.shouldHaveExitValue(0);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class51b during linking of class PreloadCircularityTest$Class51a. Cause: a null-free static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class51b during linking of class PreloadCircularityTest$Class51a (cause: null-free static field) succeeded");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class51c during linking of class PreloadCircularityTest$Class51a. Cause: a null-free static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class51a during loading of class PreloadCircularityTest$Class51c. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class51a during loading of class PreloadCircularityTest$Class51c (cause: null-free non-static field) succeeded");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class51c during linking of class PreloadCircularityTest$Class51a (cause: null-free static field) succeeded");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class51a during linking of class PreloadCircularityTest$Class51b. Cause: a null-free static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class51a during linking of class PreloadCircularityTest$Class51b (cause: null-free static field) succeeded");
    }

    static value class Class52a {
        @NullRestricted
        static Class52b vb;
    }

    @ImplicitlyConstructible
    static value class Class52b {
        @NullRestricted
        Class52c vc = new Class52c();
    }

    @ImplicitlyConstructible
    static value class Class52c {
        @NullRestricted
        Class52b vb = new Class52b();
    }

    void test_52() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class52a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class52b during linking of class PreloadCircularityTest$Class52a. Cause: a null-free static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class52c during loading of class PreloadCircularityTest$Class52b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class52b during loading of class PreloadCircularityTest$Class52c. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class52b during loading of class PreloadCircularityTest$Class52c (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class52c during loading of class PreloadCircularityTest$Class52b (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
    }

    @ImplicitlyConstructible
    static value class Class53a {
        Class53b vb = new Class53b();

        @NullRestricted
        static Class53b sb;
    }

    @ImplicitlyConstructible
    static value class Class53b {
        @NullRestricted
        Class53a va = new Class53a();
    }

    // In the following test, Class53a fails to optimistically load Class53b at load time, but successfully loads it at link time

    void test_53() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class53a");
        out.shouldHaveExitValue(0);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class53b during loading of class PreloadCircularityTest$Class53a. Cause: field type in Preload attribute");
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class53a during loading of class PreloadCircularityTest$Class53b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class53a during loading of class PreloadCircularityTest$Class53b (cause: null-free non-static field) failed: java/lang/ClassCircularityError");
        out.shouldContain("[warning][class,preload] Preloading of class PreloadCircularityTest$Class53b during loading of class PreloadCircularityTest$Class53a (cause: field type in Preload attribute) failed : java/lang/ClassCircularityError");
        out.shouldContain("[info   ][class,preload] Preloading class PreloadCircularityTest$Class53b during linking of class PreloadCircularityTest$Class53a. Cause: a null-free static field is declared with this type");
        out.shouldContain("[info   ][class,preload] Preloading class PreloadCircularityTest$Class53a during loading of class PreloadCircularityTest$Class53b. Cause: a null-free non-static field is declared with this type");
        out.shouldContain("[info   ][class,preload] Preloading of class PreloadCircularityTest$Class53a during loading of class PreloadCircularityTest$Class53b (cause: null-free non-static field) succeeded");
        out.shouldContain("[info   ][class,preload] Preloading of class PreloadCircularityTest$Class53b during linking of class PreloadCircularityTest$Class53a (cause: null-free static field) succeeded");
    }

    static value class Class54a {
        @NullRestricted
        static Class54b sb;
    }

    static class Class54b { }

    void test_54() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class54a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class54b during linking of class PreloadCircularityTest$Class54a. Cause: a null-free static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class54b during linking of class PreloadCircularityTest$Class54a (cause: null-free static field) succeeded");
        out.shouldContain("java.lang.IncompatibleClassChangeError: class PreloadCircularityTest$Class54a expects class PreloadCircularityTest$Class54b to be a value class but it is an identity class");
    }

    static class Class55a {
        @NullRestricted
        static Class55b sb;
    }

    static value class Class55b { }

    void test_55() throws Exception {
        OutputAnalyzer out = tryLoadingClass("PreloadCircularityTest$Class55a");
        out.shouldHaveExitValue(1);
        out.shouldContain("[info][class,preload] Preloading class PreloadCircularityTest$Class55b during linking of class PreloadCircularityTest$Class55a. Cause: a null-free static field is declared with this type");
        out.shouldContain("[info][class,preload] Preloading of class PreloadCircularityTest$Class55b during linking of class PreloadCircularityTest$Class55a (cause: null-free static field) succeeded");
        out.shouldContain("java.lang.IncompatibleClassChangeError: class PreloadCircularityTest$Class55b is not implicitly constructible and it is used in a null restricted static field (not supported)");
    }

    public static class TestHelper {
        public static void main(String[] args) {
            try {
                Class c = Class.forName(args[0]);
            } catch (Throwable ex) {
                ex.printStackTrace();
                System.exit(1);
            }
            System.exit(0);
        }
    }

    static OutputAnalyzer tryLoadingClass(String className) throws Exception {
        ProcessBuilder pb = exec("PreloadCircularityTest$TestHelper", className);
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        System.out.println(out.getOutput());
        return out;
    }

    static ProcessBuilder exec(String... args) throws Exception {
        List<String> argsList = new ArrayList<>();
        Collections.addAll(argsList, "--enable-preview");
        Collections.addAll(argsList, "-Dtest.class.path=" + System.getProperty("test.class.path", "."));
        Collections.addAll(argsList, "-Xlog:class+preload=info");
        Collections.addAll(argsList, args);
        return ProcessTools.createTestJavaProcessBuilder(argsList);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Crerating tests");
        PreloadCircularityTest tests = new PreloadCircularityTest();
        Class c = tests.getClass();
        System.out.println("Iterating over test methods");
        boolean hasFailedTest = false;
        StringBuilder sb = new StringBuilder("Following tests have failed: ");
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().startsWith("test_")) {
                boolean failed = false;
                try {
                    System.out.println("Running " + m.getName());
                    m.invoke(tests);
                } catch (Throwable t) {
                    t.printStackTrace();
                    failed = true;
                }
                System.out.println("Test " + m.getName() + " : " + (failed ? "FAILED" : "PASSED"));
                hasFailedTest = failed ? true : hasFailedTest;
                if (failed) sb.append(m.getName()).append(", ");
            }
        }
        if (hasFailedTest) {
            throw new RuntimeException(sb.toString());
        }
    }
}
