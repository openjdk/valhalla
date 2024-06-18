/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * ValueObjectCompilationTests
 *
 * @test
 * @bug 8287136 8292630 8279368 8287136 8287770 8279840 8279672 8292753 8287763 8279901 8287767 8293183 8293120
 *      8329345
 * @summary Negative compilation tests, and positive compilation (smoke) tests for Value Objects
 * @library /lib/combo /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.compiler/com.sun.tools.javac.code
 *     jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run junit ValueObjectCompilationTests
 */

import java.io.File;

import java.util.List;
import java.util.Set;

import com.sun.tools.javac.util.Assert;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Fieldref_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Methodref_info;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;

import com.sun.tools.javac.code.Flags;

import org.junit.jupiter.api.Test;
import tools.javac.combo.CompilationTestCase;
import toolbox.ToolBox;

class ValueObjectCompilationTests extends CompilationTestCase {

    private static String[] PREVIEW_OPTIONS = {"--enable-preview", "-source",
            Integer.toString(Runtime.version().feature())};

    public ValueObjectCompilationTests() {
        setDefaultFilename("ValueObjectsTest.java");
        setCompileOptions(PREVIEW_OPTIONS);
    }

    @Test
    void testValueModifierConstraints() {
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                value @interface IA {}
                """);
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                value interface I {}
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                class Test {
                    value int x;
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                class Test {
                    value int foo();
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                value enum Enum {}
                """);
    }

    @Test
    void testSuperClassConstraints() {
        assertFail("compiler.err.super.class.method.cannot.be.synchronized",
                """
                abstract class I {
                    synchronized void foo() {}
                }
                value class V extends I {}
                """);
        assertFail("compiler.err.concrete.supertype.for.value.class",
                """
                class ConcreteSuperType {
                    static abstract value class V extends ConcreteSuperType {}  // Error: concrete super.
                }
                """);
        assertOK(
                """
                value record Point(int x, int y) {}
                """);
        assertOK(
                """
                value class V extends Object {}
                """);
        assertFail("compiler.err.value.type.has.identity.super.type",
                """
                abstract class A {}
                value class V extends A {}
                """);
    }

    @Test
    void testRepeatedModifiers() {
        assertFail("compiler.err.repeated.modifier", "value value class ValueTest {}");
    }

    @Test
    void testParserTest() {
        assertOK(
                """
                value class Substring implements CharSequence {
                    private String str;
                    private int start;
                    private int end;

                    public Substring(String str, int start, int end) {
                        checkBounds(start, end, str.length());
                        this.str = str;
                        this.start = start;
                        this.end = end;
                    }

                    public int length() {
                        return end - start;
                    }

                    public char charAt(int i) {
                        checkBounds(0, i, length());
                        return str.charAt(start + i);
                    }

                    public Substring subSequence(int s, int e) {
                        checkBounds(s, e, length());
                        return new Substring(str, start + s, start + e);
                    }

                    public String toString() {
                        return str.substring(start, end);
                    }

                    private static void checkBounds(int start, int end, int length) {
                        if (start < 0 || end < start || length < end)
                            throw new IndexOutOfBoundsException();
                    }
                }
                """
        );
    }

    @Test
    void testSemanticsViolations() {
        assertFail("compiler.err.cant.inherit.from.final",
                """
                value class Base {}
                class Subclass extends Base {}
                """);
        assertFail("compiler.err.cant.assign.val.to.var",
                """
                value class Point {
                    int x = 10;
                    int y;
                    Point (int x, int y) {
                        this.x = x; // Error, final field 'x' is already assigned to.
                        this.y = y; // OK.
                    }
                }
                """);
        assertFail("compiler.err.cant.assign.val.to.var",
                """
                abstract value class Point {
                    int x = 10;
                    int y;
                    Point (int x, int y) {
                        this.x = x; // Error, final field 'x' is already assigned to.
                        this.y = y; // OK.
                    }
                }
                """);
        assertFail("compiler.err.cant.assign.val.to.var",
                """
                value class Point {
                    int x;
                    int y;
                    Point (int x, int y) {
                        this.x = x;
                        this.y = y;
                    }

                    void foo(Point p) {
                        this.y = p.y; // Error, y is final and can't be written outside of ctor.
                    }
                }
                """);
        assertFail("compiler.err.cant.assign.val.to.var",
                """
                abstract value class Point {
                    int x;
                    int y;
                    Point (int x, int y) {
                        this.x = x;
                        this.y = y;
                    }

                    void foo(Point p) {
                        this.y = p.y; // Error, y is final and can't be written outside of ctor.
                    }
                }
                """);
        assertFail("compiler.err.var.might.not.have.been.initialized",
                """
                value class Point {
                    int x;
                    int y;
                    Point (int x, int y) {
                        this.x = x;
                        // y hasn't been initialized
                    }
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                abstract value class V {
                    synchronized void foo() {
                     // Error, abstract value class may not declare a synchronized instance method.
                    }
                }
                """);
        assertOK(
                """
                abstract value class V {
                    static synchronized void foo() {} // OK static
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                value class V {
                    synchronized void foo() {}
                }
                """);
        assertOK(
                """
                value class V {
                    synchronized static void soo() {} // OK static
                }
                """);
        assertFail("compiler.err.type.found.req",
                """
                value class V {
                    { synchronized(this) {} }
                }
                """);
        assertFail("compiler.err.mod.not.allowed.here",
                """
                value record R() {
                    synchronized void foo() { } // Error;
                    synchronized static void soo() {} // OK.
                }
                """);
        assertFail("compiler.err.cant.ref.before.ctor.called",
                """
                value class V {
                    int x;
                    V() {
                        foo(this); // Error.
                        x = 10;
                    }
                    void foo(V v) {}
                }
                """);
        assertFail("compiler.err.cant.ref.before.ctor.called",
                """
                value class V {
                    int x;
                    V() {
                        x = 10;
                        foo(this); // error
                    }
                    void foo(V v) {}
                }
                """);
        assertFail("compiler.err.type.found.req",
                """
                interface I {}
                interface VI extends I {}
                class C {}
                value class VC<T extends VC> {
                    void m(T t) {
                        synchronized(t) {} // error
                    }
                }
                """);
        assertFail("compiler.err.type.found.req",
                """
                interface I {}
                interface VI extends I {}
                class C {}
                value class VC<T extends VC> {
                    void foo(Object o) {
                        synchronized ((VC & I)o) {} // error
                    }
                }
                """);
        // OK if the value class is abstract
        assertOK(
                """
                interface I {}
                abstract value class VI implements I {}
                class C {}
                value class VC<T extends VC> {
                    void bar(Object o) {
                        synchronized ((VI & I)o) {} // error
                    }
                }
                """);
        assertFail("compiler.err.type.found.req", // --enable-preview -source"
                """
                class V {
                    final Integer val = Integer.valueOf(42);
                    void test() {
                        synchronized (val) { // error
                        }
                    }
                }
                """);
        String[] previousOptions = getCompileOptions();
        try {
            setCompileOptions(new String[] {});
            assertOKWithWarning("compiler.warn.attempt.to.synchronize.on.instance.of.value.based.class", // empty options
                    """
                    class V {
                        final Integer val = Integer.valueOf(42);
                        void test() {
                            synchronized (val) { // warn
                            }
                        }
                    }
                    """);
            setCompileOptions(new String[] {"--source",
                    Integer.toString(Runtime.version().feature())});
            assertOKWithWarning("compiler.warn.attempt.to.synchronize.on.instance.of.value.based.class", // --source
                    """
                    class V {
                        final Integer val = Integer.valueOf(42);
                        void test() {
                            synchronized (val) { // warn
                            }
                        }
                    }
                    """);
            setCompileOptions(new String[] {"--enable-preview", "--source",
                    Integer.toString(Runtime.version().feature())});
            assertFail("compiler.err.type.found.req", // --enable-preview --source
                    """
                    class V {
                        final Integer val = Integer.valueOf(42);
                        void test() {
                            synchronized (val) { // warn
                            }
                        }
                    }
                    """);
            /* we should add tests for --release and --release --enable-preview once the stubs used for the latest
             * release have been updated, right now they are the same as those for 22
             */
        } finally {
            setCompileOptions(previousOptions);
        }
    }

    @Test
    void testInteractionWithSealedClasses() {
        assertOK(
                """
                abstract sealed value class SC {}
                value class VC extends SC {}
                """
        );
        assertOK(
                """
                abstract sealed interface SI {}
                value class VC implements SI {}
                """
        );
        assertOK(
                """
                abstract sealed class SC {}
                final class IC extends SC {}
                non-sealed class IC2 extends SC {}
                final class IC3 extends IC2 {}
                """
        );
        assertOK(
                """
                abstract sealed interface SI {}
                final class IC implements SI {}
                non-sealed class IC2 implements SI {}
                final class IC3 extends IC2 {}
                """
        );
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                abstract sealed value class SC {}
                non-sealed value class VC extends SC {}
                """
        );
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                sealed value class SI {}
                non-sealed value class VC extends SI {}
                """
        );
    }

    @Test
    void testCheckClassFileFlags() throws Exception {
        for (String source : List.of(
                """
                interface I {}
                class Test {
                    I i = new I() {};
                }
                """,
                """
                class C {}
                class Test {
                    C c = new C() {};
                }
                """,
                """
                class Test {
                    Object o = new Object() {};
                }
                """,
                """
                class Test {
                    abstract class Inner {}
                }
                """
        )) {
            File dir = assertOK(true, source);
            for (final File fileEntry : dir.listFiles()) {
                if (fileEntry.getName().contains("$")) {
                    ClassFile classFile = ClassFile.read(fileEntry);
                    Assert.check((classFile.access_flags.flags & Flags.ACC_IDENTITY) != 0);
                }
            }
        }

        for (String source : List.of(
                """
                class C {}
                """,
                """
                abstract class A {
                    int i;
                }
                """,
                """
                abstract class A {
                    synchronized void m() {}
                }
                """,
                """
                class C {
                    synchronized void m() {}
                }
                """,
                """
                abstract class A {
                    int i;
                    { i = 0; }
                }
                """,
                """
                abstract class A {
                    A(int i) {}
                }
                """,
                """
                    enum E {}
                """,
                """
                    record R() {}
                """
        )) {
            File dir = assertOK(true, source);
            for (final File fileEntry : dir.listFiles()) {
                ClassFile classFile = ClassFile.read(fileEntry);
                Assert.check(classFile.access_flags.is(Flags.ACC_IDENTITY));
            }
        }

        {
            String source =
                    """
                    abstract value class A {}
                    value class Sub extends A {} //implicitly final
                    """;
            File dir = assertOK(true, source);
            for (final File fileEntry : dir.listFiles()) {
                ClassFile classFile = ClassFile.read(fileEntry);
                switch (classFile.getName()) {
                    case "Sub":
                        Assert.check((classFile.access_flags.flags & (Flags.FINAL)) != 0);
                        break;
                    case "A":
                        Assert.check((classFile.access_flags.flags & (Flags.ABSTRACT)) != 0);
                        break;
                    default:
                        throw new AssertionError("you shoulnd't be here");
                }
            }
        }

        for (String source : List.of(
                """
                value class V {
                    int i = 0;
                    static int j;
                }
                """,
                """
                abstract value class A {
                    static int j;
                }

                value class V extends A {
                    int i = 0;
                }
                """
        )) {
            File dir = assertOK(true, source);
            for (final File fileEntry : dir.listFiles()) {
                ClassFile classFile = ClassFile.read(fileEntry);
                for (Field field : classFile.fields) {
                    if (!field.access_flags.is(Flags.STATIC)) {
                        Set<String> fieldFlags = field.access_flags.getFieldFlags();
                        Assert.check(fieldFlags.size() == 2 && fieldFlags.contains("ACC_FINAL") && fieldFlags.contains("ACC_STRICT"));
                    }
                }
            }
        }

        // testing experimental @Strict annotation
        String[] previousOptions = getCompileOptions();
        try {
            String[] testOptions = {"--add-exports", "java.base/jdk.internal.vm.annotation=ALL-UNNAMED"};
            setCompileOptions(testOptions);
            for (String source : List.of(
                    """
                    import jdk.internal.vm.annotation.Strict;
                    class Test {
                        @Strict int i;
                    }
                    """,
                    """
                    import jdk.internal.vm.annotation.Strict;
                    class Test {
                        @Strict final int i = 0;
                    }
                    """
            )) {
                File dir = assertOK(true, source);
                for (final File fileEntry : dir.listFiles()) {
                    ClassFile classFile = ClassFile.read(fileEntry);
                    for (Field field : classFile.fields) {
                        if (!field.access_flags.is(Flags.STATIC)) {
                            Set<String> fieldFlags = field.access_flags.getFieldFlags();
                            Assert.check(fieldFlags.contains("ACC_STRICT"));
                        }
                    }
                }
            }
        } finally {
            setCompileOptions(previousOptions);
        }
    }

    @Test
    void testConstruction() throws Exception {
        record Data(String src, boolean isRecord) {}
        /*for (Data data : List.of(
                new Data(
                    """
                    value class Test {
                        int i = 100;
                    }
                    """, false),
                new Data(
                    """
                    value class Test {
                        int i;
                        Test() {
                            i = 100;
                        }
                    }
                    """, false),
                new Data(
                    """
                    value class Test {
                        int i;
                        Test() {
                            i = 100;
                            super();
                        }
                    }
                    """, false),
                new Data(
                    """
                    value class Test {
                        int i;
                        Test() {
                            this.i = 100;
                            super();
                        }
                    }
                    """, false),
                new Data(
                    """
                    value record Test(int i) {}
                    """, true)
        )) {
            String expectedCodeSequence = "aload_0,bipush,putfield,aload_0,invokespecial,return,";
            String expectedCodeSequenceRecord = "aload_0,iload_1,putfield,aload_0,invokespecial,return,";
            File dir = assertOK(true, data.src);
            for (final File fileEntry : dir.listFiles()) {
                ClassFile classFile = ClassFile.read(fileEntry);
                for (Method method : classFile.methods) {
                    if (method.getName(classFile.constant_pool).equals("<init>")) {
                        Code_attribute code = (Code_attribute)method.attributes.get("Code");
                        String foundCodeSequence = "";
                        for (Instruction inst: code.getInstructions()) {
                            foundCodeSequence += inst.getMnemonic() + ",";
                        }
                        if (!data.isRecord) {
                            Assert.check(expectedCodeSequence.equals(foundCodeSequence));
                        } else {
                            Assert.check(expectedCodeSequenceRecord.equals(foundCodeSequence));
                        }
                    }
                }
            }
        }
        */
/*
        String source =
                """
                value class Test {
                    int i = 100;
                    int j;
                    {
                        j = 200;
                    }
                }
                """;
        String expectedCodeSequence = "aload_0,bipush,putfield,aload_0,invokespecial,aload_0,sipush,putfield,return,";
        File dir = assertOK(true, source);
        for (final File fileEntry : dir.listFiles()) {
            ClassFile classFile = ClassFile.read(fileEntry);
            for (Method method : classFile.methods) {
                if (method.getName(classFile.constant_pool).equals("<init>")) {
                    Code_attribute code = (Code_attribute)method.attributes.get("Code");
                    String foundCodeSequence = "";
                    for (Instruction inst: code.getInstructions()) {
                        foundCodeSequence += inst.getMnemonic() + ",";
                    }
                    Assert.check(expectedCodeSequence.equals(foundCodeSequence));
                }
            }
        }
*/
        assertFail("compiler.err.cant.ref.before.ctor.called",
                """
                value class Test {
                    Test() {
                        m();
                    }
                    void m() {}
                }
                """
        );
        assertFail("compiler.err.cant.ref.after.ctor.called",
                """
                value class Test {
                    int i;
                    Test() {
                        super();
                        this.i = i;
                    }
                }
                """
        );
        assertOK(
                """
                class UnrelatedThisLeak {
                    value class V {
                        int f;
                        V() {
                            UnrelatedThisLeak x = UnrelatedThisLeak.this;
                            f = 10;
                            x = UnrelatedThisLeak.this;
                        }
                    }
                }
                """
        );
    }

    @Test
    void testThisCallingConstructor() throws Exception {
        // make sure that this() calling constructors doesn't initialize final fields
        String source =
                """
                value class Test {
                    int i;
                    Test() {
                        this(0);
                    }

                    Test(int i) {
                        this.i = i;
                    }
                }
                """;
        File dir = assertOK(true, source);
        File fileEntry = dir.listFiles()[0];
        ClassFile classFile = ClassFile.read(fileEntry);
        String expectedCodeSequenceThisCallingConst = "aload_0,iconst_0,invokespecial,return,";
        String expectedCodeSequenceNonThisCallingConst = "aload_0,iload_1,putfield,aload_0,invokespecial,return,";
        for (Method method : classFile.methods) {
            if (method.getName(classFile.constant_pool).equals("<init>")) {
                if (method.descriptor.getParameterCount(classFile.constant_pool) == 0) {
                    Code_attribute code = (Code_attribute)method.attributes.get("Code");
                    String foundCodeSequence = "";
                    for (Instruction inst: code.getInstructions()) {
                        foundCodeSequence += inst.getMnemonic() + ",";
                    }
                    Assert.check(expectedCodeSequenceThisCallingConst.equals(foundCodeSequence));
                } else {
                    Code_attribute code = (Code_attribute)method.attributes.get("Code");
                    String foundCodeSequence = "";
                    for (Instruction inst: code.getInstructions()) {
                        foundCodeSequence += inst.getMnemonic() + ",";
                    }
                    Assert.check(expectedCodeSequenceNonThisCallingConst.equals(foundCodeSequence));
                }
            }
        }
    }

    @Test
    void testSelectors() throws Exception {
        assertOK(
                """
                value class V {
                    void selector() {
                        Class<?> c = int.class;
                    }
                }
                """
        );
        assertFail("compiler.err.expected",
                """
                value class V {
                    void selector() {
                        int i = int.some_selector;
                    }
                }
                """
        );
    }

    @Test
    void testAnonymousValue() throws Exception {
        assertOK(
                """
                class Test {
                    void m() {
                        Object o = new value Comparable<String>() {
                            @Override
                            public int compareTo(String o) {
                                return 0;
                            }
                        };
                    }
                }
                """
        );
        assertOK(
                """
                class Test {
                    void m() {
                        Object o = new value Comparable<>() {
                            @Override
                            public int compareTo(Object o) {
                                return 0;
                            }
                        };
                    }
                }
                """
        );
    }

    @Test
    void testNullAssigment() throws Exception {
        assertOK(
                """
                value final class V {
                    final int x = 10;

                    value final class X {
                        final V v;
                        final V v2;

                        X() {
                            this.v = null;
                            this.v2 = null;
                        }

                        X(V v) {
                            this.v = v;
                            this.v2 = v;
                        }

                        V foo(X x) {
                            x = new X(null);  // OK
                            return x.v;
                        }
                    }
                    V bar(X x) {
                        x = new X(null); // OK
                        return x.v;
                    }

                    class Y {
                        V v;
                        V [] va = { null }; // OK: array initialization
                        V [] va2 = new V[] { null }; // OK: array initialization
                        void ooo(X x) {
                            x = new X(null); // OK
                            v = null; // legal assignment.
                            va[0] = null; // legal.
                            va = new V[] { null }; // legal
                        }
                    }
                }
                """
        );
    }

    private File findClassFileOrFail(File dir, String name) {
        for (final File fileEntry : dir.listFiles()) {
            if (fileEntry.getName().equals(name)) {
                return fileEntry;
            }
        }
        throw new AssertionError("file not found");
    }

    private Attribute findAttributeOrFail(Attributes attributes, Class<? extends Attribute> attrClass, int numberOfAttributes) {
        int attrCount = 0;
        Attribute result = null;
        for (Attribute attribute : attributes) {
            if (attribute.getClass() == attrClass) {
                attrCount++;
                if (result == null) {
                    result = attribute;
                }
            }
        }
        if (attrCount == 0) throw new AssertionError("attribute not found");
        if (attrCount != numberOfAttributes) throw new AssertionError("incorrect number of attributes found");
        return result;
    }

    private void checkAttributeNotPresent(Attributes attributes, Class<? extends Attribute> attrClass) {
        for (Attribute attribute : attributes) {
            if (attribute.getClass() == attrClass) {
                throw new AssertionError("attribute found");
            }
        }
    }
}
