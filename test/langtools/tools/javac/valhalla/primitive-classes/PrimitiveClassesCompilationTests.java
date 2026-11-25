/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary implement primitive classes declaration
 * @enablePreview
 * @library /lib/combo /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.compiler/com.sun.tools.javac.code
 * @run junit PrimitiveClassesCompilationTests
 */

import java.io.File;
import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.reflect.AccessFlag;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.sun.tools.javac.util.Assert;

import org.junit.jupiter.api.Test;
import tools.javac.combo.CompilationTestCase;

class PrimitiveClassesCompilationTests extends CompilationTestCase {

    private static final String[] PREVIEW_OPTIONS = {
            "--enable-preview",
            "-source", Integer.toString(Runtime.version().feature())
    };

    private static final String[] NO_PREVIEW_OPTIONS = {
            "-source", Integer.toString(Runtime.version().feature())
    };

    public PrimitiveClassesCompilationTests() {
        setDefaultFilename("PrimitiveClassTest.java");
        setCompileOptions(PREVIEW_OPTIONS);
    }

    @Test
    void testDeclarations() {
        for (String source : List.of(
                """
                primitive class p {
                    public p() {}
                }
                """,
                """
                primitive class p {
                    int x;
                    public p() { x = 0; }
                    p(int x) { this.x = x; }
                }
                """,
                """
                final primitive class p {
                    public p() {}
                }
                """,
                """
                primitive final class p {
                    public p() {}
                }
                """,
                """
                @Deprecated primitive class p {
                    public p() {}
                }
                """,
                """
                primitive @Deprecated class p {
                    public p() {}
                }
                """,
                """
                class Outer {
                    primitive class inner {
                        public inner() {}
                    }
                    static primitive class nested {
                        public nested() {}
                    }
                    primitive static class nested2 {
                        public nested2() {}
                    }
                    private primitive class privateinner {
                        public privateinner() {}
                    }
                }
                """,
                """
                interface I {
                    primitive class member {
                        public member() {}
                    }
                }
                """,
                """
                class Outer {
                    void m() {
                        primitive class local {
                            public local() {}
                        }
                        final primitive class local2 {
                            public local2() {}
                        }
                        primitive final class local3 {
                            public local3() {}
                        }
                    }
                }
                """,
                // a primitive class is implicitly final, so it can be a permitted subclass
                """
                sealed interface s permits p {}
                primitive class p implements s {
                    public p() {}
                }
                """)) {
            assertOK(source);
        }
    }

    @Test
    void testClassFile() {
        File dir = assertOK(true,
                """
                primitive class p {
                    int x;
                    public p() { x = 0; }
                    p(int x) { this.x = x; }
                }
                """);
        ClassModel cm = parse(dir, "p.class");
        Assert.check((cm.flags().flagsMask() & ClassFile.ACC_IDENTITY) == 0, "a primitive class is not an identity class");
        Assert.check((cm.flags().flagsMask() & ClassFile.ACC_FINAL) != 0, "a primitive class is implicitly final");
        Assert.check(cm.minorVersion() == ClassFile.PREVIEW_MINOR_VERSION, "expected a preview class file");
        Assert.check(cm.fields().size() == 1);
        Set<AccessFlag> fieldFlags = cm.fields().get(0).flags().flags();
        Assert.check(fieldFlags.equals(Set.of(AccessFlag.FINAL, AccessFlag.STRICT_INIT)),
                "unexpected field flags: " + fieldFlags);

        dir = assertOK(true,
                """
                class Outer {
                    primitive class inner {
                        public inner() {}
                    }
                    class Inner {
                        primitive class deep {
                            public deep() {}
                        }
                    }
                    void m() {
                        primitive class local {
                            public local() {}
                        }
                    }
                }
                """);
        for (String name : List.of("Outer$inner.class", "Outer$Inner$deep.class", "Outer$1local.class")) {
            cm = parse(dir, name);
            Assert.check((cm.flags().flagsMask() & ClassFile.ACC_IDENTITY) == 0, name + ": a primitive class is not an identity class");
            Assert.check((cm.flags().flagsMask() & ClassFile.ACC_FINAL) != 0, name + ": a primitive class is implicitly final");
            // a nested primitive class is implicitly static, even in an inner class: it has no
            // enclosing instance
            String thisClass = cm.thisClass().asInternalName();
            InnerClassInfo info = cm.findAttribute(Attributes.innerClasses()).orElseThrow().classes().stream()
                    .filter(ic -> ic.innerClass().asInternalName().equals(thisClass))
                    .findFirst().orElseThrow();
            Assert.check(info.has(AccessFlag.STATIC), name + ": a nested primitive class is implicitly static");
            Assert.check(cm.fields().isEmpty(), name + ": a nested primitive class has no field for an enclosing instance");
            for (MethodModel method : cm.methods()) {
                if (method.methodName().equalsString("<init>")) {
                    Assert.check(method.methodType().equalsString("()V"),
                            name + ": the constructor takes an enclosing instance: " + method.methodType());
                }
            }
        }    }

    @Test
    void testPreviewDisabled() {
        String[] previousOptions = getCompileOptions();
        try {
            setCompileOptions(NO_PREVIEW_OPTIONS);
            for (String source : List.of(
                    """
                    primitive class p {
                        public p() {}
                    }
                    """,
                    """
                    class Outer {
                        primitive class inner {
                            public inner() {}
                        }
                    }
                    """)) {
                assertFail("compiler.err.preview.feature.disabled.plural",
                        d -> Assert.check(d.getMessage(Locale.ROOT).startsWith("primitive classes are a preview feature"),
                                d.getMessage(Locale.ROOT)),
                        source);
            }
            // the declaration rules are not checked: the preview error is the only error
            assertFail("compiler.err.preview.feature.disabled.plural",
                    d -> Assert.check(diags.errorKeys().equals(List.of("compiler.err.preview.feature.disabled.plural")),
                            diags.toString()),
                    """
                    primitive class point {
                        public point() {}
                    }
                    """);
        } finally {
            setCompileOptions(previousOptions);
        }
    }

    @Test
    void testPrimitiveAsIdentifier() {
        String source =
                """
                class Test {
                    int primitive;
                    java.util.function.IntUnaryOperator op = primitive -> primitive + 1;
                    int primitive() { return primitive; }
                    void m(boolean primitive) {
                        int i = primitive ? 1 : 0;
                        primitive = !primitive;
                    }
                    void n() {
                        int primitive = 0;
                        primitive++;
                        primitive = primitive + this.primitive + primitive();
                        primitive();
                        primitive: for (;;) { break primitive; }
                    }
                    void o(String primitive) {
                        primitive.length();
                    }
                    void p(int[] primitive) {
                        primitive[0] = 1;
                    }
                }
                """;
        assertOK(source);
        String[] previousOptions = getCompileOptions();
        try {
            setCompileOptions(NO_PREVIEW_OPTIONS);
            assertOK(source);
        } finally {
            setCompileOptions(previousOptions);
        }
    }

    @Test
    void testPrimitiveAsTypeName() {
        // like `value`: an error with preview enabled, a warning without it
        assertFail("compiler.err.restricted.type.not.allowed",
                """
                class primitive {}
                """);
        String[] previousOptions = getCompileOptions();
        try {
            setCompileOptions(NO_PREVIEW_OPTIONS);
            assertOKWithWarning("compiler.warn.restricted.type.not.allowed.preview",
                    """
                    class primitive {}
                    """);
        } finally {
            setCompileOptions(previousOptions);
        }
    }

    @Test
    void testModifierErrors() {
        // `primitive` together with `value` is an error, in either order
        for (String source : List.of(
                """
                primitive value class c {}
                """,
                """
                value primitive class c {}
                """,
                """
                class Outer {
                    void m() {
                        primitive value class local {}
                    }
                }
                """,
                """
                class Outer {
                    void m() {
                        value primitive class local {}
                    }
                }
                """)) {
            assertFail("compiler.err.illegal.combination.of.modifiers",
                    d -> Assert.check("illegal combination of modifiers: primitive and value".equals(d.getMessage(Locale.ROOT)),
                            d.getMessage(Locale.ROOT)),
                    source);
        }
        assertFail("compiler.err.repeated.modifier",
                """
                primitive primitive class c {}
                """);
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                primitive interface i {}
                """);
        assertFail("compiler.err.illegal.combination.of.modifiers",
                """
                primitive @interface a {}
                """);
        // a primitive class can't be abstract
        for (String source : List.of(
                """
                abstract primitive class c {}
                """,
                """
                primitive abstract class c {}
                """,
                """
                class Outer {
                    abstract primitive class member {}
                }
                """,
                """
                class Outer {
                    void m() {
                        abstract primitive class local {}
                    }
                }
                """)) {
            assertFail("compiler.err.illegal.combination.of.modifiers",
                    d -> Assert.check("illegal combination of modifiers: primitive and abstract".equals(d.getMessage(Locale.ROOT)),
                            d.getMessage(Locale.ROOT)),
                    source);
        }
        for (String source : List.of(
                """
                primitive enum e {}
                """,
                """
                primitive record r() {}
                """,
                """
                class Test {
                    primitive int x;
                }
                """,
                """
                class Test {
                    primitive void m() {}
                }
                """)) {
            assertFail("compiler.err.mod.not.allowed.here",
                    d -> Assert.check("modifier primitive not allowed here".equals(d.getMessage(Locale.ROOT)),
                            d.getMessage(Locale.ROOT)),
                    source);
        }
        // a primitive class is a value class, and only abstract value classes can be sealed or non-sealed
        for (String source : List.of(
                """
                sealed primitive class p {
                    public p() {}
                }
                """,
                """
                non-sealed primitive class p {
                    public p() {}
                }
                """)) {
            assertFail("compiler.err.non.abstract.value.class.cant.be.sealed.or.non.sealed", source);
        }
    }

    @Test
    void testNestedIsStatic() {
        // a nested primitive class is implicitly static, but it can create its own instances
        assertOK(
                """
                class Outer {
                    void m() {
                        primitive class local {
                            int x;
                            public local() { this(0); }
                            local(int x) { this.x = x; }
                            local next() { return new local(x + 1); }
                            java.util.function.Supplier<local> factory() { return local::new; }
                        }
                        new local().next();
                    }
                }
                """);
        // it has no enclosing instance and captures nothing, like a nested record
        for (String source : List.of(
                """
                class Outer {
                    int x;
                    primitive class member {
                        public member() {}
                        int get() { return x; }
                    }
                }
                """,
                """
                class Outer {
                    int x;
                    void m() {
                        primitive class local {
                            public local() {}
                            int get() { return x; }
                        }
                    }
                }
                """,
                """
                class Outer {
                    void m() {
                        int x = 0;
                        primitive class local {
                            public local() {}
                            int get() { return x; }
                        }
                    }
                }
                """,
                """
                class Outer {
                    <T> void m() {
                        primitive class local {
                            public local() {}
                            void n(T t) {}
                        }
                    }
                }
                """)) {
            assertFail("compiler.err.non-static.cant.be.ref", source);
        }
        assertFail("compiler.err.qualified.new.of.static.class",
                """
                class Outer {
                    primitive class member {
                        public member() {}
                    }
                    member m() {
                        return new Outer().new member();
                    }
                }
                """);
    }

    @Test
    void testNames() {
        // the name of a primitive class can contain only lowercase letters and digits
        assertOK(
                """
                primitive class uint64 {
                    public uint64() {}
                }
                """);
        for (String source : List.of(
                """
                primitive class Complex {
                    public Complex() {}
                }
                """,
                """
                primitive class uint_64 {
                    public uint_64() {}
                }
                """,
                """
                class Outer {
                    primitive class Member {
                        public Member() {}
                    }
                }
                """,
                """
                class Outer {
                    void m() {
                        primitive class Local {
                            public Local() {}
                        }
                    }
                }
                """)) {
            assertFail("compiler.err.illegal.primitive.class.name", source);
        }
    }

    @Test
    void testNoArgConstructor() {
        assertOK(
                """
                primitive class p {
                    int x;
                    public p() { this(0); }
                    p(int x) { this.x = x; }
                }
                """);
        for (String source : List.of(
                // javac generates no default constructor, not even for a public class
                """
                primitive class p {}
                """,
                """
                class Outer {
                    public primitive class p {}
                }
                """,
                """
                class Outer {
                    void m() {
                        primitive class local {}
                    }
                }
                """,
                """
                primitive class p {
                    int x;
                    public p(int x) { this.x = x; }
                }
                """,
                """
                primitive class p {
                    public p(int... xs) {}
                }
                """,
                // the no-argument constructor must be public
                """
                primitive class p {
                    p() {}
                }
                """,
                """
                primitive class p {
                    protected p() {}
                }
                """,
                """
                primitive class p {
                    private p() {}
                }
                """)) {
            assertFail("compiler.err.primitive.class.must.declare.public.no.arg.constructor", source);
        }
        // with no default constructor, `new p()` doesn't resolve either
        assertFail("compiler.err.cant.resolve.location.args",
                """
                primitive class p {
                    static p make() { return new p(); }
                }
                """);
        // like any value class constructor, it must assign every field (Q14)
        assertFail("compiler.err.strict.field.not.have.been.initialized.before.super",
                """
                primitive class p {
                    int x, y;
                    public p() { x = 0; }
                }
                """);
    }

    @Test
    void testFieldTypes() {
        // instance fields have primitive types or primitive class types; static fields can have
        // any type
        assertOK(
                """
                primitive class point {
                    static Object origin;
                    double x, y;
                    public point() { x = 0; y = 0; }
                }
                primitive class line {
                    static line unit;
                    point start, end;
                    public line() { start = new point(); end = new point(); }
                }
                """);
        for (String source : List.of(
                """
                primitive class p {
                    String s;
                    public p() { s = ""; }
                }
                """,
                """
                primitive class p {
                    Integer i;
                    public p() { i = 0; }
                }
                """,
                """
                primitive class p {
                    int[] a;
                    public p() { a = new int[0]; }
                }
                """,
                // a value class that isn't a primitive class
                """
                value class v {}
                primitive class p {
                    v f;
                    public p() { f = new v(); }
                }
                """,
                // a type variable
                """
                primitive class box<T> {
                    T t;
                    public box() { t = null; }
                }
                """)) {
            assertFail("compiler.err.illegal.primitive.class.field.type", source);
        }
    }

    @Test
    void testCyclicFields() {
        // a field must not lead back to its own class, directly or through other primitive classes;
        // each class on the cycle gets the error, at its field
        assertCyclic(
                """
                primitive class node {
                    node next;
                    public node() { next = new node(); }
                }
                """,
                "node");
        assertCyclic(
                """
                primitive class a {
                    b x;
                    public a() { x = new b(); }
                }
                primitive class b {
                    a y;
                    public b() { y = new a(); }
                }
                """,
                "a", "b");
        // reaching the same class twice is not a cycle
        assertOK(
                """
                primitive class d {
                    int i;
                    public d() { i = 0; }
                }
                primitive class b {
                    d x;
                    public b() { x = new d(); }
                }
                primitive class c {
                    d y;
                    public c() { y = new d(); }
                }
                primitive class a {
                    b f;
                    c g;
                    public a() { f = new b(); g = new c(); }
                }
                """);
    }

    /** Asserts that the source fails, and that its cyclic membership errors name these classes, in
     *  this order.
     */
    private void assertCyclic(String source, String... classes) {
        String key = "compiler.err.cyclic.primitive.class.membership";
        List<String> expected = List.of(classes).stream()
                .map(c -> "cyclic primitive class membership involving " + c)
                .toList();
        assertFail(key,
                d -> {
                    List<String> found = diags.getAllDiags().stream()
                            .filter(e -> e.getCode().equals(key))
                            .map(e -> e.getMessage(Locale.ROOT))
                            .toList();
                    Assert.check(found.equals(expected), found.toString());
                },
                source);
    }

    private static ClassModel parse(File dir, String name) {
        try {
            return ClassFile.of().parse(new File(dir, name).toPath());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
