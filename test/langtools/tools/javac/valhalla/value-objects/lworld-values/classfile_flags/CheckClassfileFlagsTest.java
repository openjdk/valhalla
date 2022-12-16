/* @test /nodynamiccopyright/
 * @bug 8292883
 * @summary [lworld] javac fails to detect class files with invalid access flags
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @compile ValueWithInvalidFlags.jcod
 * @compile/fail/ref=CheckClassfileFlagsTest.out -XDrawDiagnostics CheckClassfileFlagsTest.java
 */

public class CheckClassfileFlagsTest {
    void m() {
        ValueWithInvalidFlags v = new ValueWithInvalidFlags();
    }
}
