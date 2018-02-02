/*
 * @test /nodynamiccopyright/
 * @summary May not synchronize on value types
 * @modules jdk.incubator.mvt
 * @compile/fail/ref=CheckSync.out --should-stop:ifError=PARSE -XDrawDiagnostics -Werror -Xlint:values  CheckSync.java
 */

/* Note: ATM, value types do not have jlO in their lineage. So they anyway
   cannot synchronize using the methods declared on jlO.
*/
@jdk.incubator.mvt.ValueCapableClass
public final class CheckSync {
    @jdk.incubator.mvt.ValueCapableClass
    final class Val {

        void foo() {
            // All calls below are bad.
            wait();
            wait(10);
            wait(10, 10);
            notify();
            notifyAll();
            finalize();
            clone();
        }
    }

    final Val val = new Val();

    void test() throws InterruptedException {
        // All calls below are bad.
        val.wait();
        val.wait(10);
        val.wait(new Integer(10));
        val.wait(new Long(10));
        val.wait(10L);
        val.wait(10L, 10);
        val.wait("Hello");
        val.notify();
        val.notifyAll();
    }
}
