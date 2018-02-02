/*
 * @test /nodynamiccopyright/
 * @summary Verify that the class reader flags Value capable classes appropriately.
 * @modules jdk.incubator.mvt
 * @compile Point.java
 * @compile/fail/ref=ClassFileReaderTest.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values  ClassFileReaderTest.java
 */

public class ClassFileReaderTest {
    Point point = null;
}
