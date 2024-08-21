/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one ImplicitCreation attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @compile DuplicateImplicitCreationAttr.jcod
 * @compile/fail/ref=CheckImplicitCreationAttrIsUnique.out --enable-preview -source ${jdk.version} -XDrawDiagnostics CheckImplicitCreationAttrIsUnique.java
 */

public class CheckImplicitCreationAttrIsUnique {
    void m() {
        DuplicateImplicitCreationAttr v = new DuplicateImplicitCreationAttr();
    }
}
