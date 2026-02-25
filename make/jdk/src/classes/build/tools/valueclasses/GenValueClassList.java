package build.tools.valueclasses;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

/**
 * <ul>
 *     <li>{@code @jdk.internal.ValueBased} appears on concrete value classes.
 *     <li>{@code @jdk.internal.MigratedValueClass} appears on concrete and abstract value classes.
 * </ul>
 */
@SupportedAnnotationTypes({"jdk.internal.MigratedValueClass", "jdk.internal.ValueBased"})
public final class GenValueClassList extends AbstractProcessor {

    private ProcessingEnvironment processingEnv = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
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
            Set<TypeElement> concreteValueClasses = valueBased.map(a -> getAnnotatedTypes(env, a)).orElse(Set.of());
            allValueClasses.forEach(t -> System.out.format("---> %s (%s)%n", t.getQualifiedName(), concreteValueClasses.contains(t) ? "concrete" : "abstract"));

            Map<JavaFileObject, List<TypeElement>> fileToType =
                    allValueClasses.stream().collect(groupingBy(this::getJavaFile));

            fileToType.forEach((f, t) -> {
                Path path = Path.of(f.toUri());
                System.out.println("--> " + path + ": " + t.stream().map(TypeElement::getQualifiedName).toList());
            });
        }
        return true;
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

    private JavaFileObject getJavaFile(TypeElement type) {
        return processingEnv.getElementUtils().getFileObjectOf(type);
    }
}
