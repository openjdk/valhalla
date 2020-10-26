/*
 * @test /nodynamiccopyright/
 * @bug 8254274
 * @summary lint should warn when an instance of a value based class is synchronized upon
 * @compile/fail/ref=JdkInternalAbuseOfVbc.out --patch-module java.base=${test.src} -XDrawDiagnostics -Werror -Xlint:synchronize OwnAbuseOfVbc.java JdkInternalAbuseOfVbc.java
 */

package java.lang;

public final class JdkInternalAbuseOfVbc {

    final String ref = "String";

    void abuseVbc(OwnAbuseOfVbc vbc) throws InterruptedException {

        synchronized(this) {           // OK
            synchronized (vbc) {     // WARN

                this.wait();           // OK
                vbc.wait();            // WARN

                this.notify();         // OK
                vbc.notify();          // WARN

                this.notifyAll();      // OK
                vbc.notifyAll();       // WARN

                this.wait(10);         // OK
                vbc.wait(10);          // WARN

                this.wait(10L, 10);    // OK
                vbc.wait(10L, 10);     // WARN

            }
        }
    }
}

