/*
 * @test /nodynamiccopyright/
 * @bug 8237072
 * @summary Test various relationships between a value type and its reference projection.
 * @compile/fail/ref=OverloadingPhaseTest.out -XDrawDiagnostics OverloadingPhaseTest.java
 */

public class OverloadingPhaseTest {

    static primitive class V {
        int x = 0;
    }

    static String roo(V.ref v, int i) {
        return "Phase 1";
    }

    static String roo(V.ref v, Integer i) {
        return "Phase 2";
    }

    public static void main(String args) {
        V o = new V();
        String result;

        if (!(result = roo(o, 0)).equals("phase 2"))
            throw new AssertionError("Broken: got " + result);
        if (!(result = roo(o, Integer.valueOf(0))).equals("phase 2"))
            throw new AssertionError("Broken: got " + result);
    }
}
