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

package build.tools.valhalla.valuetypes;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.groupingBy;

/**
 * Annotation processor for generating preview sources of classes annotated as
 * value classes for preview mode.
 *
 * <p>Classes seen by this processor (annotated with {@code @MigratedValueClass}
 * will have their source files re-written into the specified output directory
 * for compilation as preview classes. Note that more than one class in a given
 * source file may be annotated.
 *
 * <p>Class re-writing is achieved by injecting the "value" keyword in front of
 * class declarations for all annotated elements in the original source file.
 *
 * <p>Note that there are two annotations in use for value classes, but since
 * we must generate sources for abstract classes, we only process one of them.
 * <ul>
 *     <li>{@code @jdk.internal.ValueBased} appears on concrete value classes.
 *     <li>{@code @jdk.internal.MigratedValueClass} appears on concrete and
 *         abstract value classes.
 * </ul>
 */
@SupportedAnnotationTypes({"jdk.internal.MigratedValueClass"})
@SupportedOptions("valueclasses.outdir")
public final class GenValueClasses extends AbstractProcessor {
    // Matches preprocessor option flag in CompileJavaModules.gmk.
    private static final String OUTDIR_OPTION_KEY = "valueclasses.outdir";

    private ProcessingEnvironment processingEnv = null;
    private Path outDir = null;
    private Trees trees = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        String outDir = this.processingEnv.getOptions().get(OUTDIR_OPTION_KEY);
        if (outDir == null) {
            throw new IllegalStateException(
                    "Must specify -A" + OUTDIR_OPTION_KEY + "=<output-directory-path>"
                            + " for annotation processor: " + GenValueClasses.class.getName());
        }
        this.outDir = Path.of(outDir.replace('/', File.separatorChar));
        this.trees = Trees.instance(this.processingEnv);
    }

    /**
     * Override to return latest version, since the runtime in which this is
     * compiled doesn't know about development source versions.
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        // We don't have direct access to MigratedValueClass classes here.
        Optional<? extends TypeElement> valueClassAnnotation =
                getAnnotation(annotations, "jdk.internal.MigratedValueClass");
        if (valueClassAnnotation.isPresent()) {
            getAnnotatedTypes(env, valueClassAnnotation.get()).stream()
                    .collect(groupingBy(this::javaSourceFile))
                    .forEach(this::generateValueClassSource);
        }
        // We may not be the only annotation processor to consume this annotation.
        return false;
    }

    /** Find the annotation element by name in the given set. */
    private static Optional<? extends TypeElement> getAnnotation(Set<? extends TypeElement> annotations, String name) {
        return annotations.stream()
                .filter(e -> e.getQualifiedName().toString().equals(name))
                .findFirst();
    }

    /** Find the type elements (classes) annotated with the given annotation element. */
    private static Set<TypeElement> getAnnotatedTypes(RoundEnvironment env, TypeElement annotation) {
        Set<TypeElement> types = new HashSet<>();
        for (Element e : env.getElementsAnnotatedWith(annotation)) {
            if (!e.getKind().isClass()) {
                throw new IllegalStateException(
                        "Unexpected element kind (" + e.getKind() + ") for element: " + e);
            }
            TypeElement type = (TypeElement) e;
            if (type.getQualifiedName().isEmpty()) {
                throw new IllegalStateException(
                        "Unexpected empty name for element: " + e);
            }
            types.add(type);
        }
        return types;
    }

    /**
     * Write a transformed version of the given Java source file with the
     * {@code value} keyword inserted before the class declaration of each
     * annotated type element.
     */
    private void generateValueClassSource(Path srcPath, List<TypeElement> classes) {
        try {
            // We know there's at least one element per source file (by construction).
            TypeElement element = classes.getFirst();
            Path relPath = moduleRelativePath(srcPath, packageName(element));
            Path outPath = outDir.resolve(moduleName(element)).resolve(relPath);
            Files.createDirectories(outPath.getParent());

            List<Long> insertPositions =
                    classes.stream().map(this::valueKeywordInsertPosition).sorted().toList();

            // For partial rebuilds, generated sources may still exist, so we overwrite them.
            try (Reader reader = new InputStreamReader(Files.newInputStream(srcPath));
                 Writer output = new OutputStreamWriter(
                         Files.newOutputStream(outPath, CREATE, TRUNCATE_EXISTING))) {
                long curPos = 0;
                for (long nxtPos : insertPositions) {
                    int nextChunkLen = Math.toIntExact(nxtPos - curPos);
                    long written = new LimitedReader(reader, nextChunkLen).transferTo(output);
                    if (written != nextChunkLen) {
                        throw new IOException("Unexpected number of characters transferred."
                                + " Expected " + nextChunkLen + " but was " + written);
                    }
                    curPos = nxtPos;
                    // curPos is at the end of the modifier section, so add a leading space.
                    // curPos ---v
                    // [modifiers] class...  -->>  [modifiers] value class...
                    output.write(" value");
                }
                // Trailing section to end-of-file transferred from original reader.
                reader.transferTo(output);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the character offset in the original source file at which to insert
     * the {@code value} keyword. The offset is the end of the modifiers section,
     * which must immediately precede the class declaration.
     */
    private long valueKeywordInsertPosition(TypeElement classElement) {
        TreePath classDecl = trees.getPath(classElement);
        ClassTree classTree = (ClassTree) classDecl.getLeaf();
        CompilationUnitTree compilationUnit = classDecl.getCompilationUnit();
        // Since annotations are held as "modifiers", and since we only process
        // elements with annotations, the positions of the modifiers section must
        // be well-defined.
        long pos = trees.getSourcePositions().getEndPosition(compilationUnit, classTree.getModifiers());
        if (pos == Diagnostic.NOPOS) {
            throw new IllegalStateException("Missing position information: " + classElement);
        }
        return pos;
    }

    private Path moduleRelativePath(Path srcPath, String pkgName) {
        Path relPath = Path.of(pkgName.replace('.', File.separatorChar)).resolve(srcPath.getFileName());
        if (!srcPath.endsWith(relPath)) {
            throw new IllegalStateException(String.format(
                    "Expected trailing path %s for source file %s", relPath, srcPath));
        }
        return relPath;
    }

    private String moduleName(TypeElement t) {
        return processingEnv.getElementUtils().getModuleOf(t).getQualifiedName().toString();
    }

    private String packageName(TypeElement t) {
        return processingEnv.getElementUtils().getPackageOf(t).getQualifiedName().toString();
    }

    private Path javaSourceFile(TypeElement type) {
        return filePath(processingEnv.getElementUtils().getFileObjectOf(type));
    }

    private static Path filePath(FileObject file) {
        return Path.of(file.toUri());
    }

    /**
     * A forwarding reader which guarantees to read no more than
     * {@code maxCharCount} characters from the underlying stream.
     */
    private static final class LimitedReader extends Reader {
        // These are short-lived, no need to null the delegate when closed.
        private final Reader delegate;
        // This should never go negative.
        private int remainingChars;

        /**
         * Creates a limited reader which reads up to {@code maxCharCount} chars
         * from the given stream.
         *
         * @param delegate     underlying reader
         * @param maxCharCount maximum chars to read (can be 0)
         */
        LimitedReader(Reader delegate, int maxCharCount) {
            this.delegate = Objects.requireNonNull(delegate);
            this.remainingChars = Math.max(maxCharCount, 0);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (remainingChars > 0) {
                int readLimit = Math.min(remainingChars, len);
                int count = delegate.read(cbuf, off, readLimit);
                // Only update remainingChars if something was read.
                if (count > 0) {
                    if (count > remainingChars) {
                        throw new IOException(
                                "Underlying Reader exceeded requested read limit." +
                                        " Expected at most " + readLimit + " but read " + count);
                    }
                    remainingChars -= count;
                }
                // Can return 0 or -1 here (the underlying reader could finish first).
                return count;
            } else if (remainingChars == 0) {
                return -1;
            } else {
                throw new AssertionError("Remaining character count should never be negative!");
            }
        }

        @Override
        public void close() {
            // Do not close the delegate since this is conceptually just a view.
        }
    }
}
