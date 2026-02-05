/*
 * @test /nodynamiccopyright/
 * @bug 8199194
 * @summary smoke test for --enabled-preview classreader support
 * @compile -XDforcePreview --enable-preview -source ${jdk.version} Bar.java
 *
 * @comment These tests succeed in bworld-apis:
 * @compile -Xlint:preview -XDrawDiagnostics Client.java
 * @compile -Werror -Xlint:preview -XDrawDiagnostics --enable-preview -source ${jdk.version} Client.java
 */

public class Client {
    void test() {
        new Bar();
    }
}
