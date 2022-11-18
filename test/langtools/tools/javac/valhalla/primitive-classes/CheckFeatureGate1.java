/*
 * @test /nodynamiccopyright/
 * @bug 8237067
 * @summary Check that feature gated constructs are not allowed in previous versions.
 * @compile/fail/ref=CheckFeatureGate1.out --release=13 -XDrawDiagnostics CheckFeatureGate1.java
 */

public class CheckFeatureGate1 {

    static primitive class Val {
        public int v = 42;
    }
}
