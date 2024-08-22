/*
 * @test /nodynamiccopyright/
 * @bug 8314165
 * @summary check for illegal circularity at class loading time
 * @compile --enable-preview -source ${jdk.version} CyclicValueClass.jcod
 * @compile/fail/ref=CheckForCyclesAtClassLoadingTimeTest.out --enable-preview -source ${jdk.version} -XDrawDiagnostics CheckForCyclesAtClassLoadingTimeTest.java
 * @ignore
 */
class CheckForCyclesAtClassLoadingTimeTest {
    CyclicValueClass cyclicValueClass;
}
