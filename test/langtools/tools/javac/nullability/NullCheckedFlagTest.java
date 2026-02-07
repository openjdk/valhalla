/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8338766
 * @summary Make null-restricted types a preview feature with an access flag
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main ${test.main.class}
 */

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.javac.util.Assert;

import toolbox.TestRunner;
import toolbox.ToolBox;

public class NullCheckedFlagTest extends TestRunner {
    ToolBox tb = new ToolBox();

    public NullCheckedFlagTest() {
        super(System.err);
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws Exception {
        return tb.findJavaFiles(paths);
    }

    public static void main(String... args) throws Exception {
        new NullCheckedFlagTest().runTests();
    }

    @Test
    public void testLoadableDescField(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                value class V {
                }
                class Test {
                    V! v1;
                    void m(V! v) {
                        v1 = v;
                    }
                }
                """);
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new toolbox.JavacTask(tb)
                .options("--enable-preview", "-source", Integer.toString(Runtime.version().feature()))
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
        Path classFilePath = classes.resolve("Test.class");
        ClassModel clazz = ClassFile.of().parse(classFilePath);
        Assert.check(clazz.minorVersion() == ClassFile.PREVIEW_MINOR_VERSION);
        FieldModel v1 = clazz.fields().getFirst();
        Assert.check(v1.fieldName().equalsString("v1"));
        Assert.check(v1.flags().has(AccessFlag.NULL_CHECKED));
    }
}
