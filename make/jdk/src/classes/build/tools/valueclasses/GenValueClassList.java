package build.tools.valueclasses;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * <ul>
 *     <li>{@code @jdk.internal.ValueBased} appears on concrete value classes.
 *     <li>{@code @jdk.internal.MigratedValueClass} appears on concrete and abstract value classes.
 * </ul>
 */
@SupportedAnnotationTypes({"jdk.internal.MigratedValueClass", "jdk.internal.ValueBased"})
public final class GenValueClassList extends AbstractProcessor {
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
            Set<String> allValueClasses = getAnnotatedTypeNames(env, migratedValueClass.get());
            Set<String> concreteValueClasses = valueBased.map(a -> getAnnotatedTypeNames(env, a)).orElse(Set.of());
            allValueClasses.forEach(n -> System.err.format("---> %s (%s)", n, concreteValueClasses.contains(n) ? "concrete" : "abstract"));
        }
        return true;
    }

    private static Optional<? extends TypeElement> getAnnotation(Set<? extends TypeElement> annotations, String name) {
        return annotations.stream()
                .filter(e -> e.getQualifiedName().toString().equals(name))
                .findFirst();
    }

    private static Set<String> getAnnotatedTypeNames(RoundEnvironment env, TypeElement annotation) {
        Set<String> typeNames = new TreeSet<>();
        for (Element e : env.getElementsAnnotatedWith(annotation)) {
            if (!e.getKind().isClass()) {
                throw new IllegalStateException(
                        "Unexpected element kind (" + e.getKind() + ") for element: " + e);
            }
            Name fqn = ((TypeElement) e).getQualifiedName();
            if (fqn.isEmpty()) {
                throw new IllegalStateException(
                        "Unexpected empty name for element: " + e);
            }
            typeNames.add(fqn.toString());
        }
        return typeNames;
    }
}
