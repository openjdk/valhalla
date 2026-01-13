/*
 * @test /nodynamiccopyright/
 * @summary Matrix test for witness accessibility check
 * @compile/fail/ref=WitnessAccessCheck.out -XDrawDiagnostics WitnessAccessCheck.java
 */

class WitnessAccessCheck {
    interface U<X> { }
    interface W<X> { }
    interface Y<X> { }
    interface Z<X> { }

    public static class A_pub {
        private __witness U<A_pub> U_pri = null; // error, weaker access
        protected __witness W<A_pub> W_pro = null; // error, weaker access
        public __witness Y<A_pub> Y_pub = null; // ok
        __witness Z<A_pub> Z_pkg = null; // error, weaker access
    }

    private static class A_priv {
        private __witness U<A_priv> U_pri = null; // ok
        protected __witness W<A_priv> W_pro = null; // ok
        public __witness Y<A_priv> Y_pub = null; // ok
        __witness Z<A_priv> Z_pkg = null; // ok
    }

    protected static class A_prot {
        private __witness U<A_prot> U_pri = null; // error, weaker access
        protected __witness W<A_prot> W_pro = null; // ok
        public __witness Y<A_prot> Y_pub = null; // ok
        __witness Z<A_prot> Z_pkg = null; // error, weaker access
    }

    static class A_pkg {
        private __witness U<A_pkg> U_pri = null; // error, weaker access
        protected __witness W<A_pkg> W_pro = null; // ok
        public __witness Y<A_pkg> Y_pub = null; // ok
        __witness Z<A_pkg> Z_pkg = null; // ok
    }
}
