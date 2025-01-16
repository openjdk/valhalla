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
            new SignatureData( // case 0
                    """
                    class Test {
                        Test! t = new Test();
                    }
                    """,
                    "LTest!;"
            ),
            new SignatureData( // case 1
                    """
                    class Test {
                        Test? t;
                    }
                    """,
                    "LTest?;"
            ),
            new SignatureData( // case 2
                    """
                    import java.util.*;
                    class Test {
                        List<Test!> t;
                    }
                    """,
                    "Ljava/util/List<LTest!;>;"
            ),
            new SignatureData( // case 3
                    """
                    import java.util.*;
                    class Test {
                        List<Test?> t;
                    }
                    """,
                    "Ljava/util/List<LTest?;>;"
            ),
            new SignatureData( // case 4
                    """
                    import java.util.*;
                    class Test {
                        List!<Test!> t = new ArrayList<>();
                    }
                    """,
                    "Ljava/util/List<LTest!;>!;"
            ),
            new SignatureData( // case 5
                    """
                    import java.util.*;
                    class Test {
                        List?<Test?> t;
                    }
                    """,
                    "Ljava/util/List<LTest?;>?;"
            ),
            new SignatureData( // case 6
                    """
                    class Test<T> {
                        T! t = (T)new Object();
                    }
                    """,
                    "TT!;"
            ),
            new SignatureData( // case 7
                    """
                    class Test<T> {
                        T? t;
                    }
                    """,
                    "TT?;"
            ),
            new SignatureData( // case 8
                    """
                    class Test {
                        String[]? t;
                    }
                    """,
                    "[Ljava/lang/String;"
            ),
            new SignatureData( // case 9
                    """
                    class Test {
                        String[]! t = {""};
                    }
                    """,
                    "[Ljava/lang/String;"
            ),
            new SignatureData( // case 10
                    """
                    class Test {
                        String![]? t;
                    }
                    """,
                    "[Ljava/lang/String!;"
            ),
            new SignatureData( // case 11
                    """
                    class Test {
                        String?[]![]? t = {{""}};
                    }
                    """,
                    "[[Ljava/lang/String?;"
            )
    );

    @Test
    void testCheckFieldSignature() throws Exception {
        int testNo = 0;
        for (SignatureData sd : signatureDataList) {
            System.err.println("executing test at index " + testNo++);
            File dir = assertOK(true, sd.source);
            for (final File fileEntry : dir.listFiles()) {
                ClassFile classFile = ClassFile.read(fileEntry);
                Field field = classFile.fields[0];
                Signature_attribute sa = (Signature_attribute)field.attributes.get("Signature");
                System.err.println(sa.getSignature(classFile.constant_pool).toString());
                Assert.check(sa.getSignature(classFile.constant_pool).toString().equals(sd.expectedSignature));
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
            new SepCompilationData(  // case 1
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
            new SepCompilationData(  // case 2
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
            new SepCompilationData( // case 3
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
            new SepCompilationData( // case 4
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
            new SepCompilationData( // case 5
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
            new SepCompilationData( // case 6
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
            ),
            new SepCompilationData( // case 7
                    """
                    class Client {
                        static String?[]![]? a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static String?[]?[]? b;
                    }
                    """,
                    List.of("Client.java:2:36: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",  // some information is lost
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData( // case 8
                    """
                    class Client {
                        static String![]?[]? a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static String?[]?[]? b;
                    }
                    """,
                    List.of("Client.java:2:36: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning"),
                    List.of("Client.java:2:36: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning")
            ),
            new SepCompilationData( // case 9
                    """
                    class Client {
                        static String?[]?[]! a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static String?[]?[]? b;
                    }
                    """,
                    List.of("Client.java:2:36: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData( // case 10
                    """
                    class Client {
                        static String?[]?[]? a = Server.b;
                    }
                    """,
                    """
                    class Server {
                        public static String?[]?[]? b;
                    }
                    """,
                    List.of("- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile"),
                    List.of("- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile")
            ),
            new SepCompilationData( // case 11
                    """
                    import java.util.List;
                    class Client {
                        static List<? extends String!> a = Server.b;
                    }
                    """,
                    """
                    import java.util.List;
                    class Server {
                        public static List<? extends String?> b;
                    }
                    """,
                    List.of("Client.java:3:46: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.plural: DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning"),
                    List.of("Client.java:3:46: compiler.warn.unchecked.nullness.conversion",
                            "- compiler.note.preview.filename: Client.java, DEFAULT",
                            "- compiler.note.preview.recompile",
                            "1 warning")
            ),
            new SepCompilationData( // case 12
                    """
                    import java.util.function.*;
                    class Client extends Vector<Byte>{
                        void foo(Server s, Unary op) {
                            int opc = 1;
                            s.unaryOp(getClass(), null, byte.class, this, null, UN_IMPL.find(op, opc, Client::unaryOperations));
                        }
                        interface Operator {}
                        interface Unary extends Operator {}
                        static class ImplCache<OP extends Operator,T> {
                            public ImplCache(Class<OP> whatKind, Class<? extends Vector<?>> whatVec) {}
                            public T find(OP op, int opc, IntFunction<T> supplier) {
                                return null;
                            }
                        }
                        static ImplCache<Unary, Server.UnaryOperation<Client, VectorMask<Byte>>>
                            UN_IMPL = new ImplCache<>(Unary.class, Client.class);
                        static Server.UnaryOperation<Client, VectorMask<Byte>> unaryOperations(int opc_) { return null; }
                    }
                    """,
                    """
                    class Server {
                        <V extends Vector<E>,
                         M extends VectorMask<E>,
                         E>
                        V unaryOp(Class<? extends V> vClass, Class<? extends M> mClass, Class<E> eClass,
                                  V v, M m,
                                  UnaryOperation<V, M> defaultImpl) {
                            return null;
                        }
                        public interface UnaryOperation<V extends Vector<?>,
                                                        M extends VectorMask<?>> {
                            V apply(V v, M m);
                        }
                    }
                    class Vector<V> {}
                    class VectorMask<VM> {}
                    """,
                    List.of(""),
                    List.of("")
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
