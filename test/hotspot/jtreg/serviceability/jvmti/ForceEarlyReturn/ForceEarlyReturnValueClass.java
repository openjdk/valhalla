/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary Test ForceEarlyReturnVoid when the target thread's top frame is a value class
 *     constructor, method or class initializer.
 * @enablePreview
 * @run junit/othervm/native --enable-native-access=ALL-UNNAMED -agentlib:ForceEarlyReturnValueClass ${test.main.class}
 */

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForceEarlyReturnValueClass {

    private static final int JVMTI_ERROR_NONE = 0;
    private static final int JVMTI_ERROR_OPAQUE_FRAME = 32;

    /**
     * Test that ForceEarlyReturnVoid fails with JVMTI_ERROR_OPAQUE_FRAME when the
     * target thread's top frame is a value class constructor.
     */
    @Test
    void testForceEarlyReturnConstructor() throws Exception {
        class TestCase {
            static volatile boolean started;
            static volatile boolean stop;
            static volatile boolean finished;
            static value class ValueClass {
                ValueClass() {
                    TestCase.started = true;
                    while (!TestCase.stop) {}
                    TestCase.finished = true;
                }
            }
        }
        Thread thread = Thread.ofPlatform().start(TestCase.ValueClass::new);
        try {
            // wait for target thread to start executing constructor
            while (!TestCase.started) {
                Thread.sleep(10);
            }
            assertEquals(JVMTI_ERROR_NONE, suspendThread(thread));
            assertEquals(JVMTI_ERROR_OPAQUE_FRAME, forceEarlyReturnVoid(thread));
        } finally {
            TestCase.stop = true;
            resumeThread(thread);
            thread.join();
        }
        assertTrue(TestCase.finished);  // constructor should have run to end
    }

    /**
     * Test that ForceEarlyReturnVoid succeeds with the target thread's top frame is a
     * value class method.
     */
    @Test
    void testForceEarlyReturnMethod() throws Exception {
        class TestCase {
            static volatile boolean started;
            static volatile boolean stop;
            static volatile boolean finished;
            static value class ValueClass {
                void run() {
                    TestCase.started = true;
                    while (!TestCase.stop) {}
                    TestCase.finished = true;  // should not get there
                }
            }
        }
        var valueObj = new TestCase.ValueClass();
        Thread thread = Thread.ofPlatform().start(valueObj::run);
        try {
            // wait for target thread to start executing method
            while (!TestCase.started) {
                Thread.sleep(10);
            }
            assertEquals(JVMTI_ERROR_NONE, suspendThread(thread));
            assertEquals(JVMTI_ERROR_NONE, forceEarlyReturnVoid(thread));
        } finally {
            TestCase.stop = true;
            resumeThread(thread);
            thread.join();
        }
        assertFalse(TestCase.finished);  // should not have run to the end
    }

    /**
     * Test that ForceEarlyReturnVoid succeeds with the target thread's top frame is a
     * value class clinit.
     */
    @Test
    void testForceEarlyReturnClassInitializer() throws Exception {
        class TestCase {
            static volatile boolean started;
            static volatile boolean stop;
            static volatile boolean finished;
            static value class ValueClass {
                static final boolean initialized;
                static {
                    TestCase.started = true;
                    while (!TestCase.stop) {}
                    initialized = true;    // should not get there
                }
            }
        }
        Thread thread = Thread.ofPlatform().start(TestCase.ValueClass::new);
        try {
            // wait for target thread to start executing class initializer
            while (!TestCase.started) {
                Thread.sleep(10);
            }
            assertEquals(JVMTI_ERROR_NONE, suspendThread(thread));
            assertEquals(JVMTI_ERROR_NONE, forceEarlyReturnVoid(thread));
        } finally {
            TestCase.stop = true;
            resumeThread(thread);
            thread.join();
        }
        assertFalse(TestCase.ValueClass.initialized);  // should not have run to the end
    }

    private static native int suspendThread(Thread thread);
    private static native int resumeThread(Thread thread);
    private static native int forceEarlyReturnVoid(Thread thread);
    private static native int forceEarlyReturnObject(Thread thread, Object retObject);
}
