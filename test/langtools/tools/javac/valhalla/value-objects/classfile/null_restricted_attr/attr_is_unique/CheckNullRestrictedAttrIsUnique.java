/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one NullRestricted attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @compile ValueClass.jcod DuplicateNullRestrictedAttr.jcod
 * @compile/fail/ref=CheckNullRestrictedAttrIsUnique.out -XDrawDiagnostics CheckNullRestrictedAttrIsUnique.java
 */

public class CheckNullRestrictedAttrIsUnique {
    void m() {
        DuplicateNullRestrictedAttr v = new DuplicateNullRestrictedAttr();
    }
}
