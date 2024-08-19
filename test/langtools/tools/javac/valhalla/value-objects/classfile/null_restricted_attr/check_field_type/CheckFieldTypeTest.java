/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one NullRestricted attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @compile --enable-preview -source ${jdk.version} -XDenableNullRestrictedTypes NullRestrictedOnPrimitive.jcod
 * @compile/fail/ref=CheckFieldTypeTest.out --enable-preview -source ${jdk.version} -XDenableNullRestrictedTypes -XDrawDiagnostics CheckFieldTypeTest.java
 */

public class CheckFieldTypeTest {
    void m() {
        NullRestrictedOnPrimitive v = new NullRestrictedOnPrimitive();
    }
}
