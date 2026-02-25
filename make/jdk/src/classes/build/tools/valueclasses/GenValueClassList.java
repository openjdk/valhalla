package build.tools.valueclasses;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 * <ul>
 *     <li>{@code @jdk.internal.ValueBased} appears on concrete value classes.
 *     <li>{@code @jdk.internal.MigratedValueClass} appears on concrete and abstract value classes.
 * </ul>
 */
@SupportedAnnotationTypes({"jdk.internal.MigratedValueClass", "jdk.internal.ValueBased"})
@SupportedOptions("valueclasses.outdir")
public final class GenValueClassList extends AbstractProcessor {
    // Matches preprocessor option flag in CompileJavaModules.gmk.
    private static final String OUTDIR_OPTION_KEY = "valueclasses.outdir";

    private ProcessingEnvironment processingEnv = null;
    private Path outDir = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        String outDir = this.processingEnv.getOptions().get(OUTDIR_OPTION_KEY);
        if (outDir == null) {
            throw new IllegalStateException(
                    "Must specify -A" + OUTDIR_OPTION_KEY + "=<output-directory-path>"
                            + " for annotation processor: " + GenValueClassList.class.getName());
        }
        this.outDir = Path.of(outDir.replace('/', File.separatorChar));
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
        // We don't have direct access to ValueBased or MigratedValueClass
        // classes here, but we can find the matching type elements.
        Optional<? extends TypeElement> migratedValueClass =
                getAnnotation(annotations, "jdk.internal.MigratedValueClass");
        Optional<? extends TypeElement> valueBased =
                getAnnotation(annotations, "jdk.internal.ValueBased");
        if (migratedValueClass.isPresent()) {
            Set<TypeElement> allValueClasses = getAnnotatedTypes(env, migratedValueClass.get());
            Map<String, List<TypeElement>> moduleToType =
                    allValueClasses.stream().collect(groupingBy(this::getModuleName));
            try {
                for (String modName : moduleToType.keySet()) {
                    Map<Path, List<TypeElement>> fileToType =
                            moduleToType.get(modName).stream().collect(groupingBy(this::getJavaFile));
                    Files.writeString(
                            createOutputFile(modName),
                            fileToType.entrySet().stream()
                                    .map(e -> formatEntry(e.getKey(), e.getValue()))
                                    .collect(joining("\n")));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private String formatEntry(Path srcPath, List<TypeElement> classes) {
        // We know there's at least one element per source file (by construction).
        Path relPath = getRelativePath(srcPath, getPackageName(classes.getFirst()));
        List<String> simpleClassNames = classes.stream()
                .map(TypeElement::getSimpleName)
                .map(Object::toString).toList();
        return String.format("%s :: %s :: %s", srcPath, relPath, String.join(",", simpleClassNames));
    }

    private Path getRelativePath(Path srcPath, String pkgName) {
        Path relPath = Path.of(pkgName.replace('.', File.separatorChar)).resolve(srcPath.getFileName());
        if (!srcPath.endsWith(relPath)) {
            throw new IllegalStateException(String.format(
                    "Expected trailing path %s for source file %s", relPath, srcPath));
        }
        return relPath;
    }

    private static Optional<? extends TypeElement> getAnnotation(Set<? extends TypeElement> annotations, String name) {
        return annotations.stream()
                .filter(e -> e.getQualifiedName().toString().equals(name))
                .findFirst();
    }

    private Set<TypeElement> getAnnotatedTypes(RoundEnvironment env, TypeElement annotation) {
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

    private Path getJavaFile(TypeElement type) {
        return getFilePath(processingEnv.getElementUtils().getFileObjectOf(type));
    }

    private Path createOutputFile(String moduleName) throws IOException {
        Files.createDirectories(this.outDir);
        // Matches naming expectations of CompileJavaModules.gmk.
        return outDir.resolve("valueclasses-" + moduleName + ".txt");
    }

    private String getModuleName(TypeElement t) {
        return processingEnv.getElementUtils().getModuleOf(t).getQualifiedName().toString();
    }

    private String getPackageName(TypeElement t) {
        return processingEnv.getElementUtils().getPackageOf(t).getQualifiedName().toString();
    }

    private static Path getFilePath(FileObject file) {
        return Path.of(file.toUri());
    }
}
