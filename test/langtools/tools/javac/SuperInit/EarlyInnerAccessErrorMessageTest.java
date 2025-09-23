/*
 * @test /nodynamiccopyright/
 * @bug 8334488
 * @summary Verify the error message generated for early access from inner class
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jlink
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile/fail/ref=EarlyInnerAccessErrorMessageTest.out -XDrawDiagnostics EarlyInnerAccessErrorMessageTest.java
 * @build InitializationWarningTester
 * @run main InitializationWarningTester EarlyInnerAccessErrorMessageTest
 */
public class EarlyInnerAccessErrorMessageTest {
    int x;
    EarlyInnerAccessErrorMessageTest() {
        class Inner {
            { System.out.println(x); }
        }
        super();
    }
}
