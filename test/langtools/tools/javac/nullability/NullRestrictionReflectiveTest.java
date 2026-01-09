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
 * @summary Smoke test for null restriction parsing
 * @library /tools/javac/lib
 * @enablePreview
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main NullRestrictionReflectiveTest
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.ExecutionTask;
import combo.ComboTestHelper;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public class NullRestrictionReflectiveTest extends ComboInstance<NullRestrictionReflectiveTest> {

    enum TypeKind implements ComboParameter {
        NN_STRING("java.lang.String!"),
        NN_STRING_ARR("java.lang.String[]!"),
        NN_STRING_ARR_ARR("java.lang.String[][]!"),
        NN_LIST_STRING("java.util.List<java.lang.String>!"),
        NN_TVAR("X!"),
        NN_TVAR_ARR("X[]!"),
        NN_TVAR_ARR_ARR("X[][]!"),
        NN_LIST_TVAR("java.util.List<X>!");

        final String typeString;

        TypeKind(String typeString) {
            this.typeString = typeString;
        }

        @Override
        public String expand(String optParameter) {
            return typeString;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<NullRestrictionReflectiveTest>()
                .withDimension("TYPE", (x, tk) -> x.tk = tk, TypeKind.values())
                .run(NullRestrictionReflectiveTest::new);
    }

    TypeKind tk;

    static final String TEMPLATE = """
            import java.util.List;

            class Test<X> {

                static <Z> Z init() { return null; }

                #{TYPE} fld = init();

                #{TYPE} meth(#{TYPE} arg) {
                    return init();
                }
            }
            """;

    @Override
    public void doWork() throws IOException {
        String latestVersion = String.valueOf(SourceVersion.latestSupported().runtimeVersion().feature());
        newCompilationTask()
                .withOptions(List.of("--enable-preview", "--release", latestVersion))
                .withSourceFromTemplate(TEMPLATE)
                .execute(this::check);
    }

    void check(ExecutionTask executionTask) {
        try {
            Class<?> testClass = executionTask.load("Test");

            // check field type
            Field testField = testClass.getDeclaredField("fld");
            checkType(testField.getGenericType());

            // check method arguments and return type
            Method testMethod = testClass.getDeclaredMethod("meth", testField.getType());
            checkType(testMethod.getGenericReturnType());
            checkType(testMethod.getGenericParameterTypes()[0]);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    void checkType(Type type) {
        if (!type.getTypeName().equals(tk.typeString)) {
            fail("Type mismatch detected:\n" +
                    "\nFound type: " + type.getTypeName() +
                    "\nExpected error: " + tk.typeString);
        }
    }
}
