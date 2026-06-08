package build.tools.valhalla.valuetypes;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Plugin for generating preview sources of classes annotated as value classes
 * for preview mode.
 *
 * <p>Classes seen by this plugin (annotated with {@code @MigratedValueClass}
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
public final class GenValueClassPlugin implements Plugin {
    private static final String VALUE_CLASS_ANNOTATION = "@jdk.internal.MigratedValueClass";

    @Override
    public String getName() {
        return "GenValueClassPlugin";
    }

    @Override
    public void init(JavacTask task, String... args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Plugin " + getName() + ": missing output directory argument");
        }
        Path outDir = Path.of(args[0]);
        if (!Files.isDirectory(outDir)) {
            throw new IllegalArgumentException("Plugin " + getName() + ": no such output directory: " + outDir);
        }
        task.addTaskListener(new ValueClassGenerator(outDir));
    }

    private record ValueClassGenerator(Path outDir) implements TaskListener {
        @Override
        public void finished(TaskEvent e) {
            CompilationUnitTree compilation = e.getCompilationUnit();

            List<JCClassDecl> classes = new ArrayList<>();
            new TreeScanner() {
                @Override
                public void visitClassDef(JCClassDecl cls) {
                    boolean hasAnnotation = cls.getModifiers().getAnnotations().stream()
                            .peek(a -> System.out.println("--> " + a))
                            .anyMatch(a -> a.toString().equals(VALUE_CLASS_ANNOTATION));
                    if (hasAnnotation) {
                        classes.add(cls);
                    }
                    super.visitClassDef(cls);
                }
            }.scan((JCTree) compilation);

            if (!classes.isEmpty()) {
                Path srcPath = filePath(compilation.getSourceFile());
                String moduleName = compilation.getModule().getName().toString();
                String packageName = compilation.getPackage().toString();
                generateValueClassSource(srcPath, moduleName, packageName, classes);
            }
        }

        /**
         * Write a transformed version of the given Java source file with the
         * {@code value} keyword inserted before the class declaration of each
         * annotated type element.
         */
        private void generateValueClassSource(
                Path srcPath, String moduleName, String packageName, List<JCClassDecl> classes) {

            System.out.println("Module: " + moduleName);
            System.out.println("Package: " + packageName);
            System.out.println("Classes: " + classes.stream().map(c -> c.type.toString()).toList());

            Path relPath = moduleRelativePath(srcPath, packageName);
            Path outPath = outDir.resolve(moduleName).resolve(relPath);

            System.out.println("Out path: " + outPath);

            try {
                Files.createDirectories(outPath.getParent());

                List<Integer> insertPositions =
                        classes.stream().map(this::valueKeywordInsertPosition).sorted().toList();

                System.out.println("Insert positions: " + insertPositions);

                // For partial rebuilds, generated sources may still exist, so we overwrite them.
                try (Reader reader = new InputStreamReader(Files.newInputStream(srcPath));
                     Writer output = new OutputStreamWriter(
                             Files.newOutputStream(outPath, CREATE, TRUNCATE_EXISTING))) {
                    int curPos = 0;
                    for (int nxtPos : insertPositions) {
                        int nextChunkLen = nxtPos - curPos;
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

        private Path moduleRelativePath(Path srcPath, String pkgName) {
            Path relPath = Path.of(pkgName.replace('.', File.separatorChar)).resolve(srcPath.getFileName());
            if (!srcPath.endsWith(relPath)) {
                throw new IllegalStateException(String.format(
                        "Expected trailing path %s for source file %s", relPath, srcPath));
            }
            return relPath;
        }

        /**
         * Returns the character offset in the original source file at which to insert
         * the {@code value} keyword. The offset is the end of the modifiers section,
         * which must immediately precede the class declaration.
         */
        private int valueKeywordInsertPosition(JCClassDecl classDecl) {
            int pos = TreeInfo.getStartPos(classDecl.getModifiers());
            if (pos == Diagnostic.NOPOS) {
                throw new IllegalStateException("Missing position information: " + classDecl);
            }
            return pos;
        }
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
