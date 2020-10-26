/*
 * @test /nodynamiccopyright/
 * @bug 8254274
 * @summary lint should warn when an instance of a value based class is synchronized upon
 * @compile/fail/ref=AuxilliaryAbuseOfVbc.out --patch-module java.base=${test.src} -XDrawDiagnostics -Werror -Xlint:synchronize AuxilliaryAbuseOfVbc.java
 */

@jdk.internal.ValueBased
class SomeVbc {}

public final class AuxilliaryAbuseOfVbc {

    void abuseVbc(SomeVbc vbc) throws InterruptedException {

        synchronized(this) {           // OK
            synchronized (vbc) {       // WARN

                vbc.wait();           // WARN
                wait();               // OK
                this.wait();          // OK
                super.wait();         // OK

                vbc.notify();         // WARN
                notify();             // OK
                this.notify();        // OK
                super.notify();       // OK

                vbc.notifyAll();      // WARN
                notifyAll();          // OK
                this.notifyAll();     // OK
                super.notifyAll();    // OK

                vbc.wait(10);         // WARN
                wait(10);             // OK
                this.wait(10);        // OK
                super.wait(10);       // OK

                vbc.wait(10L, 10);    // WARN
                wait(10L, 10);        // OK
                this.wait(10L, 10);   // OK
                super.wait(10L, 10);  // OK

            }
        }
    }
}

