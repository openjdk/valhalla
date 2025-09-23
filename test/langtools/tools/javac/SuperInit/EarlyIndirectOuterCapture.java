/*
 * @test /nodynamiccopyright/
 * @bug 8334248
 * @summary Invalid error for early construction local class constructor method reference
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jlink
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile EarlyIndirectOuterCapture.java
 * @build InitializationWarningTester
 * @run main InitializationWarningTester EarlyIndirectOuterCapture
 */

public class EarlyIndirectOuterCapture {

    EarlyIndirectOuterCapture() {
        this(null);
    }

    EarlyIndirectOuterCapture(InnerSuperclass inner) { }

    class InnerSuperclass { }

    static class InnerOuter extends EarlyIndirectOuterCapture {     // accessible
        class InnerInnerOuter extends EarlyIndirectOuterCapture {   // not accessible
            InnerInnerOuter() {
                super(new InnerSuperclass() { }); // should this be accepted?, InnerSuperclass is not an inner class of InnerInnerOuter
            }

            InnerInnerOuter(boolean b) {
                super(InnerOuter.this.new InnerSuperclass() { }); // ok, explicit
            }
        }
    }
}
