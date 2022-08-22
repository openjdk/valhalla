/*
 * @test
 * @bug 8292630
 * @summary [lworld] javac is accepting annotation interface declarations with modifiers: identity and value
 * @compile/fail/ref=AnnotationsConstraints.out -XDrawDiagnostics AnnotationsConstraints.java
 */

public class AnnotationsConstraints {
    // annotations can't have the `identity`
    identity @interface IA {}
    // nor the `value` modifiers
    value @interface VA {}
}
