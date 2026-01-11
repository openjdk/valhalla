/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @summary Smoke test for array creation API
 * @library /tools/javac/lib
 * @enablePreview
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main ArrayCreationRuntimeTest
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.ExecutionTask;
import combo.ComboTestHelper;

import javax.lang.model.SourceVersion;
import java.util.List;

public class ArrayCreationRuntimeTest extends ComboInstance<ArrayCreationRuntimeTest> {

    enum ArrayInitKind implements ComboParameter {
        INIT_1_0("[] { }"),
        INIT_1_1("[] { \"a\" }"),
        INIT_1_2("[] { \"a\", \"b\" }"),
        INIT_2_0("[] { }"),
        INIT_2_0_0("[][] { { }, { } }"),
        INIT_2_0_1_0("[][] { { }, { \"a\" }, { } }"),
        INIT_2_1_2("[][] { { \"a\" }, { \"b\", \"c\" } }");

        final String initString;

        ArrayInitKind(String typeString) {
            this.initString = typeString;
        }

        @Override
        public String expand(String optParameter) {
            return initString;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<ArrayCreationRuntimeTest>()
                .withDimension("INIT", (x, tk) -> x.aik = tk, ArrayInitKind.values())
                .run(ArrayCreationRuntimeTest::new);
    }

    ArrayInitKind aik;

    static final String TEMPLATE = """
            import java.util.Arrays;

            public class Test {

                static {
                    test(new String!#{INIT}, new String#{INIT});
                }

                {
                    test(new String!#{INIT}, new String#{INIT});
                }

                static void m() {
                    test(new String!#{INIT}, new String#{INIT});
                }

                void g() {
                    test(new String!#{INIT}, new String#{INIT});
                }

                static void test(Object[] found, Object[] expected) {
                System.out.println(found);
                    if (!Arrays.deepToString(found).equals(Arrays.deepToString(expected))) {
                        throw new AssertionError("bad array comparison " +
                            "found: " + Arrays.deepToString(found) + " - expected: " + Arrays.deepToString(expected));
                    }
                }

                public static void main(String[] args) {
                    Test.m();
                    new Test().g();
                }
            }
            """;

    @Override
    public void doWork() {
        String latestVersion = String.valueOf(SourceVersion.latestSupported().runtimeVersion().feature());
        newCompilationTask()
                .withOptions(List.of("--enable-preview", "--release", latestVersion))
                .withSourceFromTemplate(TEMPLATE)
                .execute(this::check);
    }

    void check(ExecutionTask executionTask) {
        executionTask.withClass("Test").run();
    }
}
