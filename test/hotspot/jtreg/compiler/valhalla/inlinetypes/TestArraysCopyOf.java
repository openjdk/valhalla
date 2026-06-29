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
 * @bug 8382226
 * @summary Test Arrays.copyOf with template tests
 * @library /test/lib /
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main ${test.main.class} 0
 */

/*
 * @test
 * @bug 8382226
 * @summary Test Arrays.copyOf with template tests
 * @library /test/lib /
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main ${test.main.class} 1
 */

/*
 * @test
 * @bug 8382226
 * @summary Test Arrays.copyOf with template tests
 * @library /test/lib /
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main ${test.main.class} 2
 */

/*
 * @test
 * @bug 8382226
 * @summary Test Arrays.copyOf with template tests
 * @library /test/lib /
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main ${test.main.class} 3
 */

/*
 * @test
 * @bug 8382226
 * @summary Test Arrays.copyOf with template tests
 * @library /test/lib /
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main ${test.main.class} 4
 */

/*
 * @test
 * @bug 8382226
 * @summary Test Arrays.copyOf with template tests
 * @library /test/lib /
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main ${test.main.class} 5
 */

/*
 * @test
 * @bug 8382226
 * @summary Test Arrays.copyOf with template tests
 * @library /test/lib /
 * @enablePreview
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main ${test.main.class} 6
 */


package compiler.valhalla.inlinetypes;

import compiler.lib.compile_framework.CompileFramework;
import compiler.lib.ir_framework.Scenario;
import compiler.lib.template_framework.*;
import compiler.lib.template_framework.library.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static compiler.lib.template_framework.Template.*;
import static compiler.lib.template_framework.Template.scope;

public class TestArraysCopyOf {
    public static void main(String[] args) {
        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;
        scenarios[2].addFlags("--enable-preview", "-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast", "-XX:+StressArrayCopyMacroNode");
        scenarios[3].addFlags("--enable-preview", "-XX:-MonomorphicArrayCheck", "-XX:+UnlockDiagnosticVMOptions", "-XX:+UseArrayFlattening", "-XX:-UncommonNullCast");
        scenarios[4].addFlags("--enable-preview", "-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast");
        scenarios[5].addFlags("--enable-preview", "-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast", "-XX:+StressArrayCopyMacroNode");

        String[] flagsForScenario = scenarios[Integer.parseInt(args[0])].getFlags().toArray(new String[0]);
        CompileFramework comp = new CompileFramework();
        comp.addJavaSourceCode("compiler.valhalla.inlinetypes.TestArraysCopyOfGenerated", generate(comp));
        comp.compile("--enable-preview", "--source", "28",
                     "--add-exports", "java.base/jdk.internal.value=ALL-UNNAMED",
                     "--add-exports", "java.base/jdk.internal.vm.annotation=ALL-UNNAMED",
                     "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED");
        comp.invoke("compiler.valhalla.inlinetypes.TestArraysCopyOfGenerated", "main",
                    new Object[]{ flagsForScenario });
    }

    private record Klass(String name, String definition) {}

    private static String generate(CompileFramework comp) {
        List<PrimitiveType> primitiveTypes = CodeGenerationDataNameType.PRIMITIVE_TYPES;
        List<String> primitiveTypeClasses = primitiveTypes.stream()
                .map(PrimitiveType::name).toList();
        List<String> instanceAndPrimitiveClasses =
                new ArrayList<>(CodeGenerationDataNameType.PRIMITIVE_TYPES.stream()
                                        .map(PrimitiveType::name).toList());

        List<Klass> nonValueClasses = List.of(
                new Klass("V", "static class V extends AV"),
                new Klass("W", "static class W"),
                new Klass("X", "static class X extends A"),
                new Klass("Y", "static class Y implements I"),
                new Klass("Z", "static class Z extends AV")
                                             );

        List<Klass> valueClasses = List.of(
                new Klass("V1", "static value class V1"),
                new Klass("V2", "static value class V2 extends AV"),
                new Klass("V3", "static value class V3 implements I"),
                new Klass("V4", "@LooselyConsistentValue\nstatic value class V4"),
                new Klass("V5", "@LooselyConsistentValue\nstatic value class V5 extends AV"),
                new Klass("V6", "@LooselyConsistentValue\nstatic value class V6 implements I")
                                          );

        List<Klass> concreteInstanceClasses = new ArrayList<>(nonValueClasses);
        concreteInstanceClasses.addAll(valueClasses);

        List<Klass> abstractClasses = new ArrayList<>(List.of(
                new Klass("A", "static abstract class A"),
                new Klass("AV", "static abstract value class AV")));

        instanceAndPrimitiveClasses.addAll(concreteInstanceClasses.stream().map(Klass::name).toList());
        instanceAndPrimitiveClasses.addAll(abstractClasses.stream().map(Klass::name).toList());

        List<?> newValueClassArrayScopes = List.of(
                scope("(#klass)ValueClass.newReferenceArray(#klass_name.class, length)"),
                scope("(#klass)ValueClass.newNullableAtomicArray(#klass_name.class, length)"),
                scope("(#klass)ValueClass.newNullRestrictedAtomicArray(#klass_name.class, length, #klass_name.init())"),
                scope("(#klass)ValueClass.newNullRestrictedNonAtomicArray(#klass_name.class, length, #klass_name.init())"));

        AtomicInteger uniqueIndex = new AtomicInteger(0);
        var template = Template.make(() -> scope(
                """

                            static interface I {}

                        """,
                abstractClasses.stream().map(klass -> scope(
                        let("def", klass.definition()),
                        let("klass", klass.name()),
                        """

                            #def {
                        """,
                primitiveTypes.stream().map(type -> scope(
                        let("type", type.name()),
                        let("con", type.con()),
                        """
                                #type _#type = #con;
                        """)).toList(),
                        """
                            }
                        """)).toList(),
                concreteInstanceClasses.stream().map(klass -> scope(
                        let("def", klass.definition()),
                        let("klass", klass.name()),
                        """

                            #def {
                        """,
                        primitiveTypes.stream().map(type -> scope(
                        let("type", type.name()),
                        """
                                #type _#type;
                        """)).toList(),
                        """

                                public #klass(\
                        """,
                concat(primitiveTypes, (type, _) -> scope(type.name() + " _" + type.name())),
                        ") {\n",
                loop(primitiveTypes, (type, _) -> scope(
                        let("type", type.name()),
                        "            this._#type = _#type;\n")),
                        """
                                }

                                static #klass init() {
                                    return new #klass(\
                        """,
                concat(primitiveTypes, (type, _) -> scope(type.con())),
                        ");\n",
                        """
                                }
                            }
                        """)).toList(),
                        """

                            static Object[] oArr = new Object[1];
                        """,
                loop(instanceAndPrimitiveClasses.size(), i -> scope(
                        let("i", uniqueIndex.getAndIncrement()),
                        let("klass", instanceAndPrimitiveClasses.get(i)),
                        let("test", "testNonArrayClass" + instanceAndPrimitiveClasses.get(i)),
                        generateTestMethodString(),
                        """

                            @Run(test = "#test")
                            public static void run#i() {
                                try {
                                    #test(oArr, 1);
                                    Asserts.fail("Should throw");
                                } catch (NullPointerException e) {
                                    // expected
                                }
                            }
                        """)),
                loop(primitiveTypeClasses.size(), i -> scope(
                        let("i", uniqueIndex.getAndIncrement()),
                        let("klass", primitiveTypeClasses.get(i) + "[]"),
                        let("test", "testPrimitiveArrayClass" + primitiveTypeClasses.get(i)),
                        generateTestMethodString(),
                        """

                            @Run(test = "#test")
                            public static void run#i() {
                                try {
                                    #test(oArr, 1);
                                    Asserts.fail("Should throw");
                                } catch (ArrayStoreException e) {
                                    // expected
                                }
                            }
                        """)),
                loop(concreteInstanceClasses.size(), i -> scope(
                        let("i", uniqueIndex.getAndIncrement()),
                        let("klass_name", concreteInstanceClasses.get(i).name()),
                        let("klass", concreteInstanceClasses.get(i).name() + "[]"),
                        let("test", "testInstanceArrayClass" + concreteInstanceClasses.get(i).name()),
                        generateTestMethodString(),
                        """

                            @Run(test = "#test")
                            public static void run#i() {
                                int length = 4;
                                #klass arr = new #klass_name[length];
                                #klass golden = new #klass_name[length];
                                for (int i = 0; i < length; i++) {
                                    arr[i] = #klass_name.init();
                                    golden[i] = #klass_name.init();
                                }

                                #klass res = (#klass)#test(arr, length);

                                for (int i = 0; i < length; i++) {
                                    Verify.checkEQ(res[i], golden[i]);
                                }
                            }
                        """)),
                loop(newValueClassArrayScopes, (init, i) -> scope(
                        let("i", uniqueIndex.getAndIncrement()),
                        let("klass_name", valueClasses.get(i).name()),
                        let("klass", valueClasses.get(i).name() + "[]"),
                        let("test", "testValueArrayClass" + valueClasses.get(i).name()),
                        generateTestMethodString(),
                        """

                            @Run(test = "#test")
                            public static void run#i() {
                                int length = 4;
                        """,
                        "       #klass arr =", init, ";\n",
                        "       #klass golden =", init, ";\n",
                        """

                                #klass res = (#klass)#test(arr, length);

                                for (int i = 0; i < length; i++) {
                                    Asserts.assertEQ(res[i], golden[i]);
                                }
                            }
                        """))
        ));

        return TestFrameworkClass.render("compiler.valhalla.inlinetypes",
                                         "TestArraysCopyOfGenerated",
                                         Set.of("jdk.test.lib.Asserts",
                                                "java.util.Arrays",
                                                "compiler.lib.verify.Verify",
                                                "jdk.internal.value.ValueClass",
                                                "jdk.internal.vm.annotation.LooselyConsistentValue"),
                                         comp.getEscapedClassPathOfCompiledClasses(),
                                         List.of(template.asToken())
                                        );
    }

    static String generateTestMethodString() {
        return  """

                    @Test
                    public static Object #test(Object[] arr, int length) {
                        Class c = #klass.class;
                        return Arrays.copyOf(arr, length, c);
                    }
                """;
    }

    static List<ScopeToken> loop(int limit, IntFunction<ScopeToken> function) {
        return IntStream.range(0, limit)
                .mapToObj(function)
                .toList();
    }

    static <T> List<ScopeToken> loop(List<T> items, BiFunction<T, Integer, ScopeToken> function) {
        return IntStream.range(0, items.size())
                .mapToObj(i -> function.apply(items.get(i), i))
                .toList();
    }

    static <T> List<ScopeToken> concat(List<T> items, BiFunction<T, Integer, ScopeToken> function) {
        return IntStream.range(0, items.size())
                .boxed()
                .flatMap(i -> {
                    ScopeToken token = function.apply(items.get(i), i);
                    return i == items.size() - 1 ?
                            Stream.of(token) :
                            Stream.of(token, scope(", "));
                })
                .toList();
    }
}