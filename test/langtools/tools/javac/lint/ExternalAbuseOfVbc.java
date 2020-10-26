/*
 * @test /nodynamiccopyright/
 * @bug 8254274
 * @summary lint should warn when an instance of a value based class is synchronized upon
 * @compile/fail/ref=ExternalAbuseOfVbc.out -XDrawDiagnostics -Werror -Xlint:synchronize ExternalAbuseOfVbc.java
 * @compile/ref=LintModeOffAbuseOfVbc.out -XDrawDiagnostics -Werror -Xlint:-synchronize ExternalAbuseOfVbc.java
 */

public final class ExternalAbuseOfVbc {

    final Integer val = Integer.valueOf(42);
    final String ref = "String";

    void abuseVbc() throws InterruptedException {

        synchronized(ref) {      // OK
            synchronized (val) { // WARN

                ref.wait();      // OK
                val.wait();      // WARN

                ref.notify();    // OK
                val.notify();    // WARN

                ref.notifyAll(); // OK
                val.notifyAll(); // WARN

                ref.wait(10);    // OK
                val.wait(10);    // WARN

                ref.wait(Integer.valueOf(10)); // OK
                val.wait(Integer.valueOf(10)); // WARN

                ref.wait(Long.valueOf(10));    // OK
                val.wait(Long.valueOf(10));    // WARN

                ref.wait(10L);  // OK
                val.wait(10L);  // WARN

                ref.wait(10L, 10); // OK
                val.wait(10L, 10); // WARN

                ref.wait(Long.valueOf(10), Integer.valueOf(10)); // OK
                val.wait(Long.valueOf(10), Integer.valueOf(10)); // WARN
            }
        }
    }
}

