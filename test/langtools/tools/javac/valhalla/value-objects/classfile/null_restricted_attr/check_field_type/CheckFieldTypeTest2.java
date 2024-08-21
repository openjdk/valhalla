/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one NullRestricted attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @compile --enable-preview -source ${jdk.version} NullRestrictedOnArray.jcod
 * @compile/fail/ref=CheckFieldTypeTest2.out --enable-preview -source ${jdk.version} -XDrawDiagnostics CheckFieldTypeTest2.java
 */

public class CheckFieldTypeTest2 {
    void m() {
        NullRestrictedOnArray v = new NullRestrictedOnArray();
    }
}
