/*
 * @test /nodynamiccopyright/
 * @bug 8215507 8218040
 * @summary javac should NOT forbid conversion from value array to Object[]
 * @compile/fail/ref=CovariantArrayTest.out -XDrawDiagnostics -XDdev CovariantArrayTest.java
 */
public class CovariantArrayTest {
    static final primitive class V {
        public final int v1;
        private V () {v1 = 0;}
    }

    public static void main(String args[]) {
        int [] ia = new int[1];
        Object oa[] = (Object[])ia;
        oa = ia;

        V [] va = new V[1];
        Object oa2[] = (Object[])va;
        oa2 = va;
        va = oa2;
        va = (V []) oa2;
    }
}
