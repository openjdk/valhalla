/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8338910
 * @summary [lw5] enhance the Signature attribute to represent nullability
 * @library /lib/combo /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.compiler/com.sun.tools.javac.code
 *     jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run junit NullabilitySignatureAttrTests
 */

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;

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
import com.sun.tools.classfile.ImplicitCreation_attribute;
import com.sun.tools.classfile.NullRestricted_attribute;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Signature_attribute;

import org.junit.jupiter.api.Test;
import tools.javac.combo.CompilationTestCase;

import toolbox.*;
import toolbox.Task.*;

public class NullabilitySignatureAttrTests extends CompilationTestCase {
    ToolBox tb = new ToolBox();

    private static String[] PREVIEW_OPTIONS = {"--enable-preview", "-source", Integer.toString(Runtime.version().feature())};

    public NullabilitySignatureAttrTests() {
        setDefaultFilename("Nullable.java");
        setCompileOptions(PREVIEW_OPTIONS);
    }

    record SignatureData(String source, String expectedSignature) {}

    final List<SignatureData> signatureDataList = List.of(
            new SignatureData(
                    """
                    class Test {
                        Test! t;
                    }
                    """,
                    "LTest!;"
            ),
            new SignatureData(
                    """
                    class Test {
                        Test? t;
                    }
                    """,
                    "LTest?;"
            ),
            new SignatureData(
                    """
                    import java.util.*;
                    class Test {
                        List<Test!> t;
                    }
                    """,
                    "Ljava/util/List<LTest!;>;"
            ),
            new SignatureData(
                    """
                    import java.util.*;
                    class Test {
                        List<Test?> t;
                    }
                    """,
                    "Ljava/util/List<LTest?;>;"
            ),
            new SignatureData(
                    """
                    import java.util.*;
                    class Test {
                        List!<Test!> t;
                    }
                    """,
                    "Ljava/util/List!<LTest!;>;"
            ),
            new SignatureData(
                    """
                    import java.util.*;
                    class Test {
                        List?<Test?> t;
                    }
                    """,
                    "Ljava/util/List?<LTest?;>;"
            ),
            new SignatureData(
                    """
                    class Test<T> {
                        T! t;
                    }
                    """,
                    "TT!;"
            ),
            new SignatureData(
                    """
                    class Test<T> {
                        T? t;
                    }
                    """,
                    "TT?;"
            )
    );

    /*@Test
    void testCheckFieldSignature() throws Exception {
        for (SignatureData sd : signatureDataList) {
            File dir = assertOK(true, sd.source);
            for (final File fileEntry : dir.listFiles()) {
                ClassFile classFile = ClassFile.read(fileEntry);
                Field field = classFile.fields[0];
                Signature_attribute sa = (Signature_attribute)field.attributes.get("Signature");
                System.err.println(sa.getSignature(classFile.constant_pool).toString());
                Assert.check(sa.getSignature(classFile.constant_pool).toString().equals(sd.expectedSignature));
            }
        }
    }*/

    record SepCompilationData(String clientSrc, String serverSrc, List<String> sourceExpectedWarnings, List<String> sepCompExpectedWarnings) {}
    final List<SepCompilationData> sepCompilationDataList = List.of(
            new SepCompilationData(
                    """
                    class Client {
                        static Integer! a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static Integer? b;
                    }
                    """,
                    List.of("Client.java:2:31: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning"),
                    List.of("Client.java:2:31: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning")
            ),
            new SepCompilationData(
                    """
                    class Client {
                        static Integer! a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static Integer b;
                    }
                    """,
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData(
                    """
                    class Client {
                        static Integer! a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static Integer! b = 4;
                    }
                    """,
                    List.of("- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData(
                    """
                    import java.util.*;
                    class Client {
                        static List!<String> a = Server.b;
                    }
                    """,
                    """
                    import java.util.*;
                    class Server {
                        public static List!<String> b = new ArrayList<>();
                    }
                    """,
                    List.of("- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData(
                    """
                    import java.util.*;
                    class Client {
                        static List!<String> a = Server.b;
                    }
                    """,
                    """
                    import java.util.*;
                    class Server {
                        public static List?<String> b = new ArrayList<>();
                    }
                    """,
                    List.of("Client.java:3:36: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning"),
                    List.of("Client.java:3:36: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning")
            ),
            new SepCompilationData(
                    """
                    import java.util.*;
                    class Client {
                        static List!<String> a = Server.b;
                    }
                    """,
                    """
                    import java.util.*;
                    class Server {
                        public static List<String> b = new ArrayList<>();
                    }
                    """,
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData(
                    """
                    class Client {
                        static Server!.Inner! a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        static class Inner {}
                        public static Server?.Inner? b = new Server.Inner();
                    }
                    """,
                    List.of("Client.java:2:37: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning"),
                    List.of("Client.java:2:37: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning")
            )
    );

    @Test
    void testCheckSeparateComp() throws Exception {
        Path base = Paths.get(".");
        Path src = base.resolve("src");
        tb.createDirectories(src);
        Path out = base.resolve("out");
        tb.createDirectories(out);
        int testNo = 0;
        for (SepCompilationData scd : sepCompilationDataList) {
            System.err.println("executing test at index " + testNo++);
            tb.writeJavaFiles(src, scd.serverSrc, scd.clientSrc);
            List<String> log = new JavacTask(tb)
                    .outdir(out)
                    .options("--enable-preview", "-source", Integer.toString(Runtime.version().feature()),
                            "-Xlint:null", "-XDrawDiagnostics")
                    .files(tb.findJavaFiles(src))
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);
            if (!scd.sourceExpectedWarnings.equals(log))
                throw new Exception("expected output not found: " + log);
            /* now lets remove serverSrc's source and compile client's source using the class file version of serverSrc
             */
            tb.deleteFiles(src.resolve("Server.java"));
            log = new JavacTask(tb)
                    .outdir(out)
                    .options("--enable-preview", "-source", Integer.toString(Runtime.version().feature()),
                            "-Xlint:null", "-XDrawDiagnostics", "-cp", "out")
                    .files(tb.findJavaFiles(src))
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);
            if (!scd.sepCompExpectedWarnings.equals(log))
                throw new Exception("expected output not found: " + log);

            // let's remove all the files in preparation for the next test
            tb.deleteFiles(src.resolve("Client.java"));
            tb.deleteFiles(out.resolve("Client.class"));
            tb.deleteFiles(out.resolve("Server.class"));
        }
    }
}
