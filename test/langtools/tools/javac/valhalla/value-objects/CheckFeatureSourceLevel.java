/*
 * @test /nodynamiccopyright/
 * @bug 8279368
 * @summary Add parser support for value classes
 * @compile/fail/ref=CheckFeatureSourceLevel.out --release=13 -XDrawDiagnostics CheckFeatureSourceLevel.java
 */

public class CheckFeatureSourceLevel {

    static value class Value {
        public int v = 42;
    }
}
