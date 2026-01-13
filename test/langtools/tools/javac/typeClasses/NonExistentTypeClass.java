/*
 * @test /nodynamiccopyright/
 * @summary check that a non-existent type class in a witness lookup doesn't crash javac
 * @compile/fail/ref=NonExistentTypeClass.out -XDrawDiagnostics NonExistentTypeClass.java
 */

class NonExistentTypeClass {

    interface TC_f<X> {
        __witness NonExistent<String> NON_EXISTENT_FIELD = null;
    }
    interface TC_m<X> {
        __witness NonExistent<String> NON_EXISTENT_METHOD() {
            return null;
        }
    }

    interface TC_gm<X> {
        __witness <Z> NonExistent<Z> NON_EXISTENT_GENERIC_METHOD() {
            return null;
        }
    }

    void test() {
        Object o1 = TC_f<String>.__witness;
        Object o2 = TC_m<String>.__witness;
        Object o3 = TC_gm<String>.__witness;
    }
}
