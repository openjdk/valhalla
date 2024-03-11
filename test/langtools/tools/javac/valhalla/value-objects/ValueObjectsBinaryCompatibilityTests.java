/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8292817
 * @summary add binary compatibility tests for value objects
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main ValueObjectsBinaryCompatibilityTests
 * @ignore Verifier error
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;

public class ValueObjectsBinaryCompatibilityTests extends TestRunner {
    ToolBox tb;

    ValueObjectsBinaryCompatibilityTests() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    public static void main(String... args) throws Exception {
        new ValueObjectsBinaryCompatibilityTests().runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    /* 1- compiles the first version of the source code, code1, along with the client source code
     * 2- executes the client class just to make sure that it works, sanity check
     * 3- compiles the second version, code2
     * 4- executes the client class and makes sure that the VM throws the expected error or not
     *    depending on the shouldFail argument
     */
    private void testCompatibilityAfterChange(
            Path base,
            String code1,
            String code2,
            String clientCode,
            boolean shouldFail,
            Class<?> expectedError) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path src1 = pkg.resolve("Test");
        Path client = pkg.resolve("Client");

        tb.writeJavaFiles(src1, code1);
        tb.writeJavaFiles(client, clientCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg))
                .run();

        // let's execute to check that it's working
        String output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Client")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);

        // let's first check that it runs wo issues
        if (!output.contains("Hello World!")) {
            throw new AssertionError("execution of Client didn't finish");
        }

        // now lets change the first class
        tb.writeJavaFiles(src1, code2);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(src1))
                .run();

        if (shouldFail) {
            // let's now check that we get the expected error
            output = new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("pkg.Client")
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutput(Task.OutputKind.STDERR);
            if (!output.contains(expectedError.getName())) {
                throw new AssertionError(expectedError.getName() + " expected");
            }
        } else {
            new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("pkg.Client")
                    .run(Task.Expect.SUCCESS);
        }
    }

    @Test
    public void testAbstractClassCompatibility(Path base) throws Exception {
        /* If one of the identity or value modifiers is added to an abstract class, a pre-existing binary that attempts
         * to extend the class may fail to load. Specifically, if the subclass has an incompatible identity or value
         * modifier, or implements an interface with an incompatible identity or value modifier, class loading will fail
         * with an IncompatibleClassChangeError.
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {}
                """,
                """
                package pkg;
                public abstract identity class A {}
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        // another variation of the assertion above
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {}
                """,
                """
                package pkg;
                public abstract value class A {}
                """,
                """
                package pkg;
                public identity class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        // Removing one of the identity or value modifiers from an abstract class does not break compatibility with pre-existing binaries.
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract identity class A {}
                """,
                """
                package pkg;
                public abstract class A {}
                """,
                """
                package pkg;
                public identity class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );

        // another variation of the assertion above
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract value class A {}
                """,
                """
                package pkg;
                public abstract class A {}
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );

        /* Changing an identity class that is declared abstract to no longer be declared abstract does not break
         * compatibility with pre-existing binaries.
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract identity class A {}
                """,
                """
                package pkg;
                public identity class A {}
                """,
                """
                package pkg;
                public identity class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );

        // another variation of the assertion above
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract identity class A {}
                """,
                """
                package pkg;
                public identity class A {}
                """,
                """
                package pkg;
                public class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );

        /* Modifying a non-abstract identity class to be a value class is a binary compatible change, as long as the class
         * is already final and all its constructors are private. If the class is not final, declaring it a value class
         * has the effect of declaring the class final (13.4.2.3). If the class has a non-private constructor,
         * pre-existing binaries that attempt to invoke that constructor will behave as if the constructor had been
         * removed (13.4.12).
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public final identity class A {}
                """,
                """
                package pkg;
                public value class A {}
                """,
                """
                package pkg;
                public identity class Client {
                    public static void main(String... args) {
                        A a = new A();
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                InstantiationError.class
        );

        // another variation of the assertion above
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public identity class A {}
                """,
                """
                package pkg;
                public value class A {}
                """,
                """
                package pkg;
                public identity class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        /* Modifying a non-abstract value class to be an identity class is a binary compatible change, as long as all
         * of the class's constructors are private. If the class has a non-private constructor, pre-existing binaries
         * that attempt to invoke that constructor will behave as if the constructor had been removed
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public value class A {}
                """,
                """
                package pkg;
                public identity class A {}
                """,
                """
                package pkg;
                public identity class Client {
                    public static void main(String... args) {
                        A a = new A();
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                NoSuchMethodError.class
        );
    }

    @Test
    public void testFieldCompatibility(Path base) throws Exception {
        /* Adding an instance field to an abstract class that is not an identity class also has the side-effect of
         * making the class an identity class
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {}
                """,
                """
                package pkg;
                public abstract class A {
                    int i;
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        // another variation of the assertion above
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {}
                """,
                """
                package pkg;
                public abstract class A {
                    static int i; // OK no instance field
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );

        /* Removing a static modifier from a field of an abstract class that is not an identity class also has the
         * side-effect of making the class an identity class
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {
                    static int i;
                }
                """,
                """
                package pkg;
                public abstract class A {
                    int i;
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );
    }

    @Test
    public void testSynchronizedCompatibility(Path base) throws Exception {
        /* Adding a synchronized modifier to a method of an identity class does not break compatibility with
         * pre-existing binaries
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public identity class A {
                    void m() {}
                }
                """,
                """
                package pkg;
                public identity class A {
                    synchronized void m() {}
                }
                """,
                """
                package pkg;
                public identity class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );

        /* Adding a synchronized modifier to a method of an abstract class that is not an identity class has the
         * side-effect of making the class an identity class
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {
                    void m() {}
                }
                """,
                """
                package pkg;
                public abstract class A {
                    synchronized void m() {}
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        /* Deleting a synchronized modifier of a method does not break compatibility with pre-existing binaries
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {
                    synchronized void m() {}
                }
                """,
                """
                package pkg;
                public abstract class A {
                    void m() {}
                }
                """,
                """
                package pkg;
                public class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );
    }

    @Test
    public void testConstructorCompatibility(Path base) throws Exception {
        /* Adding a throws clause to the constructor of an abstract class that is not an identity class has the
         * side-effect of making the class an identity class
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {
                    public A() {}
                }
                """,
                """
                package pkg;
                public abstract class A {
                    public A() throws Exception {}
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        /* changes to the body of the constructor of an abstract class that is not an identity class, other than adding
         * or removing the super(); call, have the side-effect of making the class an identity class
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {
                    public A() { super(); }
                    void m() {}
                }
                """,
                """
                package pkg;
                public abstract class A {
                    public A() { super(); m(); }
                    void m() {}
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        /* Adding type parameters to the constructor of an abstract class that is not an identity class has the
         * side-effect of making the class an identity class
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {
                    public A() {}
                }
                """,
                """
                package pkg;
                public abstract class A {
                    public <T> A() {}
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        /* Changing the declared access of the constructor of an abstract class that is not an identity class to permit
         * less access may also have the side-effect of making the class an identity class
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public abstract class A {
                    public A() {}
                }
                """,
                """
                package pkg;
                public abstract class A {
                    // this constructor has less access than the class so it will be considered an identity class
                    protected A() {}
                }
                """,
                """
                package pkg;
                public value class Client extends A {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class  //currently failing with this error, but the spec mentions NoSuchMethodError
        );
    }

    @Test
    public void testInterfaceCompatibility(Path base) throws Exception {
        /* If one of the identity or value modifiers is added to an interface, a pre-existing binary that attempts to
         * extend or implement the interface may fail to load. Specifically, if the subclass or subinterface has an
         * incompatible identity or value modifier, or extends a class or interface with an incompatible identity or
         * value modifier, class loading will fail with an IncompatibleClassChangeError
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public interface I {}
                """,
                """
                package pkg;
                public identity interface I {}
                """,
                """
                package pkg;
                public value class Client implements I {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        // another variation of the assertion above
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public interface I {}
                """,
                """
                package pkg;
                public value interface I {}
                """,
                """
                package pkg;
                public identity class Client implements I {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );

        /* Removing one of the identity or value modifiers from an interface does not break compatibility with
         * pre-existing binaries
         */
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public identity interface I {}
                """,
                """
                package pkg;
                public interface I {}
                """,
                """
                package pkg;
                public identity class Client implements I {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );

        // another variation of the assertion above
        testCompatibilityAfterChange(
                base,
                """
                package pkg;
                public value interface I {}
                """,
                """
                package pkg;
                public interface I {}
                """,
                """
                package pkg;
                public value class Client implements I {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """,
                false,
                null
        );
    }
}
