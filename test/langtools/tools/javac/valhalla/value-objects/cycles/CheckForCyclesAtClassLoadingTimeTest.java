/*
 * @test /nodynamiccopyright/
 * @bug 8314165
 * @summary check for illegal circularity at class loading time
 * @build CyclicValueClass
 * @compile/fail/ref=CheckForCyclesAtClassLoadingTimeTest.out -XDrawDiagnostics CheckForCyclesAtClassLoadingTimeTest.java
 */
class CheckForCyclesAtClassLoadingTimeTest {
    CyclicValueClass cyclicValueClass;
}
