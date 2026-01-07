/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Smoke test for null restriction parsing
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main NullRestrictionParserTest
 */

import java.io.IOException;
import java.util.List;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

import javax.tools.Diagnostic.Kind;

public class NullRestrictionParserTest extends ComboInstance<NullRestrictionParserTest> {

    enum AnnoKind implements ComboParameter {
        ANNO("@A "),
        NO_ANNO("");

        final String annoTemplate;

        AnnoKind(String annoTemplate) {
            this.annoTemplate = annoTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return annoTemplate;
        }
    }

    enum TypeKind implements ComboParameter {
        STRING("#{ANNO}String", 0),
        NN_STRING("#{ANNO}String!", 0),
        STRING_ARR("#{ANNO}String[]", 0),
        STRING_NN_ARR("#{ANNO}String[]!", 0),
        NN_STRING_ARR("#{ANNO}String![]", 1),
        NN_STRING_NN_ARR("#{ANNO}String![]!", 1),
        STRING_ARR_ARR("#{ANNO}String[][]", 0),
        STRING_NN_ARR_ARR("#{ANNO}String[]![]", 1),
        STRING_NN_ARR_NN_ARR("#{ANNO}String[]![]!", 1),
        NN_STRING_ARR_ARR("#{ANNO}String![]!", 1),
        NN_STRING_NN_ARR_ARR("#{ANNO}String![]![]", 2),
        NN_STRING_NN_ARR_NN_ARR("#{ANNO}String![]![]!", 2),
        NN_LIST_STRING("#{ANNO}List<String>!", 0);
        //NN_LIST_STRING_ARR("#{ANNO}List<String>[]!", 0); // this doesn't work because of an issue (see JDK-8374629)


        final String typeTemplate;
        final int errors;

        TypeKind(String typeTemplate, int errors) {
            this.typeTemplate = typeTemplate;
            this.errors = errors;
        }

        @Override
        public String expand(String optParameter) {
            return typeTemplate;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<NullRestrictionParserTest>()
                .withDimension("ANNO", (x, ak) -> x.ak = ak, AnnoKind.values())
                .withDimension("TYPE", (x, tk) -> x.tk = tk, TypeKind.values())
                .run(NullRestrictionParserTest::new);
    }

    AnnoKind ak;
    TypeKind tk;

    static final String TEMPLATE = """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.List;

            class Test {

                @Target(ElementType.TYPE_USE)
                @interface A {}

                #{TYPE} restype() { throw new AssertionError(); }
                void argtype(#{TYPE} arg) { throw new AssertionError(); }
                void final_argtype(final #{TYPE} arg) { throw new AssertionError(); }
                void varargtype(#{TYPE}... args) { throw new AssertionError(); }

                #{TYPE} field;

                void testLocal() {
                    #{TYPE} local;
                    final #{TYPE} local;
                }

                void testCast(Object o) {
                    var x = (#{TYPE})o;
                }

                void testIntersectionCast(Object o) {
                    var x = (#{TYPE} & #{TYPE})o;
                }

                void testInstanceof(Object o) {
                    var x = o instanceof #{TYPE};
                    var x = o instanceof #{TYPE} p;
                }

                void testSwitch(Object o) {
                    switch (o) {
                        case #{TYPE} p -> { }
                        default -> { }
                    };
                }

                void testCatch() {
                    try {
                        foo();
                    } catch (#{TYPE} t) { }
                }

                void testMultiCatch() {
                    try {
                        foo();
                    } catch (#{TYPE} | #{TYPE} t) { }
                }
            }
            """;

    static final int num_types;

    static {
        int occurrences = 0;
        int start = -1;
        while (start + 1 < TEMPLATE.length() && (start = TEMPLATE.indexOf("#{TYPE}", start + 1)) != -1) {
            occurrences++;
        }
        num_types = occurrences;
    }

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOptions(List.of("--enable-preview", "--release", System.getProperty("java.specification.version")))
                .withSourceFromTemplate(TEMPLATE)
                .parse(this::check);
    }

    void check(Result<?> res) {
        int expectedErrors = tk.errors * num_types;

        var errors = res.diagnosticsForKey("compiler.err.unsupported.null.restriction");

        var otherErrors = res.diagnosticsForKind(Kind.ERROR).size() - errors.length();

        if (expectedErrors != errors.length() || otherErrors > 0) {
            fail("invalid diagnostics for source:\n" +
                    res.compilationInfo() +
                    "\nFound error: " + errors +
                    "\nExpected error: " + expectedErrors);
        }
    }
}
