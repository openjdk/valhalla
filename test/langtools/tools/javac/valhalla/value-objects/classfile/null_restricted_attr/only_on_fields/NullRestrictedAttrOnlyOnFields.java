/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one NullRestricted attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @compile NullRestrictedOnMethod.jcod
 * @compile/fail/ref=NullRestrictedAttrOnlyOnFields.out -XDrawDiagnostics NullRestrictedAttrOnlyOnFields.java
 */

public class NullRestrictedAttrOnlyOnFields {
    void m() {
        NullRestrictedOnMethod v = new NullRestrictedOnMethod();
    }
}
