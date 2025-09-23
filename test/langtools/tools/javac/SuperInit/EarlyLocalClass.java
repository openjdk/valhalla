/*
 * @test /nodynamiccopyright/
 * @bug 8325805
 * @summary Verify local class in early construction context has no outer instance
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jlink
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile/fail/ref=EarlyLocalClass.out -XDrawDiagnostics EarlyLocalClass.java
 * @build InitializationWarningTester
 * @run main InitializationWarningTester EarlyLocalClass
 */
public class EarlyLocalClass {
    EarlyLocalClass() {
        class Local {
            void foo() {
                EarlyLocalClass.this.hashCode();    // this should FAIL
            }
        }
        new Local();                                // this is OK
        super();
        new Local();                                // this is OK
    }
}
