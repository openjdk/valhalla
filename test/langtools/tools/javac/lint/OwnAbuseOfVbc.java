/*
 * @test /nodynamiccopyright/
 * @bug 8254274
 * @summary lint should warn when an instance of a value based class is synchronized upon
 * @compile/fail/ref=OwnAbuseOfVbc.out --patch-module java.base=${test.src} -XDrawDiagnostics -Werror -Xlint:synchronize OwnAbuseOfVbc.java
 */
package java.lang;

@jdk.internal.ValueBased
public final class OwnAbuseOfVbc {

    final String ref = "String";

    void abuseVbc() throws InterruptedException {

        synchronized(ref) {           // OK
            synchronized (this) {     // WARN

                ref.wait();           // OK
                wait();               // WARN
                this.wait();          // WARN
                super.wait();         // WARN

                ref.notify();         // OK
                notify();             // WARN
                this.notify();        // WARN
                super.notify();       // WARN

                ref.notifyAll();      // OK
                notifyAll();          // WARN
                this.notifyAll();     // WARN
                super.notifyAll();    // WARN

                ref.wait(10);         // OK
                wait(10);             // WARN
                this.wait(10);        // WARN
                super.wait(10);       // WARN

                ref.wait(10L, 10);    // OK
                wait(10L, 10);        // WARN
                this.wait(10L, 10);   // WARN
                super.wait(10L, 10);  // WARN

            }
        }
    }
}

