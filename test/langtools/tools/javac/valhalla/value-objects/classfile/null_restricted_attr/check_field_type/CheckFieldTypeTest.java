/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one NullRestricted attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @compile NullRestrictedOnPrimitive.jcod
 * @compile/fail/ref=CheckFieldTypeTest.out -XDrawDiagnostics CheckFieldTypeTest.java
 */

public class CheckFieldTypeTest {
    void m() {
        NullRestrictedOnPrimitive v = new NullRestrictedOnPrimitive();
    }
}
