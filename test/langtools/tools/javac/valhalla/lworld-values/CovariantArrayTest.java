/*
 * @test /nodynamiccopyright/
 * @bug 8215507 8218040
 * @summary javac should forbid conversion from value array to Object[]
 * @compile/fail/ref=NoncovariantArrayTest.out -XDnonCovariantValueArrays -XDrawDiagnostics -XDdev NoncovariantArrayTest.java
 * @compile/fail/ref=NoncovariantArrayTest2.out -XDrawDiagnostics -XDdev NoncovariantArrayTest.java
 */
public class NoncovariantArrayTest { 
    static final inline class V { 
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
