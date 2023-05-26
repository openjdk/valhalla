/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test id=default
 * @summary Verifies JVMTI InterruptThread works for virtual threads.
 * @compile InterruptThreadTest.java
 * @run main/othervm/native -agentlib:InterruptThreadTest InterruptThreadTest
 */

/*
 * @test id=no-vmcontinuations
 * @requires vm.continuations
 * @compile InterruptThreadTest.java
 * @run main/othervm/native -agentlib:InterruptThreadTest -XX:+UnlockExperimentalVMOptions -XX:-VMContinuations InterruptThreadTest
 */

import java.util.concurrent.atomic.AtomicBoolean;

public class InterruptThreadTest {
    private static final String AGENT_LIB = "InterruptThreadTest";
    final Object lock = new Object();

    native boolean testJvmtiFunctionsInJNICall(Thread vthread);

    volatile private boolean target_is_ready = false;
    private boolean iterrupted = false;

    final Runnable pinnedTask = () -> {
        synchronized (lock) {
            try {
                target_is_ready = true;
                lock.wait();
            } catch (InterruptedException ie) {
                 System.err.println("Virtual thread was interrupted as expected");
                 iterrupted = true;
            }
        }
    };

    void runTest() throws Exception {
        Thread vthread = Thread.ofVirtual().name("VThread").start(pinnedTask);

        // wait for target virtual thread to reach the expected waiting state
        while (!target_is_ready) {
           synchronized (lock) {
              lock.wait(1);
            }
        }
        testJvmtiFunctionsInJNICall(vthread);
        vthread.join();
        if (!iterrupted) {
            throw new RuntimeException("Failed: Virtual thread was not interrupted!");
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            System.loadLibrary(AGENT_LIB);
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Failed to load " + AGENT_LIB + " lib");
            System.err.println("java.library.path: " + System.getProperty("java.library.path"));
            throw ex;
        }
        InterruptThreadTest t = new InterruptThreadTest();
        t.runTest();
    }
}
