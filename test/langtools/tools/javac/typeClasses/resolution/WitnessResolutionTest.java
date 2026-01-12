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

import toolbox.JavaTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.Task.TaskError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import toolbox.JavacTask;
import toolbox.Task.OutputKind;
import toolbox.ToolBox;
import toolbox.ToolBox.JavaSource;

public abstract class WitnessResolutionTest {

    final ToolBox tb = new ToolBox();

    static final StackWalker STACK_WALKER = StackWalker.getInstance();

    private String testCaseId(StackWalker.StackFrame frame) {
        return frame.getClassName() + "_" + frame.getMethodName() + "_" + frame.getLineNumber();
    }

    public Result findWitness(String witnessString, Set<String> witnessDeclTypes) {
        String testCaseId = STACK_WALKER.walk(frames -> testCaseId(frames.skip(1).findFirst().get()));
        Result result = Result.dummy();
        for (DeclarationKind dkind : DeclarationKind.values()) {
            for (LookupMode lmode : LookupMode.values()) {
                TestCase testCase = new TestCase(testCaseId, witnessString, witnessDeclTypes, dkind, lmode);
                result = Result.combine(result, testCase.result());
            }
        }
        return result;
    }

    public interface Result {
        void success(String string);
        void ambiguous();
        void notFound();

        static Result combine(Result first, Result second) {
            return new Result() {
                @Override
                public void success(String string) {
                    first.success(string);
                    second.success(string);
                }

                @Override
                public void ambiguous() {
                    first.ambiguous();
                    second.ambiguous();
                }

                @Override
                public void notFound() {
                    first.notFound();
                    second.notFound();
                }
            };
        }

        static Result dummy() {
            return new Result() {
                @Override
                public void success(String string) { }
                @Override
                public void ambiguous() { }
                @Override
                public void notFound() { }
            };
        }
    }

    enum LookupMode {
        STATIC,
        DYNAMIC;
    }

    enum DeclarationKind {
        FIELD,
        METHOD;
    }

    class TestCase {
        final TypeNode witnessType;
        final Set<String> witnessDecls;
        final List<JavaSource> auxClasses = new ArrayList<>();
        final JavaSource mainClass;
        final Path base;
        final LookupMode lmode;
        final DeclarationKind dkind;

        static class TemplatedTestClass {
            final String template;

            TemplatedTestClass(String template) {
                this.template = template;
            }

            JavaSource toSource(String... args) {
                String source = template;
                for (int i = 0; i < args.length; i++) {
                    source = source.replaceAll("#\\{" + i + "\\}", args[i]);
                }
                return new JavaSource(source);
            }

            static final TemplatedTestClass STATIC_TEST_CLASS = new TemplatedTestClass("""
                    class TestStaticLookup {
                        public static void main(String[] args) {
                            System.out.println(#{0}.__witness.m());
                        }
                    }
                    """);

            static final TemplatedTestClass DYNAMIC_TEST_CLASS = new TemplatedTestClass("""
                    import java.lang.reflect.Type;
                    import java.lang.invoke.MethodHandles;
                    import java.lang.runtime.WitnessSupport;

                    class TestDynamicLookup {
                        public static void main(String[] args) throws Throwable {
                            Type type = WitnessSupport.type(MethodHandles.lookup(), "#{1}");
                            #{0} witness = (#{0})WitnessSupport.lookupWitness(MethodHandles.lookup(), type);
                            System.out.println(witness.m());
                        }
                    }
                    """);

            static final TemplatedTestClass AUX_TEST_CLASS = new TemplatedTestClass("""
                    interface #{0}#{1} {
                        #{2}
                        #{3}
                    }
                    """);
        }

        TestCase(String id, String witnessTypeString, Set<String> witnessDecls, DeclarationKind dkind, LookupMode lmode) {
            this.base = Path.of(id);
            this.witnessDecls = witnessDecls;
            this.lmode = lmode;
            this.dkind = dkind;
            GenericTypeParser genericTypeParser = new GenericTypeParser(witnessTypeString);
            witnessType = genericTypeParser.parseType();
            for (Map.Entry<String, List<TypeNode>> typeReferences : genericTypeParser.typeDictionary.entrySet()) {
                int nParameters = typeReferences.getValue().stream().mapToInt(tn -> tn.typeArguments.size()).max().getAsInt();
                String typeParameters = nParameters == 0 ? "" :
                        IntStream.range(0, nParameters).mapToObj(i -> "X_" + i).collect(Collectors.joining(", ", "<", ">"));
                String className = typeReferences.getKey();
                String witnessDecl = "";
                if (witnessDecls.contains(className)) {
                    witnessDecl = dkind == DeclarationKind.METHOD ?
                            "__witness " + witnessType + " W_" + className + "() { return () -> \"" + className + "\"; }" :
                            "__witness " + witnessType + " W_" + className + " = () -> \"" + className + "\";";
                }
                String witnessMethod = witnessType.name.equals(className) ?
                        "String m();" : "";
                auxClasses.add(TemplatedTestClass.AUX_TEST_CLASS.toSource(className, typeParameters, witnessDecl, witnessMethod));
            }
            if (lmode == LookupMode.DYNAMIC) {
                mainClass = TemplatedTestClass.DYNAMIC_TEST_CLASS.toSource(witnessType.toString(), witnessType.toSignatureString());
            } else {
                mainClass = TemplatedTestClass.STATIC_TEST_CLASS.toSource(witnessType.toString());
            }
        }

        Result result() {
            return new Result() {
                public void success(String string) {
                    compileSuccess();
                    runSuccess(string);
                }

                public void ambiguous() {
                    if (lmode == LookupMode.STATIC) {
                        // static lookup
                        compileFail("ambiguous");
                    } else {
                        // dynamic lookup
                        compileSuccess();
                        runFail("Ambiguous");
                    }
                }

                public void notFound() {
                    if (lmode == LookupMode.STATIC) {
                        // static lookup
                        compileFail("cannot find");
                    } else {
                        // dynamic lookup
                        compileSuccess();
                        runFail("not found");
                    }
                }
            };
        }

        private void compileSuccess() {
            compile(Expect.SUCCESS, null, null);
        }

        private void compileFail(String message) {
            compile(Expect.FAIL, OutputKind.DIRECT, message);
        }

        private void runSuccess() {
            run(Expect.SUCCESS, null, null);
        }

        private void runSuccess(String out) {
            run(Expect.SUCCESS, OutputKind.STDOUT, out);
        }

        private void runFail(String message) {
            run(Expect.FAIL, OutputKind.STDERR, message);
        }

        private void compile(Expect expect, OutputKind outputKind, String expected) {
            try {
                Path src = base.resolve("src_" + lmode + "_" + dkind);
                Path out = base.resolve("out_" + lmode + "_" + dkind);
                for (JavaSource auxClass : auxClasses) {
                    auxClass.write(src);
                }
                mainClass.write(src);

                JavacTask task = new JavacTask(tb)
                        .outdir(Files.createDirectories(out))
                        .files(tb.findJavaFiles(src));
                taskHelper(task, expect, JavacTask::run, outputKind, expected);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private void run(Expect expect, OutputKind outputKind, String expected) {
            String main = mainClass.getName().replace(".java", "");
            JavaTask task = new JavaTask(tb)
                    .classpath(base.resolve("out_" + lmode + "_" + dkind).toString())
                    .className(main);
            taskHelper(task, expect, JavaTask::run, outputKind, expected);
        }

        AssertionError fail(String msg) {
            return new AssertionError(msg);
        }

        private <T extends Task> void taskHelper(T task, Expect expect, BiFunction<T, Expect, Task.Result> runner, OutputKind outputKind, String expected) {
            printTask(task, expect, expected);
            Task.Result result = null;
            try {
                result = runner.apply(task, expect);
            } catch (TaskError _) {
                String badOutcome = expect == Expect.SUCCESS ? "failed" : "succeeded";
                throw fail("task " + badOutcome + " unexpectedly");
            }
            if (expected != null) {
                String out = result.getOutput(outputKind);
                if (!out.contains(expected)) {
                    System.err.println(out);
                    throw fail("unexpected output");
                }
            }
        }

        void printTask(Task task, Expect expect, String expected) {
            System.err.println(task.name() + " task; witness = " + witnessType +
                    " ; declarations = " + witnessDecls + " ; expected outcome = " + expect +
                    " ; expected output = " + expected + " ; declaration kind = " + dkind +
                    " ; lookup mode = " + lmode);
        }
    }

    public record TypeNode(String name, List<TypeNode> typeArguments) {
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(name);
            if (!typeArguments.isEmpty()) {
                String typeArgString = typeArguments.stream()
                        .map(TypeNode::toString)
                        .collect(Collectors.joining(", ", "<", ">"));
                buf.append(typeArgString);
            }
            return buf.toString();
        }

        public String toSignatureString() {
            StringBuilder buf = new StringBuilder();
            buf.append("L" + name);
            if (!typeArguments.isEmpty()) {
                String typeArgString = typeArguments.stream()
                        .map(TypeNode::toSignatureString)
                        .collect(Collectors.joining("", "<", ">"));
                buf.append(typeArgString);
            }
            buf.append(";");
            return buf.toString();
        }
    }

    static class GenericTypeParser {
        private final Map<String, List<TypeNode>> typeDictionary = new LinkedHashMap<>();
        private final String input;
        private int pos;

        public GenericTypeParser(String input) {
            this.input = input.replaceAll("\\s+", "");
            this.pos = 0;
        }

        public TypeNode parse() {
            return parseType();
        }

        private TypeNode parseType() {
            // Parse class name
            String name = parseIdentifier();

            // Parse generic params, if any
            List<TypeNode> typeArguments = new ArrayList<>();
            if (peek() == '<') {
                next(); // Skip '<'
                typeArguments.add(parseType());
                while (peek() == ',') {
                    next(); // Skip ','
                    typeArguments.add(parseType());
                }
                expect('>'); // Expect '>'
            }

            TypeNode res = new TypeNode(name, typeArguments);
            List<TypeNode> types = typeDictionary.computeIfAbsent(name, k -> new ArrayList<>());
            types.add(res);
            return res;
        }

        private String parseIdentifier() {
            int start = pos;
            while (pos < input.length() && Character.isJavaIdentifierPart(input.charAt(pos))) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalStateException("Expected identifier at position " + pos);
            }
            return input.substring(start, pos);
        }

        private char peek() {
            if (pos < input.length())
                return input.charAt(pos);
            return (char) -1;
        }

        private void next() {
            input.charAt(pos++);
        }

        private void expect(char c) {
            if (peek() != c)
                throw new RuntimeException("Expected '" + c + "' at position " + pos);
            pos++;
        }
    }
}
