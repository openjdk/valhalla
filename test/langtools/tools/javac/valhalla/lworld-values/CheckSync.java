/*
 * @test /nodynamiccopyright/
 * @summary May not synchronize on value types
 *
 * @compile/fail/ref=CheckSync.out -XDrawDiagnostics CheckSync.java
 */

/* Note: ATM, value types do not have jlO in their lineage. So they anyway
   cannot synchronize using the methods declared on jlO.
*/

public final __ByValue class CheckSync {

    final __ByValue class Val {
        int x = 10;
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

    final Val val = __MakeDefault Val();

    void test() throws InterruptedException {
        // All calls below are bad.
        val.wait();
        val.wait(10);
        val.wait(new Integer(10));
        val.wait(new Long(10));
        val.wait(10L);
        val.wait(10L, 10);
        val.notify();
        val.notifyAll();
    }
    int x = 10;
}
