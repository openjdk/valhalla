/*
 * @test /nodynamiccopyright/
 * @bug 8279839
 * @summary [lworld] Javac has started incorrectly accepting native as a modifer for classes
 * @compile/fail/ref=NativeModifierTest.out -XDrawDiagnostics -XDdev NativeModifierTest.java
 */

public native class NativeModifierTest {

    public native class NativeClassIsNotAThing {
    }

}
