package build.tools.valueclasses;

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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.util.stream.Collectors.groupingBy;

/**
 * Annotation processor for generating preview sources of existing classes which
 * annotated as value classes for Valhalla.
 *
 * <p>Classes seen by this processor (annotated with {@code @MigratedValueClass}
 * will have their source files re-written into the configured output directory
 * for compilation as preview classes. Note that more than one class in a given
 * source file may be annotated.
 *
 * <p>Class re-writing is simply a matter of injecting the new "value" keyword
 * into a copy of the original source file after existing modifiers for all
 * annotated elements.
 *
 * <p>Note that there are two annotations in use for value classes, but since
 * we must generate sources for abstract classes, we only care about one of them.
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
            Set<TypeElement> allValueClasses = getAnnotatedTypes(env, valueClassAnnotation.get());
            Map<String, List<TypeElement>> moduleToType =
                    allValueClasses.stream().collect(groupingBy(this::getModuleName));
            for (String modName : moduleToType.keySet()) {
                moduleToType.get(modName).stream()
                        .collect(groupingBy(this::getJavaSourceFile))
                        .forEach(this::generateValueClassSource);
            }
        }
        return true;
    }

    private static Optional<? extends TypeElement> getAnnotation(Set<? extends TypeElement> annotations, String name) {
        return annotations.stream()
                .filter(e -> e.getQualifiedName().toString().equals(name))
                .findFirst();
    }

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

    private void generateValueClassSource(Path srcPath, List<TypeElement> classes) {
        try {
            // We know there's at least one element per source file (by construction).
            TypeElement element = classes.getFirst();
            Path relPath = getModuleRelativePath(srcPath, getPackageName(element));
            Path outPath = outDir.resolve(getModuleName(element)).resolve(relPath);
            Files.createDirectories(outPath.getParent());

            List<Long> insertPositions =
                    classes.stream().map(this::getValueKeywordInsertPosition).sorted().toList();

            try (Reader reader = new InputStreamReader(Files.newInputStream(srcPath));
                 Writer output = new OutputStreamWriter(Files.newOutputStream(outPath, CREATE_NEW))) {
                long curPos = 0;
                for (long nxtPos : insertPositions) {
                    int nextChunkLen = Math.toIntExact(nxtPos - curPos);
                    long written = new LimitedReader(reader, nextChunkLen).transferTo(output);
                    if (written != nextChunkLen) {
                        throw new IOException("Unexpected number of characters transferred."
                                + " Expected " + nextChunkLen + " but was " + written);
                    }
                    // Position is the end of modifier chars, so we need a leading space.
                    // pos ------v
                    // [modifiers] class... -->> [modifiers] value class...
                    output.write(" value");
                    curPos = nxtPos;
                }
                reader.transferTo(output);
            }
        } catch (IOException e) {
            // TODO: Decide about errors!
            throw new RuntimeException(e);
        }
    }

    private long getValueKeywordInsertPosition(TypeElement classElement) {
        TreePath classDecl = trees.getPath(classElement);
        ClassTree classTree = (ClassTree) classDecl.getLeaf();
        CompilationUnitTree compilationUnit = classDecl.getCompilationUnit();
        // Since annotations are held as "modifiers", and since we only process
        // elements with annotations, the positions for modifiers must be
        // well-defined (otherwise we'd get -1 here).
        return trees.getSourcePositions().getEndPosition(compilationUnit, classTree.getModifiers());
    }

    private Path getModuleRelativePath(Path srcPath, String pkgName) {
        Path relPath = Path.of(pkgName.replace('.', File.separatorChar)).resolve(srcPath.getFileName());
        if (!srcPath.endsWith(relPath)) {
            throw new IllegalStateException(String.format(
                    "Expected trailing path %s for source file %s", relPath, srcPath));
        }
        return relPath;
    }

    private String getModuleName(TypeElement t) {
        return processingEnv.getElementUtils().getModuleOf(t).getQualifiedName().toString();
    }

    private String getPackageName(TypeElement t) {
        return processingEnv.getElementUtils().getPackageOf(t).getQualifiedName().toString();
    }

    private Path getJavaSourceFile(TypeElement type) {
        return getFilePath(processingEnv.getElementUtils().getFileObjectOf(type));
    }

    private static Path getFilePath(FileObject file) {
        return Path.of(file.toUri());
    }

    /**
     * A forwarding reader which guarantees to read no more than
     * {@code maxCharCount} characters from the underlying stream.
     */
    private static final class LimitedReader extends Reader {
        // Since these are expected to be short-lived, no need
        // to null the delegate when we're closed.
        private final Reader delegate;
        // This should never go negative.
        private int remaining;

        /**
         * Creates a limited reader which reads up to {@code maxCharCount} chars
         * from the given stream.
         *
         * @param delegate     underlying reader
         * @param maxCharCount maximum chars to read (can be 0)
         */
        LimitedReader(Reader delegate, int maxCharCount) {
            this.delegate = Objects.requireNonNull(delegate);
            this.remaining = Math.max(maxCharCount, 0);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (remaining == 0) {
                return -1;
            }
            if (remaining > 0) {
                int readLimit = Math.min(remaining, len);
                int count = delegate.read(cbuf, off, readLimit);
                // Only update remaining if something was read.
                if (count > 0) {
                    if (count > remaining) {
                        throw new IOException(
                                "Underlying Reader exceeded requested read limit." +
                                        " Expected at most " + readLimit + " but read " + count);
                    }
                    remaining -= count;
                }
                return count;
            }
            throw new IllegalStateException("Remaining count should never be negative!");
        }

        @Override
        public void close() {
            // Do not close the delegate since this is conceptually just a view.
        }
    }
}
