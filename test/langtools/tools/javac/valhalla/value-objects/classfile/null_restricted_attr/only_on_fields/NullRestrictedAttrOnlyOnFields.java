/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one NullRestricted attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @compile --enable-preview -source ${jdk.version} NullRestrictedOnMethod.jcod
 * @compile/fail/ref=NullRestrictedAttrOnlyOnFields.out --enable-preview -source ${jdk.version} -XDrawDiagnostics NullRestrictedAttrOnlyOnFields.java
 */

public class NullRestrictedAttrOnlyOnFields {
    void m() {
        NullRestrictedOnMethod v = new NullRestrictedOnMethod();
    }
}
