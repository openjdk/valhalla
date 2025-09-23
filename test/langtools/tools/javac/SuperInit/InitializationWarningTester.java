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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.PrintWriter;
import java.io.IOException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.file.PathFileObject;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;

public class InitializationWarningTester {
    Context context;
    Options options;
    MyJavaCompiler javaCompiler;
    JavacFileManager javacFileManager;
    PrintWriter errOut;
    DiagnosticListener<JavaFileObject> diagnosticListener;

    public static void main(String... args) throws Throwable {
        String testSrc = System.getProperty("test.src");
        Path baseDir = Paths.get(testSrc);
        InitializationWarningTester tester = new InitializationWarningTester();
        Assert.check(args.length > 0, "no args, ending");
        tester.test(baseDir, args[0]);
    }

    java.util.List<String> compilationOutput = new ArrayList<>();

    public InitializationWarningTester() {
        context = new Context();
        diagnosticListener = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> message) {
                if (!ignoreDiagnostics &&
                    (message.getCode().contains(warningKey) ||
                     message.getCode().contains(errorKey1) ||
                     message.getCode().contains(errorKey2))) {
                    if ((message.getCode().contains(errorKey1) ||
                            message.getCode().contains(errorKey2)) &&
                            !errorExpected) {
                        throw new AssertionError("error key not expected " + message);
                    }
                    JCDiagnostic diagnostic = (JCDiagnostic) message;
                    String msgData = ((PathFileObject)diagnostic.getDiagnosticSource().getFile()).getShortName() +
                            ":" + diagnostic.getLineNumber() + ":" + diagnostic.getColumnNumber() + ": " + diagnostic.getCode() + ": " +
                            diagnostic.getArgs()[0];
                    compilationOutput.add(msgData);
                }
            }
        };
        context.put(DiagnosticListener.class, diagnosticListener);
        JavacFileManager.preRegister(context);
        MyAttr.preRegister(context, this);
        options = Options.instance(context);
        options.put("--enable-preview", "--enable-preview");
        options.put("--source", Integer.toString(Runtime.version().feature()));
        options.put("-Xlint:initialization", "-Xlint:initialization");
        javaCompiler = new MyJavaCompiler(context);
        javacFileManager = new JavacFileManager(context, false, Charset.defaultCharset());
    }

    static final String errorKey1 = "compiler.err.cant.ref.before.ctor.called";
    static final String errorKey2 = "compiler.err.cant.assign.initialized.before.ctor.called";
    static final String warningKey = "compiler.warn.would.not.be.allowed.in.prologue";

    void test(Path baseDir, String className) throws Throwable {
        DirectoryStream<Path> paths = null;
        try {
            paths = Files.newDirectoryStream(baseDir,
                    p -> (!Files.isDirectory(p) &&
                            (p.endsWith(className + ".java") ||
                            p.endsWith(className + ".out"))
                    )
            );
        } catch (IOException e) {
            throw new AssertionError("Error accessing directory: " + e.getMessage());
        }

        Path javaFile = null;
        Path goldenFile = null;
        for (Path p: paths) {
            if (p.toString().endsWith("java")) {
                javaFile = p;
            } else if (p.toString().endsWith("out")) {
                goldenFile = p;
            }
        }
        // compile
        javaCompiler.compile(com.sun.tools.javac.util.List.of(javacFileManager.getJavaFileObject(javaFile)));
        if (goldenFile != null) {
            java.util.List<String> goldenFileContent = Files.readAllLines(goldenFile);
            goldenFileContent = goldenFileContent.stream()
                    .filter(s -> s.contains(errorKey1) || s.contains(errorKey2))
                    .collect(Collectors.toList());
            Assert.check(goldenFileContent.size() == compilationOutput.size(), "compilation output length mismatch");
            for (int i = 0; i < goldenFileContent.size(); i++) {
                String goldenLine = goldenFileContent.get(i);
                String warningLine = compilationOutput.get(i);
                if (warningLine.contains(warningKey)) {
                    goldenLine = goldenLine.replace(errorKey1, warningKey);
                    goldenLine = goldenLine.replace(errorKey2, warningKey);
                }
                Assert.check(warningLine.equals(goldenLine), "error for line " + warningLine);
            }
        } else {
            Assert.check(compilationOutput.size() == 0);
        }
    }

    static class MyJavaCompiler extends JavaCompiler {
        MyJavaCompiler(Context context) {
            super(context);
            // do not generate code
            this.shouldStopPolicyIfNoError = CompileStates.CompileState.LOWER;
        }
    }

    // ignore diagnostics
    boolean ignoreDiagnostics = false;
    // even when compiling with warnings on, an error will be produced
    boolean errorExpected = false;

    static class MyAttr extends Attr {
        InitializationWarningTester tester;
        static void preRegister(Context context, InitializationWarningTester tester) {
            context.put(attrKey, (com.sun.tools.javac.util.Context.Factory<Attr>) c -> new MyAttr(c, tester));
        }

        MyAttr(Context context, InitializationWarningTester tester) {
            super(context);
            this.tester = tester;
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            boolean previousIgnoreDiags = tester.ignoreDiagnostics;
            boolean previousErrExpected = tester.errorExpected;
            try {
                if (TreeInfo.isConstructor(tree)) {
                    // remove the super constructor call if it is a no arguments invocation
                    List<Attribute.Compound> attributes = tree.sym.getDeclarationAttributes();
                    for (Attribute.Compound attribute : attributes) {
                        if (attribute.toString().equals("@IgnoreMethod")) {
                            tester.ignoreDiagnostics = true;
                        } else if (attribute.toString().equals("@ErrorExpected")) {
                            tester.errorExpected = true;
                        }
                    }
                    if (TreeInfo.hasAnyConstructorCall(tree) && !tester.errorExpected) {
                        ListBuffer<JCStatement> newStats = new ListBuffer<>();
                        for (JCStatement statement : tree.body.stats) {
                            if (statement instanceof JCExpressionStatement expressionStatement &&
                                    expressionStatement.expr instanceof JCMethodInvocation methodInvocation) {
                                if (TreeInfo.isConstructorCall(methodInvocation) && methodInvocation.args.isEmpty()) {
                                    continue;
                                }
                            }
                            newStats.add(statement);
                        }
                        tree.body.stats = newStats.toList();
                    }
                }
                super.visitMethodDef(tree);
            } finally {
                tester.ignoreDiagnostics = previousIgnoreDiags;
                tester.errorExpected = previousErrExpected;
            }
        }
    }
}
