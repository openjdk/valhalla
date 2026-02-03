/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8338910 8347754
 * @summary [lw5] enhance the Signature attribute to represent nullability
 * @enablePreview
 * @library /lib/combo /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.util
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.compiler/com.sun.tools.javac.code
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run junit NullabilitySignatureAttrTests
 */

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Set;

import com.sun.tools.javac.util.Assert;

import java.lang.classfile.Attribute;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;

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
            new SignatureData( // case 0
                    """
                    class Test {
                        String! t;
                        Test() {
                            t = "";
                            super();
                        }
                    }
                    """,
                    "!Ljava/lang/String;"
            ),
            new SignatureData( // case 1
                    """
                    import java.util.*;
                    class Test {
                        List<Test>! t;
                        Test() {
                            t = new ArrayList<>();
                            super();
                        }
                    }
                    """,
                    "!Ljava/util/List<LTest;>;"
            ),
            new SignatureData( // case 2
                    """
                    class Test<T> {
                        T! t;
                        Test() {
                            t = (T)new Object();
                            super();
                        }
                    }
                    """,
                    "!TT;"
            ),
            new SignatureData( // case 3
                    """
                    class Test {
                        String[]! t;
                        Test() {
                            t = new String[]{""};
                            super();
                        }
                    }
                    """,
                    "![Ljava/lang/String;"
            ),
            new SignatureData( // case 4
                    """
                    class Test {
                        String[][]! t;
                        Test() {
                            t = new String[][]{{""}};
                            super();
                        }
                    }
                    """,
                    "![[Ljava/lang/String;"
            )
    );

    @Test
    void testCheckFieldSignature() throws Exception {
        int testNo = 0;
        for (SignatureData sd : signatureDataList) {
            System.err.println("executing test at index " + testNo++);
            File dir = assertOK(true, sd.source);
            for (final File fileEntry : dir.listFiles()) {
                var classFile = ClassFile.of().parse(fileEntry.toPath());
                var field = classFile.fields().get(0);
                var sa = field.findAttribute(Attributes.signature()).orElseThrow();
                Assert.check(sa.signature().toString().equals(sd.expectedSignature));
            }
        }
    }

    record SepCompilationData(String clientSrc, String serverSrc, List<String> sourceExpectedWarnings, List<String> sepCompExpectedWarnings) {}
    final List<SepCompilationData> sepCompilationDataList = List.of(
            new SepCompilationData(  // case 0
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
            new SepCompilationData( // case 1
                    """
                    import java.util.*;
                    class Client {
                        static List<String>! a = Server.b;
                    }
                    """,
                    """
                    import java.util.*;
                    class Server {
                        public static List<String>! b = new ArrayList<>();
                    }
                    """,
                    List.of("- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData( // case 2
                    """
                    import java.util.*;
                    class Client {
                        static List<String>! a = Server.b;
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
            new SepCompilationData( // case 3
                    """
                    class Client {
                        static Server.Inner! a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        static class Inner {}
                        public static Server.Inner b = new Server.Inner();
                    }
                    """,
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData( // case 4
                    """
                    class Client {
                        static String[][]! a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static String[][] b;
                    }
                    """,
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData( // case 5
                    """
                    import java.util.List;
                    class Client {
                        static List<? extends String>! a = Server.b;
                    }
                    """,
                    """
                    import java.util.List;
                    class Server {
                        public static List<? extends String> b;
                    }
                    """,
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
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

            // now lets remove serverSrc's source and compile client's source using the class file version of serverSrc
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
