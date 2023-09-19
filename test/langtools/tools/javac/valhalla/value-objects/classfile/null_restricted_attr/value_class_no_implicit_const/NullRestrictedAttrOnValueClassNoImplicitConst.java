/* @test /nodynamiccopyright/
 * @summary [lw5] check that there can only be one NullRestricted attribute
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @ignore
 * @compile ValueClass.jcod NullRestrictedOnValueClass.jcod
 * @compile/fail/ref=NullRestrictedAttrOnValueClassNoImplicitConst.out -XDrawDiagnostics NullRestrictedAttrOnValueClassNoImplicitConst.java
 */

 /* testing if a value class has an implicit constructor or not would imply loading the class if it is not loaded, this could provoke
  * altering the class loading order, not sure if this is worthy. Basically the assertion in the JVMS is:
  * `The descriptor_index of the field should name a value class that has an ImplicitCreation attribute with its ACC_DEFAULT flag is set`
  */
public class NullRestrictedAttrOnValueClassNoImplicitConst {
    void m() {
        NullRestrictedOnValueClass v = new NullRestrictedOnValueClass();
    }
}
